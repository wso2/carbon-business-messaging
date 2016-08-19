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

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.andes.core.AndesAckData;
import org.wso2.carbon.andes.core.AndesChannel;
import org.wso2.carbon.andes.core.AndesMessage;
import org.wso2.carbon.andes.core.DisablePubAckImpl;
import org.wso2.carbon.andes.core.MessagingEngine;
import org.wso2.carbon.andes.core.internal.AndesContext;
import org.wso2.carbon.andes.core.internal.configuration.AndesConfigurationManager;
import org.wso2.carbon.andes.core.internal.configuration.enums.AndesConfiguration;
import org.wso2.carbon.andes.core.internal.disruptor.ConcurrentBatchEventHandler;
import org.wso2.carbon.andes.core.internal.disruptor.LogExceptionHandler;
import org.wso2.carbon.andes.core.internal.metrics.MetricsConstants;
import org.wso2.carbon.andes.core.subscription.SubscriptionEngine;
import org.wso2.carbon.andes.core.util.MessageTracer;
import org.wso2.carbon.metrics.core.Gauge;
import org.wso2.carbon.metrics.core.Level;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Disruptor based inbound event handling class.
 * Inbound events are represent within the buffer as InboundEventContainer objects. Four types of event processors
 * goes through
 * the ring buffer processing events.
 */
public class InboundEventManager {

    private static Log log = LogFactory.getLog(InboundEventManager.class);
    private final RingBuffer<InboundEventContainer> ringBuffer;
    private AtomicInteger ackedMessageCount = new AtomicInteger();
    private Disruptor<InboundEventContainer> disruptor;
    private final DisablePubAckImpl disablePubAck;

