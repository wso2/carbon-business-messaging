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

import org.wso2.carbon.andes.core.types.MQTTSubscription;
import org.wso2.carbon.andes.core.types.Subscription;

import java.util.List;

public interface SubscriptionManagerService {

    public List<Subscription> getAllSubscriptions() throws SubscriptionManagerException;

    /**
     * Retrieve all matching subscriptions from andes for the given criteria.
     *
     * @param isDurable Retrieve durable subscriptions (can be true/false)
     * @param isActive Retrieve active subscriptions (can be true/false/*, * meaning any)
     * @param protocolType The protocol type of the subscriptions to retrieve
     * @param destinationType The destination type of the subscriptions to retrieve
     *
     * @return The list of subscriptions matching the given criteria
     * @throws SubscriptionManagerException
     */
    public List<Subscription> getSubscriptions(String isDurable, String isActive, String protocolType,
                                               String destinationType) throws SubscriptionManagerException;

	/**
	 * Get the pending message count for the specified subscription.
	 *
	 * @param queueName for which the pending message count need to be calculated
	 * @return The pending message count for that subscription
	 * @throws SubscriptionManagerException
     */
	public long getPendingMessageCount(String queueName) throws SubscriptionManagerException;

	/**
	 * Retrieve all matching subscriptions from andes for the given search criteria.
	 *
	 * @param isDurable  are the subscriptions to be retrieve durable (true/ false)
	 * @param isActive   are the subscriptions to be retrieved active (true/false)
	 * @param protocolType  the protocol type of the subscriptions to be retrieved
	 * @param destinationType the destination type of the subscriptions to be retrieved
	 * @param filteredNamePattern queue or topic name pattern to search the subscriptions ("" for all)
	 * @param isFilteredNameByExactMatch exactly match the name or not
	 * @param identifierPattern  identifier pattern to search the subscriptions ("" for all)
	 * @param isIdentifierPatternByExactMatch  exactly match the identifier or not
	 * @param ownNodeId node Id of the node which own the subscriptions
	 * @param pageNumber  page number in the pagination table
	 * @param subscriptionCountPerPage number of subscriptions to be shown in the UI per page
	 * @return list of subscriptions matching to the search criteria
	 * @throws SubscriptionManagerException throws when an error occurs
	 */
	public List<Subscription> getFilteredSubscriptions(boolean isDurable, boolean isActive, String protocolType,
			String destinationType, String filteredNamePattern, boolean isFilteredNameByExactMatch,
			String identifierPattern, boolean isIdentifierPatternByExactMatch, String ownNodeId, int pageNumber,
			int subscriptionCountPerPage) throws SubscriptionManagerException;

	/**
	 * Retrieve all matching subscriptions from andes for the given search criteria.
	 *
	 * @param subscription  are the subscriptions to be retrieve durable (true/ false)
	 * @param tenantDomain  are the subscriptions to be retrieve durable (true/ false)
	 * @return list of subscriptions matching to the search criteria
	 * @throws SubscriptionManagerException throws when an error occurs
	 */
	public List<Subscription> getFilteredMQTTSubscriptions(MQTTSubscription subscription, String tenantDomain)
			throws SubscriptionManagerException;


	/**
	 * Returns the total subscription count matching to a particular search criteria.
	 *
	 * @param isDurable are the subscriptions to be retrieve durable (true/ false)
	 * @param isActive are the subscriptions to be retrieved active (true/false)
	 * @param protocolType the protocol type of the subscriptions to be retrieved
	 * @param destinationType the destination type of the subscriptions to be retrieved
	 * @param filteredNamePattern queue or topic name pattern to search the subscriptions ("" for all)
	 * @param isFilteredNameByExactMatch exactly match the name or not
	 * @param identifierPattern identifier pattern to search the subscriptions ("" for all)
	 * @param isIdentifierPatternByExactMatch exactly match the identifier or not
	 * @param ownNodeId node Id of the node which own the subscriptions
	 * @return total subscription count matching to the given criteria
	 * @throws SubscriptionManagerException throws when an error occurs
	 */
	public int getTotalSubscriptionCountForSearchResult(boolean isDurable, boolean isActive, String protocolType,
			String destinationType, String filteredNamePattern, boolean isFilteredNameByExactMatch,
			String identifierPattern, boolean isIdentifierPatternByExactMatch, String ownNodeId)
			throws SubscriptionManagerException;

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
                                  String destinationType) throws SubscriptionManagerException;
}
