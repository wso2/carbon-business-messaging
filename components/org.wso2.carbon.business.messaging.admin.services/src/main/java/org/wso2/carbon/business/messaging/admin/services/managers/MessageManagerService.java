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


/**
 * This interface provides the base for managing all messages related services.
 */
public interface MessageManagerService {

    /**
     * Purge all messages belonging to a destination.
     *
     * @param protocol        The protocol type matching for the message. Example : amqp, mqtt.
     * @param destinationType The destination type matching for the message. Example : queue, topic, durable_topic.
     * @param destinationName The name of the destination to purge messages.
     * @throws InternalServerException Error in handling messages related information
     */
    void deleteMessages(String protocol, String destinationType, String destinationName) throws InternalServerException;
}
