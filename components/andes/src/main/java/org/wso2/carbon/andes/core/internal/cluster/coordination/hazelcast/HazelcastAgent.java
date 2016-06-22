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

package org.wso2.carbon.andes.core.internal.cluster.coordination.hazelcast;

import com.hazelcast.config.Config;
import com.hazelcast.config.ReliableTopicConfig;
import com.hazelcast.config.RingbufferConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastInstanceNotActiveException;
import com.hazelcast.core.IAtomicLong;
import com.hazelcast.core.ILock;
import com.hazelcast.core.IMap;
import com.hazelcast.core.ITopic;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.andes.core.AndesException;
import org.wso2.carbon.andes.core.internal.AndesContext;
import org.wso2.carbon.andes.core.internal.cluster.HazelcastClusterAgent;
import org.wso2.carbon.andes.core.internal.cluster.coordination.ClusterCoordinationHandler;
import org.wso2.carbon.andes.core.internal.cluster.coordination.ClusterNotification;
import org.wso2.carbon.andes.core.internal.cluster.coordination.CoordinationConstants;
import org.wso2.carbon.andes.core.internal.cluster.coordination.SlotAgent;
import org.wso2.carbon.andes.core.internal.cluster.coordination.hazelcast.custom.serializer.wrapper
        .HashmapStringTreeSetWrapper;
import org.wso2.carbon.andes.core.internal.cluster.coordination.hazelcast.custom.serializer.wrapper.TreeSetLongWrapper;
import org.wso2.carbon.andes.core.internal.cluster.coordination.hazelcast.custom.serializer.wrapper.TreeSetSlotWrapper;
import org.wso2.carbon.andes.core.internal.configuration.AndesConfigurationManager;
import org.wso2.carbon.andes.core.internal.configuration.enums.AndesConfiguration;
import org.wso2.carbon.andes.core.internal.slot.Slot;
import org.wso2.carbon.andes.core.internal.slot.SlotState;
import org.wso2.carbon.andes.core.internal.slot.SlotUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * This is a singleton class, which contains all Hazelcast related operations.
 */
public class HazelcastAgent implements SlotAgent {
    private static Log log = LogFactory.getLog(HazelcastAgent.class);

    /**
     * Value used to indicate the cluster initialization success state
     */
    private static final long INIT_SUCCESSFUL = 1L;

    /**
     * Singleton HazelcastAgent Instance.
     */
    private static HazelcastAgent hazelcastAgentInstance = new HazelcastAgent();

    /**
     * Hazelcast instance exposed by Carbon.
     */
    private HazelcastInstance hazelcastInstance;

    /**
     * Distributed topic to communicate subscription change notifications among cluster nodes.
     */
    private ITopic<ClusterNotification> subscriptionChangedNotifierChannel;

    /**
     * Distributed topic to communicate binding change notifications among cluster nodes.
     */
    private ITopic<ClusterNotification> bindingChangeNotifierChannel;

    /**
     * Distributed topic to communicate queue purge notifications among cluster nodes.
     */
    private ITopic<ClusterNotification> queueChangedNotifierChannel;

    /**
     * Distributed topic to communicate exchange change notification among cluster nodes.
     */
    private ITopic<ClusterNotification> exchangeChangeNotifierChannel;

    /**
     * Distributed topic to sent among cluster nodes to run andes recover task.
     */
    private ITopic<ClusterNotification> dbSyncNotifierChannel;

    /**
     * These distributed maps are used for slot management
     */

    /**
     * distributed Map to store message ID list against queue name
     */
    private IMap<String, TreeSetLongWrapper> slotIdMap;

    /**
     * to keep track of assigned slots up to now. Key of the map contains nodeID+"_"+queueName
     */
    private IMap<String, HashmapStringTreeSetWrapper> slotAssignmentMap;

    /**
     * To keep track of slots that overlap with already assigned slots (in slotAssignmentMap). This is to ensure that
     * messages assigned to a specific assigned slot are only handled by that node itself.
     */
    private IMap<String, HashmapStringTreeSetWrapper> overlappedSlotMap;

    /**
     * distributed Map to store last assigned ID against queue name
     */
    private IMap<String, Long> lastAssignedIDMap;

    /**
     * distributed Map to store local safe zones for each node ID
     */
    private IMap<String, Long> safeZoneMap;

    /**
     * Distributed Map to keep track of non-empty slots which are unassigned from
     * other nodes
     */
    private IMap<String, TreeSetSlotWrapper> unAssignedSlotMap;

    /**
     * This map is used to store thrift server host and thrift server port
     * map's key is port or host name.
     */
    private IMap<String, String> thriftServerDetailsMap;

    /**
     * Lock used to initialize the Slot map used by the Slot manager.
     */
    private ILock initializationLock;

    /**
     * This is used to indicate if the cluster initialization was done properly. Used a atomic long
     * since am atomic boolean is not available in the current Hazelcast implementation.
     */
    private IAtomicLong initializationDoneIndicator;

    /**
     * Hazelcast based cluster agent
     */
    private HazelcastClusterAgent clusterAgent;

    /**
     * Defines the maximum number of messages that will be read at a single try from a Hazelcast reliable topic.
     * This value is set to a very low number since these reliable topics only handle cluster notifications on
     * subscription changes, exchange changes, etc.
     * and the frequency of messages being published is very low.
     */
    private static final int HAZELCAST_RELIABLE_TOPIC_READ_BACH_SIZE = 5;

