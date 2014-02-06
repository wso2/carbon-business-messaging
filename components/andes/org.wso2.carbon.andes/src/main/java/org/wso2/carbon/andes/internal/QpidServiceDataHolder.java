/*
 *  Copyright (c) 2008, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.wso2.carbon.andes.internal;

import org.wso2.carbon.base.api.ServerConfigurationService;
import org.wso2.carbon.cassandra.server.service.CassandraServerService;
import org.wso2.carbon.coordination.server.service.CoordinationServerService;
import org.wso2.carbon.event.core.EventBundleNotificationService;

/**
 * This is a singleton class that holds data that is shared within this component.
 */
public class QpidServiceDataHolder {

    private static QpidServiceDataHolder instance = new QpidServiceDataHolder();

    private String accessKey = null;
    private ServerConfigurationService carbonConfiguration = null;
    private EventBundleNotificationService eventBundleNotificationService;
    private CassandraServerService cassandraServerService = null;
    private CoordinationServerService coordinationServerService;

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


    public CassandraServerService getCassandraServerService() {
        return cassandraServerService;
    }

    public void registerCassandraServerService(CassandraServerService cassandraServerService) {
        this.cassandraServerService = cassandraServerService;
    }

    public CoordinationServerService getCoordinationServerService() {
        return coordinationServerService;
    }

    public void setCoordinationServerService(CoordinationServerService coordinationServerService) {
        this.coordinationServerService = coordinationServerService;
    }
}
