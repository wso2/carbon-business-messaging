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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.andes.core.MessagingEngine;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * This class is responsible for deleting slots and scheduling slot deletions.
 */
public class SlotDeletionExecutor {

    private static Log log = LogFactory.getLog(SlotDeletionExecutor.class);


    private LinkedBlockingQueue<Slot> slotsToDelete = new LinkedBlockingQueue<Slot>();

    /**
     * Slot deletion thread factory in one MB node
     */
    private static ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat
            ("SlotDeletionExecutor-%d").build();

    /**
     * Slot deletion executor service
     */
    private ExecutorService slotDeletionExecutorService;

    /**
     * SlotDeletionScheduler instance
     */
    private static SlotDeletionExecutor instance;

    /**
     * Reference of the deletion task.
     */
    private SlotDeletionTask slotDeletionTask;

    /**
     * SlotDeletionExecutor constructor
     */
    private SlotDeletionExecutor() {

    }

    /**
     * Create slot deletion scheduler
     */
    public void init() {
        this.slotDeletionExecutorService = Executors.newSingleThreadExecutor(namedThreadFactory);
        slotDeletionTask = new SlotDeletionTask();
        this.slotDeletionExecutorService.submit(slotDeletionTask);
    }

    /**
     * Slot deletion task running and take slot from queue.
     */
    class SlotDeletionTask implements Runnable {

        /**
         * Condition running the task.
         */
        boolean isLive = true;

        void setLive(boolean live) {
            isLive = live;
        }

        /**
         * Running slot deletion task
         */
        public void run() {
            while (isLive) {
                try {
                    // Slot to attempt current deletion
                    Slot slot = slotsToDelete.poll(1, TimeUnit.SECONDS);

                    // Check current slot to delete is not null
                    if (slot != null) {

                        // Check DB for any remaining messages. (JIRA FIX: MB-1612)
                        // If there are any remaining messages wait till overlapped slot delivers the messages
                        if (MessagingEngine.getInstance().getMessageCountForQueueInRange(
                                slot.getStorageQueueName(), slot.getStartMessageId(), slot.getEndMessageId()) == 0) {
                            // Invoke coordinator to delete slot
                            boolean deleteSuccess = deleteSlotAtCoordinator(slot);
                            if (!deleteSuccess) {
                                // Delete attempt not success, therefore adding slot to the queue
                                slotsToDelete.put(slot);
                            } else {
                                SlotDeliveryWorker slotWorker = SlotDeliveryWorkerManager.getInstance()
                                        .getSlotWorker(slot.getStorageQueueName());

                                // slotWorker can be null.
                                // For instance if the deletion request came from coordinator after a member leaves the
                                // cluster and no subscribers on coordinator node.
                                if (null != slotWorker) {
                                    slotWorker.deleteSlot(slot);
                                }
                            }
                        } else {
                            slotsToDelete.put(slot); // Not deleted. Hence putting back in queue
                        }
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("SlotDeletionTask was interrupted while trying to delete the slot.", e);
                } catch (Throwable throwable) {
                    log.error("Unexpected error occurred while trying to delete the slot.", throwable);
                }
            }
            log.info("SlotDeletionExecutor has shutdown with " + slotsToDelete.size() + " slots to delete.");
        }

        /**
         * Delete slot at coordinator and return delete status
         *
         * @param slot slot to be removed from cluster
         * @return slot deletion status
         */
        private boolean deleteSlotAtCoordinator(Slot slot) {
            boolean deleteSuccess = false;
            try {
                deleteSuccess = MessagingEngine.getInstance().getSlotCoordinator().deleteSlot
                        (slot.getStorageQueueName(), slot);
            } catch (ConnectionException e) {
                log.error("Error while trying to delete the slot " + slot + " Thrift connection failed. " +
                                  "Rescheduling delete.", e);

            }
            return deleteSuccess;
        }
    }

    /**
     * Schedule a slot for deletion. This will continuously try to delete the slot
     * until slot manager informs that the slot is successfully removed
     *
     * @param slot slot to be removed from cluster
     */
    public void executeSlotDeletion(Slot slot) {
        slotsToDelete.add(slot);

    }

    /**
     * Shutdown slot deletion executor service
     */
    public void stopSlotDeletionExecutor() {
        if (slotDeletionExecutorService != null) {
            slotDeletionTask.setLive(false);
            slotDeletionExecutorService.shutdown();
        }
    }

    /**
     * Return slot deletion scheduler object
     *
     * @return SlotDeletionScheduler object
     */
    public static SlotDeletionExecutor getInstance() {
        if (instance == null) {
            instance = new SlotDeletionExecutor();
        }
        return instance;
    }

}
