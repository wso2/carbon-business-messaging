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
import org.wso2.carbon.andes.mgt.stub.types.carbon.Queue;
import org.wso2.carbon.andes.mgt.stub.types.carbon.Topic;

import java.rmi.RemoteException;

/**
 * This class is used to call MB Cluster Manager service from client side
 */
@SuppressWarnings("UnusedDeclaration")
public class ClusterManagerClient {

    private AndesManagerServiceStub stub;

    /**
     * Constructor for ClusterManagerClient
     *
     * @param configCtx configuration context for server
     * @param backendServerURL server backend url
     * @param cookie session cookie
     * @throws Exception
     */
    @SuppressWarnings("UnusedDeclaration")
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
     * Return address of the nodes in a cluster
     *
     * @return the addresses
     */
    public String[] getAllClusterNodeAddresses()
            throws AndesManagerServiceClusterMgtAdminExceptionException, RemoteException,
                   AndesManagerServiceClusterMgtExceptionException {

        return stub.getAllClusterNodeAddresses();
    }

    /**
     * Returns the coordinator node address
     *
     * @return the address
     */
    public String getCoordinatorNodeAddress()
            throws AndesManagerServiceClusterMgtAdminExceptionException, RemoteException {
        return stub.getCoordinatorNodeAddress();
    }

    /**
     * get global queues whose workers running in given host
     *
     * suppressed 'UnusedDeclaration' warning as it is used by queue_List.jsp
     *
     * @param hostName      node ID
     * @param startingIndex starting index of queues
     * @param maxQueueCount max num of queues to fetch
     * @return array of queues
     * @throws RemoteException
     * @throws AndesManagerServiceClusterMgtAdminExceptionException
     */
    @SuppressWarnings("UnusedDeclaration")
    public Queue[] getGlobalQueuesOfNode(String hostName, int startingIndex, int maxQueueCount)
            throws RemoteException, AndesManagerServiceClusterMgtAdminExceptionException {

        return stub.getAllGlobalQueuesForNode(hostName, startingIndex, maxQueueCount);
    }

    /**
     * gets all the destination queues
     *
     * suppressed 'UnusedDeclaration' warning as it is used by queue_List.jsp
     *
     * @param hostName node ID
     * @param startingIndex starting index of queues
     * @param maxQueueCount max num of queues to fetch
     * @return Array of queues
     * @throws RemoteException
     * @throws AndesManagerServiceClusterMgtAdminExceptionException
     */
    @SuppressWarnings("UnusedDeclaration")
    public Queue[] getDestinationQueues(String hostName, int startingIndex, int maxQueueCount)
            throws RemoteException, AndesManagerServiceClusterMgtAdminExceptionException {
        return stub.getAllDestinationQueuesDetailForNode(hostName, startingIndex, maxQueueCount);
    }

    /**
     * gives all the topics residing in the cluster
     *
     * suppressed 'UnusedDeclaration' warning as it is used by topic_List.jsp
     *
     * @param startingIndex starting index of topics
     * @param maxTopicCount maximum number of topics to capture
     * @return array of topics
     * @throws RemoteException
     */
    @SuppressWarnings("UnusedDeclaration")
    public Topic[] getAllTopics(int startingIndex, int maxTopicCount) throws RemoteException,
                                                                             AndesManagerServiceClusterMgtAdminExceptionException {
        return stub.getAllTopicsForNode(startingIndex, maxTopicCount);
    }

    /**
     * Update memory usage of the node to current
     *
     * suppressed 'UnusedDeclaration' warning as it is used by nodeUpdateQueries.jsp
     *
     * @param hostName the host name
     * @return long
     * @throws RemoteException
     */
    @SuppressWarnings("UnusedDeclaration")
    public long updateMemoryUsage(String hostName) throws RemoteException,
                                                          AndesManagerServiceClusterMgtAdminExceptionException {
        return stub.getMemoryUsage(hostName);
    }

    /**
     * update number of topics in the cluster
     * suppressed 'UnusedDeclaration' warning as it is used by nodeUpdateQueries.jsp
     * @return long
     * @throws RemoteException
     */
    @SuppressWarnings("UnusedDeclaration")
    public long updateTopicCount()
            throws RemoteException, AndesManagerServiceClusterMgtAdminExceptionException {
        return stub.getNumberOfTopics();
    }

    /**
     * update number of queues whose queue manager runs on the given node
     *
     * suppressed 'UnusedDeclaration' warning as it is used by queue_List.jsp, topic_List.jsp and nodeUpdateQueries.jsp
     *
     * @return the number of queues for a node
     * @throws RemoteException
     * @throws AndesManagerServiceClusterMgtAdminExceptionException
     */
    @SuppressWarnings("UnusedDeclaration")
    public long updateQueueCountForNode() throws RemoteException,
                                                 AndesManagerServiceClusterMgtAdminExceptionException {

        return stub.getNumberOfQueues();
    }

    /**
     * update the throughput for the requested node
     *
     * suppressed 'UnusedDeclaration' warning as it is used by nodeUpdateQueries.jsp
     *
     * @param hostName the host name
     * @return the throughput value
     * @throws RemoteException
     */
    @SuppressWarnings("UnusedDeclaration")
    public long updateThroughputForNode(String hostName) throws RemoteException,
                                                                AndesManagerServiceClusterMgtAdminExceptionException {
        return stub.getThroughputForNode(hostName);
    }

    /**
     * Reassign worker of a particular queue to another node
     *
     * @param queueToUpdate queue name to update
     * @param newNodeToAssign new node name to assign
     * @return update success or failed
     */
    public boolean updateWorkerForQueue(String queueToUpdate, String newNodeToAssign)
            throws RemoteException,
                   AndesManagerServiceClusterMgtExceptionException,
                   AndesManagerServiceClusterMgtAdminExceptionException {
        return stub.updateWorkerForQueue(queueToUpdate, newNodeToAssign);
    }

    /**
     * check if broker is running in clustered mode
     *
     * @return whether clustering is enabled
     * @throws AndesManagerServiceClusterMgtExceptionException
     * @throws RemoteException
     */
    public boolean isClusteringEnabled()
            throws AndesManagerServiceClusterMgtExceptionException, RemoteException {
        return stub.isClusteringEnabled();
    }

    /**
     * get node ID assigned to this node by Zookeeper
     *
     * @return the current node's ID
     * @throws AndesManagerServiceClusterMgtExceptionException
     * @throws RemoteException
     */
    public String getMyNodeID()
            throws AndesManagerServiceClusterMgtExceptionException, RemoteException {
        return stub.getMyNodeID();
    }
}
