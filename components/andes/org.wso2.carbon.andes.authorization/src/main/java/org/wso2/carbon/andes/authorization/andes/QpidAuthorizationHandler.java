/*
 *  Copyright (c) 2008, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.wso2.carbon.andes.authorization.andes;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.andes.server.security.Result;
import org.wso2.andes.server.security.access.ObjectProperties;
import org.wso2.carbon.andes.commons.CommonsUtil;
import org.wso2.carbon.andes.commons.registry.RegistryClient;
import org.wso2.carbon.andes.commons.registry.RegistryClientException;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.api.UserStoreManager;
import org.wso2.carbon.user.core.authorization.TreeNode;

/**
 * This class includes the actual access control logic
 */
public class QpidAuthorizationHandler {

    private static final Log log = LogFactory.getLog(QpidAuthorizationHandler.class);

    private static final String DEFAULT_EXCHANGE = "default";
    private static final String DIRECT_EXCHANGE = "amq.direct";
    private static final String TOPIC_EXCHANGE = "amq.topic";
    private static final String PERMISSION_CHANGE_PERMISSION = "changePermission";
    private static final String ADMIN_ROLE = "admin";
    private static final String AT_REPLACE_CHAR="_";
    public static final String UI_EXECUTE = "ui.execute";
    private static String ROLE_EVERY_ONE = "everyone";
    public static final String PERMISSION_ADMIN_MANAGE_QUEUE_ADD_QUEUE = "/permission/admin/manage/queue/addQueue";
    public static final String PERMISSION_ADMIN_MANAGE_QUEUE_BROWSE_QUEUE = "/permission/admin/manage/queue/browseQueue";
    public static final String PERMISSION_ADMIN_MANAGE_QUEUE_DELETE_QUEUE = "/permission/admin/manage/queue/deleteQueue";
    public static final String PERMISSION_ADMIN_MANAGE_TOPIC_ADD_TOPIC = "/permission/admin/manage/topic/addTopic";
    public static final String PERMISSION_ADMIN_MANAGE_TOPIC_DELETE_TOPIC = "/permission/admin/manage/topic/deleteTopic";
    public static final String PERMISSION_ADMIN_MANAGE_DLC_BROWSE_DLC = "/permission/admin/manage/dlc/browseDlc";

    /**
        * Handle creating queue
        *
        * @param username
        *              User who is trying to create the queue
        * @param userRealm
        *             User's Realm  
        * @param properties
        *              NAME, OWNER, DURABLE
        * @return
        *              ALLOWED/DENIED
        * @throws QpidAuthorizationHandlerException
        */
    public static Result handleCreateQueue(String username, UserRealm userRealm, ObjectProperties properties)
            throws QpidAuthorizationHandlerException {
        try {
            if (null != userRealm) {
                if(isAdminUser(username, userRealm) || userRealm.getAuthorizationManager()
                        .isUserAuthorized(username, PERMISSION_ADMIN_MANAGE_QUEUE_ADD_QUEUE, UI_EXECUTE) || userRealm.getAuthorizationManager()
                        .isUserAuthorized(username, PERMISSION_ADMIN_MANAGE_TOPIC_ADD_TOPIC, UI_EXECUTE)) {
                    String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
                    String queueName =
                            getRawQueueName(properties.get(ObjectProperties.Property.NAME));

                    //For registry we use a modified queue name
                    String newQname = queueName.replace("@",AT_REPLACE_CHAR);
                    // Store queue details
                    RegistryClient.createQueue(newQname, username);

                    String queueID = CommonsUtil.getQueueID(queueName);

                    if (isOwnDomain(tenantDomain, queueName) || isTopicSubscriberQueue(queueName)) {
                        UserStoreManager userStoreManager = userRealm.getUserStoreManager();
                        String[] roleNames = userStoreManager.getRoleListOfUser(username);
                        for (String role : roleNames) {
                            if(!role.equalsIgnoreCase(ROLE_EVERY_ONE) &&  userStoreManager.isExistingRole(role)){
                                userRealm.getAuthorizationManager().authorizeRole(
                                        role, queueID, TreeNode.Permission.CONSUME.toString().toLowerCase());
                                userRealm.getAuthorizationManager().authorizeRole(
                                        role, queueID, TreeNode.Permission.PUBLISH.toString().toLowerCase());
                                userRealm.getAuthorizationManager().authorizeRole(
                                        role, queueID, PERMISSION_CHANGE_PERMISSION);
                            }
                        }
                        return Result.ALLOWED;
                    }
                }
            }
        } catch (RegistryClientException e) {
            throw new QpidAuthorizationHandlerException(e);
        } catch (UserStoreException e) {
            throw new QpidAuthorizationHandlerException(e);
        }

        return Result.DENIED;
    }

