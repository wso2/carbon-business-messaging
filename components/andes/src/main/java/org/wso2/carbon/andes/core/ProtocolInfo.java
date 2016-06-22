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

package org.wso2.carbon.andes.core;

import org.wso2.carbon.andes.core.subscription.AndesSubscriptionStore;

import java.util.HashMap;
import java.util.Map;

/**
 * This class carries the information required for a new protocol to register in Andes core.
 */
public class ProtocolInfo {

    /**
     * The Protocol which needs to be registered in Andes.
     */
    private ProtocolType protocolType;

    /**
     * The subscription stores which are required to handle local subscriptions of this protocol.
     */
    private Map<DestinationType, AndesSubscriptionStore> localSubscriptionStores = new HashMap<>();

    /**
     * The subscription stores which are required to handle cluster subscriptions of this protocol.
     */
    private Map<DestinationType, AndesSubscriptionStore> clusterSubscriptionStores = new HashMap<>();

    // Add Recovery strategies .etc

    /**
     * Constructor which initialises the {@link ProtocolType}.
     *
     * @param protocolName The name of the protocol
     * @param version      The version of the protocol
     * @throws AndesException
     */
    public ProtocolInfo(String protocolName, String version) throws AndesException {
        protocolType = new ProtocolType(protocolName, version);
    }

    /**
     * Get the protocol type which this info class refers to.
     *
     * @return The protocl type of this info object.
     */
    public ProtocolType getProtocolType() {
        return protocolType;
    }

    /**
     * Get subscription stores which handles the local subscriptions of given destination type.
     *
     * @return Subscription stores against the destination type to be handled
     */
    public Map<DestinationType, AndesSubscriptionStore> getLocalSubscriptionStores() {
        return localSubscriptionStores;
    }

    /**
     * Get subscription stores which handles the cluster subscriptions of given destination type.
     *
     * @return Subscription stores against the destination type to be handled
     */
    public Map<DestinationType, AndesSubscriptionStore> getClusterSubscriptionStores() {
        return clusterSubscriptionStores;
    }

    /**
     * Add a subscription store to handle the local subscriptions of given destination type.
     *
     * @param destinationType   The destination type which is handled by the given subscription store
     * @param subscriptionStore The subscription store which handles the specified destination type for this protocol
     */
    public void addLocalSubscriptionStore(DestinationType destinationType, AndesSubscriptionStore subscriptionStore) {
        localSubscriptionStores.put(destinationType, subscriptionStore);
    }

    /**
     * Add a subscription store to handle the cluster subscriptions of given destination type.
     *
     * @param destinationType   The destination type which is handled by the given subscription store
     * @param subscriptionStore The subscription store which handles the specified destination type for this protocol
     */
    public void addClusterSubscriptionStore(DestinationType destinationType, AndesSubscriptionStore subscriptionStore) {
        clusterSubscriptionStores.put(destinationType, subscriptionStore);
    }
}
