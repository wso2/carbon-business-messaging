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
import org.wso2.carbon.andes.core.AndesMessage;
import org.wso2.carbon.andes.core.AndesMessageMetadata;
import org.wso2.carbon.andes.core.AndesQueue;
import org.wso2.carbon.andes.core.MessagingEngine;
import org.wso2.carbon.andes.core.internal.AndesContext;
import org.wso2.carbon.andes.core.internal.configuration.AndesConfigurationManager;
import org.wso2.carbon.andes.core.internal.configuration.enums.AndesConfiguration;
import org.wso2.carbon.andes.core.store.AndesContextStore;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * This class is responsible of counting messages in a slot for each queue
 */
public class SlotMessageCounter {

    private ConcurrentHashMap<String, Slot> queueToSlotMap = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, Long> slotTimeOutMap = new ConcurrentHashMap<>();
    /**
     * Timeout in milliseconds for messages in the slot. When this timeout is exceeded slot will be
     * submitted to the coordinator
     */
    private Long timeOutForMessagesInQueue;

    /**
     * Executor used for Timeout slot submit task
     */
    private final ScheduledExecutorService submitSlotToCoordinatorExecutor;

    private Log log = LogFactory.getLog(SlotMessageCounter.class);
    private static SlotMessageCounter slotMessageCounter = new SlotMessageCounter();
    private final int slotWindowSize;
    private long currentSlotDeleteSafeZone;

    /**
     * Keep track of how many update loops are skipped without messages.
     */
    private int slotSubmitLoopSkipCount;

    private SlotCoordinator slotCoordinator;

    private static final int SLOT_SUBMIT_LOOP_SKIP_COUNT_THRESHOLD = 10;

    /**
     * Time between successive slot submit scheduled tasks.
     * <p>
     * In a slow message publishing scenario, this is the delay for each message for delivery.
     * For instance if we publish one message per minute then each message will have to wait
     * till this timeout before the messages are submitted to the slot coordinator.
     */
    public final int slotSubmitTimeout;

    private SlotMessageCounter() {

        slotSubmitTimeout = AndesConfigurationManager
                .readValue(AndesConfiguration.PERFORMANCE_TUNING_SUBMIT_SLOT_TIMER_PERIOD);

        slotWindowSize = AndesConfigurationManager
                .readValue(AndesConfiguration.PERFORMANCE_TUNING_SLOTS_SLOT_WINDOW_SIZE);

        timeOutForMessagesInQueue = AndesConfigurationManager
                .readValue(AndesConfiguration.PERFORMANCE_TUNING_SLOTS_MESSAGE_ACCUMULATION_TIMEOUT);

        slotSubmitLoopSkipCount = 0;
        slotCoordinator = MessagingEngine.getInstance().getSlotCoordinator();

        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("SlotMessageCounterTimeoutTask")
                .build();
        submitSlotToCoordinatorExecutor = Executors.newScheduledThreadPool(2, namedThreadFactory);
        scheduleSubmitSlotToCoordinatorTimer();
    }

    /**
     * This thread is to record message IDs in slot manager when a timeout is passed
     */
    private void scheduleSubmitSlotToCoordinatorTimer() {
        submitSlotToCoordinatorExecutor
                .scheduleWithFixedDelay(new SlotTimeoutTask(), slotSubmitTimeout, slotSubmitTimeout,
                                        TimeUnit.MILLISECONDS);
    }

    /**
     * Record metadata count in the current slot related to a particular queue.
     *
     * @param messageList AndesMessage list to be record
     */
    public void recordMetadataCountInSlot(Collection<AndesMessage> messageList) {
        for (AndesMessage message : messageList) {
            recordMetadataCountInSlot(message.getMetadata());
        }
    }

    /**
     * Add a new message to the count for the current slot related to a particular queue
     *
     * @param metadata AndesMessageMetadata
     */
    private void recordMetadataCountInSlot(AndesMessageMetadata metadata) {
        String storageQueueName = metadata.getStorageDestination();
        Slot currentSlot = updateQueueToSlotMap(metadata);

        if (checkMessageLimitReached(currentSlot)) {
            try {
                submitSlot(storageQueueName);
            } catch (AndesException e) {
                    /*
                    We do not do anything here since this operation will be run by timeout thread also
                     */
                log.error("Error occurred while connecting to the thrift coordinator " + e.getMessage(), e);
            }
        }
    }

    private void submitCurrentSafeZone(long currentSlotDeleteSafeZone) throws ConnectionException {
        slotCoordinator.updateSlotDeletionSafeZone(currentSlotDeleteSafeZone);
    }

