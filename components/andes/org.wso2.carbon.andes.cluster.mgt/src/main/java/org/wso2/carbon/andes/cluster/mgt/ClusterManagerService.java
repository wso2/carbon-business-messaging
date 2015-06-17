/*
 * Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 */

package org.wso2.carbon.andes.cluster.mgt;

import org.wso2.carbon.andes.cluster.mgt.internal.ClusterMgtException;
import org.wso2.carbon.andes.cluster.mgt.internal.managementBeans.ClusterManagementBeans;

import java.util.List;

/**
 * Admin service class for cluster management
 */
public class ClusterManagerService {

    /**
     * Gets the IP addresses and ports of the nodes in a cluster
     *
     * @return A list of addresses of the nodes in a cluster
     * @throws ClusterMgtAdminException
     */
    public String[] getAllClusterNodeAddresses() throws ClusterMgtAdminException {
        ClusterManagementBeans clusterManagementBeans = new ClusterManagementBeans();
        List<String> addresses;
        try {
            addresses = clusterManagementBeans.getAllClusterNodeAddresses();
            return addresses.toArray(new String[addresses.size()]);
        } catch (ClusterMgtException e) {
            throw new ClusterMgtAdminException("Cannot get all cluster node addresses. Check if clustering is enabled.", e);
        }
    }

    /**
     * Gets the coordinator node's host address and port in a cluster
     *
     * @return The coordinator node's host address and port
     * @throws ClusterMgtAdminException
     */
    public String getCoordinatorNodeAddress() throws ClusterMgtAdminException {
        try {
            ClusterManagementBeans clusterManagementBeans = new ClusterManagementBeans();
            return clusterManagementBeans.getCoordinatorNodeAddress();
        } catch (ClusterMgtException e) {
            throw new ClusterMgtAdminException("Cannot get coordinator node address. Check if clustering is enabled.", e);
        }
    }

    /**
     * check if broker is in clustering mode
     *
     * @return boolean if clustering enabled
     * @throws ClusterMgtException
     */
    public boolean isClusteringEnabled() throws ClusterMgtException {
        ClusterManagementBeans clusterManagementBeans = new ClusterManagementBeans();
        return clusterManagementBeans.isClusteringEnabled();
    }

    /**
     * get the ID assigned by zookeeper to this node
     *
     * @return String node ID
     * @throws ClusterMgtException
     */
    public String getMyNodeID() throws ClusterMgtException {
        ClusterManagementBeans clusterManagementBeans = new ClusterManagementBeans();
        return clusterManagementBeans.getMyNodeID();
    }

    /**
     * Gets the message store's health status
     *
     * @return true if healthy, else false.
     */
    public boolean getStoreHealth() throws ClusterMgtException {
        ClusterManagementBeans clusterManagementBeans = new ClusterManagementBeans();
        return clusterManagementBeans.getStoreHealth();
    }
}
