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

package org.wso2.carbon.andes.core.internal.inbound;

import com.google.common.util.concurrent.SettableFuture;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.andes.core.AndesChannel;
import org.wso2.carbon.andes.core.AndesException;
import org.wso2.carbon.andes.core.AndesMessage;
import org.wso2.carbon.andes.core.AndesMessageMetadata;
import org.wso2.carbon.andes.core.MessagingEngine;
import org.wso2.carbon.andes.core.internal.slot.SlotMessageCounter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * This is the Andes transaction event related class. This event object handles
 * the life cycle of a single transaction coming from the protocol level to Andes.
 */
public class InboundTransactionEvent implements AndesInboundStateEvent {

    private static Log log = LogFactory.getLog(InboundTransactionEvent.class);

    /**
     * Reference to Disruptor based event manager
     */
    private final InboundEventManager eventManager;

    /**
     * Internal event type of to denote the current state of the transaction
     */
    private EventType eventType;

    /**
     * This is used to make {@link #commit()} {@link #rollback()} and {@link #close()} methods
     * blocking calls
     */
    private SettableFuture<Boolean> taskCompleted;

    /**
     * Message list of the current transaction. This list doesn't have the duplicates that are
     * created for topics.
     */
    private ConcurrentLinkedQueue<AndesMessage> messageQueue;

    /**
     * Reference to {@link MessagingEngine} to do message storing operations
     */
    private final MessagingEngine messagingEngine;

    /**
     * Maximum batch size for a transaction. Limit is set for content size of the batch.
     * Exceeding this limit will lead to a failure in the subsequent commit request.
     */
    private final int maxBatchSize;

    /**
     * Content batch cached size at a given point in time.
     */
    private int currentBatchSize;

    /**
     * Reference to the channel of the publisher
     */
    private final AndesChannel channel;

    /**
     * maximum wait time for commit, rollback or close event to complete
     */
    private final long txWaitTimeout;

    /**
     * Check whether messages are stored to DB for the current transaction. If this is true that means
     * messages are stored in DB but {@link SlotMessageCounter} is not
     * updated completely.
     */
    private boolean messagesStoredNotCommitted;

    /**
     * messages list to be committed, enqueued messages
     */
    Queue<AndesMessage> getQueuedMessages() {
        return messageQueue;
    }

    /**
     * This message queue represent the message list of the current transaction. For topics this queue gets duplicates
     * after {@link MessagePreProcessor}
     *
     * @param queue message list of the current transaction
     */
    void setQueue(ConcurrentLinkedQueue<AndesMessage> queue) {
        this.messageQueue = queue;
    }

    void addMessages(Collection<AndesMessage> messages) {
        this.messageQueue.addAll(messages);
    }

    void clearMessages() {
        this.messageQueue.clear();
    }

    /**
     * Supported state events
     */
    private enum EventType {

        /**
         * Transaction commit related event type
         */
        TX_COMMIT_EVENT,

        /**
         * Transaction rollback related event type
         */
        TX_ROLLBACK_EVENT,

        /**
         * close the current transaction and release all resources
         */
        TX_CLOSE_EVENT
    }

    /**
     *
     * @param eventManager InboundEventManager
     */
    /**
     * Transaction object to do a transaction
     *
     * @param messagingEngine {@link MessagingEngine}
     * @param eventManager    InboundEventManager
     * @param maxBatchSize    maximum batch size for a commit
     * @param channel         AndesChannel
     * @param txWaitTimeout   maximum wait time for commit, rollback or close event to complete
     */
    public InboundTransactionEvent(MessagingEngine messagingEngine, InboundEventManager eventManager,
                                   int maxBatchSize, long txWaitTimeout, AndesChannel channel) {
        this.messagingEngine = messagingEngine;
        this.eventManager = eventManager;
        messageQueue = new ConcurrentLinkedQueue<>();
        taskCompleted = SettableFuture.create();
        this.maxBatchSize = maxBatchSize;
        this.channel = channel;
        this.txWaitTimeout = txWaitTimeout;
    }

    /**
     * This will commit the batched transacted message to the persistence storage using Andes
     * underlying event manager.
     * <p>
     * This is a blocking call
     *
     * @throws AndesException
     */
    public void commit() throws AndesException {

        if (currentBatchSize > maxBatchSize) {
            currentBatchSize = 0;
            messageQueue.clear();
            throw new AndesException("Current enqueued batch size exceeds maximum transactional batch size of " +
                                             maxBatchSize + " bytes.");
        }

        if (log.isDebugEnabled()) {
            log.debug("Prepare for commit. Channel id: " + channel.getId());
        }

        eventType = EventType.TX_COMMIT_EVENT;
        taskCompleted = SettableFuture.create();

        // Publish to event manager for processing
        eventManager.requestTransactionCommitEvent(this, channel);
        // Make the call blocking
        waitForCompletion();
    }

