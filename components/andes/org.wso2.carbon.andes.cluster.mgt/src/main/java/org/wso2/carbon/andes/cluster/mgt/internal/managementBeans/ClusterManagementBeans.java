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

import org.wso2.carbon.andes.cluster.mgt.NodeDetail;
import org.wso2.carbon.andes.cluster.mgt.Queue;
import org.wso2.carbon.andes.cluster.mgt.Topic;
import org.wso2.carbon.andes.cluster.mgt.internal.ClusterMgtConstants;
import org.wso2.carbon.andes.cluster.mgt.internal.ClusterMgtException;
import org.wso2.carbon.andes.cluster.mgt.internal.Utils;


import javax.management.*;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

public class ClusterManagementBeans {

    public String getZookeeperAddressAndPort() throws ClusterMgtException {
        String result = "";
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            ObjectName objectName =
                    new ObjectName("org.wso2.andes:type=ClusterManagementInformation,name=127.0.0.1");
            Object MBeanResultForPort = mBeanServer.getAttribute(objectName,
                    ClusterMgtConstants.ZOOKEEPER_PORT_MBEAN_ATTRIB);
            Object MBeanResultForAddress = mBeanServer.getAttribute(objectName,
                    ClusterMgtConstants.ZOOKEEPER_ADDRESS_MBEAN_ATTRIB);
            if (MBeanResultForPort != null && MBeanResultForAddress != null) {
                int ZkPort = (Integer) MBeanResultForPort;
                String ZkAddress = (String) MBeanResultForAddress;
                result = ZkAddress + ":" + ZkPort;
            }
        } catch (MalformedObjectNameException e) {
            throw new ClusterMgtException("Cannot find the MBean for Zookeeper Port and Address", e);
        } catch (InstanceNotFoundException e) {
            result = "Cannot receive information";
            return result;
        } catch (ReflectionException e) {
            throw new ClusterMgtException("Cannot find the MBean for Zookeeper Port and Address", e);
        } catch (AttributeNotFoundException e) {
            throw new ClusterMgtException("Cannot find the MBean for Zookeeper Port and Address", e);
        } catch (MBeanException e) {
            throw new ClusterMgtException("Cannot find the MBean for Zookeeper Port and Address", e);
        }

