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

package org.wso2.carbon.andes.core.store;

import com.gs.collections.impl.list.mutable.primitive.LongArrayList;
import com.gs.collections.impl.map.mutable.primitive.LongObjectHashMap;
import org.wso2.carbon.andes.core.AndesException;
import org.wso2.carbon.andes.core.AndesMessage;
import org.wso2.carbon.andes.core.AndesMessageMetadata;
import org.wso2.carbon.andes.core.AndesMessagePart;
import org.wso2.carbon.andes.core.DeliverableAndesMetadata;
import org.wso2.carbon.andes.core.internal.configuration.util.ConfigurationProperties;
import org.wso2.carbon.andes.core.internal.slot.Slot;
import org.wso2.carbon.andes.core.util.MessageTracer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;

/**
 * Implementation of {@link MessageStore} which observes failures such is
 * connection errors. Any {@link MessageStore} implementation specified in
 * broker.xml will be wrapped by this class.
 */
public class FailureObservingMessageStore implements MessageStore {

    /**
     * {@link MessageStore} specified in broker.xml
     */
    private MessageStore wrappedInstance;

    /**
     * Future referring to a scheduled task which check the connectivity to the
     * store.
     * Used to cancel the periodic task after store becomes operational.
     */
    private ScheduledFuture<?> storeHealthDetectingFuture;

