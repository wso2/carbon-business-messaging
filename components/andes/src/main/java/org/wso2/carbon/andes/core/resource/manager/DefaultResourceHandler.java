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

package org.wso2.carbon.andes.core.resource.manager;

import org.wso2.carbon.andes.core.AndesException;
import org.wso2.carbon.andes.core.AndesMessage;
import org.wso2.carbon.andes.core.AndesQueue;
import org.wso2.carbon.andes.core.AndesSubscription;
import org.wso2.carbon.andes.core.DestinationType;
import org.wso2.carbon.andes.core.MessagingEngine;
import org.wso2.carbon.andes.core.ProtocolType;
import org.wso2.carbon.andes.core.internal.AndesContext;
import org.wso2.carbon.andes.core.subscription.LocalSubscription;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A default resource handler that is common for protocols and destination types.
 */
public abstract class DefaultResourceHandler implements ResourceHandler {
    /**
     * Wildcard character to include all.
     */
    private static final String ALL_WILDCARD = "*";

    /**
     * The supported protocol.
     */
    private ProtocolType protocolType;

    /**
     * The supported destination type.
     */
    private DestinationType destinationType;

    /**
     * Initializing the default resource handler with protocol type and destination type.
     *
     * @param protocolType    The protocol type.
     * @param destinationType The destination type.
     */
    public DefaultResourceHandler(ProtocolType protocolType, DestinationType destinationType) {
        this.protocolType = protocolType;
        this.destinationType = destinationType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<AndesQueue> getDestinations(String keyword, int offset, int limit) throws AndesException {
        return AndesContext.getInstance().getAmqpConstructStore().getQueues(keyword)
                .stream()
                .filter(d -> d.getProtocolType() == protocolType)
                .filter(d -> d.getDestinationType() == destinationType)
                .skip(offset)
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AndesQueue getDestination(String destinationName) throws AndesException {
        return AndesContext.getInstance().getAmqpConstructStore().getQueue(destinationName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<AndesSubscription> getSubscriptions(String subscriptionName, String destinationName, boolean active,
                                                    int offset, int limit) throws AndesException {
        Set<AndesSubscription> allClusterSubscriptions = AndesContext.getInstance()
                .getSubscriptionEngine().getAllClusterSubscriptionsForDestinationType(protocolType, destinationType);

        return allClusterSubscriptions.stream()
                .filter(s -> s.getProtocolType() == protocolType)
                .filter(s -> s.isDurable() == ((destinationType == DestinationType.QUEUE)
                                               || (destinationType == DestinationType.DURABLE_TOPIC)))
                .filter(s -> s.hasExternalSubscriptions() == active)
                .filter(s -> null != subscriptionName && !ALL_WILDCARD.equals(subscriptionName)
                             && s.getSubscriptionID().contains(subscriptionName))
                .filter(s -> null != destinationName && !ALL_WILDCARD.equals(destinationName)
                             && s.getSubscribedDestination().equals(destinationName))
                .skip(offset)
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeSubscriptions(String destinationName) throws AndesException {
        AndesQueue destination = this.getDestination(destinationName);
        if (null != destination) {
            Set<AndesSubscription> activeLocalSubscribersForNode = AndesContext.getInstance()
                    .getSubscriptionEngine().getActiveLocalSubscribersForNode();

            List<LocalSubscription> subscriptions = activeLocalSubscribersForNode
                    .stream()
                    .filter(s -> s.getProtocolType() == protocolType)
                    .filter(s -> s.getDestinationType() == destinationType)
                    .filter(s -> null != destinationName && !ALL_WILDCARD.equals(destinationName)
                                 && s.getSubscribedDestination().contains(destinationName))
                    .map(s -> (LocalSubscription) s)
                    .collect(Collectors.toList());
            for (LocalSubscription subscription : subscriptions) {
                subscription.forcefullyDisconnect();
            }
        } else {
            throw new AndesException("Destination '" + destinationName + "' does not exists to removed subscriptions.");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeSubscription(String destinationName, String subscriptionId) throws AndesException {
        AndesQueue destination = this.getDestination(destinationName);
        if (null != destination) {
            Set<LocalSubscription> allSubscribersForDestination = AndesContext.getInstance()
                    .getSubscriptionEngine().getActiveLocalSubscribers(destinationName, protocolType, destinationType);
            LocalSubscription localSubscription = allSubscribersForDestination
                .stream()
                .filter(s -> s.getSubscriptionID().equals(subscriptionId))
                .findFirst()
                .orElseThrow(() -> new NullPointerException("Matching subscription could not be found to disconnect."));
            localSubscription.forcefullyDisconnect();
        } else {
            throw new AndesException("Destination '" + destinationName + "' does not exists to removed subscription '"
                                     + subscriptionId + "'.");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<AndesMessage> browseDestinationWithMessageID(String destinationName, boolean content,
                                                             long nextMessageID, int limit) throws AndesException {
        AndesQueue destination = this.getDestination(destinationName);
        if (null != destination) {
            return MessagingEngine.getInstance().getNextNMessagesFromQueue(destinationName, nextMessageID, limit,
                    content);
        } else {
            throw new AndesException("Destination '" + destinationName + "' does not exists to browse messages.");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<AndesMessage> browseDestinationWithOffset(String destinationName, boolean content, int offset, int
            limit) throws AndesException {
        AndesQueue destination = this.getDestination(destinationName);
        if (null != destination) {
            return MessagingEngine.getInstance().getNextNMessagesFromQueue(destinationName, offset, limit, content);
        } else {
            throw new AndesException("Destination '" + destinationName + "' does not exists to browse messages.");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AndesMessage getMessage(String destinationName, long andesMessageID, boolean content) throws AndesException {
        AndesQueue destination = this.getDestination(destinationName);
        if (null != destination) {
            AndesMessage andesMessage = MessagingEngine.getInstance().getNextNMessagesFromQueue(destinationName,
                    andesMessageID, 1, content).stream().findFirst().orElse(null);
            if (null != andesMessage && andesMessage.getMetadata().getMessageID() != andesMessageID) {
                andesMessage = null;
            }

            return andesMessage;
        } else {
            throw new AndesException("Destination '" + destinationName + "' does not exists to get message.");
        }
    }
}
