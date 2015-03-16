/*
*  Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
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
 * This class evaluates the user permissions that are allowed for a user when doing an action for a
 * certain queue, topic or durable topic.
 *
 * @see <a href="https://qpid.apache.org/releases/qpid-0.24/cpp-broker/book/chap-Messaging_User_Guide-Security.html#tabl-Messaging_User_Guide-ACL_Syntax-ACL_Rulesaction">Qpid Actions</a>
 */
public class AndesAuthorizationHandler {

    /**
     * The logger used to log information, warnings, errors, etc.
     */
    private static final Log log = LogFactory.getLog(AndesAuthorizationHandler.class);

    /**
     * Pre-declared name for amqp "Default Exchange".
     */
    private static final String DEFAULT_EXCHANGE = "default";

    /**
     * Pre-declared name for amqp "Direct Exchange".
     */
    private static final String DIRECT_EXCHANGE = "amq.direct";

    /**
     * Pre-declared name for amqp "Topic Exchange".
     */
    private static final String TOPIC_EXCHANGE = "amq.topic";

    /**
     * Permission string for changing permission for a queue/topic.
     */
    private static final String PERMISSION_CHANGE_PERMISSION = "changePermission";

    /**
     * Underscore character for routing key change.
     */
    private static final String AT_REPLACE_CHAR = "_";

    /**
     * Permission value for changing permissions through UI.
     */
    private static final String UI_EXECUTE = "ui.execute";

    /**
     * Permission path for adding a queue.
     */
    private static final String PERMISSION_ADMIN_MANAGE_QUEUE_ADD_QUEUE =
                                                        "/permission/admin/manage/queue/addQueue";

    /**
     * Permission path for deleting a queue.
     */
    private static final String PERMISSION_ADMIN_MANAGE_QUEUE_DELETE_QUEUE =
                                                    "/permission/admin/manage/queue/deleteQueue";

    /**
     * Permission path for purging a queue messages.
     */
    private static final String PERMISSION_ADMIN_MANAGE_QUEUE_PURGE_QUEUE =
                                                        "/permission/admin/manage/queue/purgeQueue";

    /**
     * permission path for adding a topic.
     */
    private static final String PERMISSION_ADMIN_MANAGE_TOPIC_ADD_TOPIC =
                                                        "/permission/admin/manage/topic/addTopic";

    /**
     * Permission path for deleting a topic.
     */
    private static final String PERMISSION_ADMIN_MANAGE_TOPIC_DELETE_TOPIC =
                                                    "/permission/admin/manage/topic/deleteTopic";

    /**
     * Permission path for purging topic messages.
     */
    private static final String PERMISSION_ADMIN_MANAGE_TOPIC_PURGE_TOPIC =
                                                    "/permission/admin/manage/topic/purgeTopic";

    /**
     * Prefix for creating an internal role for queues.
     */
    private static final String QUEUE_ROLE_PREFIX = "Q_";

    /**
     * Prefix for creating an internal role for topics.
     */
    private static final String TOPIC_ROLE_PREFIX = "T_";

    /**
     * The prefix used for temporary topic destination names.
     */
    private static final String TEMP_QUEUE_SUFFIX = "tmp_";

