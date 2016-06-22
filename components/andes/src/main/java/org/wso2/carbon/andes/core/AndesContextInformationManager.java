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
import org.wso2.carbon.andes.core.internal.cluster.coordination.ClusterCoordinationHandler;
import org.wso2.carbon.andes.core.internal.cluster.coordination.hazelcast.HazelcastAgent;
import org.wso2.carbon.andes.core.store.AndesContextStore;
import org.wso2.carbon.andes.core.store.MessageStore;
import org.wso2.carbon.andes.core.subscription.SubscriptionEngine;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * This class is for managing control information of
 * Andes. (eg: exchanges/queues/bindings)
 */
public class AndesContextInformationManager {

    /**
     * The logger used for logging information, warnings, errors and etc.
     */
    private static final Log log = LogFactory.getLog(AndesContextInformationManager.class);

    //keep listeners that should be triggered when constructs are updated
    private List<QueueListener> queueListeners = new ArrayList<QueueListener>();
    private List<ExchangeListener> exchangeListeners = new ArrayList<ExchangeListener>();
    private List<BindingListener> bindingListeners = new ArrayList<BindingListener>();

    /**
     * Reference to AndesContextStore to manage exchanges/bindings and queues in persistence storage
     */
    private AndesContextStore contextStore;

    /**
     * Reference to message store to be used from message count related functionality
     */
    private MessageStore messageStore;

    /**
     * To manage exchanges bindings and queues
     */
    private AMQPConstructStore constructStore;

    /**
     * Interface to store and retrieve Andes subscription related information
     */
    private SubscriptionEngine subscriptionEngine;

    /**
     * Manages all operations related to subscription changes such as addition, disconnection and deletion
     */
    AndesSubscriptionManager subscriptionManager;

    /**
     * Initializes the andes context information manager
     *
     * @param subscriptionEngine The subscriptions store
     */
    public AndesContextInformationManager(AMQPConstructStore constructStore,
                                          SubscriptionEngine subscriptionEngine,
                                          AndesContextStore contextStore,
                                          MessageStore messageStore) {

        this.subscriptionManager = ClusterResourceHolder.getInstance().getSubscriptionManager();
        this.subscriptionEngine = subscriptionEngine;
        this.messageStore = messageStore;
        this.contextStore = contextStore;
        this.constructStore = constructStore;
        //register listeners for queue changes
        addQueueListener(new ClusterCoordinationHandler(HazelcastAgent.getInstance()));

        //register listeners for exchange changes
        addExchangeListener(new ClusterCoordinationHandler(HazelcastAgent.getInstance()));

        //register listeners for binding changes
        addBindingListener(new ClusterCoordinationHandler(HazelcastAgent.getInstance()));
    }

    /**
     * Register a listener interested in local binding changes
     *
     * @param listener listener to register
     */
    public void addBindingListener(BindingListener listener) {
        bindingListeners.add(listener);
    }

    /**
     * Register a listener interested on queue changes
     *
     * @param listener listener to be registered
     */
    public void addQueueListener(QueueListener listener) {
        queueListeners.add(listener);
    }

    /**
     * Register a listener interested on exchange changes
     *
     * @param listener listener to be registered
     */
    public void addExchangeListener(ExchangeListener listener) {
        exchangeListeners.add(listener);
    }

    /**
     * Create an exchange in andes kernel
     *
     * @param exchange qpid exchange
     * @throws AndesException
     */
    public void createExchange(AndesExchange exchange) throws AndesException {
        constructStore.addExchange(exchange, true);
        notifyExchangeListeners(exchange, ExchangeListener.ExchangeChange.Added);
    }

    /**
     * Delete exchange from andes kernel
     *
     * @param exchange exchange to delete
     * @throws AndesException
     */
    public void deleteExchange(AndesExchange exchange) throws AndesException {
        constructStore.removeExchange(exchange.exchangeName, true);
        notifyExchangeListeners(exchange, ExchangeListener.ExchangeChange.Deleted);
    }

    /**
     * Create queue in andes kernel
     *
     * @param queue queue to create
     * @throws AndesException
     */
    public void createQueue(AndesQueue queue) throws AndesException {
        constructStore.addQueue(queue, true);
        notifyQueueListeners(queue, QueueListener.QueueEvent.ADDED);
    }