    public InboundEventManager(SubscriptionEngine subscriptionEngine,
                               MessagingEngine messagingEngine) {

        Integer bufferSize = AndesConfigurationManager.readValue(
                AndesConfiguration.PERFORMANCE_TUNING_PUBLISHING_BUFFER_SIZE);
        Integer writeHandlerCount = AndesConfigurationManager.readValue(
                AndesConfiguration.PERFORMANCE_TUNING_PARALLEL_MESSAGE_WRITERS);
        Integer ackHandlerCount = AndesConfigurationManager.readValue(
                AndesConfiguration.PERFORMANCE_TUNING_ACK_HANDLER_COUNT);
        Integer writerBatchSize = AndesConfigurationManager.readValue(
                AndesConfiguration.PERFORMANCE_TUNING_MESSAGE_WRITER_BATCH_SIZE);
        Integer ackHandlerBatchSize = AndesConfigurationManager.readValue(
                AndesConfiguration.PERFORMANCE_TUNING_ACKNOWLEDGEMENT_HANDLER_BATCH_SIZE);
        Integer transactionHandlerCount = AndesConfigurationManager.readValue(
                AndesConfiguration.PERFORMANCE_TUNING_PARALLEL_TRANSACTION_MESSAGE_WRITERS);
        Integer transactionBatchSize = AndesConfigurationManager.readValue(
                AndesConfiguration.MAX_TRANSACTION_BATCH_SIZE);

        disablePubAck = new DisablePubAckImpl();
        int maxContentChunkSize = AndesConfigurationManager.readValue(
                AndesConfiguration.PERFORMANCE_TUNING_MAX_CONTENT_CHUNK_SIZE);
        int contentChunkHandlerCount = AndesConfigurationManager.readValue(
                AndesConfiguration.PERFORMANCE_TUNING_CONTENT_CHUNK_HANDLER_COUNT);

        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder()
                .setNameFormat("DisruptorInboundEventThread-%d").build();
        ExecutorService executorPool = Executors.newCachedThreadPool(namedThreadFactory);


        disruptor = new Disruptor<>(InboundEventContainer.getFactory(),
                                    bufferSize,
                                    executorPool,
                                    ProducerType.MULTI,
                                    new BlockingWaitStrategy());

        disruptor.handleExceptionsWith(new LogExceptionHandler());

        ConcurrentBatchEventHandler[] concurrentBatchEventHandlers =
                new ConcurrentBatchEventHandler[writeHandlerCount + ackHandlerCount + transactionHandlerCount];

        ContentChunkHandler[] chunkHandlers = new ContentChunkHandler[contentChunkHandlerCount];
        for (int i = 0; i < contentChunkHandlerCount; i++) {
            chunkHandlers[i] = new ContentChunkHandler(maxContentChunkSize);
        }

        for (int turn = 0; turn < writeHandlerCount; turn++) {
            concurrentBatchEventHandlers[turn] = new ConcurrentBatchEventHandler(turn, writeHandlerCount,
                                                                                 writerBatchSize,
                                                                                 InboundEventContainer.Type
                                                                                         .MESSAGE_EVENT,
                                                                                 new MessageWriter(messagingEngine,
                                                                                                   writerBatchSize));
        }

        for (int turn = 0; turn < transactionHandlerCount; turn++) {
            concurrentBatchEventHandlers[writeHandlerCount + turn] =
                    new ConcurrentBatchEventHandler(turn, transactionHandlerCount,
                                                    transactionBatchSize,
                                                    InboundEventContainer.Type.TRANSACTION_COMMIT_EVENT,
                                                    new MessageWriter(messagingEngine, transactionBatchSize));
        }

        for (int turn = 0; turn < ackHandlerCount; turn++) {
            concurrentBatchEventHandlers[writeHandlerCount + transactionHandlerCount + turn] =
                    new ConcurrentBatchEventHandler(turn, ackHandlerCount,
                                                    ackHandlerBatchSize,
                                                    InboundEventContainer.Type.ACKNOWLEDGEMENT_EVENT,
                                                    new AckHandler(messagingEngine));
        }

        MessagePreProcessor preProcessor = new MessagePreProcessor(subscriptionEngine);

        // Order in which handlers run in Disruptor
        // - ContentChunkHandlers
        // - MessagePreProcessor
        // - MessageWriters and AckHandlers
        // - StateEventHandler
        disruptor.handleEventsWith(chunkHandlers).then(preProcessor);
        disruptor.after(preProcessor).handleEventsWith(concurrentBatchEventHandlers);

        // State event handler should run at last.
        // State event handler update the state of Andes after other handlers work is done.
        disruptor.after(concurrentBatchEventHandlers).handleEventsWith(new StateEventHandler());
        ringBuffer = disruptor.start();

        //Will add the gauge to metrics manager
        AndesContext.getInstance().getMetricService().gauge(MetricsConstants.DISRUPTOR_INBOUND_RING, Level.INFO,
                                                            new InBoundRingGauge());
        AndesContext.getInstance().getMetricService().gauge(MetricsConstants.DISRUPTOR_MESSAGE_ACK, Level.INFO,
                                                            new AckedMessageCountGauge());
    }

    /**
     * When a message is received from a transport it is handed over to MessagingEngine through the implementation of
     * inbound event manager. (e.g: through a disruptor ring buffer) Eventually the message will be stored
     *
     * @param message       AndesMessage
     * @param andesChannel  AndesChannel
     * @param pubAckHandler PubAckHandler
     */
    public void messageReceived(AndesMessage message, AndesChannel andesChannel, PubAckHandler pubAckHandler) {
        message.getMetadata().setArrivalTime(System.currentTimeMillis());
        // Publishers claim events in sequence
        long sequence = ringBuffer.next();
        InboundEventContainer event = ringBuffer.get(sequence);

        event.setEventType(InboundEventContainer.Type.MESSAGE_EVENT);
        event.getMessageList().add(message);
        event.pubAckHandler = pubAckHandler;
        event.setChannel(andesChannel);
        // make the event available to EventProcessors
        ringBuffer.publish(sequence);

        //Tracing message activity
        MessageTracer.trace(message, MessageTracer.PUBLISHED_TO_INBOUND_DISRUPTOR);

        if (log.isDebugEnabled()) {
            log.debug("[ sequence: " + sequence + " ] Message published to disruptor.");
        }
    }

