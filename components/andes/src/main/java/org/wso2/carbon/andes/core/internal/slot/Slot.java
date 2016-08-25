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
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.andes.core.AndesException;
import org.wso2.carbon.andes.core.DeliverableAndesMetadata;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class stores all the data related to a slot
 */
public class Slot implements Serializable, Comparable<Slot> {

    private static Log log = LogFactory.getLog(Slot.class);

    /**
     * Default Serialization UID
     */
    private static final long serialVersionUID = 1L;

    /**
     * Number of messages in the slot
     */
    private long messageCount;

    /**
     * Start message ID of the slot
     */
    private long startMessageId;

    /**
     * End message ID of the slot
     */
    private long endMessageId;

    /**
     * Keep messages read from the message ranges of the slot. Messages are unique and
     * kept until slot is deleted
     */
    private transient ConcurrentHashMap<Long, DeliverableAndesMetadata> messagesOfSlot;

    /**
     * QueueName which the slot belongs to. This is set when the slot is assigned to a subscriber
     */
    private String storageQueueName;

    /**
     * Keep if slot is active, if not it is eligible to be removed
     */
    private boolean isSlotActive;

    /**
     * Indicates whether the slot is a fresh one or an overlapped one
     */
    private boolean isAnOverlappingSlot;

    /**
     * Keep state of the slot
     */
    private List<SlotState> slotStates;

    /**
     * Keep actual destination of messages in slot
     */
    private String destinationOfMessagesInSlot;

    /**
     * Track the number of undelivered messages in the slot
     */
    private AtomicInteger pendingMessageCount;

    public Slot() {
        this(SlotState.CREATED);
    }

    public Slot(SlotState slotState) {
        isSlotActive = true;
        isAnOverlappingSlot = false;
        this.slotStates = new ArrayList<>();
        slotStates.add(slotState);
        pendingMessageCount = new AtomicInteger();
        messagesOfSlot = new ConcurrentHashMap<>();
    }

    public Slot(long start, long end, String destinationOfMessagesInSlot) {
        this();
        this.startMessageId = start;
        this.endMessageId = end;
        this.destinationOfMessagesInSlot = destinationOfMessagesInSlot;
    }

    public void setStorageQueueName(String storageQueueName) {
        this.storageQueueName = storageQueueName;
    }

    public String getStorageQueueName() {
        return storageQueueName;
    }

    public long getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(long messageCount) {
        this.messageCount = messageCount;
    }

    public long getEndMessageId() {
        return endMessageId;
    }

    public void setEndMessageId(long endMessageId) {
        this.endMessageId = endMessageId;
    }

    public long getStartMessageId() {
        return startMessageId;
    }

    public void setStartMessageId(long startMessageId) {
        this.startMessageId = startMessageId;
    }

    public void setSlotInActive() {
        isSlotActive = false;
    }

    public boolean isSlotActive() {
        return isSlotActive;
    }

    public boolean isAnOverlappingSlot() {
        return isAnOverlappingSlot;
    }

    public void setAnOverlappingSlot(boolean isAnOverlappingSlot) {
        this.isAnOverlappingSlot = isAnOverlappingSlot;
        if (isAnOverlappingSlot) {
            addState(SlotState.OVERLAPPED);
        }
    }

    /**
     * Add a message to messages read by slot if it is not already there
     *
     * @param metadata metadata of the message to add
     */
    public void addMessageToSlotIfAbsent(DeliverableAndesMetadata metadata) {
        messagesOfSlot.putIfAbsent(metadata.getMessageId(), metadata);
    }

    /**
     * Get all messages read from the slot
     *
     * @return list of metadata of messages
     */
    public List<DeliverableAndesMetadata> getAllMessagesOfSlot() {
        return new ArrayList<>(messagesOfSlot.values());
    }

    /**
     * Remove a message read from slot. Deprecated as we remove all tracking
     * at once when slot is deleted
     *
     * @param messageID ID of the message to remove from slot
     */
    @Deprecated
    public void removeMessageFromSlot(long messageID) {
        messagesOfSlot.remove(messageID);
    }

    /**
     * Mark all messages read by this slot as SLOT_RETURNED. Also clear
     * messages in slot
     */
    public void markMessagesOfSlotAsReturned() {
        for (DeliverableAndesMetadata andesMetadata : messagesOfSlot.values()) {
            andesMetadata.markAsStale();
            andesMetadata.markAsSlotReturned();
        }
        messagesOfSlot.clear();
    }

    /**
     * Remove all messages in slot
     */
    public void deleteAllMessagesInSlot() {
        for (DeliverableAndesMetadata messageMetadata : messagesOfSlot.values()) {
            long messageId = messageMetadata.getMessageId();
            messageMetadata.markAsSlotRemoved();
            if (messageMetadata.isOKToDispose()) {
                if (log.isDebugEnabled()) {
                    log.debug("removing tracking object from memory id " + messageId);
                }
            } else {
                log.error("Tracking data for message id " + messageId + " removed while in an invalid state. ("
                                  + messageMetadata.getStatusHistory() + ")");
            }
        }
        messagesOfSlot.clear();
    }

