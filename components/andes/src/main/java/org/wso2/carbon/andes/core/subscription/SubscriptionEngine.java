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

import com.gs.collections.impl.map.mutable.ConcurrentHashMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.andes.core.AndesException;
import org.wso2.carbon.andes.core.AndesMessageMetadata;
import org.wso2.carbon.andes.core.AndesSubscription;
import org.wso2.carbon.andes.core.DestinationType;
import org.wso2.carbon.andes.core.ProtocolInfo;
import org.wso2.carbon.andes.core.ProtocolType;
import org.wso2.carbon.andes.core.SubscriptionListener.SubscriptionChange;
import org.wso2.carbon.andes.core.internal.AndesContext;
import org.wso2.carbon.andes.core.internal.cluster.ClusterResourceHolder;
import org.wso2.carbon.andes.core.internal.metrics.MetricsConstants;
import org.wso2.carbon.andes.core.store.AndesContextStore;
import org.wso2.carbon.metrics.core.Gauge;
import org.wso2.carbon.metrics.core.Level;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Wraps different protocol related subscription stores. Operations on the subscription store are done through this.
 */
public class SubscriptionEngine {

    private static Log log = LogFactory.getLog(SubscriptionEngine.class);

    /**
     * Channel wise indexing of local subscriptions for acknowledgement handling
     */
    private Map<UUID, LocalSubscription> channelIdMap = new ConcurrentHashMap<>();

    private AndesContextStore andesContextStore;

    private SubscriptionProcessor clusterSubscriptionProcessor;
    private SubscriptionProcessor localSubscriptionProcessor;

    public SubscriptionEngine() throws AndesException {
        andesContextStore = AndesContext.getInstance().getAndesContextStore();
        clusterSubscriptionProcessor = SubscriptionProcessorBuilder.getClusterSubscriptionProcessor();
        localSubscriptionProcessor = SubscriptionProcessorBuilder.getLocalSubscriptionProcessor();
    }

    /**
     * get all (ACTIVE/INACTIVE) CLUSTER subscription entries subscribed for a queue/topic
     * hierarchical topic subscription mapping also happens here
     *
     * @param destination     queue/topic name
     * @param protocolType    Type of the subscriptions
     * @param destinationType The destination type to retrieve subscribers for
     * @return Set of andes subscriptions
     * @throws AndesException
     */
    public Set<AndesSubscription> getClusterSubscribersForDestination(String destination, ProtocolType protocolType,
                                                                      DestinationType destinationType) throws
                                                                                                       AndesException {

        return clusterSubscriptionProcessor.getMatchingSubscriptions(destination, protocolType, destinationType);
    }

    /**
     * get all ACTIVE LOCAL subscription entries subscribed for a destination/topic
     * Hierarchical topic mapping is NOT considered here
     *
     * @param destination     queue/topic name
     * @param protocolType    The subscription type to retrieve subscribers for
     * @param destinationType The destination type to retrieve subscribers for
     * @return list of matching subscriptions
     */
    public Set<LocalSubscription> getActiveLocalSubscribers(String destination, ProtocolType protocolType,
                                                            DestinationType destinationType) throws AndesException {

        Set<AndesSubscription> localSubscriptions = localSubscriptionProcessor
                .getMatchingSubscriptions(destination, protocolType, destinationType);

        Set<LocalSubscription> activeLocalSubscriptionList = new HashSet<>();

        if (null != localSubscriptions) {

            for (AndesSubscription localSubscription : localSubscriptions) {
                if (localSubscription.hasExternalSubscriptions()) {
                    activeLocalSubscriptionList.add((LocalSubscription) localSubscription);
                }
            }

        }

        return activeLocalSubscriptionList;
    }

    /**
     * Get local subscription given the channel id of subscription
     *
     * @param channelID id of the channel subscriber deals with
     * @return subscription object. Null if no match
     * @throws AndesException
     */
    public LocalSubscription getLocalSubscriptionForChannelId(UUID channelID) throws AndesException {
        return channelIdMap.get(channelID);
    }

