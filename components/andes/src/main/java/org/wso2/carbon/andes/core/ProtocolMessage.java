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

import java.util.UUID;

/**
 * This class represents object of a message that is deliverable by any protocol.
 * Channel wise fields extracted from DeliverableAndesMetadata required by protocol channel is snapshot from it and
 * inject into this. Protocol delivery MUST read channel wise information from here
 */
public class ProtocolMessage {

    /**
     * Message reference to kernel side. Whenever we change message from kernel side
     * it will be reflected here (i.e stale message)
     */
    private DeliverableAndesMetadata message;

    /**
     * ID of the message.
     */
    private long messageID;

    /**
     * ID of the channel message is delivered to
     */
    private UUID channelID;

    /**
     * Number of times message is delivered to this channel
     */
    private int numberOfDeliveriesForProtocolChannel;

    /**
     * Is message re-delivered to the channel
     */
    private boolean isRedelivered = false;


    /**
     * Constructor - create new ProtocolMessage
     *
     * @param message   message to be delivered
     * @param channelID Id of the channel message is delivered to
     */
    public ProtocolMessage(DeliverableAndesMetadata message, UUID channelID) {
        this.message = message;
        this.channelID = channelID;
        this.messageID = message.getMessageId();
        this.numberOfDeliveriesForProtocolChannel = message.getNumOfDeliveries4Channel(channelID);
        if (numberOfDeliveriesForProtocolChannel > 1) {
            isRedelivered = true;
        }
    }

    public long getMessageID() {
        return messageID;
    }

    /**
     * Check if message is redelivered
     *
     * @return true if re-delivered
     */
    public boolean isRedelivered() {
        return isRedelivered;
    }

    /**
     * Get reference of message to deliver from kernel
     *
     * @return message to deliver
     */
    public DeliverableAndesMetadata getMessage() {
        return message;
    }

    /**
     * Get number of times message is delivered to the channel
     *
     * @return delivered times.
     */
    public int getNumberOfDeliveriesForProtocolChannel() {
        return numberOfDeliveriesForProtocolChannel;
    }

    /**
     * Get ID of the channel message is being delivered
     *
     * @return unique ID of the channel
     */
    public UUID getChannelID() {
        return channelID;
    }
}
