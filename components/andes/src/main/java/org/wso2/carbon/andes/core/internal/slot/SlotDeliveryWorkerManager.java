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
import org.wso2.carbon.andes.core.DeliverableAndesMetadata;
import org.wso2.carbon.andes.core.DestinationType;
import org.wso2.carbon.andes.core.ProtocolType;
import org.wso2.carbon.andes.core.internal.configuration.AndesConfigurationManager;
import org.wso2.carbon.andes.core.internal.configuration.enums.AndesConfiguration;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * This class is responsible of allocating SloDeliveryWorker threads to each queue
 */
public class SlotDeliveryWorkerManager {

    private Map<Integer, SlotDeliveryWorker> slotDeliveryWorkerMap = new ConcurrentHashMap<>();

    private ExecutorService slotDeliveryWorkerExecutor;

    private static Log log = LogFactory.getLog(SlotDeliveryWorkerManager.class);

    /**
     * Number of slot delivery worker threads running inn one MB node
     */
    private static ThreadFactory namedThreadFactory = new ThreadFactoryBuilder()
            .setNameFormat("SlotDeliveryWorkerExecutor-%d").build();

    /**
     * Number of slot delivery worker threads running in one MB node
     */
    private Integer numberOfThreads;

    /**
     * SlotDeliveryWorker instance
     */
    private static SlotDeliveryWorkerManager slotDeliveryWorkerManagerManager = new SlotDeliveryWorkerManager();

    private SlotDeliveryWorkerManager() {
        numberOfThreads = AndesConfigurationManager
                .readValue(AndesConfiguration.PERFORMANCE_TUNING_SLOTS_WORKER_THREAD_COUNT);
        this.slotDeliveryWorkerExecutor = Executors.newFixedThreadPool(numberOfThreads, namedThreadFactory);
    }

    /**
     * @return SlotDeliveryWorkerManager instance
     */
    public static SlotDeliveryWorkerManager getInstance() {
        return slotDeliveryWorkerManagerManager;
    }

    public void rescheduleMessagesForDelivery(String storageQueueName, List<DeliverableAndesMetadata> messages) {
        SlotDeliveryWorker slotWorker = getSlotWorker(storageQueueName);

        if (null != slotWorker) {
            slotWorker.rescheduleMessagesForDelivery(storageQueueName, messages);
        }
    }

    /**
     * When a subscription is added this method will be called. This method will decide which
     * SlotDeliveryWorker thread is assigned to which queue. If a worker is already running on
     * the queue, it will not start a new one.
     *
     * @param storageQueueName name of the queue to start slot delivery worker for
     * @param destination      The destination name
     * @param protocolType     The protocol which the messages in this storage queue belongs to
     * @param destinationType  The destination type which the messages in this storage queue belongs to
     */
    public synchronized void startSlotDeliveryWorker(String storageQueueName, String destination,
                                                     ProtocolType protocolType, DestinationType destinationType)
            throws AndesException {
        int slotDeliveryWorkerId = getIdForSlotDeliveryWorker(storageQueueName);
        if (getSlotDeliveryWorkerMap().containsKey(slotDeliveryWorkerId)) {
            //if this queue is not already in the queue
            if (!getSlotDeliveryWorkerMap().get(slotDeliveryWorkerId).isStorageQueueAdded(storageQueueName)) {
                SlotDeliveryWorker slotDeliveryWorker = getSlotDeliveryWorkerMap().get(slotDeliveryWorkerId);
                slotDeliveryWorker.startDeliveryForQueue(storageQueueName, destination, protocolType, destinationType);
                // In case the SlotDeliveryWorker has been stopped due to 0 active subscribers, we must re-start it.
                if (!slotDeliveryWorker.isRunning()) {
                    if (log.isDebugEnabled()) {
                        log.debug("SlotDeliveryWorker : " + slotDeliveryWorker.getId()
                                          + "has been stopped. Therefore restarting.");
                    }
                    slotDeliveryWorker.setRunning(true);
                    slotDeliveryWorkerExecutor.execute(slotDeliveryWorker);
                }
                if (log.isDebugEnabled()) {
                    log.debug("Assigned Already Running Slot Delivery Worker. Reading messages storageQ= "
                                      + storageQueueName + " MsgDest= " + destination);
                }
            }
        } else {
            SlotDeliveryWorker slotDeliveryWorker = new SlotDeliveryWorker();
            if (log.isDebugEnabled()) {
                log.debug("Slot Delivery Worker Started. Reading messages storageQ= " + storageQueueName + " MsgDest= "
                                  + destination);
            }
            slotDeliveryWorker.startDeliveryForQueue(storageQueueName, destination, protocolType, destinationType);
            getSlotDeliveryWorkerMap().put(slotDeliveryWorkerId, slotDeliveryWorker);
            slotDeliveryWorkerExecutor.execute(slotDeliveryWorker);
        }
    }

