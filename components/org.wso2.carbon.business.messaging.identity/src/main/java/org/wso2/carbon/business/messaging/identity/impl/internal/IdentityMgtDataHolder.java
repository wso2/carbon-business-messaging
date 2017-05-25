/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.wso2.carbon.business.messaging.identity.impl.internal;

import org.wso2.carbon.business.messaging.identity.AuthorizationService;
import org.wso2.carbon.business.messaging.identity.connector.AuthorizationStoreConnectorFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Data holder for auth/credential store connector factories
 */
public class IdentityMgtDataHolder {

    private static IdentityMgtDataHolder instance = new IdentityMgtDataHolder();

    private AuthorizationService authorizationService;

    private Map<String, AuthorizationStoreConnectorFactory> authorizationStoreConnectorFactoryMap = new HashMap<>();

    private IdentityMgtDataHolder() {

    }

    /**
     * @param authorizationService
     */
    void registerAuthorizationService(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    /**
     * @return
     */
    public AuthorizationService getAuthorizationService() {

        if (authorizationService == null) {
            throw new IllegalStateException("Carbon Authorization Service is null.");
        }
        return authorizationService;
    }

    public static IdentityMgtDataHolder getInstance() {
        return instance;
    }

    /**
     * Register AuthorizationStoreConnectorFactory
     *
     * @param key
     * @param authorizationStoreConnectorFactory
     */
    void registerAuthorizationStoreConnectorFactory(String key, AuthorizationStoreConnectorFactory
            authorizationStoreConnectorFactory) {

        authorizationStoreConnectorFactoryMap.put(key, authorizationStoreConnectorFactory);
    }

    /**
     * Get the map of registered authorizationStoreConnectorFactories
     *
     * @return authorizationStoreConnectorFactoryMap
     */

    public Map<String, AuthorizationStoreConnectorFactory> getAuthorizationStoreConnectorFactoryMap() {

        return authorizationStoreConnectorFactoryMap;
    }

    /**
     * Unregister given authorizationStoreConnectorFactory
     *
     * @param authorizationStoreConnectorFactory
     */
    public void unregisterAuthorizationStoreConnectorFactory(AuthorizationStoreConnectorFactory
                                                                     authorizationStoreConnectorFactory) {

        if (authorizationStoreConnectorFactory != null && !authorizationStoreConnectorFactoryMap.isEmpty()) {
            Optional<String> connectorId = authorizationStoreConnectorFactoryMap.entrySet().stream()
                    .filter(t -> t.getValue().equals(authorizationStoreConnectorFactory))
                    .map(Map.Entry::getKey)
                    .findFirst();
            if (connectorId.isPresent()) {
                authorizationStoreConnectorFactoryMap.remove(connectorId.get());
            }
        }
    }

}

