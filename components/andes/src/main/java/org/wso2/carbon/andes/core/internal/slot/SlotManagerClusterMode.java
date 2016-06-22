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

package org.wso2.carbon.andes.core.internal.slot;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.gs.collections.impl.map.mutable.ConcurrentHashMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.andes.core.AndesException;
import org.wso2.carbon.andes.core.internal.cluster.coordination.SlotAgent;
import org.wso2.carbon.andes.core.internal.cluster.coordination.hazelcast.HazelcastAgent;
import org.wso2.carbon.andes.core.internal.cluster.coordination.rdbms.DatabaseSlotAgent;
import org.wso2.carbon.andes.core.internal.configuration.AndesConfigurationManager;
import org.wso2.carbon.andes.core.internal.configuration.enums.AndesConfiguration;

import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Slot Manager Cluster Mode is responsible of slot allocating, slot creating,
 * slot re-assigning and slot managing tasks in cluster mode
 */
public class SlotManagerClusterMode {

    private static final int INITIAL_MESSAGE_ID = -1;

    private static final Log log = LogFactory.getLog(SlotManagerClusterMode.class);

    private static final SlotManagerClusterMode slotManager = new SlotManagerClusterMode();

    private static final int SAFE_ZONE_EVALUATION_INTERVAL = 5 * 1000;

    //safe zone calculator
    private final SlotDeleteSafeZoneCalc slotDeleteSafeZoneCalc;

    //first message id of fresh slot
    private long firstMessageId;

    /**
     * Denotes whether a slot recovery task is scheduled
     */
    private AtomicBoolean slotRecoveryScheduled;

    /**
     * Queues that need to be recovered  While a slot recovery is scheduled a submit
     * slot comes to the given queue that queue will be removed from the scheduled task.
     */
    private Set<String> queuesToRecover;

    private SlotAgent slotAgent;

    private SlotManagerClusterMode() {

        //start a thread to calculate slot delete safe zone
        slotDeleteSafeZoneCalc = new SlotDeleteSafeZoneCalc(SAFE_ZONE_EVALUATION_INTERVAL);
        new Thread(slotDeleteSafeZoneCalc).start();

        String slotMgtMode = AndesConfigurationManager.readValue(AndesConfiguration.SLOT_MANAGEMENT_STORAGE);
        if ("RDBMS".equalsIgnoreCase(slotMgtMode)) {
            // Use RDBMS slot information storing
            slotAgent = new DatabaseSlotAgent();
        } else if ("HAZELCAST".equalsIgnoreCase(slotMgtMode)) {
            // Use Hazelcast slot information storing
            slotAgent = HazelcastAgent.getInstance();
        } else {
            throw new RuntimeException("Unknown slot management storage mode \"" + slotMgtMode + "\"");
        }
        log.info("Using " + slotMgtMode + " based slot management mode");
        firstMessageId = INITIAL_MESSAGE_ID;
        slotRecoveryScheduled = new AtomicBoolean(false);

    }

    /**
     * @return SlotManagerClusterMode instance
     */
    public static SlotManagerClusterMode getInstance() {
        return slotManager;
    }

    /**
     * Get a slot by giving the queue name. This method first lookup the free slot pool for slots
     * and if there are no slots in the free slot pool then return a newly created slot
     *
     * @param queueName name of the queue
     * @return Slot object
     */
    public Slot getSlot(String queueName, String nodeId) throws AndesException {

        Slot slotToBeAssigned;

        /**
         *First look in the unassigned slots pool for free slots. These slots are previously own by
         * other nodes
         */
        String lockKey = queueName + SlotManagerClusterMode.class;
        synchronized (lockKey.intern()) {
            slotToBeAssigned = getUnassignedSlot(queueName);

            if (null == slotToBeAssigned) {
                slotToBeAssigned = getOverlappedSlot(nodeId, queueName);
            }
            if (null == slotToBeAssigned) {
                slotToBeAssigned = getFreshSlot(queueName, nodeId);
            }

            if (null != slotToBeAssigned) {
                updateSlotAssignmentMap(queueName, slotToBeAssigned, nodeId);
                if (log.isDebugEnabled()) {
                    log.debug("Assigning slot for node : " + nodeId + " | " + slotToBeAssigned);
                }
            }
        }

        return slotToBeAssigned;

    }

