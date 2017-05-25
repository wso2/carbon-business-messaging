/*
 * Copyright (c) 2017, WSO2 Inc. (http://wso2.com) All Rights Reserved.
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

package org.wso2.carbon.business.messaging.admin.services.managers.bean.impl;

import org.apache.commons.lang.NotImplementedException;
import org.wso2.carbon.business.messaging.admin.services.beans.SubscriptionManagementBeans;
import org.wso2.carbon.business.messaging.admin.services.exceptions.SubscriptionManagerException;
import org.wso2.carbon.business.messaging.admin.services.managers.SubscriptionManagerService;
import org.wso2.carbon.business.messaging.admin.services.types.Subscription;

import java.util.List;

/**
 * This implementation provides the base for managing all subscriptions related services through JMX.
 */
public class SubscriptionManagerServiceBeanImpl implements SubscriptionManagerService {

    private SubscriptionManagementBeans subscriptionManagementBeans;

    public SubscriptionManagerServiceBeanImpl() {
        subscriptionManagementBeans = new SubscriptionManagementBeans();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Subscription> getSubscriptions(String protocol, String subscriptionType, String subscriptionName,
            String destinationName, String active, int offset, int limit) throws SubscriptionManagerException {
        return subscriptionManagementBeans
                .getSubscriptions(protocol, subscriptionType, subscriptionName, destinationName, active, offset, limit);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void closeSubscriptions(String protocol, String subscriptionType, String destinationName,
            boolean unsubscribeOnly) throws SubscriptionManagerException {
        if (unsubscribeOnly) {
            throw new NotImplementedException();
        } else {
            subscriptionManagementBeans.closeSubscriptions(protocol, subscriptionType, destinationName);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void closeSubscription(String protocol, String subscriptionType, String subscriptionID,
            boolean unsubscribeOnly) throws SubscriptionManagerException {
        if (unsubscribeOnly) {
            throw new NotImplementedException();
        } else {
            subscriptionManagementBeans.closeSubscription(protocol, subscriptionType, subscriptionID);
        }
    }
}
