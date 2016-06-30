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

package org.wso2.carbon.andes.internal;

import org.wso2.carbon.andes.listeners.BrokerLifecycleListener;
import org.wso2.carbon.base.api.ServerConfigurationService;
import org.wso2.carbon.andes.event.core.EventBundleNotificationService;
import org.wso2.carbon.server.admin.common.IServerAdmin;

import java.util.ArrayList;
import java.util.List;

/**
 * This is a singleton class that holds data that is shared within this component.
 */
public class QpidServiceDataHolder {

    private static QpidServiceDataHolder instance = new QpidServiceDataHolder();

    private String accessKey = null;
    private ServerConfigurationService carbonConfiguration = null;
    private EventBundleNotificationService eventBundleNotificationService;
    private List<BrokerLifecycleListener> brokerLifecycleListeners = new ArrayList<>();

    /**
     * This OSGi service is used in the situation where we need to shutdown from the carbon kernel.
     */
    private IServerAdmin service;

    /**
     * Get IServerAdmin that was stored when the bundle started up
     *
     * @return
     */
    public IServerAdmin getService() {
        return service;
    }

    /**
     * Store IServerAdmin; that was received when the bundle started up
     *
     * @param service
     */
    public void setService(IServerAdmin service) {
        this.service = service;
    }

    private QpidServiceDataHolder() {
    }

    public static QpidServiceDataHolder getInstance() {
        return instance;
    }

    /**
        * Store access key received from the authentication service when the bundle activates
        *
        * @param accessKey
        */
    public void setAccessKey(String accessKey) {
        this.accessKey =  accessKey;
    }

    /**
        * Get stored access key that was received when the bundle started up
        *
        * @return
        */
    public String getAccessKey() {
        return accessKey;
    }

    /**
        * Set Carbon ServerConfiguration instance
        *
        * @param carbonConfiguration
        */
    public void setCarbonConfiguration(ServerConfigurationService carbonConfiguration) {
        this.carbonConfiguration = carbonConfiguration;
    }

    /**
        * Get Carbon ServerConfiguration instance
        *
        * @return
        */
    public ServerConfigurationService getCarbonConfiguration() {
        return carbonConfiguration;
    }

    public void registerEventBundleNotificationService(
                      EventBundleNotificationService eventBundleNotificationService){
        this.eventBundleNotificationService = eventBundleNotificationService;
    }

    public EventBundleNotificationService getEventBundleNotificationService(){
        return this.eventBundleNotificationService;
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
     * @param brokerLifecycleListeners
     */
    public void setBrokerLifecycleListeners(List<BrokerLifecycleListener> brokerLifecycleListeners)
    {
        this.brokerLifecycleListeners = brokerLifecycleListeners;
    }
}
