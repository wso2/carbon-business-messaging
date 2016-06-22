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

package org.wso2.carbon.andes.core.internal.configuration.util;

/**
 * Enum to specify message delivery strategies for topic messages. This is configured
 * at broker.xml under <delivery>/<topicMessageDeliveryStrategy>
 */
public enum TopicMessageDeliveryStrategy {

    /**
     * broker do not loose any message to any subscriber. When there are slow
     * subscribers this can cause broker go Out of Memory.
     */
    DISCARD_NONE,

    /**
     * we deliver to the speed of the slowest topic subscriber. This can cause fast
     * subscribers to starve. But eliminate Out of Memory issue
     */
    SLOWEST_SUB_RATE,

    /**
     * broker will try best to deliver. To eliminate Out of Memory threat broker limits
     * sent but not acked message count to <maxUnackedMessages>. If it is breached, message can
     * either be lost or actually sent but ack is not honoured
     */
    DISCARD_ALLOWED
}
