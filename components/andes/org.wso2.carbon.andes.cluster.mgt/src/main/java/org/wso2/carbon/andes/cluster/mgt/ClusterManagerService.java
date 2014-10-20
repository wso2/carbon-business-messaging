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


    private static final Log log = LogFactory.getLog(ClusterManagerService.class);

    public Queue[] getAllDestinationQueuesDetailForNode(String hostName, int startingIndex, int maxQueueCount)
            throws ClusterMgtAdminException {

        try {
            Queue[] queueDetailsArray;
            int resultSetSize = maxQueueCount;
            ArrayList<Queue> resultList;

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
            throw new ClusterMgtAdminException("Can not get the queue manager.", e);
        }
    }

    /**
     * gives topics whole list of topics in the cluster
     *
     * @param startingIndex
     * @param maxTopicCount
     * @return array of Topic
     */
    public Topic[] getAllTopicsForNode(int startingIndex, int maxTopicCount) throws ClusterMgtAdminException {
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
            throw new ClusterMgtAdminException("Can not access MBean information for topics.", e);
        }
    }

    /**
     * get throughput for the requested node
     *
     * @param hostname
     * @return long
     */
    public long getThroughputForNode(String hostname) throws ClusterMgtAdminException {

        return 0;
    }

    /**
     * get memory usage for the requested node
     *
     * @param hostname
     * @return long
     */
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
            long result = 0;
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
     * @return long
     * @throws ClusterMgtAdminException
     */
    public long getNumberOfQueues() throws ClusterMgtAdminException {
        try {
            long result = 0;
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
     *
     * @param topicName
     * @return long
     */
    public long getNumberofSubscriptionsForTopic(String topicName) throws ClusterMgtAdminException {

        try {
            long numOfSubscribers = 0;
            ClusterManagementBeans clusterManagementBeans = new ClusterManagementBeans();
            numOfSubscribers = clusterManagementBeans.getNumOfSubscribersForTopic(topicName);
            return numOfSubscribers;
        } catch (Exception e) {
            throw new ClusterMgtAdminException("Cannot access MBean information for topics.", e);
        }
    }

    /**
     * @param queueName
     * @return long number of messages
     * @throws ClusterMgtAdminException
     */
    public long getNumberOfMessagesForQueue(String queueName) throws ClusterMgtAdminException {
        try {
            long numOfMessages = 0;
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
     * @param queueToUpdate
     * @param newNodeToAssign
     * @return success if assign was successful
     */
    public boolean updateWorkerForQueue(String queueToUpdate, String newNodeToAssign) throws ClusterMgtException {
        boolean result = false;
        ClusterManagementBeans clusterManagementBeans = new ClusterManagementBeans();
        result = clusterManagementBeans.updateWorkerForQueue(queueToUpdate, newNodeToAssign);
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


}