    /**
     * Create a new slot from store
     *
     * @param queueName name of the queue
     * @param nodeId    id of the node
     * @return slot object
     */
    private Slot getFreshSlot(String queueName, String nodeId) throws AndesException {

        Slot slotToBeAssigned = null;
        TreeSet<Long> messageIDSet;
        // Get message id set from database
        messageIDSet = slotAgent.getMessageIds(queueName);

        if (null != messageIDSet && !(messageIDSet.isEmpty())) {

            slotToBeAssigned = new Slot();
            //start msgID will be last assigned ID + 1 so that slots are created with no
            // message ID gaps in-between
            long lastAssignedId = slotAgent.getQueueToLastAssignedId(queueName);

            if (0L != lastAssignedId) {
                slotToBeAssigned.setStartMessageId(lastAssignedId + 1);
            } else {
                slotToBeAssigned.setStartMessageId(0L);
            }

            //end messageID will be the lowest in published message ID list. Get and remove
            slotToBeAssigned.setEndMessageId(messageIDSet.pollFirst());

            //remove polled message id from database
            slotAgent.deleteMessageId(queueName, slotToBeAssigned.getEndMessageId());

            //set storage queue name (db queue to read messages from)
            slotToBeAssigned.setStorageQueueName(queueName);

            //modify last assigned ID by queue to database
            slotAgent.createSlot(slotToBeAssigned.getStartMessageId(), slotToBeAssigned.getEndMessageId(),
                                 slotToBeAssigned.getStorageQueueName(), nodeId);

            slotAgent.setQueueToLastAssignedId(queueName, slotToBeAssigned.getEndMessageId());

            if (log.isDebugEnabled()) {
                log.debug("Giving a slot from fresh pool. Slot: " + slotToBeAssigned.getId());
            }
        }
        return slotToBeAssigned;

    }

    /**
     * Get an unassigned slot (slots dropped by sudden subscription closes)
     *
     * @param queueName name of the queue slot is required
     * @return slot or null if cannot find
     */
    private Slot getUnassignedSlot(String queueName) throws AndesException {
        Slot slotToBeAssigned;
        String lockKey = queueName + SlotManagerClusterMode.class;
        synchronized (lockKey.intern()) {
            //get oldest unassigned slot from database
            slotToBeAssigned = slotAgent.getUnAssignedSlot(queueName);

            if (log.isDebugEnabled()) {
                if (null != slotToBeAssigned) {
                    log.debug("Giving a slot from unassigned slots. Slot: " + slotToBeAssigned +
                                      " to queue: " + queueName);
                }
            }
        }
        return slotToBeAssigned;
    }

    /**
     * Get an overlapped slot by nodeId and the queue name. These are slots
     * which are overlapped with some slots that were acquired by given node
     *
     * @param nodeId    id of the node
     * @param queueName name of the queue slot is required
     * @return slot or null if not found
     */
    private Slot getOverlappedSlot(String nodeId, String queueName) throws AndesException {
        Slot slotToBeAssigned;
        String lockKey = queueName + SlotManagerClusterMode.class;
        synchronized (lockKey.intern()) {
            //get oldest overlapped slot from database
            slotToBeAssigned = slotAgent.getOverlappedSlot(nodeId, queueName);
            if (log.isDebugEnabled()) {
                if (null != slotToBeAssigned) {
                    log.debug(" Giving overlapped slot id=" + slotToBeAssigned.getId() + " queue name= " + queueName);
                }
            }
        }
        return slotToBeAssigned;
    }

