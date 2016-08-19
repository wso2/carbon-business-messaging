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

package org.wso2.carbon.andes.core;

import com.gs.collections.impl.list.mutable.primitive.LongArrayList;
import com.gs.collections.impl.map.mutable.primitive.LongObjectHashMap;
import org.apache.log4j.Logger;
import org.wso2.carbon.andes.core.internal.AndesContext;
import org.wso2.carbon.andes.core.internal.cluster.ClusterResourceHolder;
import org.wso2.carbon.andes.core.internal.cluster.MessageIdGenerator;
import org.wso2.carbon.andes.core.internal.cluster.TimeStampBasedMessageIdGenerator;
import org.wso2.carbon.andes.core.internal.cluster.coordination.ClusterCoordinationHandler;
import org.wso2.carbon.andes.core.internal.cluster.coordination.hazelcast.HazelcastAgent;
import org.wso2.carbon.andes.core.internal.configuration.AndesConfigurationManager;
import org.wso2.carbon.andes.core.internal.configuration.enums.AndesConfiguration;
import org.wso2.carbon.andes.core.internal.slot.ConnectionException;
import org.wso2.carbon.andes.core.internal.slot.Slot;
import org.wso2.carbon.andes.core.internal.slot.SlotCoordinator;
import org.wso2.carbon.andes.core.internal.slot.SlotCoordinatorCluster;
import org.wso2.carbon.andes.core.internal.slot.SlotCoordinatorStandalone;
import org.wso2.carbon.andes.core.internal.slot.SlotDeliveryWorkerManager;
import org.wso2.carbon.andes.core.internal.slot.SlotManagerClusterMode;
import org.wso2.carbon.andes.core.internal.slot.SlotManagerStandalone;
import org.wso2.carbon.andes.core.internal.slot.SlotMessageCounter;
import org.wso2.carbon.andes.core.internal.thrift.MBThriftClient;
import org.wso2.carbon.andes.core.store.MessageStore;
import org.wso2.carbon.andes.core.subscription.LocalSubscription;
import org.wso2.carbon.andes.core.subscription.SubscriptionEngine;
import org.wso2.carbon.andes.core.util.DLCQueueUtils;
import org.wso2.carbon.andes.core.util.MessageTracer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * This class will handle all message related functions of WSO2 Message Broker
 */
public class MessagingEngine {

    /**
     * Logger for MessagingEngine
     */
    private static final Logger log;

    /**
     * Static instance of MessagingEngine
     */
    private static MessagingEngine messagingEngine;

    /**
     * Cluster wide unique message id generator
     */
    private MessageIdGenerator messageIdGenerator;

    /**
     * reference to subscription store
     */
    private SubscriptionEngine subscriptionEngine;

    /**
     * Reference to MessageStore. This holds the messages received by andes
     */
    private MessageStore messageStore;

    /**
     * This listener is primarily added so that messaging engine and communicate a queue purge situation to the cluster.
     * Addition/Deletion of queues are done through AndesContextInformationManager
     */
    private QueueListener queueListener;

    /**
     * Slot coordinator who is responsible of coordinating with the SlotManager
     */
    private SlotCoordinator slotCoordinator;

    /**
     * Maximum length of a content chunk ({@link AndesMessagePart})
     */
    private int maxContentChunkLength;

    /**
     * private constructor for singleton pattern
     */
    private MessagingEngine() {
    }

    /**
     * Recover message
     *
     * @param andesMetadataList message to be recovered
     * @param subToResend       Subscription to send
     * @throws AndesException
     */
    public void recoverMessage(List<DeliverableAndesMetadata> andesMetadataList, LocalSubscription subToResend)
            throws AndesException {
        for (DeliverableAndesMetadata andesMetadata : andesMetadataList) {
            reQueueMessageToSubscriber(andesMetadata, subToResend);
        }
    }

    static {
        log = Logger.getLogger(MessagingEngine.class);
        messagingEngine = new MessagingEngine();
    }

    /**
     * MessagingEngine is used for executing operations related to messages. (Storing,
     * retrieving etc) This works as an API for different transports implemented by MB
     *
     * @return MessagingEngine
     */
    public static MessagingEngine getInstance() {
        return messagingEngine;
    }

