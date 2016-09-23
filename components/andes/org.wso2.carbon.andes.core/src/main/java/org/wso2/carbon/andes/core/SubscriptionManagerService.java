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

import org.wso2.carbon.andes.core.types.Subscription;

import java.util.List;
import javax.management.MBeanException;

public interface SubscriptionManagerService {

    public List<Subscription> getAllSubscriptions() throws SubscriptionManagerException;

    /**
     * Retrieve all matching subscriptions from andes for the given criteria.
     *
     * @param isDurable retrieve durable subscriptions (can be true/false)
     * @param isActive retrieve active subscriptions (can be true/false/*, * meaning any)
     * @param protocolType the protocol type of the subscriptions to retrieve
     * @param destinationType the destination type of the subscriptions to retrieve
     *
     * @return the list of subscriptions matching the given criteria
     * @throws SubscriptionManagerException
     */
    public List<Subscription> getSubscriptions(String isDurable, String isActive, String protocolType,
                                               String destinationType) throws SubscriptionManagerException;


    /**
     * Return pending message count for the given subscription
     *
     * @param subscriptionId of the subscription
     * @param isDurable of type String (acceptable values => * | true | false)
     * @param isActive of type String (acceptable values => * | true | false)
     * @param protocolType The protocol type of the subscriptions
     * @param destinationType The destination type of the subscriptions
     * @return pending message count for the given subscription
     * @throws MBeanException
     */
    public long getPendingMessageCount(String subscriptionId, String isDurable, String isActive, String protocolType,
                                       String destinationType) throws SubscriptionManagerException;

    /**
     * Retrieve all matching subscriptions from andes for the given search criteria.
     *
     * @param isDurable                         retrieve durable subscriptions (can be true/false)
     * @param isActive                          retrieve active subscriptions (can be true/false/*, * meaning any)
     * @param protocolType                      the protocol type of the subscriptions to retrieve
     * @param destinationType                   the destination type of the subscriptions to retrieve
     * @param filteredNamePattern               queue or topic name pattern to search the subscriptions (* for all)
     * @param identifierPattern                 identifier pattern to search the subscriptions (* for all)
     * @param ownNodeId                         node Id of the node which own the subscriptions
     * @param pageNumber                        page number in the pagination table
     * @param subscriptionCountPerPage          number of subscriptions to be shown in the UI per page
     * @param isFilteredNameByExactMatch        exactly match the name or not
     * @param isIdentifierPatternByExactMatch   exactly match the identifier or not
     * @return list of subscriptions matching to the search criteria
     * @throws SubscriptionManagerException throws when an error occurs
     */
    public List<Subscription> getFilteredSubscriptions(String isDurable, String isActive, String protocolType,
            String destinationType, String filteredNamePattern, String identifierPattern, String ownNodeId,
            int pageNumber, int subscriptionCountPerPage, boolean isFilteredNameByExactMatch,
		    boolean isIdentifierPatternByExactMatch) throws SubscriptionManagerException;

    /**
     * Returns the total subscription count matching to a particular search criteria.
     *
     * @param isDurable                         retrieve durable subscriptions (can be true/false)
     * @param isActive                          retrieve active subscriptions (can be true/false/*, * meaning any)
     * @param protocolType                      the protocol type of the subscriptions to retrieve
     * @param destinationType                   the destination type of the subscriptions to retrieve
     * @param filteredNamePattern               queue or topic name pattern to search the subscriptions (* for all)
     * @param identifierPattern                 identifier pattern to search the subscriptions (* for all)
     * @param ownNodeId                         node Id of the node which own the subscriptions
     * @param isFilteredNameByExactMatch        exactly match the name or not
     * @param isIdentifierPatternByExactMatch   exactly match the identifier or not
     * @return total subscription count matching to the given criteria
     * @throws SubscriptionManagerException throws when an error occurs
     */
    public int getTotalSubscriptionCountForSearchResult(String isDurable, String isActive, String protocolType,
            String destinationType, String filteredNamePattern, String identifierPattern, String ownNodeId,
		    boolean isFilteredNameByExactMatch, boolean isIdentifierPatternByExactMatch) throws
            SubscriptionManagerException;

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
