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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.andes.core.AndesChannel;
import org.wso2.carbon.andes.core.AndesException;
import org.wso2.carbon.andes.core.internal.AndesContext;
import org.wso2.carbon.andes.core.internal.cluster.error.detection.NetworkPartitionListener;
import org.wso2.carbon.andes.core.internal.configuration.AndesConfigurationManager;
import org.wso2.carbon.andes.core.internal.configuration.enums.AndesConfiguration;
import org.wso2.carbon.andes.core.internal.metrics.MetricsConstants;
import org.wso2.carbon.andes.core.store.FailureObservingStoreManager;
import org.wso2.carbon.andes.core.store.HealthAwareStore;
import org.wso2.carbon.andes.core.store.StoreHealthListener;
import org.wso2.carbon.metrics.core.Gauge;
import org.wso2.carbon.metrics.core.Level;

import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Flow control is typically employed in controlling fast producers from overloading slow consumers in
 * producer-consumer scenarios. Flow control manager handles flow controlling by blocking and unblocking channels.
 */
public class FlowControlManager implements StoreHealthListener, NetworkPartitionListener {

    /**
     * Class logger
     */
    private static Log log = LogFactory.getLog(FlowControlManager.class);

    /**
     * Global high limit that trigger flow control globally
     */
    private final int globalLowLimit;

    /**
     * Global low limit that disables trigger flow control disable
     */
    private final int globalHighLimit;

    /**
     * List of active channels
     */
    private final ArrayList<AndesChannel> channels;

    /**
     * Executor used for flow control timeout tasks
     */
    private final ScheduledExecutorService executor;

    /**
     * Configured flow control high limit for local channel
     */
    private final int channelHighLimit;

    /**
     * Configured flow control low limit for local channel
     */
    private final int channelLowLimit;

    /**
     * Track total number of unprocessed messages
     */
    private AtomicInteger messagesOnGlobalBuffer;

    /**
     * Indicate if the flow control is enabled globally
     * TODO: Need to evaluate change globalBufferBasedFlowControlEnabled to volatile.
     */
    @SuppressFBWarnings(value = "IS2_INCONSISTENT_SYNC")
    private boolean globalBufferBasedFlowControlEnabled;

    /**
     * Set to true if there are global level error(s) occurred
     */
    private boolean globalErrorBasedFlowControlEnabled;
    /**
     * Global flow control time out task
     */
    private Runnable flowControlTimeoutTask = new BufferBasedFlowControlTimeoutTask();

    /**
     * Used to close the flow control timeout task if not required
     */
    private ScheduledFuture<?> scheduledBufferBasedFlowControlTimeoutFuture;

    /**
     * Flag set to true when shutdown hook triggered and use this flog to avoid
     * unblocking flow control while shutting down
     */
    private boolean shutDownTriggered;

    public FlowControlManager() {
        // Read configured limits
        globalLowLimit = (Integer) AndesConfigurationManager
                .readValue(AndesConfiguration.FLOW_CONTROL_GLOBAL_LOW_LIMIT);
        globalHighLimit = (Integer) AndesConfigurationManager
                .readValue(AndesConfiguration.FLOW_CONTROL_GLOBAL_HIGH_LIMIT);
        channelLowLimit = ((Integer) AndesConfigurationManager
                .readValue(AndesConfiguration.FLOW_CONTROL_BUFFER_BASED_LOW_LIMIT));
        channelHighLimit = ((Integer) AndesConfigurationManager
                .readValue(AndesConfiguration.FLOW_CONTROL_BUFFER_BASED_HIGH_LIMIT));

        if (globalHighLimit <= globalLowLimit || channelHighLimit <= channelLowLimit) {
            throw new RuntimeException("Flow Control limits are not configured correctly.");
        }

        messagesOnGlobalBuffer = new AtomicInteger(0);
        globalBufferBasedFlowControlEnabled = false;
        globalErrorBasedFlowControlEnabled = false;
        channels = new ArrayList<>();

        FailureObservingStoreManager.registerStoreHealthListener(this);
        if (AndesContext.getInstance().isClusteringEnabled()) { // network partition detection works only when clustered
            AndesContext.getInstance().getClusterAgent().addNetworkPartitionListener(this);
        }
        // Initialize executor service for state validity checking
        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().
                setNameFormat("AndesScheduledTaskManager-FlowControl").build();
        executor = Executors.newSingleThreadScheduledExecutor(namedThreadFactory);

        //Will start the gauge
        AndesContext.getInstance().getMetricService().gauge(
                MetricsConstants.ACTIVE_CHANNELS, Level.INFO, new ChannelGauge());
    }

