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
import org.wso2.carbon.andes.core.AndesException;
import org.wso2.carbon.andes.core.AndesSubscription;
import org.wso2.carbon.andes.core.DestinationType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Subscription store for queue
 */
public class QueueSubscriptionStore implements AndesSubscriptionStore {

    /**
     * All local queue subscriptions will be stored in this.
     */
    private Map<String, Set<AndesSubscription>> queueSubscriptionMap = new ConcurrentHashMap<>();

    /**
     * {@inheritDoc}
     */
    @Override
    public void addSubscription(AndesSubscription subscription) throws AndesException {
        String destination = subscription.getSubscribedDestination();

        Set<AndesSubscription> subscriptionSet = queueSubscriptionMap.get(destination);

        if (null == subscriptionSet) {
            subscriptionSet = Collections.newSetFromMap(new ConcurrentHashMap<AndesSubscription, Boolean>());
            subscriptionSet.add(subscription);

            queueSubscriptionMap.put(destination, subscriptionSet);
        } else {
            // If already available then update it
            if (subscriptionSet.contains(subscription)) {
                updateSubscription(subscription);
            } else {
                subscriptionSet.add(subscription);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateSubscription(AndesSubscription subscription) {
        Set<AndesSubscription> subscriptionSet = queueSubscriptionMap.get(subscription.getSubscribedDestination());

        if (null != subscriptionSet) {
            subscriptionSet.remove(subscription);
            subscriptionSet.add(subscription);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSubscriptionAvailable(AndesSubscription subscription) {
        boolean subscriptionAvailable = false;

        for (Map.Entry<String, Set<AndesSubscription>> entry : queueSubscriptionMap.entrySet()) {
            if (entry.getValue().contains(subscription)) {
                subscriptionAvailable = true;
                break;
            }
        }

        return subscriptionAvailable;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeSubscription(AndesSubscription subscription) {
        Set<AndesSubscription> subscriptionSet = queueSubscriptionMap.get(subscription.getSubscribedDestination());

        if (null != subscriptionSet) {
            subscriptionSet.remove(subscription);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<AndesSubscription> getMatchingSubscriptions(String destination, DestinationType destinationType) {
        Set<AndesSubscription> matchingSubscriptions = queueSubscriptionMap.get(destination);

        if (null == matchingSubscriptions) {
            matchingSubscriptions = Collections.emptySet();
        }
        return matchingSubscriptions;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<AndesSubscription> getAllSubscriptions() {
        List<AndesSubscription> subscriptionList = new ArrayList<>();

        for (Map.Entry<String, Set<AndesSubscription>> entry : queueSubscriptionMap.entrySet()) {
            subscriptionList.addAll(entry.getValue());
        }

        return subscriptionList;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> getAllDestinations(DestinationType destinationType) {
        return queueSubscriptionMap.keySet();
    }

}