    /**
     * Evaluates user permissions when creating a queue.
     *
     * @param username   User who is trying to create the queue
     * @param userRealm  User's Realm
     * @param properties NAME, OWNER, DURABLE
     * @return ALLOWED/DENIED
     * @throws AndesAuthorizationHandlerException
     */
    public static Result handleCreateQueue(String username, UserRealm userRealm,
                                           ObjectProperties properties)
            throws AndesAuthorizationHandlerException {
        Result accessResult = Result.DENIED;
        try {
            if (null != userRealm) {
                if (UserCoreUtil.isPrimaryAdminUser(username, userRealm.getRealmConfiguration())) {
                    registerAndAuthorizeQueue(username, userRealm, properties);
                    accessResult = Result.ALLOWED;
                } else if (userRealm.getAuthorizationManager().isUserAuthorized(username,
                                            PERMISSION_ADMIN_MANAGE_QUEUE_ADD_QUEUE, UI_EXECUTE)) {
                    registerAndAuthorizeQueue(username, userRealm, properties);
                    accessResult = Result.ALLOWED;
                } else if (userRealm.getAuthorizationManager().isUserAuthorized(username,
                                            PERMISSION_ADMIN_MANAGE_TOPIC_ADD_TOPIC, UI_EXECUTE)) {
                    registerAndAuthorizeQueue(username, userRealm, properties);
                    accessResult = Result.ALLOWED;
                } else if (isDurableTopicSubscriberQueue(
                                                    properties.get(ObjectProperties.Property.NAME),
                                                     properties.get(ObjectProperties.Property.OWNER))
                           && Boolean.valueOf(properties.get(ObjectProperties.Property.DURABLE))) {
                    registerAndAuthorizeQueue(username, userRealm, properties);
                    accessResult = Result.ALLOWED;
                } else if (isTopicSubscriberQueue(properties.get(ObjectProperties.Property.NAME)) &&
                           !Boolean.valueOf(properties.get(ObjectProperties.Property.DURABLE))) {
                    registerAndAuthorizeQueue(username, userRealm, properties);
                    accessResult = Result.ALLOWED;
                }
            }
        } catch (RegistryClientException e) {
            throw new AndesAuthorizationHandlerException("Error handling create queue.", e);
        } catch (UserStoreException e) {
            throw new AndesAuthorizationHandlerException("Error handling create queue.", e);
        }

        return accessResult;
    }

    /**
     * Register queue and authorize to login user if login user has permission
     * Permission not validating when user subscribe to topic or durable topic and allow user to
     * register queue because topic name (routing key) not pass by andes in first call (CREATE) of
     * subscription. But permission check in the BIND operation to verify user has permission to
     * subscribe to given topic. It is possible in bind operation because topic name (routing key)
     * pass by andes.
     *
     * @param username   username of logged user
     * @param userRealm  The {@link org.wso2.carbon.user.api.UserRealm}
     * @param properties {@link org.wso2.andes.server.security.access.ObjectProperties} of the queue
     * @throws RegistryClientException
     * @throws UserStoreException
     */
    private static void registerAndAuthorizeQueue(String username, UserRealm userRealm,
                                                  ObjectProperties properties)
            throws RegistryClientException, UserStoreException {
        String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        String queueName =
                getRawQueueName(properties.get(ObjectProperties.Property.NAME));
        if (isOwnDomain(tenantDomain, queueName)) {

            //For registry we use a modified queue name
            String newQueueName = queueName.replace("@", AT_REPLACE_CHAR);
            // Store queue details
            RegistryClient.createQueue(newQueueName, username);

            String queueID = CommonsUtil.getQueueID(queueName);

            authorizeQueuePermissionsToLoggedInUser(username, newQueueName, queueID,
                                                    userRealm);
        }
    }

    /**
     * Evaluates whether the user has consuming permissions for a queue, topic or durable topic.
     * <p/>
     * IMPORTANT : Consuming an AMQP queue is not as same as consuming a JMS queue. The former is an
     * atomic operation that is allowed for the user who created the queue where as the latter is
     * the binding to an exchange based on permission granted.
     *
     * @param username   User who is trying to consume the queue
     * @param userRealm  User's Realm
     * @param properties NAME, OWNER, TEMPORARY
     * @return ALLOWED/DENIED
     * @throws AndesAuthorizationHandlerException
     */
    public static Result handleConsumeQueue(String username, UserRealm userRealm,
                                            ObjectProperties properties)
            throws AndesAuthorizationHandlerException {
        Result accessResult = Result.DENIED;
        if (null == userRealm) {
            accessResult = Result.DENIED;
        } else {

            // Queue properties
            String queueName = getRawQueueName(properties.get(ObjectProperties.Property.NAME));
            String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
            String queueID = CommonsUtil.getQueueID(queueName);

            try {
                // authorise if admin user
                if (UserCoreUtil.isPrimaryAdminUser(username, userRealm
                        .getRealmConfiguration()) && isOwnDomain(tenantDomain, queueName)) {
                    accessResult = Result.ALLOWED;

                    // authorise if either consume or browse queue
                } else if (userRealm.getAuthorizationManager().isUserAuthorized(
                        username, queueID, TreeNode.Permission.CONSUME.toString().toLowerCase())) {
                    accessResult = Result.ALLOWED;

                }
                // if non of the above deny permission
                return accessResult;
            } catch (UserStoreException e) {
                throw new AndesAuthorizationHandlerException("Error handling consume queue.", e);
            }
        }

        return accessResult;
    }

