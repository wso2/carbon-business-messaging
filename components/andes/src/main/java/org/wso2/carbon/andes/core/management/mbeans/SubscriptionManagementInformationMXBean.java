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

package org.wso2.carbon.andes.core.management.mbeans;

import javax.management.MBeanException;
import javax.management.openmbean.CompositeData;

/**
 * Interface for managing subscriptions.
 */
public interface SubscriptionManagementInformationMXBean {

    /**
     * Gets subscriptions belonging to a specific protocol type and destination type. The subscriptions can be filtered
     * by subscription name, destination name and whether they are active or not.
     *
     * @param protocolTypeAsString    The protocol type matching for the subscription.
     * @param destinationTypeAsString The destination type matching for the subscription.
     * @param subscriptionName        The name of the subscription. If "*", all subscriptions are included. Else
     *                                subscriptions that <strong>contains</strong> the value are included.
     * @param destinationName         The name of the destination name. If "*", all destinations are included. Else
     *                                destinations that <strong>equals</strong> the value are included.
     * @param active                  Filtering the subscriptions that are active or inactive.
     * @param offset                  The starting index to return.
     * @param limit                   The number of subscriptions to return.
     * @return An array of {@link CompositeData} representing subscriptions.
     * @throws MBeanException
     */
    CompositeData[] getSubscriptions(String protocolTypeAsString, String destinationTypeAsString, String
            subscriptionName, String destinationName, boolean active, int offset, int limit) throws MBeanException;

    /**
     * Close/unsubscribe subscriptions forcefully belonging to a specific protocol type, destination type.
     *
     * @param protocolTypeAsString     The protocol type matching for the subscription.
     * @param subscriptionTypeAsString The subscription type matching for the subscription.
     * @param destinationName          The name of the destination to close/unsubscribe. If "*", all destinations are
     *                                 included. Else destinations that <strong>contains</strong> the value are
     *                                 included.
     * @throws MBeanException
     */
    void removeSubscriptions(String protocolTypeAsString, String subscriptionTypeAsString, String destinationName)
            throws MBeanException;

    /**
     * Close/Remove/Unsubscribe subscriptions forcefully belonging to a specific protocol type, destination type.
     *
     * @param protocolTypeAsString     The protocol type matching for the subscription.
     * @param subscriptionTypeAsString The subscription type matching for the subscription.
     * @param destinationName          The name of the destination to close/unsubscribe. If "*", all destinations are
     *                                 included. Else destinations that <strong>equals</strong> the value are included.
     * @throws MBeanException
     */
    void removeSubscription(String protocolTypeAsString, String subscriptionTypeAsString, String destinationName,
                            String subscriptionId) throws MBeanException;
}