    /**
     * get all ACTIVE CLUSTER subscription entries subscribed on a given node
     *
     * @param nodeID id of the broker node
     * @return list of subscriptions
     */
    public Set<AndesSubscription> getActiveClusterSubscribersForNode(String nodeID) {
        return clusterSubscriptionProcessor.getActiveSubscribersForNode(nodeID);
    }

    /**
     * Get all cluster subscription entries subscribed on a given node.
     *
     * @param nodeID ID of the broker node
     * @return list of subscriptions
     */
    public Set<AndesSubscription> getClusterSubscribersForNode(String nodeID) {
        return clusterSubscriptionProcessor.getSubscribersForNode(nodeID);
    }

    /**
     * Get all active local subscribers subscribed to current node.
     *
     * @return Set of active local subscribers.
     */
    public Set<AndesSubscription> getActiveLocalSubscribersForNode() {
        return localSubscriptionProcessor
                .getActiveSubscribersForNode(ClusterResourceHolder.getInstance().getClusterManager().getMyNodeID());
    }

    /**
     * UI ONLY.
     * get number of active subscribers for queue/topic in CLUSTER
     *
     * @param destination     queue/topic name
     * @param protocolType    Type of the subscriptions
     * @param destinationType The type of the destination to get subscription count for
     * @return number of subscriptions in cluster
     * @throws AndesException
     */
    public int numberOfSubscriptionsInCluster(String destination, ProtocolType protocolType,
                                              DestinationType destinationType) throws AndesException {
        return getClusterSubscribersForDestination(destination, protocolType, destinationType).size();
    }

    /**
     * Check if a given subscription is already available in the subscription store.
     * Use to validate data of the subscription store.
     *
     * @param subscription The subscription to check for
     * @return True if available in the store
     * @throws AndesException
     */
    public boolean isSubscriptionAvailable(AndesSubscription subscription) throws AndesException {
        return clusterSubscriptionProcessor.isSubscriptionAvailable(subscription);
    }

    /**
     * Get ALL (ACTIVE + INACTIVE) local subscriptions whose bound queue is given
     *
     * @param queueName Queue name to search
     * @return List if matching subscriptions
     * @throws AndesException
     */
    public Set<LocalSubscription> getListOfLocalSubscriptionsBoundToQueue(String queueName, ProtocolType protocolType,
                                                                          DestinationType destinationType)
            throws AndesException {

        Set<LocalSubscription> subscriptionsOfQueue = new HashSet<>();
        Set<AndesSubscription> andesSubscriptions = localSubscriptionProcessor
                .getMatchingSubscriptions(queueName, protocolType, destinationType);

        for (AndesSubscription andesSubscription : andesSubscriptions) {
            if (andesSubscription instanceof LocalSubscription && andesSubscription.getTargetQueue()
                    .equals(queueName)) {
                LocalSubscription localSubscription = (LocalSubscription) andesSubscription;
                subscriptionsOfQueue.add(localSubscription);
            }
        }

        return subscriptionsOfQueue;
    }

    /**
     * Get ALL (ACTIVE + INACTIVE) cluster subscriptions whose bound queue is given
     *
     * @param queueName       Queue name to search.
     * @param protocolType    The protocol for the relevant subscription.
     * @param destinationType The destination type for the relevant subscription.
     * @return List if matching subscriptions.
     * @throws AndesException
     */
    public Set<AndesSubscription> getListOfClusterSubscriptionsBoundToQueue(String queueName, ProtocolType protocolType,
                                                                            DestinationType destinationType)
            throws AndesException {

        Set<AndesSubscription> subscriptionsOfQueue = new HashSet<>();
        Set<AndesSubscription> queueSubscriptions = clusterSubscriptionProcessor
                .getAllSubscriptionsForDestinationType(protocolType, destinationType);

        // Add queue subscriptions
        if (null != queueSubscriptions) {
            for (AndesSubscription sub : queueSubscriptions) {
                if (sub.getTargetQueue().equals(queueName)) {
                    subscriptionsOfQueue.add(sub);
                }
            }
        }

        return subscriptionsOfQueue;
    }

