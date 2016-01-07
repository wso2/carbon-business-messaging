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
