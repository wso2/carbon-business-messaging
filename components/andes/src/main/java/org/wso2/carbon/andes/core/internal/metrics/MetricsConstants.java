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

package org.wso2.carbon.andes.core.internal.metrics;

/**
 * Will maintain the list of metrics
 */
public class MetricsConstants {

    /**
     * Prefix will be used to distinguish MB specific metrics
     */
    public static final String PREFIX = "org.wso2.mb.";

    /*DB OPERATIONS*/
    /**
     * Time taken for database write operations
     */
    public static final String DB_WRITE = PREFIX + "database.write";

    /**
     * Time taken for database read operations
     */
    public static final String DB_READ = PREFIX + "database.read";

    /**
     * Add message content to the message store
     */
    public static final String ADD_MESSAGE_PART = PREFIX + "store.messagePart.add";

    /**
     * Add message meta data list to the database
     */
    public static final String ADD_META_DATA_LIST = PREFIX + "store.metadataList.add";
    /**
     * Add message meta data to database
     */
    public static final String ADD_META_DATA = PREFIX + "store.metadata.add";

    /**
     * Add meta data to queue list
     */
    public static final String ADD_META_DATA_TO_QUEUE_LIST = PREFIX + "store.metadataToQueueList.add";
    /**
     * Add meta data to queue
     */
    public static final String ADD_META_DATA_TO_QUEUE = PREFIX + "store.metadataToQueue.add";
    /**
     * Add meta data as a batch
     */
    public static final String ADD_META_DATA_TO_BATCH = PREFIX + "store.metadataToBatch.add";
    /**
     * Get message content as batch
     */
    public static final String GET_CONTENT_BATCH = PREFIX + "store.contentBatch.get";
    /**
     * Get message meta data
     */
    public static final String GET_META_DATA = PREFIX + "store.metadata.get";
    /**
     * Get message meta data list
     */
    public static final String GET_META_DATA_LIST = PREFIX + "store.metadataList.get";
    /**
     * Get message meta data list in DLC for a queue
     */
    public static final String GET_META_DATA_LIST_FOR_QUEUE_FROM_DLC = PREFIX + "store.metadataListForQueueFromDLC.get";
    /**
     * Get message content
     */
    public static final String GET_CONTENT = PREFIX + "store.content.get";

    /**
     * Get next message meta data from queue
     */
    public static final String GET_NEXT_MESSAGE_IDS_FROM_QUEUE = PREFIX + "store.nextMessageIDsFromQueue.get";

    /**
     * Get next message meta data from queue
     */
    public static final String GET_NEXT_MESSAGE_METADATA_FROM_QUEUE = PREFIX + "store.nextMessageMetadataFromQueue.get";
    /**
     * Get next message meta data in dead letter channel for queue
     */
    public static final String GET_NEXT_MESSAGE_METADATA_IN_DLC_FOR_QUEUE = PREFIX
            + "store.nextMessageMetadataInDLCForQueue.get";
    /**
     * Get next message meta data in dead letter channel
     */
    public static final String GET_NEXT_MESSAGE_METADATA_IN_DLC = PREFIX + "store.nextMessageMetadataInDLC.get";
    /**
     * Move message metadata to DLC
     */
    public static final String MOVE_METADATA_TO_DLC = PREFIX + "store.messageMetadataToDLC.Move";
    /**
     * Delete message meta data from queue
     */
    public static final String DELETE_MESSAGE_META_DATA_FROM_QUEUE = PREFIX + "store.messageMetadataFromQueue.delete";
    /**
     * Delete metadata and content
     */
    public static final String DELETE_MESSAGE_META_DATA_AND_CONTENT = PREFIX + "store.messageMetadataAndContent.delete";

    /**
     * Delete all messages addressed for a specific destination queue from DLC
     */
    public static final String DELETE_MESSAGES_FOR_QUEUE_FROM_DLC = PREFIX + "store.messagesForQueueFromDLC.delete";

    /**
     * Update meta data
     */
    public static final String UPDATE_META_DATA_INFORMATION = PREFIX + "store.metadata.update";

    /*Buffer Values*/

    /**
     * At a given time the number of messages in the inbound disruptor ring
     */
    public static final String DISRUPTOR_INBOUND_RING = PREFIX + "inbound.disruptor.message.count";
    /**
     * At a given time the number of messages which have being acknowledged in the inbound ring
     */
    public static final String DISRUPTOR_MESSAGE_ACK = PREFIX + "inbound.disruptor.ack.count";
    /**
     * At a given time the number of messages in the outbound ring
     */
    public static final String DISRUPTOR_OUTBOUND_RING = PREFIX + "outbound.disruptor.message.count";

    /**
     * At a given time number of queue subscribers
     */
    public static final String QUEUE_SUBSCRIBERS = PREFIX + "queue.subscribers.count";
    /**
     * At a given time number of topic subscriber
     */
    public static final String TOPIC_SUBSCRIBERS = PREFIX + "topic.subscribers.count";
    /**
     * At a given time the number of active channels
     */
    public static final String ACTIVE_CHANNELS = PREFIX + "channels.active.count";

    /**
     * Number of messages received per second. This metric is calculated when a message reaches server.
     */
    public static final String MSG_RECEIVE_RATE = PREFIX + "message.receive";
    /**
     * Number of acknowledgments received from publishers per second.
     */
    public static final String ACK_RECEIVE_RATE = PREFIX + "ack.receive";
    /**
     * Number of messages sent per second. This metric is calculated when a message reaches server.
     */
    public static final String MSG_SENT_RATE = PREFIX + "message.sent";
    /**
     * Number of acknowledgments received from publishers per second.
     */
    public static final String ACK_SENT_RATE = PREFIX + "ack.sent";

}