    /**
     * Initialises the MessagingEngine with a given durableMessageStore. Message retrieval and
     * storing strategy will be set according to the configurations by calling this.
     *
     * @param messageStore       MessageStore
     * @param subscriptionEngine SubscriptionStore
     * @throws AndesException
     */
    public void initialise(MessageStore messageStore, SubscriptionEngine subscriptionEngine) throws AndesException {

        configureMessageIDGenerator();

        this.messageStore = messageStore;
        this.subscriptionEngine = subscriptionEngine;

        //register listeners for queue changes
        queueListener = new ClusterCoordinationHandler(HazelcastAgent.getInstance());

        maxContentChunkLength = AndesConfigurationManager.
                readValue(AndesConfiguration.PERFORMANCE_TUNING_MAX_CONTENT_CHUNK_SIZE);
        /*
        Initialize the SlotCoordinator
         */
        if (AndesContext.getInstance().isClusteringEnabled()) {
            slotCoordinator = new SlotCoordinatorCluster();
        } else {
            slotCoordinator = new SlotCoordinatorStandalone();
        }
    }

    /**
     * Return the requested chunk of a message's content.
     *
     * @param messageID       Unique ID of the Message
     * @param offsetInMessage The offset of the required chunk in the Message content.
     * @return AndesMessagePart
     * @throws AndesException
     */
    public AndesMessagePart getMessageContentChunk(long messageID, int offsetInMessage) throws AndesException {
        return messageStore.getContent(messageID, offsetInMessage);
    }

    /**
     * Read content for given message metadata list
     *
     * @param messageIdList message id list for the content to be retrieved
     * @return <code>Map<Long, List<AndesMessagePart>></code> Message id and its corresponding message part list
     * @throws AndesException
     */
    public LongObjectHashMap<List<AndesMessagePart>> getContent(LongArrayList messageIdList) throws AndesException {
        return messageStore.getContent(messageIdList);

    }

    /**
     * Persist received messages. Implemented {@link MessageStore} will be used to
     * persist the messages
     *
     * @param messageList List of {@link AndesMessage} to persist
     * @throws AndesException
     */
    public void messagesReceived(List<AndesMessage> messageList) throws AndesException {
        messageStore.storeMessages(messageList);
    }

    /**
     * Get a single metadata object
     *
     * @param messageID id of the message
     * @return AndesMessageMetadata
     * @throws AndesException
     */
    public AndesMessageMetadata getMessageMetaData(long messageID) throws AndesException {
        return messageStore.getMetadata(messageID);
    }

    /**
     * Message is rejected
     *
     * @param andesMetadata message that is rejected.
     * @param channelID     ID of the channel reject received
     * @throws AndesException
     */
    public void messageRejected(DeliverableAndesMetadata andesMetadata, UUID channelID) throws AndesException {
        andesMetadata.markAsNackedByClient(channelID);
        LocalSubscription subToResend = subscriptionEngine.getLocalSubscriptionForChannelId(channelID);
        if (subToResend != null) {
            subToResend.msgRejectReceived(andesMetadata.getMessageId());
            reQueueMessageToSubscriber(andesMetadata, subToResend);
        } else {
            log.warn("Cannot handle reject. Subscription not found for channel " + channelID + "Dropping message id= "
                             + andesMetadata.getMessageId());
            andesMetadata.markDeliveredChannelAsClosed(channelID);
        }
        //Tracing message activity
        MessageTracer.trace(andesMetadata, MessageTracer.MESSAGE_REJECTED);
    }

    /**
     * Schedule message for subscription. Slot Returned and slot removed messages
     * are not scheduled
     *
     * @param messageMetadata message to be scheduled
     * @param subscription    subscription to send
     * @throws AndesException
     */
    public void reQueueMessageToSubscriber(DeliverableAndesMetadata messageMetadata, LocalSubscription subscription)
            throws AndesException {

        if (!messageMetadata.isOKToDispose()) {
            MessageFlusher.getInstance().scheduleMessageForSubscription(subscription, messageMetadata);
            //Tracing message activity
            MessageTracer.trace(messageMetadata, MessageTracer.MESSAGE_REQUEUED_SUBSCRIBER);
        }
    }