    public FailureObservingMessageStore(MessageStore messageStore) {
        this.wrappedInstance = messageStore;
        this.storeHealthDetectingFuture = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DurableStoreConnection initializeMessageStore(AndesContextStore contextStore,
                                                         ConfigurationProperties connectionProperties)
            throws AndesException {
        try {
            return wrappedInstance.initializeMessageStore(contextStore, connectionProperties);
        } catch (AndesStoreUnavailableException exception) {
            notifyFailures(exception);
            throw exception;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void storeMessagePart(List<AndesMessagePart> partList) throws AndesException {
        try {
            wrappedInstance.storeMessagePart(partList);
        } catch (AndesStoreUnavailableException exception) {
            notifyFailures(exception);
            throw exception;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AndesMessagePart getContent(long messageId, int offsetValue) throws AndesException {
        try {
            return wrappedInstance.getContent(messageId, offsetValue);
        } catch (AndesStoreUnavailableException exception) {
            notifyFailures(exception);
            throw exception;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LongObjectHashMap<List<AndesMessagePart>> getContent(LongArrayList messageIDList) throws AndesException {
        try {
            return wrappedInstance.getContent(messageIDList);
        } catch (AndesStoreUnavailableException exception) {
            notifyFailures(exception);
            throw exception;
        }
    }

    @Override
    public void storeMessages(List<AndesMessage> messageList) throws AndesException {
        try {
            wrappedInstance.storeMessages(messageList);
        } catch (AndesStoreUnavailableException exception) {
            notifyFailures(exception);
            throw exception;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void moveMetadataToQueue(long messageId, String currentQueueName, String targetQueueName)
            throws AndesException {
        try {
            wrappedInstance.moveMetadataToQueue(messageId, currentQueueName, targetQueueName);
        } catch (AndesStoreUnavailableException exception) {
            notifyFailures(exception);
            throw exception;
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void moveMetadataToDLC(long messageId, String dlcQueueName) throws AndesException {
        try {
            wrappedInstance.moveMetadataToDLC(messageId, dlcQueueName);
        } catch (AndesStoreUnavailableException exception) {
            notifyFailures(exception);
            throw exception;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void moveMetadataToDLC(List<AndesMessageMetadata> messages, String dlcQueueName) throws AndesException {
        try {
            wrappedInstance.moveMetadataToDLC(messages, dlcQueueName);
        } catch (AndesStoreUnavailableException exception) {
            notifyFailures(exception);
            throw exception;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateMetadataInformation(String currentQueueName, List<AndesMessageMetadata> metadataList)
            throws AndesException {
        try {
            wrappedInstance.updateMetadataInformation(currentQueueName, metadataList);
        } catch (AndesStoreUnavailableException exception) {
            notifyFailures(exception);
            throw exception;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AndesMessageMetadata getMetadata(long messageId) throws AndesException {
        try {
            return wrappedInstance.getMetadata(messageId);
        } catch (AndesStoreUnavailableException exception) {
            notifyFailures(exception);
            throw exception;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<DeliverableAndesMetadata> getMetadataList(Slot slot, String storageQueueName, long firstMsgId,
                                                          long lastMsgID) throws AndesException {
        try {
            return wrappedInstance.getMetadataList(slot, storageQueueName, firstMsgId, lastMsgID);
        } catch (AndesStoreUnavailableException exception) {
            notifyFailures(exception);
            throw exception;
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getMessageCountForQueueInRange(final String storageQueueName, long firstMessageId, long lastMessageId)
            throws AndesException {
        try {
            return wrappedInstance.getMessageCountForQueueInRange(storageQueueName, firstMessageId, lastMessageId);
        } catch (AndesStoreUnavailableException exception) {
            notifyFailures(exception);
            throw exception;
        }
    }

    /**
     * {@inheritDoc}
     */
    public LongArrayList getNextNMessageIdsFromQueue(final String storageQueueName, long firstMsgId, int count)
            throws AndesException {
        try {
            return wrappedInstance.getNextNMessageIdsFromQueue(storageQueueName, firstMsgId, count);
        } catch (AndesStoreUnavailableException exception) {
            notifyFailures(exception);
            throw exception;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<AndesMessageMetadata> getNextNMessageMetadataFromQueue(String storageQueueName, long firstMsgId,
                                                                       int count) throws AndesException {
        try {
            return wrappedInstance.getNextNMessageMetadataFromQueue(storageQueueName, firstMsgId, count);
        } catch (AndesStoreUnavailableException exception) {
            notifyFailures(exception);
            throw exception;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<AndesMessage> getNextNMessagesFromQueue(String storageQueueName, long firstMsgId, int count, boolean
            getContentFlag) throws AndesException {
        try {
            return wrappedInstance.getNextNMessagesFromQueue(storageQueueName, firstMsgId, count, getContentFlag);
        } catch (AndesStoreUnavailableException exception) {
            notifyFailures(exception);
            throw exception;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<AndesMessageMetadata> getNextNMessageMetadataFromQueue(String storageQueueName, int offset, int
            count) throws AndesException {
        try {
            return wrappedInstance.getNextNMessageMetadataFromQueue(storageQueueName, offset, count);
        } catch (AndesStoreUnavailableException exception) {
            notifyFailures(exception);
            throw exception;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<AndesMessage> getNextNMessagesFromQueue(String storageQueueName, int offset, int count,
                                                        boolean getContentFlag) throws AndesException {
        try {
            return wrappedInstance.getNextNMessagesFromQueue(storageQueueName, offset, count, getContentFlag);
        } catch (AndesStoreUnavailableException exception) {
            notifyFailures(exception);
            throw exception;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<AndesMessageMetadata> getNextNMessageMetadataForQueueFromDLC(String storageQueueName,
                                                                             String dlcQueueName, long firstMsgId,
                                                                             int count) throws AndesException {
        try {
            return wrappedInstance
                    .getNextNMessageMetadataForQueueFromDLC(storageQueueName, dlcQueueName, firstMsgId, count);
        } catch (AndesStoreUnavailableException exception) {
            notifyFailures(exception);
            throw exception;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<AndesMessageMetadata> getNextNMessageMetadataFromDLC(String dlcQueueName, long firstMsgId, int count)
            throws AndesException {
        try {
            return wrappedInstance.getNextNMessageMetadataFromDLC(dlcQueueName, firstMsgId, count);
        } catch (AndesStoreUnavailableException exception) {
            notifyFailures(exception);
            throw exception;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteMessageMetadataFromQueue(String storageQueueName, List<AndesMessageMetadata> messagesToRemove)
            throws AndesException {
        try {
            wrappedInstance.deleteMessageMetadataFromQueue(storageQueueName, messagesToRemove);
        } catch (AndesStoreUnavailableException exception) {
            notifyFailures(exception);
            throw exception;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteMessages(final String storageQueueName, List<AndesMessageMetadata> messagesToRemove)
            throws AndesException {
        try {
            wrappedInstance.deleteMessages(storageQueueName, messagesToRemove);

            //Tracing message activity
            if (MessageTracer.isEnabled()) {
                for (AndesMessageMetadata message : messagesToRemove) {
                    MessageTracer.trace(message.getMessageID(), storageQueueName, MessageTracer.MESSAGE_DELETED);
                }
            }

        } catch (AndesStoreUnavailableException exception) {
            notifyFailures(exception);
            throw exception;
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteDLCMessages(List<AndesMessageMetadata> messagesToRemove) throws AndesException {
        try {
            wrappedInstance.deleteDLCMessages(messagesToRemove);
        } catch (AndesStoreUnavailableException exception) {
            notifyFailures(exception);
            throw exception;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<AndesMessageMetadata> getExpiredMessages(int limit) throws AndesException {
        try {
            return wrappedInstance.getExpiredMessages(limit);
        } catch (AndesStoreUnavailableException exception) {
            notifyFailures(exception);
            throw exception;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteMessagesFromExpiryQueue(LongArrayList messagesToRemove) throws AndesException {
        try {
            wrappedInstance.deleteMessagesFromExpiryQueue(messagesToRemove);
        } catch (AndesStoreUnavailableException exception) {
            notifyFailures(exception);
            throw exception;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addMessageToExpiryQueue(Long messageId, Long expirationTime, boolean isMessageForTopic,
                                        String destination) throws AndesException {
        try {
            wrappedInstance.addMessageToExpiryQueue(messageId, expirationTime, isMessageForTopic, destination);
        } catch (AndesStoreUnavailableException exception) {
            notifyFailures(exception);
            throw exception;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int deleteAllMessageMetadata(String storageQueueName) throws AndesException {
        try {
            return wrappedInstance.deleteAllMessageMetadata(storageQueueName);
        } catch (AndesStoreUnavailableException exception) {
            notifyFailures(exception);
            throw exception;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int clearDLCQueue(String dlcQueueName) throws AndesException {
        try {
            return wrappedInstance.clearDLCQueue(dlcQueueName);
        } catch (AndesStoreUnavailableException exception) {
            notifyFailures(exception);
            throw exception;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LongArrayList getMessageIDsAddressedToQueue(String storageQueueName, Long startMessageID)
            throws AndesException {
        try {
            return wrappedInstance.getMessageIDsAddressedToQueue(storageQueueName, startMessageID);
        } catch (AndesStoreUnavailableException exception) {
            notifyFailures(exception);
            throw exception;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addQueue(String storageQueueName) throws AndesException {
        try {
            wrappedInstance.addQueue(storageQueueName);
        } catch (AndesStoreUnavailableException exception) {
            notifyFailures(exception);
            throw exception;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Integer> getMessageCountForAllQueues(List<String> queueNames) throws AndesException {
        try {
            return wrappedInstance.getMessageCountForAllQueues(queueNames);
        } catch (AndesStoreUnavailableException exception) {
            notifyFailures(exception);
            throw exception;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getMessageCountForQueue(String storageQueueName) throws AndesException {
        try {
            return wrappedInstance.getMessageCountForQueue(storageQueueName);
        } catch (AndesStoreUnavailableException exception) {
            notifyFailures(exception);
            throw exception;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getMessageCountForQueueInDLC(String storageQueueName, String dlcQueueName) throws AndesException {
        try {
            return wrappedInstance.getMessageCountForQueueInDLC(storageQueueName, dlcQueueName);
        } catch (AndesStoreUnavailableException exception) {
            notifyFailures(exception);
            throw exception;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getMessageCountForDLCQueue(String dlcQueueName) throws AndesException {
        try {
            return wrappedInstance.getMessageCountForDLCQueue(dlcQueueName);
        } catch (AndesStoreUnavailableException exception) {
            notifyFailures(exception);
            throw exception;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void resetMessageCounterForQueue(String storageQueueName) throws AndesException {
        try {
            wrappedInstance.resetMessageCounterForQueue(storageQueueName);
        } catch (AndesStoreUnavailableException exception) {
            notifyFailures(exception);
            throw exception;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeQueue(String storageQueueName) throws AndesException {
        try {
            wrappedInstance.removeQueue(storageQueueName);
        } catch (AndesStoreUnavailableException exception) {
            notifyFailures(exception);
            throw exception;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeLocalQueueData(String storageQueueName) {
        wrappedInstance.removeLocalQueueData(storageQueueName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void incrementMessageCountForQueue(String storageQueueName, long incrementBy) throws AndesException {
        try {
            wrappedInstance.incrementMessageCountForQueue(storageQueueName, incrementBy);
        } catch (AndesStoreUnavailableException exception) {
            notifyFailures(exception);
            throw exception;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void decrementMessageCountForQueue(String storageQueueName, long decrementBy) throws AndesException {
        try {
            wrappedInstance.decrementMessageCountForQueue(storageQueueName, decrementBy);
        } catch (AndesStoreUnavailableException exception) {
            notifyFailures(exception);
            throw exception;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void storeRetainedMessages(Map<String, AndesMessage> retainMap) throws AndesException {
        try {
            wrappedInstance.storeRetainedMessages(retainMap);
        } catch (AndesStoreUnavailableException exception) {
            notifyFailures(exception);
            throw exception;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getAllRetainedTopics() throws AndesException {
        try {
            return wrappedInstance.getAllRetainedTopics();
        } catch (AndesStoreUnavailableException exception) {
            notifyFailures(exception);
            throw exception;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<Integer, AndesMessagePart> getRetainedContentParts(long messageID) throws AndesException {
        try {
            return wrappedInstance.getRetainedContentParts(messageID);
        } catch (AndesStoreUnavailableException exception) {
            notifyFailures(exception);
            throw exception;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DeliverableAndesMetadata getRetainedMetadata(String destination) throws AndesException {
        try {
            return wrappedInstance.getRetainedMetadata(destination);
        } catch (AndesStoreUnavailableException exception) {
            notifyFailures(exception);
            throw exception;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        wrappedInstance.close();
        FailureObservingStoreManager.close();
    }

    /**
     * {@inheritDoc}.
     * <p>
     * Alters the behavior where
     * <ol>
     * <li>checks the operational status of the wrapped context store</li>
     * <li>if context store is operational it will cancel the periodic task</li>
     * </ol>
     */
    @Override
    public boolean isOperational(String testString, long testTime) {

        boolean operational = false;
        if (wrappedInstance.isOperational(testString, testTime)) {
            operational = true;
            if (storeHealthDetectingFuture != null) {
                // we have detected that store is operational therefore
                // we don't need to run the periodic task to check weather store is available.
                storeHealthDetectingFuture.cancel(false);
                storeHealthDetectingFuture = null;
            }

        }
        return operational;
    }

    /**
     * A convenient method to notify all {@link StoreHealthListener}s that
     * context store became offline
     *
     * @param e the exception occurred.
     */
    private synchronized void notifyFailures(AndesStoreUnavailableException e) {

        if (storeHealthDetectingFuture == null) {
            // this is the first failure 
            FailureObservingStoreManager.notifyStoreNonOperational(e, wrappedInstance);
            storeHealthDetectingFuture = FailureObservingStoreManager.scheduleHealthCheckTask(this);

        }

    }

}
