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

import org.wso2.carbon.andes.core.subscription.LocalSubscription;
import org.wso2.carbon.andes.core.util.MessageTracer;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Class to keep track of message delivery information used for delivery.
 */
public class MessageDeliveryInfo {

    private MessageFlusher messageFlusher;
    /**
     * Destination of the messages (not storage queue name). For durable topics
     * this is the internal queue name with subscription id.
     */
    private String destination;

    /**
     * Subscription iterator
     */
    private Iterator<LocalSubscription> iterator;

    /**
     * In-memory message list scheduled to be delivered. These messages will be flushed
     * to subscriber.Used Map instead of Set because of https://wso2.org/jira/browse/MB-1624
     */
    private ConcurrentHashMap<Long, DeliverableAndesMetadata> readButUndeliveredMessages = new ConcurrentHashMap<>();

    /***
     * In case of a purge, we must store the timestamp when the purge was called.
     * This way we can identify messages received before that timestamp that fail and ignore them.
     */
    private Long lastPurgedTimestamp;

    /**
     * The destination type of the messages in the buffer
     */
    private DestinationType destinationType;

    /**
     * The protocol type of the messages in the buffer.
     */
    private ProtocolType protocolType;

    /***
     * Constructor
     * initialize lastPurgedTimestamp to 0.
     */
    public MessageDeliveryInfo(MessageFlusher messageFlusher) {
        this.messageFlusher = messageFlusher;
        lastPurgedTimestamp = 0L;
    }

    /**
     * Buffer messages to be delivered
     *
     * @param message message metadata to buffer
     */
    public void bufferMessage(DeliverableAndesMetadata message) {
        readButUndeliveredMessages.put(message.getMessageId(), message);
        message.markAsBuffered();
        //Tracing message
        MessageTracer.trace(message, MessageTracer.METADATA_BUFFERED_FOR_DELIVERY);

    }

    /**
     * Re-buffer messages in case of failures
     *
     * @param message Message metadata to buffer
     */
    public void reBufferMessage(DeliverableAndesMetadata message) {
        readButUndeliveredMessages.putIfAbsent(message.getMessageId(), message);
        message.markAsBuffered(); //Tracing message
        MessageTracer.trace(message, MessageTracer.METADATA_BUFFERED_FOR_DELIVERY);

    }

    /**
     * Check if message buffer for destination is empty
     *
     * @return true if empty
     */
    public boolean isMessageBufferEmpty() {
        return readButUndeliveredMessages.isEmpty();
    }

    /**
     * Get the number of messages buffered for the destination to be delivered
     *
     * @return number of messages buffered
     */
    public int getSizeOfMessageBuffer() {
        return readButUndeliveredMessages.size();
    }

    /**
     * Returns boolean variable saying whether this destination has room or not
     *
     * @return whether this destination has room or not
     */
    public boolean messageBufferHasRoom() {
        boolean hasRoom = true;
        if (readButUndeliveredMessages.size() >= messageFlusher.getMaxNumberOfReadButUndeliveredMessages()) {
            hasRoom = false;
        }
        return hasRoom;
    }

    /***
     * Clear the read-but-undelivered collection of messages of the given queue from memory
     *
     * @return Number of messages that was in the read-but-undelivered buffer
     */
    public int clearReadButUndeliveredMessages() {

        int messageCount = readButUndeliveredMessages.size();

        readButUndeliveredMessages.clear();

        return messageCount;
    }

    /***
     * @return Last purged timestamp of queue.
     */
    public Long getLastPurgedTimestamp() {
        return lastPurgedTimestamp;
    }

    /**
     * set last purged timestamp for queue.
     *
     * @param lastPurgedTimestamp the time stamp of the message message which was purged most recently
     */
    public void setLastPurgedTimestamp(Long lastPurgedTimestamp) {
        this.lastPurgedTimestamp = lastPurgedTimestamp;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public Collection<DeliverableAndesMetadata> getReadButUndeliveredMessages() {
        return readButUndeliveredMessages.values();
    }

    public Iterator<LocalSubscription> getIterator() {
        return iterator;
    }

    public void setIterator(Iterator<LocalSubscription> iterator) {
        this.iterator = iterator;
    }

    public DestinationType getDestinationType() {
        return destinationType;
    }

    public void setDestinationType(DestinationType destinationType) {
        this.destinationType = destinationType;
    }

    public ProtocolType getProtocolType() {
        return protocolType;
    }

    public void setProtocolType(ProtocolType protocolType) {
        this.protocolType = protocolType;
    }
}