    /**
     * Re-queue message to andes core. This message will be delivered to
     * any eligible subscriber to receive later. This also check message status before
     * re-schedule
     *
     * @param messageMetadata message to reschedule
     * @param destinationType the destination type of the messages
     * @throws AndesException in case of an error
     */
    public void reQueueMessage(DeliverableAndesMetadata messageMetadata, DestinationType destinationType)
            throws AndesException {
        if (!messageMetadata.isOKToDispose()) {
            MessageFlusher.getInstance().reQueueMessage(messageMetadata, destinationType);
            //Tracing Message
            MessageTracer.trace(messageMetadata.getMessageId(), messageMetadata.getDestination(),
                                MessageTracer.MESSAGE_REQUEUED_BUFFER);
        }
    }

    /**
     * Move the messages meta data in the given message to the Dead Letter Channel and
     * remove those meta data from the original queue.
     *
     * @param messageToRemove      Message to be removed
     * @param destinationQueueName The original destination queue of the message
     * @throws AndesException
     */
    public void moveMessageToDeadLetterChannel(DeliverableAndesMetadata messageToRemove, String destinationQueueName)
            throws AndesException {
        String deadLetterQueueName = DLCQueueUtils.identifyTenantInformationAndGenerateDLCString(destinationQueueName);

        messageStore.moveMetadataToDLC(messageToRemove.getMessageId(), deadLetterQueueName);

        // Increment count by 1 in DLC and decrement by 1 in original queue

        messageToRemove.markAsDLCMessage();
        messageToRemove.getSlot().decrementPendingMessageCount();

        //Tracing message activity
        MessageTracer.trace(messageToRemove.getMessageId(), destinationQueueName, MessageTracer.MOVED_TO_DLC);
    }

    /**
     * Remove in-memory message buffers of the destination matching to given destination in this
     * node. This is called from the Hazelcast Agent when it receives a queue purged event.
     *
     * @param destination     queue or topic name (subscribed routing key) whose messages should be removed
     * @param protocolType    The protocol which the destination belongs to
     * @param destinationType The type of the destination
     * @return number of messages removed
     * @throws AndesException
     */
    public int clearMessagesFromQueueInMemory(String destination, Long purgedTimestamp, ProtocolType protocolType,
                                              DestinationType destinationType) throws AndesException {

        MessageFlusher messageFlusher = MessageFlusher.getInstance();
        MessageDeliveryInfo messageDeliveryInfo = messageFlusher
                .getMessageDeliveryInfo(destination, protocolType, destinationType);
        messageDeliveryInfo.setLastPurgedTimestamp(purgedTimestamp);
        return messageDeliveryInfo.clearReadButUndeliveredMessages();
    }

