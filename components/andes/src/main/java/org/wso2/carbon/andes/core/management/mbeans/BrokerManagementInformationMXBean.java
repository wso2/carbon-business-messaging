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

package org.wso2.carbon.andes.core.management.mbeans;

import org.wso2.carbon.andes.core.ProtocolType;

import java.util.List;
import java.util.Set;

/**
 * Exposes the Cluster Management related information
 */
public interface BrokerManagementInformationMXBean {

    /**
     * Gets the supported protocols of the broker.
     *
     * @return A set of protocols.
     */
    Set<ProtocolType> getSupportedProtocols();

    /**
     * Checks if clustering is enabled
     *
     * @return whether clustering is enabled
     */
    boolean isClusteringEnabled();

    /**
     * Gets node ID assigned for the node
     *
     * @return node ID
     */
    String getMyNodeID();

    /**
     * Gets the coordinator node's address
     *
     * @return Address of the coordinator node
     */
    String getCoordinatorNodeAddress();

    /**
     * Gets all the address of the nodes in a cluster
     *
     * @return A list of address of the nodes in a cluster
     */
    List<String> getAllClusterNodeAddresses();

    /**
     * Gets the message store's health status
     *
     * @return true if healthy, else false.
     */
    boolean getStoreHealth();
}