    /**
     * Authorize binding a destination to an exchange based on permissions.
     *
     * @param username   topicID
     *                   User who is trying to do the binding
     * @param userRealm  User's Realm
     * @param properties NAME, ROUTING_KEY
     * @return ALLOWED/DENIED
     * @throws AndesAuthorizationHandlerException
     */
    public static Result handleBindQueue(String username, UserRealm userRealm,
                                         ObjectProperties properties)
            throws AndesAuthorizationHandlerException {
        Result accessResult = Result.DENIED;
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
                    if (UserCoreUtil.isPrimaryAdminUser(username, userRealm.getRealmConfiguration())
                        && isOwnDomain(tenantDomain, queueName)) {
                        accessResult = Result.ALLOWED;
                    } else if (userRealm.getAuthorizationManager().isUserAuthorized(
                            username, queueID,
                            TreeNode.Permission.CONSUME.toString().toLowerCase())) {
                        accessResult = Result.ALLOWED;
                    } else if (isDurableTopicSubscriberQueue(
                                            properties.get(ObjectProperties.Property.QUEUE_NAME),
                                            properties.get(ObjectProperties.Property.OWNER)) &&
                               Boolean.valueOf(properties.get(ObjectProperties.Property.DURABLE))) {
                        accessResult = Result.ALLOWED;
                    } else if (isTopicSubscriberQueue(queueName) && !Boolean.valueOf(
                            properties.get(ObjectProperties.Property.DURABLE))) {
                        accessResult = Result.ALLOWED;
                    }
                } else if (DIRECT_EXCHANGE.equals(exchangeName)) {
                    String queueID = CommonsUtil.getQueueID(queueName);

                    // Authorize
                    if (UserCoreUtil.isPrimaryAdminUser(username, userRealm.getRealmConfiguration())
                        && isOwnDomain(tenantDomain, queueName)) {
                        accessResult = Result.ALLOWED;
                    } else if (userRealm.getAuthorizationManager().isUserAuthorized(
                            username, queueID,
                            TreeNode.Permission.CONSUME.toString().toLowerCase())) {
                        accessResult = Result.ALLOWED;
                    }
                } else if (TOPIC_EXCHANGE.equals(exchangeName)) {

                    String newRoutingKey = routingKey.replace("@", AT_REPLACE_CHAR);
                    String roleName = UserCoreUtil.addInternalDomainName(TOPIC_ROLE_PREFIX +
                                                                         newRoutingKey
                                                                                 .replace("/", "-"));
                    UserStoreManager userStoreManager = userRealm.getUserStoreManager();
                    String topicId = CommonsUtil.getTopicID(routingKey);
                    String newQName = queueName.replace("@", AT_REPLACE_CHAR);
                    String tempQueueId = CommonsUtil.getQueueID(queueName);
                    // Authorize
                    if (!isOwnDomain(tenantDomain, routingKey)) {
                        accessResult = Result.DENIED;
                    } else if (!userStoreManager.isExistingRole(roleName) && userRealm
                            .getAuthorizationManager().isUserAuthorized(username,
                                            PERMISSION_ADMIN_MANAGE_TOPIC_ADD_TOPIC, UI_EXECUTE)) {

                        //This is triggered when a topic is created.So the user who creates the
                        // topic will get publish/subscribe permissions

                        // Store subscription
                        RegistryClient.createSubscription(newRoutingKey, newQName, username);

                        authorizeTopicPermissionsToLoggedInUser(username, newRoutingKey, topicId,
                                                                tempQueueId, userRealm);
                        accessResult = Result.ALLOWED;
                    } else if (UserCoreUtil.isPrimaryAdminUser(username, userRealm
                            .getRealmConfiguration())
                               && isOwnDomain(tenantDomain, queueName)) {
                        // admin user who is in the same tenant domain get permission

                        // Store subscription
                        RegistryClient.createSubscription(newRoutingKey, newQName, username);

                        accessResult = Result.ALLOWED;
                    } else if (userRealm.getAuthorizationManager().isUserAuthorized(username,
                                topicId, TreeNode.Permission.SUBSCRIBE.toString().toLowerCase())) {
                        //This is triggered when a new subscriber is arrived when the topic
                        // has already been created

                        // Store subscription
                        RegistryClient.createSubscription(newRoutingKey, newQName, username);

                        authorizeTopicPermissionsToLoggedInUser(username, newRoutingKey, topicId,
                                                                tempQueueId, userRealm);
                        accessResult = Result.ALLOWED;
                    }
                }
            }
        } catch (UserStoreException e) {
            throw new AndesAuthorizationHandlerException("Error handling bind queue.", e);
        } catch (RegistryClientException e) {
            throw new AndesAuthorizationHandlerException("Error handling bind queue.", e);
        }

        return accessResult;
    }

    /**
     * Authorise publishing to a given exchange based on user's permissions.
     *
     * @param username   User who is trying to publish
     * @param userRealm  User's Realm
     * @param properties NAME, ROUTING_KEY   @return
     *                   ALLOWED, DENIED
     * @throws AndesAuthorizationHandlerException
     */
    public static Result handlePublishToExchange(String username, UserRealm userRealm,
                                                 ObjectProperties properties)
            throws AndesAuthorizationHandlerException {
        Result accessResult = Result.DENIED;
        try {
            if (null != userRealm) {

                String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();

                // Exchange properties
                String exchangeName =
                        getRawExchangeName(properties.get(ObjectProperties.Property.NAME));
                String routingKey =
                        getRawRoutingKey(properties.get(ObjectProperties.Property.ROUTING_KEY));

                if (DIRECT_EXCHANGE.equals(exchangeName)) {  // Publish to queue

                    String queueID = CommonsUtil.getQueueID(routingKey);

                    // Authorize admin user
                    if (UserCoreUtil
                            .isPrimaryAdminUser(username, userRealm.getRealmConfiguration())) {
                        accessResult = Result.ALLOWED;
                    } else if (userRealm.getAuthorizationManager().isUserAuthorized(
                            username, queueID,
                            TreeNode.Permission.PUBLISH.toString().toLowerCase())) {
                        accessResult = Result.ALLOWED;
                    }
                } else if (TOPIC_EXCHANGE.equals(exchangeName)) {   // Publish to topic
                    String permissionID = CommonsUtil.getTopicID(routingKey);

                    // Authorize admin user
                    if (!isOwnDomain(tenantDomain, routingKey)) {
                        accessResult = Result.DENIED;
                    } else if (UserCoreUtil
                            .isPrimaryAdminUser(username, userRealm.getRealmConfiguration())) {
                        accessResult = Result.ALLOWED;
                    } else if (userRealm.getAuthorizationManager().isUserAuthorized(
                            username, permissionID,
                            TreeNode.Permission.PUBLISH.toString().toLowerCase())) {
                        accessResult = Result.ALLOWED;
                    }
                } else if (DEFAULT_EXCHANGE.equals(exchangeName)) {  // Publish to queue

                    String queueID = CommonsUtil.getQueueID(routingKey);

                    // Authorize
                    if (UserCoreUtil.isPrimaryAdminUser(username, userRealm.getRealmConfiguration())
                        && isOwnDomain(tenantDomain, routingKey)) {
                        accessResult = Result.ALLOWED;
                    } else if (userRealm.getAuthorizationManager().isUserAuthorized(
                            username, queueID,
                            TreeNode.Permission.PUBLISH.toString().toLowerCase())) {
                        accessResult = Result.ALLOWED;
                    }
                }
            }
        } catch (UserStoreException e) {
            throw new AndesAuthorizationHandlerException("Error handling publish queue.", e);
        }

        return accessResult;
    }

    /**
     * Evaluates whether the user has unbind permissions for an exchange.
     *
     * @param properties NAME, QUEUE_NAME, ROUTING_KEY
     * @return ALLOWED/DENIED
     * @throws AndesAuthorizationHandlerException
     */
    public static Result handleUnbindQueue(ObjectProperties properties)
            throws AndesAuthorizationHandlerException {
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
            throw new AndesAuthorizationHandlerException("Error handling unbind queue.", e);
        }
    }

    /**
     * The following method handles the deletion of a queue by checking whether the user is
     * authorized or not. Admin users and users with queue or topic deletion are allowed to delete
     * the queue or topic.
     *
     * @param username   User who is trying to publish
     * @param userRealm  User's Realm
     * @param properties NAME, OWNER, DURABLE
     * @return ALLOWED/DENIED
     * @throws AndesAuthorizationHandlerException
     */
    public static Result handleDeleteQueue(String username, UserRealm userRealm,
                                           ObjectProperties properties)
            throws AndesAuthorizationHandlerException {
        Result accessResult = Result.DENIED;
        try {
            String queueName =
                    getRawQueueName(properties.get(ObjectProperties.Property.NAME));
            if (UserCoreUtil.isPrimaryAdminUser(username, userRealm.getRealmConfiguration())) {
                deleteQueueFromRegistry(queueName);
                accessResult = Result.ALLOWED;
            } else if (userRealm.getAuthorizationManager().isUserAuthorized(username,
                                        PERMISSION_ADMIN_MANAGE_QUEUE_DELETE_QUEUE, UI_EXECUTE)) {
                deleteQueueFromRegistry(queueName);
                accessResult = Result.ALLOWED;
            } else if (userRealm.getAuthorizationManager().isUserAuthorized(username,
                                        PERMISSION_ADMIN_MANAGE_TOPIC_DELETE_TOPIC, UI_EXECUTE)) {
                deleteQueueFromRegistry(queueName);
                accessResult = Result.ALLOWED;
            } else if (isDurableTopicSubscriberQueue(properties.get(ObjectProperties.Property.NAME),
                                                 properties.get(ObjectProperties.Property.OWNER))
                       && Boolean.valueOf(properties.get(ObjectProperties.Property.DURABLE))) {
                accessResult = Result.ALLOWED;
            } else if (isTopicSubscriberQueue(queueName) &&
                       !Boolean.valueOf(properties.get(ObjectProperties.Property.DURABLE))) {
                accessResult = Result.ALLOWED;
            }
        } catch (RegistryClientException e) {
            throw new AndesAuthorizationHandlerException("Error handling delete queue.", e);
        } catch (UserStoreException e) {
            throw new AndesAuthorizationHandlerException("Error handling delete queue.", e);
        }
        return accessResult;
    }

    /**
     * This method handles the deletion of messages of a topic or queue. The deletion of messages is
     * only allowed if the permissions for the user exists.
     *
     * @param username  User who is trying to publish
     * @param userRealm User's Realm that represents the user store
     * @return ALLOWED/DENIED
     * @throws AndesAuthorizationHandlerException
     */
    public static Result handlePurgeQueue(String username, UserRealm userRealm)
            throws AndesAuthorizationHandlerException {
        Result accessResult = Result.DENIED;
        try {
            if (UserCoreUtil.isPrimaryAdminUser(username, userRealm.getRealmConfiguration())
                || userRealm.getAuthorizationManager()
                    .isUserAuthorized(username, PERMISSION_ADMIN_MANAGE_QUEUE_PURGE_QUEUE, UI_EXECUTE)
                || userRealm.getAuthorizationManager()
                    .isUserAuthorized(username, PERMISSION_ADMIN_MANAGE_TOPIC_PURGE_TOPIC, UI_EXECUTE))
            {
                accessResult = Result.ALLOWED;
            }
        } catch (UserStoreException e) {
            throw new AndesAuthorizationHandlerException("Error handling purge queue.", e);
        }
        return accessResult;
    }

    /**
     * Remove queue from registry and un-assign role from queue
     *
     * @param queueName queue name to unregister
     * @throws RegistryClientException
     * @throws UserStoreException
     */
    private static void deleteQueueFromRegistry(String queueName)
            throws RegistryClientException, UserStoreException {
        // Modifying queue name for registry
        String newQName = queueName.replace("@", AT_REPLACE_CHAR);

        // Delete queue details
        RegistryClient.deleteQueue(queueName);

        // Deleting internal role created for user.
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
     * Internally durable queue routing keys have the format [client id]:[raw routing key]. This
     * method extracts raw name from it's internal name..
     *
     * @param routingKey Internal routing key
     * @return Raw routing key
     */
    private static String getRawRoutingKey(String routingKey) {
        return routingKey.substring(routingKey.indexOf("carbon:") + 1, routingKey.length());
    }

    /**
     * Internally default exchange has the name <<default>> that can not be used as Registry node.
     * This method trims off leading and trailing > and < characters and returns "default"
     *
     * @param exchangeName <<default>> for the default exchange
     * @return default for <<default>>
     */
    private static String getRawExchangeName(String exchangeName) {
        return exchangeName.equals("<<default>>") ? DEFAULT_EXCHANGE : exchangeName;
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
            if ((routingKey.length() >= tenantDomain.length() + 1) && routingKey.substring(0,
                                            tenantDomain.length() + 1).equals(tenantDomain + "/")) {
                isOwnDomain = true;
            } else if (tenantDomain.equalsIgnoreCase("carbon.super")) {
                if (!routingKey.contains("/")) {
                    isOwnDomain = true;
                }
            }
        } else {
            // tenantDomain is null,this implies this is a normal user.
            if (!routingKey.contains("/")) {
                isOwnDomain = true;
            }
        }

        return isOwnDomain;
    }

    /**
     * when a subscriber is created for a topic in tenant mode, a temporary queue as
     * 'tmp_<queueId></>' created for its messages. this is to check whether a queue is such kind of
     * one.
     *
     * @param queueName - topic subscriber's queue
     * @return true if queue is a temporary queue for topics. false otherwise
     */
    private static boolean isTopicSubscriberQueue(String queueName) {
        return queueName.startsWith(TEMP_QUEUE_SUFFIX);

    }

    /**
     * Durable queue created with prefix of virtual host name when a subscriber is created for a
     * durable topic. This check whether subscription for create durable topic.
     *
     * @param queueName   durable topic subscriber's queue
     * @param virtualHost virtual host name
     * @return check queue name start with virtual host name to verify subscription is for durable
     * topic
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
    private static void authorizeQueuePermissionsToLoggedInUser(String username, String queueName,
                                                                String queueId, UserRealm userRealm)
                                                                        throws UserStoreException {

        // if this is the dead letter channel user is not given permission to consume or subscribe
        if (DLCQueueUtils.isDeadLetterQueue(queueName)) {
            if (log.isDebugEnabled()) {
                log.debug("Dead letter channel permission to subscribe or consume is not granted " +
                          "to users");
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
                                                              TreeNode.Permission.CONSUME.toString()
                                                                      .toLowerCase());
            userRealm.getAuthorizationManager().authorizeRole(roleName, queueId,
                                                              TreeNode.Permission.PUBLISH.toString()
                                                                      .toLowerCase());
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
    private static void authorizeTopicPermissionsToLoggedInUser(String username, String topicName,
                                                                String topicId, String tempQueueID,
                                                                UserRealm userRealm)
                                                                    throws UserStoreException {

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
                                                          TreeNode.Permission.SUBSCRIBE.toString()
                                                                  .toLowerCase());
        userRealm.getAuthorizationManager().authorizeRole(roleName, topicId,
                                                          TreeNode.Permission.PUBLISH.toString()
                                                                  .toLowerCase());
        userRealm.getAuthorizationManager().authorizeRole(roleName, topicId,
                                                          PERMISSION_CHANGE_PERMISSION);

        //Giving permissions for the temporary queue
        userRealm.getAuthorizationManager().authorizeRole(roleName, tempQueueID,
                                                          TreeNode.Permission.CONSUME.toString()
                                                                  .toLowerCase());
        userRealm.getAuthorizationManager().authorizeRole(roleName, tempQueueID,
                                                          TreeNode.Permission.PUBLISH.toString()
                                                                  .toLowerCase());
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