    /**
        * Handle consuming queue
        *
        * IMPORTANT : Consuming an AMQP queue is not as same as consuming a JMS queue. The former is an atomic
        * operation that is allowed for the user who created the queue where as the latter is the binding to an exchange
        * based on permission granted.  
        *
        * @param username
        *              User who is trying to consume the queue
        * @param userRealm
        *             User's Realm
        * @param properties
        *              NAME, OWNER, TEMPORARY
        * @return
        *              ALLOWED/DENIED
        * @throws QpidAuthorizationHandlerException 
        */
    public static Result handleConsumeQueue(String username, UserRealm userRealm, ObjectProperties properties)
            throws QpidAuthorizationHandlerException {
        try {
            if (null != userRealm) {
                if(isAdminUser(username, userRealm) || userRealm.getAuthorizationManager()
                        .isUserAuthorized(username, PERMISSION_ADMIN_MANAGE_QUEUE_BROWSE_QUEUE, UI_EXECUTE) || userRealm.getAuthorizationManager()
                        .isUserAuthorized(username, PERMISSION_ADMIN_MANAGE_DLC_BROWSE_DLC, UI_EXECUTE)) {
                    // Queue properties
                    String queueName = getRawQueueName(properties.get(ObjectProperties.Property.NAME));
                    String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
                    String queueID = CommonsUtil.getQueueID(queueName);

                    if (isAdminUser(username, userRealm) && isOwnDomain(tenantDomain, queueName)) {
                        return Result.ALLOWED;
                    } else if (userRealm.getAuthorizationManager().isUserAuthorized(
                            username, queueID, TreeNode.Permission.CONSUME.toString().toLowerCase())) {
                        return Result.ALLOWED;
                    }
                }
            }
        } catch (UserStoreException e) {
            throw new QpidAuthorizationHandlerException(e);
        }

        return Result.DENIED;
    }

    /**
         * Authorize binding a queue to an exchange
         *
         * @param username                             topicID
         *              User who is trying to do the binding
         * @param userRealm
         *             User's Realm   
         * @param properties
         *              NAME, ROUTING_KEY
         * @return
         *              ALLOWED/DENIED
         * @throws QpidAuthorizationHandlerException 
         */
    public static Result handleBindQueue(String username, UserRealm userRealm, ObjectProperties properties)
            throws QpidAuthorizationHandlerException {
        try {
            if (null != userRealm) {
                String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
                // Bind properties
                String exchangeName =
                        getRawExchangeName(properties.get(ObjectProperties.Property.NAME));
                String queueName =
                        getRawQueueName(properties.get(ObjectProperties.Property.QUEUE_NAME));
                String routingKey =
                        getRawRoutingKey(properties.get(ObjectProperties.Property.ROUTING_KEY));
                
                if (DEFAULT_EXCHANGE.equals(exchangeName)) {
                    String queueID = CommonsUtil.getQueueID(queueName);

                    // Authorize
                    if (isAdminUser(username, userRealm) && isOwnDomain(tenantDomain, queueName)) {
                        return Result.ALLOWED;
                    } else if (userRealm.getAuthorizationManager().isUserAuthorized(
                            username, queueID,
                            TreeNode.Permission.CONSUME.toString().toLowerCase())) {
                        return Result.ALLOWED;
                    }
                } else if (DIRECT_EXCHANGE.equals(exchangeName)) {
                    String queueID = CommonsUtil.getQueueID(queueName);

                    // Authorize
                    if (isAdminUser(username, userRealm) && isOwnDomain(tenantDomain, queueName)) {
                        return Result.ALLOWED;
                    } else if (userRealm.getAuthorizationManager().isUserAuthorized(
                            username, queueID,
                            TreeNode.Permission.CONSUME.toString().toLowerCase())) {
                        return Result.ALLOWED;
                    }
                } else if (TOPIC_EXCHANGE.equals(exchangeName)) {

                    // Note:  we don't give topic name as <domain_name/topicname> but just the <topicname> with current authorization
                    //        model,hence commented this

                    /*if (CarbonContext.getThreadLocalCarbonContext().getTenantId() > 0) {
                        // then we need to remove the domain name path from the topic name before saving to the registry
                        String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
                        routingKey = routingKey.substring(tenantDomain.length() + 1);
                    }*/
                    String topicID = CommonsUtil.getTopicID(routingKey);

                    // Authorize
                    String newRoutingKey = routingKey.replace("@", AT_REPLACE_CHAR);
                    String newQName = queueName.replace("@", AT_REPLACE_CHAR);
                    if (isAdminUser(username, userRealm) && (isOwnDomain(tenantDomain, queueName) || isTopicSubscriberQueue(queueName))) {

                        // Store subscription
                      RegistryClient.createSubscription(newRoutingKey, newQName, username);
                        
                        return Result.ALLOWED;
                    } else if (userRealm.getAuthorizationManager().isUserAuthorized(
                            username, topicID,
                            TreeNode.Permission.SUBSCRIBE.toString().toLowerCase())) {
                        // Store subscription

                        RegistryClient.createSubscription(newRoutingKey,newQName, username);

                        return Result.ALLOWED;
                    }
                }
            }
        } catch (UserStoreException e) {
            throw new QpidAuthorizationHandlerException(e);
        } catch (RegistryClientException e) {
            throw new QpidAuthorizationHandlerException(e);
        }

        return Result.DENIED;
    }