    /**
     * This will rollback the transaction. This is done using Andes underlying event manager
     * This is a blocking call.
     *
     * @throws AndesException
     */
    public void rollback() throws AndesException {
        if (log.isDebugEnabled()) {
            log.debug("Prepare for rollback. Channel: " + channel.getId());
        }

        eventType = EventType.TX_ROLLBACK_EVENT;
        taskCompleted = SettableFuture.create();

        // Publish to event manager for processing
        eventManager.requestTransactionRollbackEvent(this, channel);
        // Make the call blocking
        waitForCompletion();
    }

    /**
     * Add a message to a transaction. Added messages will be persisted in DB only when
     * commit is invoked. Underlying event manager will add the message to the the transaction
     * <p>
     * This is a asynchronous call
     *
     * @param message AndesMessage
     */
    public void enqueue(AndesMessage message) {
        currentBatchSize = currentBatchSize + message.getMetadata().getMessageContentLength();

        if (currentBatchSize > maxBatchSize) {
            messageQueue.clear(); // if max batch size exceeds invalidate commit.
        } else {

            // This will go through ContentChunkHandler and add the message to message list of the transaction
            eventManager.requestTransactionEnqueueEvent(message, this, channel);

            if (log.isDebugEnabled()) {
                log.debug("Enqueue message with message id " +
                                  message.getMetadata().getMessageID() + " for transaction ");
            }
        }
    }

    /**
     * Release all resources used by transaction object. This should be called when the transactional session is
     * closed. This is to prevent unwanted resource usage (DB connections etc) after closing
     * a transactional session.
     *
     * @throws AndesException
     */
    public void close() throws AndesException {
        eventType = EventType.TX_CLOSE_EVENT;
        taskCompleted = SettableFuture.create();
        eventManager.requestTransactionCloseEvent(this, channel);
        waitForCompletion();
    }

    /**
     * Update internal state of the transaction according to the prepared event of the transaction
     * This method is call by the state event handler.
     */
    @Override
    public void updateState() throws AndesException {
        switch (eventType) {
            case TX_COMMIT_EVENT:
                executeCommitEvent();
                break;
            case TX_ROLLBACK_EVENT:
                executeRollbackEvent();
                break;
            case TX_CLOSE_EVENT:
                executeCloseEvent();
                break;
            default:
                if (log.isDebugEnabled()) {
                    log.debug("Event " + eventType + " ignored.");
                }
                break;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String eventInfo() {
        return "Event type " + eventType;
    }

    /**
     * Close the current transaction
     */
    private void executeCloseEvent() throws AndesException {
        try {
            messageQueue.clear();
            currentBatchSize = 0;
            taskCompleted.set(true);
        } catch (Throwable t) {
            // Exception is passed to the the caller who is waiting on the future
            taskCompleted.setException(t);
            messageQueue.clear();
            throw new AndesException("Exception occurred while closing transactional channel " +
                                             channel.getId(), t);
        }
    }

    /**
     * Update the state of Andes core by informing the slot counter about written messages.
     * This is called by {@link StateEventHandler}
     * Messages are written to DB by {@link MessageWriter}
     */
    private void executeCommitEvent() throws AndesException {
        try {
            messagesStoredNotCommitted = true;
            // update slot information for transaction related messages
            SlotMessageCounter.getInstance().recordMetadataCountInSlot(getQueuedMessages());
            messageQueue.clear();
            messagesStoredNotCommitted = false; // Once slots are updated rolling back is irrelevant.
            currentBatchSize = 0;
            taskCompleted.set(true);
        } catch (Throwable t) {
            // Exception is passed to the the caller of get method of settable future
            taskCompleted.setException(t);
            throw new AndesException("Exception occurred while committing transaction. Channel id " +
                                             channel.getId(), t);
        }
    }

    /**
     * Undo changes done by the current transaction. This has DB interactions and Andes core state changes mixed
     * hence calling this from {@link StateEventHandler}.
     */
    private void executeRollbackEvent() throws AndesException {
        try {
            if (messagesStoredNotCommitted) {
                List<AndesMessageMetadata> messagesToRemove = new ArrayList<>();
                for (AndesMessage message : messageQueue) {
                    messagesToRemove.add(message.getMetadata());
                }
                messagingEngine.deleteMessages(messagesToRemove);
                messagesStoredNotCommitted = false;
            }

            messageQueue.clear();
            currentBatchSize = 0;
            taskCompleted.set(true);
        } catch (Throwable t) {
            taskCompleted.setException(t);
            throw new AndesException("Exception occurred while rolling back transaction. Channel id " +
                                             channel.getId(), t);
        }
    }

    /**
     * Wait until the respective task set the value of the future once the task is completed
     *
     * @return True if task is successful and false otherwise
     * @throws AndesException
     */
    private Boolean waitForCompletion() throws AndesException {
        try {
            return taskCompleted.get(txWaitTimeout, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException e) {
            String errMsg = "Error occurred while processing transaction event " + eventType;
            log.error(errMsg, e);
            throw new AndesException(errMsg, e);
        } catch (TimeoutException e) {
            String errMsg = eventType + " Timeout. Didn't complete within " + txWaitTimeout + " seconds.";
            log.error(errMsg, e);
            throw new AndesException(errMsg, e);
        }
        return false;
    }
}
