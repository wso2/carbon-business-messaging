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

package org.wso2.carbon.andes.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.andes.core.internal.cluster.ClusterResourceHolder;
import org.wso2.carbon.andes.core.subscription.LocalSubscription;

/**
 * This class will handle removing messages depending on subscription behavior
 */
public class OrphanedMessageHandler implements SubscriptionListener {
    private static Log log = LogFactory.getLog(OrphanedMessageHandler.class);
    AndesSubscriptionManager subscriptionManager = ClusterResourceHolder.getInstance().getSubscriptionManager();

    /**
     * Handle subscription changes in cluster. This will perform
     * what needs to be done to the messages addressed to the subscriber
     *
     * @param subscription subscription changed
     * @param changeType   type of change happened
     * @throws AndesException
     */
    @Override
    public void handleClusterSubscriptionsChanged(AndesSubscription subscription, SubscriptionChange changeType)
            throws AndesException {

    }

    /**
     * Handle local subscription changes. This will perform
     * what needs to be done to the messages addressed to the subscriber
     *
     * @param localSubscription subscription changed
     * @param changeType        type of change happened
     * @throws AndesException
     */
    @Override
    public void handleLocalSubscriptionsChanged(LocalSubscription localSubscription,
                                                SubscriptionChange changeType)
            throws AndesException {

        switch (changeType) {
            case ADDED:
                break;
            /**
             * When a normal topic subscription closes, check if there is any other subscription
             * available for subscribed destination (considering hierarchical case). If there is
             * none purge all messages addressed to the storage queue belonging to this node
             */
            case DISCONNECTED:
                if (localSubscription.getTargetQueueBoundExchangeName()
                        .equals(AndesUtils.TOPIC_DELIVERY_STRATEGY) && !localSubscription
                        .isDurable()) {
                    String subscribedDestination = localSubscription.getSubscribedDestination();
                    if (!subscriptionManager.checkIfActiveNonDurableLocalSubscriptionExistsForTopic
                            (subscribedDestination, localSubscription.getProtocolType())) {
                        if (log.isDebugEnabled()) {
                            log.debug("Purging messages of this node persisted under " + subscribedDestination);
                        }
                        log.info("Purging messages of this node persisted under " + subscribedDestination);
                        removeMessagesOfDestinationForNode(subscribedDestination, null,
                                                           localSubscription.getProtocolType(), DestinationType.TOPIC);
                    }

                }
                break;
            case DELETED:
                /**
                 * When a normal topic subscription closes, check if there is any other subscription
                 * available for subscribed destination (considering hierarchical case). If there is
                 * none purge all messages addressed to the storage queue belonging to this node
                 */
                if (DestinationType.TOPIC == localSubscription.getDestinationType()) {
                    String subscribedDestination = localSubscription.getSubscribedDestination();
                    if (!subscriptionManager.checkIfActiveNonDurableLocalSubscriptionExistsForTopic
                            (subscribedDestination, localSubscription.getProtocolType())) {
                        log.info("Purging messages of this node persisted under " + subscribedDestination);
                        removeMessagesOfDestinationForNode(subscribedDestination, null,
                                                           localSubscription.getProtocolType(), DestinationType.TOPIC);
                    }
                }
                break;
        }
    }

    /**
     * Remove all the messages persisted for a destination.
     *
     * @param destination     The destination
     * @param ownerName       Owner of the destination
     * @param protocolType    The protocol which the destination belongs to
     * @param destinationType The destination type
     * @throws AndesException
     */
    private void removeMessagesOfDestinationForNode(String destination, String ownerName, ProtocolType protocolType,
                                                    DestinationType destinationType) throws AndesException {

        MessagingEngine.getInstance().purgeMessages(destination, ownerName, protocolType, destinationType);
    }
}