    /**
        * Authorise publishing to a given exchange
        *
        *
     * @param username
     *              User who is trying to publish
     *@param userRealm
     *             User's Realm
     * @param properties
*              NAME, ROUTING_KEY   @return
        *              ALLOWED, DENIED
        * @throws QpidAuthorizationHandlerException
        */
    public static Result handlePublishToExchange(String username,UserRealm userRealm, ObjectProperties properties)
            throws QpidAuthorizationHandlerException {
        try {
            if (null != userRealm) {

                String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();

                // Exchange properties
                String exchangeName = getRawExchangeName(properties.get(ObjectProperties.Property.NAME));
                String routingKey = getRawRoutingKey(properties.get(ObjectProperties.Property.ROUTING_KEY));

                if (DIRECT_EXCHANGE.equals(exchangeName)) {  // Publish to queue

                    String queueID = CommonsUtil.getQueueID(routingKey);

                    // Authorize
                    if (isAdminUser(username, userRealm) && isOwnDomain(tenantDomain, routingKey)) {
                        return Result.ALLOWED;
                    } else if (userRealm.getAuthorizationManager().isUserAuthorized(
                            username, queueID,
                            TreeNode.Permission.PUBLISH.toString().toLowerCase())) {
                        return Result.ALLOWED;
                    }
                } else if (TOPIC_EXCHANGE.equals(exchangeName)) {   // Publish to topic

                    // Note:  we don't give topic name as <domain_name/topicname> but just the <topicname> with current authorization
                    //        model,hence commented this

                    /*if (CarbonContext.getThreadLocalCarbonContext().getTenantId() > 0) {
                         then we need to remove the domain name path from the topic name before saving to the registry
                        String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
                        routingKey = routingKey.substring(tenantDomain.length() + 1);
                    }*/
                    String permissionID = CommonsUtil.getTopicID(routingKey);

                    // Authorize
                    if (isAdminUser(username, userRealm)) {
                        return Result.ALLOWED;
                    } else if (userRealm.getAuthorizationManager().isUserAuthorized(
                            username, permissionID,
                            TreeNode.Permission.PUBLISH.toString().toLowerCase())) {
                        return Result.ALLOWED;
                    }
                } else if (DEFAULT_EXCHANGE.equals(exchangeName)) {  // Publish to queue

                    String queueID = CommonsUtil.getQueueID(routingKey);

                    // Authorize
                    if (isAdminUser(username, userRealm) && isOwnDomain(tenantDomain, routingKey)) {
                        return Result.ALLOWED;
                    } else if (userRealm.getAuthorizationManager().isUserAuthorized(
                            username, queueID,
                            TreeNode.Permission.PUBLISH.toString().toLowerCase())) {
                        return Result.ALLOWED;
                    }
                }
            }
        } catch (UserStoreException e) {
            throw new QpidAuthorizationHandlerException(e);
        }

        return Result.DENIED;
    }

    public static Result handleUnbindQueue(ObjectProperties properties)
            throws QpidAuthorizationHandlerException {
        try {
            // Bind properties
            String exchangeName =
                    getRawExchangeName(properties.get(ObjectProperties.Property.NAME));
            String queueName =
                    getRawQueueName(properties.get(ObjectProperties.Property.QUEUE_NAME));
            String routingKey =
                    getRawRoutingKey(properties.get(ObjectProperties.Property.ROUTING_KEY));


            String newRoutingKey = routingKey.replace("@", AT_REPLACE_CHAR);
            String newQName = queueName.replace("@", AT_REPLACE_CHAR);
            if (TOPIC_EXCHANGE.equals(exchangeName)) {
                // Delete subscription details
                RegistryClient.deleteSubscription(routingKey, queueName);
            }

            return Result.ALLOWED;
        } catch (RegistryClientException e) {
            throw new QpidAuthorizationHandlerException(e);
        }
    }

