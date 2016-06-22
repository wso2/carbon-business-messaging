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

import com.google.common.util.concurrent.SettableFuture;
import com.gs.collections.impl.map.mutable.ConcurrentHashMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.andes.core.AndesException;
import org.wso2.carbon.andes.core.DeliverableAndesMetadata;
import org.wso2.carbon.andes.core.DestinationType;
import org.wso2.carbon.andes.core.MessageDeliveryInfo;
import org.wso2.carbon.andes.core.MessageFlusher;
import org.wso2.carbon.andes.core.MessagingEngine;
import org.wso2.carbon.andes.core.ProtocolType;
import org.wso2.carbon.andes.core.internal.AndesContext;
import org.wso2.carbon.andes.core.internal.cluster.error.detection.NetworkPartitionListener;
import org.wso2.carbon.andes.core.store.FailureObservingStoreManager;
import org.wso2.carbon.andes.core.store.HealthAwareStore;
import org.wso2.carbon.andes.core.store.StoreHealthListener;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

/**
 * SlotDelivery worker is responsible of distributing messages to subscribers. Messages will be
 * taken from a slot.
 */
public class SlotDeliveryWorker extends Thread implements StoreHealthListener, NetworkPartitionListener {


    /**
     * Keeps data related to storage queues for reference
     */
    private ConcurrentHashMap<String, StorageQueueData> storageQueueDataMap;

    /**
     * Map to keep track of subscription to slots map.
     * There was no provision to remove messageBufferingTracker when last subscriber close before receive all
     * messages in slot.
     * We use this map to delete remaining tracking when last subscriber close in particular destination.
     */
    private final ConcurrentMap<String, Map<String, Slot>> storageQueueToSlotTracker = new ConcurrentHashMap<>();

    private static Log log = LogFactory.getLog(SlotDeliveryWorker.class);

    /**
     * This map contains slotId to slot hash map against queue name
     */
    private volatile boolean running;
    private volatile boolean threadCompleted;
    private MessageFlusher messageFlusher;
    private SlotCoordinator slotCoordinator;

    /**
     * Indicates and provides a barrier if messages stores become offline.
     * marked as volatile since this value could be set from a different thread
     * (other than those of Disruptor)
     */
    private volatile SettableFuture<Boolean> messageStoresUnavailable;

    /**
     * Indicates and provides a barrier if network is partitions become offline.
     * marked as volatile since this value could be set from a different thread
     * (from hazelcast related threads.)
     */
    private volatile SettableFuture<Boolean> networkOutageDetected;


    /**
     * Maximum number to retries retrieve metadata list for a given storage
     * queue ( in the errors occur in message stores)
     */
    private static final int MAX_META_DATA_RETRIEVAL_COUNT = 5;

    public SlotDeliveryWorker() {

        messageFlusher = MessageFlusher.getInstance();
        this.storageQueueDataMap = new ConcurrentHashMap<>();
        slotCoordinator = MessagingEngine.getInstance().getSlotCoordinator();
        messageStoresUnavailable = null;
        FailureObservingStoreManager.registerStoreHealthListener(this);

        if (AndesContext.getInstance().isClusteringEnabled()) {
            // network partition detection works only when clustered.
            AndesContext.getInstance().getClusterAgent().addNetworkPartitionListener(this);
        }
    }

    public void rescheduleMessagesForDelivery(String storageQueueName, List<DeliverableAndesMetadata> messages) {
        StorageQueueData storageQueueData = storageQueueDataMap.get(storageQueueName);

        if (null != storageQueueData) { // This storage queue has been removed and hence does not need to reschedule
            String destination = storageQueueDataMap.get(storageQueueName).getDestinationName();
            MessageFlusher.getInstance()
                    .addAlreadyTrackedMessagesToBuffer(destination, storageQueueData.getProtocolType(),
                                                       storageQueueData.getDestinationType(), messages);

        }
    }