    /**
     * Create a new Andes channel for a new local channel.
     *
     * @param listener  Local flow control listener
     * @param channelId the identifier of the channel
     * @return AndesChannel
     */
    public synchronized AndesChannel createChannel(String channelId,
                                                   FlowControlListener listener) throws AndesException {

        if (globalErrorBasedFlowControlEnabled) {
            throw new AndesException("Global error based flow control is enabled. new connections are not allowed");
        }


        AndesChannel channel = new AndesChannel(this, channelId, listener, globalBufferBasedFlowControlEnabled,
                                                globalErrorBasedFlowControlEnabled);
        channels.add(channel);
        return channel;
    }

    /**
     * Create a new Andes channel for a new local channel.
     *
     * @param listener Local flow control listener
     * @return AndesChannel
     */
    public synchronized AndesChannel createChannel(FlowControlListener listener) throws AndesException {

        if (globalErrorBasedFlowControlEnabled) {
            throw new AndesException("Global error based flow control is enabled. new connections are not allowed");
        }

        AndesChannel channel = new AndesChannel(this, listener, globalBufferBasedFlowControlEnabled,
                                                globalErrorBasedFlowControlEnabled);
        channels.add(channel);
        return channel;
    }

    /**
     * Get the flow control high limit for local channel
     *
     * @return Flow control high limit
     */
    public int getChannelHighLimit() {
        return channelHighLimit;
    }

    /**
     * Get the flow control low limit for local channel
     *
     * @return Flow control low limit
     */
    public int getChannelLowLimit() {
        return channelLowLimit;
    }

    /**
     * Get the scheduled executor used for flow controlling tasks
     *
     * @return Scheduled executor
     */
    public ScheduledExecutorService getScheduledExecutor() {
        return executor;
    }

    /**
     * This method should be called when a message is put into the buffer
     *
     * @param size Number of items added to buffer
     */
    public void notifyAddition(int size) {
        int count = messagesOnGlobalBuffer.addAndGet(size);

        if ((!globalBufferBasedFlowControlEnabled) && (count >= globalHighLimit)) {
            blockListenersOnBufferBasedFlowControl();
        }
    }

    /**
     * This method should be called after a message is processed and no longer required in the buffer.
     *
     * @param size Number of items removed from buffer
     */
    public void notifyRemoval(int size) {
        int count = messagesOnGlobalBuffer.addAndGet(-size);

        if (globalBufferBasedFlowControlEnabled && count <= globalLowLimit) {
            unblockListenersOnBufferBasedFlowControl();
        }
    }


    /**
     * Notify all the channels to enable buffer based flow control
     */
    private synchronized void blockListenersOnBufferBasedFlowControl() {
        if (!globalBufferBasedFlowControlEnabled) {
            globalBufferBasedFlowControlEnabled = true;

            for (AndesChannel channel : channels) {
                channel.notifyGlobalBufferBasedFlowControlActivation();
            }

            scheduledBufferBasedFlowControlTimeoutFuture = executor.schedule(
                    flowControlTimeoutTask, 1, TimeUnit.MINUTES);
            log.info("Global buffer based flow control enabled.");
        }
    }

    /**
     * Notify all the channels to disable buffer based flow control
     */
    private synchronized void unblockListenersOnBufferBasedFlowControl() {
        if (globalBufferBasedFlowControlEnabled && !shutDownTriggered) {
            scheduledBufferBasedFlowControlTimeoutFuture.cancel(false);
            globalBufferBasedFlowControlEnabled = false;

            for (AndesChannel channel : channels) {
                channel.notifyGlobalBufferBasedFlowControlDeactivation();
            }

            log.info("Global buffer based flow control disabled.");
        }
    }

    /**
     * Notify all the channels to enable error based flow control
     */
    private synchronized void blockListenersOnErrorBasedFlowControl(boolean forcefullyDisconnect) {
        if (!globalErrorBasedFlowControlEnabled) {
            globalErrorBasedFlowControlEnabled = true;

            for (AndesChannel channel : channels) {
                channel.notifyGlobalErrorBasedFlowControlActivation();
            }

            if (forcefullyDisconnect) {
                // before the iteration its important to have a constant view on available channels.
                // if we send 'disconnect' to channels using the original collection that will result in a 
                // concurrent modification ( since underlying socket/client is asked to disconnect,
                // client disconnects, and this collection is modified while we iterating.
                ArrayList<AndesChannel> constantView = new ArrayList<>(channels);

                for (AndesChannel channel : constantView) {
                    channel.disconnect();
                }
            }

            log.info("Global error based flow control enabled.");
        }
    }

