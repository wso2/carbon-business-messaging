/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.andes.core.internal.inbound;

import com.google.common.util.concurrent.SettableFuture;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.andes.core.AndesAckData;
import org.wso2.carbon.andes.core.AndesException;
import org.wso2.carbon.andes.core.DeliverableAndesMetadata;
import org.wso2.carbon.andes.core.MessagingEngine;
import org.wso2.carbon.andes.core.internal.AndesContext;
import org.wso2.carbon.andes.core.internal.disruptor.BatchEventHandler;
import org.wso2.carbon.andes.core.store.AndesTransactionRollbackException;
import org.wso2.carbon.andes.core.store.FailureObservingStoreManager;
import org.wso2.carbon.andes.core.store.HealthAwareStore;
import org.wso2.carbon.andes.core.store.StoreHealthListener;
import org.wso2.carbon.andes.core.subscription.LocalSubscription;
import org.wso2.carbon.andes.core.subscription.SubscriptionEngine;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Acknowledgement Handler for the Disruptor based inbound event handling.
 * This handler processes acknowledgements received from clients and updates Andes.
 */
public class AckHandler implements BatchEventHandler, StoreHealthListener {

    private static Log log = LogFactory.getLog(AckHandler.class);

    private final MessagingEngine messagingEngine;

    private final SubscriptionEngine subscriptionEngine;

    /**
     * Maximum number to retries to delete messages from message store
     */
    private static final int MAX_MESSAGE_DELETION_COUNT = 5;

    /**
     * Indicates and provides a barrier if messages stores become offline.
     * marked as volatile since this value could be set from a different thread
     * (other than those of disrupter)
     */
    private volatile SettableFuture<Boolean> messageStoresUnavailable;

    /**
     * Keeps message meta-data that needs to be removed from the message store.
     */
    List<DeliverableAndesMetadata> messagesToRemove;

    AckHandler(MessagingEngine messagingEngine) {
        this.messagingEngine = messagingEngine;
        this.subscriptionEngine = AndesContext.getInstance().getSubscriptionEngine();
        this.messageStoresUnavailable = null;
        this.messagesToRemove = new ArrayList<>();
        FailureObservingStoreManager.registerStoreHealthListener(this);
    }

    @Override
    public void onEvent(final List<InboundEventContainer> eventList) throws Exception {
        if (log.isTraceEnabled()) {
            StringBuilder messageIDsString = new StringBuilder();
            for (InboundEventContainer inboundEvent : eventList) {
                messageIDsString.append(inboundEvent.ackData.getAcknowledgedMessage().getMessageId()).append(" , ");
            }
            log.trace(eventList.size() + " messages received : " + messageIDsString);
        }
        if (log.isDebugEnabled()) {
            log.debug(eventList.size() + " acknowledgements received from disruptor.");
        }

        try {
            ackReceived(eventList);
        } catch (AndesException e) {
            // Log the AndesException since there is no point in passing the exception to Disruptor
            log.error("Error occurred while processing acknowledgements ", e);
        }
    }

    /**
     * Updates the state of Andes and deletes relevant messages. (For topics
     * message deletion will happen only when
     * all the clients acknowledges)
     *
     * @param eventList inboundEvent list
     */
    public void ackReceived(final List<InboundEventContainer> eventList) throws AndesException {

        for (InboundEventContainer event : eventList) {

            AndesAckData ack = event.ackData;
            // For topics message is shared. If all acknowledgements are received only we should remove message
            boolean deleteMessage = ack.getAcknowledgedMessage().markAsAcknowledgedByChannel(ack.getChannelID());

            LocalSubscription subscription = subscriptionEngine.getLocalSubscriptionForChannelId(ack.getChannelID());
            subscription.ackReceived(ack.getAcknowledgedMessage().getMessageId());

            if (deleteMessage) {
                if (log.isDebugEnabled()) {
                    log.debug("Ok to delete message id " + ack.getAcknowledgedMessage().getMessageId());
                }
                //it is a must to set this to event container. Otherwise, multiple event handlers will see the status
                event.ackData.setBaringMessageRemovable();
                messagesToRemove.add(ack.getAcknowledgedMessage());
            }

        }

        /*
         * Checks for the message store availability if its not available
         * Ack handler needs to await until message store becomes available
         */
        if (messageStoresUnavailable != null) {
            try {

                log.info("Message store has become unavailable therefore waiting until store becomes available");
                messageStoresUnavailable.get();
                log.info("Message store became available. Resuming ack handler");
                messageStoresUnavailable = null; // we are passing the blockade
                // (therefore clear the it).
            } catch (InterruptedException e) {
                throw new AndesException("Thread interrupted while waiting for message stores to come online", e);
            } catch (ExecutionException e) {
                throw new AndesException("Error occurred while waiting for message stores to come online", e);
            }
        }

        deleteMessagesFromStore(0);
    }

    /**
     * Delete acknowledged messages from message store. Deletion is retried if it failed due to a
     * AndesTransactionRollbackException.
     *
     * @param numberOfRetriesBefore number of recursive calls
     * @throws AndesException
     */
    private void deleteMessagesFromStore(int numberOfRetriesBefore) throws AndesException {
        try {
            messagingEngine.deleteMessages(messagesToRemove);

            if (log.isTraceEnabled()) {
                StringBuilder messageIDsString = new StringBuilder();
                for (DeliverableAndesMetadata metadata : messagesToRemove) {
                    messageIDsString.append(metadata.getMessageId()).append(" , ");
                }
                log.trace(messagesToRemove.size() + " message ok to remove : " + messageIDsString);
            }
            messagesToRemove.clear();
        } catch (AndesTransactionRollbackException txRollback) {
            if (numberOfRetriesBefore <= MAX_MESSAGE_DELETION_COUNT) {

                log.warn("unable to delete messages (" + messagesToRemove.size()
                                 + "), due to transaction roll back. Operation will be attempted again", txRollback);
                deleteMessagesFromStore(numberOfRetriesBefore + 1);
            } else {
                throw new AndesException("Unable to delete acked messages, in final attempt " + numberOfRetriesBefore
                                                 + ". This might lead to message duplication.");
            }
        } catch (AndesException ex) {
            log.warn(String.format(
                    "unable to delete messages, probably due to errors in message stores.messages count : %d, "
                            + "operation will be attempted again",
                    messagesToRemove.size()));
            throw ex;
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Creates a {@link SettableFuture} indicating message store became offline.
     */
    @Override
    public void storeNonOperational(HealthAwareStore store, Exception ex) {
        log.info(String.format("Message store became not operational. messages to delete : %d",
                               messagesToRemove.size()));
        messageStoresUnavailable = SettableFuture.create();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Sets a value for {@link SettableFuture} indicating message store became
     * online.
     */
    @Override
    public void storeOperational(HealthAwareStore store) {
        log.info(String.format("Message store became operational. messages to delete : %d",
                               messagesToRemove.size()));
        messageStoresUnavailable.set(false);
    }
}
