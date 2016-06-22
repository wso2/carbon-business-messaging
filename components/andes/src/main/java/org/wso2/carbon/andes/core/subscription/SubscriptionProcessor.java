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

package org.wso2.carbon.andes.core.subscription;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.andes.core.AndesException;
import org.wso2.carbon.andes.core.AndesSubscription;
import org.wso2.carbon.andes.core.DestinationType;
import org.wso2.carbon.andes.core.ProtocolType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * Process adding, removing, updating and retrieving subscriptions managing each subscription type accordingly.
 * The main responsibility is to redirect requests to handle wildcard subscriptions to their specific subscription
 * processor. But can be used to handle all subscriptions.
 * </p>
 * <p>
 * If different subscription types needs different handlers to process subscriptions they can extend {@link
 * AndesSubscriptionStore} and use {@link SubscriptionProcessorBuilder} to get {@link
 * SubscriptionProcessor} intialized with relevant subscription handler for each subscription type.
 * </p>
 * <p>
 * For one subscription type only one subscription handler is allowed.
 * </p>
 */
public class SubscriptionProcessor {

    private static Log log = LogFactory.getLog(SubscriptionProcessor.class);

    /**
     * An object that acts as a composite key to subscription store map structure.
     */
    private class StoreKey {

        ProtocolType protocolType;

        DestinationType destinationType;

        public StoreKey(ProtocolType protocolType, DestinationType destinationType) {
            this.protocolType = protocolType;
            this.destinationType = destinationType;
        }

        /**
         * Generates object specific hashcode.
         * <p>
         * HashCode = 2 * protocolType_hash + destinationType_hash
         * <p>
         * 2 * is used to omit cases such as 2 + 3 == 3 + 2.
         *
         * @return Generated hash code.
         */
        @Override
        public int hashCode() {
            return (2 * protocolType.hashCode()) + destinationType.hashCode();
        }

        /**
         * Evaluates whether the given object is equal to this.
         *
         * @param obj The object to compare
         * @return True if protocol type and destination type is equal.
         */
        @Override
        public boolean equals(Object obj) {
            boolean equal = false;
            if (obj instanceof StoreKey &&
                    ((StoreKey) obj).protocolType.equals(this.protocolType)
                    && ((StoreKey) obj).destinationType.equals(this.destinationType)) {
                equal = true;
            }
            return equal;
        }
    }


    /**
     * Keeps all the handlers for each subscription type.
     */
    // Setting the initial capacity to 11 since after plugging in AMQP and MQTT the size will be 11.
    private Map<StoreKey, AndesSubscriptionStore> subscriptionStores = new HashMap<>(11);

    /**
     * Add a processor for a given protocol
     *
     * @param protocolType           The protocol type of the handler
     * @param destinationType        The destination type of the handler
     * @param andesSubscriptionStore The subscription processor to handle the given protocol
     */
    protected void addHandler(ProtocolType protocolType, DestinationType destinationType,
                              AndesSubscriptionStore andesSubscriptionStore) {
        StoreKey storeKey = new StoreKey(protocolType, destinationType);
        subscriptionStores.put(storeKey, andesSubscriptionStore);
    }

    /**
     * Remove a handler for a given protocol type and destination type.
     *
     * @param protocolType    The protocol type of the handler
     * @param destinationType The destination type of the handler
     */
    protected void removeHandler(ProtocolType protocolType, DestinationType destinationType) {
        subscriptionStores.remove(new StoreKey(protocolType, destinationType));
    }

    /**
     * Get the matching subscription store for the given subscription.
     *
     * @param subscription The subscription to get matching store for
     * @return The store relevant to the given subscription
     */
    private AndesSubscriptionStore getSubscriptionStore(AndesSubscription subscription) throws AndesException {
        return getSubscriptionStore(subscription.getProtocolType(), subscription.getDestinationType());
    }

    /**
     * Always retrieve the correct subscription handler through this method so validations can happen and can avoid
     * unnecessary null pointers in case the subscription type is not found.
     *
     * @param protocolType    The subscription type of the handler
     * @param destinationType The destination type of the handler
     * @return The subscription handler
     */
    private AndesSubscriptionStore getSubscriptionStore(ProtocolType protocolType, DestinationType destinationType)
            throws AndesException {
        StoreKey storeKey = new StoreKey(protocolType, destinationType);

        AndesSubscriptionStore andesSubscriptionStore = subscriptionStores.get(storeKey);

        if (null == andesSubscriptionStore) {
            throw new AndesException("Subscription Store for protocol type " + protocolType + " " +
                                             "and destination type " + destinationType + "is not recognized.");
        }

        return andesSubscriptionStore;
    }

    /**
     * Add a subscription to it's specific subscription handler.
     *
     * @param subscription The subscription to be added
     * @throws AndesException
     */
    public void addSubscription(AndesSubscription subscription) throws AndesException {
        AndesSubscriptionStore andesSubscriptionStore = getSubscriptionStore(subscription);

        if (null != andesSubscriptionStore) {
            andesSubscriptionStore.addSubscription(subscription);
        } else {
            log.warn("A subscription store with protocol type " + subscription.getProtocolType().getProtocolName()
                             + " and destination type " + subscription.getDestinationType() + " is not found to add " +
                             "subscription "
                             + subscription);
        }
    }

