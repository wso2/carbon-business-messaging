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

package org.wso2.carbon.andes.cluster.mgt.ui;

import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.wso2.carbon.andes.mgt.stub.AndesManagerServiceClusterMgtAdminExceptionException;
import org.wso2.carbon.andes.mgt.stub.AndesManagerServiceClusterMgtExceptionException;
import org.wso2.carbon.andes.mgt.stub.AndesManagerServiceStub;
import org.wso2.carbon.andes.mgt.stub.types.carbon.NodeDetail;
import org.wso2.carbon.andes.mgt.stub.types.carbon.Queue;
import org.wso2.carbon.andes.mgt.stub.types.carbon.Topic;

import java.rmi.RemoteException;

/**
 * This class is used to call MB Cluster Manager service from client side
 */
public class ClusterManagerClient {

    private AndesManagerServiceStub stub;

    /**
     * Constructor for ClusterManagerClient
     *
     * @param configCtx
     * @param backendServerURL
     * @param cookie
     * @throws Exception
     */
    public ClusterManagerClient(ConfigurationContext configCtx, String backendServerURL,
                                String cookie) throws Exception {
        String serviceURL = backendServerURL + "AndesManagerService";
        stub = new AndesManagerServiceStub(configCtx, serviceURL);
        ServiceClient client = stub._getServiceClient();
        Options options = client.getOptions();
        options.setManageSession(true);
        options.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, cookie);
    }

    /**
     * gives complete nodes list
     *
     * @param startingIndex
     * @param maxMessageBoxesCount
     * @return
     * @throws RemoteException
     */
    public NodeDetail[] getAllNodeDetail(int startingIndex, int maxMessageBoxesCount)
            throws RemoteException, AndesManagerServiceClusterMgtAdminExceptionException {

        NodeDetail[] result = stub.getAllNodeDetail(startingIndex, maxMessageBoxesCount);
        return result;
    }

    /**
     * returns number of nodes in the cluster
     *
     * @return int
     * @throws RemoteException
     */
    public int getNumOfNodes() throws RemoteException, AndesManagerServiceClusterMgtAdminExceptionException {
        return stub.getNumOfNodes();
    }

    /**
     * get global queues whose workers running in given host
     *
     * @param hostName      node ID
     * @param startingIndex starting index of queues
     * @param maxQueueCount max num of queues to fetch
     * @return Array of Queues
     * @throws RemoteException
     * @throws AndesManagerServiceClusterMgtAdminExceptionException
     */
    public Queue[] getGlobalQueuesOfNode(String hostName, int startingIndex, int maxQueueCount)
            throws RemoteException, AndesManagerServiceClusterMgtAdminExceptionException {

        Queue[] result = stub.getAllGlobalQueuesForNode(hostName, startingIndex, maxQueueCount);
        return result;
    }

    public Queue[] getDestinationQueues(String hostName, int startingIndex, int maxQueueCount)
            throws RemoteException, AndesManagerServiceClusterMgtAdminExceptionException {
        Queue[] result = stub.getAllDestinationQueuesDetailForNode(hostName, startingIndex, maxQueueCount);
        return result;
    }

    /**
     * gives all the topics residing in the cluster
     *
     * @param startingIndex
     * @param maxTopicCount
     * @return
     * @throws RemoteException
     */
    public Topic[] getAllTopics(int startingIndex, int maxTopicCount) throws RemoteException,
            AndesManagerServiceClusterMgtAdminExceptionException {
        return stub.getAllTopicsForNode(startingIndex, maxTopicCount);
    }

    //TO DELETE
    public long updateNumOfSubscriptionsForTopic(String topicName) throws RemoteException,
            AndesManagerServiceClusterMgtAdminExceptionException {
        return stub.getNumberofSubscriptionsForTopic(topicName);
    }

    /**
     * Update memory usage of the node to current
     *
     * @param hostName
     * @return long
     * @throws RemoteException
     */
    public long updateMemoryUsage(String hostName) throws RemoteException,
            AndesManagerServiceClusterMgtAdminExceptionException {
        return stub.getMemoryUsage(hostName);
    }

    /**
     * update number of topics in the cluster
     *
     * @return long
     * @throws RemoteException
     */
    public long updateTopicCount() throws RemoteException, AndesManagerServiceClusterMgtAdminExceptionException {
        return stub.getNumberOfTopics();
    }

    public long updateNumOfMessagesForQueue(String queueName) throws
            AndesManagerServiceClusterMgtAdminExceptionException, RemoteException {
        return stub.getNumberOfMessagesForQueue(queueName);
    }

    /**
     * update number of queues whose queue manager runs on the given node
     *
     * @return
     * @throws RemoteException
     * @throws AndesManagerServiceClusterMgtAdminExceptionException
     */
    public long updateQueueCountForNode() throws RemoteException,
            AndesManagerServiceClusterMgtAdminExceptionException {

        return stub.getNumberOfQueues();
    }

    /**
     * update the throughput for the requested node
     *
     * @param hostName
     * @return
     * @throws RemoteException
     */
    public long updateThroughputForNode(String hostName) throws RemoteException,
            AndesManagerServiceClusterMgtAdminExceptionException {
        return stub.getThroughputForNode(hostName);
    }

    /**
     * Restart the node requested
     *
     * @param hostName
     * @return success
     */
    public boolean restartNode(String hostName) {
        return true;
    }

    /**
     * Get current cassandra connection ip:port
     *
     * @return
     * @throws RemoteException
     */
    public String getCassandraConnection() throws RemoteException,
            AndesManagerServiceClusterMgtAdminExceptionException {
        return stub.getCassandraConnection();
    }

    /**
     * Get current zookeeper connection ip:port
     *
     * @return
     * @throws RemoteException
     */
    public String getZookeeperConnection() throws RemoteException,
            AndesManagerServiceClusterMgtAdminExceptionException {
        return stub.getZookeeperConnection();
    }

    /**
     * Reassign worker of a particular queue to another node
     *
     * @param queueToUpdate
     * @param newNodeToAssign
     * @return
     */
    public boolean updateWorkerForQueue(String queueToUpdate, String newNodeToAssign) throws RemoteException,
            AndesManagerServiceClusterMgtExceptionException {
        boolean result = stub.updateWorkerForQueue(queueToUpdate, newNodeToAssign);
        return result;
    }

    /**
     * check if broker is running in clustered mode
     *
     * @return
     * @throws AndesManagerServiceClusterMgtExceptionException
     * @throws RemoteException
     */
    public boolean isClusteringEnabled() throws AndesManagerServiceClusterMgtExceptionException, RemoteException {
        boolean result = stub.isClusteringEnabled();
        return result;
    }

    /**
     * get node ID assigned to this node by Zookeeper
     *
     * @return
     * @throws AndesManagerServiceClusterMgtExceptionException
     * @throws RemoteException
     */
    public String getMyNodeID() throws AndesManagerServiceClusterMgtExceptionException, RemoteException {
        return stub.getMyNodeID();
    }

    /**
     * get total subscription count for all queues in cluster of a given node
     *
     * @param hostName
     * @return total subscription count
     * @throws AndesManagerServiceClusterMgtAdminExceptionException
     * @throws RemoteException
     */
    public int getTotalSubscriptionCountForNode(String hostName) throws
            AndesManagerServiceClusterMgtAdminExceptionException, RemoteException {
        int totalSubscriptionCount = 0;
        Queue[] queueList = stub.getAllDestinationQueuesDetailForNode(hostName, 0, Integer.MAX_VALUE);
        if (queueList == null) {
            return totalSubscriptionCount;
        }
        for (Queue aQueue : queueList) {
            int subscriptionCountForCurrentQueue = aQueue.getSubscriberCount();
            totalSubscriptionCount += subscriptionCountForCurrentQueue;
        }
        return totalSubscriptionCount;
    }
}