    /**
     * This is the andes-specific purge method and can be called from AMQPBridge,
     * Remove messages of the queue matching to given destination queue (h2 / mysql etc. )
     *
     * @param destination     queue or topic name (subscribed routing key) whose messages should be removed
     * @param ownerName       The user who initiated the purge request
     * @param protocolType    The protocol which the destination to purge belongs to
     * @param destinationType The destination type which the destination to purge belongs to
     * @return number of messages removed (in memory message count may not be 100% accurate
     * since we cannot guarantee that we caught all messages in delivery threads.)
     * @throws AndesException
     */
    public int purgeMessages(String destination, String ownerName, ProtocolType protocolType,
                             DestinationType destinationType) throws AndesException {

        // The timestamp is recorded to track messages that came before the purge event.
        Long purgedTimestamp = System.currentTimeMillis();
        String nodeID = ClusterResourceHolder.getInstance().getClusterManager().getMyNodeID();
        String storageQueueName = AndesUtils.getStorageQueueForDestination(destination, nodeID, destinationType);

        try {
            // Clear all slots assigned to the Queue. This should ideally stop any messages being buffered during the
            // purge.
            // This call clears all slot associations for the queue in all nodes. (could take time)
            //Slot relations should be cleared through the storage queue name
            slotCoordinator.clearAllActiveSlotRelationsToQueue(storageQueueName);
        } catch (ConnectionException e) {
            String message = "Error while establishing a connection with the thrift server to delete active slots " +
                    "assigned for the purged queue : " + destination;
            log.error(message);
            throw new AndesException(message, e);
        }

        // Clear in memory messages of self (node)
        clearMessagesFromQueueInMemory(destination, purgedTimestamp, protocolType, destinationType);

        //Notify the cluster if queues
        if (DestinationType.QUEUE == destinationType) {
            AndesQueue purgedQueue = new AndesQueue(destination, ownerName, false, true, protocolType, destinationType);
            purgedQueue.setLastPurgedTimestamp(purgedTimestamp);

            queueListener.handleLocalQueuesChanged(purgedQueue, QueueListener.QueueEvent.PURGED);
        }

        // Clear any and all message references addressed to the queue from the persistent store.
        // We can measure the message count in store, but cannot exactly infer the message count
        // in memory within all nodes at the time of purging. (Adding that could unnecessarily
        // block critical pub sub flows.)
        // queues destination = storage queue. But for topics it is different
        int purgedNumOfMessages = purgeQueueFromStore(storageQueueName);
        log.info("Purged messages of destination " + destination);
        return purgedNumOfMessages;
    }

    /**
     * Clear all references to all message metadata / content addressed to a specific queue. Used when purging.
     *
     * @param storageQueueName name of the queue, could be a storage queue or a dlc queue
     * @throws AndesException
     */
    public int purgeQueueFromStore(String storageQueueName) throws AndesException {

        try {
            int deletedMessageCount;
            if (!(DLCQueueUtils.isDeadLetterQueue(storageQueueName))) {
                // delete all messages for the queue
                deletedMessageCount = messageStore.deleteAllMessageMetadata(storageQueueName);
            } else {
                //delete all the messages in dlc
                deletedMessageCount = messageStore.clearDLCQueue(storageQueueName);
            }
            return deletedMessageCount;

        } catch (AndesException e) {
            // This will be a store-specific error.
            throw new AndesException("Error occurred when purging queue from store : " + storageQueueName, e);
        }
    }

    /**
     * Delete messages from store. No message state changes are involved here.
     *
     * @param messagesToRemove list of messages to remove
     * @throws AndesException
     */
    public void deleteMessages(Collection<AndesMessageMetadata> messagesToRemove) throws AndesException {
        Map<String, List<AndesMessageMetadata>> storageSeparatedMessages = new HashMap<>();

        for (AndesMessageMetadata message : messagesToRemove) {
            List<AndesMessageMetadata> messagesOfStorageQueue = storageSeparatedMessages
                    .get(message.getStorageDestination());
            if (null == messagesOfStorageQueue) {
                messagesOfStorageQueue = new ArrayList<>();
            }
            messagesOfStorageQueue.add(message);
            storageSeparatedMessages.put(message.getStorageDestination(), messagesOfStorageQueue);
        }

        //delete message content along with metadata
        for (Map.Entry<String, List<AndesMessageMetadata>> entry : storageSeparatedMessages.entrySet()) {
            messageStore.deleteMessages(entry.getKey(), entry.getValue());
        }

        //TODO:message can be in delivery path. If so we need to decrement slot message count
    }

    /**
     * Delete a list of messages from the dead letter channel.
     *
     * @param messagesToRemove list of messages to remove
     * @throws AndesException
     */
    public void deleteDLCMessages(List<AndesMessageMetadata> messagesToRemove) throws AndesException {
        messageStore.deleteDLCMessages(messagesToRemove);
    }

