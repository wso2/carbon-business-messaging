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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.andes.core.internal.outbound;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.andes.core.AndesException;
import org.wso2.carbon.andes.core.DeliverableAndesMetadata;
import org.wso2.carbon.andes.core.DestinationType;
import org.wso2.carbon.andes.core.MessageDeliveryInfo;
import org.wso2.carbon.andes.core.MessageFlusher;
import org.wso2.carbon.andes.core.MessagingEngine;
import org.wso2.carbon.andes.core.ProtocolType;
import org.wso2.carbon.andes.core.internal.slot.ConnectionException;
import org.wso2.carbon.andes.core.internal.slot.Slot;
import org.wso2.carbon.andes.core.internal.slot.SlotCoordinator;
import org.wso2.carbon.andes.core.internal.slot.SlotDeletionExecutor;
import org.wso2.carbon.andes.core.task.Task;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Handle message delivery {@link Task} implementation for a given queue
 */
final class MessageDeliveryTask extends Task {

    private static Log log = LogFactory.getLog(MessageDeliveryTask.class);

    /**
     * Maximum number to retries retrieve metadata list for a given storage
     * queue ( in the errors occur in message stores)
     */
    private static final int MAX_META_DATA_RETRIEVAL_COUNT = 5;
    /**
     * The storage queue name.
     */
    private String storageQueueName;

    /**
     * The destination of the messages in the storage queue.
     */
    private String destinationName;

    /**
     * The Protocol Type of the messages in the storage queue.
     */
    private ProtocolType protocolType;

    /**
     * The Destination Type of the messages in the storage queue.
     */
    private DestinationType destinationType;

    /**
     * Reference to {@link MessageFlusher} to deliver messages
     */
    private MessageFlusher messageFlusher;

    /**
     * Reference to slot coordinator to retrieve slots
     */
    private SlotCoordinator slotCoordinator;

    /**
     * Mapping for slot id to slot
     */
    private Map<String, Slot> slotTrackerMap;

    MessageDeliveryTask(String destination,
                        ProtocolType protocolType,
                        String storageQueueName,
                        DestinationType destinationType,
                        SlotCoordinator slotCoordinator,
                        MessageFlusher messageFlusher) {

        this.destinationName = destination;
        this.destinationType = destinationType;
        this.protocolType = protocolType;
        this.storageQueueName = storageQueueName;
        this.slotCoordinator = slotCoordinator;
        this.messageFlusher = messageFlusher;
        slotTrackerMap = new HashMap<>();

    }

