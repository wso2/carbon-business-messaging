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
package org.wso2.carbon.andes.core.internal.cluster.coordination;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.andes.core.AndesBinding;
import org.wso2.carbon.andes.core.AndesException;
import org.wso2.carbon.andes.core.AndesExchange;
import org.wso2.carbon.andes.core.AndesQueue;
import org.wso2.carbon.andes.core.AndesSubscription;
import org.wso2.carbon.andes.core.BindingListener;
import org.wso2.carbon.andes.core.ExchangeListener;
import org.wso2.carbon.andes.core.QueueListener;
import org.wso2.carbon.andes.core.SubscriptionListener;
import org.wso2.carbon.andes.core.internal.AndesContext;
import org.wso2.carbon.andes.core.internal.cluster.ClusterResourceHolder;
import org.wso2.carbon.andes.core.internal.cluster.coordination.hazelcast.HazelcastAgent;
import org.wso2.carbon.andes.core.subscription.BasicSubscription;
import org.wso2.carbon.andes.core.subscription.LocalSubscription;

/**
 * Listens to cluster events (queues, bindings and subscription change events)
 */
public class ClusterCoordinationHandler implements QueueListener,
                                                   ExchangeListener, BindingListener, SubscriptionListener {


    private static Log log = LogFactory.getLog(ClusterCoordinationHandler.class);
    private HazelcastAgent hazelcastAgent;

    public ClusterCoordinationHandler(HazelcastAgent hazelcastAgent) {
        this.hazelcastAgent = hazelcastAgent;
    }

    @Override
    public void handleClusterQueuesChanged(AndesQueue andesQueue, QueueEvent changeType) throws AndesException {
        switch (changeType) {
            case ADDED:
                //create a queue
                ClusterResourceHolder.getInstance().getConfigSynchronizer().clusterQueueAdded(andesQueue);
                break;
            case DELETED:
                //Delete remaining subscriptions from the local and cluster subscription maps
                ClusterResourceHolder.getInstance().getSubscriptionManager().deleteAllLocalSubscriptionsOfBoundQueue(
                        andesQueue.queueName, andesQueue.getProtocolType(), andesQueue.getDestinationType());
                ClusterResourceHolder.getInstance().getSubscriptionManager().deleteAllClusterSubscriptionsOfBoundQueue(
                        andesQueue.queueName, andesQueue.getProtocolType(), andesQueue.getDestinationType());

                //delete queue
                ClusterResourceHolder.getInstance().getConfigSynchronizer().clusterQueueRemoved(andesQueue);
                break;
            case PURGED:
                //purge queue
                ClusterResourceHolder.getInstance().getConfigSynchronizer().clusterQueuePurged(andesQueue);
                break;
        }

    }

    /***
     * {@inheritDoc}
     *
     * @param andesQueue changed queue
     * @param changeType what type of change has happened
     * @throws AndesException
     */
    @Override
    public void handleLocalQueuesChanged(AndesQueue andesQueue, QueueEvent changeType) throws AndesException {
        //notify cluster that queues are changed
        if (AndesContext.getInstance().isClusteringEnabled()) {
            // Notify global listeners
            ClusterNotification clusterNotification = new ClusterNotification(andesQueue.encodeAsString(),
                                                                              changeType.toString(),
                                                                              "Queue Notification Message : " +
                                                                                      changeType.toString());
            hazelcastAgent.notifyQueuesChanged(clusterNotification);
        }

    }

    @Override
    public void handleClusterExchangesChanged(AndesExchange exchange, ExchangeChange changeType) throws AndesException {
        switch (changeType) {
            case Added:
                //create a exchange
                ClusterResourceHolder.getInstance().getConfigSynchronizer().clusterExchangeAdded(exchange);
                break;
            case Deleted:
                //delete exchange
                ClusterResourceHolder.getInstance().getConfigSynchronizer().clusterExchangeRemoved(exchange);
                break;
        }
    }

    @Override
    public void handleLocalExchangesChanged(AndesExchange exchange, ExchangeChange changeType) throws AndesException {
        //notify cluster that exchanges are changed
        if (AndesContext.getInstance().isClusteringEnabled()) {
            ClusterNotification clusterNotification =
                    new ClusterNotification(exchange.encodeAsString(), changeType.toString(),
                                            "Exchange Notification Message : " + changeType.toString());
            hazelcastAgent.notifyExchangesChanged(clusterNotification);
        } else { // if running in standalone mode short-circuit cluster notification
            handleClusterExchangesChanged(exchange, changeType);
        }
    }

    @Override
    public void handleClusterBindingsChanged(AndesBinding binding, BindingEvent changeType) throws AndesException {
        switch (changeType) {
            case ADDED:
                //create a binding
                ClusterResourceHolder.getInstance().getConfigSynchronizer().clusterBindingAdded(binding);
                break;
            case DELETED:
                //delete binding
                ClusterResourceHolder.getInstance().getConfigSynchronizer().clusterBindingRemoved(binding);
                break;
        }
    }

    @Override
    public void handleLocalBindingsChanged(AndesBinding binding, BindingEvent changeType) throws AndesException {
        //notify cluster that bindings are changed
        if (AndesContext.getInstance().isClusteringEnabled()) {
            ClusterNotification clusterNotification = new ClusterNotification(binding.encodeAsString(),
                                                                              changeType.toString(),
                                                                              "Binding Notification Message : " +
                                                                                      changeType.toString());
            hazelcastAgent.notifyBindingsChanged(clusterNotification);
        }
        // if running in standalone mode short-circuit cluster notification
        // handleClusterBindingsChanged(binding,changeType);
    }

    @Override
    public void handleClusterSubscriptionsChanged(AndesSubscription subscription, SubscriptionChange changeType)
            throws AndesException {
        ClusterResourceHolder.getInstance().getSubscriptionManager().updateClusterSubscriptionMaps(subscription,
                                                                                                   changeType);
    }

    @Override
    public void handleLocalSubscriptionsChanged(LocalSubscription subscription, SubscriptionChange changeType)
            throws AndesException {
        //notify cluster that subscriptions are changed
        if (AndesContext.getInstance().isClusteringEnabled()) {
            ClusterNotification clusterNotification = new ClusterNotification(subscription.encodeAsStr(),
                                                                              changeType.toString(),
                                                                              "Subscription Notification Message : "
                                                                                      + changeType.toString());
            //check hazelcast instance active because hazelcast bundle get deactivated before notification send
            if (hazelcastAgent.isActive()) {
                hazelcastAgent.notifySubscriptionsChanged(clusterNotification);
            }
        } else { // if running in standalone mode short-circuit cluster notification
            handleClusterSubscriptionsChanged(new BasicSubscription(subscription.encodeAsStr()), changeType);
        }
    }
}
