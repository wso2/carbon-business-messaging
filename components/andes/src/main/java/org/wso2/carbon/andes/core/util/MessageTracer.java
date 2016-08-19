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

package org.wso2.carbon.andes.core.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.andes.core.AndesMessage;
import org.wso2.carbon.andes.core.AndesMessageMetadata;

/**
 * Purpose of this class is to log message activities
 */
public class MessageTracer {

    private static Log log = LogFactory.getLog(MessageTracer.class);

    public static final String REACHED_ANDES_CORE = "reached andes core";
    public static final String PUBLISHED_TO_INBOUND_DISRUPTOR = "submitted to inbound disruptor";
    public static final String MESSAGE_ID_MAPPED = "mapped to andes message";
    public static final String MESSAGE_CLONED = "cloned with message id";
    public static final String CONTENT_WRITTEN_TO_DB = "content written to database";
    public static final String SLOT_INFO_UPDATED = "slot information updated";
    public static final String PUBLISHED_TO_OUTBOUND_DISRUPTOR = "submitted to outbound disruptor";
    public static final String METADATA_READ_FROM_DB = "metadata read from database";
    public static final String METADATA_BUFFERED_FOR_DELIVERY = "metadata buffered for delivery";
    public static final String CONTENT_READ = "content read from database";
    public static final String DISPATCHED_TO_PROTOCOL = "dispatched to protocol level for delivery";
    public static final String MESSAGE_REJECTED = "message rejected";
    public static final String MESSAGE_REQUEUED_SUBSCRIBER = "message re-queued to subscriber";
    public static final String MOVED_TO_DLC = "message moved to DLC";
    public static final String MESSAGE_DELETED = "message deleted";
    public static final String ACK_RECEIVED_FROM_PROTOCOL = "ACK received from protocol";
    public static final String ACK_PUBLISHED_TO_DISRUPTOR = "ACK event submitted to disruptor";
    public static final String MESSAGE_REQUEUED_BUFFER = "message re-queued to buffer";
    public static final String CONTENT_DECOMPRESSION_HANDLER_MESSAGE = "Content decompressed";
    public static final String MESSAGE_BEYOND_LAST_ROLLBACK = "Message is beyond the last rollback point. Therefore " +
            "deliveryCount is not increased.";
    public static final String DISCARD_STALE_MESSAGE = "discarding delivery as message is stale";

    /**
     * This method will print debug logs for message activities. This will accept parameters for
     * message id, destination name and activity message
     *
     * @param messageId   Andes message id
     * @param destination destination name
     * @param content     message activity
     */
    public static void trace(long messageId, String destination, String content) {
        if (log.isTraceEnabled()) {
            StringBuilder messageContent = new StringBuilder();
            messageContent.append("Message { Destination: ");
            messageContent.append(destination);
            if (messageId > 0) { // Check if andes message id is assigned, else ignore
                messageContent.append(" , Id: ");
                messageContent.append(messageId);
            }
            messageContent.append(" } ");
            messageContent.append(content);
            log.trace(messageContent.toString());
        }
    }

    /**
     * This method will print debug logs for message activities. This will accept andes message as
     * a parameter
     *
     * @param message Andes message
     * @param content Message activity
     */
    public static void trace(AndesMessage message, String content) {
        trace(message.getMetadata().getMessageId(), message.getMetadata().getDestination(), content);
    }

    /**
     * This method will print debug logs for message activities. This will accept metadata as
     * a parameter
     *
     * @param metadata andes metadata object
     * @param content  message activity
     */
    public static void trace(AndesMessageMetadata metadata, String content) {
        trace(metadata.getMessageId(), metadata.getDestination(), content);
    }

    /**
     * This method will check if trace logs are enabled. This method can be used when performing
     * operations inside trace() method parameters
     *
     * @return Status of MessageTracer
     */
    public static boolean isEnabled() {
        return log.isTraceEnabled();
    }

}
