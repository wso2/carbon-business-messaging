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

package org.wso2.carbon.business.messaging.core.internal;

import org.wso2.carbon.business.messaging.core.authentication.AuthenticationService;
import org.wso2.carbon.business.messaging.core.listeners.BrokerLifecycleListener;

import java.util.ArrayList;
import java.util.List;

/**
 * This is a singleton class that holds data that is shared within this component.
 */
public class BrokerServiceDataHolder {

    /**
     * Holds the context information related to the broker service
     */
    private static BrokerServiceDataHolder instance = new BrokerServiceDataHolder();

    /**
     * Allows accessing the
     */
    private String accessKey = null;

    /**
     * List of external services which will be called when the broker shuts down
     */
    private List<BrokerLifecycleListener> brokerLifecycleListeners = new ArrayList<>();
    /**
     * Holds the implementation of authenticationService interface
     */
    private AuthenticationService authenticationService;

    private BrokerServiceDataHolder() {
    }

    public static BrokerServiceDataHolder getInstance() {
        return instance;
    }

    /**
     * Store access key received from the authentication service when the bundle activates
     *
     * @param accessKey key which will be used for authentication
     */
    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    /**
     * Get stored access key that was received when the bundle started up
     *
     * @return key which will be used for authentication
     */
    public String getAccessKey() {
        return accessKey;
    }

    /**
     * Get list of external andes server shutdown listeners
     *
     * @return List of broker shutdown listeners
     */
    public List<BrokerLifecycleListener> getBrokerLifecycleListeners() {
        return brokerLifecycleListeners;
    }

    /**
     * Set the list of external andes server shutdown listeners
     *
     * @param brokerLifecycleListeners set of listeners which will be triggered during broker shutdown
     */
    public void setBrokerLifecycleListeners(List<BrokerLifecycleListener> brokerLifecycleListeners) {
        this.brokerLifecycleListeners = brokerLifecycleListeners;
    }

    /**
     * Set the authenticationService implementation
     *
     * @param authenticationService service implementation of AuthenticationService interface
     */
    public void setAuthenticationService(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    /**
     * Get the registered authenticationService implementation
     *
     * @return
     */
    public AuthenticationService getAuthenticationService() {
        return authenticationService;
    }
}
