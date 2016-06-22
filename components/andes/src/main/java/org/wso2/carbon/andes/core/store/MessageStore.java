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

import java.util.List;
import java.util.Map;

/**
 * Message meta data and content storing related data base types specific logic is abstracted out
 * using this interface.
 */
public interface MessageStore extends HealthAwareStore {

    /**
     * Initialise the MessageStore and returns the DurableStoreConnection used by store
     *
     * @param connectionProperties ConfigurationProperties to be used to create the connection
     * @return DurableStoreConnection object created to make the connection to store
     * @throws org.wso2.carbon.andes.core.AndesException
     */
    DurableStoreConnection initializeMessageStore(AndesContextStore contextStore,
                                                  ConfigurationProperties connectionProperties) throws AndesException;

    /**
     * store a message content chunk set
     *
     * @param partList message content chunk list
     * @throws AndesException
     */
    void storeMessagePart(List<AndesMessagePart> partList) throws AndesException;

    /**
     * read content chunk from store
     *
     * @param messageId   id of the message chunk belongs
     * @param offsetValue chunk offset
     * @return message content part
     * @throws AndesException
     */
    AndesMessagePart getContent(long messageId, int offsetValue) throws AndesException;

    /**
     * Read content for given message metadata list
     *
     * @param messageIDList message id list for the content to be retrieved
     * @return <code>Map<Long, List<AndesMessagePart>></code> Message id and its corresponding message part list
     * @throws AndesException
     */
    LongObjectHashMap<List<AndesMessagePart>> getContent(LongArrayList messageIDList) throws AndesException;

    /**
     * Store messages into database.
     *
     * @param messageList messages to be stored
     */
    void storeMessages(List<AndesMessage> messageList) throws AndesException;

    /**
     * Store a message in a different Queue without altering the meta data.
     *
     * @param messageId        The message Id to move
     * @param currentQueueName The current destination of the message
     * @param targetQueueName  The target destination Queue name
     * @throws AndesException
     */
    void moveMetadataToQueue(long messageId, String currentQueueName, String targetQueueName) throws AndesException;

    /**
     * Method to move a message to dead letter channel
     *
     * @param messageId    the message id to move
     * @param dlcQueueName the dead letter channel queue name for the message to be moved
     * @throws AndesException
     */
    void moveMetadataToDLC(long messageId, String dlcQueueName) throws AndesException;

    /**
     * Method to move a list of messages to a specified dead letter channel
     *
     * @param messages     the list of messages to move
     * @param dlcQueueName the dead letter channel queue name for the message to be moved
     * @throws AndesException
     */
    void moveMetadataToDLC(List<AndesMessageMetadata> messages, String dlcQueueName) throws AndesException;

    /**
     * Update the meta data for the given message with the given information in the AndesMetaData. Update destination
     * and meta data bytes.
     *
     * @param currentQueueName The queue the Meta Data currently in
     * @param metadataList     The updated meta data list.
     * @throws AndesException
     */
    void updateMetadataInformation(String currentQueueName, List<AndesMessageMetadata> metadataList)
            throws AndesException;

    /**
     * read metadata from store
     *
     * @param messageId id of the message
     * @return metadata of the message
     * @throws AndesException
     */
    AndesMessageMetadata getMetadata(long messageId) throws AndesException;

    /**
     * read a metadata list from store specifying a message id range
     *
     * @param storageQueueName name of the queue messages are stored
     * @param firstMsgId       first id of the range
     * @param lastMsgID        last id of the range
     * @return list of metadata
     * @throws AndesException
     */
    List<DeliverableAndesMetadata> getMetadataList(Slot slot, final String storageQueueName, long firstMsgId,
                                                   long lastMsgID) throws AndesException;

    /**
     * Get number of messages in the queue within the message id range
     *
     * @param storageQueueName name of the queue
     * @param firstMessageId   starting message id of the range
     * @param lastMessageId    end message id of the range
     * @return number of messages for the queue within the provided message id range
     * @throws AndesException
     */
    long getMessageCountForQueueInRange(final String storageQueueName, long firstMessageId, long lastMessageId)
            throws AndesException;

    /**
     * read  a metadata list from store specifying a starting message id and a count
     *
     * @param storageQueueName name of the queue
     * @param firstMsgId       first id
     * @param count            how many messages to read
     * @return list of metadata
     * @throws AndesException
     */
    List<AndesMessageMetadata> getNextNMessageMetadataFromQueue(final String storageQueueName, long firstMsgId,
                                                                int count) throws AndesException;