    /**
     * Check if message is already added to messages read by slot.
     *
     * @param messageID ID of the new message to add
     * @return true if message is already added
     */
    public boolean checkIfMessageIsAlreadyAdded(long messageID) {
        boolean messageExists = false;
        if (null != messagesOfSlot.get(messageID)) {
            messageExists = true;
        }
        return messageExists;
    }

    public String getDestinationOfMessagesInSlot() {
        return destinationOfMessagesInSlot;
    }

    public void setDestinationOfMessagesInSlot(String destinationOfMessagesInSlot) {
        this.destinationOfMessagesInSlot = destinationOfMessagesInSlot;
    }

    /**
     * Check if state going to be added is valid considering it as the next
     * transition compared to current latest state.
     *
     * @param state state to be transferred
     * @return true if the transition is valid according to slot model
     */
    public boolean addState(SlotState state) {

        boolean isValidTransition = false;

        if (slotStates.isEmpty()) {
            if (SlotState.CREATED.equals(state)) {
                isValidTransition = true;
                slotStates.add(state);
            } else {
                log.warn("Invalid State transition suggested: " + state);
            }
        } else {
            isValidTransition = slotStates.get(slotStates.size() - 1).isValidNextTransition(state);
            if (isValidTransition) {
                slotStates.add(state);
            } else {
                log.warn("Invalid State transition from " + slotStates.get(slotStates.size() - 1) + " suggested: "
                                 + state + " Slot ID: " + this.getId());
            }
        }

        return isValidTransition;
    }

    /**
     * Return last state of the Slot
     *
     * @return current state of the slot
     */
    public SlotState getCurrentState() {
        return slotStates.get(slotStates.size() - 1);
    }

    /**
     * Convert Slot state list to a string
     *
     * @return Encoded string
     */
    public String encodeSlotStates() {
        String encodedString;
        StringBuilder builder = new StringBuilder();
        for (SlotState slotState : slotStates) {
            builder.append(slotState.getCode()).append("%");
        }
        encodedString = builder.toString();
        return encodedString;
    }

    /**
     * Decode slot states from a string
     *
     * @param stateInfo encoded string
     */
    public void decodeAndSetSlotStates(String stateInfo) {
        String[] states = StringUtils.split(stateInfo, "%");
        slotStates.clear();
        if (states != null) {
            for (String state : states) {
                int code = Integer.parseInt(state);
                slotStates.add(SlotState.parseSlotState(code));
            }
        } else {
            log.warn("Invalid encoded string received. Encoded string: " + stateInfo);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Slot slot = (Slot) o;

        return endMessageId == slot.endMessageId && startMessageId == slot.startMessageId &&
                storageQueueName.equals(slot.storageQueueName);

    }

    @Override
    public int hashCode() {
        int result = (int) (startMessageId ^ (startMessageId >>> 32));
        result = 31 * result + (int) (endMessageId ^ (endMessageId >>> 32));
        result = 31 * result + storageQueueName.hashCode();
        return result;
    }

    @Override
    public String toString() {
        StringBuilder slotInfo = new StringBuilder();
        slotInfo.append(" Id : ").append(this.getId()).append(" States : ").append(this.slotStates)
                .append(" Is overlapping : ").append(isAnOverlappingSlot).append(" Destination : ")
                .append(destinationOfMessagesInSlot);
        return slotInfo.toString();
    }

    /**
     * Return unique id for the slot
     *
     * @return slot message id
     */
    public String getId() {
        return storageQueueName + "|" + startMessageId + "-" + endMessageId;
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public int compareTo(Slot other) {
        if ((this.getStartMessageId() == other.getStartMessageId()) && (this.getEndMessageId() == other
                .getEndMessageId()) && this.getStorageQueueName().equals(other.getStorageQueueName())) {
            return 0;
        } else {
            return this.getStartMessageId() > other.getStartMessageId() ? 1 : -1;
        }
    }

    /**
     * Decrement message count in slot and if it is zero prepare for slot deletion
     */
    public void decrementPendingMessageCount() throws AndesException {
        int messageCount = pendingMessageCount.decrementAndGet();
        if (messageCount == 0) {

            if (log.isDebugEnabled()) {
                log.debug("Slot has no pending messages. Now re-checking slot for messages. " + this.toString());
            }
            setSlotInActive();
            SlotDeletionExecutor.getInstance().executeSlotDeletion(this);
        }
    }

    /**
     * Increment the pending message count in a slot
     */
    public void incrementPendingMessageCount(int amount) {
        pendingMessageCount.addAndGet(amount);
    }
}
