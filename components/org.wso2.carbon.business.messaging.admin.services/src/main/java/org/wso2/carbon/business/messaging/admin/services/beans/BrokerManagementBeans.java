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

import org.apache.commons.lang.StringUtils;
import org.wso2.carbon.business.messaging.admin.services.exceptions.BrokerManagerException;
import org.wso2.carbon.business.messaging.admin.services.managers.bean.utils.BrokerManagementConstants;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

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
        boolean isClustered = false;
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            ObjectName objectName =
                    new ObjectName("org.wso2.andes:type=ClusterManagementInformation," +
                                   "name=ClusterManagementInformation");
            Object result = mBeanServer.getAttribute(objectName, BrokerManagementConstants.IS_CLUSTERING_ENABLED);

            if (null != result) {
                isClustered = (Boolean) result;
            }

            return isClustered;
        } catch (MalformedObjectNameException | InstanceNotFoundException | AttributeNotFoundException
                                                                            | ReflectionException | MBeanException e) {
            throw new BrokerManagerException("Cannot access cluster information", e);
        }
    }

    /**
     * Gets the current node's ID.
     *
     * @return Current node's ID.
     * @throws BrokerManagerException
     */
    public String getMyNodeID() throws BrokerManagerException {
        String myNodeID = "";
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            ObjectName objectName =
                    new ObjectName("org.wso2.andes:type=ClusterManagementInformation," +
                                   "name=ClusterManagementInformation");
            Object result = mBeanServer.getAttribute(objectName, BrokerManagementConstants.MY_NODE_ID);

            if (null != result) {
                myNodeID = (String) result;
            }
            return myNodeID;

        } catch (MalformedObjectNameException | InstanceNotFoundException | ReflectionException
                                                                    | MBeanException | AttributeNotFoundException e) {
            throw new BrokerManagerException("Cannot access cluster information", e);
        }
    }

    /**
     * Gets the coordinator node's host address and port in a cluster.
     *
     * @return The coordinator node's host address and port.
     * @throws BrokerManagerException
     */
    public String getCoordinatorNodeAddress() throws BrokerManagerException {
        String coordinatorNodeAddress = StringUtils.EMPTY;
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            ObjectName objectName =
                    new ObjectName("org.wso2.andes:type=ClusterManagementInformation," +
                                   "name=ClusterManagementInformation");
            Object result = mBeanServer.getAttribute(objectName, BrokerManagementConstants.COORDINATOR_NODE_ADDRESS);
            if (result != null) {
                coordinatorNodeAddress = (String) result;
            }
            return coordinatorNodeAddress;

        } catch (MalformedObjectNameException | ReflectionException | InstanceNotFoundException
                                                                    | AttributeNotFoundException | MBeanException e) {
            throw new BrokerManagerException("Cannot get coordinator node address. Check if clustering is enabled.", e);
        }
    }

    /**
     * Gets the IP addresses and ports of the nodes in a cluster.
     *
     * @return The list of addresses of the nodes in a cluster.
     * @throws BrokerManagerException
     */
    public List<String> getAllClusterNodeAddresses() throws BrokerManagerException {
        List<String> allClusterNodeAddresses = new ArrayList<>();
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {

            ObjectName objectName =
                    new ObjectName("org.wso2.andes:type=ClusterManagementInformation," +
                                   "name=ClusterManagementInformation");
            Object result = mBeanServer.getAttribute(objectName, BrokerManagementConstants.ALL_CLUSTER_NODE_ADDRESSES);

            if (result != null) {
                //noinspection unchecked
                allClusterNodeAddresses = (List<String>) result;
            }
            return allClusterNodeAddresses;

        } catch (MalformedObjectNameException | ReflectionException | InstanceNotFoundException
                                                                    | MBeanException | AttributeNotFoundException e) {
            throw new BrokerManagerException("Cannot get cluster node addresses. Check if clustering is enabled.", e);
        }
    }

    /**
     * Gets the message store's health status.
     *
     * @return True if healthy, else false.
     * @throws BrokerManagerException
     */
    public boolean getStoreHealth() throws BrokerManagerException {
        boolean storeHealth = false;
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            ObjectName objectName =
                    new ObjectName("org.wso2.andes:type=ClusterManagementInformation," +
                                   "name=ClusterManagementInformation");
            Object result = mBeanServer.getAttribute(objectName, BrokerManagementConstants.STORE_HEALTH);
            if (result != null) {
                storeHealth = (Boolean) result;
            }
            return storeHealth;

        } catch (MalformedObjectNameException | ReflectionException | InstanceNotFoundException
                                                                    | MBeanException | AttributeNotFoundException e) {
            throw new BrokerManagerException("Cannot get message store health.", e);
        }
    }
}