    /**
        * Handle deleting queue
        *
        * @param properties
        *              NAME, OWNER, DURABLE
        * @return
        *              ALLOWED/DENIED
        * @throws QpidAuthorizationHandlerException
        */
    public static Result handleDeleteQueue(String username,UserRealm userRealm, ObjectProperties properties)
            throws QpidAuthorizationHandlerException {
        try {
            if(isAdminUser(username, userRealm) || userRealm.getAuthorizationManager()
                    .isUserAuthorized(username, PERMISSION_ADMIN_MANAGE_QUEUE_DELETE_QUEUE, UI_EXECUTE) || userRealm.getAuthorizationManager()
                    .isUserAuthorized(username, PERMISSION_ADMIN_MANAGE_TOPIC_DELETE_TOPIC, UI_EXECUTE)) {
                String queueName =
                        getRawQueueName(properties.get(ObjectProperties.Property.NAME));

                // Delete queue details

                String newQName = queueName.replace("@", AT_REPLACE_CHAR);
                RegistryClient.deleteQueue(queueName);

                return Result.ALLOWED;
            }
        } catch (RegistryClientException e) {
            throw new QpidAuthorizationHandlerException(e);
        } catch (UserStoreException e) {
            throw new QpidAuthorizationHandlerException(e);
        }
        return Result.DENIED;
    }

    /**
         * Internally durable queue names have the format [client id]:[raw queue name]. This method
         * extracts raw name from it's internal name..
         *
         * @param queueName
         *          Internal queue name
         * @return
         *          Raw queue name
         */
    private static String getRawQueueName(String queueName) {
        if (queueName.indexOf(";") > -1){
            queueName = queueName.substring(0, queueName.indexOf(";"));
        }
        return queueName.substring(queueName.indexOf(":") + 1, queueName.length());
    }

    /**
         * Internally durable queue routing keys have the format [client id]:[raw routing key]. This method
         * extracts raw name from it's internal name..
         *
         * @param routingKey
         *          Internal routing key
         * @return
         *          Raw routing key
         */
    private static String getRawRoutingKey(String routingKey) {
        return routingKey.substring(routingKey.indexOf(":") + 1, routingKey.length());
    }

    /**
        * Internally default exchange has the name <<default>> that can not be used as Registry node. This method
        * trims off leading and trailing > and < characters and returns "default"
        *
        * @param exchangeName
        *               <<default>> for the default exchange
        * @return
        *               default for <<default>>
        */
    private static String getRawExchangeName(String exchangeName) {
        return exchangeName.equals("<<default>>") ? DEFAULT_EXCHANGE : exchangeName;
    }

    private static boolean isAdminUser(String username, UserRealm userRealm) {
        try {
            String[] userRoles = userRealm.getUserStoreManager().getRoleListOfUser(username);

            for (String userRole:userRoles) {
                if (ADMIN_ROLE.equals(userRole)) {
                    return true;
                }
            }
        } catch (UserStoreException e) {
            // do nothing
        }

        return false;
    }

    /**
     *  Check whether a queue/topic belongs to given domain in order to avoid other tenant domains' users operate on
     *  the given queue/topic
     * @param tenantDomain - domain name of tenant
     * @param routingKey - queue/topic name to be verified against tenantDomain
     * @return
     */
    private static boolean isOwnDomain(String tenantDomain, String routingKey) {
         boolean isOwnDomain = false;

        if(tenantDomain != null){
            if(routingKey.length()>=tenantDomain.length()+1 && routingKey.substring(0,tenantDomain.length()+1).equals(tenantDomain+"/")){
                isOwnDomain = true;
            } else if (tenantDomain.equalsIgnoreCase("carbon.super")){
                if(!routingKey.contains("/")){
                    isOwnDomain =  true;
                }
            }
        } else {   // tenantDomain is null,this implies this is a normal user.
            if(!routingKey.contains("/")){
                isOwnDomain =  true;
            }
        }

        return isOwnDomain;
    }

    /**
     * when a subscriber is created for a topic in tenant mode, a temporary queue as 'tmp_<queueId></>' created for its messages. this is to check
     * whether a queue is such kind of one.
     * @param queueName - topic subscriber's queue
     * @return
     */
    private static boolean isTopicSubscriberQueue(String queueName) {
        return queueName.startsWith("tmp_");

    }
}

