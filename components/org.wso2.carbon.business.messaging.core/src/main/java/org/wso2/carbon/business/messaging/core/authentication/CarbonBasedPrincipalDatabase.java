/*
 * Copyright (c) 2005-2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.business.messaging.core.authentication;

import org.apache.log4j.Logger;
import org.wso2.andes.server.security.auth.database.PrincipalDatabase;
import org.wso2.andes.server.security.auth.sasl.AuthenticationProviderInitialiser;
import org.wso2.andes.server.security.auth.sasl.plain.PlainInitialiser;
import org.wso2.andes.server.security.auth.sasl.plain.PlainPasswordCallback;
import org.wso2.carbon.business.messaging.core.exceptions.AuthenticationException;
import org.wso2.carbon.business.messaging.core.internal.BrokerServiceDataHolder;

import java.io.IOException;
import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.login.AccountNotFoundException;

/**
 * Carbon-based principal database for Apache Qpid. This uses Carbon user manager to handle authentication.
 */
public class CarbonBasedPrincipalDatabase implements PrincipalDatabase {

    private static final Logger logger = Logger.getLogger(CarbonBasedPrincipalDatabase.class);
    private Map<String, AuthenticationProviderInitialiser> saslServers;
    private AuthenticationService authenticationService;

    public CarbonBasedPrincipalDatabase() {
        saslServers = new HashMap<String, AuthenticationProviderInitialiser>();
        //Get the registered authentication service implementation
        authenticationService = BrokerServiceDataHolder.getInstance().getAuthenticationService();
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
    @Override
    public Map<String, AuthenticationProviderInitialiser> getMechanisms() {
        return saslServers;
    }

    @Override
    public List<Principal> getUsers() {
        return null;
    }

    @Override
    public boolean deletePrincipal(Principal principal) throws AccountNotFoundException {
        return true;
    }

    /**
     * Create Principal instance for a valid user
     *
     * @param username Principal username
     * @return Principal instance
     */
    @Override
    //TODO: Once auth JAAS component is available
    public Principal getUser(String username) {
        Principal user = null;
        return user;
    }

    @Override
    public boolean verifyPassword(String principal, char[] password) throws AccountNotFoundException {
        return true;
    }

    @Override
    public boolean updatePassword(Principal principal, char[] password) throws AccountNotFoundException {
        return true;
    }

    @Override
    public boolean createPrincipal(Principal principal, char[] password) {
        return true;
    }

    @Override
    public void reload() throws IOException {
    }

    /**
     * This method sets of a given principal is authenticated or not.
     *
     * @param principal        Principal to be authenticated
     * @param passwordCallback Callback to set if the user is authenticated or not. This also holds user's password.
     * @throws IOException IOException can be thrown
     */
    @Override
    public void setPassword(Principal principal, PasswordCallback passwordCallback)
            throws IOException, AccountNotFoundException {

        if (principal == null) {
            throw new IllegalArgumentException("Principal should never be null");
        }

        // Given username/password
        String userName = principal.getName();
        String password = "";
        if (passwordCallback instanceof PlainPasswordCallback) {
            password = ((PlainPasswordCallback) passwordCallback).getPlainPassword();
        }

        boolean isAuthenticated = false;
        try {
            //Use authenticationService interface based implementation
            if (authenticationService != null) {
                isAuthenticated = authenticationService.isValidUser(userName, password);
            } else {
                logger.error("No AuthenticationService implementation is available");
            }
        } catch (AuthenticationException e) {
            logger.error("Error while validating user" + userName, e);
        }
        if (passwordCallback instanceof PlainPasswordCallback) {
            // Let the engine know if the user is authenticated or not
            ((PlainPasswordCallback) passwordCallback).setAuthenticated(isAuthenticated);
        }

    }

    //TODO: Once auth JAAS component is available
    public void setPasswordFile(String passwordFile) {
    }

}