    /**
     * create disconnect or remove a cluster subscription entry.
     *
     * @param subscription subscription to add disconnect or remove
     * @param type         tye pf change
     * @throws AndesException
     */
    public synchronized void createDisconnectOrRemoveClusterSubscription(AndesSubscription subscription,
                                                                         SubscriptionChange type) throws
                                                                                                  AndesException {

        if (SubscriptionChange.ADDED == type) {
            clusterSubscriptionProcessor.addSubscription(subscription);
        } else if (SubscriptionChange.DELETED == type) {
            clusterSubscriptionProcessor.removeSubscription(subscription);
        } else if (SubscriptionChange.DISCONNECTED == type) {
            subscription.setHasExternalSubscriptions(false);
            clusterSubscriptionProcessor.updateSubscription(subscription);
        }
    }

    /**
     * Update a subscription object with the given object.
     * Use to update subscription properties of already available subscriptions.
     *
     * @param subscription The subscription with updated properties
     * @throws AndesException
     */
    public void updateClusterSubscription(AndesSubscription subscription) throws AndesException {
        clusterSubscriptionProcessor.updateSubscription(subscription);
    }

    /**
     * Create,disconnect or remove local subscription
     *
     * @param subscription subscription to add/disconnect or remove
     * @param type         type of change
     * @throws AndesException
     */
    public synchronized void createDisconnectOrRemoveLocalSubscription(LocalSubscription subscription,
                                                                       SubscriptionChange type) throws AndesException {

        if (SubscriptionChange.ADDED == type) {
            localSubscriptionProcessor.addSubscription(subscription);

            //Store the subscription
            andesContextStore.storeDurableSubscription(subscription);
            log.info("Local subscription " + type + " " + subscription.toString());

        } else if (SubscriptionChange.DELETED == type) {
            localSubscriptionProcessor.removeSubscription(subscription);

            removeSubscriptionDirectly(subscription);
            log.info("Local Subscription " + type + " " + subscription.toString());

        } else if (SubscriptionChange.DISCONNECTED == type) {
            updateLocalSubscription(subscription);
            log.info("Local subscription " + type + " " + subscription.toString());
        }

        // Update channel id map
        if (type == SubscriptionChange.ADDED) {
            channelIdMap.put(subscription.getChannelID(), subscription);
        } else { //@DISCONNECT or REMOVE
            UUID channelIDOfSubscription = subscription.getChannelID();
            //when we delete the mock durable topic subscription it has no underlying channel
            if (null != channelIDOfSubscription) {
                channelIdMap.remove(channelIDOfSubscription);
            }
        }

    }

    /**
     * Update local subscription in database
     *
     * @param subscription updated subscription
     * @throws AndesException
     */
    public void updateLocalSubscription(LocalSubscription subscription) throws AndesException {

        if (localSubscriptionProcessor.isSubscriptionAvailable(subscription)) {
            localSubscriptionProcessor.updateSubscription(subscription);
            andesContextStore.updateDurableSubscription(subscription);
        } else {
            localSubscriptionProcessor.addSubscription(subscription);
            andesContextStore.storeDurableSubscription(subscription);
        }

        UUID channelIDOfSubscription = subscription.getChannelID();
        channelIdMap.put(channelIDOfSubscription, subscription);
    }

    /**
     * Directly remove a subscription from store
     *
     * @param subscriptionToRemove subscription to remove
     * @throws AndesException on an exception dealing with store
     */
    public void removeSubscriptionDirectly(AndesSubscription subscriptionToRemove) throws AndesException {
        String destination = subscriptionToRemove.getSubscribedDestination();
        andesContextStore.removeDurableSubscription(subscriptionToRemove);
        if (log.isDebugEnabled()) {
            log.debug("Directly removed cluster subscription for " + "destination = " + destination);
        }
    }

    /**
     * To remove the local subscription
     *
     * @param subscription Subscription to be removed
     * @throws AndesException
     */
    public void removeLocalSubscription(LocalSubscription subscription) throws AndesException {

        localSubscriptionProcessor.removeSubscription(subscription);

        removeSubscriptionDirectly(subscription);
    }

