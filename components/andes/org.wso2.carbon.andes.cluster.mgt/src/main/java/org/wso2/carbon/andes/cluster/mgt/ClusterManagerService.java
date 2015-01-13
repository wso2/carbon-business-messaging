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


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.andes.cluster.mgt.internal.ClusterMgtException;
import org.wso2.carbon.andes.cluster.mgt.internal.Utils;
import org.wso2.carbon.andes.cluster.mgt.internal.managementBeans.ClusterManagementBeans;

import java.util.ArrayList;
import java.util.List;

/**
 * Admin service class for cluster management
 */
public class ClusterManagerService {

    /**
     * Logging service
     */
    private static final Log log = LogFactory.getLog(ClusterManagerService.class);

    /**
     * Gets the IP addresses and ports of the nodes in a cluster
     *
     * @return A list of addresses of the nodes in a cluster
     * @throws ClusterMgtAdminException
     */
    public String[] getAllClusterNodeAddresses() throws ClusterMgtAdminException {
        ClusterManagementBeans clusterManagementBeans = new ClusterManagementBeans();
        List<String> addresses = null;
        try {
            addresses = clusterManagementBeans.getAllClusterNodeAddresses();
        } catch (ClusterMgtException e) {
            e.printStackTrace();
        }
        if (addresses != null) {
            return addresses.toArray(new String[addresses.size()]);
        } else {
            return new String[]{};
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
        } catch (Exception e) {
            throw new ClusterMgtAdminException("Cannot get coordinator node address. Check if clustering is enabled.", e);
        }
    }

    /**
     * Gets all the details of destination queues for a node
     * <p/>
     * suppressed 'UnusedDeclaration' warning as it is invoked by a service
     *
     * @param hostName      the host name
     * @param startingIndex starting index of details
     * @param maxQueueCount maximum queues to return
     * @return array of queues
     * @throws ClusterMgtAdminException
     */
    @SuppressWarnings("UnusedDeclaration")
    public Queue[] getAllDestinationQueuesDetailForNode(String hostName, int startingIndex,
                                                        int maxQueueCount)
            throws ClusterMgtAdminException {

        try {
            Queue[] queueDetailsArray;
            int resultSetSize = maxQueueCount;

            ClusterManagementBeans clusterManagementBeans = new ClusterManagementBeans();
            List<String> queuesOfCluster = clusterManagementBeans.queuesOfCluster();
            List<Queue> queueObjectList = new ArrayList<Queue>();
            for (String destinationQueue : queuesOfCluster) {
                Queue aQueue = new Queue();
                aQueue.setQueueName(destinationQueue);
                queueObjectList.add(aQueue);
            }
            //filter according to tenant
            ArrayList<Queue> queuesSpecificToTenant = (ArrayList<Queue>) Utils.filterDomainSpecificQueues
                    (queueObjectList);

            if ((queuesSpecificToTenant.size() - startingIndex) < maxQueueCount) {
                resultSetSize = (queuesSpecificToTenant.size() - startingIndex);
            }
            for (Queue aQueue : queuesSpecificToTenant) {
                //get number of messages in node queue and set
                aQueue.setMessageCount(clusterManagementBeans.
                        getMessageCountOfNodeAddressedToDestinationQueue(hostName, aQueue.getQueueName()));

                //get number of subscribers and set
                aQueue.setSubscriberCount(clusterManagementBeans.
                        getSubscriberCountOfNodeAddressedToDestinationQueue(hostName, aQueue.getQueueName()));
            }
            queueDetailsArray = new Queue[resultSetSize];
            int index = 0;
            int queueDetailsIndex = 0;
            for (Queue queueDetail : queuesSpecificToTenant) {
                if (startingIndex == index || startingIndex < index) {
                    queueDetailsArray[queueDetailsIndex] = new Queue();

                    queueDetailsArray[queueDetailsIndex].setQueueName(queueDetail.getQueueName());
                    queueDetailsArray[queueDetailsIndex].setMessageCount(queueDetail.getMessageCount());
                    queueDetailsArray[queueDetailsIndex].setSubscriberCount(queueDetail.getSubscriberCount());

                    queueDetailsIndex++;
                    if (queueDetailsIndex == maxQueueCount) {
                        break;
                    }

                }
                index++;
            }
            return queueDetailsArray;

        } catch (Exception e) {
            throw new ClusterMgtAdminException("Cannot get all the details of destination queues for a node.", e);
        }
    }