    /**
     * Acknowledgement received from clients for sent messages will be handled through this method
     *
     * @param ackData AndesAckData
     */
    public void ackReceived(AndesAckData ackData) {
        //For metrics
        ackedMessageCount.getAndIncrement();

        // Publishers claim events in sequence
        long sequence = ringBuffer.next();
        InboundEventContainer event = ringBuffer.get(sequence);

        event.setEventType(InboundEventContainer.Type.ACKNOWLEDGEMENT_EVENT);
        event.ackData = ackData;
        // make the event available to EventProcessors
        ringBuffer.publish(sequence);

        //Tracing message
        if (MessageTracer.isEnabled()) {
            MessageTracer.trace(ackData.getAcknowledgedMessage().getMessageId(), ackData.getAcknowledgedMessage()
                    .getDestination(), MessageTracer.ACK_PUBLISHED_TO_DISRUPTOR);
        }

        if (log.isDebugEnabled()) {
            log.debug("[ sequence: " + sequence + " ] Message acknowledgement published to disruptor. Message id " +
                              ackData.getAcknowledgedMessage().getMessageId());
        }
    }

    /**
     * Publish state change event to event Manager
     *
     * @param stateEvent AndesInboundStateEvent
     */
    public void publishStateEvent(AndesInboundStateEvent stateEvent) {

        // Publishers claim events in sequence
        long sequence = ringBuffer.next();
        InboundEventContainer event = ringBuffer.get(sequence);
        try {
            event.setEventType(InboundEventContainer.Type.STATE_CHANGE_EVENT);
            event.setStateEvent(stateEvent);
        } finally {
            // make the event available to EventProcessors
            ringBuffer.publish(sequence);
            if (log.isDebugEnabled()) {
                log.debug("[ Sequence: " + sequence + " ] State change event '" + stateEvent.eventInfo() +
                                  "' published to Disruptor");
            }
        }

    }

    /**
     * Publish an event to update safe zone message ID
     * as per this node (this is used when deleting slots)
     */
    public void updateSlotDeletionSafeZone() {
        // Publishers claim events in sequence
        long sequence = ringBuffer.next();
        InboundEventContainer event = ringBuffer.get(sequence);
        try {
            event.setEventType(InboundEventContainer.Type.SAFE_ZONE_DECLARE_EVENT);
        } finally {
            // make the event available to EventProcessors
            ringBuffer.publish(sequence);
            if (log.isDebugEnabled()) {
                log.debug("[ Sequence: " + sequence + " ] " + event.getEventType() + "' published to Disruptor");
            }
        }
    }

    /**
     * Publish an event to recover from a member left without a submit slot event for messages written to DB
     */
    public void publishRecoveryEvent() {
        // Publishers claim events in sequence
        long sequence = ringBuffer.next();
        InboundEventContainer event = ringBuffer.get(sequence);
        try {
            event.setEventType(InboundEventContainer.Type.PUBLISHER_RECOVERY_EVENT);
        } finally {
            // make the event available to EventProcessors
            ringBuffer.publish(sequence);
            if (log.isDebugEnabled()) {
                log.debug("[ Sequence: " + sequence + " ] " + event.getEventType() + "' published to Disruptor");
            }
        }
    }

