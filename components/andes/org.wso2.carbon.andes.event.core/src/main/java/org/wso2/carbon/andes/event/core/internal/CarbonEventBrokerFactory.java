/*
 * Copyright 2004,2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.andes.event.core.internal;

import org.apache.axiom.om.OMElement;
import org.wso2.carbon.andes.event.core.EventBroker;
import org.wso2.carbon.andes.event.core.EventBrokerFactory;
import org.wso2.carbon.andes.event.core.TopicManagerService;
import org.wso2.carbon.andes.event.core.TopicManagerServiceFactory;
import org.wso2.carbon.andes.event.core.delivery.DeliveryManagerFactory;
import org.wso2.carbon.andes.event.core.exception.EventBrokerConfigurationException;
import org.wso2.carbon.andes.event.core.subscription.SubscriptionManagerFactory;
import org.wso2.carbon.andes.event.core.util.EventBrokerConstants;
import org.wso2.carbon.andes.event.core.internal.util.JavaUtil;

import javax.xml.namespace.QName;
import java.util.concurrent.*;

/**
 * Factory to create new carbon event brokers
 */
public class CarbonEventBrokerFactory implements EventBrokerFactory {

    public static final String EB_MIN_SPARE_THREADS = "minSpareThreads";
    public static final String EB_MAX_THREADS = "maxThreads";
    public static final String EB_MAX_QUEUED_REQUESTS = "maxQueuedRequests";
    public static final String EB_KEEP_ALIVE_TIME = "keepAliveTime";

    /**
     * {@inheritDoc}
     */
    public EventBroker getEventBroker(OMElement config) throws EventBrokerConfigurationException {

        CarbonEventBroker carbonEventBroker = new CarbonEventBroker();

        // setting the topic manager
        OMElement topicManagerElement =
                config.getFirstChildWithName(new QName(EventBrokerConstants.EB_CONF_NAMESPACE,
                                                       EventBrokerConstants.EB_CONF_ELE_TOPIC_MANAGER));
        TopicManagerServiceFactory topicManagerServiceFactory =
                (TopicManagerServiceFactory) JavaUtil.getObject(topicManagerElement);
        TopicManagerService topicManager = topicManagerServiceFactory.getTopicManagerService(topicManagerElement);
        carbonEventBroker.setTopicManagerService(topicManager);

        // setting the subscription manager
        OMElement subscriptionManager =
                config.getFirstChildWithName(new QName(EventBrokerConstants.EB_CONF_NAMESPACE,
                                                       EventBrokerConstants.EB_CONF_ELE_SUBSCRIPTION_MANAGER));
        SubscriptionManagerFactory subscriptionManagerFactory =
                (SubscriptionManagerFactory) JavaUtil.getObject(subscriptionManager);
        carbonEventBroker.setSubscriptionManager(
                subscriptionManagerFactory.getSubscriptionManager(subscriptionManager));

        // setting the delivery manager
        OMElement delivaryManager =
                config.getFirstChildWithName(new QName(EventBrokerConstants.EB_CONF_NAMESPACE,
                                                       EventBrokerConstants.EB_CONF_ELE_DELIVERY_MANAGER));
        DeliveryManagerFactory delivaryManagerfactory =
                (DeliveryManagerFactory) JavaUtil.getObject(delivaryManager);
        carbonEventBroker.setDeliveryManager(
                delivaryManagerfactory.getDeliveryManger(delivaryManager));

        // getting the event publisher properties and setting the executor
        OMElement eventPublisher = config.getFirstChildWithName(new QName(EventBrokerConstants.EB_CONF_NAMESPACE,
                                                                          EventBrokerConstants.EB_CONF_ELE_EVENT_PUBLISHER));
        int minSpareThreads = Integer.parseInt(JavaUtil.getValue(eventPublisher, EB_MIN_SPARE_THREADS));
        int maxThreads = Integer.parseInt(JavaUtil.getValue(eventPublisher, EB_MAX_THREADS));
        int maxQueuedRequests =
                Integer.parseInt(JavaUtil.getValue(eventPublisher, EB_MAX_QUEUED_REQUESTS));
        long keepAliveTime = Integer.parseInt(JavaUtil.getValue(eventPublisher, EB_KEEP_ALIVE_TIME));


        BlockingQueue<Runnable> queue = new ArrayBlockingQueue<Runnable>(maxQueuedRequests);

        ExecutorService executor = new ThreadPoolExecutor(minSpareThreads, maxThreads,
                                                          keepAliveTime, TimeUnit.MILLISECONDS, queue);
        carbonEventBroker.setExecutor(executor);

        carbonEventBroker.init();

        return carbonEventBroker;
    }
}
