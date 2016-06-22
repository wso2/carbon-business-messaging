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

package org.wso2.carbon.andes.core.subscription;

import org.wso2.carbon.andes.core.AndesContent;
import org.wso2.carbon.andes.core.AndesException;
import org.wso2.carbon.andes.core.AndesMessageMetadata;
import org.wso2.carbon.andes.core.ProtocolMessage;
import org.wso2.carbon.andes.core.ProtocolType;

import java.util.UUID;

/**
 * Contract defining subscription related to outbound operations.
 */
public interface OutboundSubscription {

    /**
     * Forcefully disconnects protocol subscriber from server. This is called when a server admin wants to disconnect a
     * subscriber using management console.
     *
     * @throws AndesException
     */
    void forcefullyDisconnect() throws AndesException;

    /**
     * Check if message is accepted by 'selector' set to the subscription.
     *
     * @param messageMetadata message to be checked
     * @return true if message is selected, false otherwise
     * @throws AndesException on an error
     */
    boolean isMessageAcceptedBySelector(AndesMessageMetadata messageMetadata)
            throws AndesException;

    /**
     * Deliver the message and content to the subscriber
     *
     * @param messageMetadata metadata of the message
     * @param content         content of the message
     * @return delivery is success. If delivery rule evaluations are failed delivery will not be a success
     * @throws AndesException
     */
    public boolean sendMessageToSubscriber(ProtocolMessage messageMetadata, AndesContent content) throws
                                                                                                  AndesException;

    /**
     * Check if subscription is active. If the underlying channel can accept
     * messages it is considered as live
     *
     * @return true if subscriber is active
     */
    public boolean isActive();

    /**
     * Get ID of the subscription channel
     *
     * @return unique id
     */
    public UUID getChannelID();

    /**
     * Get the storage queue name for this subscription.
     *
     * @param destination    The destination this subscriber subscribed to
     * @param subscribedNode The node this subscriber subscribed to
     * @return The storage queue name
     */
    public String getStorageQueueName(String destination, String subscribedNode);

    /**
     * Return the protocol type of the subscription.
     *
     * @return protocol type
     */
    public ProtocolType getProtocolType();

}
