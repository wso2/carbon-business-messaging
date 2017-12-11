/*
 * Copyright (c) 2017, WSO2 Inc. (http://wso2.com) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.business.messaging.admin.services.managers;

import org.wso2.carbon.business.messaging.admin.services.exceptions.InternalServerException;
import org.wso2.carbon.business.messaging.admin.services.types.Subscription;

import java.util.List;

/**
 * This interface provides the base for managing all subscriptions related services.
 */
public interface SubscriptionManagerService {

    /**
     * Gets subscriptions belonging to a specific protocol type and destination type. The subscriptions can be filtered
     * by subscription name, destination name and whether they are active or not.
     *
     * @param protocol         The protocol type matching for the subscription. Example : amqp, mqtt.
     * @param subscriptionType The destination type matching for the subscription. Example : queue, topic,
     *                         durable_topic.
     * @param subscriptionName The name of the subscription. If "*", all subscriptions are included. Else subscriptions
     *                         that <strong>contains</strong> the value are included.
     * @param destinationName  The name of the destination name. If "*", all destinations are included. Else
     *                         destinations that <strong>equals</strong> the value are included.
     * @param active           Filtering the subscriptions that are active or inactive. Supported values = "*", "true"
     *                         and "false".
     * @param offset           The starting index to return.
     * @param limit            The number of subscriptions to return.
     * @return A list of {@link Subscription}s.
     * @throws InternalServerException Error in handling subscription related information
     */
    List<Subscription> getSubscriptions(String protocol, String subscriptionType, String subscriptionName,
            String destinationName, String active, int offset, int limit) throws InternalServerException;

    /**
     * Close/Unsubscribe subscriptions forcefully belonging to a specific protocol type, destination type.
     *
     * @param protocol         The protocol type matching for the subscription. Example : amqp, mqtt.
     * @param subscriptionType The subscription type matching for the subscription. Example : queue, topic,
     *                         durable_topic.
     * @param destinationName  The name of the destination to close/unsubscribe. If "*", all destinations are included.
     *                         Else destinations that <strong>contains</strong> the value are included.
     * @param unsubscribeOnly  Whether only to unsubscribe only or remove the the subscription.
     * @throws InternalServerException Error in handling subscription related information
     */
    void closeSubscriptions(String protocol, String subscriptionType, String destinationName, boolean unsubscribeOnly)
            throws InternalServerException;

    /**
     * Close/Remove/Unsubscribe subscriptions forcefully belonging to a specific protocol type, destination type.
     *
     * @param protocol         The protocol type matching for the subscription. Example : amqp, mqtt.
     * @param subscriptionType The subscription type matching for the subscription. Example : queue, topic,
     *                         durable_topic.
     * @param subscriptionID   The subscription ID.
     * @param unsubscribeOnly  Whether only to unsubscribe only or remove the the subscription.
     * @throws InternalServerException Error in handling subscription related information
     */
    void closeSubscription(String protocol, String subscriptionType, String subscriptionID, boolean unsubscribeOnly)
            throws InternalServerException;
}