    /**
     * Get message metadata from queue starting from given index up a given message count.
     *
     * @param storageQueueName Name of the queue
     * @param firstMsgId       The starting message ID.
     * @param count            Maximum num of messages to return
     * @return List of {@link AndesMessageMetadata}
     * @throws AndesException
     */
    List<AndesMessage> getNextNMessagesFromQueue(final String storageQueueName, long firstMsgId, int count,
                                                 boolean getContentFlag) throws AndesException;

    /**
     * Get message metadata from queue starting from given index up a given message count.
     *
     * @param storageQueueName Name of the queue
     * @param offset           Starting index
     * @param count            Maximum num of messages to return
     * @return List of {@link AndesMessageMetadata}
     * @throws AndesException
     */
    List<AndesMessageMetadata> getNextNMessageMetadataFromQueue(final String storageQueueName,
                                                                int offset, int count) throws AndesException;

    /**
     * Get {@link AndesMessage}s from queue starting from given index up a given message count.
     *
     * @param storageQueueName Name of the queue
     * @param offset           Starting index
     * @param count            Maximum num of messages to return
     * @param getContentFlag   Whether to fetch content or not.
     * @return A list of messages.
     * @throws AndesException
     */
    List<AndesMessage> getNextNMessagesFromQueue(final String storageQueueName, int offset, int count,
                                                 boolean getContentFlag) throws AndesException;

    /**
     * Read a list of message ids from store specifying a starting message id
     * and a count
     *
     * @param storageQueueName name of the queue
     * @param firstMsgId       first id
     * @param count            how many messages to read
     * @return list of messageIds
     * @throws AndesException
     */
    LongArrayList getNextNMessageIdsFromQueue(final String storageQueueName, long firstMsgId, int count)
            throws AndesException;

    /**
     * Retrieve a metadata list from dead letter channel for a specific queue specifying a starting message id and a
     * count
     *
     * @param storageQueueName name of the storage queue
     * @param dlcQueueName     name of the dead letter channel queue
     * @param firstMsgId       first message id
     * @param count            number of messages to read
     * @return list of metadata
     * @throws AndesException
     */
    List<AndesMessageMetadata> getNextNMessageMetadataForQueueFromDLC(final String storageQueueName,
                                                                      String dlcQueueName, long firstMsgId, int count)
            throws AndesException;

    /**
     * Retrieve a metadata list from dead letter channel specifying a starting message id and a count
     *
     * @param dlcQueueName name of the dead letter channel queue
     * @param firstMsgId   first id
     * @param count        number of messages to read
     * @return list of metadata
     * @throws AndesException
     */
    List<AndesMessageMetadata> getNextNMessageMetadataFromDLC(String dlcQueueName, long firstMsgId, int count)
            throws AndesException;

    /**
     * delete message metadata of messages for a queue
     *
     * @param storageQueueName name of the queue
     * @param messagesToRemove messages to remove
     * @throws AndesException
     */
    void deleteMessageMetadataFromQueue(final String storageQueueName, List<AndesMessageMetadata> messagesToRemove)
            throws AndesException;

    /**
     * Method to delete a set of messages from the database
     * If deleteAllMetaData is set to true, the storageQueue in the metadata table could be cleared at once
     * and the messages destined to that queue, given by the messagesToRemove list could be deleted from the content
     * table
     * Else, the messages in the list 'messagesToRemove' will be deleted
     *
     * @param storageQueueName name of the queue
     * @param messagesToRemove the list of messages to remove
     * @throws AndesException
     */
    void deleteMessages(final String storageQueueName, List<AndesMessageMetadata> messagesToRemove)
            throws AndesException;

    /**
     * Method to delete a list of messages from the dead letter channel.
     *
     * @param messagesToRemove the list of messages to remove
     * @throws AndesException
     */
    void deleteDLCMessages(List<AndesMessageMetadata> messagesToRemove) throws AndesException;

    /**
     * get expired messages from store
     *
     * @param limit max num of messages to read
     * @return AndesRemovableMetadata
     * @throws AndesException
     */
    List<AndesMessageMetadata> getExpiredMessages(int limit) throws AndesException;

    /**
     * delete messages from expiry queue
     *
     * @param messagesToRemove message IDs to remove
     * @throws AndesException
     */
    void deleteMessagesFromExpiryQueue(LongArrayList messagesToRemove) throws AndesException;

    /**
     * add messages to expiry queue
     *
     * @param messageId         id of the message to add
     * @param expirationTime    expiration time
     * @param isMessageForTopic is message addressed to topic
     * @param destination       destination message is addressed to
     * @throws AndesException
     */
    void addMessageToExpiryQueue(Long messageId, Long expirationTime, boolean isMessageForTopic, String destination)
            throws AndesException;

