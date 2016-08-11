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
import org.wso2.carbon.andes.core.internal.AndesContext;
import org.wso2.carbon.security.caas.api.CarbonPrincipal;
import org.wso2.carbon.security.caas.user.core.bean.User;
import org.wso2.carbon.security.caas.user.core.exception.AuthorizationStoreException;
import org.wso2.carbon.security.caas.user.core.exception.IdentityStoreException;
import org.wso2.carbon.security.caas.user.core.exception.StoreException;
import org.wso2.carbon.security.caas.user.core.store.AuthorizationStore;

import javax.security.auth.Subject;
import java.security.Principal;
import java.util.Properties;


/**
 * Handles authorization of Andes.
 */
public class AndesAuthorizationManager {

    private static Log log = LogFactory.getLog(AndesAuthorizationManager.class);

    /**
     * Check whether a given user is authorized.
     *
     * @param subject The authenticated subject
     * @param resource The resource to check authorization for
     * @param action The action require to authorize
     * @return True if authorized
     */
    public boolean isAuthorized(Subject subject, String resource, AuthorizeAction action, Properties properties) {
        boolean authorized = false;

        for (Principal principal : subject.getPrincipals()) {
            if (principal instanceof CarbonPrincipal) {
                User user = ((CarbonPrincipal) principal).getUser();

                try {
                    AuthorizationStore authorizationStore =
                            AndesContext.getInstance().getRealmService().getAuthorizationStore();

                    if (user.getUserName().equals("admin")) {
                        authorized = true;
                    } else {
                        if (action.name().equals(AuthorizeAction.CREATE.name())) {
                            authorized = true;
                        } else if (action.name().equals(AuthorizeAction.BIND.name())) {
                            authorized = AndesAuthorizationHandler.handleBindQueue(user, resource, properties);
                        } else if (action.name().equals(AuthorizeAction.CONSUME.name())) {

                            authorized = AndesAuthorizationHandler.handleConsumeQueue(user, resource);
                        }

                    }
                } catch (IdentityStoreException | StoreException | AuthorizationStoreException e) {
                    log.error("Unable to authorize user", e);
                }
            }
        }

        return authorized;
    }




}