    /**
     * Update the slot assignment when a slot is assigned for a node
     *
     * @param queueName     Name of the queue
     * @param allocatedSlot Slot object which is allocated to a particular node
     * @param nodeId        ID of the node to which slot is Assigned
     */
    private void updateSlotAssignmentMap(String queueName, Slot allocatedSlot, String nodeId) throws AndesException {
        //Lock is used because this method will be called by multiple nodes at the same time
        String lockKey = nodeId + SlotManagerClusterMode.class;
        synchronized (lockKey.intern()) {
            //Update assigned node, assigned queue and set state to assigned
            slotAgent.updateSlotAssignment(nodeId, queueName, allocatedSlot);
        }
    }

    /**
     * Record Slot's last message ID related to a particular queue
     *
     * @param queueName               name of the queue which this message ID belongs to
     * @param lastMessageIdInTheSlot  last message ID of the slot
     * @param startMessageIdInTheSlot start message ID of the slot
     * @param nodeId                  Node ID of the node that is sending the request.
     * @param localSafeZone           Local safe zone of the requesting node.
     */
    public void updateMessageID(String queueName, String nodeId, long startMessageIdInTheSlot,
                                long lastMessageIdInTheSlot, long localSafeZone) throws AndesException {

        //setting up first message id of the slot
        if (firstMessageId > startMessageIdInTheSlot || firstMessageId == -1) {
            firstMessageId = startMessageIdInTheSlot;
        }

        if (slotRecoveryScheduled.get()) {
            queuesToRecover.remove(queueName);
        }

        // Read message Id set for slots from store
        TreeSet<Long> messageIdSet;
        messageIdSet = slotAgent.getMessageIds(queueName);

        String lockKey = queueName + SlotManagerClusterMode.class;
        synchronized (lockKey.intern()) {
            //Get last assigned message id from database
            long lastAssignedMessageId = slotAgent.getQueueToLastAssignedId(queueName);

            // Check if input slot's start message ID is less than last assigned message ID
            if (startMessageIdInTheSlot < lastAssignedMessageId) {
                if (log.isDebugEnabled()) {
                    log.debug("Found overlapping slots during slot submit: " +
                                      startMessageIdInTheSlot + " to : " + lastMessageIdInTheSlot +
                                      ". Comparing to lastAssignedID : " + lastAssignedMessageId);
                }
                // Find overlapping slots
                TreeSet<Slot> overlappingSlots = getOverlappedAssignedSlots(queueName, startMessageIdInTheSlot,
                                                                            lastMessageIdInTheSlot);

                if (!(overlappingSlots.isEmpty())) {

                    if (log.isDebugEnabled()) {
                        log.debug("Found " + overlappingSlots.size() + " overlapping slots.");
                    }
                    // Following means that we have a piece of the slot exceeding the earliest
                    // assigned slot. breaking that piece and adding it as a new,unassigned slot.
                    if (startMessageIdInTheSlot < overlappingSlots.first().getStartMessageId()) {
                        Slot leftExtraSlot = new Slot(startMessageIdInTheSlot, overlappingSlots.first().
                                getStartMessageId() - 1, queueName);
                        if (log.isDebugEnabled()) {
                            log.debug("Left Extra Slot in overlapping slots : " + leftExtraSlot);
                        }
                    }
                    // This means that we have a piece of the slot exceeding the latest assigned slot.
                    // breaking that piece and adding it as a new,unassigned slot.
                    if (lastMessageIdInTheSlot > overlappingSlots.last().getEndMessageId()) {
                        Slot rightExtraSlot = new Slot(overlappingSlots.last().getEndMessageId() + 1,
                                                       lastMessageIdInTheSlot, queueName);

                        if (log.isDebugEnabled()) {
                            log.debug("RightExtra in overlapping slot : " + rightExtraSlot);
                        }
                        //Update last message ID - expand ongoing slot to cater this leftover part.
                        slotAgent.addMessageId(queueName, lastMessageIdInTheSlot);

                        if (log.isDebugEnabled()) {
                            log.debug(lastMessageIdInTheSlot + " added to store " +
                                              "(RightExtraSlot). Current values in " +
                                              "store " + messageIdSet);
                        }
                    }
                } else {
                    /*
                     * The fact that the slot ended up in this condition means that, all previous slots within this
                     * range have been already processed and deleted. This is a very rare scenario.
                     */
                    if (log.isDebugEnabled()) {
                        log.debug("A submit slot request has come from the past after deletion of any " +
                                          "possible overlapping slots. nodeId : " + nodeId + " StartMessageID : " +
                                          startMessageIdInTheSlot + " EndMessageID : " + lastMessageIdInTheSlot);
                    }

                    slotAgent.addMessageId(queueName, lastMessageIdInTheSlot);
                }
            } else {
                //Update the store only if the last assigned message ID is less than the new start message ID
                slotAgent.addMessageId(queueName, lastMessageIdInTheSlot);

                if (log.isDebugEnabled()) {
                    log.debug("No overlapping slots found during slot submit " + startMessageIdInTheSlot + " to : " +
                                      lastMessageIdInTheSlot + ". Added msgID " +
                                      lastMessageIdInTheSlot + " to store");
                }
            }
            //record local safe zone
            slotAgent.setLocalSafeZoneOfNode(nodeId, localSafeZone);
        }
    }

