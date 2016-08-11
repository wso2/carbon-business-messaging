/*
  * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.andes.core.security;


import org.wso2.carbon.security.caas.api.CarbonPrincipal;
import org.wso2.carbon.security.caas.api.handler.UsernamePasswordCallbackHandler;

import java.security.Principal;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

/**
 * Handles authentication of Andes.
 */
public class AndesAuthenticationManager {

    /**
     * The login context name to be used for authentication which is configured in carbon JAAS.
     */
    private String loginContextName;

    /**
     * Constructor which initialises the login context name of the jaas implementation.
     *
     * @param loginContextName The name of the login context from jaas config to be used
     */
    public AndesAuthenticationManager(String loginContextName) {
        this.loginContextName = loginContextName;
    }

    /**
     * Authenticate a user with a username and password. Throws {@link LoginException} if authentication fails.
     *
     * @param username The username
     * @param password The password
     * @return Authenticated subject
     * @throws LoginException
     */
    public Subject authenticate(String username, String password) throws LoginException {
        UsernamePasswordCallbackHandler callbackHandler = new UsernamePasswordCallbackHandler();
//        callbackHandler.initialise(username, password);
//
        LoginContext loginContext = new LoginContext(loginContextName, callbackHandler);
        loginContext.login();
        return loginContext.getSubject();
    }

    /**
     * Authenticate a user from a CallbackHandler.  Throws {@link LoginException} if authentication fails.
     * @param callbackHandler The CallbackHandler initialized with login details
     * @return Authenticated subject
     * @throws LoginException
     */
    public Subject authenticate(CallbackHandler callbackHandler) throws LoginException {
        LoginContext loginContext = new LoginContext(loginContextName, callbackHandler);
        loginContext.login();
        return loginContext.getSubject();
    }

    /**
     * Extract the username from the authenticated subject.
     *
     * @param subject The authenticated subject
     * @return Returns username of the authenticated subject, returns a null String if not found.
     */
    public CarbonPrincipal extractUserPrincipalFromSubject(Subject subject) {
        CarbonPrincipal userPrincipal = null;
        for (Principal principal : subject.getPrincipals()) {
            if (principal instanceof CarbonPrincipal) {
                if (null != ((CarbonPrincipal) principal).getUser()) {
                    userPrincipal = (CarbonPrincipal) principal;
                    break;
                }
            }
        }

        return userPrincipal;
    }


}
