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

package org.wso2.carbon.andes.authorization.andes;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.andes.server.queue.DLCQueueUtils;
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
import org.wso2.carbon.user.core.util.UserCoreUtil;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

/**
 * This class includes the actual access control logic
 */
public class QpidAuthorizationHandler {

    private static final Log log = LogFactory.getLog(QpidAuthorizationHandler.class);
    private static final String DEFAULT_EXCHANGE = "default";
    private static final String DIRECT_EXCHANGE = "amq.direct";
    private static final String TOPIC_EXCHANGE = "amq.topic";
    private static final String PERMISSION_CHANGE_PERMISSION = "changePermission";
    private static final String AT_REPLACE_CHAR = "_";
    private static final String UI_EXECUTE = "ui.execute";
    private static final String PERMISSION_ADMIN_MANAGE_QUEUE_ADD_QUEUE = "/permission/admin/manage/queue/addQueue";
    private static final String PERMISSION_ADMIN_MANAGE_QUEUE_DELETE_QUEUE =
            "/permission/admin/manage/queue/deleteQueue";
    private static final String PERMISSION_ADMIN_MANAGE_QUEUE_PURGE_QUEUE =
            "/permission/admin/manage/queue/purgeQueue";
    private static final String PERMISSION_ADMIN_MANAGE_TOPIC_ADD_TOPIC = "/permission/admin/manage/topic/addTopic";
    private static final String PERMISSION_ADMIN_MANAGE_TOPIC_DELETE_TOPIC =
            "/permission/admin/manage/topic/deleteTopic";
    private static final String PERMISSION_ADMIN_MANAGE_TOPIC_PURGE_TOPIC =
            "/permission/admin/manage/topic/purgeTopic";
    private static final String QUEUE_ROLE_PREFIX = "Q_";
    private static final String TOPIC_ROLE_PREFIX = "T_";