    /**
     * Defines the maximum number of messages that can be stored in the ring buffer associated with a
     * Hazelcast Reliable Topic. The buffer could be initialized with a somewhat low number since these reliable topics
     * only handle cluster notifications on subscription changes, exchange changes, etc. which are a very not so
     * frequent. But, there could be extreme and rare situations where subscriptions, bindings, etc. change at a very
     * high rate and to be able to tolerate that, the capacity is kept at 1000.
     */
    private static final int HAZELCAST_RING_BUFFER_CAPACITY = 1000;

    /**
     * Disables statistics on the messages published to Hazelcast Reliable Topics.
     * We don't need statistics on the messages that are published, therefore, we have disabled statistics.
     */
    private static final boolean ENABLE_STATISTICS = false;

    private String subscriptionListenerId;
    private String bindingListenerId;
    private String exchangeListenerId;
    private String queueListenerId;
    private String dbSyncNotificationListenerId;

    /**
     * Private constructor.
     */
    private HazelcastAgent() {

    }

    /**
     * Get singleton HazelcastAgent.
     *
     * @return HazelcastAgent
     */
    public static synchronized HazelcastAgent getInstance() {
        return hazelcastAgentInstance;
    }

    /**
     * Initialize HazelcastAgent instance.
     *
     * @param hazelcastInstance obtained hazelcastInstance from the OSGI service
     */
    @SuppressWarnings("unchecked")
    public void init(HazelcastInstance hazelcastInstance) {
        log.info("Initializing Hazelcast Agent");
        this.hazelcastInstance = hazelcastInstance;

        // Set cluster agent in Andes Context
        clusterAgent = new HazelcastClusterAgent(hazelcastInstance);
        AndesContext.getInstance().setClusterAgent(clusterAgent);
        addTopicListeners();

        /**
         * Initialize hazelcast maps for slots
         */
        unAssignedSlotMap = hazelcastInstance.getMap(CoordinationConstants.UNASSIGNED_SLOT_MAP_NAME);
        slotIdMap = hazelcastInstance.getMap(CoordinationConstants.SLOT_ID_MAP_NAME);
        lastAssignedIDMap = hazelcastInstance.getMap(CoordinationConstants.LAST_ASSIGNED_ID_MAP_NAME);
        safeZoneMap = hazelcastInstance.getMap(CoordinationConstants.LAST_PUBLISHED_ID_MAP_NAME);
        slotAssignmentMap = hazelcastInstance.getMap(CoordinationConstants.SLOT_ASSIGNMENT_MAP_NAME);
        overlappedSlotMap = hazelcastInstance.getMap(CoordinationConstants.OVERLAPPED_SLOT_MAP_NAME);

        /**
         * Initialize hazelcast map fot thrift server details
         */
        thriftServerDetailsMap = hazelcastInstance.getMap(CoordinationConstants.THRIFT_SERVER_DETAILS_MAP_NAME);

        /**
         * Initialize distributed lock and boolean related to slot map initialization
         */
        initializationLock = hazelcastInstance.getLock(CoordinationConstants.INITIALIZATION_LOCK);
        initializationDoneIndicator = hazelcastInstance
                .getAtomicLong(CoordinationConstants.INITIALIZATION_DONE_INDICATOR);

        log.info("Successfully initialized Hazelcast Agent");
    }

