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
import org.wso2.carbon.andes.core.AndesException;
import org.wso2.carbon.andes.core.AndesMessage;
import org.wso2.carbon.andes.core.MessagingEngine;
import org.wso2.carbon.andes.core.internal.disruptor.BatchEventHandler;
import org.wso2.carbon.andes.core.store.AndesBatchUpdateException;
import org.wso2.carbon.andes.core.store.AndesTransactionRollbackException;
import org.wso2.carbon.andes.core.store.FailureObservingStoreManager;
import org.wso2.carbon.andes.core.store.HealthAwareStore;
import org.wso2.carbon.andes.core.store.StoreHealthListener;
import org.wso2.carbon.andes.core.util.MessageTracer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Writes messages in Disruptor ring buffer to message store in batches.
 */
public class MessageWriter implements BatchEventHandler, StoreHealthListener {

    private static Log log = LogFactory.getLog(MessageWriter.class);

    /**
     * List of messages to write to message store.
     */
    private final List<AndesMessage> currentMessageList;

    /**
     * If the message store became non-operational ( due to errors) when persisting bunch of messages
     * (held in {@link MessageWriter}{@link #currentMessageList} ) those will be move to this list.
     * Once message store becomes operational we will save these messages before saving new message arrived
     */
    private final List<AndesMessage> previouslyFailedMessageList;

    /**
     * Temporary storage for retain messages
     */
    private final Map<String, AndesMessage> retainMap;

    /**
     * Indicates and provides a barrier if messages stores become offline.
     * marked as volatile since this value could be set from a different thread
     * (other than those of disruptor)
     */
    private volatile SettableFuture<Boolean> messageStoresUnavailable;

    /**
     * Reference to messaging engine. This is used to store messages
     */
    private final MessagingEngine messagingEngine;

    public MessageWriter(MessagingEngine messagingEngine, int messageBatchSize) {
        this.messagingEngine = messagingEngine;
        /*
         * For topics the size may be more than messageBatchSize since inbound
         * event might contain more than one message
         * But this is valid for queues.
         */
        currentMessageList = new ArrayList<>(messageBatchSize);
        previouslyFailedMessageList = new ArrayList<>(messageBatchSize); // init in the same capacity
        retainMap = new HashMap<>();
        messageStoresUnavailable = null;
        FailureObservingStoreManager.registerStoreHealthListener(this);
    }

    @Override
    public void onEvent(final List<InboundEventContainer> eventList) throws Exception {

        // For topics there may be multiple messages in one event.
        for (InboundEventContainer event : eventList) {
            currentMessageList.addAll(event.getMessageList());

            if (null != event.retainMessage) {
                retainMap.put(event.retainMessage.getMetadata().getDestination(), event.retainMessage);
            }
        }

        if (messageStoresUnavailable != null) {
            log.info("message store has become unavailable therefore waiting until store becomes available");
            messageStoresUnavailable.get(); // stop processing until message stores are available.
            log.info("message store has become available therefore resuming work");
            messageStoresUnavailable = null;
        }

        // Try inserting message batch that failed before.
        if (!previouslyFailedMessageList.isEmpty()) {

            try {
                messagingEngine.messagesReceived(previouslyFailedMessageList);

            } catch (AndesException ex) {
                log.error("errors encountered while persisting previously failed messages batch, "
                                  + " this incident will result messages being lost", ex);

            }

            previouslyFailedMessageList.clear();
        }

        try {
            messagingEngine.messagesReceived(currentMessageList);

            if (!retainMap.isEmpty()) {
                messagingEngine.storeRetainedMessages(retainMap);
            }

            if (log.isDebugEnabled()) {
                log.debug(currentMessageList.size() + " messages received from disruptor.");
            }

            if (MessageTracer.isEnabled()) {
                for (AndesMessage message : currentMessageList) {
                    //Tracing message
                    MessageTracer.trace(message, MessageTracer.CONTENT_WRITTEN_TO_DB);
                }
            }

            if (log.isTraceEnabled()) {
                StringBuilder messageIDsString = new StringBuilder();
                for (AndesMessage message : currentMessageList) {
                    messageIDsString.append(message.getMetadata().getMessageID()).append(" , ");
                }
                log.trace(currentMessageList.size() + " messages written : " + messageIDsString);
            }

            // clear the messages
            currentMessageList.clear();
            // clear retained messages map
            retainMap.clear();

        } catch (AndesBatchUpdateException batchInsertEx) {

            log.error(String.format("Unable to store messages, probably due to errors in message stores."
                                            + "success inserts: %d, failed inserts: %d",
                                    batchInsertEx.getSuccessfullBatches().size(),
                                    batchInsertEx.getFailedBatches().size()), batchInsertEx);
            // This happens when message store becomes unavailable and came back online after a while.
            // Previous message insert batch was successful in Database but connection was dropped before
            // client (= MB) got to know about it ( aka. commit worked).
            // Now message writer goes and inserts same batch again -> results in failures in batch update.
            // Therefore here we remove conflicting message parts (which are probably already in the database).
            //currentMessageList.removeAll(batchInsertEx.getFailedInserts());
            handleStoreFailure();
            throw batchInsertEx;
        } catch (AndesTransactionRollbackException transRollbackEx) {
            // Transaction failed therefore we will re-attempt this batch with next batch insertion.
            log.warn("Unable to store messages, since transaction rollback. " +
                             "opertation will be reattempted. messages count : " +
                             currentMessageList.size());
            handleStoreFailure();
            throw transRollbackEx;
        } catch (Exception ex) {
            log.warn("Unable to store messages, due to errors in message stores. " +
                             "opertation will be reattempted. messages count : " +
                             currentMessageList.size());
            handleStoreFailure();
            throw ex;
        }
    }

    /**
     * Move the messages to previouslyFailedMessageList and clear currentMessageList and retainMap
     */
    private void handleStoreFailure() {
        previouslyFailedMessageList.addAll(currentMessageList);
        currentMessageList.clear();
    }

    /**
     * {@inheritDoc}
     * TODO: Name Change
     * <p> Creates a {@link SettableFuture} indicating message store became offline.
     */
    @Override
    public void storeNonOperational(HealthAwareStore store, Exception ex) {
        log.info(String.format("Message store became nonoperational. messages to store : %d",
                               currentMessageList.size()));
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
        log.info(String.format("Message store became operational. messages to store : %d", currentMessageList.size()));
        messageStoresUnavailable.set(false);

    }
}
