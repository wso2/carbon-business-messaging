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


/**
 * This interface defines the structure of a message delivery strategy
 */
public interface MessageDeliveryStrategy {

    /**
     * Deliver message. It will find current subscriptions to deliver by the destination of the message
     * and send the messages accordingly
     *
     * @param messageDeliveryInfo The message delivery information holder
     * @param storageQueue        Storage queue related to messages
     * @return number of messages sent
     * @throws AndesException in case of a delivery failure
     */
    int deliverMessageToSubscriptions(MessageDeliveryInfo messageDeliveryInfo, String storageQueue)
            throws AndesException;
}
