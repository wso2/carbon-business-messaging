/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 * 
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.andes.authentication.andes;

import org.apache.log4j.Logger;
import org.dna.mqtt.moquette.server.IAuthenticator;
import org.wso2.carbon.andes.authentication.internal.AuthenticationServiceDataHolder;
import org.wso2.carbon.user.api.TenantManager;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.api.UserStoreManager;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

/**
 * Authenticates connecting users against Carbon user store. Intended usage is
 * via providing fully qualified class name
 * in broker.xml
 */
public class CarbonBasedMQTTAuthenticator implements IAuthenticator {

    private static final Logger logger = Logger.getLogger(CarbonBasedMQTTAuthenticator.class);

    /**
     * {@inheritDoc} Authenticates the user against carbon user store.
     */
    @Override
    public boolean checkValid(String username, String password) {

        boolean isAuthenticated = false;

        try {
            int tenantId = getTenantIdOfUser(username);

            if (MultitenantConstants.INVALID_TENANT_ID != tenantId) {
                UserRealm userRealm =
                                      AuthenticationServiceDataHolder.getInstance().getRealmService()
                                                                     .getTenantUserRealm(tenantId);
                UserStoreManager userStoreManager = userRealm.getUserStoreManager();
                isAuthenticated =
                                  userStoreManager.authenticate(MultitenantUtils.getTenantAwareUsername(username),
                                                                password);
            } else {
                logger.error(String.format("access denied, unable to find a tenant for user name : %s", username));
            }

        } catch (UserStoreException e) {
            String errorMsg = String.format("unable to authenticate user : %s", username);
            logger.error(errorMsg, e);
        }

        return isAuthenticated;
    }

    /**
     * Returns tenent Id given the user name or returns
     * {@link MultitenantConstants#INVALID_TENANT_ID} if none can be found.
     * 
     * @param username
     * @return
     * @throws UserStoreException
     */
    private int getTenantIdOfUser(String username) throws UserStoreException {
        int tenantId = MultitenantConstants.INVALID_TENANT_ID;
        String domainName = MultitenantUtils.getTenantDomain(username);
        if (domainName != null) {
            TenantManager tenantManager =
                                          AuthenticationServiceDataHolder.getInstance().getRealmService()
                                                                         .getTenantManager();
            tenantId = tenantManager.getTenantId(domainName);
        }
        return tenantId;
    }

}