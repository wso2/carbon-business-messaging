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
import org.wso2.andes.kernel.AndesKernelBoot;
import org.wso2.andes.server.queue.DLCQueueUtils;
import org.wso2.andes.server.security.Result;
import org.wso2.andes.server.security.access.ObjectProperties;
import org.wso2.andes.server.security.access.Operation;
import org.wso2.carbon.andes.authorization.internal.AuthorizationServiceDataHolder;
import org.wso2.carbon.andes.commons.CommonsUtil;
import org.wso2.carbon.andes.commons.registry.RegistryClient;
import org.wso2.carbon.andes.commons.registry.RegistryClientException;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.user.api.AuthorizationManager;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.api.UserStoreManager;
import org.wso2.carbon.user.core.authorization.TreeNode;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.user.core.util.UserCoreUtil;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private static final String PERMISSION_ADMIN_MANAGE_QUEUE_ADD = "/permission/admin/manage/queue/add";

    /**
     * Permission path for deleting a queue.
     */
    private static final String PERMISSION_ADMIN_MANAGE_QUEUE_DELETE = "/permission/admin/manage/queue/delete";

    /**
     * Permission path for purging a queue messages.
     */
    private static final String PERMISSION_ADMIN_MANAGE_QUEUE_PURGE = "/permission/admin/manage/queue/purge";

    /**
     * Permission path for browsing a queue
     */
    private static final String PERMISSION_ADMIN_MANAGE_QUEUE_BROWSE = "/permission/admin/manage/queue/browse";

    /**
     * permission path for adding a topic.
     */
    private static final String PERMISSION_ADMIN_MANAGE_TOPIC_ADD = "/permission/admin/manage/topic/add";

    /**
     * Permission path for deleting a topic.
     */
    private static final String PERMISSION_ADMIN_MANAGE_TOPIC_DELETE = "/permission/admin/manage/topic/delete";

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
     * Empty space used to DEBUG logs
     */
    private static final String SPACE = " ";

    /**
     * Parent resource path of each topic
     */
    private static final String PARENT_RESOURCE_PATH = "\\bevent/topics/\\b";

    /**
     * This map used to handle 'consume' authorization of non durable topic
     */
    private static Map<String, String> temporaryQueueToTopicMap = new HashMap<>();

    /**
     * Evaluates user permissions when creating a queue.
     *
     * @param username   User who is trying to create the queue
     * @param userRealm  User's Realm
     * @param properties NAME, OWNER, DURABLE
     * @return ALLOWED/DENIED
     * @throws AndesAuthorizationHandlerException if user store or registry exception occurred
     */
    public static Result handleCreateQueue(String username, UserRealm userRealm,
                                           ObjectProperties properties, Operation operation)
            throws AndesAuthorizationHandlerException {
        Result accessResult = Result.DENIED;
        try {
            if (null != userRealm) {
                if (!isOwnDomain(properties.get(ObjectProperties.Property.NAME), userRealm, properties)) {
                    accessResult = Result.DENIED;
                } else if (isAdmin(username, userRealm)) {
                    registerAndAuthorizeQueue(username, userRealm, properties);
                    accessResult = Result.ALLOWED;
                } else if (userRealm.getAuthorizationManager().isUserAuthorized(username,
                        PERMISSION_ADMIN_MANAGE_QUEUE_ADD, UI_EXECUTE)) {
                    registerAndAuthorizeQueue(username, userRealm, properties);
                    accessResult = Result.ALLOWED;
                } else if (userRealm.getAuthorizationManager().isUserAuthorized(username,
                        PERMISSION_ADMIN_MANAGE_TOPIC_ADD, UI_EXECUTE)) {
                    registerAndAuthorizeQueue(username, userRealm, properties);
                    accessResult = Result.ALLOWED;
                } else if (Boolean.valueOf(properties.get(ObjectProperties.Property.DURABLE)) &&
                        Boolean.valueOf(properties.get(ObjectProperties.Property.EXCLUSIVE))) {
                    registerAndAuthorizeQueue(username, userRealm, properties);
                    accessResult = Result.ALLOWED;
                } else if (isTopicSubscriberQueue(properties.get(ObjectProperties.Property.NAME)) &&
                           !Boolean.valueOf(properties.get(ObjectProperties.Property.DURABLE))) {
                    registerAndAuthorizeQueue(username, userRealm, properties);
                    accessResult = Result.ALLOWED;
                }
            }
        } catch (RegistryClientException | UserStoreException e) {
            throw new AndesAuthorizationHandlerException("Error handling create queue.", e);
        }
        if (log.isDebugEnabled()) {
            log.debug(username + SPACE + accessResult.toString().toLowerCase() + SPACE + operation.toString() + SPACE
                    + properties.get(ObjectProperties.Property.NAME));
        }
        return accessResult;
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
     * @throws AndesAuthorizationHandlerException if user store or registry exception occurred
     */
    public static Result handleConsumeQueue(String username, UserRealm userRealm,
                                            ObjectProperties properties, Operation operation)
            throws AndesAuthorizationHandlerException {
        Result accessResult = Result.DENIED;
        if (null == userRealm) {
            accessResult = Result.DENIED;
        } else {

            // Queue properties
            String routingKey = getRawQueueName(properties.get(ObjectProperties.Property.NAME));
            String queueID = CommonsUtil.getQueueID(routingKey);

            try {
                if (!isOwnDomain(properties.get(ObjectProperties.Property.NAME), userRealm, properties)) {
                    accessResult = Result.DENIED;

                    // authorise if admin user
                } else if (isAdmin(username, userRealm)) {
                    accessResult = Result.ALLOWED;

                    // authorise consume
                } else if (userRealm.getAuthorizationManager().isUserAuthorized(
                        username, queueID, TreeNode.Permission.CONSUME.toString().toLowerCase())) {
                    accessResult = Result.ALLOWED;

                    //check whether topic subscriber
                } else if (isTopicSubscriberQueue(properties.get(ObjectProperties.Property.NAME)) &&
                        !Boolean.valueOf(properties.get(ObjectProperties.Property.DURABLE))) {
                    //retrieve topic passing tmp queue name
                    String topicName = temporaryQueueToTopicMap.get(properties.get(ObjectProperties.Property.NAME));
                    if (null != topicName) {
                        //get resource path of topic
                        String topicId = CommonsUtil.getTopicID(RegistryClient.getTenantBasedTopicName(topicName));
                        //check user has subscriber permission to given topic
                        if (isAuthorizeToParentInHierarchy(username, userRealm, topicId,
                                TreeNode.Permission.SUBSCRIBE)) {
                            accessResult = Result.ALLOWED;
                        }
                    }
                }
            } catch (UserStoreException | RegistryClientException e) {
                throw new AndesAuthorizationHandlerException("Error handling consume queue.", e);
            }
        }
        if (log.isDebugEnabled()) {
            if (isTopicSubscriberQueue(properties.get(ObjectProperties.Property.NAME)) &&
                    !Boolean.valueOf(properties.get(ObjectProperties.Property.DURABLE))) {
                String topicName = temporaryQueueToTopicMap.get(properties.get(ObjectProperties.Property.NAME));
                log.debug(username + SPACE + accessResult.toString().toLowerCase() + SPACE + operation.toString() + SPACE
                        + topicName);
            } else {
                log.debug(username + SPACE + accessResult.toString().toLowerCase() + SPACE + operation.toString() + SPACE
                        + properties.get(ObjectProperties.Property.NAME));
            }
        }
        return accessResult;
    }

    /**
     * Evaluate whether the user has browsing permission for a queue
     *
     * @param username   User who is trying to consume the queue
     * @param userRealm  User's Realm
     * @param properties NAME, OWNER, TEMPORARY
     * @return ALLOWED/DENIED
     * @throws AndesAuthorizationHandlerException if user store exception occurred
     */
    public static Result handleBrowseQueue(String username, UserRealm userRealm,
                                            ObjectProperties properties, Operation operation)
            throws AndesAuthorizationHandlerException {
        Result accessResult = Result.DENIED;
        if (null == userRealm) {
            accessResult = Result.DENIED;
        } else {

            try {
                // authorise if admin user
                if (!isOwnDomain(properties.get(ObjectProperties.Property.NAME), userRealm, properties)) {
                    accessResult = Result.DENIED;
                } else if (isAdmin(username, userRealm)) {
                    accessResult = Result.ALLOWED;

                    // authorise browse
                } else if (userRealm.getAuthorizationManager().isUserAuthorized(
                        username, PERMISSION_ADMIN_MANAGE_QUEUE_BROWSE, UI_EXECUTE)) {
                    accessResult = Result.ALLOWED;

                }
                // if non of the above deny permission
                return accessResult;
            } catch (UserStoreException e) {
                throw new AndesAuthorizationHandlerException("Error handling browse queue.", e);
            }
        }
        if (log.isDebugEnabled()) {
            log.debug(username + SPACE + accessResult.toString().toLowerCase() + SPACE + operation.toString() + SPACE
                    + properties.get(ObjectProperties.Property.NAME));
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
     * @throws AndesAuthorizationHandlerException if user store or registry exception occurred
     */
    public static Result handleBindQueue(String username, UserRealm userRealm,
                                         ObjectProperties properties, Operation operation)
            throws AndesAuthorizationHandlerException {
        Result accessResult = Result.DENIED;
        try {
            if (null != userRealm) {
                // Bind properties
                String exchangeName =
                        getRawExchangeName(properties.get(ObjectProperties.Property.NAME));
                String queueName =
                        getRawQueueName(properties.get(ObjectProperties.Property.QUEUE_NAME));
                String routingKey =
                        getRawRoutingKey(properties.get(ObjectProperties.Property.ROUTING_KEY));

                String queueID = CommonsUtil.getQueueID(queueName);
                String topicId = CommonsUtil.getTopicID(RegistryClient.getTenantBasedTopicName(routingKey));

                switch (exchangeName) {
                    case DEFAULT_EXCHANGE:

                        // Authorize
                        if (!isOwnDomain(routingKey, userRealm, properties)) {
                            accessResult = Result.DENIED;
                        } else if (isAdmin(username, userRealm)) {
                            accessResult = Result.ALLOWED;
                        } else if (userRealm.getAuthorizationManager().isUserAuthorized(
                                username, queueID,
                                TreeNode.Permission.CONSUME.toString().toLowerCase())) {
                            accessResult = Result.ALLOWED;
                        }  else if (userRealm.getAuthorizationManager().isUserAuthorized(
                                username, queueID,
                                TreeNode.Permission.PUBLISH.toString().toLowerCase())) {
                            accessResult = Result.ALLOWED;
                        }  else if (userRealm.getAuthorizationManager()
                                             .isUserAuthorized(username,
                                                               PERMISSION_ADMIN_MANAGE_QUEUE_ADD,
                                                               UI_EXECUTE)) {
                            accessResult = Result.ALLOWED;
                        }
                        else if (Boolean.valueOf(properties.get(ObjectProperties.Property.DURABLE)) &&
                                Boolean.valueOf(properties.get(ObjectProperties.Property.EXCLUSIVE))) {
                            accessResult = Result.ALLOWED;
                        } else if (isTopicSubscriberQueue(queueName) && !Boolean.valueOf(
                                properties.get(ObjectProperties.Property.DURABLE))) {
                            accessResult = Result.ALLOWED;
                        }
                        if (log.isDebugEnabled()) {
                            log.debug(username + SPACE + accessResult.toString().toLowerCase() + SPACE
                                    + operation.toString() + SPACE
                                    + properties.get(ObjectProperties.Property.QUEUE_NAME));
                        }
                        break;

                    case DIRECT_EXCHANGE:

                        // Authorize
                        if (!isOwnDomain(routingKey, userRealm, properties)) {
                            accessResult = Result.DENIED;
                        } else if (isAdmin(username, userRealm)) {
                            accessResult = Result.ALLOWED;
                        } else if (userRealm.getAuthorizationManager().isUserAuthorized(
                                username, queueID,
                                TreeNode.Permission.CONSUME.toString().toLowerCase())) {
                            accessResult = Result.ALLOWED;
                        } else if (userRealm.getAuthorizationManager().isUserAuthorized(
                                username, queueID,
                                TreeNode.Permission.PUBLISH.toString().toLowerCase())) {
                            accessResult = Result.ALLOWED;
                        } else if (userRealm.getAuthorizationManager()
                                            .isUserAuthorized(username,
                                                              PERMISSION_ADMIN_MANAGE_QUEUE_ADD,
                                                              UI_EXECUTE)) {
                            accessResult = Result.ALLOWED;
                        }
                        if (log.isDebugEnabled()) {
                            log.debug(username + SPACE + accessResult.toString().toLowerCase() + SPACE
                                    + operation.toString() + SPACE
                                    + properties.get(ObjectProperties.Property.QUEUE_NAME));
                        }
                        break;

                    case TOPIC_EXCHANGE:

                        String newRoutingKey = routingKey.replace("@", AT_REPLACE_CHAR);
                        String roleName = UserCoreUtil.addInternalDomainName(TOPIC_ROLE_PREFIX +
                                newRoutingKey.replace(".*", "").replace(".#", "")
                                        .replace(".","-").replace("/", "-"));
                        UserStoreManager userStoreManager = userRealm.getUserStoreManager();
                        String newQName = queueName.replace("@", AT_REPLACE_CHAR);
                        // Authorize
                        if (!isOwnDomain(routingKey, userRealm, properties)) {
                            accessResult = Result.DENIED;
                        } else if (isAdmin(username, userRealm)) {

                            // Store subscription
                            RegistryClient.createSubscription(newRoutingKey, newQName, username);
                            temporaryQueueToTopicMap.put(queueName, routingKey);

                            //get admin role of admin user (super tenant admin or tenant admin)
                            String[] userRoles = userRealm.getUserStoreManager().getRoleListOfUser(username);
                            String adminRole = userRealm.getRealmConfiguration().getAdminRoleName();
                            String role = null;
                            for (String userRole : userRoles) {
                                if (userRole.equals(adminRole)) {
                                    role = userRole;
                                    break;
                                }
                            }

                            //grant permission to topic hierarchy
                            grantPermissionToHierarchyLevel(username, userRealm, topicId, role);


                            accessResult = Result.ALLOWED;
                        } else if (!userStoreManager.isExistingRole(roleName)
                                   && !userRealm.getAuthorizationManager().isUserAuthorized(username,
                                        topicId, TreeNode.Permission.SUBSCRIBE.toString().toLowerCase())
                                   && !userRealm.getAuthorizationManager().isUserAuthorized(username,
                                        topicId, TreeNode.Permission.PUBLISH.toString().toLowerCase())
                                   && userRealm.getAuthorizationManager().isUserAuthorized(username,
                                        PERMISSION_ADMIN_MANAGE_TOPIC_ADD, UI_EXECUTE)) {
                            //If topic created by admin then we don't create internal role. Therefore we have to check
                            //given topic permission assigned to admin. If admin has permission, then we not allow to
                            //other user to authorize.
                            boolean isAdminAuthorized = false;
                            if (RegistryClient.isResourceExist(CommonsUtil
                                    .getTopicID(RegistryClient.getTenantBasedTopicName(newRoutingKey)))) {
                                isAdminAuthorized = true;
                            }

                            //This is triggered when a topic is created. So the user who creates the
                            // topic will get publish/subscribe permissions
                            if (!isAdminAuthorized) {
                                // Store subscription
                                RegistryClient.createSubscription(newRoutingKey, newQName, username);

                                authorizeTopicPermissionsToLoggedInUser(username, newRoutingKey, topicId,
                                        queueName, userRealm);
                                accessResult = Result.ALLOWED;
                            }

                        } //check that user authorize to parent of topic hierarchy
                        else if (userRealm.getAuthorizationManager().isUserAuthorized(username,
                                topicId, TreeNode.Permission.SUBSCRIBE.toString().toLowerCase())) {

                                //This is triggered when a new subscriber is arrived when the topic
                                // has already been created

                                // Store subscription
                                RegistryClient.createSubscription(newRoutingKey, newQName, username);

                                if (isTopicSubscriberQueue(queueName) &&
                                        !Boolean.valueOf(properties.get(ObjectProperties.Property.DURABLE))) {
                                    //if user has consume permission to topic then map tmp queue with topic name
                                    // because in consume we are getting only tmp queue name
                                    temporaryQueueToTopicMap.put(queueName, routingKey);
                                } else {
                                    //Giving permissions for the durable topic queue because this has to be persist in
                                    //permission table. We need to handle durable subscription even server shutdown and
                                    //start again. We cannot maintain durable subscription queue permission as above in memory.
                                    String[] userRoles = userRealm.getUserStoreManager().getRoleListOfUser(username);
                                    for (String userRole : userRoles) {
                                        if (userRealm.getAuthorizationManager().isRoleAuthorized(
                                                userRole, topicId, TreeNode.Permission.SUBSCRIBE.toString().toLowerCase())) {
                                            authorizeRoleToPublishConsume(userRealm, userRole, queueID);
                                        }

                                    }

                                }
                                accessResult = Result.ALLOWED;

                        } else if (userRealm.getAuthorizationManager().isUserAuthorized(username,
                                PERMISSION_ADMIN_MANAGE_TOPIC_ADD, UI_EXECUTE)) {
                            accessResult = Result.ALLOWED;
                        } else if (userRealm.getAuthorizationManager().isUserAuthorized(username,
                                topicId, TreeNode.Permission.PUBLISH.toString().toLowerCase())) {
                            accessResult = Result.ALLOWED;
                        }
                        if (log.isDebugEnabled()) {
                            log.debug(username + SPACE + accessResult.toString().toLowerCase() + SPACE
                                    + operation.toString() + SPACE
                                    + properties.get(ObjectProperties.Property.ROUTING_KEY));
                        }
                        break;
                }
            }
        } catch (UserStoreException | RegistryClientException e) {
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
     * @throws AndesAuthorizationHandlerException if user store or registry exception occurred
     */
    public static Result handlePublishToExchange(String username, UserRealm userRealm,
                                                 ObjectProperties properties, Operation operation)
            throws AndesAuthorizationHandlerException {
        Result accessResult = Result.DENIED;
        try {
            if (null != userRealm) {

                // Exchange properties
                String exchangeName =
                        getRawExchangeName(properties.get(ObjectProperties.Property.NAME));
                String routingKey =
                        getRawRoutingKey(properties.get(ObjectProperties.Property.ROUTING_KEY));

                String queueID = CommonsUtil.getQueueID(routingKey);
                String permissionID = CommonsUtil.getTopicID(RegistryClient.getTenantBasedTopicName(routingKey));

                switch (exchangeName) {
                    case DIRECT_EXCHANGE:   // Publish to queue

                        // Authorize admin user
                        if (!isOwnDomain(routingKey, userRealm, properties)) {
                            accessResult = Result.DENIED;
                        } else if (isAdmin(username, userRealm)) {
                            accessResult = Result.ALLOWED;
                        } else if (userRealm.getAuthorizationManager().isUserAuthorized(
                                username, queueID,
                                TreeNode.Permission.PUBLISH.toString().toLowerCase())) {
                            accessResult = Result.ALLOWED;
                        }
                        break;

                    case TOPIC_EXCHANGE:    // Publish to topic

                        // Authorize admin user
                        if (!isOwnDomain(routingKey, userRealm, properties)) {
                            accessResult = Result.DENIED;
                        } else if (isAdmin(username, userRealm)) {
                            accessResult = Result.ALLOWED;
                        } else if (isAuthorizeToParentInHierarchy(username, userRealm, permissionID,
                                TreeNode.Permission.PUBLISH)) {
                            accessResult = Result.ALLOWED;
                        }
                        break;
                    case DEFAULT_EXCHANGE:   // Publish to queue

                        // Authorize
                        if (!isOwnDomain(routingKey, userRealm, properties)) {
                            accessResult = Result.DENIED;
                        } else if (isAdmin(username, userRealm)) {
                            accessResult = Result.ALLOWED;
                        } else if (userRealm.getAuthorizationManager().isUserAuthorized(
                                username, queueID,
                                TreeNode.Permission.PUBLISH.toString().toLowerCase())) {
                            accessResult = Result.ALLOWED;
                        }
                        break;

                }
            }
        } catch (UserStoreException e) {
            throw new AndesAuthorizationHandlerException("Error handling publish queue.", e);
        } catch (RegistryClientException e) {
            throw new AndesAuthorizationHandlerException("Error checking permission hierarchy", e);
        }
        if (log.isDebugEnabled()) {
            log.debug(username + SPACE + accessResult.toString().toLowerCase() + SPACE
                    + operation.toString() + SPACE
                    + properties.get(ObjectProperties.Property.ROUTING_KEY));
        }
        return accessResult;
    }

    /**
     * Evaluates whether the user has unbind permissions for an exchange.
     *
     * @param properties NAME, QUEUE_NAME, ROUTING_KEY
     * @return ALLOWED/DENIED
     * @throws AndesAuthorizationHandlerException if user store or registry exception occurred
     */
    public static Result handleUnbindQueue(String username, UserRealm userRealm, ObjectProperties properties,
                                           Operation operation) throws AndesAuthorizationHandlerException {
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
                // delete tmp queue to topic mapping
                temporaryQueueToTopicMap.remove(queueName);
                //clear permission assigned to admin role when unbind from topic
                if (isAdmin(username, userRealm)) {
                    //get admin role of admin user (super tenant admin or tenant admin)
                    String[] userRoles = userRealm.getUserStoreManager().getRoleListOfUser(username);
                    String adminRole = userRealm.getRealmConfiguration().getAdminRoleName();
                    String role = null;
                    for (String userRole : userRoles) {
                        if (userRole.equals(adminRole)) {
                            role = userRole;
                            break;
                        }
                    }
                    String queueID = CommonsUtil.getQueueID(queueName);
                    String topicId = CommonsUtil.getTopicID(RegistryClient.getTenantBasedTopicName(routingKey));
                    userRealm.getAuthorizationManager().clearRoleAuthorization(role, queueID,
                            TreeNode.Permission.CONSUME.toString().toLowerCase());
                    userRealm.getAuthorizationManager().clearRoleAuthorization(role, topicId,
                            TreeNode.Permission.SUBSCRIBE.toString().toLowerCase());
                    userRealm.getAuthorizationManager().clearRoleAuthorization(role, topicId,
                            TreeNode.Permission.PUBLISH.toString().toLowerCase());
                    userRealm.getAuthorizationManager().clearRoleAuthorization(role, topicId,
                            PERMISSION_CHANGE_PERMISSION);
                }
            }
            if (log.isDebugEnabled()) {
                log.debug(username + SPACE + Result.ALLOWED.toString().toLowerCase() + SPACE
                        + operation.toString() + SPACE
                        + properties.get(ObjectProperties.Property.ROUTING_KEY));
            }
            return Result.ALLOWED;
        } catch (RegistryClientException | UserStoreException e) {
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
     * @throws AndesAuthorizationHandlerException if user store or registry exception occurred
     */
    public static Result handleDeleteQueue(String username, UserRealm userRealm,
                                           ObjectProperties properties, Operation operation)
            throws AndesAuthorizationHandlerException {
        Result accessResult = Result.DENIED;
        try {
            if (null != userRealm) {
                String queueName = getRawQueueName(properties.get(ObjectProperties.Property.NAME));
                if (!isOwnDomain(queueName, userRealm, properties)) {
                    accessResult = Result.DENIED;
                } else if (isAdmin(username, userRealm)) {
                    deleteQueueFromRegistry(queueName);
                    accessResult = Result.ALLOWED;
                } else if (userRealm.getAuthorizationManager().isUserAuthorized(username,
                        PERMISSION_ADMIN_MANAGE_QUEUE_DELETE, UI_EXECUTE)) {
                    deleteQueueFromRegistry(queueName);
                    accessResult = Result.ALLOWED;
                } else if (userRealm.getAuthorizationManager().isUserAuthorized(username,
                        PERMISSION_ADMIN_MANAGE_TOPIC_DELETE, UI_EXECUTE)) {
                    deleteQueueFromRegistry(queueName);
                    accessResult = Result.ALLOWED;
                } else if (Boolean.valueOf(properties.get(ObjectProperties.Property.DURABLE)) &&
                        Boolean.valueOf(properties.get(ObjectProperties.Property.EXCLUSIVE))) {
                    deleteQueueFromRegistry(queueName);
                    accessResult = Result.ALLOWED;
                } else if (isTopicSubscriberQueue(queueName) &&
                                !Boolean.valueOf(properties.get(ObjectProperties.Property.DURABLE))) {
                    deleteQueueFromRegistry(queueName);
                    accessResult = Result.ALLOWED;
                }
            }
        } catch (RegistryClientException | UserStoreException e) {
            throw new AndesAuthorizationHandlerException("Error handling delete queue.", e);
        }
        if (log.isDebugEnabled()) {
            log.debug(username + SPACE + accessResult.toString().toLowerCase() + SPACE
                    + operation.toString() + SPACE
                    + properties.get(ObjectProperties.Property.NAME));
        }
        return accessResult;
    }

    /**
     * This method handles the deletion of messages of a topic or queue. The deletion of messages is
     * only allowed if the permissions for the user exists.
     *
     * @param username  User who is trying to publish
     * @param userRealm User's Realm that represents the user store
     * @param properties NAME, OWNER, DURABLE
     * @return ALLOWED/DENIED
     * @throws AndesAuthorizationHandlerException if user store or registry exception occurred
     */
    public static Result handlePurgeQueue(String username, UserRealm userRealm, ObjectProperties properties,
                                          Operation operation) throws AndesAuthorizationHandlerException {
        Result accessResult = Result.DENIED;
        try {
            if (null != userRealm) {
                String queueName = getRawQueueName(properties.get(ObjectProperties.Property.NAME));
                if (isAdmin(username, userRealm)) {
                    deleteQueueFromRegistry(queueName);
                    accessResult = Result.ALLOWED;
                } else if (userRealm.getAuthorizationManager().isUserAuthorized(username,
                        PERMISSION_ADMIN_MANAGE_QUEUE_PURGE, UI_EXECUTE)) {
                    deleteQueueFromRegistry(queueName);
                    accessResult = Result.ALLOWED;
                } else if (Boolean.valueOf(properties.get(ObjectProperties.Property.DURABLE)) &&
                        Boolean.valueOf(properties.get(ObjectProperties.Property.EXCLUSIVE))) {
                    deleteQueueFromRegistry(queueName);
                    accessResult = Result.ALLOWED;
                } else if (isTopicSubscriberQueue(queueName) &&
                        !Boolean.valueOf(properties.get(ObjectProperties.Property.DURABLE))) {
                    deleteQueueFromRegistry(queueName);
                    accessResult = Result.ALLOWED;
                }
            }
        } catch (RegistryClientException | UserStoreException e) {
            throw new AndesAuthorizationHandlerException("Error handling purge queue.", e);
        }
        if (log.isDebugEnabled()) {
            log.debug(username + SPACE + accessResult.toString().toLowerCase() + SPACE
                    + operation.toString() + SPACE
                    + properties.get(ObjectProperties.Property.NAME));
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
     * @throws RegistryClientException if registry exception occurred
     * @throws UserStoreException if user store exception occurred
     */
    private static void registerAndAuthorizeQueue(String username, UserRealm userRealm,
                                                  ObjectProperties properties)
            throws RegistryClientException, UserStoreException {
        String queueName =
                getRawQueueName(properties.get(ObjectProperties.Property.NAME));
        if (isOwnDomain(properties.get(ObjectProperties.Property.NAME), userRealm, properties)) {

            //For registry we use a modified queue name
            String newQueueName = queueName.replace("@", AT_REPLACE_CHAR);
            // Store queue details
            RegistryClient.createQueue(newQueueName, username);

            if (log.isDebugEnabled()) {
                log.debug(queueName + " created in the registry");
            }

            String queueID = CommonsUtil.getQueueID(queueName);

            //we avoid creating role for non durable topic subscriber temporary queue and durable topic subscriber
            //subscription id queue
            boolean isCreateRole = true;
            if (Boolean.valueOf(properties.get(ObjectProperties.Property.DURABLE)) &&
                    Boolean.valueOf(properties.get(ObjectProperties.Property.EXCLUSIVE))) {
                isCreateRole = false;
            } else if (isTopicSubscriberQueue(properties.get(ObjectProperties.Property.NAME)) &&
                    !Boolean.valueOf(properties.get(ObjectProperties.Property.DURABLE))) {
                isCreateRole = false;
            } else if (isAdmin(username, userRealm)) {
                isCreateRole = false;
            }
            if (isCreateRole) {
                authorizeQueuePermissionsToLoggedInUser(username, newQueueName, queueID,
                        userRealm);
            }
        }
    }

    /**
     * Check if the current user is tenant Admin user
     *
     * @param username
     *         Username
     * @param userRealm
     *         User's Realm
     * @return True if the user is the admin user of the given domain
     * @throws UserStoreException if user store exception occurred
     */
    private static boolean isAdmin(String username, UserRealm userRealm)
            throws UserStoreException {
        boolean isAdmin = false;

        String[] userRoles = userRealm.getUserStoreManager().getRoleListOfUser(username);
        String adminRole = userRealm.getRealmConfiguration().getAdminRoleName();
        for (String userRole : userRoles) {
            if (userRole.equals(adminRole)) {
                isAdmin = true;
                break;
            }
        }

        return isAdmin;
    }

    /**
     * Remove queue from registry and un-assign role from queue
     *
     * @param queueName queue name to unregister
     * @throws RegistryClientException if registry exception occurred
     * @throws UserStoreException if user store exception occurred
     */
    private static void deleteQueueFromRegistry(String queueName)
            throws RegistryClientException, UserStoreException {
        // Modifying queue name for registry
        String newQName = queueName.replace("@", AT_REPLACE_CHAR);

        // Delete queue details
        RegistryClient.deleteQueue(queueName);

        if (log.isDebugEnabled()) {
            log.debug(queueName + " deleted from the registry");
        }

        if (!AndesKernelBoot.isKernelShuttingDown()) {
            // Deleting internal role created for user.
            removeQueueRoleCreateForLoggedInUser(newQName);
        }
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
        String virtualHostFormatted = "carbon" + ":";
        int startIndex = !routingKey.contains(virtualHostFormatted) ? 0 : routingKey.indexOf(virtualHostFormatted);
        return routingKey.substring(startIndex, routingKey.length());
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
     * @param routingKey   - queue/topic name to be verified against tenantDomain
     * @param  userRealm - User's Realm
     * @param properties OWNER, TEMPORARY, EXCLUSIVE
     * @return true if queue/topic belongs to given domain and false otherwise
     */
    private static boolean isOwnDomain(String routingKey, UserRealm userRealm, ObjectProperties properties) throws UserStoreException {
        boolean isOwnDomain = false;
        RealmService realmService = AuthorizationServiceDataHolder.getInstance().getRealmService();
        String tenantDomain = realmService.getTenantManager().getDomain(userRealm.getAuthorizationManager().getTenantId());
        if (tenantDomain != null) {
            //first we check whether routing key - queue/topic name is valid i.e. tenant_domain/routing_key or routing_key
            if(!(routingKey.substring(routingKey.contains("/") ? routingKey.indexOf("/") + 1 : 0).isEmpty())) {
                if ((routingKey.length() >= tenantDomain.length() + 1) && routingKey.substring(0,
                        tenantDomain.length() + 1).equals(tenantDomain + "/")) {
                    isOwnDomain = true;
                } else if (tenantDomain.equalsIgnoreCase(MultitenantConstants.SUPER_TENANT_DOMAIN_NAME)) {
                    if (!routingKey.contains("/")) {
                        isOwnDomain = true;
                    }
                } else if (isTopicSubscriberQueue(routingKey) &&
                        !Boolean.valueOf(properties.get(ObjectProperties.Property.DURABLE))) {
                    //allow permission to non durable topics to create temporary queue i.e. tmp_127_0_0_1_46981_1
                    isOwnDomain = true;
                } else if (Boolean.valueOf(properties.get(ObjectProperties.Property.DURABLE)) &&
                        Boolean.valueOf(properties.get(ObjectProperties.Property.EXCLUSIVE))) {
                    //only sub tenant call comes into this block, super tenant call would evaluate in above
                    //allow permission only if subscription id prefix with tenant domain
                    //i.e. {tenant_domain}/{subscription_id}
                    if (routingKey.substring(routingKey.lastIndexOf(":") + 1).startsWith(tenantDomain)) {
                        isOwnDomain = true;
                    }
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
     * Create a new role which has the same name as the queueName and assign the logged in
     * user to the newly created role. Then, authorize the newly created role to subscribe and
     * publish to the queue.
     *
     * @param username  name of the logged in user
     * @param queueName queue name
     * @param queueId   ID given to the queue
     * @param userRealm User's Realm
     * @throws UserStoreException if user store exception occurred
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

        String roleName = UserCoreUtil.addInternalDomainName(QUEUE_ROLE_PREFIX
                + queueName.replace(".","-").replace("/", "-"));

        UserStoreManager userStoreManager = userRealm.getUserStoreManager();

        if (!userStoreManager.isExistingRole(roleName)) {
            String[] user = {MultitenantUtils.getTenantAwareUsername(username)};
            userStoreManager.addRole(roleName, user, null);
            authorizeRoleToPublishConsume(userRealm, roleName, queueId);
            if (log.isDebugEnabled()) {
                log.debug("permission granted to user = " + username + " role = " + roleName
                        + " queue = " + queueName + " queueId = " + queueId);
            }
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
     * @param queueName   temp queue name
     * @param userRealm   User's Realm
     * @throws UserStoreException if user store exception occurred
     */
    private static void authorizeTopicPermissionsToLoggedInUser(String username, String topicName,
                                                                String topicId, String queueName,
                                                                UserRealm userRealm)
                                                                    throws UserStoreException {

        String roleName = UserCoreUtil.addInternalDomainName(TOPIC_ROLE_PREFIX +
                                                             topicName.replace(".*", "").replace(".#", "")
                                                                     .replace(".","-").replace("/", "-"));
        UserStoreManager userStoreManager = userRealm.getUserStoreManager();
        String[] user = {MultitenantUtils.getTenantAwareUsername(username)};
        String tempQueueId = CommonsUtil.getQueueID(queueName);

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
        //giving permissions to the topic
        grantPermissionToHierarchyLevel(username, userRealm, topicId, roleName);

        if (isTopicSubscriberQueue(queueName)) {
            //if user has add topic permission then map tmp queue with topic name because in
            //consume we are getting only tmp queue name
            temporaryQueueToTopicMap.put(queueName, topicName);
        } else {
            //Giving permissions for the durable topic queue because this has to be persist in permission table.
            //We need to handle durable subscription even server shutdown and start again. We cannot maintain durable
            //subscription queue permission as above in memory.
            authorizeRoleToPublishConsume(userRealm, roleName, tempQueueId);
        }
        if (log.isDebugEnabled()) {
            log.debug("permission granted to user = " + username + " role = " + roleName
                    + " topic = " + topicName + " topicId = " + topicId);
        }
    }

    /**
     * Every queue has a role with the name QUEUE_ROLE_PREFIX+queueName. This role is used
     * to store the permissions for the user who created the queue.This role should be
     * deleted when the queue/topic is deleted.
     *
     * @param queueName name of the queue or topic
     * @throws UserStoreException if user store exception occurred
     */
    private static void removeQueueRoleCreateForLoggedInUser(String queueName)
            throws UserStoreException {
        String roleName = UserCoreUtil.addInternalDomainName(QUEUE_ROLE_PREFIX +
                                                             queueName.replace(".","-").replace("/", "-"));

        AuthorizationManager authorizationManager = CarbonContext.getThreadLocalCarbonContext()
                .getUserRealm().getAuthorizationManager();
        UserStoreManager userStoreManager = CarbonContext.getThreadLocalCarbonContext()
                .getUserRealm().getUserStoreManager();

        if (userStoreManager.isExistingRole(roleName)) {
            userStoreManager.deleteRole(roleName);
            authorizationManager.clearResourceAuthorizations(CommonsUtil.getQueueID(queueName));
        }
        if (log.isDebugEnabled()) {
            log.debug("role " + roleName + " associated with queue " + queueName + " deleted");
        }
    }

    /**
     * Admin user and user who had add topic permission create the hierarchy topic get permission to all level by default
     *
     * @param userRealm User's Realm
     * @param topicId topic id
     * @param role admin role
     * @throws UserStoreException if user store exception occrred
     */
    private static void grantPermissionToHierarchyLevel(String username, UserRealm userRealm, String topicId, String role)
            throws UserStoreException {
        //tokenize resource path
        StringTokenizer tokenizer = new StringTokenizer(topicId, "/");
        StringBuilder resourcePathBuilder = new StringBuilder();
        //get token count
        int tokenCount = tokenizer.countTokens();
        int count = 0;
        Pattern pattern = Pattern.compile(PARENT_RESOURCE_PATH);

        while (tokenizer.hasMoreElements()) {
            //get each element in topicId resource path
            String resource = tokenizer.nextElement().toString();
            //build resource path again
            resourcePathBuilder.append(resource);
            //we want to give permission to any resource after event/topics/ in build resource path
            Matcher matcher = pattern.matcher(resourcePathBuilder.toString());
            if (matcher.find()) {
                authorizeRole(userRealm, role, resourcePathBuilder.toString(),
                        TreeNode.Permission.SUBSCRIBE.toString().toLowerCase());
                authorizeRole(userRealm, role, resourcePathBuilder.toString(),
                        TreeNode.Permission.PUBLISH.toString().toLowerCase());
                authorizeRole(userRealm, role, resourcePathBuilder.toString(),
                        PERMISSION_CHANGE_PERMISSION);
            }
            count++;
            if (count < tokenCount) {
                resourcePathBuilder.append("/");
            }
            if (log.isDebugEnabled()) {
                log.debug("permission granted to user = " + username + " role = " + role
                        + " hierarchical topic = " + resourcePathBuilder.toString());
            }

        }
    }

    /**
     * We want to check user is authorize for parent topic in case of hierarchical topic
     * i.e. Let's say there is topic hierarchical topic country.province.city1
     * user who has permission to either country or province should be able to subscribe or
     * publish to city1 as well.
     *
     * @param username username of logged user
     * @param userRealm User's Realm
     * @param topicId topic id
     * @param permission The permission type to check for
     *
     * @return is user authorize
     * @throws UserStoreException if user store exception occurred
     */
    private static boolean isAuthorizeToParentInHierarchy(String username, UserRealm userRealm, String topicId, TreeNode.Permission permission)
            throws UserStoreException, RegistryClientException {

        //check given resource path exist before check permission in parent hierarchy
        if (!RegistryClient.isResourceExist(topicId)) {
            return false;
        }
        //tokenize resource path
        StringTokenizer tokenizer = new StringTokenizer(topicId, "/");
        StringBuilder resourcePathBuilder = new StringBuilder();
        //get token count
        int tokenCount = tokenizer.countTokens();
        int count = 0;
        boolean userAuthorized = false;
        Pattern pattern = Pattern.compile(PARENT_RESOURCE_PATH);

        while (tokenizer.hasMoreElements()) {
            //get each element in topicId resource path
            String resource = tokenizer.nextElement().toString();
            //build resource path again
            resourcePathBuilder.append(resource);
            //we want to check that build resource path has permission to any resource
            // after event/topics/
            Matcher matcher = pattern.matcher(resourcePathBuilder.toString());
            if (matcher.find()) {
                if (userRealm.getAuthorizationManager().isUserAuthorized(username,
                        resourcePathBuilder.toString(), permission.toString().toLowerCase())) {
                    userAuthorized = true;
                    break;
                }

            }
            count++;
            if (count < tokenCount) {
                resourcePathBuilder.append("/");
            }

        }
        if (log.isDebugEnabled()) {
            log.debug(username + " is authorized to parent hierarchy topic = " + resourcePathBuilder.toString()
                    + " topicId = " + topicId + SPACE + userAuthorized);
        }
        return userAuthorized;
    }

    /**
     * Assign permissions given the action, resource path and the role to which permission should be assigned.
     *
     * Upon the failure of authorization, the operation will be re-attempted once since the failure could have been
     * caused by multiple authorizations that are happening concurrently.
     *
     * @param userRealm    the realm for to which the user belongs
     * @param role         the role to assign the permission
     * @param resourcePath resource to which the permission should be assigned
     * @param action       the permission that should be assigned
     * @throws UserStoreException if an exception occurs when adding permission
     */
    private static void authorizeRole(UserRealm userRealm, String role, String resourcePath, String action) throws
            UserStoreException {
        try {
            userRealm.getAuthorizationManager().authorizeRole(role, resourcePath, action);
        } catch (UserStoreException e) {
            log.warn("Could not authorize role: " + role + " to resourceID: " + resourcePath
                     + " for action: " + action + ". Hence, retrying authorization", e);
            userRealm.getAuthorizationManager().authorizeRole(role, resourcePath, action);
        }
    }

    /**
     * Add permission for a given role to publish and consume.
     *
     * @param userRealm    the realm for to which the user belongs
     * @param roleName     the role to assign the permission
     * @param resourcePath resource to which the permission should be assigned
     * @throws UserStoreException if an exception occurs when adding permission
     */
    private static void authorizeRoleToPublishConsume(UserRealm userRealm, String roleName, String resourcePath)
            throws UserStoreException {
        authorizeRole(userRealm, roleName, resourcePath, TreeNode.Permission.CONSUME.toString().toLowerCase());
        authorizeRole(userRealm, roleName, resourcePath, TreeNode.Permission.PUBLISH.toString().toLowerCase());
        authorizeRole(userRealm, roleName, resourcePath, PERMISSION_CHANGE_PERMISSION);
    }
}

