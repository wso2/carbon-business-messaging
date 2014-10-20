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

package org.wso2.carbon.andes.authorization.internal;

import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.user.core.service.RealmService;

/**
 * This singleton class holds common properties shared inside the authorization bundle.
 */
public class AuthorizationServiceDataHolder {

    private static AuthorizationServiceDataHolder instance = new AuthorizationServiceDataHolder();

    private RegistryService registryService = null;
    private RealmService realmService = null;

    private AuthorizationServiceDataHolder() {
    }

    public static AuthorizationServiceDataHolder getInstance() {
        return instance;
    }

    /**
        * Set RegistryService instance received when the bundle starts up
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
        *               RegistryService instance 
        */
    public RegistryService getRegistryService() {
        return registryService;
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
