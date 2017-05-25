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

package org.wso2.carbon.business.messaging.admin.services.beans;

import org.wso2.carbon.business.messaging.admin.services.exceptions.BrokerManagerException;

import java.util.List;

/**
 * JMX client for managing broker configuration 40related resources.
 */
public class BrokerManagementBeans {

    /**
     * Checks whether clustering is enabled.
     *
     * @return A boolean whether clustering is enabled.
     * @throws BrokerManagerException
     */
    public boolean isClusteringEnabled() throws BrokerManagerException {
        throw new UnsupportedOperationException();
    }

    /**
     * Gets the current node's ID.
     *
     * @return Current node's ID.
     * @throws BrokerManagerException
     */
    public String getMyNodeID() throws BrokerManagerException {
        throw new UnsupportedOperationException();
    }

    /**
     * Gets the coordinator node's host address and port in a cluster.
     *
     * @return The coordinator node's host address and port.
     * @throws BrokerManagerException
     */
    public String getCoordinatorNodeAddress() throws BrokerManagerException {
        throw new UnsupportedOperationException();
    }

    /**
     * Gets the IP addresses and ports of the nodes in a cluster.
     *
     * @return The list of addresses of the nodes in a cluster.
     * @throws BrokerManagerException
     */
    public List<String> getAllClusterNodeAddresses() throws BrokerManagerException {
        throw new UnsupportedOperationException();
    }

    /**
     * Gets the message store's health status.
     *
     * @return True if healthy, else false.
     * @throws BrokerManagerException
     */
    public boolean getStoreHealth() throws BrokerManagerException {
        throw new UnsupportedOperationException();
    }
}