    public void addTopicListeners() {
        // Defines the time it takes for a message published to a Hazelcast reliable topic to be expired.
        // The messages that are published to these topics should ideally be read at the same time. One instance
        // where this would not happen is when a node gets disconnected. Since all the messages that are published
        // to these topics are stored in the database, this situation is handled by synchronizing the information in
        // the databases when the node recovers. Therefore, we do not need undelivered messages to delivered
        // after a while. Therefore, we need messages to be held in the buffer onle for a very little time.
        int hazelcastRingBufferTTL = AndesConfigurationManager.readValue(
                AndesConfiguration.COORDINATION_CLUSTER_NOTIFICATION_TIMEOUT);

        /**
         * subscription changes
         */
        //configure Hazelcast ring buffer and reliable topic for subscription changes
        addReliableTopicConfig(CoordinationConstants.HAZELCAST_SUBSCRIPTION_CHANGED_NOTIFIER_TOPIC_NAME,
                               ENABLE_STATISTICS, HAZELCAST_RELIABLE_TOPIC_READ_BACH_SIZE);
        addRingBufferConfig(CoordinationConstants.HAZELCAST_SUBSCRIPTION_CHANGED_NOTIFIER_TOPIC_NAME,
                            HAZELCAST_RING_BUFFER_CAPACITY, hazelcastRingBufferTTL);

        //add listener for subscription changes
        this.subscriptionChangedNotifierChannel = this.hazelcastInstance.getReliableTopic(
                CoordinationConstants.HAZELCAST_SUBSCRIPTION_CHANGED_NOTIFIER_TOPIC_NAME);
        ClusterSubscriptionChangedListener clusterSubscriptionChangedListener = new
                ClusterSubscriptionChangedListener();
        clusterSubscriptionChangedListener.addSubscriptionListener(new ClusterCoordinationHandler(this));
        this.subscriptionChangedNotifierChannel.addMessageListener(clusterSubscriptionChangedListener);

        if (StringUtils.isNotEmpty(subscriptionListenerId)) {
            this.subscriptionChangedNotifierChannel.removeMessageListener(subscriptionListenerId);
        }
        subscriptionListenerId = this.subscriptionChangedNotifierChannel.addMessageListener(
                clusterSubscriptionChangedListener);

        /**
         * exchange changes
         */

        //configure Hazelcast ring buffer and reliable topic for exchange changes
        addReliableTopicConfig(CoordinationConstants.HAZELCAST_EXCHANGE_CHANGED_NOTIFIER_TOPIC_NAME,
                               ENABLE_STATISTICS, HAZELCAST_RELIABLE_TOPIC_READ_BACH_SIZE);
        addRingBufferConfig(CoordinationConstants.HAZELCAST_EXCHANGE_CHANGED_NOTIFIER_TOPIC_NAME,
                            HAZELCAST_RING_BUFFER_CAPACITY, hazelcastRingBufferTTL);

        //add listener for exchange changes
        this.exchangeChangeNotifierChannel = this.hazelcastInstance.getReliableTopic(
                CoordinationConstants.HAZELCAST_EXCHANGE_CHANGED_NOTIFIER_TOPIC_NAME);
        ClusterExchangeChangedListener clusterExchangeChangedListener = new ClusterExchangeChangedListener();
        clusterExchangeChangedListener.addExchangeListener(new ClusterCoordinationHandler(this));
        if (StringUtils.isNotEmpty(exchangeListenerId)) {
            this.exchangeChangeNotifierChannel.removeMessageListener(exchangeListenerId);
        }
        exchangeListenerId = this.exchangeChangeNotifierChannel.addMessageListener(clusterExchangeChangedListener);

        /**
         * queue changes
         */

        //configure Hazelcast ring buffer and reliable topic for queue changes
        addReliableTopicConfig(CoordinationConstants.HAZELCAST_QUEUE_CHANGED_NOTIFIER_TOPIC_NAME,
                               ENABLE_STATISTICS, HAZELCAST_RELIABLE_TOPIC_READ_BACH_SIZE);
        addRingBufferConfig(CoordinationConstants.HAZELCAST_QUEUE_CHANGED_NOTIFIER_TOPIC_NAME,
                            HAZELCAST_RING_BUFFER_CAPACITY, hazelcastRingBufferTTL);

        //add listener for queue changes
        this.queueChangedNotifierChannel = this.hazelcastInstance.getReliableTopic(
                CoordinationConstants.HAZELCAST_QUEUE_CHANGED_NOTIFIER_TOPIC_NAME);
        ClusterQueueChangedListener clusterQueueChangedListener = new ClusterQueueChangedListener();
        clusterQueueChangedListener.addQueueListener(new ClusterCoordinationHandler(this));
        if (StringUtils.isNotEmpty(queueListenerId)) {
            this.queueChangedNotifierChannel.removeMessageListener(queueListenerId);
        }
        queueListenerId = this.queueChangedNotifierChannel.addMessageListener(clusterQueueChangedListener);

        /**
         * binding changes
         */

        //configure Hazelcast ring buffer and reliable topic for binding changes
        addReliableTopicConfig(CoordinationConstants.HAZELCAST_BINDING_CHANGED_NOTIFIER_TOPIC_NAME,
                               ENABLE_STATISTICS, HAZELCAST_RELIABLE_TOPIC_READ_BACH_SIZE);
        addRingBufferConfig(CoordinationConstants.HAZELCAST_BINDING_CHANGED_NOTIFIER_TOPIC_NAME,
                            HAZELCAST_RING_BUFFER_CAPACITY, hazelcastRingBufferTTL);

        //add listener for binding changes
        this.bindingChangeNotifierChannel = this.hazelcastInstance.getReliableTopic(
                CoordinationConstants.HAZELCAST_BINDING_CHANGED_NOTIFIER_TOPIC_NAME);
        ClusterBindingChangedListener clusterBindingChangedListener = new ClusterBindingChangedListener();
        clusterBindingChangedListener.addBindingListener(new ClusterCoordinationHandler(this));
        if (StringUtils.isNotEmpty(bindingListenerId)) {
            this.bindingChangeNotifierChannel.removeMessageListener(bindingListenerId);
        }
        bindingListenerId = this.bindingChangeNotifierChannel.addMessageListener(clusterBindingChangedListener);

        /**
         * Adding database sync notification to run andes recovery task
         */
        //configure Hazelcast ring buffer and reliable topic for DB sync notification
        addReliableTopicConfig(CoordinationConstants.HAZELCAST_DB_SYNC_NOTIFICATION_TOPIC_NAME,
                               ENABLE_STATISTICS, HAZELCAST_RELIABLE_TOPIC_READ_BACH_SIZE);
        addRingBufferConfig(CoordinationConstants.HAZELCAST_DB_SYNC_NOTIFICATION_TOPIC_NAME,
                            HAZELCAST_RING_BUFFER_CAPACITY, hazelcastRingBufferTTL);

        //add listener for DB sync notification
        this.dbSyncNotifierChannel = this.hazelcastInstance.getReliableTopic(
                CoordinationConstants.HAZELCAST_DB_SYNC_NOTIFICATION_TOPIC_NAME);
        DatabaseSyncNotificationListener databaseSyncNotificationListener = new DatabaseSyncNotificationListener();
        if (StringUtils.isNotEmpty(dbSyncNotificationListenerId)) {
            this.dbSyncNotifierChannel.removeMessageListener(dbSyncNotificationListenerId);
        }
        dbSyncNotificationListenerId = this.dbSyncNotifierChannel.addMessageListener(databaseSyncNotificationListener);

    }


