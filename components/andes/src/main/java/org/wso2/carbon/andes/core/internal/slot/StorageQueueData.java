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

package org.wso2.carbon.andes.core.internal.slot;

import org.wso2.carbon.andes.core.DestinationType;
import org.wso2.carbon.andes.core.ProtocolType;

/**
 * Keeps track of the queue data relevant to it's storage queue.
 */
public class StorageQueueData {

    /**
     * The storage queue name.
     */
    private String storageQueueName;

    /**
     * The destination of the messages in the storage queue.
     */
    private String destinationName;

    /**
     * The Protocol Type of the messages in the storage queue.
     */
    private ProtocolType protocolType;

    /**
     * The Destination Type of the messages in the storage queue.
     */
    private DestinationType destinationType;

    /**
     * Constructor initializing the fields.
     *
     * @param storageQueueName The storage queue name
     * @param destinationName  The destination of the messages in the storage queue
     * @param protocolType     The Protocol Type of the messages in the storage queue
     * @param destinationType  The Destination Type of the messages in the storage queue
     */
    public StorageQueueData(String storageQueueName, String destinationName,
                            ProtocolType protocolType, DestinationType destinationType) {
        this.storageQueueName = storageQueueName;
        this.destinationName = destinationName;
        this.protocolType = protocolType;
        this.destinationType = destinationType;
    }

    public String getStorageQueueName() {
        return storageQueueName;
    }

    public String getDestinationName() {
        return destinationName;
    }

    public ProtocolType getProtocolType() {
        return protocolType;
    }

    public DestinationType getDestinationType() {
        return destinationType;
    }
}