    /**
     * Delete messages from store. Optionally move to dead letter channel.  Delete
     * call is blocking and then slot message count is dropped in order. Message state
     * is updated.
     *
     * @param messagesToRemove List of messages to remove
     * @throws AndesException
     */
    public void deleteMessages(List<DeliverableAndesMetadata> messagesToRemove) throws AndesException {

        Map<String, List<AndesMessageMetadata>> storageSeparatedMessages = new HashMap<>();

        for (DeliverableAndesMetadata message : messagesToRemove) {
            List<AndesMessageMetadata> messagesOfStorageQueue = storageSeparatedMessages
                    .get(message.getStorageDestination());
            if (null == messagesOfStorageQueue) {
                messagesOfStorageQueue = new ArrayList<>();
            }
            messagesOfStorageQueue.add(message);
            storageSeparatedMessages.put(message.getStorageDestination(), messagesOfStorageQueue);
        }

        //delete message content along with metadata
        for (Map.Entry<String, List<AndesMessageMetadata>> entry : storageSeparatedMessages.entrySet()) {
            messageStore.deleteMessages(entry.getKey(), entry.getValue());
        }
        //mark messages as deleted
        messagesToRemove.forEach(DeliverableAndesMetadata::markAsDeletedMessage);

    }

    public void moveMessageToDeadLetterChannel(List<DeliverableAndesMetadata> messagesToMove) throws AndesException {
        Map<String, List<AndesMessageMetadata>> storageSeparatedMessages = new HashMap<>();
        for (DeliverableAndesMetadata message : messagesToMove) {
            List<AndesMessageMetadata> messagesOfStorageQueue = storageSeparatedMessages
                    .get(message.getStorageDestination());
            if (null == messagesOfStorageQueue) {
                messagesOfStorageQueue = new ArrayList<>();
            }
            messagesOfStorageQueue.add(message);
            storageSeparatedMessages.put(message.getStorageDestination(), messagesOfStorageQueue);
        }

        for (Map.Entry<String, List<AndesMessageMetadata>> entry : storageSeparatedMessages.entrySet()) {
            //move messages to dead letter channel
            String dlcQueueName = DLCQueueUtils.identifyTenantInformationAndGenerateDLCString(entry.getKey());
            messageStore.moveMetadataToDLC(entry.getValue(), dlcQueueName);
        }

        //mark the messages as DLC messages
        for (DeliverableAndesMetadata message : messagesToMove) {
            message.markAsDLCMessage();
            message.getSlot().decrementPendingMessageCount();
        }
    }

    public void moveMessageToDeadLetterChannel(Collection<AndesMessageMetadata> messagesToMove) throws AndesException {
        Map<String, List<AndesMessageMetadata>> storageSeparatedMessages = new HashMap<>();

        for (AndesMessageMetadata message : messagesToMove) {
            List<AndesMessageMetadata> messagesOfStorageQueue = storageSeparatedMessages
                    .get(message.getStorageDestination());
            if (null == messagesOfStorageQueue) {
                messagesOfStorageQueue = new ArrayList<>();
            }
            messagesOfStorageQueue.add(message);
            storageSeparatedMessages.put(message.getStorageDestination(), messagesOfStorageQueue);
        }
        for (Map.Entry<String, List<AndesMessageMetadata>> entry : storageSeparatedMessages.entrySet()) {
            //move messages to dead letter channel
            String dlcQueueName = DLCQueueUtils.identifyTenantInformationAndGenerateDLCString(entry.getKey());
            messageStore.moveMetadataToDLC(entry.getValue(), dlcQueueName);
        }

        //TODO:message can be in delivery path. If so we need to decrement slot message count
    }

    /**
     * Get content chunk from store
     *
     * @param messageId   id of the message
     * @param offsetValue chunk id
     * @return message content
     * @throws AndesException
     */
    public AndesMessagePart getContent(long messageId, int offsetValue) throws AndesException {
        return messageStore.getContent(messageId, offsetValue);
    }

    /**
     * Get a map of queue names and the message count for each queue from the message store.
     *
     * @param queueNames list of queue names of which the message count should be retrieved
     * @return Map of queue names and the message count for each queue
     */
    public Map<String, Integer> getMessageCountForAllQueues(List<String> queueNames) throws AndesException {
        return messageStore.getMessageCountForAllQueues(queueNames);
    }

    /**
     * Get message count for queue
     *
     * @param queueName name of the queue
     * @return message count of the queue
     * @throws AndesException
     */
    public long getMessageCountOfQueue(String queueName) throws AndesException {
        return messageStore.getMessageCountForQueue(queueName);
    }

