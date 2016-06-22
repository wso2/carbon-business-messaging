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
 * Wrapper class of message acknowledgment data publish to disruptor
 */
public class AndesAckData {

    /**
     * Acknowledged message
     */
    private DeliverableAndesMetadata acknowledgedMessage;

    /**
     * ID of the channel acknowledge is received
     */
    private UUID channelID;

    /**
     * Holds if acknowledged message is ready to be removed. If all channels
     * acknowledged it becomes removable. Message reference cannot be used here
     * as we need to keep it in disruptor data holder
     */
    private boolean isBaringMessageRemovable = false;

    /**
     * Generate AndesAckData object. This holds acknowledge event in disruptor
     *
     * @param channelID           ID of the channel ack is received
     * @param acknowledgedMessage message being acknowledged
     */
    public AndesAckData(UUID channelID, DeliverableAndesMetadata acknowledgedMessage) {
        this.channelID = channelID;
        this.acknowledgedMessage = acknowledgedMessage;
    }

    /**
     * Get the reference of the message being acknowledged
     *
     * @return Metadata of the acknowledged message
     */
    public DeliverableAndesMetadata getAcknowledgedMessage() {
        return acknowledgedMessage;
    }

    /**
     * Get ID of the channel acknowledgement is received
     *
     * @return channel ID
     */
    public UUID getChannelID() {
        return channelID;
    }

    /**
     * Check if message being acknowledged is ready to be removed
     *
     * @return true if removable
     */
    public boolean isBaringMessageRemovable() {
        return isBaringMessageRemovable;
    }

    /**
     * Set message being acknowledged is ready to be removed. This happens
     * if acknowledgements are received from all channels
     */
    public void setBaringMessageRemovable() {
        this.isBaringMessageRemovable = true;
    }

}