    /**
     * Remove cluster subscriptions from database
     *
     * @param subscriptionToRemove The the set of andes subscriptions to be removed
     */
    public void removeClusterSubscriptions(Set<AndesSubscription> subscriptionToRemove) throws AndesException {
        for (AndesSubscription subscription : subscriptionToRemove) {
            String destination = subscription.getSubscribedDestination();
            if (!subscriptionToRemove.isEmpty()) {

                andesContextStore.removeDurableSubscription(subscription);
                if (log.isDebugEnabled()) {
                    log.debug("Subscription Removed for  " + destination + "@" + subscription.getSubscriptionID() + " "
                                      + subscriptionToRemove);
                }
            } else {
                log.warn("Could not find a cluster subscription ID " + subscription.getSubscriptionID()
                                 + " under destination " + destination);
            }
        }
    }

    /**
     * Gets a set of ACTIVE and INACTIVE topics in cluster
     *
     * @return set of ACTIVE and INACTIVE topics in cluster
     */
    public Set<String> getTopics() {
        return clusterSubscriptionProcessor.getAllDestinations(DestinationType.TOPIC);
    }

    /**
     * Return destination based on subscription
     * Destination would be target queue if it is durable topic, otherwise it is queue or non durable topic
     *
     * @param subscription subscription to get destination
     * @return destination of subscription
     */
    public String getDestination(AndesSubscription subscription) {
        if (DestinationType.DURABLE_TOPIC == subscription.getDestinationType()) {
            return subscription.getTargetQueue();
        } else {
            return subscription.getSubscribedDestination();
        }
    }

    /**
     * Marks all the durable subscriptions for a specific node with "has external" false. Meaning
     * that the subscription is marked disconnected. The "has external" refers that the subscription
     * is active or not.
     *
     * @param isCoordinator True if current node is the coordinator, false otherwise.
     * @param nodeID        The current node ID.
     * @throws AndesException Throw when updating the context store.
     */
    public void deactivateClusterDurableSubscriptionsForNodeID(boolean isCoordinator, String nodeID)
            throws AndesException {

        Set<AndesSubscription> subscriptionsForNode = clusterSubscriptionProcessor.getActiveSubscribersForNode(nodeID);

        for (AndesSubscription subscription : subscriptionsForNode) {
            if (DestinationType.DURABLE_TOPIC == subscription.getDestinationType() && subscription
                    .hasExternalSubscriptions()) {

                // Marking the subscription as false
                subscription.setHasExternalSubscriptions(false);

                if (log.isDebugEnabled()) {
                    log.debug("Updating cluster map with subscription ID : " + subscription.getSubscriptionID() +
                                      " with has external as false.");
                }

                // Updating the context store by the coordinator
                if (isCoordinator) {
                    andesContextStore.updateDurableSubscription(subscription);
                    if (log.isDebugEnabled()) {
                        log.debug("Updating context store with subscription ID : " + subscription.getSubscriptionID()
                                          + " with has external as false.");
                    }
                }
            }
        }
    }

    /**
     * Marks all the durable subscriptions as inactive
     *
     * @throws AndesException Throw when updating the context store.
     */
    public void deactivateAllActiveSubscriptions() throws AndesException {

        Map<String, String> subscriptions = andesContextStore.getAllDurableSubscriptionsByID();
        Map<String, String> modifiedSubscriptions = new HashMap<>();
        for (Map.Entry<String, String> entry : subscriptions.entrySet()) {

            if (log.isDebugEnabled()) {
                log.debug("Deactivating subscription with id: " + entry.getKey());
            }
            BasicSubscription subscription = new BasicSubscription(entry.getValue());

            //The HasExternalSubscriptions attribute of a subscription indicates whether the the subscription is active
            //Therefore, setting it to false makes the subscriptions inactive
            subscription.setHasExternalSubscriptions(false);

            modifiedSubscriptions.put(entry.getKey(), subscription.encodeAsStr());

        }

        //update all the stored durable subscriptions to be inactive
        andesContextStore.updateDurableSubscriptions(modifiedSubscriptions);
    }

    /**
     * Filter out the subscriptions based on the 'selector' set. This modifies the input
     * collections of subscriptions
     *
     * @param subscriptions4Queue collection of subscriptions
     * @param message             message to evaluate selectors against
     */
    public void filterInterestedSubscriptions(Collection<LocalSubscription> subscriptions4Queue,
                                              AndesMessageMetadata message) throws AndesException {

        Iterator<LocalSubscription> subscriptionIterator = subscriptions4Queue.iterator();

        while (subscriptionIterator.hasNext()) {
            LocalSubscription subscription = subscriptionIterator.next();
            if (!subscription.isMessageAcceptedBySelector(message)) {
                subscriptionIterator.remove();
            }
        }
    }