    /**
     * Handle creating queue
     *
     * @param username   User who is trying to create the queue
     * @param userRealm  User's Realm
     * @param properties NAME, OWNER, DURABLE
     * @return ALLOWED/DENIED
     * @throws QpidAuthorizationHandlerException
     */
    public static Result handleCreateQueue(String username, UserRealm userRealm,
                                           ObjectProperties properties)
            throws QpidAuthorizationHandlerException {
        try {
            if (null != userRealm) {
                if (isAdminUser(username, userRealm)) {
                    registerAndAuthorizeQueue(username, userRealm, properties);
                    return Result.ALLOWED;
                } else if (userRealm.getAuthorizationManager().isUserAuthorized(username,
                                                                                PERMISSION_ADMIN_MANAGE_QUEUE_ADD_QUEUE, UI_EXECUTE)) {
                    registerAndAuthorizeQueue(username, userRealm, properties);
                    return Result.ALLOWED;
                } else if (userRealm.getAuthorizationManager().isUserAuthorized(username,
                                                                                PERMISSION_ADMIN_MANAGE_TOPIC_ADD_TOPIC, UI_EXECUTE)) {
                    registerAndAuthorizeQueue(username, userRealm, properties);
                    return Result.ALLOWED;
                } else if (isDurableTopicSubscriberQueue(properties.get(ObjectProperties.Property.NAME),
                                                         properties.get(ObjectProperties.Property.OWNER)) && Boolean.valueOf(
                        properties.get(ObjectProperties.Property.DURABLE))) {
                    registerAndAuthorizeQueue(username, userRealm, properties);
                    return Result.ALLOWED;
                } else if (isTopicSubscriberQueue(properties.get(ObjectProperties.Property.NAME)) &&
                           !Boolean.valueOf(properties.get(ObjectProperties.Property.DURABLE))) {
                    registerAndAuthorizeQueue(username, userRealm, properties);
                    return Result.ALLOWED;
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
     * Register queue and authorize to login user if login user has permission
     * Permission not validating when user subscribe to topic or durable topic and allow user to register queue because
     * topic name (routing key) not pass by QPID in first call (CREATE) of subscription. But permission
     * check in the BIND operation to verify user has permission to subscribe to given topic. It is possible in bind
     * operation because topic name (routing key) pass by QPID.
     *
     * @param username   - username of logged user
     * @param userRealm  - @link {org.wso2.carbon.user.api.UserRealm}
     * @param properties - @link {org.wso2.andes.server.security.access.ObjectProperties}
     * @throws RegistryClientException
     * @throws UserStoreException
     */
    private static void registerAndAuthorizeQueue(String username, UserRealm userRealm,
                                                  ObjectProperties properties)
            throws RegistryClientException, UserStoreException {
        String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        String queueName =
                getRawQueueName(properties.get(ObjectProperties.Property.NAME));

        //For registry we use a modified queue name
        String newQueueName = queueName.replace("@", AT_REPLACE_CHAR);
        // Store queue details
        RegistryClient.createQueue(newQueueName, username);

        String queueID = CommonsUtil.getQueueID(queueName);

        if (isOwnDomain(tenantDomain, queueName)) {
            authorizeQueuePermissionsToLoggedInUser(username, newQueueName, queueID,
                                                    userRealm);
        }
    }

    /**
     * Handle consuming queue
     * <p/>
     * IMPORTANT : Consuming an AMQP queue is not as same as consuming a JMS queue. The former is an atomic
     * operation that is allowed for the user who created the queue where as the latter is the binding to an exchange
     * based on permission granted.
     *
     * @param username   User who is trying to consume the queue
     * @param userRealm  User's Realm
     * @param properties NAME, OWNER, TEMPORARY
     * @return ALLOWED/DENIED
     * @throws QpidAuthorizationHandlerException
     */
    public static Result handleConsumeQueue(String username, UserRealm userRealm,
                                            ObjectProperties properties)
            throws QpidAuthorizationHandlerException {
        if (null == userRealm) {
            return Result.DENIED;
        }
        // Queue properties
        String queueName = getRawQueueName(properties.get(ObjectProperties.Property.NAME));
        String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        String queueID = CommonsUtil.getQueueID(queueName);

        // if queue is owned by a different domain deny permission
        if (!isOwnDomain(tenantDomain, queueName)) {
            log.warn("Permission denied to publish to " + queueName + " in domain " + tenantDomain);
            return Result.DENIED;
        }

        try {
            // authorise if admin user
            if (isAdminUser(username, userRealm)) {

                return Result.ALLOWED;

                // authorise if either consume or browse queue
            } else if (userRealm.getAuthorizationManager().isUserAuthorized(
                    username, queueID, TreeNode.Permission.CONSUME.toString().toLowerCase())) {

                return Result.ALLOWED;

            }
            // if non of the above deny permission
            return Result.DENIED;
        } catch (UserStoreException e) {
            throw new QpidAuthorizationHandlerException(e);
        }
    }

    /**
     * Authorize binding a queue to an exchange
     *
     * @param username   topicID
     *                   User who is trying to do the binding
     * @param userRealm  User's Realm
     * @param properties NAME, ROUTING_KEY
     * @return ALLOWED/DENIED
     * @throws QpidAuthorizationHandlerException
     */
    public static Result handleBindQueue(String username, UserRealm userRealm,
                                         ObjectProperties properties)
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
                    } else if (isDurableTopicSubscriberQueue(properties.get(ObjectProperties.Property.QUEUE_NAME), properties.get(ObjectProperties.Property.OWNER))
                               && Boolean.valueOf(properties.get(ObjectProperties.Property.DURABLE))) {
                        return Result.ALLOWED;
                    } else if (isTopicSubscriberQueue(queueName) && !Boolean.valueOf(
                            properties.get(ObjectProperties.Property.DURABLE))) {
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

                    // Note:  we don't give topic name as <domain_name/topicname> but just the <topicname> with
                    // current authorization
                    //        model,hence commented this

                    /*if (CarbonContext.getThreadLocalCarbonContext().getTenantId() > 0) {
                        // then we need to remove the domain name path from the topic name before saving to the registry
                        String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
                        routingKey = routingKey.substring(tenantDomain.length() + 1);
                    }*/
                    String newRoutingKey = routingKey.replace("@", AT_REPLACE_CHAR);
                    String roleName = UserCoreUtil.addInternalDomainName(TOPIC_ROLE_PREFIX +
                                                                         newRoutingKey.replace("/", "-"));
                    UserStoreManager userStoreManager = userRealm.getUserStoreManager();
                    String topicId = CommonsUtil.getTopicID(routingKey);
                    String newQName = queueName.replace("@", AT_REPLACE_CHAR);
                    String tempQueueId = CommonsUtil.getQueueID(queueName);
                    // Authorize
                    if (!userStoreManager.isExistingRole(roleName) && userRealm
                            .getAuthorizationManager().isUserAuthorized(username, PERMISSION_ADMIN_MANAGE_TOPIC_ADD_TOPIC, UI_EXECUTE)) {

                        //This is triggered when a topic is created.So the user who creates the
                        // topic will get publish/subscribe permissions

                        // Store subscription
                        RegistryClient.createSubscription(newRoutingKey, newQName, username);

                        authorizeTopicPermissionsToLoggedInUser(username, newRoutingKey, topicId,
                                                                tempQueueId, userRealm);
                        return Result.ALLOWED;
                    } else if (isAdminUser(username, userRealm) && isOwnDomain(tenantDomain,
                                                                               queueName)) {
                        // admin user who is in the same tenant domain get permission

                        // Store subscription
                        RegistryClient.createSubscription(newRoutingKey, newQName, username);

                        return Result.ALLOWED;
                    } else if (userRealm.getAuthorizationManager().isUserAuthorized(username,
                                                                                    topicId, TreeNode.Permission.SUBSCRIBE.toString().toLowerCase())) {
                        //This is triggered when a new subscriber is arrived when the topic
                        // has already been created

                        // Store subscription
                        RegistryClient.createSubscription(newRoutingKey, newQName, username);

                        authorizeTopicPermissionsToLoggedInUser(username, newRoutingKey, topicId,
                                                                tempQueueId, userRealm);
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
     * @param username   User who is trying to publish
     * @param userRealm  User's Realm
     * @param properties NAME, ROUTING_KEY   @return
     *                   ALLOWED, DENIED
     * @throws QpidAuthorizationHandlerException
     */
    public static Result handlePublishToExchange(String username, UserRealm userRealm,
                                                 ObjectProperties properties)
            throws QpidAuthorizationHandlerException {
        try {
            if (null != userRealm) {

                String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();

                // Exchange properties
                String exchangeName = getRawExchangeName(properties.get(ObjectProperties.Property.NAME));
                String routingKey = getRawRoutingKey(properties.get(ObjectProperties.Property.ROUTING_KEY));

                // if queue is owned by a different domain deny permission
                if (!isOwnDomain(tenantDomain, routingKey)) {
                    log.warn("Permission denied to publish to " + routingKey + " in domain "
                             + tenantDomain);
                    return Result.DENIED;
                }

                if (DIRECT_EXCHANGE.equals(exchangeName)) {  // Publish to queue

                    String queueID = CommonsUtil.getQueueID(routingKey);

                    // Authorize admin user
                    if (isAdminUser(username, userRealm)) {
                        return Result.ALLOWED;
                    } else if (userRealm.getAuthorizationManager().isUserAuthorized(
                            username, queueID,
                            TreeNode.Permission.PUBLISH.toString().toLowerCase())) {
                        return Result.ALLOWED;
                    }
                } else if (TOPIC_EXCHANGE.equals(exchangeName)) {   // Publish to topic

                    // Note:  we don't give topic name as <domain_name/topicname> but just the <topicname> with
                    // current authorization
                    //        model,hence commented this

                    /*if (CarbonContext.getThreadLocalCarbonContext().getTenantId() > 0) {
                         then we need to remove the domain name path from the topic name before saving to the registry
                        String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
                        routingKey = routingKey.substring(tenantDomain.length() + 1);
                    }*/
                    String permissionID = CommonsUtil.getTopicID(routingKey);

                    // Authorize admin user
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

    /**
     * Handle queue unbinding
     *
     * @param properties NAME, QUEUE_NAME, ROUTING_KEY
     * @return ALLOWED/DENIED
     * @throws QpidAuthorizationHandlerException
     */
    public static Result handleUnbindQueue(ObjectProperties properties)
            throws QpidAuthorizationHandlerException {
        // Bind properties
        String exchangeName =
                getRawExchangeName(properties.get(ObjectProperties.Property.NAME));
        String queueName =
                getRawQueueName(properties.get(ObjectProperties.Property.QUEUE_NAME));
        String routingKey =
                getRawRoutingKey(properties.get(ObjectProperties.Property.ROUTING_KEY));

        String newRoutingKey = routingKey.replace("@", AT_REPLACE_CHAR);
        String newQName = queueName.replace("@", AT_REPLACE_CHAR);

        try {
            if (TOPIC_EXCHANGE.equals(exchangeName)) {
                // Delete subscription details
                RegistryClient.deleteSubscription(newRoutingKey, newQName);
            }

            return Result.ALLOWED;
        } catch (RegistryClientException e) {
            throw new QpidAuthorizationHandlerException(e);
        }
    }

    /**
     * Handle deleting queue
     *
     * @param username User who is trying to publish
     * @param userRealm User's Realm
     * @param properties NAME, OWNER, DURABLE
     * @return ALLOWED/DENIED
     * @throws QpidAuthorizationHandlerException
     */
    public static Result handleDeleteQueue(String username, UserRealm userRealm,
                                           ObjectProperties properties)
            throws QpidAuthorizationHandlerException {
        try {
            String queueName =
                    getRawQueueName(properties.get(ObjectProperties.Property.NAME));
            if (isAdminUser(username, userRealm)) {
                unregisterQueue(queueName);
                return Result.ALLOWED;
            } else if (userRealm.getAuthorizationManager().isUserAuthorized(username,
                                                                            PERMISSION_ADMIN_MANAGE_QUEUE_DELETE_QUEUE, UI_EXECUTE)) {
                unregisterQueue(queueName);
                return Result.ALLOWED;
            } else if (userRealm.getAuthorizationManager().isUserAuthorized(username,
                                                                            PERMISSION_ADMIN_MANAGE_TOPIC_DELETE_TOPIC, UI_EXECUTE)) {
                unregisterQueue(queueName);
                return Result.ALLOWED;
            } else if (isDurableTopicSubscriberQueue(properties.get(ObjectProperties.Property.NAME),
                                                     properties.get(ObjectProperties.Property.OWNER)) && Boolean.valueOf(
                    properties.get(ObjectProperties.Property.DURABLE))) {
                return Result.ALLOWED;
            } else if (isTopicSubscriberQueue(queueName) &&
                       !Boolean.valueOf(properties.get(ObjectProperties.Property.DURABLE))) {
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
     * Handle purging queue
     *
     * @param username User who is trying to publish
     * @param userRealm User's Realm
     * @return ALLOWED/DENIED
     * @throws QpidAuthorizationHandlerException
     */
    public static Result handlePurgeQueue(String username, UserRealm userRealm)
            throws QpidAuthorizationHandlerException {
        try {
            if (isAdminUser(username, userRealm) || userRealm.getAuthorizationManager()
                    .isUserAuthorized(username, PERMISSION_ADMIN_MANAGE_QUEUE_PURGE_QUEUE,
                                      UI_EXECUTE) || userRealm.getAuthorizationManager()
                        .isUserAuthorized(username, PERMISSION_ADMIN_MANAGE_TOPIC_PURGE_TOPIC, UI_EXECUTE)) {

                return Result.ALLOWED;
            }
        } catch (UserStoreException e) {
            throw new QpidAuthorizationHandlerException(e);
        }
        return Result.DENIED;
    }

    /**
     * Remove queue from registry and un-assign role from queue
     *
     * @param queueName queue name to unregister
     * @throws RegistryClientException
     * @throws UserStoreException
     */
    private static void unregisterQueue(String queueName)
            throws RegistryClientException, UserStoreException {
        // Delete queue details
        String newQName = queueName.replace("@", AT_REPLACE_CHAR);
        RegistryClient.deleteQueue(queueName);


        removeQueueRoleCreateForLoggedInUser(newQName);
    }

    /**
     * Internally durable queue names have the format [client id]:[raw queue name]. This method
     * extracts raw name from it's internal name..
     *
     * @param queueName Internal queue name
     * @return Raw queue name
     */
    private static String getRawQueueName(String queueName) {
        if (queueName.contains(";")) {
            queueName = queueName.substring(0, queueName.indexOf(";"));
        }
        return queueName.substring(queueName.indexOf(":") + 1, queueName.length());
    }

    /**
     * Internally durable queue routing keys have the format [client id]:[raw routing key]. This method
     * extracts raw name from it's internal name..
     *
     * @param routingKey Internal routing key
     * @return Raw routing key
     */
    private static String getRawRoutingKey(String routingKey) {
        return routingKey.substring(routingKey.indexOf("carbon:") + 1, routingKey.length());
    }

    /**
     * Internally default exchange has the name <<default>> that can not be used as Registry node. This method
     * trims off leading and trailing > and < characters and returns "default"
     *
     * @param exchangeName <<default>> for the default exchange
     * @return default for <<default>>
     */
    private static String getRawExchangeName(String exchangeName) {
        return exchangeName.equals("<<default>>") ? DEFAULT_EXCHANGE : exchangeName;
    }

    /**
     * Check whether the user has admin privileges
     *
     * @param username  username
     * @param userRealm userRealm of the user
     * @return true if user has admin privileges and false otherwise
     */
    private static boolean isAdminUser(String username, UserRealm userRealm) {
        try {
            String[] userRoles = userRealm.getUserStoreManager().getRoleListOfUser(username);
            String adminRole = userRealm.getRealmConfiguration().getAdminRoleName();

            for (String userRole : userRoles) {
                if (adminRole.equals(userRole)) {
                    return true;
                }
            }
        } catch (UserStoreException e) {
            log.error("Error while retrieving roles for user " + username, e);
        }
        return false;
    }

    /**
     * Check whether a queue/topic belongs to given domain in order to avoid other tenant domains'
     * users operate on the given queue/topic
     *
     * @param tenantDomain - domain name of tenant
     * @param routingKey   - queue/topic name to be verified against tenantDomain
     * @return true if queue/topic belongs to given domain and false otherwise
     */
    private static boolean isOwnDomain(String tenantDomain, String routingKey) {
        boolean isOwnDomain = false;

        if (tenantDomain != null) {
            if (routingKey.length() >= tenantDomain.length() + 1 && routingKey.substring(0,
                                                                                         tenantDomain.length() + 1).equals(tenantDomain + "/")) {
                isOwnDomain = true;
            } else if (tenantDomain.equalsIgnoreCase("carbon.super")) {
                if (!routingKey.contains("/")) {
                    isOwnDomain = true;
                }
            }
        } else {   // tenantDomain is null,this implies this is a normal user.
            if (!routingKey.contains("/")) {
                isOwnDomain = true;
            }
        }

        return isOwnDomain;
    }

    /**
     * when a subscriber is created for a topic in tenant mode, a temporary queue as 'tmp_<queueId></>' created for
     * its messages. this is to check
     * whether a queue is such kind of one.
     *
     * @param queueName - topic subscriber's queue
     * @return true if queue is a temporary queue for topics. false otherwise
     */
    private static boolean isTopicSubscriberQueue(String queueName) {
        return queueName.startsWith("tmp_");

    }

    /**
     * Durable queue created with prefix of virtual host name when a subscriber is created for a durable topic. This check whether subscription
     * for create durable topic
     *
     * @param queueName   - durable topic subscriber's queue
     * @param virtualHost - virtual host name
     * @return check queue name start with virtual host name to verify subscription is for durable topic
     */
    private static boolean isDurableTopicSubscriberQueue(String queueName, String virtualHost) {
        return !virtualHost.isEmpty() && queueName.startsWith(virtualHost);
    }

    /**
     * Create a new role which has the same name as the queueName and assign the logged in
     * user to the newly created role. Then, authorize the newly created role to subscribe and
     * publish to the queue.
     *
     * @param username  name of the logged in user
     * @param queueName queue name
     * @param queueId   ID given to the queue
     * @param userRealm User's Realm
     * @throws UserStoreException
     */
    private static void authorizeQueuePermissionsToLoggedInUser(String username,
                                                                String queueName,
                                                                String queueId,
                                                                UserRealm userRealm) throws
                                                                                     UserStoreException {

        // if this is the dead letter channel user is not given permission to consume or subscribe
        if (DLCQueueUtils.isDeadLetterQueue(queueName)) {
            if (log.isDebugEnabled()) {
                log.debug("Dead letter channel permission to subscribe or consume is not granted to " +
                          "users");
            }
            return;
        }

        //if the queue name has the tenant domain prefix we need to remove it
        if (CarbonContext.getThreadLocalCarbonContext().getTenantId() > 0) {
            String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
            if (queueName.startsWith(tenantDomain)) {
                queueName = queueName.substring(tenantDomain.length() + 1);
            }
        }

        String roleName = UserCoreUtil.addInternalDomainName(QUEUE_ROLE_PREFIX +
                                                             queueName.replace("/", "-"));
        UserStoreManager userStoreManager = userRealm.getUserStoreManager();


        if (!userStoreManager.isExistingRole(roleName)) {
            String[] user = {MultitenantUtils.getTenantAwareUsername(username)};
            userStoreManager.addRole(roleName, user, null);
            userRealm.getAuthorizationManager().authorizeRole(roleName, queueId,
                                                              PERMISSION_CHANGE_PERMISSION);
            userRealm.getAuthorizationManager().authorizeRole(roleName, queueId,
                                                              TreeNode.Permission.CONSUME.toString().toLowerCase());
            userRealm.getAuthorizationManager().authorizeRole(roleName, queueId,
                                                              TreeNode.Permission.PUBLISH.toString().toLowerCase());
        } else {
            log.warn("Unable to provide permissions to the user, " +
                     " " + username + ", to subscribe and publish to " + queueName);
        }
    }

    /**
     * Create a new role which has the same name as the topicName and assign the logged in
     * user to the newly created role. Then, authorize the newly created role to subscribe and
     * publish to the topic.
     *
     * @param username    name of the logged in user
     * @param topicName   destination name. Either topic or queue name
     * @param topicId     Id given to the destination
     * @param tempQueueID Id given to the binding temp queue
     * @param userRealm   User's Realm
     * @throws UserStoreException
     */
    private static void authorizeTopicPermissionsToLoggedInUser(String username,
                                                                String topicName,
                                                                String topicId,
                                                                String tempQueueID,
                                                                UserRealm userRealm) throws
                                                                                     UserStoreException {

        String roleName = UserCoreUtil.addInternalDomainName(TOPIC_ROLE_PREFIX +
                                                             topicName.replace("/", "-"));
        UserStoreManager userStoreManager = userRealm.getUserStoreManager();
        String[] user = {MultitenantUtils.getTenantAwareUsername(username)};

        if (!userStoreManager.isExistingRole(roleName)) {
            userStoreManager.addRole(roleName, user, null);
        }

        boolean userShouldBeAdded = true;
        for (String foundUser : userStoreManager.getUserListOfRole(roleName)) {
            if (username.equals(foundUser)) {
                userShouldBeAdded = false;
                break;
            }
        }

        if (userShouldBeAdded) {
            userStoreManager.updateUserListOfRole(roleName, new String[0], user);
        }
        //Giving permissions to the topic
        userRealm.getAuthorizationManager().authorizeRole(roleName, topicId,
                                                          TreeNode.Permission.SUBSCRIBE.toString().toLowerCase());
        userRealm.getAuthorizationManager().authorizeRole(roleName, topicId,
                                                          TreeNode.Permission.PUBLISH.toString().toLowerCase());
        userRealm.getAuthorizationManager().authorizeRole(roleName, topicId,
                                                          PERMISSION_CHANGE_PERMISSION);

        //Giving permissions for the temporary queue
        userRealm.getAuthorizationManager().authorizeRole(roleName, tempQueueID,
                                                          TreeNode.Permission.CONSUME.toString().toLowerCase());
        userRealm.getAuthorizationManager().authorizeRole(roleName, tempQueueID,
                                                          TreeNode.Permission.PUBLISH.toString().toLowerCase());
        userRealm.getAuthorizationManager().authorizeRole(roleName, tempQueueID,
                                                          PERMISSION_CHANGE_PERMISSION);
    }

    /**
     * Every queue has a role with the name QUEUE_ROLE_PREFIX+queueName. This role is used
     * to store the permissions for the user who created the queue.This role should be
     * deleted when the queue/topic is deleted.
     *
     * @param queueName name of the queue or topic
     * @throws UserStoreException
     */
    private static void removeQueueRoleCreateForLoggedInUser(String queueName)
            throws UserStoreException {
        String roleName = UserCoreUtil.addInternalDomainName(QUEUE_ROLE_PREFIX +
                                                             queueName.replace("/", "-"));

        UserStoreManager userStoreManager = CarbonContext.getThreadLocalCarbonContext()
                .getUserRealm().getUserStoreManager();

        if (userStoreManager.isExistingRole(roleName)) {
            userStoreManager.deleteRole(roleName);
        }
    }
}