    /**
     * Notify all the channels to disable error based flow control
     */
    private synchronized void unblockListenersOnErrorBasedFlowControl() {
        if (globalErrorBasedFlowControlEnabled) {
            globalErrorBasedFlowControlEnabled = false;

            for (AndesChannel channel : channels) {
                channel.notifyGlobalErrorBasedFlowControlDeactivation();
            }

            log.info("Global error based flow control disabled.");
        }
    }

    /**
     * Remove channel from tracking
     *
     * @param channel Andes channel
     */
    public synchronized void deleteChannel(AndesChannel channel) {
        channels.remove(channel);

        log.info("Channel removed (ID: " + channel.getIdentifier() + ")");
    }

    /**
     * This timeout task avoid flow control being enforced forever. This can happen if the recordAdditionToBuffer get a
     * context switch after evaluating the existing condition and during that time all the messages present in global
     * buffer get processed from the StateEventHandler.
     */
    private class BufferBasedFlowControlTimeoutTask implements Runnable {
        @Override
        public void run() {
            if (globalBufferBasedFlowControlEnabled && (messagesOnGlobalBuffer.get() <= globalLowLimit)) {
                unblockListenersOnBufferBasedFlowControl();
            }
        }
    }

    /**
     * Notify all channels to enable flow control when shutdown hook triggered to avoid message loss in publishers
     */
    public synchronized void prepareChannelsForShutdown() {
        if (!globalErrorBasedFlowControlEnabled) {
            globalErrorBasedFlowControlEnabled = true;
            shutDownTriggered = true;

            log.info("Prepare channels for shutdown.");

            for (AndesChannel channel : channels) {
                channel.notifyGlobalBufferBasedFlowControlActivation();
            }

            scheduledBufferBasedFlowControlTimeoutFuture =
                    executor.schedule(flowControlTimeoutTask, 1, TimeUnit.MINUTES);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * When message stores becomes offline flow control message will enforce
     * global error based flow control
     */
    @Override
    public void storeNonOperational(HealthAwareStore store, Exception ex) {
        blockListenersOnErrorBasedFlowControl(false); // sending false to be consistent with previous behavior.
    }

    /**
     * {@inheritDoc}
     * <p>
     * When message stores becomes offline flow control message will stop global
     * error based flow control
     */
    @Override
    public void storeOperational(HealthAwareStore store) {
        unblockListenersOnErrorBasedFlowControl();
    }

    /**
     * {@inheritDoc}
     * <p>
     * When the cluster size become less minimum node count required flow
     * control will be enabled.
     * This will effectively let any-partition(s) which has more then minimum
     * node count to accept traffic, while other partitions will not.
     */
    @Override
    public void minimumNodeCountNotFulfilled(int currentNodeCount) {
        log.info("Network partition detected, activating error based flow control");
        blockListenersOnErrorBasedFlowControl(true);
    }

    /**
     * {@inheritDoc}
     * <p>
     * When the cluster size becomes larger than minimum node count required
     * flow
     * control will be disabled.
     * This will effectively let any-partition(s) which has more then minimum
     * node count to accept traffic, while other partitions will not.
     * Note:
     * One side effect is; consider a scenario where:
     * <ol>
     * <li>Data store goes offline and error based flow control in enabled.</li>
     * <li>Network becomes partitioned and error based flow control is invoked (
     * even if flow control is in effect)</li>
     * <li>at a later point in time, datastore become online or Network
     * partition is resolved.</li>
     * </ol>
     * but this method will not wait for other problem to resolve ( network
     * being
     * Partitioned or message store become online) to disable error based flow
     * control.
     */
    @Override
    public void minimumNodeCountFulfilled(int currentNodeCount) {
        log.info("Network partition resolved, deactivating error based flow control");
        unblockListenersOnErrorBasedFlowControl();

    }

    /**
     * This will get current number of channels.
     */
    private class ChannelGauge implements Gauge<Integer> {
        public Integer getValue() {
            return channels.size();
        }
    }
}
