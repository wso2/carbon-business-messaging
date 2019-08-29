/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dna.mqtt.moquette.server.IAuthenticator;
import org.dna.mqtt.moquette.server.AuthenticationInfo;
import org.wso2.carbon.andes.authentication.internal.AuthenticationServiceDataHolder;
import org.wso2.carbon.context.PrivilegedCarbonContext;
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

    private static final Log logger = LogFactory.getLog(CarbonBasedMQTTAuthenticator.class);
    private static final String DOMAIN_NAME_SEPARATOR = "!";

    /**
     * {@inheritDoc} Authenticates the user against carbon user store.
     */
    @Override
    public AuthenticationInfo checkValid(String username, String password) {

        boolean isAuthenticated = false;
        // Carbon kernel uses '@' to separate domain while MB uses '!'
        String carbonCompliantUsername = username.replace(DOMAIN_NAME_SEPARATOR, "@");
        AuthenticationInfo authenticationInfo = new AuthenticationInfo();
        try {
            PrivilegedCarbonContext.startTenantFlow();

            int tenantId = getTenantIdOfUser(carbonCompliantUsername);

            if (MultitenantConstants.INVALID_TENANT_ID != tenantId) {
                if (username.contains(DOMAIN_NAME_SEPARATOR)) {
                    String tenantDomain = username.substring(username.indexOf(DOMAIN_NAME_SEPARATOR) + 1);
                    PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenantDomain);
                    PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId(true);
                } else {
                    PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantId(MultitenantConstants.SUPER_TENANT_ID);
                    PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(MultitenantConstants
                                                                                            .SUPER_TENANT_DOMAIN_NAME);
                }

                UserRealm userRealm = AuthenticationServiceDataHolder.getInstance().getRealmService()
                                                                                        .getTenantUserRealm(tenantId);
                UserStoreManager userStoreManager = userRealm.getUserStoreManager();
                username = MultitenantUtils.getTenantAwareUsername(carbonCompliantUsername);
                isAuthenticated = userStoreManager.authenticate(username, password);
                authenticationInfo.setUsername(carbonCompliantUsername);
                authenticationInfo.setTenantDomain(MultitenantUtils.getTenantDomain(carbonCompliantUsername));

            } else {
                logger.error(String.format("Access denied, unable to find a tenant for user name : %s", username));

            }

        } catch (UserStoreException e) {
            String errorMsg = String.format("Unable to authenticate user : %s", username);
            logger.error(errorMsg, e);
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
        authenticationInfo.setAuthenticated(isAuthenticated);
        return authenticationInfo;
    }

    /**
     * Returns tenant Id given the user name or returns
     * {@link MultitenantConstants#INVALID_TENANT_ID} if none can be found.
     * 
     * @param username The username of the user.
     * @return The tenant ID.
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