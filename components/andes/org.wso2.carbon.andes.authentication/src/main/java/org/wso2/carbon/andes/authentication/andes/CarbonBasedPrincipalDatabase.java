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

package org.wso2.carbon.andes.authentication.andes;

import org.apache.log4j.Logger;
import org.wso2.andes.server.security.auth.database.PrincipalDatabase;
import org.wso2.andes.server.security.auth.sasl.AuthenticationProviderInitialiser;
import org.wso2.andes.server.security.auth.sasl.UsernamePrincipal;
import org.wso2.andes.server.security.auth.sasl.plain.PlainInitialiser;
import org.wso2.andes.server.security.auth.sasl.plain.PlainPasswordCallback;
import org.wso2.carbon.andes.authentication.internal.AuthenticationServiceDataHolder;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.login.AccountNotFoundException;
import java.io.IOException;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Carbon-based principal database for Apache Qpid. This uses Carbon user manager to handle authentication
 */
public class CarbonBasedPrincipalDatabase implements PrincipalDatabase {

    private static final String DOMAIN_NAME_SEPARATOR = "!";

    private static final Logger logger = Logger.getLogger(CarbonBasedPrincipalDatabase.class);
    private Map<String, AuthenticationProviderInitialiser> saslServers;

    public CarbonBasedPrincipalDatabase() {
        saslServers = new HashMap<String, AuthenticationProviderInitialiser>();

        // Accept Plain incoming and compare it with UM value
        PlainInitialiser plain = new PlainInitialiser();
        plain.initialise(this);

        saslServers.put(plain.getMechanismName(), plain);
    }

    /**
     * Get list of SASL mechanism objects. We only use PLAIN.
     *
     * @return List of mechanism objects
     */
    public Map<String, AuthenticationProviderInitialiser> getMechanisms() {
        return saslServers;
    }

    public List<Principal> getUsers() {
        return null;
    }

    public boolean deletePrincipal(Principal principal)
            throws AccountNotFoundException {
        return true;
    }

    /**
     * Create Principal instance for a valid user
     *
     * @param username Principal username
     * @return Principal instance
     */
    public Principal getUser(String username) {
        Principal user = null;

        try {
            UserRealm userRealm = getUserRealm(username);

            if ((null != userRealm) && userRealm.getUserStoreManager().isExistingUser(username)) {
                user = new UsernamePrincipal(username);
            }
        } catch (Exception e) {
            logger.error("Error while retrieving RegistryService.", e);
        }

        return user;
    }

    public boolean verifyPassword(String principal, char[] password)
            throws AccountNotFoundException {
        return true;
    }

    public boolean updatePassword(Principal principal, char[] password)
            throws AccountNotFoundException {
        return true;
    }

    public boolean createPrincipal(Principal principal, char[] password) {
        return true;
    }

    public void reload() throws IOException {
    }

    /**
     * This method sets of a given principal is authenticated or not.
     *
     * @param principal        Principal to be authenticated
     * @param passwordCallback Callback to set if the user is authenticated or not. This also holds user's password.
     * @throws IOException
     * @throws AccountNotFoundException
     */
    public void setPassword(Principal principal, PasswordCallback passwordCallback)
            throws IOException, AccountNotFoundException {
        try {
            if (principal == null) {
                throw new IllegalArgumentException("Principal should never be null");
            }

            // Given username/password
            String username = principal.getName();
            String password = ((PlainPasswordCallback) passwordCallback).getPlainPassword();
            String domainName = null;

            boolean isAuthenticated = false;

            // Authenticate internal call from another Carbon component
            if (password.equals(AuthenticationServiceDataHolder.getInstance().getAccessKey())) {
                isAuthenticated = true;
            } else { // External call
                UserRealm userRealm = getUserRealm(username);

                // Can not find the user realm
                if (null == userRealm) {
                    throw new AccountNotFoundException("Invalid User : " + principal);
                }

                // Get username from tenant username
                int domainNameSeparatorIndex = username.lastIndexOf(DOMAIN_NAME_SEPARATOR);
                if (-1 != domainNameSeparatorIndex) {
                    domainName = username.substring(domainNameSeparatorIndex + 1);
                    username = username.substring(0, domainNameSeparatorIndex).replaceAll(DOMAIN_NAME_SEPARATOR,"@");
                }

                // User not found in the UM
                if (!userRealm.getUserStoreManager().isExistingUser(username)) {

                    throw new AccountNotFoundException("Invalid User : " + principal);
                }

                // Check if the user is authenticated
                isAuthenticated = userRealm.getUserStoreManager().authenticate(username, password);
                if (isAuthenticated && -1 != domainNameSeparatorIndex) {
                    RealmService realmService = AuthenticationServiceDataHolder.getInstance().getRealmService();
                    int tenantID = realmService.getTenantManager().getTenantId(domainName);
                    PrivilegedCarbonContext.destroyCurrentContext();
                    PrivilegedCarbonContext cc = PrivilegedCarbonContext.getThreadLocalCarbonContext();
                    cc.setTenantDomain(domainName);
                    cc.setTenantId(tenantID);
                }
            }

            // Let the engine know if the user is authenticated or not
            ((PlainPasswordCallback) passwordCallback).setAuthenticated(isAuthenticated);
        } catch (UserStoreException e) {
            logger.error("User not authenticated.", e);
        } catch (NullPointerException e) {
            logger.error("Error while authenticating.", e);
        }
    }

    public void setPasswordFile(String passwordFile) {
    }

    private UserRealm getUserRealm(String username) {
        UserRealm userRealm = null;

        RealmService realmService = AuthenticationServiceDataHolder.getInstance().getRealmService();
        if (null != realmService) {
            try {
                // Get tenant ID
                int tenantID = MultitenantConstants.SUPER_TENANT_ID;
                int domainNameSeparatorIndex = username.lastIndexOf(DOMAIN_NAME_SEPARATOR);
                PrivilegedCarbonContext.destroyCurrentContext();
                PrivilegedCarbonContext cc = PrivilegedCarbonContext.getThreadLocalCarbonContext();
                cc.setTenantDomain(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME);
                cc.setTenantId(tenantID);
                if (-1 != domainNameSeparatorIndex) { // Service case
                    String domainName = username.substring(domainNameSeparatorIndex + 1);
                    tenantID = realmService.getTenantManager().getTenantId(domainName);
                }

                // Get Realm
                userRealm = realmService.getTenantUserRealm(tenantID);
            } catch (org.wso2.carbon.user.api.UserStoreException e) {
                logger.error("Error while getting tenant user realm for user " + username, e);
            }
        }

        return userRealm;
    }
}