    @Override
    public void run() {
        /**
         * This while loop is necessary since whenever there are messages this thread should
         * deliver them
         */
        running = true;
        threadCompleted = false;
        while (running) {

            //Iterate through all the queues registered in this thread
            int idleQueueCounter = 0;

            for (Map.Entry<String, StorageQueueData> storageQueueDataEntry : storageQueueDataMap.entrySet()) {
                String storageQueueName = storageQueueDataEntry.getKey();
                StorageQueueData storageQueueData = storageQueueDataEntry.getValue();
                String destinationOfMessagesInQueue = storageQueueData.getDestinationName();
                DestinationType destinationType = storageQueueData.getDestinationType();
                try {
                    MessageDeliveryInfo messageDeliveryInfo = messageFlusher
                            .getMessageDeliveryInfo(destinationOfMessagesInQueue, storageQueueData.getProtocolType(),
                                                    destinationType);

                    //Check in memory buffer in MessageFlusher has room
                    if (messageDeliveryInfo.messageBufferHasRoom()) {

                        //get a slot from coordinator.
                        Slot currentSlot = requestSlot(storageQueueName);
                        currentSlot.setDestinationOfMessagesInSlot(destinationOfMessagesInQueue);

                        /**
                         * If the slot is empty
                         */
                        if (0 == currentSlot.getEndMessageId()) {

                                    /*
                                    If the message buffer in MessageFlusher is not empty
                                     send those messages
                                     */
                            if (log.isDebugEnabled()) {
                                log.debug("Received an empty slot from slot manager");
                            }
                            boolean sentFromMessageBuffer = messageFlusher.sendMessagesInBuffer(messageDeliveryInfo,
                                                                                                storageQueueName);
                            if (!sentFromMessageBuffer) {
                                //No available free slots
                                idleQueueCounter++;
                                if (idleQueueCounter == storageQueueDataMap.size()) {
                                    try {
                                        if (log.isDebugEnabled()) {
                                            log.debug("Sleeping Slot Delivery Worker");
                                        }
                                        Thread.sleep(100);
                                    } catch (InterruptedException ignored) {
                                        //Silently ignore
                                    }
                                }
                            }
                        } else {
                            if (log.isDebugEnabled()) {
                                log.debug("Received slot for storage queue " + storageQueueName + " " +
                                                  "is: " + currentSlot.getStartMessageId() +
                                                  " - " + currentSlot.getEndMessageId() +
                                                  "Thread Id:" + Thread.currentThread().getId());
                            }
                            List<DeliverableAndesMetadata> messagesRead = getMetaDataListBySlot(storageQueueName,
                                                                                                currentSlot);

                            if (messagesRead != null && !messagesRead.isEmpty()) {
                                if (log.isDebugEnabled()) {
                                    log.debug("Number of messages read from slot " + currentSlot.getStartMessageId()
                                                      + " - " + currentSlot.getEndMessageId() + " is " + messagesRead
                                            .size()
                                                      + " storage queue= " + storageQueueName);
                                }

                                storageQueueToSlotTracker.putIfAbsent(storageQueueName, new HashMap<String, Slot>());

                                Map<String, Slot> subscriptionSlots = storageQueueToSlotTracker.get(storageQueueName);

                                Slot trackedSlot = subscriptionSlots.get(currentSlot.getId());
                                if (trackedSlot == null) {
                                    subscriptionSlots.put(currentSlot.getId(), currentSlot);
                                    trackedSlot = currentSlot;
                                } else {
                                    if (log.isDebugEnabled()) {
                                        log.debug("Overlapped slot received. Slot ID " + trackedSlot.getId());
                                    }
                                }

                                filterOverlappedMessages(trackedSlot, messagesRead);
                                MessageFlusher.getInstance()
                                        .sendMessageToBuffer(messagesRead, trackedSlot, messageDeliveryInfo);
                                MessageFlusher.getInstance()
                                        .sendMessagesInBuffer(messageDeliveryInfo, storageQueueName);
                            } else {
                                currentSlot.setSlotInActive();
                                SlotDeletionExecutor.getInstance().executeSlotDeletion(currentSlot);
                            }
                        }

                    } else {
                                /*If there are messages to be sent in the message
                                            buffer in MessageFlusher send them */
                        if (log.isDebugEnabled()) {
                            log.debug("The queue " + storageQueueName + " has no room. Thus sending " +
                                              "from buffer.");
                        }
                        messageFlusher.sendMessagesInBuffer(messageDeliveryInfo, storageQueueName);
                    }
                } catch (ConnectionException e) {
                    log.error("Error occurred while connecting to the thrift coordinator " + e.getMessage(), e);
                    setRunning(false);
                    //Any exception should be caught here. Otherwise SDW thread will stop
                    //and MB node will become useless
                } catch (AndesException e) {
                    log.error("Error running Message Store Reader " + e.getMessage(), e);
                } catch (Throwable e) {
                    log.error("Error while running Slot Delivery Worker. ", e);
                }
            }
        }

        log.info(
                "SlotDeliveryWorker stopped. Thread name " + Thread.currentThread().getName() + " with Thread Id : "
                        + this.getId());

        threadCompleted = true;
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

    public void stopDeliveryForQueue(String storageQueue) {
        StorageQueueData storageQueueData = storageQueueDataMap.remove(storageQueue);

        if (null != storageQueueData) { // If null, this has already been removed when subscription disconnecting
            MessageFlusher.getInstance().clearUpAllBufferedMessagesForDelivery(storageQueueData.getDestinationName(),
                                                                               storageQueueData.getDestinationType());

            Map<String, Slot> orphanedSlots = storageQueueToSlotTracker.remove(storageQueue);

            // Check if there are any orphaned slots
            if (null != orphanedSlots) {
                for (Slot slot : orphanedSlots.values()) {
                    if (log.isDebugEnabled()) {
                        log.debug("Orphan slot situation and clear tracking of messages for slot = " + slot);
                    }
                    slot.markMessagesOfSlotAsReturned();
                }
            }
        }

        if (0 == storageQueueDataMap.size()) {
            if (log.isDebugEnabled()) {
                log.debug("Stopping SlotDeliveryWorker : " + this.getId() + " due to 0 subscribers on its queues. ");
            }
            this.setRunning(false);
        }

        log.info("SlotDeliveryWorker stopped. Thread name " + Thread.currentThread().getName());
    }

    /**
     * Returns a list of {@link DeliverableAndesMetadata} in specified slot
     *
     * @param storageQueueName name of the storage queue which this slot belongs to
     * @param slot             the slot which messages are retrieved.
     * @return a list of {@link DeliverableAndesMetadata}
     * @throws AndesException an exception if there are errors at message store level.
     */
    private List<DeliverableAndesMetadata> getMetaDataListBySlot(String storageQueueName, Slot slot)
            throws AndesException {
        return getMetadataListBySlot(storageQueueName, slot, 0);
    }

    /**
     * Returns a list of {@link DeliverableAndesMetadata} in specified slot. This method is recursive.
     *
     * @param storageQueueName      storage queue of the slot
     * @param slot                  Retrieve metadata relevant to the given {@link Slot}
     * @param numberOfRetriesBefore retry count for the query
     * @return return a list of {@link DeliverableAndesMetadata}
     * @throws AndesException
     */
    private List<DeliverableAndesMetadata> getMetadataListBySlot(String storageQueueName, Slot slot,
                                                                 int numberOfRetriesBefore) throws AndesException {

        List<DeliverableAndesMetadata> messagesRead;

        if (messageStoresUnavailable != null) {
            try {

                log.info("Message store has become unavailable therefore " +
                                 "waiting until store becomes available. thread id: " + this.getId());
                messageStoresUnavailable.get();
                messageStoresUnavailable = null; // we are passing the blockade (therefore clear it).
                log.info("Message store became available. resuming work. thread id: " + this.getId());

            } catch (InterruptedException e) {
                throw new AndesException("Thread interrupted while waiting for message stores to come online", e);
            } catch (ExecutionException e) {
                throw new AndesException("Error occurred while waiting for message stores to come online", e);
            }
        }

        try {

            long firstMsgId = slot.getStartMessageId();
            long lastMsgId = slot.getEndMessageId();
            //Read messages in the slot
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
                                slot.toString(),
                                numberOfRetriesBefore);
                throw new AndesException(errorMsg, aex);
            }

        }