    /**
     * This method will reassigned slots which are owned by a node to a free slots pool
     *
     * @param nodeId node ID of the leaving node
     */
    public void reassignSlotsWhenMemberLeaves(String nodeId) throws AndesException {

        TreeSet<Slot> assignedSlotsSet;
        //Get all assigned nodes by node id
        assignedSlotsSet = slotAgent.getAssignedSlotsByNodeId(nodeId);
        if (null != assignedSlotsSet) {
            for (Slot slotToBeReAssigned : assignedSlotsSet) {
                //Re-assign only if the slot is not empty
                if (!(SlotUtils.checkSlotEmptyFromMessageStore(slotToBeReAssigned))) {
                    slotAgent.reassignSlot(slotToBeReAssigned);
                    if (log.isDebugEnabled()) {
                        log.debug("Returned slot " + slotToBeReAssigned + "from node " +
                                          nodeId + " as member left");
                    }
                } else { // Delete empty slots
                    SlotDeletionExecutor.getInstance().executeSlotDeletion(slotToBeReAssigned);
                }
            }
            slotAgent.deleteOverlappedSlots(nodeId);
        } else {
            if (log.isDebugEnabled()) {
                log.debug("No slots to return from node " + nodeId + " as member left");
            }
        }
    }

    /**
     * Remove slot entry from slot assignment
     *
     * @param storageQueueName name of the queue which is owned by the slot to be deleted
     * @param emptySlot        reference of the slot to be deleted
     */
    public boolean deleteSlot(String storageQueueName, Slot emptySlot, String nodeId) throws AndesException {
        boolean slotDeleted = false;

        long startMsgId = emptySlot.getStartMessageId();
        long endMsgId = emptySlot.getEndMessageId();
        long slotDeleteSafeZone = getSlotDeleteSafeZone();
        if (log.isDebugEnabled()) {
            log.debug("Trying to delete slot. safeZone= " + getSlotDeleteSafeZone() + " startMsgID: " + startMsgId);
        }
        if (slotDeleteSafeZone > endMsgId) {
            String lockKey = nodeId + SlotManagerClusterMode.class;
            synchronized (lockKey.intern()) {
                slotDeleted = slotAgent.deleteSlot(nodeId, storageQueueName, startMsgId, endMsgId);
                if (log.isDebugEnabled()) {
                    log.debug(" Deleted slot id = " + emptySlot.getId() + " queue name = " + storageQueueName
                                      + " deleteSuccess: " + slotDeleted);
                }
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Cannot delete slot as it is within safe zone " + "startMsgID= " + startMsgId + " safeZone= "
                                  + slotDeleteSafeZone + " endMsgId= " + endMsgId + " slotToDelete= " + emptySlot);
            }
        }
        return slotDeleted;
    }

    /**
     * Re-assign the slot when there are no local subscribers in the node
     *
     * @param nodeId    node ID of the node without subscribers
     * @param queueName name of the queue whose slots to be reassigned
     */
    public void reAssignSlotWhenNoSubscribers(String nodeId, String queueName) throws AndesException {
        String lockKeyForNodeId = nodeId + SlotManagerClusterMode.class;
        synchronized (lockKeyForNodeId.intern()) {
            slotAgent.deleteSlotAssignmentByQueueName(nodeId, queueName);
            if (log.isDebugEnabled()) {
                log.debug("Cleared assigned slots of queue " + queueName + " Assigned to node " +
                                  nodeId);
            }
        }
    }

    protected Long getLocalSafeZone(String nodeID) throws AndesException {
        Long lastPublishId;
        lastPublishId = slotAgent.getLocalSafeZoneOfNode(nodeID);
        return lastPublishId;
    }

    protected Set<String> getMessagePublishedNodes() throws AndesException {
        return slotAgent.getMessagePublishedNodes();
    }

    /**
     * Get slotDeletion safe zone. Slots can only be removed if their start message id is
     * beyond this zone.
     *
     * @return current safe zone value
     */
    public long getSlotDeleteSafeZone() {
        return slotDeleteSafeZoneCalc.getSlotDeleteSafeZone();
    }

    /**
     * Record safe zone by node. This ping comes from nodes as messages are not published by them
     * so that safe zone value keeps moving ahead.
     *
     * @param nodeID         ID of the node
     * @param safeZoneOfNode safe zone value of the node
     * @return current calculated safe zone
     */
    public long updateAndReturnSlotDeleteSafeZone(String nodeID, long safeZoneOfNode) {
        try {
            slotAgent.setLocalSafeZoneOfNode(nodeID, safeZoneOfNode);
        } catch (AndesException e) {
            log.error("Error occurred while updating safezone value " + safeZoneOfNode + " for node " + nodeID, e);
        }
        return slotDeleteSafeZoneCalc.getSlotDeleteSafeZone();
    }

    /**
     * Delete all slot associations with a given queue. This is required to handle a queue purge event.
     *
     * @param queueName name of destination queue
     */
    public void clearAllActiveSlotRelationsToQueue(String queueName) throws AndesException {
        if (log.isDebugEnabled()) {
            log.debug("Clearing all slots for queue " + queueName);
        }
        //Clear related slots in slot table
        slotAgent.deleteSlotsByQueueName(queueName);
        //Clear message ids from message id table
        slotAgent.deleteMessageIdsByQueueName(queueName);
    }

    /**
     * Used to shut down the Slot manager in order before closing any dependent services.
     */
    public void shutDownSlotManager() {
        slotDeleteSafeZoneCalc.setRunning(false);
    }

    /**
     * Get an ordered set of existing, assigned slots that overlap with the input slot range.
     *
     * @param queueName  name of destination queue
     * @param startMsgID start message ID of input slot
     * @param endMsgID   end message ID of input slot
     * @return TreeSet<Slot>c
     */
    private TreeSet<Slot> getOverlappedAssignedSlots(String queueName, long startMsgID, long endMsgID)
            throws AndesException {

        TreeSet<Slot> overlappedSlots = new TreeSet<>();
        TreeSet<Slot> assignedOverlappingSlots = new TreeSet<>();

        String lockKey = queueName + SlotManagerClusterMode.class;

        synchronized (lockKey.intern()) {
            // Get all slots created for given queue name
            TreeSet<Slot> slotListForQueue = slotAgent.getAllSlotsByQueueName(queueName);

            // Check each slot for overlapped slots
            for (Slot slot : slotListForQueue) {
                if (endMsgID < slot.getStartMessageId()) {
                    continue; // skip this one, its below our range
                }
                if (startMsgID > slot.getEndMessageId()) {
                    continue; // skip this one, its above our range
                }

                if (SlotState.ASSIGNED == slot.getCurrentState()) {
                    assignedOverlappingSlots.add(slot);
                }

                // Set slot as overlapped if not skipped
                slot.setAnOverlappingSlot(true);

                if (log.isDebugEnabled()) {
                    log.debug("Marked already assigned slot as an overlapping slot. Slot= " + slot.getId());
                }

                overlappedSlots.add(slot);

                if (log.isDebugEnabled()) {
                    log.debug("Found an overlapping slot : " + slot);
                }
            }
            slotAgent.updateOverlappedSlots(queueName, assignedOverlappingSlots);
        }
        return overlappedSlots;
    }

    /**
     * Recover any messages that are persisted but not notified to the slot coordinator from killed nodes.
     * <p>
     * For instance if a node get killed after persisting messages but before submitting slots,
     * until another message is published to any remaining node a new slot will not be created.
     * Hence these messages will not get delivered until another message is published.
     * <p>
     * Recover mechanism here will schedule tasks for each queue so that if no message get received within the
     * given time period that queue slot manager will create a slot and capture those messages it self.
     *
     * @param deletedNodeId node id of delete node
     */
    public void deletePublisherNode(final String deletedNodeId) {

        int threadPoolCount = 1; // Single thread is suffice for this task
        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("RecoverSlotsThreadPool").build();
        ScheduledExecutorService recoverSlotScheduler = Executors
                .newScheduledThreadPool(threadPoolCount, namedThreadFactory);

        // this is accessed from another thread therefore using a set that supports concurrency

        Set<String> concurrentSet;

        try {
            concurrentSet = Collections
                    .newSetFromMap(new ConcurrentHashMap<String, Boolean>(slotAgent.getAllQueues().size()));
            concurrentSet.addAll(slotAgent.getAllQueues());
            queuesToRecover = concurrentSet;
        } catch (AndesException ex) {
            log.error("Failed to get all queue names", ex);
        }

        recoverSlotScheduler.schedule(new Runnable() {
            @Override
            public void run() {

                try {
                    long lastId = SlotMessageCounter.getInstance().getCurrentNodeSafeZoneId();
                    //TODO: Delete if the queue has not progressed
                    for (String queueName : queuesToRecover) {
                        // Trigger a submit slot for each queue so that new slots are created
                        // for queues that have not published any messages after a node crash
                        try {
                            updateMessageID(queueName, deletedNodeId, lastId - 1, lastId, lastId);
                        } catch (AndesException ex) {
                            log.error("Failed to update message id", ex);
                        }
                    }
                    slotRecoveryScheduled.set(false);
                    try {
                        if (log.isDebugEnabled()) {
                            log.debug("Removing " + deletedNodeId + " from safe zone calculation.");
                        }
                        slotAgent.removePublisherNode(deletedNodeId);
                    } catch (AndesException e) {
                        log.error("Failed to remove publisher node ID from safe zone calculation", e);
                    }

                } catch (Throwable e) {
                    log.error("Error occurred while trying to run recover slot scheduler", e);
                }
            }
        }, SlotMessageCounter.getInstance().slotSubmitTimeout, TimeUnit.MILLISECONDS);

        slotRecoveryScheduled.set(true);

    }

    /**
     * Return last assign message id of slot for given queue when MB cluster mode
     *
     * @param queueName name of destination queue
     * @return last assign message id
     */
    public Long getLastAssignedSlotMessageIdInClusterMode(String queueName) throws AndesException {
        return slotAgent.getQueueToLastAssignedId(queueName);
    }

    /**
     * Clear and reset slot storage
     *
     * @throws AndesException
     */
    public void clearSlotStorage() throws AndesException {
        slotAgent.clearSlotStorage();
    }

}