    /**
     * Check if queue is deletable
     *
     * @param queueName       name of the queue
     * @param protocolType    The protocol which this queue belongs to
     * @param destinationType The destination type of the queue
     * @return possibility of deleting queue
     * @throws AndesException
     */
    public boolean checkIfQueueDeletable(String queueName, ProtocolType protocolType, DestinationType destinationType)
            throws AndesException {
        boolean queueDeletable = false;

        Set<AndesSubscription> queueSubscriptions =
                subscriptionEngine.getClusterSubscribersForDestination(queueName, protocolType, destinationType);

        if (queueSubscriptions.isEmpty()) {
            queueDeletable = true;
        }
        return queueDeletable;
    }

    /**
     * Delete the queue from broker. This will purge the queue and
     * delete cluster-wide
     *
     * @param queueName       name of the queue
     * @param protocolType    The protocol which the queue to delete belongs to
     * @param destinationType The destination type which the queue belongs to
     * @throws AndesException
     */
    public void deleteQueue(String queueName, ProtocolType protocolType, DestinationType destinationType)
            throws AndesException {
        //identify queue to delete
        AndesQueue queueToDelete = null;
        List<AndesQueue> queueList = contextStore.getAllQueuesStored();
        for (AndesQueue queue : queueList) {
            if (queue.queueName.equals(queueName)) {
                queueToDelete = queue;
                break;
            }
        }

        //delete all local and cluster subscription entries if remaining (inactive entries)
        subscriptionManager.deleteAllLocalSubscriptionsOfBoundQueue(queueName, protocolType, destinationType);
        subscriptionManager.deleteAllClusterSubscriptionsOfBoundQueue(queueName, protocolType, destinationType);

        //purge the queue cluster-wide
        MessagingEngine.getInstance().purgeMessages(queueName, null, protocolType, destinationType);

        // delete queue from construct store
        constructStore.removeQueue(queueName);

        //Notify cluster to delete queue
        notifyQueueListeners(queueToDelete, QueueListener.QueueEvent.DELETED);
        log.info("Delete queue : " + queueName);
    }

    /**
     * Create andes binding in Andes kernel
     *
     * @param andesBinding binding to be created
     * @throws AndesException
     */
    public void createBinding(AndesBinding andesBinding) throws AndesException {
        constructStore.addBinding(andesBinding, true);
        notifyBindingListeners(andesBinding, BindingListener.BindingEvent.ADDED);
    }

    /**
     * Remove andes binding from andes kernel
     *
     * @param andesBinding binding to be removed
     * @throws AndesException
     */
    public void removeBinding(AndesBinding andesBinding) throws AndesException {
        constructStore.removeBinding(andesBinding.boundExchangeName,
                                     andesBinding.boundQueue.queueName, true);
        notifyBindingListeners(andesBinding, BindingListener.BindingEvent.DELETED);
    }

    /**
     * Notifying the exchange listeners stating that a change has occurred for the exchanges in the
     * local node. This will then get notified throughout the cluster if clustered deployment is
     * available.
     *
     * @param exchange The andes exchange in which the change occurred. The exchange can be
     *                 "default", "direct" or "topic".
     * @param change   The change that is being occurred
     * @throws AndesException
     */
    private void notifyExchangeListeners(AndesExchange exchange,
                                         ExchangeListener.ExchangeChange change)
            throws AndesException {
        for (ExchangeListener listener : exchangeListeners) {
            listener.handleLocalExchangesChanged(exchange, change);
        }
    }

    /**
     * Notifying the queue listeners stating that a change has occurred for the queues in the
     * local node. This will then get notified throughout the cluster if clustered deployment is
     * available.
     *
     * @param queue  Andes queue in which the change occurred.
     * @param change The change that was occurred.
     * @throws AndesException
     */
    private void notifyQueueListeners(AndesQueue queue, QueueListener.QueueEvent change)
            throws AndesException {
        for (QueueListener listener : queueListeners) {
            listener.handleLocalQueuesChanged(queue, change);
        }
    }

    /**
     * Notifying the bindings listeners stating that a change has occurred for the bindings in the
     * local node. This will then get notified throughout the cluster if clustered deployment is
     * available.
     *
     * @param binding The binding in which the change occurred. A binding describes the relationship
     *                between an exchange and a messages queue which is represented by the routing
     *                key.
     * @param change  The change that occurred.
     * @throws AndesException
     */
    private void notifyBindingListeners(AndesBinding binding, BindingListener.BindingEvent change)
            throws AndesException {
        for (BindingListener listener : bindingListeners) {
            listener.handleLocalBindingsChanged(binding, change);
        }
    }
}
