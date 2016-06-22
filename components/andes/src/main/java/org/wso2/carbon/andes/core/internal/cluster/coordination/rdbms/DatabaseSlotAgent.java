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

package org.wso2.carbon.andes.core.internal.cluster.coordination.rdbms;

import com.google.common.util.concurrent.SettableFuture;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.andes.core.AndesException;
import org.wso2.carbon.andes.core.internal.AndesContext;
import org.wso2.carbon.andes.core.internal.cluster.coordination.SlotAgent;
import org.wso2.carbon.andes.core.internal.slot.Slot;
import org.wso2.carbon.andes.core.internal.slot.SlotState;
import org.wso2.carbon.andes.core.store.AndesContextStore;
import org.wso2.carbon.andes.core.store.AndesDataIntegrityViolationException;
import org.wso2.carbon.andes.core.store.AndesStoreUnavailableException;
import org.wso2.carbon.andes.core.store.FailureObservingStoreManager;
import org.wso2.carbon.andes.core.store.HealthAwareStore;
import org.wso2.carbon.andes.core.store.StoreHealthListener;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;

/**
 * If the slotManagement is set to "RDBMS" in broker.xml all operations related to slots are handled through this class
 */
public class DatabaseSlotAgent implements SlotAgent, StoreHealthListener {

    /**
     * Indicates and provides a barrier if messages stores become offline.
     * marked as volatile since this value could be set from a different thread
     */
    private volatile SettableFuture<Boolean> messageStoresUnavailable;

    /**
     * Used for logging purposes
     */
    private static Log log = LogFactory.getLog(DatabaseSlotAgent.class);

    /**
     * Used to perform database operations on the context store.
     */
    private AndesContextStore andesContextStore = AndesContext.getInstance().getAndesContextStore();

    /**
     * Declares the maximum number of times we'll tolerate a store failure
     */
    private static final int MAX_STORE_FAILURE_TOLERANCE_COUNT = 3;

