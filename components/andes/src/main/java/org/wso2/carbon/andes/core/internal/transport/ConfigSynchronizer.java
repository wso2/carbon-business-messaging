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

package org.wso2.carbon.andes.core.internal.transport;

import org.wso2.carbon.andes.core.AndesBinding;
import org.wso2.carbon.andes.core.AndesException;
import org.wso2.carbon.andes.core.AndesExchange;
import org.wso2.carbon.andes.core.AndesQueue;

/**
 * When a queue, exchange or binding changed notify the interested parties through this listener
 * interface
 */
public interface ConfigSynchronizer {

    void clusterExchangeAdded(AndesExchange exchange) throws AndesException;

    /**
     * remove exchange from local node
     *
     * @param exchange exchange to be removed
     */
    void clusterExchangeRemoved(AndesExchange exchange) throws AndesException;

    /**
     * create a queue in local node
     *
     * @param queue queue to be created
     */
    void clusterQueueAdded(AndesQueue queue) throws AndesException;

    /**
     * remove queue from local node
     *
     * @param queue queue to be removed
     */
    void clusterQueueRemoved(AndesQueue queue) throws AndesException;

    /**
     * purge queue from local node - clear all in memory message buffers for the queue in this node.
     *
     * @param queue The Andes Queue object to purge
     * @throws AndesException
     */
    void clusterQueuePurged(AndesQueue queue) throws AndesException;

    /**
     * add binding to the local node
     *
     * @param binding binding to be added
     */
    void clusterBindingAdded(AndesBinding binding) throws AndesException;

    /**
     * remove binding rom local node
     *
     * @param binding binding to be removed
     */
    void clusterBindingRemoved(AndesBinding binding) throws AndesException;
}