    public void notifySubscriptionsChanged(ClusterNotification clusterNotification) throws AndesException {
        if (log.isDebugEnabled()) {
            log.debug("Sending GOSSIP: " + clusterNotification.getDescription());
        }
        try {
            this.subscriptionChangedNotifierChannel.publish(clusterNotification);
        } catch (Exception ex) {
            log.error("Error while sending subscription change notification : "
                              + clusterNotification.getEncodedObjectAsString(), ex);
            throw new AndesException("Error while sending queue change notification : "
                                             + clusterNotification.getEncodedObjectAsString(), ex);
        }

    }

    public void notifyQueuesChanged(ClusterNotification clusterNotification) throws AndesException {

        if (log.isDebugEnabled()) {
            log.debug("Sending GOSSIP: " + clusterNotification.getDescription());
        }
        try {
            this.queueChangedNotifierChannel.publish(clusterNotification);
        } catch (Exception e) {
            log.error("Error while sending queue change notification : "
                              + clusterNotification.getEncodedObjectAsString(), e);
            throw new AndesException("Error while sending queue change notification : "
                                             + clusterNotification.getEncodedObjectAsString(), e);
        }
    }

    public void notifyExchangesChanged(ClusterNotification clusterNotification) throws AndesException {
        if (log.isDebugEnabled()) {
            log.debug("Sending GOSSIP: " + clusterNotification.getDescription());
        }
        try {
            this.exchangeChangeNotifierChannel.publish(clusterNotification);
        } catch (Exception e) {
            log.error("Error while sending exchange change notification"
                              + clusterNotification.getEncodedObjectAsString(), e);
            throw new AndesException("Error while sending exchange change notification"
                                             + clusterNotification.getEncodedObjectAsString(), e);
        }
    }

    public void notifyBindingsChanged(ClusterNotification clusterNotification) throws AndesException {
        if (log.isDebugEnabled()) {
            log.debug("GOSSIP: " + clusterNotification.getDescription());
        }
        try {
            this.bindingChangeNotifierChannel.publish(clusterNotification);
        } catch (Exception e) {
            log.error("Error while sending binding change notification"
                              + clusterNotification.getEncodedObjectAsString(), e);
            throw new AndesException("Error while sending binding change notification"
                                             + clusterNotification.getEncodedObjectAsString(), e);
        }
    }

    public void notifyDBSyncEvent(ClusterNotification clusterNotification) throws AndesException {
        if (log.isDebugEnabled()) {
            log.debug("GOSSIP: " + clusterNotification.getDescription());
        }
        try {
            this.dbSyncNotifierChannel.publish(clusterNotification);
        } catch (Exception e) {
            log.error("Error while sending db sync notification"
                              + clusterNotification.getEncodedObjectAsString(), e);
            throw new AndesException("Error while sending db sync notification"
                                             + clusterNotification.getEncodedObjectAsString(), e);
        }
    }

    /**
     * This method returns a map containing thrift server port and hostname
     *
     * @return thriftServerDetailsMap
     */
    public IMap<String, String> getThriftServerDetailsMap() {
        return thriftServerDetailsMap;
    }

    /**
     * Acquire the distributed lock related to cluster initialization. This lock is required to
     * avoid two nodes initializing the map twice.
     */
    public void acquireInitializationLock() {
        if (log.isDebugEnabled()) {
            log.debug("Trying to acquire initialization lock.");
        }

        initializationLock.lock();

        if (log.isDebugEnabled()) {
            log.debug("Initialization lock acquired.");
        }
    }

    /**
     * Inform other members in the cluster that the cluster was initialized properly.
     */
    public void indicateSuccessfulInitilization() {
        initializationDoneIndicator.set(INIT_SUCCESSFUL);
    }

    /**
     * Check if a member has already initialized the cluster
     *
     * @return true if cluster is already initialized
     */
    public boolean isClusterInitializedSuccessfully() {
        return initializationDoneIndicator.get() == INIT_SUCCESSFUL;
    }

    /**
     * Release the initialization lock.
     */
    public void releaseInitializationLock() {
        initializationLock.unlock();

        if (log.isDebugEnabled()) {
            log.debug("Initialization lock released.");
        }
    }