    /**
     * Update in-memory queue to slot map. This method is is not synchronized. Single publisher should access this.
     * Ideally through a disruptor event handler
     *
     * @param metadata Andes metadata whose ID needs to be reported to SlotManager
     * @return Current slot which this metadata belongs to
     */
    private Slot updateQueueToSlotMap(AndesMessageMetadata metadata) {
        String storageQueueName = metadata.getStorageDestination();
        Slot currentSlot = queueToSlotMap.get(storageQueueName);
        if (currentSlot == null) {
            currentSlot = new Slot();
            currentSlot.setStartMessageId(metadata.getMessageId());
            currentSlot.setEndMessageId(metadata.getMessageId());
            currentSlot.setMessageCount(1L);
            queueToSlotMap.put(storageQueueName, currentSlot);
            slotTimeOutMap.put(storageQueueName, System.currentTimeMillis());
        } else {
            long currentMsgCount = currentSlot.getMessageCount();
            long newMessageCount = currentMsgCount + 1;
            currentSlot.setMessageCount(newMessageCount);
            currentSlot.setEndMessageId(metadata.getMessageId());
            queueToSlotMap.put(storageQueueName, currentSlot);
        }
        return currentSlot;
    }

    /**
     * Submit last message ID in the slot to SlotManager.
     *
     * @param storageQueueName name of the queue which this slot belongs to
     */
    public synchronized void submitSlot(String storageQueueName) throws AndesException {
        Slot slot = queueToSlotMap.get(storageQueueName);
        if (null != slot) {
            Long lastSlotUpdateTime = slotTimeOutMap.get(storageQueueName);

            // Check if the number of messages in slot is greater than or equal to slot window size or slot timeout
            // has reached. This is to avoid timer task or disruptor creating smaller/overlapping slots.
            if (checkMessageLimitReached(slot) || checkTimeOutReached(lastSlotUpdateTime)) {
                try {
                    long localSafeZone = inferLocalSafeZone(storageQueueName);
                    slotTimeOutMap.remove(storageQueueName);
                    queueToSlotMap.remove(storageQueueName);
                    slotCoordinator.updateMessageId(storageQueueName, slot.getStartMessageId(), slot.getEndMessageId(),
                                                    localSafeZone);
                } catch (ConnectionException e) {
                    // we only log here since this is called again from timer task if previous attempt failed
                    log.error("Error occurred while connecting to the thrift coordinator.", e);
                }
            }
        }
    }

    /**
     * Figure out if the currentStorageQueue's endMessageID is larger than startMessageID's of other queues. If yes,
     * set the minimum startMessageID from those queues as the local safe Zone.
     *
     * @param currentStorageQueueName
     * @return Local Safe Zone
     */
    private long inferLocalSafeZone(String currentStorageQueueName) {

        long localSafeZone = queueToSlotMap.get(currentStorageQueueName).getEndMessageId();

        for (Map.Entry<String, Slot> queueSlotEntry : queueToSlotMap.entrySet()) {

            if (!queueSlotEntry.getKey().equals(currentStorageQueueName)) {
                localSafeZone = Math.min(queueSlotEntry.getValue().getStartMessageId(), localSafeZone);
            }
        }

        return localSafeZone;
    }

    public void updateSafeZoneForNode(long currentSafeZoneVal) {
        currentSlotDeleteSafeZone = currentSafeZoneVal;
    }

    /**
     * Message id generated through {@link org.wso2.carbon.andes.core.internal.inbound.MessagePreProcessor}.
     * This Id is updated through scheduled task.
     */
    public long getCurrentNodeSafeZoneId() {
        return currentSlotDeleteSafeZone;
    }

    /**
     * @return SlotMessageCounter instance
     */
    public static SlotMessageCounter getInstance() {
        return slotMessageCounter;
    }

    /**
     * Check if the slot window size has exceeded
     *
     * @param slot Slot
     * @return true if slot window size has exceeded
     */
    private boolean checkMessageLimitReached(Slot slot) {
        return slot.getMessageCount() >= slotWindowSize;
    }

    /**
     * Check if we slot is timed out
     *
     * @param lastSlotUpdateTime Last update time of the Slot
     * @return true if slot is timed-out
     */
    private boolean checkTimeOutReached(Long lastSlotUpdateTime) {
        return (System.currentTimeMillis() - lastSlotUpdateTime) >= timeOutForMessagesInQueue;
    }

