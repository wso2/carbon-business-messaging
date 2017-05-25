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
package org.wso2.carbon.business.messaging.identity.impl;

import org.wso2.carbon.business.messaging.identity.AuthorizationService;
import org.wso2.carbon.business.messaging.identity.AuthorizationStore;
import org.wso2.carbon.business.messaging.identity.connector.config.AuthorizationStoreConnectorConfig;
import org.wso2.carbon.business.messaging.identity.exception.AuthorizationStoreException;

import java.util.Map;

/**
 * Initialise authorization store
 */
public class AuthorizationServiceImpl implements AuthorizationService {

    private AuthorizationStore authorizationStore = new AuthorizationStoreImpl();

    public AuthorizationServiceImpl(Map<String, AuthorizationStoreConnectorConfig> connectorConfig)
            throws
            AuthorizationStoreException {

        authorizationStore.init(this, connectorConfig);
    }

    @Override
    public AuthorizationStore getAuthorizationStore() {
        return authorizationStore;
    }
}