    public DatabaseSlotAgent() {

        // The existence of messageStoresUnavailable indicates a store failure. Therefore, it should be initialized
        // with to mark the operational status of the store
        messageStoresUnavailable = null;

        // Register in the FailureObservingStoreManager to get notifications on store non-operational and operational
        // status;
        FailureObservingStoreManager.registerStoreHealthListener(this);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createSlot(long startMessageId, long endMessageId, String storageQueueName, String assignedNodeId)
            throws AndesException {

        String task = "create slot with start message id: " + startMessageId + ", end message id: " + endMessageId
                + " for queue: " + storageQueueName + " and node: " + assignedNodeId;

        for (int attemptCount = 1; attemptCount <= MAX_STORE_FAILURE_TOLERANCE_COUNT; attemptCount++) {
            waitUntilStoresBecomeAvailable(task);
            try {
                andesContextStore.createSlot(startMessageId, endMessageId, storageQueueName, assignedNodeId);
                break;
            } catch (AndesStoreUnavailableException e) {
                handleFailure(attemptCount, task, e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean deleteSlot(String nodeId, String queueName, long startMessageId, long endMessageId)
            throws AndesException {

        String task = "delete slot with start message id: " + startMessageId + ", end message id: " + endMessageId
                + " for queue: " + queueName + " and node: " + nodeId;

        for (int attemptCount = 1; attemptCount <= MAX_STORE_FAILURE_TOLERANCE_COUNT; attemptCount++) {
            waitUntilStoresBecomeAvailable(task);
            try {
                return andesContextStore.deleteSlot(startMessageId, endMessageId);
            } catch (AndesStoreUnavailableException e) {
                handleFailure(attemptCount, task, e);

            }
        }
        return false;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteSlotAssignmentByQueueName(String nodeId, String queueName) throws AndesException {

        String task = "delete slots for queue: " + queueName + " and node: " + nodeId;

        for (int attemptCount = 1; attemptCount <= MAX_STORE_FAILURE_TOLERANCE_COUNT; attemptCount++) {
            waitUntilStoresBecomeAvailable(task);
            try {
                andesContextStore.deleteSlotAssignmentByQueueName(nodeId, queueName);
                break;
            } catch (AndesStoreUnavailableException e) {
                handleFailure(attemptCount, task, e);
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Slot getUnAssignedSlot(String queueName) throws AndesException {

        String task = "retrieve unassigned slot for queue: " + queueName;
        Slot unassignedSlot = null;
        for (int attemptCount = 1; attemptCount <= MAX_STORE_FAILURE_TOLERANCE_COUNT; attemptCount++) {
            waitUntilStoresBecomeAvailable(task);
            try {
                unassignedSlot = andesContextStore.selectUnAssignedSlot(queueName);
                break;
            } catch (AndesStoreUnavailableException e) {
                handleFailure(attemptCount, task, e);
            }
        }
        return unassignedSlot;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void updateSlotAssignment(String nodeId, String queueName, Slot allocatedSlot)
            throws AndesException {

        String task = "update slot with start message id: " + allocatedSlot.getStartMessageId()
                + " for queue: " + queueName + " and node: " + nodeId;

        for (int attemptCount = 1; attemptCount <= MAX_STORE_FAILURE_TOLERANCE_COUNT; attemptCount++) {
            waitUntilStoresBecomeAvailable(task);
            try {
                andesContextStore.createSlotAssignment(nodeId, queueName, allocatedSlot.getStartMessageId(),
                                                       allocatedSlot.getEndMessageId());
                break;
            } catch (AndesStoreUnavailableException e) {
                handleFailure(attemptCount, task, e);
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public long getQueueToLastAssignedId(String queueName) throws AndesException {

        String task = "get last assigned message id for queue: " + queueName;

        long lastAssignedId = 0;
        for (int attemptCount = 1; attemptCount <= MAX_STORE_FAILURE_TOLERANCE_COUNT; attemptCount++) {
            waitUntilStoresBecomeAvailable(task);
            try {
                lastAssignedId = andesContextStore.getQueueToLastAssignedId(queueName);
                break;
            } catch (AndesStoreUnavailableException e) {
                handleFailure(attemptCount, task, e);
            }
        }
        return lastAssignedId;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void setQueueToLastAssignedId(String queueName, long lastAssignedId) throws AndesException {

        String task = "set last assigned message id: " + lastAssignedId + " for queue: " + queueName;
        for (int attemptCount = 1; attemptCount <= MAX_STORE_FAILURE_TOLERANCE_COUNT; attemptCount++) {
            waitUntilStoresBecomeAvailable(task);
            try {
                andesContextStore.setQueueToLastAssignedId(queueName, lastAssignedId);
                break;
            } catch (AndesStoreUnavailableException e) {
                handleFailure(attemptCount, task, e);
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Long getLocalSafeZoneOfNode(String nodeId) throws AndesException {

        String task = "get last published message id for node: " + nodeId;

        long lastPublishedId = 0;
        for (int attemptCount = 1; attemptCount <= MAX_STORE_FAILURE_TOLERANCE_COUNT; attemptCount++) {
            waitUntilStoresBecomeAvailable(task);
            try {
                lastPublishedId = andesContextStore.getLocalSafeZoneOfNode(nodeId);
                break;
            } catch (AndesStoreUnavailableException e) {
                handleFailure(attemptCount, task, e);
            }
        }
        return lastPublishedId;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void setLocalSafeZoneOfNode(String nodeId, long localSafeZone) throws AndesException {

        String task = "set local safe zone message id: " + localSafeZone + " for node: " + nodeId;

        for (int attemptCount = 1; attemptCount <= MAX_STORE_FAILURE_TOLERANCE_COUNT; attemptCount++) {
            waitUntilStoresBecomeAvailable(task);
            try {
                andesContextStore.setLocalSafeZoneOfNode(nodeId, localSafeZone);
                break;
            } catch (AndesStoreUnavailableException e) {
                handleFailure(attemptCount, task, e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removePublisherNode(String nodeId) throws AndesException {

        String task = "remove publisher node: " + nodeId;

        for (int attemptCount = 1; attemptCount <= MAX_STORE_FAILURE_TOLERANCE_COUNT; attemptCount++) {
            waitUntilStoresBecomeAvailable(task);
            try {
                andesContextStore.removePublisherNodeId(nodeId);
                break;
            } catch (AndesStoreUnavailableException e) {
                handleFailure(attemptCount, task, e);
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public TreeSet<String> getMessagePublishedNodes() throws AndesException {

        String task = "retrieve publisher nodes";

        TreeSet<String> publishedNodes = new TreeSet<>();
        for (int attemptCount = 1; attemptCount <= MAX_STORE_FAILURE_TOLERANCE_COUNT; attemptCount++) {
            waitUntilStoresBecomeAvailable(task);
            try {
                publishedNodes = andesContextStore.getMessagePublishedNodes();
                break;
            } catch (AndesStoreUnavailableException e) {
                handleFailure(attemptCount, task, e);
            }
        }
        return publishedNodes;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void setSlotState(long startMessageId, long endMessageId, SlotState slotState) throws AndesException {

        String task = "set state: " + slotState.name() + " to slot " + "with start message id: " + startMessageId
                + " and end message id: " + endMessageId;

        for (int attemptCount = 1; attemptCount <= MAX_STORE_FAILURE_TOLERANCE_COUNT; attemptCount++) {
            waitUntilStoresBecomeAvailable(task);
            try {
                andesContextStore.setSlotState(startMessageId, endMessageId, slotState);
                break;
            } catch (AndesStoreUnavailableException e) {
                handleFailure(attemptCount, task, e);
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public Slot getOverlappedSlot(String nodeId, String queueName) throws AndesException {

        String task = "get overlapped slots for queue: " + queueName + " and node: " + nodeId;

        Slot overlappedSlot = null;
        for (int attemptCount = 1; attemptCount <= MAX_STORE_FAILURE_TOLERANCE_COUNT; attemptCount++) {
            waitUntilStoresBecomeAvailable(task);
            try {
                overlappedSlot = andesContextStore.getOverlappedSlot(nodeId, queueName);
                break;
            } catch (AndesStoreUnavailableException e) {
                handleFailure(attemptCount, task, e);
            }
        }
        return overlappedSlot;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void addMessageId(String queueName, long messageId) throws AndesException {

        String task = "add message id: " + messageId + " for queue: " + queueName;

        for (int attemptCount = 1; attemptCount <= MAX_STORE_FAILURE_TOLERANCE_COUNT; attemptCount++) {
            waitUntilStoresBecomeAvailable(task);
            try {
                andesContextStore.addMessageId(queueName, messageId);
                break;
            } catch (AndesDataIntegrityViolationException ignore) {
                //Same message id can be added to list when slots are overlapped. In RDBMS slot store
                //composite primary key of queue name and message id have been used to avoid duplicates.
                //Therefore primary key violation exception can be ignored.
            } catch (AndesStoreUnavailableException e) {
                handleFailure(attemptCount, task, e);
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public TreeSet<Long> getMessageIds(String queueName) throws AndesException {

        String task = "get message ids for queue: " + queueName;

        TreeSet<Long> messageIds = new TreeSet<>();
        for (int attemptCount = 1; attemptCount <= MAX_STORE_FAILURE_TOLERANCE_COUNT; attemptCount++) {
            waitUntilStoresBecomeAvailable(task);
            try {
                messageIds = andesContextStore.getMessageIds(queueName);
                break;
            } catch (AndesStoreUnavailableException e) {
                handleFailure(attemptCount, task, e);
            }
        }
        return messageIds;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteMessageId(String queueName, long messageId) throws AndesException {

        String task = "delete slot with start message id: " + messageId + " for queue: " + queueName;
        for (int attemptCount = 1; attemptCount <= MAX_STORE_FAILURE_TOLERANCE_COUNT; attemptCount++) {
            waitUntilStoresBecomeAvailable(task);
            try {
                andesContextStore.deleteMessageId(messageId);
                break;
            } catch (AndesStoreUnavailableException e) {
                handleFailure(attemptCount, task, e);
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteSlotsByQueueName(String queueName) throws AndesException {

        String task = "delete slot assignments for queue: " + queueName;
        for (int attemptCount = 1; attemptCount <= MAX_STORE_FAILURE_TOLERANCE_COUNT; attemptCount++) {
            waitUntilStoresBecomeAvailable(task);
            try {
                andesContextStore.deleteSlotsByQueueName(queueName);
                break;
            } catch (AndesStoreUnavailableException e) {
                handleFailure(attemptCount, task, e);
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteMessageIdsByQueueName(String queueName) throws AndesException {

        String task = "delete slots for queue: " + queueName;

        for (int attemptCount = 1; attemptCount <= MAX_STORE_FAILURE_TOLERANCE_COUNT; attemptCount++) {
            waitUntilStoresBecomeAvailable(task);
            try {
                andesContextStore.deleteMessageIdsByQueueName(queueName);
                break;
            } catch (AndesStoreUnavailableException e) {
                handleFailure(attemptCount, task, e);
            }
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public TreeSet<Slot> getAssignedSlotsByNodeId(String nodeId) throws AndesException {

        String task = "retrieve assigned slots for node: " + nodeId;

        TreeSet<Slot> assignedSlots = new TreeSet<>();
        for (int attemptCount = 1; attemptCount <= MAX_STORE_FAILURE_TOLERANCE_COUNT; attemptCount++) {
            waitUntilStoresBecomeAvailable(task);
            try {
                assignedSlots = andesContextStore.getAssignedSlotsByNodeId(nodeId);
                break;
            } catch (AndesStoreUnavailableException e) {
                handleFailure(attemptCount, task, e);
            }
        }
        return assignedSlots;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public TreeSet<Slot> getAllSlotsByQueueName(String queueName) throws AndesException {

        String task = "retrieve all slots for queue: " + queueName;

        TreeSet<Slot> allSlotsForQueue = new TreeSet<>();
        for (int attemptCount = 1; attemptCount <= MAX_STORE_FAILURE_TOLERANCE_COUNT; attemptCount++) {
            waitUntilStoresBecomeAvailable(task);
            try {
                allSlotsForQueue = andesContextStore.getAllSlotsByQueueName(queueName);
                break;
            } catch (AndesStoreUnavailableException e) {
                handleFailure(attemptCount, task, e);
            }
        }
        return allSlotsForQueue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void reassignSlot(Slot slotToBeReassigned) throws AndesException {

        String task = "delete slot assignment with start message id: " + slotToBeReassigned.getStartMessageId()
                + " and end message id: " + slotToBeReassigned.getEndMessageId();

        for (int attemptCount = 1; attemptCount <= MAX_STORE_FAILURE_TOLERANCE_COUNT; attemptCount++) {
            waitUntilStoresBecomeAvailable(task);
            try {
                andesContextStore.deleteSlotAssignment(slotToBeReassigned.getStartMessageId(),
                                                       slotToBeReassigned.getEndMessageId());
                break;
            } catch (AndesStoreUnavailableException e) {
                handleFailure(attemptCount, task, e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteOverlappedSlots(String nodeId) throws AndesException {
        //Not necessary in RDBMS mode, because RDBMS mode uses single table to store information
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateOverlappedSlots(String queueName, TreeSet<Slot> overlappedSlots)
            throws AndesException {

        //We don't need to check whether the store is available here since it is handled in the lower level method
        for (Slot slot : overlappedSlots) {
            this.setSlotState(slot.getStartMessageId(), slot.getEndMessageId(), SlotState.OVERLAPPED);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> getAllQueues() throws AndesException {

        String task = "retrieve all queue";

        Set<String> allQueues = new HashSet<>();
        for (int attemptCount = 1; attemptCount <= MAX_STORE_FAILURE_TOLERANCE_COUNT; attemptCount++) {
            waitUntilStoresBecomeAvailable(task);
            try {
                allQueues = andesContextStore.getAllQueues();
                break;
            } catch (AndesStoreUnavailableException e) {
                handleFailure(attemptCount, task, e);
            }
        }
        return allQueues;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clearSlotStorage() throws AndesException {

        String task = "clear slot storage";
        for (int attemptCount = 1; attemptCount <= MAX_STORE_FAILURE_TOLERANCE_COUNT; attemptCount++) {
            waitUntilStoresBecomeAvailable(task);
            try {
                andesContextStore.clearSlotStorage();
                break;
            } catch (AndesStoreUnavailableException e) {
                handleFailure(attemptCount, task, e);
            }
        }
    }

    /**
     * Method to block the thread until the stores become available.
     */
    private void waitUntilStoresBecomeAvailable(String task) throws AndesException {

        // If messageStoresUnavailable is not null, that means that the store is non-operational
        if (null != messageStoresUnavailable) {
            log.info("Context store has become unavailable while trying to " + task + " therefore waiting"
                             + " until stores become available. thread id: " + Thread.currentThread().getId());
            try {
                // Block the thread until the store becomes operational
                messageStoresUnavailable.get();
                messageStoresUnavailable = null;
            } catch (InterruptedException e) {
                throw new AndesException("Thread interrupted while waiting for message stores to come online", e);
            } catch (ExecutionException e) {
                throw new AndesException("Error occurred while waiting for message stores to come online", e);
            }
        }
    }

    /**
     * Handle Store unavailable exception.
     * If we have tried MAX_STORE_FAILURE_TOLERANCE_COUNT times to perform operation, this failure means that
     * the database connection is frequently getting disconnected. We cannot handle such a situation.
     * Therefore,log the failure and abort operation
     * Else, we'll just ignore the failure, it'll be handled from the level above
     *
     * @param attemptCount the number times we have tried to perform the database operation
     * @param operation    The String representation of the operation that was being performed with the relevant
     *                     parameters
     * @param exception    The exception that was thrown
     * @throws AndesException in the case where we have tried tha maximum possible times, throw the exception
     */
    private void handleFailure(int attemptCount, String operation, AndesStoreUnavailableException exception)
            throws AndesException {

        if (MAX_STORE_FAILURE_TOLERANCE_COUNT == attemptCount) {
            log.info("Number of maximum tolerances of store failure has been reached while trying to " + operation
                             + ". Aborting operation");
            throw exception;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void storeOperational(HealthAwareStore store) {
        log.info("Context store became operational. Therefore, resuming Database Slot Agent");
        messageStoresUnavailable.set(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void storeNonOperational(HealthAwareStore store, Exception ex) {
        log.info("Context store became non-operational. Therefore, blocking Database Slot Agent");
        messageStoresUnavailable = SettableFuture.create();
    }

}
