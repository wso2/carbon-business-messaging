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
 * Listener listening for queue changes local and cluster
 */
public interface QueueListener {

    /**
     * Queue change event types
     */
    enum QueueEvent {
        ADDED,
        DELETED,
        PURGED
    }

    /**
     * Handle queue has changed in the cluster
     *
     * @param andesQueue changed queue
     * @param changeType what type of change has happened
     */
    void handleClusterQueuesChanged(AndesQueue andesQueue, QueueEvent changeType) throws AndesException;

    /**
     * Handle the event where a queue has changed in another node
     *
     * @param andesQueue changed queue
     * @param changeType what type of change has happened
     */
    void handleLocalQueuesChanged(AndesQueue andesQueue, QueueEvent changeType) throws AndesException;
}
