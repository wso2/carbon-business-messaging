/*
 * Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 */

package org.wso2.carbon.andes.core;

import org.wso2.carbon.andes.core.internal.registry.SubscriptionManagementBeans;
import org.wso2.carbon.andes.core.internal.util.Utils;
import org.wso2.carbon.andes.core.types.Subscription;

import java.util.List;

public class SubscriptionManagerServiceImpl implements SubscriptionManagerService {

    @Deprecated
    //kept temporarily for back tracking purposes TODO hasithad remove after verifying
    public List<Subscription> getAllSubscriptions() throws SubscriptionManagerException {
        List<Subscription> allSubscriptions = SubscriptionManagementBeans.getInstance().getAllSubscriptions();
        //show queues belonging to current domain of user
        //also set queue name used by user
        return Utils.filterDomainSpecificSubscribers(allSubscriptions);
    }

    /**
     * {@inheritDoc}
     */
    public List<Subscription> getSubscriptions(String isDurable, String isActive, String protocolType,
                                               String destinationType) throws SubscriptionManagerException {

        List<Subscription> subscriptions = SubscriptionManagementBeans.getInstance().getSubscriptions
                (isDurable, isActive, protocolType, destinationType);

        return Utils.filterDomainSpecificSubscribers(subscriptions);
    }

    @Override
    public long getPendingMessageCount(String subscriptionId, String isDurable, String isActive, String protocolType,
                                       String destinationType) throws SubscriptionManagerException {
        return  SubscriptionManagementBeans.getInstance().getPendingMessageCount( subscriptionId, isDurable, isActive,
                protocolType, destinationType);
    }

    /**
     * {@inheritDoc}
     */
    public List<Subscription> getFilteredSubscriptions(String isDurable, String isActive, String protocolType,
            String destinationType, String filteredNamePattern, String identifierPattern, String ownNodeId,
            int pageNumber, int subscriptionCountPerPage, boolean isFilteredNameByExactMatch,
            boolean isIdentifierPatternByExactMatch) throws SubscriptionManagerException {

        List<Subscription> subscriptions = SubscriptionManagementBeans.getInstance().getFilteredSubscriptions
                (isDurable, isActive, protocolType, destinationType, filteredNamePattern, identifierPattern,
                        ownNodeId, pageNumber, subscriptionCountPerPage, isFilteredNameByExactMatch, isIdentifierPatternByExactMatch);

        return Utils.filterDomainSpecificSubscribers(subscriptions);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getTotalSubscriptionCountForSearchResult(String isDurable, String isActive, String protocolType,
            String destinationType, String filteredNamePattern, String identifierPattern, String ownNodeId,
            boolean isFilteredNameByExactMatch, boolean isIdentifierPatternByExactMatch)
            throws SubscriptionManagerException {

        int subscriptionCountForSearchResult = SubscriptionManagementBeans.getInstance()
                .getTotalSubscriptionCountForSearchResult(isDurable, isActive, protocolType, destinationType,
                        filteredNamePattern, identifierPattern, ownNodeId, isFilteredNameByExactMatch, isIdentifierPatternByExactMatch);

        return subscriptionCountForSearchResult;
    }

    /**
	 * Close subscription by subscriptionID. This method will break the connection
	 *
	 * between server and particular subscription
	 * @param subscriptionID ID of the subscription to close
	 * @param destination queue/topic name of subscribed destination
     * @param protocolType The protocol type of the subscriptions to close
     * @param destinationType The destination type of the subscriptions to close
	 * @throws SubscriptionManagerException
	 */
	public void closeSubscription(String subscriptionID, String destination, String protocolType,
                                  String destinationType) throws SubscriptionManagerException {
		SubscriptionManagementBeans.getInstance().closeSubscription(subscriptionID, destination, protocolType,
                destinationType);
	}
}