    /**
     * Get number of messages in the queue within the message id range
     *
     * @param storageQueueName name of the queue
     * @param firstMessageId   starting message id of the range
     * @param lastMessageId    end message id of the range
     * @return number of messages for the queue within the provided message id range
     * @throws AndesException
     */
    public long getMessageCountForQueueInRange(final String storageQueueName, long firstMessageId, long lastMessageId)
            throws AndesException {
        return messageStore.getMessageCountForQueueInRange(storageQueueName, firstMessageId, lastMessageId);
    }

    /**
     * Get message count in DLC for a specific queue.
     *
     * @param queueName    name of the storage queue
     * @param dlcQueueName name of the dlc queue
     * @return message count of the queue
     * @throws AndesException
     */
    public long getMessageCountInDLCForQueue(String queueName, String dlcQueueName) throws AndesException {
        return messageStore.getMessageCountForQueueInDLC(queueName, dlcQueueName);
    }

    /**
     * Get message count in DLC.
     *
     * @param dlcQueueName name of the dlc queue
     * @return message count of the queue
     * @throws AndesException
     */
    public long getMessageCountInDLC(String dlcQueueName) throws AndesException {
        return messageStore.getMessageCountForDLCQueue(dlcQueueName);
    }

    /**
     * Get message metadata from queue between two message id values
     *
     * @param queueName  queue name
     * @param firstMsgId id of the starting id
     * @param lastMsgID  id of the last id
     * @return List of message metadata
     * @throws AndesException
     */
    public List<DeliverableAndesMetadata> getMetadataList(final Slot slot, final String queueName, long firstMsgId,
                                                          long lastMsgID) throws AndesException {
        return messageStore.getMetadataList(slot, queueName, firstMsgId, lastMsgID);
    }

    /**
     * Get message metadata from queue starting from given id up a given
     * message count
     *
     * @param queueName  name of the queue
     * @param firstMsgId id of the starting id
     * @param count      maximum num of messages to return
     * @return List of message metadata
     * @throws AndesException
     */
    public List<AndesMessageMetadata> getNextNMessageMetadataFromQueue(final String queueName, long firstMsgId,
                                                                       int count) throws AndesException {
        return messageStore.getNextNMessageMetadataFromQueue(queueName, firstMsgId, count);
    }

    /**
     * Get {@link AndesMessage}s from queue starting from given id up a given message count
     *
     * @param queueName      Name of the queue
     * @param firstMsgId     ID of the starting id
     * @param count          Maximum number of messages to return
     * @param getContentFlag Whether to fetch content or not
     * @return List of messages.
     * @throws AndesException
     */
    public List<AndesMessage> getNextNMessagesFromQueue(final String queueName, long firstMsgId, int count,
                                                        boolean getContentFlag) throws AndesException {
        return messageStore.getNextNMessagesFromQueue(queueName, firstMsgId, count, getContentFlag);
    }

    /**
     * Get message metadata from queue starting from given index up a given message count.
     *
     * @param queueName Name of the queue
     * @param offset    Starting index
     * @param count     Maximum num of messages to return
     * @return List of {@link AndesMessageMetadata}
     * @throws AndesException
     */
    public List<AndesMessageMetadata> getNextNMessageMetadataFromQueue(final String queueName, int offset, int count)
            throws AndesException {
        return messageStore.getNextNMessageMetadataFromQueue(queueName, offset, count);
    }

    /**
     * Get {@link AndesMessage}s from queue starting from given index up a given message count.
     *
     * @param queueName      Name of the queue
     * @param offset         Starting index
     * @param count          Maximum num of messages to return
     * @param getContentFlag Whether to fetch content or not
     * @return List of {@link AndesMessage}
     * @throws AndesException
     */
    public List<AndesMessage> getNextNMessagesFromQueue(final String queueName, int offset, int count,
                                                        boolean getContentFlag) throws AndesException {
        return messageStore.getNextNMessagesFromQueue(queueName, offset, count, getContentFlag);
    }