        return result;
    }

    public String getCassandraAddressAndPort() {
        return "";
    }

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

    public String getIPAddressForNode(int nodeID) throws ClusterMgtException {
        String IPAddress = "";
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            ObjectName objectName =
                    new ObjectName("org.wso2.andes:type=ClusterManagementInformation," +
                            "name=ClusterManagementInformation");
            String operationName = "getIPAddressForNode";
            Object[] parameters = new Object[]{nodeID};
            String[] signature = new String[]{int.class.getName()};
            IPAddress = (String) mBeanServer.invoke(
                    objectName,
                    operationName,
                    parameters,
                    signature);
            return IPAddress;

        } catch (MalformedObjectNameException e) {
            throw new ClusterMgtException("Cannot access Ip address information for node " + nodeID, e);
        } catch (ReflectionException e) {
            throw new ClusterMgtException("Cannot access Ip address information for node " + nodeID, e);
        } catch (MBeanException e) {
            throw new ClusterMgtException("Cannot access Ip address information for node " + nodeID, e);
        } catch (InstanceNotFoundException e) {
            throw new ClusterMgtException("Cannot access Ip address information for node " + nodeID, e);
        }
    }

    public List<String> queuesOfCluster() throws ClusterMgtException {
        List<String> destinationQueuesOfCluster = new ArrayList<String>();
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            ObjectName objectName =
                    new ObjectName("org.wso2.andes:type=ClusterManagementInformation," +
                            "name=ClusterManagementInformation");
            Object result = mBeanServer.getAttribute(objectName, ClusterMgtConstants.QUEUES_OF_CLUSTER);

            if (result != null) {
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

    public ArrayList<NodeDetail> getNodesWithZookeeperID() throws ClusterMgtException {
        ArrayList<NodeDetail> nodeDetailsList = new ArrayList<NodeDetail>();
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            ObjectName objectName =
                    new ObjectName("org.wso2.andes:type=ClusterManagementInformation," +
                            "name=ClusterManagementInformation");
            Object result = mBeanServer.getAttribute(objectName, ClusterMgtConstants.ZOOKEEPER_NODES_MBEAN_ATTRIB);
            if (result != null) {
                List<Integer> ZkIDList = (List<Integer>) result;
                for (Integer zKID : ZkIDList) {
                    String zKIDString = Integer.toString(zKID);
                    NodeDetail aNodeDetail = new NodeDetail();
                    aNodeDetail.setZookeeperID(zKIDString);
                    aNodeDetail.setHostName(zKIDString);
                    aNodeDetail.setNumOfGlobalQueues(Utils.filterDomainSpecificQueues(getGlobalQueuesRunningInNode
                            (zKIDString)).size());
                    aNodeDetail.setIpAddress(getIPAddressForNode(zKID));
                    //aNodeDetail.setNumOfTopics(getTopicList().size());
                    nodeDetailsList.add(aNodeDetail);
                }
            }

            return nodeDetailsList;

        } catch (MalformedObjectNameException e) {
            throw new ClusterMgtException("Cannot access Zookeeper nodes", e);
        } catch (InstanceNotFoundException e) {
            throw new ClusterMgtException("Cannot access Zookeeper nodes", e);
        } catch (ReflectionException e) {
            throw new ClusterMgtException("Cannot access Zookeeper nodes", e);
        } catch (AttributeNotFoundException e) {
            throw new ClusterMgtException("Cannot access Zookeeper nodes", e);
        } catch (MBeanException e) {
            throw new ClusterMgtException("Cannot access Zookeeper nodes", e);
        }
    }

    public ArrayList<Topic> getTopicList() throws ClusterMgtException {
        ArrayList<Topic> topicDetailsList = new ArrayList<Topic>();
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            ObjectName objectName =
                    new ObjectName("org.wso2.andes:type=ClusterManagementInformation," +
                            "name=ClusterManagementInformation");
            Object result = mBeanServer.getAttribute(objectName, ClusterMgtConstants.TOPICS_MBEAN_ATTRIB);

            if (result != null) {
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

    public ArrayList<Queue> getGlobalQueuesRunningInNode(String nodeName) throws ClusterMgtException {
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

    public int getNumOfSubscribersForTopic(String topicName) throws ClusterMgtException {
        int numberOfSubscribers = 0;
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

    public int getNumberOfAllMessagesForQueue(String queueName) throws ClusterMgtException {
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            ObjectName objectName =
                    new ObjectName("org.wso2.andes:type=ClusterManagementInformation," +
                            "name=ClusterManagementInformation");
            String operationName = "getMessageCount";
            Object[] parameters = new Object[]{queueName};
            String[] signature = new String[]{String.class.getName()};

            int numberOfMessages = (Integer) mBeanServer.invoke(
                    objectName,
                    operationName,
                    parameters,
                    signature);
            return numberOfMessages;

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

    public boolean updateWorkerForQueue(String queueToUpdate, String newNodeToAssign) throws ClusterMgtException {

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
            throw new ClusterMgtException("Cannot access topic subscriber information", e);
        } catch (ReflectionException e) {
            throw new ClusterMgtException("Cannot access topic subscriber information", e);
        } catch (MBeanException e) {
            throw new ClusterMgtException("Cannot access topic subscriber information", e);
        } catch (InstanceNotFoundException e) {
            throw new ClusterMgtException("Cannot access topic subscriber information for node", e);
        }

        return operationResult;
    }

    public int getMessageCountOfNodeAddressedToDestinationQueue(String hostName, String destinationQueueName) throws
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
            throw new ClusterMgtException("Cannot count messages in Node Queue at node " + hostName, e);
        } catch (ReflectionException e) {
            throw new ClusterMgtException("Cannot count messages in Node Queue at node " + hostName, e);
        } catch (MBeanException e) {
            throw new ClusterMgtException("Cannot count messages in Node Queue at node " + hostName, e);
        } catch (InstanceNotFoundException e) {
            throw new ClusterMgtException("Cannot count messages in Node Queue at node " + hostName, e);
        }
    }

    public int getSubscriberCountOfNodeAddressedToDestinationQueue(String hostName,
                                                                   String destinationQueueName) throws
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
}
