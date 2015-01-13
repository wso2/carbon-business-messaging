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
import org.wso2.carbon.andes.cluster.mgt.Queue;
import org.wso2.carbon.andes.cluster.mgt.Topic;
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
     * Gets the queues in a cluster
     *
     * @return a list of queues available in the cluster
     * @throws ClusterMgtException
     */
    public List<String> queuesOfCluster() throws ClusterMgtException {
        List<String> destinationQueuesOfCluster = new ArrayList<String>();
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            ObjectName objectName =
                    new ObjectName("org.wso2.andes:type=ClusterManagementInformation," +
                                   "name=ClusterManagementInformation");
            Object result = mBeanServer.getAttribute(objectName, ClusterMgtConstants.QUEUES_OF_CLUSTER);

            if (result != null) {
                //noinspection unchecked
                destinationQueuesOfCluster = (List<String>) result;
            }
            return destinationQueuesOfCluster;

        } catch (MalformedObjectNameException e) {
            throw new ClusterMgtException("Cannot get queues of cluster", e);
        } catch (InstanceNotFoundException e) {
            throw new ClusterMgtException("Cannot get queues of cluster", e);
        } catch (ReflectionException e) {
            throw new ClusterMgtException("Cannot get queues of cluster", e);
        } catch (AttributeNotFoundException e) {
            throw new ClusterMgtException("Cannot get queues of cluster", e);
        } catch (MBeanException e) {
            throw new ClusterMgtException("Cannot get queues of cluster", e);
        }
    }

    /**
     * Gets the topics in a cluster
     *
     * @return a list of topics available in the cluster
     * @throws ClusterMgtException
     */
    public ArrayList<Topic> getTopicList() throws ClusterMgtException {
        ArrayList<Topic> topicDetailsList = new ArrayList<Topic>();
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            ObjectName objectName =
                    new ObjectName("org.wso2.andes:type=ClusterManagementInformation," +
                                   "name=ClusterManagementInformation");
            Object result = mBeanServer.getAttribute(objectName, ClusterMgtConstants.TOPICS_MBEAN_ATTRIB);

            if (result != null) {
                //noinspection unchecked
                List<String> TopicNamesList = (List<String>) result;

                for (String topicName : TopicNamesList) {
                    Topic aTopic = new Topic();
                    aTopic.setName(topicName);
                    aTopic.setNumberOfSubscribers(getNumOfSubscribersForTopic(topicName));
                    topicDetailsList.add(aTopic);
                }

            }

            return topicDetailsList;
        } catch (MalformedObjectNameException e) {
            throw new ClusterMgtException("Cannot access topic information", e);
        } catch (InstanceNotFoundException e) {
            throw new ClusterMgtException("Cannot access topic information", e);
        } catch (ReflectionException e) {
            throw new ClusterMgtException("Cannot access topic information", e);
        } catch (AttributeNotFoundException e) {
            throw new ClusterMgtException("Cannot access topic information", e);
        } catch (MBeanException e) {
            throw new ClusterMgtException("Cannot access topic information", e);
        }
    }

    /**
     * Gets global queues running in the node
     *
     * @param nodeName the node name
     * @return a list of queues
     * @throws ClusterMgtException
     */
    public ArrayList<Queue> getGlobalQueuesRunningInNode(String nodeName)
            throws ClusterMgtException {
        int nodeId = Integer.parseInt(nodeName);
        ArrayList<Queue> queueDetailsList = new ArrayList<Queue>();
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            ObjectName objectName =
                    new ObjectName("org.wso2.andes:type=ClusterManagementInformation," +
                                   "name=ClusterManagementInformation");
            String operationName = "getGlobalQueuesAssigned";
            Object[] parameters = new Object[]{nodeId};
            String[] signature = new String[]{int.class.getName()};
            Object result = mBeanServer.invoke(
                    objectName,
                    operationName,
                    parameters,
                    signature);
            if (result != null) {
                String[] queueNamesArray = (String[]) result;
                for (String queueName : queueNamesArray) {
                    Queue aQueue = new Queue();
                    aQueue.setQueueName(queueName);
                    aQueue.setMessageCount(getNumberOfAllMessagesForQueue(queueName));
                    queueDetailsList.add(aQueue);
                }
            }

            return queueDetailsList;

        } catch (MalformedObjectNameException e) {
            throw new ClusterMgtException("Cannot access global queue information", e);
        } catch (ReflectionException e) {
            throw new ClusterMgtException("Cannot access global queue information", e);
        } catch (MBeanException e) {
            throw new ClusterMgtException("Cannot access global queue information", e);
        } catch (InstanceNotFoundException e) {
            throw new ClusterMgtException("Cannot access global queue information", e);
        }
    }

    /**
     * Gets the number of subscribers for a topic
     *
     * @param topicName the topic name
     * @return the number of topics
     * @throws ClusterMgtException
     */
    public int getNumOfSubscribersForTopic(String topicName) throws ClusterMgtException {
        int numberOfSubscribers;
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            ObjectName objectName =
                    new ObjectName("org.wso2.andes:type=ClusterManagementInformation," +
                                   "name=ClusterManagementInformation");
            String operationName = "getSubscriberCount";
            Object[] parameters = new Object[]{topicName};
            String[] signature = new String[]{String.class.getName()};

            numberOfSubscribers = (Integer) mBeanServer.invoke(
                    objectName,
                    operationName,
                    parameters,
                    signature);
            return numberOfSubscribers;

        } catch (MalformedObjectNameException e) {
            throw new ClusterMgtException("Cannot access topic subscriber information", e);
        } catch (ReflectionException e) {
            throw new ClusterMgtException("Cannot access topic subscriber information", e);
        } catch (MBeanException e) {
            throw new ClusterMgtException("Cannot access topic subscriber information", e);
        } catch (InstanceNotFoundException e) {
            throw new ClusterMgtException("Cannot access topic subscriber information", e);
        }
    }

    /**
     * Gets the number of all the messages for a queue
     *
     * @param queueName the queue name
     * @return the number of messages
     * @throws ClusterMgtException
     */
    public int getNumberOfAllMessagesForQueue(String queueName) throws ClusterMgtException {
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            ObjectName objectName =
                    new ObjectName("org.wso2.andes:type=ClusterManagementInformation," +
                                   "name=ClusterManagementInformation");
            String operationName = "getMessageCount";
            Object[] parameters = new Object[]{queueName};
            String[] signature = new String[]{String.class.getName()};

            return (Integer) mBeanServer.invoke(
                    objectName,
                    operationName,
                    parameters,
                    signature);

        } catch (MalformedObjectNameException e) {
            throw new ClusterMgtException("Cannot access queue information", e);
        } catch (ReflectionException e) {
            throw new ClusterMgtException("Cannot access queue information", e);
        } catch (MBeanException e) {
            throw new ClusterMgtException("Cannot access queue information", e);
        } catch (InstanceNotFoundException e) {
            throw new ClusterMgtException("Cannot access queue information", e);
        }
    }

    /**
     * Updates the worker for a queue
     *
     * @param queueToUpdate the queue to update
     * @param newNodeToAssign the new node to assign
     * @return whether assigning succeeded or failed
     * @throws ClusterMgtException
     */
    public boolean updateWorkerForQueue(String queueToUpdate, String newNodeToAssign)
            throws ClusterMgtException {

        boolean operationResult = false;
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            ObjectName objectName =
                    new ObjectName("org.wso2.andes:type=ClusterManagementInformation," +
                                   "name=ClusterManagementInformation");
            String operationName = "updateWorkerForQueue";
            Object[] parameters = new Object[]{queueToUpdate, newNodeToAssign};
            String[] signature = new String[]{String.class.getName(), String.class.getName()};
            Object result = mBeanServer.invoke(
                    objectName,
                    operationName,
                    parameters,
                    signature);
            if (result != null) {
                operationResult = (Boolean) result;
            }

        } catch (MalformedObjectNameException e) {
            throw new ClusterMgtException("Cannot update worker for queue", e);
        } catch (ReflectionException e) {
            throw new ClusterMgtException("Cannot update worker for queue", e);
        } catch (MBeanException e) {
            throw new ClusterMgtException("Cannot update worker for queue", e);
        } catch (InstanceNotFoundException e) {
            throw new ClusterMgtException("Cannot update worker for queue", e);
        }

        return operationResult;
    }

    /**
     * Gets the message count of a node addressed to a destination queue
     *
     * @param hostName the host name
     * @param destinationQueueName the destination queue name
     * @return the number of messages
     * @throws ClusterMgtException
     */
    public int getMessageCountOfNodeAddressedToDestinationQueue(String hostName,
                                                                String destinationQueueName) throws
                                                                                             ClusterMgtException {
        int nodeId = Integer.parseInt(hostName);
        int messageCount = 0;
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            ObjectName objectName =
                    new ObjectName("org.wso2.andes:type=ClusterManagementInformation," +
                                   "name=ClusterManagementInformation");
            String operationName = "getNodeQueueMessageCount";
            Object[] parameters = new Object[]{nodeId, destinationQueueName};
            String[] signature = new String[]{int.class.getName(), String.class.getName()};
            Object result = mBeanServer.invoke(
                    objectName,
                    operationName,
                    parameters,
                    signature);
            if (result != null) {
                messageCount = (Integer) result;
            }

            return messageCount;

        } catch (MalformedObjectNameException e) {
            throw new ClusterMgtException("Cannot count messages in the mentioned destination queue at node " + hostName, e);
        } catch (ReflectionException e) {
            throw new ClusterMgtException("Cannot count messages in the mentioned destination queue at node " + hostName, e);
        } catch (MBeanException e) {
            throw new ClusterMgtException("Cannot count messages in the mentioned destination queue at node " + hostName, e);
        } catch (InstanceNotFoundException e) {
            throw new ClusterMgtException("Cannot count messages in the mentioned destination queue at node " + hostName, e);
        }
    }

    /**
     * Gets the subscriber count of a node addressed to a destination queue
     *
     * @param hostName the host name
     * @param destinationQueueName the destination queue name
     * @return the subscriber count
     * @throws ClusterMgtException
     */
    public int getSubscriberCountOfNodeAddressedToDestinationQueue(String hostName,
                                                                   String destinationQueueName)
            throws
            ClusterMgtException {
        int nodeId = Integer.parseInt(hostName);
        int subscriberCount = 0;
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            ObjectName objectName =
                    new ObjectName("org.wso2.andes:type=ClusterManagementInformation," +
                                   "name=ClusterManagementInformation");
            String operationName = "getNodeQueueSubscriberCount";
            Object[] parameters = new Object[]{nodeId, destinationQueueName};
            String[] signature = new String[]{int.class.getName(), String.class.getName()};
            Object result = mBeanServer.invoke(
                    objectName,
                    operationName,
                    parameters,
                    signature);
            if (result != null) {
                subscriberCount = (Integer) result;
            }

            return subscriberCount;

        } catch (MalformedObjectNameException e) {
            throw new ClusterMgtException("Cannot access subscriber count for queue " + destinationQueueName, e);
        } catch (ReflectionException e) {
            throw new ClusterMgtException("Cannot access subscriber count for queue " + destinationQueueName, e);
        } catch (MBeanException e) {
            throw new ClusterMgtException("Cannot access subscriber count for queue " + destinationQueueName, e);
        } catch (InstanceNotFoundException e) {
            throw new ClusterMgtException("Cannot access subscriber count for queue " + destinationQueueName, e);
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
            Object result = mBeanServer.getAttribute(objectName, "CoordinatorNodeAddress");
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
            Object result = mBeanServer.getAttribute(objectName, "AllClusterNodeAddresses");

            if (result != null) {
                //noinspection unchecked
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
}
