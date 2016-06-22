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

package org.wso2.carbon.andes.core.management;


import org.wso2.carbon.andes.core.Andes;
import org.wso2.carbon.andes.core.AndesException;
import org.wso2.carbon.andes.core.AndesSubscription;
import org.wso2.carbon.andes.core.DestinationType;
import org.wso2.carbon.andes.core.MessagingEngine;
import org.wso2.carbon.andes.core.ProtocolType;
import org.wso2.carbon.andes.core.management.mbeans.SubscriptionManagementInformationMXBean;
import org.wso2.carbon.andes.core.resource.manager.AndesResourceManager;
import org.wso2.carbon.andes.core.util.CompositeDataHelper;

import java.util.ArrayList;
import java.util.List;
import javax.management.MBeanException;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.OpenDataException;

/**
 * MBeans for managing resources related to subscriptions.
 */
public class SubscriptionManagementInformationImpl implements SubscriptionManagementInformationMXBean {

    /**
     * Andes resource manager instance.
     */
    AndesResourceManager andesResourceManager;

    /**
     * Helper class for converting a subscription for {@link CompositeData}.
     */
    private CompositeDataHelper.SubscriptionCompositeDataHelper subscriptionCompositeDataHelper;

    /**
     * Instantiates the MBeans related to subscriptions.
     */
    public SubscriptionManagementInformationImpl() {
        subscriptionCompositeDataHelper = new CompositeDataHelper().new SubscriptionCompositeDataHelper();
        andesResourceManager = Andes.getInstance().getAndesResourceManager();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompositeData[] getSubscriptions(String protocolTypeAsString, String destinationTypeAsString, String
            subscriptionName, String destinationName, boolean active, int offset, int limit) throws MBeanException {
        List<CompositeData> compositeDataList = new ArrayList<>();

        try {
            ProtocolType protocolType = new ProtocolType(protocolTypeAsString);
            DestinationType destinationType = DestinationType.valueOf(destinationTypeAsString.toUpperCase());

            List<AndesSubscription> andesSubscriptions = andesResourceManager.getSubscriptions(protocolType,
                                                                                               destinationType,
                                                                                               subscriptionName,
                                                                                               destinationName, active,
                                                                                               offset, limit);

            for (AndesSubscription subscription : andesSubscriptions) {
                Long pendingMessageCount =
                        MessagingEngine.getInstance().getMessageCountOfQueue(subscription.getStorageQueueName());
                compositeDataList.add(subscriptionCompositeDataHelper.getSubscriptionAsCompositeData(
                        subscription, pendingMessageCount));

            }
        } catch (AndesException | OpenDataException e) {
            throw new MBeanException(e, "Error getting subscriptions");
        }
        return compositeDataList.toArray(new CompositeData[compositeDataList.size()]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeSubscriptions(String protocolTypeAsString, String subscriptionTypeAsString, String
            destinationName) throws MBeanException {
        try {
            ProtocolType protocolType = new ProtocolType(protocolTypeAsString);
            DestinationType subscriptionType = DestinationType.valueOf(subscriptionTypeAsString.toUpperCase());
            andesResourceManager.removeSubscriptions(protocolType, subscriptionType, destinationName);
        } catch (AndesException e) {
            throw new MBeanException(e, "Error in removing subscriptions");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeSubscription(String protocolTypeAsString, String subscriptionTypeAsString, String
            destinationName, String subscriptionId) throws MBeanException {
        try {
            ProtocolType protocolType = new ProtocolType(protocolTypeAsString);
            DestinationType subscriptionType = DestinationType.valueOf(subscriptionTypeAsString.toUpperCase());
            andesResourceManager.removeSubscription(protocolType, subscriptionType, destinationName, subscriptionId);
        } catch (AndesException e) {
            throw new MBeanException(e, "Error in removing a subscription.");
        }
    }
}