    /**
     * Get message metadata from queue starting from given id up a given message count.
     *
     * @param queueName    name of the queue
     * @param dlcQueueName name of the dead letter channel queue
     * @param firstMsgId   id of the starting id
     * @param count        maximum num of messages to return
     * @return List of message metadata
     * @throws AndesException
     */
    public List<AndesMessageMetadata> getNextNMessageMetadataInDLCForQueue(final String queueName,
                                                                           final String dlcQueueName, long firstMsgId,
                                                                           int count) throws AndesException {
        return messageStore.getNextNMessageMetadataForQueueFromDLC(queueName, dlcQueueName, firstMsgId, count);
    }

    /**
     * Get message metadata from queue starting from given id up a given message count
     *
     * @param dlcQueueName name of the dead letter channel queue name
     * @param firstMsgId   id of the starting id
     * @param count        maximum num of messages to return
     * @return List of message metadata
     * @throws AndesException
     */
    public List<AndesMessageMetadata> getNextNMessageMetadataFromDLC(final String dlcQueueName, long firstMsgId,
                                                                     int count) throws AndesException {
        return messageStore.getNextNMessageMetadataFromDLC(dlcQueueName, firstMsgId, count);
    }

    /**
     * Get expired but not yet deleted messages from message store
     *
     * @param limit upper bound for number of messages to be returned
     * @return AndesRemovableMetadata
     * @throws AndesException
     */
    public List<AndesMessageMetadata> getExpiredMessages(int limit) throws AndesException {
        return messageStore.getExpiredMessages(limit);
    }

    /**
     * Update the meta data for the given message with the given information in the AndesMetaData. Update destination
     * and meta data bytes.
     *
     * @param currentQueueName The queue the Meta Data currently in
     * @param metadataList     The updated meta data list.
     * @throws AndesException
     */
    public void updateMetadataInformation(String currentQueueName, List<AndesMessageMetadata> metadataList)
            throws AndesException {
        messageStore.updateMetadataInformation(currentQueueName, metadataList);
    }

    /**
     * Generate a new message ID. The return id will be always unique
     * even for different message broker nodes
     *
     * @return id generated
     */
    public long generateUniqueId() {
        long messageId = messageIdGenerator.getNextId();
        if (log.isTraceEnabled()) {
            log.trace("MessageID generated: " + messageId);
        }
        return messageId;
    }

    private void configureMessageIDGenerator() {
        // Configure message ID generator
        String idGeneratorImpl = AndesConfigurationManager.readValue(AndesConfiguration.PERSISTENCE_ID_GENERATOR);
        if (idGeneratorImpl != null && !"".equals(idGeneratorImpl)) {
            try {
                Class clz = Class.forName(idGeneratorImpl);

                Object o = clz.newInstance();
                messageIdGenerator = (MessageIdGenerator) o;
            } catch (Exception e) {
                log.error("Error while loading Message id generator implementation : " +
                                  idGeneratorImpl +
                                  " adding TimeStamp based implementation as the default", e);
                messageIdGenerator = new TimeStampBasedMessageIdGenerator();
            }
        } else {
            messageIdGenerator = new TimeStampBasedMessageIdGenerator();
        }
    }

    /**
     * Start message delivery. Start threads. If not created create.
     */
    public void startMessageDelivery() {
        log.info("Starting SlotDelivery Workers.");
        //Start all slotDeliveryWorkers
        SlotDeliveryWorkerManager.getInstance().startAllSlotDeliveryWorkers();
        //Start thrift reconnecting thread if started
        if (MBThriftClient.isReconnectingStarted()) {
            MBThriftClient.setReconnectingFlag(true);
        }
        log.info("Start Disruptor writing messages to store.");
    }

    /**
     * Stop message delivery threads
     */
    public void stopMessageDelivery() {

        log.info("Stopping SlotDelivery Worker.");
        //Stop all slotDeliveryWorkers
        SlotDeliveryWorkerManager.getInstance().stopSlotDeliveryWorkers();
        //Stop thrift reconnecting thread if started
        if (MBThriftClient.isReconnectingStarted()) {
            MBThriftClient.setReconnectingFlag(false);
        }
        SlotMessageCounter.getInstance().stop();
        //Stop delivery disruptor
        MessageFlusher.getInstance().getFlusherExecutor().stop();
    }

