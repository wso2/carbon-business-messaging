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

import org.wso2.carbon.andes.core.AndesException;

/**
 * Builder class for {@link SubscriptionProcessor}.
 */
public class SubscriptionProcessorBuilder {

    SubscriptionProcessor clusterSubscriptionProcessor = new SubscriptionProcessor();
    SubscriptionProcessor localSubscriptionProcessor = new SubscriptionProcessor();

    /**
     * Build cluster subscription processor with relevant classes for processing cluster subscriptions.
     *
     * @return Subscription processor initialized for processing cluster subscriptions
     * @throws AndesException
     */
    public static SubscriptionProcessor getClusterSubscriptionProcessor() throws AndesException {
        SubscriptionProcessor subscriptionProcessor = new SubscriptionProcessor();

//        // Add handlers for AMQP
//        subscriptionProcessor.addHandler(ProtocolType.AMQP, DestinationType.QUEUE, new QueueSubscriptionStore());
//        subscriptionProcessor.addHandler(ProtocolType.AMQP, DestinationType.TOPIC,
//                new TopicSubscriptionBitMapStore(ProtocolType.AMQP));
//        subscriptionProcessor.addHandler(ProtocolType.AMQP, DestinationType.DURABLE_TOPIC,
//                new TopicSubscriptionBitMapStore(ProtocolType.AMQP));
//
//        // Add handlers for MQTT
//        subscriptionProcessor.addHandler(ProtocolType.MQTT, DestinationType.TOPIC,
//                new TopicSubscriptionBitMapStore(ProtocolType.MQTT));
//        subscriptionProcessor.addHandler(ProtocolType.MQTT, DestinationType.DURABLE_TOPIC,
//                new TopicSubscriptionBitMapStore(ProtocolType.MQTT));

        return subscriptionProcessor;
    }

    /**
     * Build loca subscription processor with relevant classes for processing local subscriptions.
     *
     * @return Subscription processor initialized for processing local subscriptions
     * @throws AndesException
     */
    public static SubscriptionProcessor getLocalSubscriptionProcessor() throws AndesException {
        SubscriptionProcessor subscriptionProcessor = new SubscriptionProcessor();

//        // Add handlers for AMQP
//        subscriptionProcessor.addHandler(ProtocolType.AMQP, DestinationType.QUEUE,
//                new QueueSubscriptionStore());
//
//        // Using queue subscription store for topics since wildcard matching is not required for local subscriptions
//        subscriptionProcessor.addHandler(ProtocolType.AMQP, DestinationType.TOPIC, new QueueSubscriptionStore());
//
//        // Local durable topic subscription store is specific to local mode since subscriptions are stored
//        // against their targetQueue in this store
//        subscriptionProcessor.addHandler(ProtocolType.AMQP, DestinationType.DURABLE_TOPIC,
//                new LocalDurableTopicSubscriptionStore());
//
//        // Add handlers for MQTT
//        // Using queue subscription store for topics since wildcard matching is not required for local subscriptions
//        subscriptionProcessor.addHandler(ProtocolType.MQTT, DestinationType.TOPIC, new QueueSubscriptionStore());
//        subscriptionProcessor.addHandler(ProtocolType.MQTT, DestinationType.DURABLE_TOPIC,
//                new LocalDurableTopicSubscriptionStore());

        return subscriptionProcessor;
    }
}