    /**
     * Gets topics whole list of topics in the cluster
     * <p/>
     * suppressed 'UnusedDeclaration' warning as it is invoked by a service
     *
     * @param startingIndex starting index of details
     * @param maxTopicCount maximum number of topics to return
     * @return array of topics
     */
    @SuppressWarnings("UnusedDeclaration")
    public Topic[] getAllTopicsForNode(int startingIndex, int maxTopicCount)
            throws ClusterMgtAdminException {
        try {
            Topic[] topicDetailsArray;
            ClusterManagementBeans clusterManagementBeans = new ClusterManagementBeans();
            ArrayList<Topic> temp = clusterManagementBeans.getTopicList();

            int resultSetSize = maxTopicCount;

            if ((temp.size() - startingIndex) < maxTopicCount) {
                resultSetSize = (temp.size() - startingIndex);
            }
            topicDetailsArray = new Topic[resultSetSize];
            int index = 0;
            int topicDetailDetailsIndex = 0;
            for (Topic topicDetail : temp) {
                if (startingIndex == index || startingIndex < index) {
                    topicDetailsArray[topicDetailDetailsIndex] = new Topic();
                    topicDetailsArray[topicDetailDetailsIndex].setNumberOfSubscribers(topicDetail
                                                                                              .getNumberOfSubscribers());
                    topicDetailsArray[topicDetailDetailsIndex].setName(topicDetail.getName());
                    topicDetailDetailsIndex++;
                    if (topicDetailDetailsIndex == maxTopicCount) {
                        break;
                    }
                }
                index++;
            }

            return topicDetailsArray;

        } catch (Exception e) {
            throw new ClusterMgtAdminException("Cannot access MBean information for topics.", e);
        }
    }

    /**
     * get throughput for the requested node
     * <p/>
     * suppressed 'UnusedDeclaration' warning as it is invoked by a service
     *
     * @param hostname the host name
     * @return long
     */
    @SuppressWarnings("UnusedDeclaration")
    public long getThroughputForNode(String hostname) throws ClusterMgtAdminException {
        // TODO : to be implemented
        return 0;
    }

    /**
     * get memory usage for the requested node
     * <p/>
     * suppressed 'UnusedDeclaration' warning as it is invoked by a service
     *
     * @param hostname the host name
     * @return long
     */
    @SuppressWarnings("UnusedDeclaration")
    public long getMemoryUsage(String hostname) throws ClusterMgtAdminException {
        return 0;
    }

    /**
     * get current number of topics those have one
     * or more subscribers subscribed to that topic on the given node
     *
     * @return long
     */
    public long getNumberOfTopics() throws ClusterMgtAdminException {
        try {
            long result;
            ClusterManagementBeans clusterManagementBeans = new ClusterManagementBeans();
            ArrayList<Topic> topicList = clusterManagementBeans.getTopicList();
            result = topicList.size();
            return result;
        } catch (Exception e) {
            throw new ClusterMgtAdminException("Cannot access MBean information for topics.", e);
        }
    }

    /**
     * gives number queues whose queue manager runs on the given node
     *
     * @return the number of queues
     * @throws ClusterMgtAdminException
     */
    public long getNumberOfQueues() throws ClusterMgtAdminException {
        try {
            long result;
            ClusterManagementBeans clusterManagementBeans = new ClusterManagementBeans();
            List<String> queuesOfCluster = clusterManagementBeans.queuesOfCluster();
            List<Queue> queueObjectList = new ArrayList<Queue>();
            for (String destinationQueue : queuesOfCluster) {
                Queue aQueue = new Queue();
                aQueue.setQueueName(destinationQueue);
                queueObjectList.add(aQueue);
            }
            //filter according to tenant
            ArrayList<Queue> queuesSpecificToTenant = (ArrayList<Queue>) Utils.filterDomainSpecificQueues
                    (queueObjectList);
            result = queuesSpecificToTenant.size();
            return result;
        } catch (Exception e) {
            throw new ClusterMgtAdminException("Cannot get the queue manager.", e);
        }

    }

