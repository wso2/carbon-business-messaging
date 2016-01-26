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

package org.wso2.carbon.andes.authorization.andes;

import org.apache.log4j.Logger;
import org.dna.mqtt.moquette.server.IAuthorizer;
import org.wso2.andes.configuration.enums.MQTTAuthoriztionPermissionLevel;
import org.wso2.andes.mqtt.MQTTAuthorizationSubject;
import org.wso2.carbon.andes.authorization.internal.AuthorizationServiceDataHolder;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.user.api.AuthorizationManager;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.authorization.TreeNode;

/**
 * Authorize the connecting users against Carbon Permission Model. Intended usage is
 * via providing fully qualified class name in broker.xml
 *
 * This is just a simple authorization model. For dynamic topics use an implementation based on IAuthorizer
 */
public class CarbonPermissionBasedMQTTAuthorizer implements IAuthorizer {

    private static final Logger logger = Logger.getLogger(CarbonPermissionBasedMQTTAuthorizer.class);

    //topic will be based on carbon permission based model eg: if the topic is smarthome/bulb then the
    //permission string will be /permission/mqtt/topic/smarthome/bulb
    private static final String PERMISSION_PREFIX = "/permission/mqtt/topic/";

    /**
     *
     * @param authorizationSubject is the object passed from authentication
     * @param topic the topic that client is ought to be authorized
     * @param permissionLevel whenther its publishing or subscribing
     * @return
     */
    @Override
    public boolean isAuthorized(MQTTAuthorizationSubject authorizationSubject, String topic,
                                MQTTAuthoriztionPermissionLevel permissionLevel) {
        String permissionString = getPermissionStringFromTopic(topic);
        String username = authorizationSubject.getUsername();
        try {
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(
                    authorizationSubject.getTenantDomain(), true);
            int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId();
            UserRealm userRealm = AuthorizationServiceDataHolder.getInstance().getRealmService()
                    .getTenantUserRealm(tenantId);
            AuthorizationManager authorizationManager = userRealm.getAuthorizationManager();

            String permissionLevelString = TreeNode.Permission.SUBSCRIBE.toString().toLowerCase();
            if (permissionLevel == MQTTAuthoriztionPermissionLevel.PUBLISH) {
                permissionLevelString = TreeNode.Permission.PUBLISH.toString().toLowerCase();
            }
            //scope based authorization
            //List<String> scopes = (List<String>)authorizationSubject.getProperties().get("scopes");
            return authorizationManager.isUserAuthorized(authorizationSubject.getUsername(),
                                                             permissionString, permissionLevelString);
        } catch (UserStoreException e) {
            String errorMsg = String.format("Unable to authenticate the user : %s", username);
            logger.error(errorMsg, e);
            return false;
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
    }

    /**
     * this takes topic as paramter and converts to the permission String
     * if there is +/#(priority order) in topic then the client requires permission before those character
     * eg: smarthome/+ requires permission for /permission/mqtt/topic/smarthome
     * @param topic that needs to be converted to permission String
     * @return the permission string
     */
    private String getPermissionStringFromTopic(String topic){
        String permission = topic;
        if(topic!=null && !topic.isEmpty()) {
            if (permission.charAt(0)=='/') {
                permission = permission.substring(1, permission.length());
            }
            permission = permission.split("\\+")[0];
            permission = permission.split("#")[0];
            permission = PERMISSION_PREFIX + permission;
            if (permission.charAt(permission.length()-1)=='/') {
                permission = permission.substring(0, permission.length()-1);
            }
            System.out.println(permission);
        }
        return  permission;
    }
}