    /**
     * Retrieve all the cluster subscriptions.
     *
     * @return Set of all the cluster subscriptions
     */
    public Set<AndesSubscription> getAllClusterSubscriptions() {
        return clusterSubscriptionProcessor.getAllSubscriptions();
    }

    /**
     * Retrieve all the cluster subscribers for a specific destination type in a specific protocol.
     *
     * @param protocolType    The protocol type to retrieve data from
     * @param destinationType The destination type of the subscriptions to retrieve
     * @return Set of subscriptions registered for the given destination type in given protocol.
     */
    public Set<AndesSubscription> getAllClusterSubscriptionsForDestinationType(ProtocolType protocolType,
                                                                               DestinationType destinationType) {
        return clusterSubscriptionProcessor.getAllSubscriptionsForDestinationType(protocolType, destinationType);
    }

    /**
     * Gauge will return total number of queue subscriptions for current node
     */
    private class QueueSubscriberGauge implements Gauge<Integer> {
        public Integer getValue() {
            Set<ProtocolType> protocols = AndesContext.getInstance().getAndesContextStore().getProtocols();
            int queueSubscriberCount = 0;
            for (ProtocolType protocolType : protocols) {
                if (!protocolType.getProtocolName().startsWith("MQTT")) {
                    queueSubscriberCount = queueSubscriberCount + localSubscriptionProcessor
                            .getAllSubscriptionsForDestinationType(protocolType, DestinationType.QUEUE).size();
                }
            }
            return queueSubscriberCount;
        }
    }

    /**
     * Gauge will return total number of topic subscriptions current node
     */
    private class TopicSubscriberGauge implements Gauge<Integer> {
        public Integer getValue() {
            Set<ProtocolType> protocols = AndesContext.getInstance().getAndesContextStore().getProtocols();
            int topicSubscriberCount = 0;
            for (ProtocolType protocolType : protocols) {
                topicSubscriberCount = topicSubscriberCount + localSubscriptionProcessor
                        .getAllSubscriptionsForDestinationType(protocolType, DestinationType.TOPIC).size();
            }
            return topicSubscriberCount;
        }
    }

    /**
     * Add a handlers to handle specific destination type subscriptions for a protocol.
     *
     * @param protocolInfo The protocol information containing the subscription store information
     */
    public void addSubscriptionHandlersForProtocol(ProtocolInfo protocolInfo) throws AndesException {
        final ProtocolType protocolType = protocolInfo.getProtocolType();

        //Add subscribers gauge to metrics manager
        AndesContext.getInstance().getMetricService().gauge(MetricsConstants.QUEUE_SUBSCRIBERS, Level.INFO,
                new QueueSubscriberGauge());
        //Add topic gauge to metrics manager
        AndesContext.getInstance().getMetricService().gauge(MetricsConstants.TOPIC_SUBSCRIBERS, Level.INFO,
                new TopicSubscriberGauge());

        protocolInfo.getClusterSubscriptionStores().entrySet().stream().forEach(
                entry -> clusterSubscriptionProcessor.addHandler(protocolType, entry.getKey(), entry.getValue()));

        protocolInfo.getLocalSubscriptionStores().entrySet().stream().forEach(
                entry -> localSubscriptionProcessor.addHandler(protocolType, entry.getKey(), entry.getValue()));


    }

    /**
     * Remove handlers specific to a protocol.
     *
     * @param protocolInfo The protocol information object.
     */
    public void removeSubscriptionHandlersForProtocol(ProtocolInfo protocolInfo) {
        final ProtocolType protocolType = protocolInfo.getProtocolType();

        protocolInfo.getClusterSubscriptionStores().keySet().stream().forEach(
                entry -> clusterSubscriptionProcessor.removeHandler(protocolType, entry));

        protocolInfo.getLocalSubscriptionStores().keySet().stream().forEach(
                entry -> localSubscriptionProcessor.removeHandler(protocolType, entry));
    }

}
