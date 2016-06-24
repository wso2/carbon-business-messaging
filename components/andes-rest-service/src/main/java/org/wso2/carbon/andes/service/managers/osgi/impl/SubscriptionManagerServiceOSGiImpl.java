/*
 * Copyright (c) 2016, WSO2 Inc. (http://wso2.com) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.andes.service.managers.osgi.impl;

import org.apache.commons.lang.BooleanUtils;
import org.wso2.carbon.andes.core.AndesException;
import org.wso2.carbon.andes.core.AndesSubscription;
import org.wso2.carbon.andes.core.DestinationType;
import org.wso2.carbon.andes.core.ProtocolType;
import org.wso2.carbon.andes.core.resource.manager.AndesResourceManager;
import org.wso2.carbon.andes.service.exceptions.SubscriptionManagerException;
import org.wso2.carbon.andes.service.internal.AndesRESTComponentDataHolder;
import org.wso2.carbon.andes.service.managers.SubscriptionManagerService;
import org.wso2.carbon.andes.service.types.Subscription;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation for handling subscription related resource through OSGi.
 */
public class SubscriptionManagerServiceOSGiImpl implements SubscriptionManagerService {
    private AndesResourceManager andesResourceManager;
    public SubscriptionManagerServiceOSGiImpl() {
        andesResourceManager = AndesRESTComponentDataHolder.getInstance().getAndesInstance().getAndesResourceManager();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Subscription> getSubscriptions(String protocol, String subscriptionType, String subscriptionName,
                                               String destinationName, String active, int offset, int limit)
                                                                                throws SubscriptionManagerException {
        List<Subscription> subscriptions = new ArrayList<>();
        try {
            ProtocolType protocolType = new ProtocolType(protocol);
            DestinationType destinationType = DestinationType.valueOf(subscriptionType);
            List<AndesSubscription> andesSubscriptions = andesResourceManager.getSubscriptions(protocolType,
                    destinationType, subscriptionName, destinationName, active, offset, limit);
            for (AndesSubscription andesSubscription : andesSubscriptions) {
                subscriptions.add(getSubscriptionFromAndesSubscription(andesSubscription));
            }
        } catch (AndesException e) {
            throw new SubscriptionManagerException("Unable to get the list of subscriptions.", e);
        }
        return subscriptions;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void closeSubscriptions(String protocol, String subscriptionType, String destinationName,
                                   boolean unsubscribeOnly) throws SubscriptionManagerException {
        throw new SubscriptionManagerException("Not implemented");

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void closeSubscription(String protocol, String subscriptionType, String subscriptionID,
                                  boolean unsubscribeOnly) throws SubscriptionManagerException {
        throw new SubscriptionManagerException("Not implemented");
    }

    /**
     * Converts an {@link AndesSubscription} to a {@link Subscription}.
     *
     * @param andesSubscription The andes subscription.
     * @return Converted subscription.
     * @throws AndesException
     */
    private Subscription getSubscriptionFromAndesSubscription(AndesSubscription andesSubscription)
            throws AndesException {
        Subscription subscription = new Subscription();
        subscription.setSubscriptionIdentifier(andesSubscription.getSubscriptionID());
        subscription.setSubscribedQueueOrTopicName(andesSubscription.getSubscribedDestination());
        subscription.setSubscriberQueueBoundExchange(andesSubscription.getTargetQueueBoundExchangeName());
        subscription.setSubscriberQueueName(andesSubscription.getStorageQueueName());
        subscription.setDurable(andesSubscription.isDurable());
        subscription.setActive(andesSubscription.hasExternalSubscriptions());
        subscription.setNumberOfMessagesRemainingForSubscriber
                (andesResourceManager.getMessageCountForStorageQueue(andesSubscription.getStorageQueueName()));
        subscription.setSubscriberNodeAddress(andesSubscription.getSubscribedNode());
        subscription.setProtocolType(andesSubscription.getProtocolType().toString());
        subscription.setDestinationType(andesSubscription.getDestinationType().toString());
        return subscription;
    }
}
