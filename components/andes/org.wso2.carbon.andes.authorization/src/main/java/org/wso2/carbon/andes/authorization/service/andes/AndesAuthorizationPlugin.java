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

package org.wso2.carbon.andes.authorization.service.andes;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.log4j.Logger;
import org.wso2.andes.configuration.qpid.plugins.ConfigurationPlugin;
import org.wso2.andes.server.security.AbstractPlugin;
import org.wso2.andes.server.security.Result;
import org.wso2.andes.server.security.SecurityManager;
import org.wso2.andes.server.security.SecurityPluginFactory;
import org.wso2.andes.server.security.access.ObjectProperties;
import org.wso2.andes.server.security.access.ObjectType;
import org.wso2.andes.server.security.access.Operation;
import org.wso2.carbon.andes.authorization.andes.AndesAuthorizationHandler;
import org.wso2.carbon.andes.authorization.andes.AndesAuthorizationHandlerException;
import org.wso2.carbon.andes.authorization.internal.AuthorizationServiceDataHolder;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import javax.security.auth.Subject;
import java.security.Principal;

/**
 * Andes access control class based on Carbon Authorization Manager
 */
public class AndesAuthorizationPlugin extends AbstractPlugin {

    private static final Logger logger = Logger.getLogger(AndesAuthorizationPlugin.class);
    private static final String DOMAIN_NAME_SEPARATOR = "!";

    /**
     * Factory method for AndesAuthorizationPlugin
     */
    public static final SecurityPluginFactory<AndesAuthorizationPlugin>
            FACTORY = new SecurityPluginFactory<AndesAuthorizationPlugin>() {
        public AndesAuthorizationPlugin newInstance(ConfigurationPlugin config)
                throws ConfigurationException {
            return new AndesAuthorizationPlugin();
        }

        public String getPluginName() {
            return AndesAuthorizationPlugin.class.getName();
        }

        public Class<AndesAuthorizationPlugin> getPluginClass() {
            return AndesAuthorizationPlugin.class;
        }
    };

    /**
     * Authorize access to broker
     *
     * @param objectType We only control access to virtual host
     * @param instance the accessing instance
     * @return Authorization result
     */
    public Result access(ObjectType objectType, Object instance) {
        try {
            Subject subject = SecurityManager.getThreadSubject();
            Principal principal = (Principal) (subject.getPrincipals().toArray())[0];

            if (principal == null) { // No user associated with the thread
                return getDefault();
            }

            // Allow access to virtual host for all logged in users. Authorization happens only if a user is
            // authenticated.
            // So, at this point, the user is logged in.
            if (objectType == ObjectType.VIRTUALHOST) {
                return Result.ALLOWED;
            }
        } catch (Exception e) {
            logger.error("Authorising access to broker failed.", e);
        }

        return Result.DENIED;
    }

    /**
     * Authorize operations inside broker
     *
     * @param operation  Operation on broker object (CONSUME, PUBLISH, etc)
     * @param objectType Type of object (EXCHANGE, QUEUE, etc)
     * @param properties Properties attached to the operation
     * @return ALLOWED/DENIED
     */
    public Result authorise(Operation operation, ObjectType objectType,
                            ObjectProperties properties) {
        try {

            // Get username from tenant username
            PrivilegedCarbonContext.startTenantFlow();
            Subject subject = SecurityManager.getThreadSubject();

            Principal principal = null;
            if (subject != null) {
                principal = (Principal) (subject.getPrincipals().toArray())[0];
            }

            if (principal == null) { // No user associated with the thread
                return getDefault();
            }

            String username = principal.getName();

            if (username.contains(DOMAIN_NAME_SEPARATOR)) {
                String tenantDomain = username.substring(username.indexOf(DOMAIN_NAME_SEPARATOR) + 1);
                PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenantDomain);
                PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId(true);
            } else {
                PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantId(MultitenantConstants.SUPER_TENANT_ID);
                PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(MultitenantConstants
                                                                                              .SUPER_TENANT_DOMAIN_NAME);
            }

            // Get User Realm
            UserRealm userRealm = getUserRealm(username);

            int domainNameSeparatorIndex = username.indexOf(DOMAIN_NAME_SEPARATOR);
            if (-1 != domainNameSeparatorIndex) {
                username = username.substring(0, domainNameSeparatorIndex);
            }
            switch (operation) {
                case CREATE:
                    if (ObjectType.EXCHANGE == objectType) {
                        return Result.ALLOWED;
                    } else if (ObjectType.QUEUE == objectType) {
                        return AndesAuthorizationHandler.handleCreateQueue(
                                username, userRealm, properties);
                    }
                case BIND:
                    return AndesAuthorizationHandler.handleBindQueue(
                            username, userRealm, properties);
                case PUBLISH:
                    return AndesAuthorizationHandler.handlePublishToExchange(
                            username, userRealm, properties);
                case CONSUME:
                    return AndesAuthorizationHandler.handleConsumeQueue(
                            username, userRealm, properties);
                case BROWSE:
                    return AndesAuthorizationHandler.handleBrowseQueue(
                            username, userRealm, properties);
                case UNBIND:
                    return AndesAuthorizationHandler.handleUnbindQueue(username, userRealm, properties);
                case DELETE:
                    if (ObjectType.EXCHANGE == objectType) {
                        return Result.ALLOWED;
                    } else if (ObjectType.QUEUE == objectType) {
                        return AndesAuthorizationHandler.handleDeleteQueue(username, userRealm, properties);
                    }
                case PURGE:
                    return AndesAuthorizationHandler.handlePurgeQueue(username, userRealm, properties);
            }
        } catch (AndesAuthorizationHandlerException e) {
            logger.error("Error while invoking AndesAuthorizationHandler", e);
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }

        return Result.DENIED;
    }

    /**
     * Gets user realm of a user
     * @param username username of the user
     * @return the UserRealm
     */
    private static UserRealm getUserRealm(String username) {
        UserRealm userRealm = null;

        RealmService realmService = AuthorizationServiceDataHolder.getInstance().getRealmService();
        if (null != realmService) {
            try {
                // Get tenant ID
                int tenantID = !username.contains(DOMAIN_NAME_SEPARATOR) ?
                        MultitenantConstants.SUPER_TENANT_ID :
                        realmService.getTenantManager().getTenantId(username.substring(username.indexOf(DOMAIN_NAME_SEPARATOR) + 1));

                // Get Realm
                userRealm = realmService.getTenantUserRealm(tenantID);
            } catch (UserStoreException e) {
                logger.error("Error while getting tenant user realm for user " + username, e);
            } catch (NullPointerException e) {
                logger.error("Error while accessing the realm service.", e);
            }
        }

        return userRealm;
    }
}
