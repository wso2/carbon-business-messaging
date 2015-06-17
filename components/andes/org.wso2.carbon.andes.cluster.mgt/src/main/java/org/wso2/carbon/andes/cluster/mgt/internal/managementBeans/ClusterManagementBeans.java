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
package org.wso2.carbon.andes.cluster.mgt.internal.managementBeans;

import org.apache.commons.lang.StringUtils;
import org.wso2.carbon.andes.cluster.mgt.internal.ClusterMgtConstants;
import org.wso2.carbon.andes.cluster.mgt.internal.ClusterMgtException;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

/**
 * Cluster Management MBeans invoker
 */
public class ClusterManagementBeans {

    /**
     * Checks whether clustering is enabled
     *
     * @return a boolean whether clustering is enabled
     * @throws ClusterMgtException
     */
    public boolean isClusteringEnabled() throws ClusterMgtException {
        boolean isClustered = false;
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            ObjectName objectName =
                    new ObjectName("org.wso2.andes:type=ClusterManagementInformation," +
                                   "name=ClusterManagementInformation");
            Object result = mBeanServer.getAttribute(objectName, ClusterMgtConstants.IS_CLUSTERING_ENABLED);

            if (result != null) {
                isClustered = (Boolean) result;
            }

            return isClustered;
        } catch (MalformedObjectNameException e) {
            throw new ClusterMgtException("Cannot access cluster information", e);
        } catch (InstanceNotFoundException e) {
            throw new ClusterMgtException("Cannot access cluster information", e);
        } catch (ReflectionException e) {
            throw new ClusterMgtException("Cannot access cluster information", e);
        } catch (AttributeNotFoundException e) {
            throw new ClusterMgtException("Cannot access cluster information", e);
        } catch (MBeanException e) {
            throw new ClusterMgtException("Cannot access cluster information", e);
        }
    }

    /**
     * Gets the current node's ID
     *
     * @return current node's ID
     * @throws ClusterMgtException
     */
    public String getMyNodeID() throws ClusterMgtException {
        String myNodeID = "";
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            ObjectName objectName =
                    new ObjectName("org.wso2.andes:type=ClusterManagementInformation," +
                                   "name=ClusterManagementInformation");
            Object result = mBeanServer.getAttribute(objectName, ClusterMgtConstants.MY_NODE_ID);

            if (result != null) {
                myNodeID = (String) result;
            }
            return myNodeID;

        } catch (MalformedObjectNameException e) {
            throw new ClusterMgtException("Cannot access cluster information", e);
        } catch (InstanceNotFoundException e) {
            throw new ClusterMgtException("Cannot access cluster information", e);
        } catch (ReflectionException e) {
            throw new ClusterMgtException("Cannot access cluster information", e);
        } catch (AttributeNotFoundException e) {
            throw new ClusterMgtException("Cannot access cluster information", e);
        } catch (MBeanException e) {
            throw new ClusterMgtException("Cannot access cluster information", e);
        }
    }

    /**
     * Gets the coordinator node's host address and port in a cluster
     *
     * @return The coordinator node's host address and port
     * @throws ClusterMgtException
     */
    public String getCoordinatorNodeAddress() throws ClusterMgtException {
        String coordinatorNodeAddress = StringUtils.EMPTY;
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            ObjectName objectName =
                    new ObjectName("org.wso2.andes:type=ClusterManagementInformation," +
                                   "name=ClusterManagementInformation");
            Object result = mBeanServer.getAttribute(objectName, ClusterMgtConstants.COORDINATOR_NODE_ADDRESS);
            if (result != null) {
                coordinatorNodeAddress = (String) result;
            }
            return coordinatorNodeAddress;

        } catch (MalformedObjectNameException e) {
            throw new ClusterMgtException("Cannot get coordinator node address. Check if clustering is enabled.", e);
        } catch (ReflectionException e) {
            throw new ClusterMgtException("Cannot get coordinator node address. Check if clustering is enabled.", e);
        } catch (MBeanException e) {
            throw new ClusterMgtException("Cannot get coordinator node address. Check if clustering is enabled.", e);
        } catch (InstanceNotFoundException e) {
            throw new ClusterMgtException("Cannot get coordinator node address. Check if clustering is enabled.", e);
        } catch (AttributeNotFoundException e) {
            throw new ClusterMgtException("Cannot get coordinator node address. Check if clustering is enabled.", e);
        }
    }

    /**
     * Gets the IP addresses and ports of the nodes in a cluster
     *
     * @return A list of addresses of the nodes in a cluster
     * @throws ClusterMgtException
     */
    public List<String> getAllClusterNodeAddresses() throws ClusterMgtException {
        List<String> allClusterNodeAddresses = new ArrayList<String>();
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {

            ObjectName objectName =
                    new ObjectName("org.wso2.andes:type=ClusterManagementInformation," +
                                   "name=ClusterManagementInformation");
            Object result = mBeanServer.getAttribute(objectName, ClusterMgtConstants.ALL_CLUSTER_NODE_ADDRESSES);

            if (result != null) {
                allClusterNodeAddresses = (List<String>) result;
            }
            return allClusterNodeAddresses;

        } catch (MalformedObjectNameException e) {
            throw new ClusterMgtException("Cannot get cluster node addresses. Check if clustering is enabled.", e);
        } catch (ReflectionException e) {
            throw new ClusterMgtException("Cannot get cluster node addresses. Check if clustering is enabled.", e);
        } catch (MBeanException e) {
            throw new ClusterMgtException("Cannot get cluster node addresses. Check if clustering is enabled.", e);
        } catch (InstanceNotFoundException e) {
            throw new ClusterMgtException("Cannot get cluster node addresses. Check if clustering is enabled.", e);
        } catch (AttributeNotFoundException e) {
            throw new ClusterMgtException("Cannot get cluster node addresses. Check if clustering is enabled.", e);
        }
    }

    /**
     * Gets the broker's message store health.
     *
     * @return If messages store is broken, the exception string which cause the problem. Else empty string is returned.
     * @throws ClusterMgtException
     */
    public String getExceptionStringValue() throws ClusterMgtException {
        String storeHealth = StringUtils.EMPTY;
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            ObjectName objectName =
                    new ObjectName("org.wso2.andes:type=ClusterManagementInformation," +
                                   "name=ClusterManagementInformation");
            Object result = mBeanServer.getAttribute(objectName, ClusterMgtConstants.STORE_HEALTH);
            if (result != null) {
                storeHealth = (String) result;
            }
            return storeHealth;

        } catch (MalformedObjectNameException e) {
            throw new ClusterMgtException("Cannot get message store health.", e);
        } catch (ReflectionException e) {
            throw new ClusterMgtException("Cannot get coordinator node address.", e);
        } catch (MBeanException e) {
            throw new ClusterMgtException("Cannot get coordinator node address.", e);
        } catch (InstanceNotFoundException e) {
            throw new ClusterMgtException("Cannot get coordinator node address.", e);
        } catch (AttributeNotFoundException e) {
            throw new ClusterMgtException("Cannot get coordinator node address.", e);
        }
    }
}