    /**
     * Shut down worker threads, submitSlotToCoordinatorExecutor so that server can shut down properly without
     * unexpected behaviour.
     */
    public void stop() {
        log.info("Stopping slot timeout task executor");
        submitSlotToCoordinatorExecutor.shutdown();
    }

    /**
     * Iterate through all the queues/topics and do a update message id event to the coordinator node with a offset
     * to the provided message id.
     *
     * @param recoveryMessageId message id that is taken as the seed for the update message id event
     */
    public void sendRecoverySlotSubmit(long recoveryMessageId) {
        try {
            log.info("Starting publisher slot recovery event with recovery message id " + recoveryMessageId);
            AndesContextStore contextStore = AndesContext.getInstance().getAndesContextStore();
            List<AndesQueue> queueList = contextStore.getAllQueuesStored();
            for (AndesQueue queue : queueList) {
                slotCoordinator.updateMessageId(queue.queueName, recoveryMessageId, recoveryMessageId,
                                                currentSlotDeleteSafeZone);
                // NOTE: Two queues can't have the same message id at the MB_SLOT_MESSAGE_ID table hence incrementing.
                // Get fresh slot logic deletes the current 'last-queue-to-message-id' mapping with only the message id
                recoveryMessageId++;
                log.info("Moving last published message id of queue " + queue.queueName + " to " + recoveryMessageId);
            }
            log.info("Publisher slot recovery event completed for " + queueList.size() +
                             " queue(s). Recovery message id " + recoveryMessageId);

        } catch (ConnectionException e) {
            log.error("Error occurred while connecting to Thrift server", e);
        } catch (AndesException e) {
            log.error("Error occurred while executing scheduled submit slot", e);
        }
    }

    /**
     * Message counter periodic task used to update the coordinator with timed-out slots and new safe zone values
     */
    private class SlotTimeoutTask implements Runnable {

        @Override
        public void run() {
            try {
                Set<Map.Entry<String, Long>> slotTimeoutEntries = slotTimeOutMap.entrySet();

                if (!slotTimeoutEntries.isEmpty()) {
                    updateCoordinatorWithTimedOutSlots(slotTimeoutEntries);
                } else {
                    updateCoordinatorWithCurrentSafezone();
                }
                // This is to avoid subsequent executions being suppressed
            } catch (Throwable exception) {
                log.error("Error occurred while executing SlotTimeoutTask", exception);
            }
        }

        /**
         * Find and submit timed out slots to slot coordinator
         *
         * @param slotTimeoutEntries Set of slot last update time entries
         */
        private void updateCoordinatorWithTimedOutSlots(Set<Map.Entry<String, Long>> slotTimeoutEntries) {
            for (Map.Entry<String, Long> entry : slotTimeoutEntries) {

                Long lastSlotUpdateTime = entry.getValue();
                String storageQueueName = entry.getKey();

                if (checkTimeOutReached(lastSlotUpdateTime)) {
                    try {
                        submitSlot(storageQueueName);
                    } catch (AndesException exception) {
                        // We do not do anything here since this thread will be run periodically
                        log.error("Error occurred while connecting to the thrift coordinator ", exception);
                    }
                }
            }
        }

        /**
         * Local nodes safe-zone is sent to the coordinator. This is done to keep the safezone moving forward when
         * there are no publishers in the local node.
         */
        private void updateCoordinatorWithCurrentSafezone() {
            slotSubmitLoopSkipCount++;
            if (slotSubmitLoopSkipCount == SLOT_SUBMIT_LOOP_SKIP_COUNT_THRESHOLD) {
                //update current slot Deletion Safe Zone
                try {

                    long evaluatedSafeZone = currentSlotDeleteSafeZone;

                    // If there are any slots pending submission to coordinator, we must lower the safe zone to their
                    // starting point. If we do not consider pending slots at this calculation, safe zone will fly up
                    // unexpectedly.
                    for (Map.Entry<String, Slot> slotEntry : queueToSlotMap.entrySet()) {
                        if (!checkMessageLimitReached(slotEntry.getValue())) {
                            evaluatedSafeZone = Math.min(slotEntry.getValue().getStartMessageId(), evaluatedSafeZone);
                        }
                    }

                    if (log.isDebugEnabled()) {
                        log.debug("Updating coordinator with local safe zone " + evaluatedSafeZone);
                    }

                    submitCurrentSafeZone(evaluatedSafeZone);
                    currentSlotDeleteSafeZone = evaluatedSafeZone;
                    slotSubmitLoopSkipCount = 0;
                } catch (ConnectionException e) {
                    log.error("Error while sending slot deletion safe zone update", e);
                }
            }
        }
    }

}
