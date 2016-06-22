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
import org.wso2.carbon.andes.core.AndesSubscription;
import org.wso2.carbon.andes.core.DestinationType;
import org.wso2.carbon.andes.core.MessagingEngine;
import org.wso2.carbon.andes.core.SubscriptionListener;
import org.wso2.carbon.andes.core.internal.AndesContext;
import org.wso2.carbon.andes.core.subscription.LocalSubscription;
import org.wso2.carbon.andes.core.subscription.SubscriptionEngine;

import java.util.Collection;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * This class will reassign the slots own by this node when its last subscriber leaves
 */
public class OrphanedSlotHandler implements SubscriptionListener {

    private static Log log = LogFactory.getLog(OrphanedSlotHandler.class);

    /**
     * Used for asynchronously execute slot reassign task
     */
    private final ExecutorService executor;

    /**
     * This is used to get the matching local subscription when it leaves. Currently the object references returned
     * for ADDED and DISCONNECTED are not the same even when it is actually the same subscriber.
     */
    private final ConcurrentHashMap<String, LocalSubscription> trackedSubscriptions;

    /**
     * Manger used to notify of the stale storage queue
     */
    private SlotDeliveryWorkerManager slotDeliveryWorkerManager;

    public OrphanedSlotHandler() {
        ThreadFactory namedThreadFactory = new ThreadFactoryBuilder().setNameFormat("AndesReassignSlotTaskExecutor")
                .build();
        executor = Executors.newSingleThreadExecutor(namedThreadFactory);
        slotDeliveryWorkerManager = SlotDeliveryWorkerManager.getInstance();
        trackedSubscriptions = new ConcurrentHashMap<>();
    }

    @Override
    public void handleClusterSubscriptionsChanged(AndesSubscription subscription, SubscriptionChange changeType)
            throws AndesException {
        //Cluster wise changes are not necessary
    }

    @Override
    public void handleLocalSubscriptionsChanged(LocalSubscription subscription, SubscriptionChange changeType)
            throws AndesException {
        switch (changeType) {
            case ADDED:
                if (log.isDebugEnabled()) {
                    log.debug("Adding a subscription " + subscription + " trackedSubscription.size = "
                                      + trackedSubscriptions.size());
                }
                trackedSubscriptions.put(
                        subscription.getSubscribedNode() + subscription.getSubscriptionID(),
                        subscription);
                break;
            case DELETED:
                if (log.isDebugEnabled()) {
                    log.debug("Deleting subscription : " + subscription + " trackedSubscription.size = "
                                      + trackedSubscriptions.size());
                }
                LocalSubscription matchingDeletedSubscription = trackedSubscriptions.remove(
                        subscription.getSubscribedNode() + subscription.getSubscriptionID());

                if (null != matchingDeletedSubscription) {
                    reAssignSlotsIfNeeded(matchingDeletedSubscription);
                } else {
                    log.warn("Deleting a subscription which was not added previously " + subscription);
                    reAssignSlotsIfNeeded(subscription);
                }

                break;
            case DISCONNECTED:
                if (log.isDebugEnabled()) {
                    log.debug("Disconnecting subscription : " + subscription + " trackedSubscription.size = "
                                      + trackedSubscriptions.size());
                }
                LocalSubscription matchingDisconnectedSubscription = trackedSubscriptions.get(
                        subscription.getSubscribedNode() + subscription.getSubscriptionID());

                if (null != matchingDisconnectedSubscription) {
                    // if matching subscription is found, use that so that unacked Messages can also be processed.
                    reAssignSlotsIfNeeded(matchingDisconnectedSubscription);
                } else {
                    log.warn("Disconnection a subscription which was not added previously " + subscription);
                    reAssignSlotsIfNeeded(subscription);
                }

                break;
        }
    }

    /**
     * Re-assign slots back to the slot manager if this is the last subscriber of this node.
     *
     * @param subscription current subscription fo the leaving node
     * @throws AndesException
     */
    private void reAssignSlotsIfNeeded(LocalSubscription subscription) throws AndesException {
        if (subscription.isDurable()) {
            // Problem happens only with Queues and durable topic subscriptions and shared durable topic subscriptions

            String destination;

            if (DestinationType.QUEUE == subscription.getDestinationType()) {
                //Queues and Topics
                destination = subscription.getSubscribedDestination();
            } else {
                // ( effectively at this point (isDurable && isBoundToTopic()) evaluates to true
                // Durable topics subscriptions, and shared durable subscriptions
                // refer to : https://github.com/wso2/andes/wiki/Subscription-Types-and-Attributes
                destination = subscription.getTargetQueue();
            }

            if (null != subscription.getStorageQueueName()) {
                SubscriptionEngine subscriptionEngine = AndesContext.getInstance().getSubscriptionEngine();

                // (!isDurable && isBoundToTopic) is a tautology for ( !  isDurable() )
                // for queues, durable topic subscriptions, shared durable topic subscriptions scenarios
                Collection<LocalSubscription> localSubscribersForQueue = subscriptionEngine
                        .getActiveLocalSubscribers(destination, subscription.getProtocolType(),
                                                   subscription.getDestinationType());
                if (localSubscribersForQueue.size() == 0) {
                    scheduleSlotToReassign(subscription.getStorageQueueName());
                } else {
                    slotDeliveryWorkerManager.rescheduleMessagesForDelivery(subscription.getStorageQueueName(),
                                                                            subscription.getUnackedMessages());
                }
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("subscription.storageQueueName is null for subscription object : " + subscription);
                }
            }
        }
    }

    /**
     * Schedule to re-assign slots of the node related to a particular queue when last subscriber leaves
     *
     * @param storageQueue Name of the storageQueue
     */
    public void scheduleSlotToReassign(String storageQueue) {
        slotDeliveryWorkerManager.stopDeliveryForDestination(storageQueue);
        executor.submit(new SlotReAssignTask(storageQueue));
    }

    /**
     * This class is a scheduler class to schedule re-assignment of slots when last subscriber leaves a particular
     * queue
     */
    private class SlotReAssignTask extends TimerTask {

        /**
         * Storage queue handled by this task
         */
        private String storageQueue;

        public SlotReAssignTask(String storageQueue) {
            this.storageQueue = storageQueue;
        }

        public void run() {
            if (log.isDebugEnabled()) {
                log.debug("Trying to reAssign slots for queue " + storageQueue);
            }
            try {
                MessagingEngine.getInstance().getSlotCoordinator().reAssignSlotWhenNoSubscribers(storageQueue);

                if (log.isDebugEnabled()) {
                    log.debug("Re-assigned slots for queue: " + storageQueue);
                }

            } catch (ConnectionException e) {
                log.error("Error occurred while re-assigning the slot to slot manager", e);
            }
        }
    }
}
