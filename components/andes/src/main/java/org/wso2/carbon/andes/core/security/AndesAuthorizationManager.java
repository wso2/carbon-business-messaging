/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.andes.core.security;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.security.caas.api.CarbonPrincipal;
import org.wso2.carbon.security.caas.user.core.bean.User;
import org.wso2.carbon.security.caas.user.core.exception.AuthorizationStoreException;
import org.wso2.carbon.security.caas.user.core.exception.IdentityStoreException;
import org.wso2.carbon.security.caas.user.core.exception.StoreException;

import java.security.Principal;
import java.util.Properties;
import java.util.Set;
import javax.security.auth.Subject;


/**
 * Handles authorization of Andes.
 */
public class AndesAuthorizationManager {

    private static Log log = LogFactory.getLog(AndesAuthorizationManager.class);

    /**
     * Check whether a given user is authorized.
     *
     * @param subject  The authenticated subject
     * @param resource The resource to check authorization for
     * @param action   The action require to authorize
     * @return True if authorized
     */
    public boolean isAuthorized(Subject subject, String resource, AuthorizeAction action, Properties properties) {
        boolean authorized = false;
        Set<Principal> principals = null;
        if (subject != null) { // No user associated with the thread
            principals = subject.getPrincipals();
        }
        if (principals == null) {
            return false;
        }
        for (Principal principal : principals) {
            if (principal instanceof CarbonPrincipal) {
                User user = ((CarbonPrincipal) principal).getUser();

                try {

                    if (user.getUserName().equals("admin")) {
                        authorized = true;
                    } else {
                        if (action.name().equals(AuthorizeAction.CREATE.name())) {
                            authorized = true;
                        } else if (action.name().equals(AuthorizeAction.BIND.name())) {
                            authorized = AndesAuthorizationHandler.handleBindQueue(user, resource, properties);
                        } else if (action.name().equals(AuthorizeAction.CONSUME.name())) {
                            authorized = AndesAuthorizationHandler.handleConsumeQueue(user, resource);
                        } else if (action.name().equals(AuthorizeAction.UNBIND.name())) {
                            authorized = AndesAuthorizationHandler.handleUnbindQueue(user, resource, properties);
                        } else if (action.name().equals(AuthorizeAction.DELETE.name())) {
                            authorized = AndesAuthorizationHandler.handleDeleteQueue(user, resource, properties);

                        }

                    }
                } catch (IdentityStoreException | StoreException | AuthorizationStoreException e) {
                    log.error("User " + user.getUserName() + "is not authorized to do " + action.name() , e);
                }
            }
        }

        return authorized;
    }


}