        return messagesRead;

    }

    /**
     * Get a slot from the Slot to deliver ( from the coordinator if the MB is clustered)
     *
     * @param storageQueueName the storage queue name for from which a slot should be returned.
     * @return a {@link Slot}
     * @throws ConnectionException if connectivity to coordinator is lost.
     */
    private Slot requestSlot(String storageQueueName) throws ConnectionException {

        if (networkOutageDetected != null) {
            try {

                log.warn("Network outage detected therefore " +
                                 "waiting until network restores. thread id: " + this.getId());
                networkOutageDetected.get();
                networkOutageDetected = null; // we are passing the blockade (therefore clear it).
                log.info("Network outage resolved. resuming work. thread id: " + this.getId());

            } catch (InterruptedException e) {
                throw new ConnectionException("Thread interrupted while waiting for message stores to come online", e);
            } catch (ExecutionException e) {
                throw new ConnectionException("Error occurred while waiting for message stores to come online", e);
            }
        }

        long startTime = System.currentTimeMillis();
        Slot currentSlot = slotCoordinator.getSlot(storageQueueName);
        long endTime = System.currentTimeMillis();

        if (log.isDebugEnabled()) {
            log.debug((endTime - startTime) + " milliSec took to get a slot" +
                              " from slot manager");
        }
        return currentSlot;
    }

    /**
     * Add a queue to queue list of this SlotDeliveryWorkerThread
     *
     * @param storageQueueName queue name of the newly added queue
     * @param protocolType     The protocol which the storage queue holds messages of
     * @param destinationType  The destination type of the messages which this storage queue holds
     */
    public void startDeliveryForQueue(String storageQueueName, String destination, ProtocolType protocolType,
                                      DestinationType destinationType) throws AndesException {
        StorageQueueData storageQueueData = new StorageQueueData(storageQueueName, destination, protocolType,
                                                                 destinationType);
        storageQueueDataMap.put(storageQueueName, storageQueueData);
    }

    /**
     * @return Whether the worker thread is in running state or not
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Set state of the worker thread
     *
     * @param running new state of the worker
     */
    public void setRunning(boolean running) {
        this.running = running;

        /* If running is false, we have to wait until thread finishes

          Below function works similar to thread.join() but due to following reason we cannot use thread.join() here.

          1. Since this thread is invoked from a thread pool executor, this.join() will not invoke the real running
              threads' join() method. For this we have to get Thread.currentThread() from inside run method and
              keep a reference to allow access from outside.
          2. Although we can get the thread reference as above, threadReference.join() will wait indefinitely since
             thread pool executor does not terminate the threads when the processing is finished, it keeps the threads
             in WAITING state to be executed again if asked to.
         */
        if (!running) {
            while (!threadCompleted) {
                try {
                    Thread.sleep(100L);
                } catch (InterruptedException e) {
                    log.warn("Error waiting for SlotDeliveryWorker to complete", e);
                }
            }
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

        Map<String, Slot> slotsOfQueue = storageQueueToSlotTracker.get(slot.getStorageQueueName());
        if (null != slotsOfQueue) {
            slotsOfQueue.remove(slot.getId());
        } else {
            log.debug("Slot has been deleted by the SlotDeliveryWorker before the SlotDeletionExecutor can get to it." +
                              " Slot : " + slot);
        }

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
            for (Map.Entry<String, Map<String, Slot>> storageQueueToSlotEntry : storageQueueToSlotTracker.entrySet()) {
                String storageQueue = storageQueueToSlotEntry.getKey();
                Map<String, Slot> slotIdToSlotMap = storageQueueToSlotEntry.getValue();
                for (Map.Entry<String, Slot> slotEntry : slotIdToSlotMap.entrySet()) {
                    String slotID = slotEntry.getKey();
                    List<DeliverableAndesMetadata> messagesOfSlot = slotEntry.getValue().getAllMessagesOfSlot();
                    if (!messagesOfSlot.isEmpty()) {

                        int writerFlushCounter = 0;
                        for (DeliverableAndesMetadata message : messagesOfSlot) {
                            information.append(storageQueue).append(",").append(slotID).append(",")
                                    .append(message.dumpMessageStatus()).append("\n");
                            writerFlushCounter = writerFlushCounter + 1;
                            if (writerFlushCounter % 10 == 0) {
                                information.flush();
                            }
                        }

                        information.flush();
                    }
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
     * {@inheritDoc}
     * <p> Creates a {@link SettableFuture} indicating message store became offline.
     */
    @Override
    public void storeNonOperational(HealthAwareStore store, Exception ex) {
        log.warn(this.getId() + " Message stores became not operational therefore waiting");
        messageStoresUnavailable = SettableFuture.create();

    }

    /**
     * {@inheritDoc}
     * <p> Sets a value for {@link SettableFuture} indicating message store became online.
     */
    @Override
    public void storeOperational(HealthAwareStore store) {
        log.info(this.getId() + "Message stores became operational therefore resuming work");
        messageStoresUnavailable.set(false);
    }

    /**
     * {@inheritDoc}
     * When the minimum node count is not met slot delivery worker thread should
     * stop working. therefore a barrier (/ settable future is created)
     */
    @Override
    public void minimumNodeCountNotFulfilled(int currentNodeCount) {
        log.info(this.getId() +
                         " network outage detected therefore stopping work, current cluster size: " +
                         currentNodeCount);

        if (networkOutageDetected != null) {
            networkOutageDetected.setException(
                    new AndesException(
                            "possible race condition detected. duplicate network-outage notification received"));
        }

        networkOutageDetected = SettableFuture.create();

    }


    /**
     * network partition is healed. therefore removing the barrier (- allows the this thread to work)
     */
    @Override
    public void minimumNodeCountFulfilled(int currentNodeCount) {
        log.info(
                this.getId() + " network outage resolved therefore resuming work, current cluster size: " +
                        currentNodeCount);
        networkOutageDetected.set(false);

    }

    /**
     * Check if the given storage queue is already added in the current worker.
     *
     * @param storageQueueName The storage queue name to check for
     * @return True if storage queue is already added to this worker
     */
    public boolean isStorageQueueAdded(String storageQueueName) {
        return storageQueueDataMap.containsKey(storageQueueName);
    }
}

