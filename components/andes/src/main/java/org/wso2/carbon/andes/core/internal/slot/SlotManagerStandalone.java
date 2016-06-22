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

import com.gs.collections.impl.map.mutable.ConcurrentHashMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Iterator;
import java.util.TreeSet;

/**
 * This class is  responsible of slot allocating, slot creating, slot re-assigning and slot
 * managing tasks in standalone mode
 */
public class SlotManagerStandalone {

    /**
     * To keep message IDs against queues.
     */
    private ConcurrentHashMap<String, TreeSet<Long>> slotIDMap;

    /**
     * To keep track of last assigned message ID against queue.
     */
    private ConcurrentHashMap<String, Long> queueToLastAssignedIDMap;

    /**
     * Slots which are previously owned and released by last subscriber of this node. Key is the
     * queueName. Value is a tree set of slots
     */
    private ConcurrentHashMap<String, TreeSet<Slot>> unAssignedSlotMap;

    /**
     * To keep track of assigned slots for each queue. Key is the queue name and value is a tree
     * set of slots
     */
    private ConcurrentHashMap<String, TreeSet<Slot>> slotAssignmentMap;

    private static SlotManagerStandalone slotManagerStandalone = new SlotManagerStandalone();

    private static Log log = LogFactory.getLog(SlotManagerStandalone.class);

    private SlotManagerStandalone() {

        /**
         * Initialize distributed maps used in this class
         */
        slotIDMap = new ConcurrentHashMap<>();
        queueToLastAssignedIDMap = new ConcurrentHashMap<>();
        slotAssignmentMap = new ConcurrentHashMap<>();
        unAssignedSlotMap = new ConcurrentHashMap<>();
    }

    /**
     * Get a slot by giving the queue name.
     *
     * @param queueName Name of the queue
     * @return Slot object
     */
    public Slot getSlot(String queueName) {
        Slot slotToBeAssigned;
        String lockKey = queueName + SlotManagerStandalone.class;
        synchronized (lockKey.intern()) {
            //First look at slots which are returned when last subscriber leaves
            slotToBeAssigned = getUnassignedSlot(queueName);
            if (null == slotToBeAssigned) {
                slotToBeAssigned = getFreshSlot(queueName);
                if (log.isDebugEnabled()) {
                    log.debug("Slot Manager - giving a slot from fresh pool. Slot= " + slotToBeAssigned);
                }
            }
            if (null == slotToBeAssigned) {
                if (log.isDebugEnabled()) {
                    log.debug("Slot Manager - returns empty slot for the queue: " + queueName);
                }
            } else {
                updateSlotAssignmentMap(queueName, slotToBeAssigned);
            }
            return slotToBeAssigned;
        }
    }

    /**
     * Get an unassigned slot (slots dropped by sudden subscription closes)
     *
     * @param queueName name of the queue slot is required
     * @return slot or null if cannot find
     */
    private Slot getUnassignedSlot(String queueName) {
        Slot slotToBeAssigned = null;
        TreeSet<Slot> unAssignedSlotSet = unAssignedSlotMap.get(queueName);
        if (null != unAssignedSlotSet) {
            slotToBeAssigned = unAssignedSlotSet.pollFirst();
        }
        return slotToBeAssigned;
    }

    /**
     * Get a new slot from slotIDMap
     *
     * @param queueName Name of the queue
     * @return Slot object
     */
    private Slot getFreshSlot(String queueName) {
        Slot slotToBeAssigned = null;
        TreeSet<Long> messageIDSet = slotIDMap.get(queueName);
        if (null != messageIDSet && !messageIDSet.isEmpty()) {
            slotToBeAssigned = new Slot();
            Long lastAssignedId = queueToLastAssignedIDMap.get(queueName);
            if (lastAssignedId != null) {
                slotToBeAssigned.setStartMessageId(lastAssignedId + 1);
            } else {
                slotToBeAssigned.setStartMessageId(0L);
            }
            slotToBeAssigned.setEndMessageId(messageIDSet.pollFirst());
            slotToBeAssigned.setStorageQueueName(queueName);
            slotIDMap.put(queueName, messageIDSet);
            if (log.isDebugEnabled()) {
                log.debug(slotToBeAssigned.getEndMessageId() + " removed to slotIdMap. Current " +
                                  "values in " +
                                  "map " + messageIDSet);
            }
            queueToLastAssignedIDMap.put(queueName, slotToBeAssigned.getEndMessageId());
        }

        return slotToBeAssigned;

    }