    /**
     * This method is to decide slotDeliveryWorkerId for the queue
     *
     * @param queueName name of the newly created queue
     * @return slot delivery worker ID
     */
    public int getIdForSlotDeliveryWorker(String queueName) {
        // Get the absolute value since String.hashCode() can give both positive and negative values.
        return Math.abs(queueName.hashCode() % numberOfThreads);
    }

    /**
     * Stop delivery task for the given storage queue locally. This is normally called when all the subscribers for a
     * destination leave the local node.
     *
     * @param storageQueueName Name of the Storage queue
     */
    public void stopDeliveryForDestination(String storageQueueName) {
        SlotDeliveryWorker slotWorker = getSlotWorker(storageQueueName);

        // Check if there is a slot delivery worker for the storageQueueName
        if (null != slotWorker) {
            if (log.isDebugEnabled()) {
                log.debug("Stopping delivery for storage queue " + storageQueueName +
                                  " with SlotDeliveryWorker : " + slotWorker.getId());
            }

            /* synchronizing so that while stopDelivery is completing, startDelivery will be waiting if called.
              If, stop delivery is to stop the delivery worker since no subscribers, it has to be completed
              before calling the start delivery again for the same SlotDeliveryWorker */
            synchronized (this) {
                slotWorker.stopDeliveryForQueue(storageQueueName);
            }
        }
    }

    /**
     * Stop all stop delivery workers in the thread pool
     */
    public void stopSlotDeliveryWorkers() {
        for (Map.Entry<Integer, SlotDeliveryWorker> slotDeliveryWorkerEntry : getSlotDeliveryWorkerMap().entrySet()) {
            slotDeliveryWorkerEntry.getValue().setRunning(false);
        }
    }

    /**
     * @return SlotDeliveryWorkerMap  a map which stores slot delivery worker ID against
     * SlotDelivery
     * Worker
     * object references
     */
    private Map<Integer, SlotDeliveryWorker> getSlotDeliveryWorkerMap() {
        return slotDeliveryWorkerMap;
    }

    /**
     * Start all the SlotDeliveryWorkers if not already in running state.
     */
    public void startAllSlotDeliveryWorkers() {
        for (Map.Entry<Integer, SlotDeliveryWorker> entry : slotDeliveryWorkerMap.entrySet()) {
            SlotDeliveryWorker slotDeliveryWorker = entry.getValue();
            if (!slotDeliveryWorker.isRunning()) {
                slotDeliveryWorkerExecutor.execute(slotDeliveryWorker);
            }
        }
    }

    /**
     * Returns SlotDeliveryWorker mapped to a given queue
     *
     * @param queueName name of the queue
     * @return SlotDeliveryWorker instance
     */
    public SlotDeliveryWorker getSlotWorker(String queueName) {
        return slotDeliveryWorkerMap.get(getIdForSlotDeliveryWorker(queueName));
    }

    /**
     * Dump all message status of the slots owned by this slot delivery worker
     *
     * @param fileToWrite file to dump
     * @throws AndesException
     */
    public void dumpAllSlotInformationToFile(File fileToWrite) throws AndesException {
        for (SlotDeliveryWorker slotDeliveryWorker : slotDeliveryWorkerMap.values()) {
            slotDeliveryWorker.dumpAllSlotInformationToFile(fileToWrite);
        }
    }
}