    /**
     * Store level method to remove all metadata addressed to a specific queue.
     *
     * @param storageQueueName name of the queue being purged
     * @return the number of messages that were deleted
     * @throws AndesException
     */
    int deleteAllMessageMetadata(String storageQueueName) throws AndesException;

    /**
     * Store level method to remove all metadata in a dead letter channel
     *
     * @param dlcQueueName name of the queue being purged
     * @throws AndesException
     */
    int clearDLCQueue(String dlcQueueName) throws AndesException;

    /***
     * Get Message ID list addressed to a specific queue.
     *
     * @param storageQueueName name of the queue being purged.
     * @throws AndesException
     */
    LongArrayList getMessageIDsAddressedToQueue(String storageQueueName, Long startMessageID) throws AndesException;

    /**
     * Add message counting entry for queue. queue count is initialised to zero. The counter for
     * created queue can then be incremented and decremented.
     *
     * @param storageQueueName name of queue
     * @see this.removeMessageCounterForQueue this.incrementMessageCountForQueue,
     * this.decrementMessageCountForQueue
     */
    void addQueue(String storageQueueName) throws AndesException;

    /**
     * Get a map of queue names and the message count in the database for each queue in the data store
     *
     * @param queueNames list of queue names of which the message count should be retrieved
     * @return Map of queue names and the message count for each queue
     */
    Map<String, Integer> getMessageCountForAllQueues(List<String> queueNames) throws AndesException;

    /**
     * Get message count of queue
     *
     * @param storageQueueName name of queue
     * @return message count
     */
    long getMessageCountForQueue(String storageQueueName) throws AndesException;

    /**
     * Get message count of queue in DLC
     *
     * @param storageQueueName name of queue
     * @param dlcQueueName     name of the DLC queue
     * @return message count
     * @throws AndesException
     */
    long getMessageCountForQueueInDLC(String storageQueueName, String dlcQueueName) throws AndesException;

    /**
     * Get message count of in a dead letter channel
     *
     * @param dlcQueueName name of queue
     * @return message count
     * @throws AndesException
     */
    long getMessageCountForDLCQueue(String dlcQueueName) throws AndesException;

    /**
     * Store level method to reset the message counter of a given queue to 0.
     *
     * @param storageQueueName name of the queue being purged
     * @throws AndesException
     */
    void resetMessageCounterForQueue(String storageQueueName) throws AndesException;

    /**
     * Remove Message counting entry
     *
     * @param storageQueueName name of the queue actually stored in DB
     */
    void removeQueue(String storageQueueName) throws AndesException;

    /**
     * Remove queue entry from the queue mapping cache
     *
     * @param storageQueueName the name of the queue to be removed
     */
    void removeLocalQueueData(String storageQueueName);

    /**
     * Increment message counter for a queue by a given incrementBy value
     *
     * @param storageQueueName name of the queue actually stored in DB
     * @param incrementBy      increment counter by
     * @throws AndesException
     */
    void incrementMessageCountForQueue(String storageQueueName, long incrementBy) throws AndesException;

    /**
     * Decrement message counter for a queue
     *
     * @param storageQueueName name of the queue actually stored in DB
     * @param decrementBy      decrement counter by
     * @throws AndesException
     */
    void decrementMessageCountForQueue(String storageQueueName, long decrementBy) throws AndesException;

    /**
     * Store retained message list in the message store.
     *
     * @param retainMap Retained messages map
     * @see AndesMessageMetadata#retain
     */
    void storeRetainedMessages(Map<String, AndesMessage> retainMap) throws AndesException;

    /**
     * Return all topic names with retained messages in the database
     *
     * @return Topic list with retained messages
     * @throws AndesException
     * @see AndesMessageMetadata#retain
     */
    List<String> getAllRetainedTopics() throws AndesException;

    /**
     * Get all content parts for the given message ID. The message ID should belong to a
     * existing retained message.
     *
     * @param messageID Message ID of the message
     * @return List of content parts
     * @throws AndesException
     * @see AndesMessageMetadata#retain
     */
    Map<Integer, AndesMessagePart> getRetainedContentParts(long messageID) throws AndesException;

    /**
     * Return retained message metadata for the given destination. Null is returned if
     * no retained message is available for a destination.
     *
     * @param destination Destination/Topic name
     * @return AndesMessageMetadata
     * @throws AndesException
     * @see AndesMessageMetadata#retain
     */
    DeliverableAndesMetadata getRetainedMetadata(String destination) throws AndesException;

    /**
     * close the message store
     */
    void close();

}