    /**
     * Put an entry to slotAssignmentMap when a slot is assigned to slot delivery worker
     *
     * @param queueName    Name of the queue
     * @param assignedSlot Slot which is assigned to slot delivery worker
     */
    private void updateSlotAssignmentMap(String queueName, Slot assignedSlot) {
        TreeSet<Slot> assignedSlotSet = slotAssignmentMap.get(queueName);
        if (null == assignedSlotSet) {
            assignedSlotSet = new TreeSet<>();
        }
        assignedSlotSet.add(assignedSlot);
        slotAssignmentMap.put(queueName, assignedSlotSet);

    }

    /**
     * Record Slot's last message ID related to a particular queue
     *
     * @param queueName              Name of the queue which this message ID belongs to
     * @param lastMessageIdInTheSlot Last message ID of the slot
     */
    public void updateMessageID(String queueName, Long lastMessageIdInTheSlot) {

        TreeSet<Long> messageIdSet = slotIDMap.get(queueName);
        if (messageIdSet == null) {
            messageIdSet = new TreeSet<>();
        }
        String lockKey = queueName + SlotManagerStandalone.class;
        synchronized (lockKey.intern()) {
            /**
             * Update the slotIDMap
             */
            messageIdSet.add(lastMessageIdInTheSlot);

            slotIDMap.put(queueName, messageIdSet);
            if (log.isDebugEnabled()) {
                log.debug(lastMessageIdInTheSlot + " added to slotIdMap. Current values in " +
                                  "map " + messageIdSet);
            }

        }

    }

    /**
     * Delete slot details when slot is empty. (All the messages are delivered and acknowledgments are
     * returned )
     *
     * @param queueName       Name of the queue
     * @param slotToBeDeleted Slot to be deleted
     * @return Whether deleted or not
     */
    public boolean deleteSlot(String queueName, Slot slotToBeDeleted) {
        String lockKey = queueName + SlotManagerStandalone.class;
        synchronized (lockKey.intern()) {
            TreeSet<Slot> assignedSlotSet = slotAssignmentMap.get(queueName);
            if (null != assignedSlotSet) {
                Iterator assignedSlotIterator = assignedSlotSet.iterator();
                while (assignedSlotIterator.hasNext()) {
                    Slot assignedSlot = (Slot) assignedSlotIterator.next();
                    if (assignedSlot.getEndMessageId() == slotToBeDeleted.getEndMessageId()) {
                        assignedSlotIterator.remove();
                        break;
                    }
                }

            }
        }
        return true;
    }

    /**
     * Re-assign the slot when there are no local subscribers in the node
     *
     * @param queueName Name of the queue
     */
    public void reAssignSlotWhenNoSubscribers(String queueName) {
        TreeSet<Slot> slotsToBeReAssigned = slotAssignmentMap.remove(queueName);
        String lockKey = queueName + SlotManagerStandalone.class;
        if (null != slotsToBeReAssigned) {
            synchronized (lockKey.intern()) {
                TreeSet<Slot> unassignedSlots = unAssignedSlotMap.get(queueName);
                if (null == unassignedSlots) {
                    unassignedSlots = new TreeSet<>();
                }
                for (Slot slotToBeReAssigned : slotsToBeReAssigned) {
                    unassignedSlots.add(slotToBeReAssigned);
                }
                unAssignedSlotMap.put(queueName, unassignedSlots);
            }
        }
    }

    /**
     * Delete all slot associations with a given queue. This is required to handle a queue purge event.
     *
     * @param queueName Name of destination queue
     */
    public void clearAllActiveSlotRelationsToQueue(String queueName) {

        if (null != slotIDMap) {
            slotIDMap.remove(queueName);
        }
        if (null != slotAssignmentMap) {
            slotAssignmentMap.remove(queueName);
        }
        if (null != unAssignedSlotMap) {
            unAssignedSlotMap.remove(queueName);
        }

    }

    /**
     * @return SlotManagerStandalone instance
     */
    public static SlotManagerStandalone getInstance() {
        return slotManagerStandalone;
    }

    /**
     * Return last assign message id of slot for given queue when MB standalone mode
     *
     * @param queueName name of destination queue
     * @return last assign message id
     */
    public Long getLastAssignedSlotMessageIdInStandaloneMode(String queueName) {
        return slotIDMap.get(queueName).last();
    }

}