    /**
     * Slot delivery task
     * {@inheritDoc}
     */
    @Override
    public TaskHint call() throws Exception {

        TaskHint taskHint = TaskHint.ACTIVE;
        MessageDeliveryInfo messageDeliveryInfo =
                messageFlusher.getMessageDeliveryInfo(destinationName, protocolType, destinationType);

        // Check in memory buffer in MessageFlusher has room
        if (messageDeliveryInfo.messageBufferHasRoom()) {

            // Get a slot from coordinator.
            Slot currentSlot = requestSlot(storageQueueName);
            currentSlot.setDestinationOfMessagesInSlot(destinationName);

            // If the slot is empty
            if (0 == currentSlot.getEndMessageId()) {

                // If the message buffer in MessageFlusher is not empty send those messages
                if (log.isDebugEnabled()) {
                    log.debug("Received an empty slot from slot manager");
                }
                boolean sentFromMessageBuffer =
                        messageFlusher.sendMessagesInBuffer(messageDeliveryInfo, storageQueueName);
                if (!sentFromMessageBuffer) {
                    taskHint = TaskHint.IDLE; // Didn't do productive work
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Received slot for storage queue " + storageQueueName + " is: " +
                                      currentSlot.getStartMessageId() + " - " + currentSlot.getEndMessageId() +
                                      "Thread Id:" + Thread.currentThread().getId());
                }
                List<DeliverableAndesMetadata> messagesRead = getMetaDataListBySlot(storageQueueName, currentSlot);

                if (CollectionUtils.isNotEmpty(messagesRead)) {
                    if (log.isDebugEnabled()) {
                        log.debug("Number of messages read from slot " + currentSlot.getStartMessageId()
                                          + " - " + currentSlot.getEndMessageId() + " is " + messagesRead.size()
                                          + " storage queue= " + storageQueueName);
                    }

                    Slot trackedSlot = slotTrackerMap.get(currentSlot.getId());
                    if (trackedSlot == null) {
                        slotTrackerMap.put(currentSlot.getId(), currentSlot);
                        trackedSlot = currentSlot;
                    } else {
                        if (log.isDebugEnabled()) {
                            log.debug("Overlapped slot received. Slot ID " + trackedSlot.getId());
                        }
                    }

                    filterOverlappedMessages(trackedSlot, messagesRead);
                    MessageFlusher.getInstance().sendMessageToBuffer(messagesRead, trackedSlot, messageDeliveryInfo);
                    MessageFlusher.getInstance().sendMessagesInBuffer(messageDeliveryInfo, storageQueueName);
                } else {
                    currentSlot.setSlotInActive();
                    SlotDeletionExecutor.getInstance().executeSlotDeletion(currentSlot);
                }
            }

        } else {
            // If there are messages to be sent in the message buffer in MessageFlusher send them
            if (log.isDebugEnabled()) {
                log.debug("The queue " + storageQueueName + " has no room. Thus sending from buffer.");
            }
            messageFlusher.sendMessagesInBuffer(messageDeliveryInfo, storageQueueName);
        }
        return taskHint;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAdd() {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onRemove() {
        onStopDelivery();
    }

    /**
     * unque id of the {@link Task}
     * @return name of storage queue handle by this {@link MessageDeliveryTask}
     */
    @Override
    public String getId() {
        return storageQueueName;
    }

    /**
     * Get a slot from the Slot to deliver ( from the coordinator if the MB is clustered)
     *
     * @param storageQueueName the storage queue name for from which a slot should be returned.
     * @return a {@link Slot}
     * @throws ConnectionException if connectivity to coordinator is lost.
     */
    private Slot requestSlot(String storageQueueName) throws ConnectionException {

        long startTime = System.currentTimeMillis();
        Slot currentSlot = slotCoordinator.getSlot(storageQueueName);
        long endTime = System.currentTimeMillis();

        if (log.isDebugEnabled()) {
            log.debug((endTime - startTime) + " milli seconds to get a slot from slot manager");
        }
        return currentSlot;
    }

    /**
     * Returns a list of {@link org.wso2.carbon.andes.core.AndesMessageMetadata} in specified slot.
     * This method is recursive.
     *
     * @param storageQueueName      storage queue of the slot
     * @param slot                  Retrieve metadata relevant to the given {@link Slot}
     * @param numberOfRetriesBefore retry count for the query
     * @return return a list of {@link org.wso2.carbon.andes.core.AndesMessageMetadata}
     * @throws AndesException
     */
    private List<DeliverableAndesMetadata> getMetadataListBySlot(String storageQueueName, Slot slot,
                                                                 int numberOfRetriesBefore) throws AndesException {

        List<DeliverableAndesMetadata> messagesRead;

        try {

            long firstMsgId = slot.getStartMessageId();
            long lastMsgId = slot.getEndMessageId();
            // Read messages in the slot
            messagesRead = MessagingEngine.getInstance().getMetaDataList(slot, storageQueueName, firstMsgId, lastMsgId);

            if (log.isDebugEnabled()) {
                StringBuilder messageIDString = new StringBuilder();
                for (DeliverableAndesMetadata metadata : messagesRead) {
                    messageIDString.append(metadata.getMessageID()).append(" , ");
                }
                log.debug("Messages Read: " + messageIDString);
            }

        } catch (AndesException aex) {

            if (numberOfRetriesBefore <= MAX_META_DATA_RETRIEVAL_COUNT) {
                String errorMsg = String
                        .format("error occurred retrieving metadata list for slot :" + " %s, retry count = %d",
                                slot.toString(), numberOfRetriesBefore);
                log.error(errorMsg, aex);
                messagesRead = getMetadataListBySlot(storageQueueName, slot, numberOfRetriesBefore + 1);
            } else {
                String errorMsg = String
                        .format("error occurred retrieving metadata list for slot : %s, in final attempt = %d. "
                                        + "this slot will not be delivered and become stale in message store",
                                slot.toString(), numberOfRetriesBefore);
                throw new AndesException(errorMsg, aex);
            }

        }
        return messagesRead;
    }

    /**
     * Returns a list of {@link org.wso2.carbon.andes.core.AndesMessageMetadata} in specified slot
     *
     * @param storageQueueName name of the storage queue which this slot belongs to
     * @param slot             the slot which messages are retrieved.
     * @return a list of {@link org.wso2.carbon.andes.core.AndesMessageMetadata}
     * @throws AndesException an exception if there are errors at message store level.
     */
    private List<DeliverableAndesMetadata> getMetaDataListBySlot(String storageQueueName, Slot slot)
            throws AndesException {
        return getMetadataListBySlot(storageQueueName, slot, 0);
    }

    /**
     * This will remove already buffered messages from the messagesRead list. This is to avoid resending a message.
     *
     * @param slot     Slot which contains the given messages
     * @param messages Messages of the given slots
     */
    private void filterOverlappedMessages(Slot slot, List<DeliverableAndesMetadata> messages) {
        Iterator<DeliverableAndesMetadata> readMessageIterator = messages.iterator();

        // Filter already read messages
        while (readMessageIterator.hasNext()) {
            DeliverableAndesMetadata currentMessage = readMessageIterator.next();
            if (slot.checkIfMessageIsAlreadyAdded(currentMessage.getMessageID())) {
                if (log.isDebugEnabled()) {
                    log.debug("Tracker rejected message id= " + currentMessage.getMessageID() + " from buffering "
                                      + "to deliver. This is an already buffered message");
                }
                readMessageIterator.remove();
            } else {
                currentMessage.changeSlot(slot);
                slot.addMessageToSlotIfAbsent(currentMessage);
            }
        }
    }

    /**
     * Put messages directly to the buffer for re-delivery
     * @param messages list of DeliverableAndesMetadata to be delivered
     */
    void rescheduleMessagesForDelivery(List<DeliverableAndesMetadata> messages) {
        MessageFlusher.getInstance().
                addAlreadyTrackedMessagesToBuffer(destinationName, protocolType, destinationType, messages);
    }

    /**
     * Dump all message status of the slots owned by this slot delivery worker
     *
     * @param fileToWrite file to dump
     * @throws AndesException
     */
    public void dumpAllSlotInformationToFile(File fileToWrite) throws AndesException {
        try {
            FileWriter information = new FileWriter(fileToWrite);
            for (Map.Entry<String, Slot> slotEntry : slotTrackerMap.entrySet()) {
                String slotID = slotEntry.getKey();
                List<DeliverableAndesMetadata> messagesOfSlot = slotEntry.getValue().getAllMessagesOfSlot();
                if (!messagesOfSlot.isEmpty()) {

                    int writerFlushCounter = 0;
                    for (DeliverableAndesMetadata message : messagesOfSlot) {
                        information.append(storageQueueName).append(",").append(slotID).append(",")
                                .append(message.dumpMessageStatus()).append("\n");
                        writerFlushCounter = writerFlushCounter + 1;
                        if (writerFlushCounter % 10 == 0) {
                            information.flush();
                        }
                    }

                    information.flush();
                }
            }

            information.flush();
            information.close();

        } catch (FileNotFoundException e) {
            log.error("File to write is not found", e);
            throw new AndesException("File to write is not found", e);
        } catch (IOException e) {
            log.error("Error while dumping message status to file", e);
            throw new AndesException("Error while dumping message status to file", e);
        }

    }

    /**
     * Submit slot to execute delete
     *
     * @param slot slot to delete
     */
    public void deleteSlot(Slot slot) {
        if (log.isDebugEnabled()) {
            log.debug("Releasing tracking of messages for slot " + slot.toString());
        }
        slot.deleteAllMessagesInSlot();
        slotTrackerMap.remove(slot.getId());

    }

    /**
     * Update slot states when delivery stop
     */
    private void onStopDelivery() {

        MessageFlusher.getInstance().clearUpAllBufferedMessagesForDelivery(destinationName, destinationType);

        for (Slot slot : slotTrackerMap.values()) {
            if (log.isDebugEnabled()) {
                log.debug("Orphan slot situation and clear tracking of messages for slot = " + slot);
            }
            slot.markMessagesOfSlotAsReturned();
        }
    }
}
