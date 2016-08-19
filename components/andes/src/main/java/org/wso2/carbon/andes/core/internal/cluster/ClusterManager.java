/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.andes.core.internal.cluster;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.andes.core.Andes;
import org.wso2.carbon.andes.core.AndesException;
import org.wso2.carbon.andes.core.internal.AndesContext;
import org.wso2.carbon.andes.core.internal.cluster.coordination.CoordinationConstants;
import org.wso2.carbon.andes.core.internal.configuration.AndesConfigurationManager;
import org.wso2.carbon.andes.core.internal.configuration.enums.AndesConfiguration;
import org.wso2.carbon.andes.core.internal.slot.SlotManagerClusterMode;
import org.wso2.carbon.andes.core.internal.slot.SlotMessageCounter;
import org.wso2.carbon.andes.core.store.AndesContextStore;
import org.wso2.carbon.andes.core.store.FailureObservingStoreManager;
import org.wso2.carbon.andes.core.store.HealthAwareStore;
import org.wso2.carbon.andes.core.store.StoreHealthListener;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * Cluster Manager is responsible for Handling the Broker Cluster Management Tasks like
 * Queue Worker distribution. Fail over handling for cluster nodes. etc.
 */
public class ClusterManager implements StoreHealthListener {

    /**
     * Class logger
     */
    private Log log = LogFactory.getLog(ClusterManager.class);

    /**
     * Id of the local node
     */
    private String nodeId;

    /**
     * AndesContextStore instance
     */
    private AndesContextStore andesContextStore;

    /**
     * Cluster agent for managing cluster communication
     */
    private ClusterAgent clusterAgent;

    /**
     * This is the cached value of the operational status of the store
     */
    private boolean storeOperational;

    private ScheduledExecutorService slotRecoveryTaskService;

    public static final int SLOT_SUBMIT_TASK_POOL_SIZE = 1;

    /**
     * Create a ClusterManager instance
     */
    public ClusterManager() {
        this.andesContextStore = AndesContext.getInstance().getAndesContextStore();
    }

    /**
     * Initialize the Cluster manager.
     *
     * @throws AndesException
     */
    public void init() throws AndesException {

        if (AndesContext.getInstance().isClusteringEnabled()) {
            initClusterMode();
            ThreadFactory namedThreadFactory = new ThreadFactoryBuilder()
                    .setNameFormat("SlotRecoveryOneTimeTask-%d").build();
            slotRecoveryTaskService = Executors.newScheduledThreadPool(SLOT_SUBMIT_TASK_POOL_SIZE, namedThreadFactory);
        } else {
            initStandaloneMode();
        }
        // set storeOperational to true since it can be assumed that the store is operational at startup
        // if it is non-operational, the value will be updated immediately
        storeOperational = true;
        // Register this instance to listen to the store health
        FailureObservingStoreManager.registerStoreHealthListener(this);

    }

    /**
     * Handles changes needs to be done in current node when a node joins to the cluster.
     *
     * @param addedNodeId An ID for the newly added node. This is does not refer to the correct node ID. I.E its not a
     *                    reference to the member's node ID attribute.
     */
    public void memberAdded(String addedNodeId) {
        log.info("Handling cluster gossip: Node " + addedNodeId + "  Joined the Cluster");
    }

    /**
     * Handles changes needs to be done in current node when a node leaves the cluster
     *
     * @param deletedNodeId deleted node id
     */
    public void memberRemoved(String deletedNodeId) throws AndesException {
        log.info("Handling cluster gossip: Node " + deletedNodeId + "  left the Cluster");

        if (clusterAgent.isCoordinator()) {
            SlotManagerClusterMode.getInstance().deletePublisherNode(deletedNodeId);

            //clear persisted states of disappeared node
            clearAllPersistedStatesOfDisappearedNode(deletedNodeId);

            //Reassign the slot to free slots pool
            SlotManagerClusterMode.getInstance().reassignSlotsWhenMemberLeaves(deletedNodeId);

            // Schedule a slot recovery task for lost submit slot event from left member node
            log.info("Scheduling slot recovery task.");
            slotRecoveryTaskService.schedule(new Runnable() {
                @Override
                public void run() {
                    Andes.getInstance().triggerRecoveryEvent();
                }
            }, SlotMessageCounter.getInstance().slotSubmitTimeout, TimeUnit.MILLISECONDS);
        }

        // Deactivate durable subscriptions belonging to the node
        ClusterResourceHolder.getInstance().getSubscriptionManager().deactivateClusterDurableSubscriptionsForNodeID(
                clusterAgent.isCoordinator(), deletedNodeId);
    }

    /**
     * Get whether clustering is enabled
     *
     * @return true if clustering is enabled, false otherwise.
     */
    public boolean isClusteringEnabled() {
        return AndesContext.getInstance().isClusteringEnabled();
    }

    /**
     * Get the node ID of the current node
     *
     * @return current node's ID
     */
    public String getMyNodeID() {
        return nodeId;
    }

    /**
     * Perform cleanup tasks before shutdown
     */
    public void prepareLocalNodeForShutDown() throws AndesException {
        //clear stored node IDS and mark subscriptions of node as closed
        clearAllPersistedStatesOfDisappearedNode(nodeId);
    }