    /**
     * Method to check if the hazelcast instance has shutdown.
     *
     * @return boolean
     */
    public boolean isActive() {
        if (null != hazelcastInstance) {
            return hazelcastInstance.getLifecycleService().isRunning();
        } else {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createSlot(long startMessageId, long endMessageId, String storageQueueName, String assignedNodeId)
            throws AndesException {
        //createSlot() method in Hazelcast agent does not need to perform anything
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean deleteSlot(String nodeId, String queueName, long startMessageId, long endMessageId)
            throws AndesException {
        boolean slotDeleted = false;
        try {
            HashMap<String, TreeSet<Slot>> queueToSlotMap = null;
            HashmapStringTreeSetWrapper wrapper = this.slotAssignmentMap.get(nodeId);
            if (null != wrapper) {
                queueToSlotMap = wrapper.getStringListHashMap();
            }
            if (null != queueToSlotMap) {
                TreeSet<Slot> currentSlotList = queueToSlotMap.get(queueName);
                if (null != currentSlotList) {
                    // com.google.gson.Gson gson = new GsonBuilder().create();
                    //get the actual reference of the slot to be removed
                    Slot matchingSlot = null; //currentSlotList.ceiling(emptySlot);
                    for (Slot slot : currentSlotList) {
                        if (slot.getStartMessageId() == startMessageId) {
                            matchingSlot = slot;
                        }
                    }
                    if (null != matchingSlot) {
                        if (matchingSlot.addState(SlotState.DELETED)) {
                            currentSlotList.remove(matchingSlot);
                            queueToSlotMap.put(queueName, currentSlotList);
                            wrapper.setStringListHashMap(queueToSlotMap);
                            slotAssignmentMap.set(nodeId, wrapper);
                            slotDeleted = true;
                        }
                    } else {
                        // We can say slot deleted since the slot does not exist
                        slotDeleted = true;
                    }
                }
            }
        } catch (HazelcastInstanceNotActiveException ex) {
            throw new AndesException("Failed to delete slot for queue : " +
                                             queueName + " from node " + nodeId, ex);
        }
        return slotDeleted;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteSlotAssignmentByQueueName(String nodeId, String queueName) throws AndesException {
        try {
            TreeSet<Slot> slotListToReturn = new TreeSet<>();
            //Get assigned slots from Hazelcast, delete all belonging to queue
            //and set back
            HashmapStringTreeSetWrapper wrapper = this.slotAssignmentMap.get(nodeId);
            HashMap<String, TreeSet<Slot>> queueToSlotMap = null;
            if (null != wrapper) {
                queueToSlotMap = wrapper.getStringListHashMap();
            }
            if (null != queueToSlotMap) {
                TreeSet<Slot> assignedSlotList = queueToSlotMap.remove(queueName);
                if (null != assignedSlotList) {
                    slotListToReturn.addAll(assignedSlotList);
                }
                wrapper.setStringListHashMap(queueToSlotMap);
                this.slotAssignmentMap.set(nodeId, wrapper);
            }

            //Get overlapped slots from Hazelcast, delete all belonging to queue and
            //set back
            HashmapStringTreeSetWrapper overlappedSlotWrapper = this.overlappedSlotMap.get(nodeId);
            HashMap<String, TreeSet<Slot>> queueToOverlappedSlotMap = null;
            if (null != overlappedSlotWrapper) {
                queueToOverlappedSlotMap = overlappedSlotWrapper.getStringListHashMap();
            }
            if (null != queueToOverlappedSlotMap) {
                TreeSet<Slot> assignedOverlappedSlotList = queueToOverlappedSlotMap.remove(queueName);
                if (null != assignedOverlappedSlotList) {
                    slotListToReturn.addAll(assignedOverlappedSlotList);
                }
                overlappedSlotWrapper.setStringListHashMap(queueToOverlappedSlotMap);
                this.overlappedSlotMap.set(nodeId, overlappedSlotWrapper);
            }

            //add the deleted slots to un-assigned slot map, so that they can be assigned again.
            if (!(slotListToReturn.isEmpty())) {
                TreeSetSlotWrapper treeSetStringWrapper = unAssignedSlotMap.get(queueName);
                TreeSet<Slot> unAssignedSlotSet = new TreeSet<>();
                if (null != treeSetStringWrapper) {
                    unAssignedSlotSet = treeSetStringWrapper.getSlotTreeSet();
                } else {
                    treeSetStringWrapper = new TreeSetSlotWrapper();
                }
                if (null == unAssignedSlotSet) {
                    unAssignedSlotSet = new TreeSet<>();
                }
                for (Slot returnSlot : slotListToReturn) {
                    //Reassign only if the slot is not empty
                    if (!(SlotUtils.checkSlotEmptyFromMessageStore(returnSlot))) {
                        if (returnSlot.addState(SlotState.RETURNED)) {
                            unAssignedSlotSet.add(returnSlot);
                        }
                    }
                    treeSetStringWrapper.setSlotTreeSet(unAssignedSlotSet);
                    unAssignedSlotMap.set(queueName, treeSetStringWrapper);
                }
            }
        } catch (HazelcastInstanceNotActiveException ex) {
            throw new AndesException("Failed to delete slot assignment for queue : " +
                                             queueName + " from node " + nodeId, ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Slot getUnAssignedSlot(String queueName) throws AndesException {
        Slot slotToBeAssigned = null;
        try {
            TreeSetSlotWrapper unAssignedSlotWrapper = unAssignedSlotMap.get(queueName);
            if (null != unAssignedSlotWrapper) {
                TreeSet<Slot> slotsFromUnassignedSlotMap = unAssignedSlotWrapper.getSlotTreeSet();
                if (null != slotsFromUnassignedSlotMap && !(slotsFromUnassignedSlotMap.isEmpty())) {
                    //Get and remove slot and update hazelcast map
                    slotToBeAssigned = slotsFromUnassignedSlotMap.pollFirst();
                    unAssignedSlotWrapper.setSlotTreeSet(slotsFromUnassignedSlotMap);
                    unAssignedSlotMap.set(queueName, unAssignedSlotWrapper);
                }
            }
        } catch (HazelcastInstanceNotActiveException ex) {
            throw new AndesException("Failed to get unassigned slot for queue : " +
                                             queueName, ex);
        }
        return slotToBeAssigned;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateSlotAssignment(String nodeId, String queueName, Slot allocatedSlot) throws AndesException {
        TreeSet<Slot> currentSlotList;
        HashMap<String, TreeSet<Slot>> queueToSlotMap;

        try {
            HashmapStringTreeSetWrapper wrapper = this.slotAssignmentMap.get(nodeId);
            if (null == wrapper) {
                wrapper = new HashmapStringTreeSetWrapper();
                queueToSlotMap = new HashMap<>();
                wrapper.setStringListHashMap(queueToSlotMap);
                this.slotAssignmentMap.putIfAbsent(nodeId, wrapper);
            }
            wrapper = this.slotAssignmentMap.get(nodeId);
            queueToSlotMap = wrapper.getStringListHashMap();
            currentSlotList = queueToSlotMap.get(queueName);
            if (null == currentSlotList) {
                currentSlotList = new TreeSet<>();
            }

            //update slot state
            if (allocatedSlot.addState(SlotState.ASSIGNED)) {
                //remove any similar slot from hazelcast and add the updated one
                currentSlotList.remove(allocatedSlot);
                currentSlotList.add(allocatedSlot);
                queueToSlotMap.put(queueName, currentSlotList);
                wrapper.setStringListHashMap(queueToSlotMap);
                this.slotAssignmentMap.set(nodeId, wrapper);
            }
        } catch (HazelcastInstanceNotActiveException ex) {
            throw new AndesException("Failed to update slot assignment for queue : " +
                                             queueName + " from node " + nodeId, ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getQueueToLastAssignedId(String queueName) throws AndesException {
        long lastAssignedId = this.lastAssignedIDMap.get(queueName) != null ?
                this.lastAssignedIDMap.get(queueName) : 0L;
        return lastAssignedId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setQueueToLastAssignedId(String queueName, long lastAssignedId) throws AndesException {
        this.lastAssignedIDMap.set(queueName, lastAssignedId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Long getLocalSafeZoneOfNode(String nodeId) throws AndesException {
        return this.safeZoneMap.get(nodeId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setLocalSafeZoneOfNode(String nodeId, long localSafeZone) throws AndesException {
        this.safeZoneMap.set(nodeId, localSafeZone);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removePublisherNode(String nodeId) throws AndesException {
        safeZoneMap.delete(nodeId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TreeSet<String> getMessagePublishedNodes() throws AndesException {
        TreeSet<String> messagePublishedNodes = new TreeSet<>();
        messagePublishedNodes.addAll(this.safeZoneMap.keySet());
        return messagePublishedNodes;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setSlotState(long startMessageId, long endMessageId, SlotState slotState) throws AndesException {
        // In hazelcast slot state is already stored in slot object. This method is used only in
        // database slot management.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Slot getOverlappedSlot(String nodeId, String queueName) throws AndesException {
        Slot slotToBeAssigned = null;
        TreeSet<Slot> currentSlotList;
        HashMap<String, TreeSet<Slot>> queueToSlotMap;
        try {
            HashmapStringTreeSetWrapper wrapper = this.overlappedSlotMap.get(nodeId);
            if (null != wrapper) {
                queueToSlotMap = wrapper.getStringListHashMap();
                currentSlotList = queueToSlotMap.get(queueName);
                if (null != currentSlotList && !(currentSlotList.isEmpty())) {
                    //get and remove slot
                    slotToBeAssigned = currentSlotList.pollFirst();
                    queueToSlotMap.put(queueName, currentSlotList);
                    //update hazelcast map
                    wrapper.setStringListHashMap(queueToSlotMap);
                    this.overlappedSlotMap.set(nodeId, wrapper);
                }
            }
        } catch (HazelcastInstanceNotActiveException ex) {
            throw new AndesException("Failed to getOverlappedSlot for queue : " +
                                             queueName + " from node " + nodeId, ex);
        }
        return slotToBeAssigned;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addMessageId(String queueName, long messageId) throws AndesException {
        try {
            TreeSet<Long> messageIdSet = this.getMessageIds(queueName);
            TreeSetLongWrapper wrapper = this.slotIdMap.get(queueName);
            messageIdSet.add(messageId);
            wrapper.setLongTreeSet(messageIdSet);
            this.slotIdMap.set(queueName, wrapper);
        } catch (HazelcastInstanceNotActiveException ex) {
            throw new AndesException("Failed to addMessageId for queue : " +
                                             queueName, ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TreeSet<Long> getMessageIds(String queueName) throws AndesException {
        TreeSetLongWrapper wrapper = null;
        try {
            wrapper = this.slotIdMap.get(queueName);
            if (null == wrapper) {
                wrapper = new TreeSetLongWrapper();
                this.slotIdMap.putIfAbsent(queueName, wrapper);
            }
        } catch (HazelcastInstanceNotActiveException ex) {
            throw new AndesException("Failed to getMessageIds for queue : " +
                                             queueName, ex);
        }
        return wrapper.getLongTreeSet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteMessageId(String queueName, long messageId) throws AndesException {
        try {
            TreeSetLongWrapper wrapper = this.slotIdMap.get(queueName);
            TreeSet<Long> messageIDSet;
            messageIDSet = wrapper.getLongTreeSet();
            if (null != messageIDSet && !(messageIDSet.isEmpty())) {
                messageIDSet.pollFirst();
                //set modified published ID map to hazelcast
                wrapper.setLongTreeSet(messageIDSet);
                this.slotIdMap.set(queueName, wrapper);
            }
        } catch (HazelcastInstanceNotActiveException ex) {
            throw new AndesException("Failed to deleteMessageId for queue : " +
                                             queueName, ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteSlotsByQueueName(String queueName) throws AndesException {
        try {
            if (null != this.unAssignedSlotMap) {
                this.unAssignedSlotMap.remove(queueName);
            }

            // The requirement here is to clear slot associations for the queue on all nodes.
            List<String> nodeIDs = AndesContext.getInstance().getClusterAgent().getAllNodeIdentifiers();

            for (String nodeID : nodeIDs) {
                HashmapStringTreeSetWrapper wrapper = slotAssignmentMap.get(nodeID);
                HashMap<String, TreeSet<Slot>> queueToSlotMap = null;
                if (null != wrapper) {
                    queueToSlotMap = wrapper.getStringListHashMap();
                }
                if (null != queueToSlotMap) {
                    queueToSlotMap.remove(queueName);
                    wrapper.setStringListHashMap(queueToSlotMap);
                    slotAssignmentMap.set(nodeID, wrapper);
                }

                //clear overlapped slot map
                HashmapStringTreeSetWrapper overlappedSlotsWrapper = overlappedSlotMap.get(nodeID);
                if (null != overlappedSlotsWrapper) {
                    HashMap<String, TreeSet<Slot>> queueToOverlappedSlotMap = null;
                    if (null != wrapper) {
                        queueToOverlappedSlotMap = overlappedSlotsWrapper.getStringListHashMap();
                    }
                    if (null != queueToSlotMap) {
                        queueToOverlappedSlotMap.remove(queueName);
                        overlappedSlotsWrapper.setStringListHashMap(queueToOverlappedSlotMap);
                        overlappedSlotMap.set(nodeID, overlappedSlotsWrapper);
                    }
                }
            }
        } catch (HazelcastInstanceNotActiveException ex) {
            throw new AndesException("Failed to deleteSlotsByQueueName for queue : " +
                                             queueName, ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteMessageIdsByQueueName(String queueName) throws AndesException {
        if (null != this.slotIdMap) {
            this.slotIdMap.remove(queueName);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TreeSet<Slot> getAssignedSlotsByNodeId(String nodeId) throws AndesException {
        TreeSet<Slot> resultSet = new TreeSet<>();
        try {
            HashmapStringTreeSetWrapper wrapper = this.slotAssignmentMap.remove(nodeId);
            HashMap<String, TreeSet<Slot>> queueToSlotMap = null;
            if (null != wrapper) {
                queueToSlotMap = wrapper.getStringListHashMap();
            }
            if (null != queueToSlotMap) {
                for (Map.Entry<String, TreeSet<Slot>> entry : queueToSlotMap.entrySet()) {
                    TreeSet<Slot> slotsToBeReassigned = entry.getValue();
                    resultSet.addAll(slotsToBeReassigned);
                }
            }
        } catch (HazelcastInstanceNotActiveException ex) {
            throw new AndesException("Failed to deleteSlotsByQueueName for node : " +
                                             nodeId, ex);
        }
        return resultSet;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TreeSet<Slot> getAllSlotsByQueueName(String queueName) throws AndesException {
        TreeSet<Slot> resultSet = new TreeSet<>();
        TreeSet<String> messagePublishedNodes = getMessagePublishedNodes();

        for (String nodeId : messagePublishedNodes) {
            if (!(overlappedSlotMap.containsKey(nodeId))) {
                overlappedSlotMap.put(nodeId, new HashmapStringTreeSetWrapper());
            }
            HashmapStringTreeSetWrapper olWrapper = overlappedSlotMap.get(nodeId);
            if (null != olWrapper) {
                HashMap<String, TreeSet<Slot>> olSlotMap = olWrapper.getStringListHashMap();
                if (null != olSlotMap) {
                    if (!(olSlotMap.containsKey(queueName))) {
                        olSlotMap.put(queueName, new TreeSet<Slot>());
                        olWrapper.setStringListHashMap(olSlotMap);
                        overlappedSlotMap.set(nodeId, olWrapper);
                    } else {
                        resultSet.addAll(olSlotMap.get(queueName));
                    }
                }
            }


            HashmapStringTreeSetWrapper wrapper = slotAssignmentMap.get(nodeId);
            if (null != wrapper) {
                HashMap<String, TreeSet<Slot>> queueToSlotMap = wrapper.getStringListHashMap();
                if (queueToSlotMap != null) {
                    TreeSet<Slot> slotListForQueueOnNode = queueToSlotMap.get(queueName);
                    if (null != slotListForQueueOnNode) {
                        resultSet.addAll(slotListForQueueOnNode);
                    }
                }
            }
        }

        TreeSetSlotWrapper treeSetStringWrapper = unAssignedSlotMap.get(queueName);
        TreeSet<Slot> unAssignedSlotSet;
        if (null != treeSetStringWrapper) {
            unAssignedSlotSet = treeSetStringWrapper.getSlotTreeSet();

            if (null != unAssignedSlotSet) {
                resultSet.addAll(unAssignedSlotSet);
            }
        }

        return resultSet;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reassignSlot(Slot slotToBeReassigned) throws AndesException {
        try {
            TreeSet<Slot> freeSlotTreeSet = new TreeSet<>();
            TreeSetSlotWrapper treeSetStringWrapper = new TreeSetSlotWrapper();

            treeSetStringWrapper.setSlotTreeSet(freeSlotTreeSet);

            this.unAssignedSlotMap.putIfAbsent(slotToBeReassigned.getStorageQueueName(),
                                               treeSetStringWrapper);

            if (slotToBeReassigned.addState(SlotState.RETURNED)) {
                treeSetStringWrapper = this.unAssignedSlotMap.get(slotToBeReassigned.getStorageQueueName());
                freeSlotTreeSet = treeSetStringWrapper.getSlotTreeSet();
                freeSlotTreeSet.add(slotToBeReassigned);
                treeSetStringWrapper.setSlotTreeSet(freeSlotTreeSet);
                this.unAssignedSlotMap.set(slotToBeReassigned.getStorageQueueName(), treeSetStringWrapper);
            }
        } catch (HazelcastInstanceNotActiveException ex) {
            throw new AndesException("Failed to reassign slot", ex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteOverlappedSlots(String nodeId) throws AndesException {
        this.overlappedSlotMap.remove(nodeId);
    }

    /**
     * TODO Fix with new changes
     * {@inheritDoc}
     */
    @Override
    public void updateOverlappedSlots(String queueName, TreeSet<Slot> overlappedSlots)
            throws AndesException {
        TreeSet<String> messagePublishedNodes = getMessagePublishedNodes();

        for (String nodeId : messagePublishedNodes) {
            HashmapStringTreeSetWrapper wrapper = slotAssignmentMap.get(nodeId);
            HashMap<String, TreeSet<Slot>> queueToSlotMap = new HashMap<>();
            if (null == wrapper) {
                wrapper = new HashmapStringTreeSetWrapper();
            } else {
                queueToSlotMap = wrapper.getStringListHashMap();
            }

            HashmapStringTreeSetWrapper olWrapper = overlappedSlotMap.get(nodeId);
            HashMap<String, TreeSet<Slot>> olSlotMap = olWrapper.getStringListHashMap();
            for (Slot slot : overlappedSlots) {
                TreeSet<Slot> slotsForNode = queueToSlotMap.get(queueName);
                if (slotsForNode.contains(slot)) {
                    //Add to global overlappedSlotMap
                    olSlotMap.get(queueName).remove(slot);
                    olSlotMap.get(queueName).add(slot);

                    slotsForNode.remove(slot);
                }
            }
            wrapper.setStringListHashMap(queueToSlotMap);
            slotAssignmentMap.set(nodeId, wrapper);
            // Add all marked slots collected into the olSlot to global overlappedSlotsMap.
            olWrapper.setStringListHashMap(olSlotMap);
            overlappedSlotMap.set(nodeId, olWrapper);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> getAllQueues() throws AndesException {
        return this.slotIdMap.keySet();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clearSlotStorage() throws AndesException {
        // Maps will be automatically cleared.
    }

    /**
     * Method to configure a given reliable topic of the Hazelcast instance. Refer to Hazelcast Reliable
     * Topic Configurations for more information on the configuration parameters
     *
     * @param reliableTopicName the name of the reliable topic. The configuration will be stored under the same name
     * @param enableStatistics  whether to enable statistics
     * @param readBatchSize     the maximum number of items that will be read at a single try. If the number of items
     *                          that are present is less than the readBatchSize, the available items will be read
     */
    private void addReliableTopicConfig(String reliableTopicName, boolean enableStatistics, int readBatchSize) {
        Config config = hazelcastInstance.getConfig();

        // Create a new Hazelcast reliable topic configuration with the given values
        ReliableTopicConfig topicConfig = new ReliableTopicConfig(reliableTopicName);
        topicConfig.setStatisticsEnabled(enableStatistics);
        topicConfig.setReadBatchSize(readBatchSize);

        // Add the current reliable topic configuration to the configurations of the Hazelcast instance
        config.addReliableTopicConfig(topicConfig);
    }

    /**
     * Method to configure a given ring buffer of the Hazelcast instance. Refer to Hazelcast Ring Buffer
     * configurations for more information on the configuration parameters
     *
     * @param ringBufferName the name from which the ring buffer should be created.
     *                       Same as the associated reliable topic name
     * @param capacity       the size of the ring buffer. Defines the number of messages that will be stored
     * @param timeToLive     the time it takes for a message published to a reliable topic to expire
     */
    private void addRingBufferConfig(String ringBufferName, int capacity, int timeToLive) {
        Config config = hazelcastInstance.getConfig();

        // Create a new ring buffer configuration with the given name and values
        RingbufferConfig ringConfig = new RingbufferConfig(ringBufferName);
        ringConfig.setCapacity(capacity);
        ringConfig.setTimeToLiveSeconds(timeToLive);

        // Add the current ring buffer configuration to the configurations of the Hazelcast instance
        config.addRingBufferConfig(ringConfig);
    }
}
