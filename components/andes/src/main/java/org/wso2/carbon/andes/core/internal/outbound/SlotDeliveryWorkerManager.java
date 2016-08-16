/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.andes.core.AndesException;
import org.wso2.carbon.andes.core.DeliverableAndesMetadata;
import org.wso2.carbon.andes.core.DestinationType;
import org.wso2.carbon.andes.core.MessageFlusher;
import org.wso2.carbon.andes.core.MessagingEngine;
import org.wso2.carbon.andes.core.ProtocolType;
import org.wso2.carbon.andes.core.internal.AndesContext;
import org.wso2.carbon.andes.core.internal.cluster.error.detection.NetworkPartitionListener;
import org.wso2.carbon.andes.core.internal.configuration.AndesConfigurationManager;
import org.wso2.carbon.andes.core.internal.configuration.enums.AndesConfiguration;
import org.wso2.carbon.andes.core.internal.slot.Slot;
import org.wso2.carbon.andes.core.internal.thrift.MBThriftClient;
import org.wso2.carbon.andes.core.internal.thrift.ThriftConnectionListener;
import org.wso2.carbon.andes.core.store.FailureObservingStoreManager;
import org.wso2.carbon.andes.core.store.HealthAwareStore;
import org.wso2.carbon.andes.core.store.StoreHealthListener;
import org.wso2.carbon.andes.core.task.TaskExecutorService;

import java.io.File;
import java.util.List;
import java.util.concurrent.ThreadFactory;

/**
 * This class is responsible of allocating SloDeliveryWorker threads to each queue
 */
public final class SlotDeliveryWorkerManager implements StoreHealthListener, NetworkPartitionListener,
                                                        ThriftConnectionListener {

    private static Log log = LogFactory.getLog(SlotDeliveryWorkerManager.class);

    /**
     * Delay for waiting for an idle task
     */
    private static final long IDLE_TASK_DELAY_MILLIS = 100;

    /**
     * Slot Delivery Worker Manager instance
     */
    private static SlotDeliveryWorkerManager slotDeliveryWorkerManager = new SlotDeliveryWorkerManager();

    private final TaskExecutorService<MessageDeliveryTask> taskManager;

    private SlotDeliveryWorkerManager() {
        int numberOfThreads = AndesConfigurationManager
                .readValue(AndesConfiguration.PERFORMANCE_TUNING_SLOTS_WORKER_THREAD_COUNT);
        ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat("MessageDeliveryTaskThreadPool-%d").build();
        taskManager = new TaskExecutorService<>(numberOfThreads, IDLE_TASK_DELAY_MILLIS, threadFactory);
        taskManager.setExceptionHandler(new DeliveryTaskExceptionHandler());
        AndesContext andesContext = AndesContext.getInstance();

        if (andesContext.isClusteringEnabled()) {
            // network partition detection and thrift client works only when clustered.
            andesContext.getClusterAgent().addNetworkPartitionListener(this);
            MBThriftClient.addConnectionListener(this);
        }

        FailureObservingStoreManager.registerStoreHealthListener(this);
    }

    /**
     * @return SlotDeliveryWorkerManager instance
     */
    public static SlotDeliveryWorkerManager getInstance() {
        return slotDeliveryWorkerManager;
    }

    /**
     * Rescheduled messages for re delivery
     *
     * @param storageQueueName storage queue name
     * @param messages message list
     */
    public void rescheduleMessagesForDelivery(String storageQueueName, List<DeliverableAndesMetadata> messages) {
        MessageDeliveryTask messageDeliveryTask = taskManager.getTask(storageQueueName);

        if (null != messageDeliveryTask) {
            messageDeliveryTask.rescheduleMessagesForDelivery(messages);
        }
    }

    /**
     * When a subscription is added this method will be called. if this is the first subscriber for the destination
     * a {@link MessageDeliveryTask} will be added to the {@link TaskExecutorService}
     *
     * @param storageQueueName name of the queue to start slot delivery worker for
     * @param destination      The destination name
     * @param protocolType     The protocol which the messages in this storage queue belongs to
     * @param destinationType  The destination type which the messages in this storage queue belongs to
     */
    public void onSubscriptionAdded(String storageQueueName, String destination,
                                    ProtocolType protocolType, DestinationType destinationType) throws AndesException {

        MessageDeliveryTask messageDeliveryTask =
                new MessageDeliveryTask(destination, protocolType, storageQueueName,
                                        destinationType, MessagingEngine.getInstance().getSlotCoordinator(),
                                        MessageFlusher.getInstance());
        taskManager.add(messageDeliveryTask);
    }

    /**
     * Stop delivery task for the given storage queue locally. This is normally called when all the subscribers for a
     * destination leave the local node.
     *
     * @param storageQueueName Name of the Storage queue
     */
    public void stopDeliveryForDestination(String storageQueueName) {
        if (log.isDebugEnabled()) {
            log.debug("Stopping delivery for storage queue " + storageQueueName + " with MessageDeliveryTask : "
                              + storageQueueName);
        }
        taskManager.remove(storageQueueName);
    }

    /**
     * Stop all stop delivery workers in the thread pool
     */
    public void stopMessageDelivery() {
        taskManager.stop();
    }

    /**
     * Start all the SlotDeliveryWorkers if not already in running state.
     */
    public void startMessageDelivery() {
        taskManager.start();
    }

    /**
     * Delete the relevant slot from {@link MessageDeliveryTask}
     *
     * @param slot Slot to be deleted
     */
    public void deleteSlot( Slot slot) {
        MessageDeliveryTask messageDeliveryTask = taskManager.getTask(slot.getStorageQueueName());
        if (null != messageDeliveryTask) {
            messageDeliveryTask.deleteSlot(slot);
        }
    }

    /**
     * Dump all message status of the slots owned by this slot delivery worker
     *
     * @param fileToWrite file to dump
     * @throws AndesException
     */
    public void dumpAllSlotInformationToFile(File fileToWrite) throws AndesException {
        // NOTE: Will be replaced with subscription store update PR
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void minimumNodeCountNotFulfilled(int currentNodeCount) {
        log.warn("Network outage detected therefore stopping message delivery. Current cluster size "
                         + currentNodeCount);
        taskManager.stop();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void minimumNodeCountFulfilled(int currentNodeCount) {
        log.info("Network outage resolved therefore resuming message delivery. Current cluster size "
                         + currentNodeCount);
        taskManager.start();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void storeNonOperational(HealthAwareStore store, Exception ex) {
        log.warn("Message stores became not operational therefore waiting");
        taskManager.stop();

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void storeOperational(HealthAwareStore store) {
        log.info("Message stores became operational therefore resuming work");
        taskManager.start();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onThriftClientDisconnect() {
        log.warn("Thrift client disconnected. Waiting till reconnect");
        taskManager.stop();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onThriftClientConnect() {
        log.info("Thrift client connection established");
        taskManager.start();
    }
}