    /**
     * Gets the unique ID for the local node
     *
     * @return unique ID
     */
    public int getUniqueIdForLocalNode() {
        if (AndesContext.getInstance().isClusteringEnabled()) {
            return clusterAgent.getUniqueIdForLocalNode();
        }
        return 0;
    }

    /**
     * Initialize the node in stand alone mode without hazelcast.
     *
     * @throws AndesException, UnknownHostException
     */
    private void initStandaloneMode() throws AndesException {

        try {
            // Get Node ID configured by user in broker.xml (if not "default" we must use it as the ID)
            this.nodeId = AndesConfigurationManager.readValue(AndesConfiguration.COORDINATION_NODE_ID);

            if (AndesConfiguration.COORDINATION_NODE_ID.get().getDefaultValue().equals(this.nodeId)) {
                this.nodeId = CoordinationConstants.NODE_NAME_PREFIX + InetAddress.getLocalHost().toString();
            }

            //update node information in durable store
            List<String> nodeList = new ArrayList<>(andesContextStore.getAllStoredNodeData().keySet());

            for (String node : nodeList) {
                andesContextStore.removeNodeData(node);
            }

            clearAllPersistedStatesOfDisappearedNode(nodeId);

            log.info("Initializing Standalone Mode. Current Node ID:" + this.nodeId + " "
                             + InetAddress.getLocalHost().getHostAddress());

            andesContextStore.storeNodeDetails(nodeId, InetAddress.getLocalHost().getHostAddress());
        } catch (UnknownHostException e) {
            throw new AndesException("Unable to get the localhost address.", e);
        }
    }

    /**
     * Initializes cluster mode
     *
     * @throws AndesException
     */
    private void initClusterMode() throws AndesException {

        // Set the cluster agent from the Andes Context.
        this.clusterAgent = AndesContext.getInstance().getClusterAgent();

        clusterAgent.start(this);

        this.nodeId = clusterAgent.getLocalNodeIdentifier();
        log.info("Initializing Cluster Mode. Current Node ID:" + this.nodeId);

        String localMemberHostAddress = clusterAgent.getLocalNodeIdentifier();

        if (log.isDebugEnabled()) {
            log.debug("Stored node ID : " + this.nodeId + ". Stored node data(Hazelcast local "
                              + "member host address) : " + localMemberHostAddress);
        }

        //add node information to durable store
        andesContextStore.storeNodeDetails(nodeId, localMemberHostAddress);

        /**
         * If nodeList size is one, this is the first node joining to cluster. Here we check if
         * there has been any nodes that lived before and somehow suddenly got killed. If there are
         * such nodes clear the state of them and copy back node queue messages of them back to
         * global queue. We need to clear up current node's state as well as there might have been a
         * node with same id and it was killed
         */
        clearAllPersistedStatesOfDisappearedNode(nodeId);

        List<String> storedNodes = new ArrayList<>(andesContextStore.getAllStoredNodeData().keySet());
        List<String> availableNodeIds = clusterAgent.getAllNodeIdentifiers();
        for (String storedNodeId : storedNodes) {
            if (!availableNodeIds.contains(storedNodeId)) {
                clearAllPersistedStatesOfDisappearedNode(storedNodeId);
            }
        }
    }

    /**
     * Clears all persisted states of a disappeared node
     *
     * @param nodeId node ID
     * @throws AndesException
     */
    private void clearAllPersistedStatesOfDisappearedNode(String nodeId) throws AndesException {

        log.info("Clearing the Persisted State of Node with ID " + nodeId);

        //remove node from nodes list
        andesContextStore.removeNodeData(nodeId);
        //close all local queue and topic subscriptions belonging to the node
        ClusterResourceHolder.getInstance().getSubscriptionManager().closeAllClusterSubscriptionsOfNode(nodeId);

    }


    /**
     * Perform coordinator initialization tasks, when this node is elected as the new coordinator
     */
    public void localNodeElectedAsCoordinator() {
    }

    /**
     * Gets the coordinator node's address. i.e address:port
     *
     * @return Address of the coordinator node
     */
    public String getCoordinatorNodeAddress() {
        if (AndesContext.getInstance().isClusteringEnabled()) {
            CoordinatorInformation coordinatorDetails = clusterAgent.getCoordinatorDetails();
            String ipAddress = coordinatorDetails.getHostname();
            String port = coordinatorDetails.getPort();
            if (null != ipAddress && null != port) {
                return ipAddress + "," + port;
            }
        }

        return StringUtils.EMPTY;
    }

    /**
     * Gets address of all the members in the cluster. i.e address:port
     *
     * @return A list of address of the nodes in a cluster
     */
    public List<String> getAllClusterNodeAddresses() {

        if (AndesContext.getInstance().isClusteringEnabled()) {
            return clusterAgent.getAllClusterNodeAddresses();
        }

        return new ArrayList<>();
    }

    /**
     * Gets the message store's health status
     *
     * @return true if healthy, else false.
     */
    public boolean getStoreHealth() {
        return storeOperational;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void storeNonOperational(HealthAwareStore store, Exception ex) {
        storeOperational = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void storeOperational(HealthAwareStore store) {
        storeOperational = true;
    }
}