    /**
     * Returns number of subscriptions for the topic
     * <p/>
     * suppressed 'UnusedDeclaration' warning as it is invoked by a service
     *
     * @param topicName topic name
     * @return the number of subscriptions
     * @throws ClusterMgtAdminException
     */
    @SuppressWarnings("UnusedDeclaration")
    public long getNumberOfSubscriptionsForTopic(String topicName) throws ClusterMgtAdminException {

        try {
            long numOfSubscribers;
            ClusterManagementBeans clusterManagementBeans = new ClusterManagementBeans();
            numOfSubscribers = clusterManagementBeans.getNumOfSubscribersForTopic(topicName);
            return numOfSubscribers;
        } catch (Exception e) {
            throw new ClusterMgtAdminException("Cannot access MBean information for topics.", e);
        }
    }

    /**
     * Gets the number of messages for a queue
     * <p/>
     * suppressed 'UnusedDeclaration' warning as it is invoked by a service
     *
     * @param queueName queue name
     * @return long number of messages
     * @throws ClusterMgtAdminException
     */
    @SuppressWarnings("UnusedDeclaration")
    public long getNumberOfMessagesForQueue(String queueName) throws ClusterMgtAdminException {
        try {
            long numOfMessages;
            ClusterManagementBeans clusterManagementBeans = new ClusterManagementBeans();
            numOfMessages = clusterManagementBeans.getNumberOfAllMessagesForQueue(queueName);
            return numOfMessages;
        } catch (Exception e) {
            throw new ClusterMgtAdminException("Cannot access MBean information for queues.", e);
        }
    }

    /**
     * Reassign worker of a particular queue to another node
     *
     * @param queueToUpdate   name of the queue to update
     * @param newNodeToAssign the new node to assign to
     * @return success if assign was successful
     * @throws ClusterMgtAdminException
     */
    public boolean updateWorkerForQueue(String queueToUpdate, String newNodeToAssign)
            throws ClusterMgtAdminException {
        boolean result;
        try {
            ClusterManagementBeans clusterManagementBeans = new ClusterManagementBeans();
            result = clusterManagementBeans.updateWorkerForQueue(queueToUpdate, newNodeToAssign);
            log.info("Updated worker for " + queueToUpdate);
        } catch (ClusterMgtException e) {
            throw new ClusterMgtAdminException("Cannot access MBean information for queues.", e);
        }
        return result;
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
     * gives queues whose queue manager runs on the given node
     * <p/>
     * suppressed 'UnusedDeclaration' warning as it is invoked by a service
     *
     * @param hostName      the host name
     * @param startingIndex the starting index
     * @param maxQueueCount maximum queue count
     * @return Array of Queues
     * @throws ClusterMgtAdminException
     */
    @SuppressWarnings("UnusedDeclaration")
    public Queue[] getAllGlobalQueuesForNode(String hostName, int startingIndex, int maxQueueCount)
            throws ClusterMgtAdminException {

        try {
            Queue[] queueDetailsArray;
            int resultSetSize = maxQueueCount;
            ArrayList<Queue> resultList;

            //get queues  whose queue manager runs on the given node
            ClusterManagementBeans clusterManagementBeans = new ClusterManagementBeans();
            ArrayList<Queue> allGlobalQueuesRunningInNode = clusterManagementBeans.getGlobalQueuesRunningInNode(hostName);

            //filter queues according to tenant
            resultList = (ArrayList<Queue>) Utils.filterDomainSpecificQueues(allGlobalQueuesRunningInNode);

            if ((resultList.size() - startingIndex) < maxQueueCount) {
                resultSetSize = (resultList.size() - startingIndex);
            }
            queueDetailsArray = new Queue[resultSetSize];
            int index = 0;
            int queueDetailsIndex = 0;
            for (Queue queueDetail : resultList) {
                if (startingIndex == index || startingIndex < index) {
                    queueDetailsArray[queueDetailsIndex] = new Queue();

                    queueDetailsArray[queueDetailsIndex].setQueueName(queueDetail.getQueueName());
                    queueDetailsArray[queueDetailsIndex].setMessageCount(queueDetail.getMessageCount());

                    //queueDetailsArray[queueDetailsIndex].setQueueDepth(queueDetail.getQueueDepth());
                    //queueDetailsArray[queueDetailsIndex].setUpdatedTime(queueDetail.getUpdatedTime());
                    //queueDetailsArray[queueDetailsIndex].setCreatedTime(queueDetail.getCreatedTime());

                    queueDetailsIndex++;
                    if (queueDetailsIndex == maxQueueCount) {
                        break;
                    }

                }

                index++;
            }

            return queueDetailsArray;

        } catch (Exception e) {
            throw new ClusterMgtAdminException("Cannot get the queue manager ", e);
        }
    }
}