    /**
     * Properly shutdown all messaging related operations / tasks
     *
     * @throws InterruptedException
     */
    public void close() throws InterruptedException {

        stopMessageDelivery();
        stopMessageExpirationWorker();

        completePendingStoreOperations();
    }

    public void completePendingStoreOperations() {
        messageStore.close();
    }

    /**
     * Start Checking for Expired Messages (JMS Expiration)
     */
    public void startMessageExpirationWorker() {

        MessageExpirationWorker messageExpirationWorker = ClusterResourceHolder.getInstance()
                .getMessageExpirationWorker();

        if (messageExpirationWorker == null) {
            ClusterResourceHolder.getInstance().setMessageExpirationWorker(new MessageExpirationWorker());

        } else {
            if (!messageExpirationWorker.isWorking()) {
                messageExpirationWorker.startWorking();
            }
        }
    }

    /**
     * Stop Checking for Expired Messages (JMS Expiration)
     */
    public void stopMessageExpirationWorker() {

        MessageExpirationWorker messageExpirationWorker = ClusterResourceHolder.getInstance()
                .getMessageExpirationWorker();

        if (messageExpirationWorker != null && messageExpirationWorker.isWorking()) {
            messageExpirationWorker.stopWorking();
        }
    }

    public SlotCoordinator getSlotCoordinator() {
        return slotCoordinator;
    }

    /**
     * Store retained messages in the message store.
     *
     * @param retainMap Retained message Map
     * @see AndesMessageMetadata#retain
     */
    public void storeRetainedMessages(Map<String, AndesMessage> retainMap) throws AndesException {
        messageStore.storeRetainedMessages(retainMap);
    }

    /**
     * Return matching retained message metadata for the given subscription topic name. An empty list is returned if no
     * match is found.
     *
     * @param subscriptionTopicName Destination string provided by the subscriber
     * @return AndesMessageMetadata
     * @throws AndesException
     */
    public List<DeliverableAndesMetadata> getRetainedMessageByTopic(String subscriptionTopicName)
            throws AndesException {
        List<DeliverableAndesMetadata> retainMessageList = new ArrayList<>();
        List<String> topicList = messageStore.getAllRetainedTopics();

        for (String topicName : topicList) {
            if (TopicParserUtil.isMatching(topicName, subscriptionTopicName)) {
                retainMessageList.add(messageStore.getRetainedMetadata(topicName));
            }
        }

        return retainMessageList;
    }

    /**
     * Return message content for the given retained message metadata.
     *
     * @param metadata Message metadata
     * @return AndesContent
     * @throws AndesException
     */
    public AndesContent getRetainedMessageContent(AndesMessageMetadata metadata) throws AndesException {
        long messageID = metadata.getMessageId();
        int contentSize = metadata.getMessageContentLength();

        Map<Integer, AndesMessagePart> retainedContentParts = messageStore.getRetainedContentParts(messageID);

        return new RetainedContent(retainedContentParts, contentSize, messageID, maxContentChunkLength);
    }

    /**
     * Return last assign message id of slot for given queue
     *
     * @param queueName name of destination queue
     * @return last assign message id
     */
    public long getLastAssignedSlotMessageId(String queueName) throws AndesException {
        long lastMessageId = 0;
        long messageIdDifference = 1024 * 256 * 5000;
        Long lastAssignedSlotMessageId;
        if (ClusterResourceHolder.getInstance().getClusterManager().isClusteringEnabled()) {
            lastAssignedSlotMessageId = SlotManagerClusterMode.getInstance()
                    .getLastAssignedSlotMessageIdInClusterMode(queueName);
        } else {
            lastAssignedSlotMessageId = SlotManagerStandalone.getInstance()
                    .getLastAssignedSlotMessageIdInStandaloneMode(queueName);
        }
        if (lastAssignedSlotMessageId != null) {
            lastMessageId = lastAssignedSlotMessageId - messageIdDifference;
        }
        return lastMessageId;
    }
}
