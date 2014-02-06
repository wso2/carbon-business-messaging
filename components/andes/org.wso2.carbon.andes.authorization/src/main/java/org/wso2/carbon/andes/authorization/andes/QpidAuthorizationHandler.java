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
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.andes.commons.CommonsUtil;
import org.wso2.carbon.andes.commons.registry.RegistryClient;
import org.wso2.carbon.andes.commons.registry.RegistryClientException;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.api.UserStoreException;
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
                String queueName =
                        getRawQueueName(properties.get(ObjectProperties.Property.NAME));

                //For registry we use a modified queue name
                String newQname = queueName.replace("@",AT_REPLACE_CHAR);
                // Store queue details
                RegistryClient.createQueue(newQname, username);

                String queueID = CommonsUtil.getQueueID(queueName);

                userRealm.getAuthorizationManager().authorizeUser(
                        username, queueID, TreeNode.Permission.CONSUME.toString().toLowerCase());
                userRealm.getAuthorizationManager().authorizeUser(
                        username, queueID, TreeNode.Permission.PUBLISH.toString().toLowerCase());
                userRealm.getAuthorizationManager().authorizeUser(
                        username, queueID, PERMISSION_CHANGE_PERMISSION);

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
                // Queue properties
                String queueName = getRawQueueName(properties.get(ObjectProperties.Property.NAME));

                String queueID = CommonsUtil.getQueueID(queueName);

                if (isAdminUser(username, userRealm)) {
                    return Result.ALLOWED;
                } else if (userRealm.getAuthorizationManager().isUserAuthorized(
                        username, queueID, TreeNode.Permission.CONSUME.toString().toLowerCase())) {
                    return Result.ALLOWED;
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
                    if (isAdminUser(username, userRealm)) {
                        return Result.ALLOWED;
                    } else if (userRealm.getAuthorizationManager().isUserAuthorized(
                            username, queueID,
                            TreeNode.Permission.CONSUME.toString().toLowerCase())) {
                        return Result.ALLOWED;
                    }
                } else if (DIRECT_EXCHANGE.equals(exchangeName)) {
                    String queueID = CommonsUtil.getQueueID(queueName);

                    // Authorize
                    if (isAdminUser(username, userRealm)) {
                        return Result.ALLOWED;
                    } else if (userRealm.getAuthorizationManager().isUserAuthorized(
                            username, queueID,
                            TreeNode.Permission.CONSUME.toString().toLowerCase())) {
                        return Result.ALLOWED;
                    }
                } else if (TOPIC_EXCHANGE.equals(exchangeName)) {

                    if (CarbonContext.getThreadLocalCarbonContext().getTenantId() > 0) {
                        // then we need to remove the domain name path from the topic name before saving to the registry
                        String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
                        routingKey = routingKey.substring(tenantDomain.length() + 1);
                    }
                    String topicID = CommonsUtil.getTopicID(routingKey);

                    // Authorize
                    String newRoutingKey = routingKey.replace("@", AT_REPLACE_CHAR);
                    String newQName = queueName.replace("@", AT_REPLACE_CHAR);
                    if (isAdminUser(username, userRealm)) {

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
        * @param username
        *              User who is trying to publish
        * @param userRealm
        *             User's Realm
        * @param properties
        *              NAME, ROUTING_KEY
        * @return
        *              ALLOWED, DENIED
        * @throws QpidAuthorizationHandlerException
        */
    public static Result handlePublishToExchange(String username, UserRealm userRealm, ObjectProperties properties)
            throws QpidAuthorizationHandlerException {
        try {
            if (null != userRealm) {
                // Exchange properties
                String exchangeName = getRawExchangeName(properties.get(ObjectProperties.Property.NAME));
                String routingKey = getRawRoutingKey(properties.get(ObjectProperties.Property.ROUTING_KEY));

                if (DIRECT_EXCHANGE.equals(exchangeName)) {
                    // Publish to queue
                    String queueID = CommonsUtil.getQueueID(routingKey);

                    // Authorize
                    if (isAdminUser(username, userRealm)) {
                        return Result.ALLOWED;
                    } else if (userRealm.getAuthorizationManager().isUserAuthorized(
                            username, queueID,
                            TreeNode.Permission.PUBLISH.toString().toLowerCase())) {
                        return Result.ALLOWED;
                    }
                } else if (TOPIC_EXCHANGE.equals(exchangeName)) {
                    // Publish to topic
                    if (CarbonContext.getThreadLocalCarbonContext().getTenantId() > 0) {
                        // then we need to remove the domain name path from the topic name before saving to the registry
                        String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
                        routingKey = routingKey.substring(tenantDomain.length() + 1);
                    }
                    String permissionID = CommonsUtil.getTopicID(routingKey);

                    // Authorize
                    if (isAdminUser(username, userRealm)) {
                        return Result.ALLOWED;
                    } else if (userRealm.getAuthorizationManager().isUserAuthorized(
                            username, permissionID,
                            TreeNode.Permission.PUBLISH.toString().toLowerCase())) {
                        return Result.ALLOWED;
                    }
                } else if (DEFAULT_EXCHANGE.equals(exchangeName)) {
                    // Publish to queue
                    String queueID = CommonsUtil.getQueueID(routingKey);

                    // Authorize
                    if (isAdminUser(username, userRealm)) {
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
    public static Result handleDeleteQueue(ObjectProperties properties)
            throws QpidAuthorizationHandlerException {
        try {
            String queueName =
                getRawQueueName(properties.get(ObjectProperties.Property.NAME));

            // Delete queue details

            String newQName = queueName.replace("@", AT_REPLACE_CHAR);
            RegistryClient.deleteQueue(queueName);

            return Result.ALLOWED;
        } catch (RegistryClientException e) {
            throw new QpidAuthorizationHandlerException(e);
        }
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
}

