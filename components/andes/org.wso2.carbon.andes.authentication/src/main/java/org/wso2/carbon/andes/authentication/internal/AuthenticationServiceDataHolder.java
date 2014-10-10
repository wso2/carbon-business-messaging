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

package org.wso2.carbon.andes.authentication.internal;

import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.user.core.service.RealmService;

/**
 * This singleton class holds common properties used inside the authentication bundle.
 */
public class AuthenticationServiceDataHolder {

    private static AuthenticationServiceDataHolder instance = new AuthenticationServiceDataHolder();
    
    private RegistryService registryService = null;
    private RealmService realmService = null;
    private String accessKey = null;

    private AuthenticationServiceDataHolder() {
    }

    public static AuthenticationServiceDataHolder getInstance() {
        return instance;
    }

    /**
        * Sets RegistryService instance received before the bundle started up
        * 
        * @param registryService
        *               RegistryService instance 
        */
    public void setRegistryService(RegistryService registryService) {
        this.registryService = registryService;
    }

    /**
        * Get stored RegistryService instance
        *
        * @return
        *            RegistryService instance
        */
    public RegistryService getRegistryService() {
        return registryService;
    }

    /**
        * Sets access key generated at the time of bundle activation
        *
        * @param accessKey
        *               Generated access key (a UUID)  
        */
    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    /**
        * Get stored access key
        *
        * @return
        *               Access key value 
        */
    public String getAccessKey() {
        return accessKey;
    }

    /**
        * Get stored RealmService instance
        *
        * @return
        *               RealmService instance
        */
    public RealmService getRealmService() {
        return realmService;
    }

    /**
        * Set RealmService instance
        *
        * @param realmService
        *               RealmService instance
        */
    public void setRealmService(RealmService realmService) {
        this.realmService = realmService;
    }
}