    /**
     * Update a wildcard subscription to it's specific subscription handler.
     *
     * @param subscription he subscription to be updated
     * @throws AndesException
     */
    public void updateSubscription(AndesSubscription subscription) throws AndesException {
        AndesSubscriptionStore andesSubscriptionStore = getSubscriptionStore(subscription);
        andesSubscriptionStore.updateSubscription(subscription);
    }

    /**
     * Check if a subscription is already available.
     *
     * @param subscription The subscription to be checked
     * @return True if subscription is found in the handler
     * @throws AndesException
     */
    public boolean isSubscriptionAvailable(AndesSubscription subscription) throws AndesException {
        AndesSubscriptionStore andesSubscriptionStore = getSubscriptionStore(subscription);
        return andesSubscriptionStore.isSubscriptionAvailable(subscription);
    }

    /**
     * Remove a subscription from it's specific subscription handler.
     *
     * @param subscription The subscription to remove
     * @throws AndesException
     */
    public void removeSubscription(AndesSubscription subscription) throws AndesException {
        AndesSubscriptionStore andesSubscriptionStore = getSubscriptionStore(subscription);
        andesSubscriptionStore.removeSubscription(subscription);
    }

    /**
     * Get valid subscriptions for a given non-wildcard destination from it's specific subscription handler.
     *
     * @param destination     The non-wildcard destination
     * @param protocolType    The subscription type to resolve the specific subscription handler
     * @param destinationType The type of the destination to retrieve subscriptions for
     * @return Set of matching subscriptions
     * @throws AndesException
     */
    public Set<AndesSubscription> getMatchingSubscriptions(String destination, ProtocolType protocolType,
                                                           DestinationType destinationType)
            throws AndesException {
        AndesSubscriptionStore andesSubscriptionStore = getSubscriptionStore(protocolType, destinationType);
        return andesSubscriptionStore.getMatchingSubscriptions(destination, destinationType);
    }

    /**
     * Get all active subscribers registered within all types of subscription handlers for a specific node.
     *
     * @param nodeID The Id of the node
     * @return Set of active subscriptions for the given node
     */
    public Set<AndesSubscription> getActiveSubscribersForNode(String nodeID) {
        Set<AndesSubscription> subscriptions = new HashSet<>();

        for (Map.Entry<StoreKey, AndesSubscriptionStore> entry : subscriptionStores.entrySet()) {
            for (AndesSubscription subscription : entry.getValue().getAllSubscriptions()) {
                if (subscription.getSubscribedNode().equals(nodeID) && subscription.hasExternalSubscriptions()) {
                    subscriptions.add(subscription);
                }
            }
        }

        return subscriptions;
    }

    /**
     * Get all subscribers registered within all types of subscription handlers for a specific node.
     *
     * @param nodeID The ID of the node
     * @return Set of active subscriptions for the given node
     */
    public Set<AndesSubscription> getSubscribersForNode(String nodeID) {
        Set<AndesSubscription> subscriptions = new HashSet<>();

        for (Map.Entry<StoreKey, AndesSubscriptionStore> entry : subscriptionStores.entrySet()) {
            for (AndesSubscription subscription : entry.getValue().getAllSubscriptions()) {
                if (subscription.getSubscribedNode().equals(nodeID)) {
                    subscriptions.add(subscription);
                }
            }
        }

        return subscriptions;
    }

    /**
     * Get all destinations that these subscribers have subscribed to
     *
     * @param destinationType The type of the destination to retrieve all destinations for
     * @return Set of all topics
     */
    public Set<String> getAllDestinations(DestinationType destinationType) {
        Set<String> topics = new HashSet<>();

        for (Map.Entry<StoreKey, AndesSubscriptionStore> entry : subscriptionStores.entrySet()) {
            topics.addAll(entry.getValue().getAllDestinations(destinationType));
        }

        return topics;
    }

    /**
     * Retrieve all the subscriptions contained in all the handlers.
     * <p>
     * This method can be used where all available subscriptions should be checked. This method can be used instead
     * of retrieving all the destinations, and then retrieving all the available subscriptions for those destination
     * in a loop.
     *
     * @return All the subscriptions that are saved in memory
     */
    public Set<AndesSubscription> getAllSubscriptions() {
        Set<AndesSubscription> allSubscriptions = new HashSet<>();

        for (Map.Entry<StoreKey, AndesSubscriptionStore> entry : subscriptionStores.entrySet()) {
            allSubscriptions.addAll(entry.getValue().getAllSubscriptions());
        }

        return allSubscriptions;
    }

    /**
     * Get all subscriptions for a specific destination type of a protocol type.
     *
     * @param protocolType    The protocol for which the subscriptions needs to be retrieved
     * @param destinationType The destination type for which the subscriptions needs to be retrieved
     * @return Set of matching subscriptions
     */
    public Set<AndesSubscription> getAllSubscriptionsForDestinationType(ProtocolType protocolType,
                                                                        DestinationType destinationType) {
        StoreKey storeKey = new StoreKey(protocolType, destinationType);

        Set<AndesSubscription> subscriptionsForDestinationType = new HashSet<>();
        for (AndesSubscription subscription : subscriptionStores.get(storeKey).getAllSubscriptions()) {
            if (subscription.getDestinationType() == destinationType) {
                subscriptionsForDestinationType.add(subscription);
            }
        }

        return subscriptionsForDestinationType;
    }
}