    /**
     * Publish transaction enqueue event to Disruptor.
     * This will go through the {@link ContentChunkHandler} and
     * add the re-sized message to the {@link InboundTransactionEvent}
     *
     * @param message          enqueued {@link AndesMessage}
     * @param transactionEvent {@link InboundTransactionEvent}
     * @param channel          {@link AndesChannel} of the publisher
     */
    public void requestTransactionEnqueueEvent(AndesMessage message,
                                               InboundTransactionEvent transactionEvent, AndesChannel channel) {
        long sequence = ringBuffer.next();
        InboundEventContainer eventContainer = ringBuffer.get(sequence);

        try {
            eventContainer.setEventType(InboundEventContainer.Type.TRANSACTION_ENQUEUE_EVENT);
            eventContainer.setTransactionEvent(transactionEvent);
            eventContainer.addMessage(message);
            eventContainer.pubAckHandler = disablePubAck;
            eventContainer.setChannel(channel);
        } finally {
            ringBuffer.publish(sequence);
            if (log.isDebugEnabled()) {
                log.debug("[ Sequence: " + sequence + " ] " + eventContainer.getEventType() +
                                  "' published to Disruptor");
            }
        }
    }

    /**
     * Publish transaction commit event to Disruptor for processing
     *
     * @param transactionEvent {@link InboundTransactionEvent}
     * @param channel          {@link AndesChannel}
     */
    public void requestTransactionCommitEvent(InboundTransactionEvent transactionEvent, AndesChannel channel) {
        requestTransactionEvent(transactionEvent, InboundEventContainer.Type.TRANSACTION_COMMIT_EVENT, channel);
    }

    /**
     * Publish rollback event to Disruptor for processing.
     *
     * @param transactionEvent {@link InboundTransactionEvent}
     * @param channel          {@link AndesChannel}
     */
    public void requestTransactionRollbackEvent(InboundTransactionEvent transactionEvent, AndesChannel channel) {
        requestTransactionEvent(transactionEvent, InboundEventContainer.Type.TRANSACTION_ROLLBACK_EVENT, channel);
    }

    /**
     * Publish transaction close event to Disruptor for processing
     *
     * @param transactionEvent {@link InboundTransactionEvent}
     * @param channel          {@link AndesChannel}
     */
    public void requestTransactionCloseEvent(InboundTransactionEvent transactionEvent, AndesChannel channel) {
        requestTransactionEvent(transactionEvent, InboundEventContainer.Type.TRANSACTION_CLOSE_EVENT, channel);
    }

    /**
     * different transaction related events are published to Disruptor using this method
     *
     * @param transactionEvent {@link InboundTransactionEvent}
     * @param eventType        {@link InboundEventContainer.Type}
     * @param channel          {@link AndesChannel}
     */
    private void requestTransactionEvent(InboundTransactionEvent transactionEvent,
                                         InboundEventContainer.Type eventType, AndesChannel channel) {
        long sequence = ringBuffer.next();
        InboundEventContainer eventContainer = ringBuffer.get(sequence);

        try {
            eventContainer.setEventType(eventType);
            eventContainer.setTransactionEvent(transactionEvent);
            eventContainer.pubAckHandler = disablePubAck;
            eventContainer.setChannel(channel);
        } finally {
            ringBuffer.publish(sequence);
            if (log.isDebugEnabled()) {
                log.debug("[ Sequence: " + sequence + " ] " + eventContainer.getEventType() +
                                  "' published to Disruptor");
            }
        }
    }

    /**
     * Stop disruptor. This wait until disruptor process pending events in ring buffer.
     */
    public void stop() {
        disruptor.shutdown();
    }

    /**
     * Utility to get the in bound ring gauge
     */
    private class InBoundRingGauge implements Gauge<Long> {

        @Override
        public Long getValue() {
            //The total message size will be reduced by the remaining capacity to get the total ring size
            return ringBuffer.getBufferSize() - ringBuffer.remainingCapacity();
        }
    }

    /**
     * Utility to get the acked message count
     */
    private class AckedMessageCountGauge implements Gauge<Integer> {

        @Override
        public Integer getValue() {
            //Acknowledged message count at a given time
            return ackedMessageCount.getAndSet(0);
        }
    }

}
