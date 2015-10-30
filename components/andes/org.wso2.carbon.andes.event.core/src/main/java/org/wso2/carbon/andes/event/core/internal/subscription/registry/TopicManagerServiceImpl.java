/*
 * Copyright 2004,2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.andes.event.core.internal.subscription.registry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.andes.event.core.TopicManagerService;
import org.wso2.carbon.andes.event.core.TopicNode;
import org.wso2.carbon.andes.event.core.TopicRolePermission;
import org.wso2.carbon.andes.event.core.exception.EventBrokerException;
import org.wso2.carbon.andes.event.core.internal.ds.EventBrokerHolder;
import org.wso2.carbon.andes.event.core.subscription.Subscription;
import org.wso2.carbon.andes.event.core.util.EventBrokerConstants;
import org.wso2.carbon.andes.event.core.internal.util.JavaUtil;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.registry.core.Collection;
import org.wso2.carbon.registry.core.Resource;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.user.api.AuthorizationManager;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.api.UserStoreManager;
import org.wso2.carbon.user.core.util.UserCoreUtil;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is utilized to perform actions related to topics.
 */
public class TopicManagerServiceImpl implements TopicManagerService {

    private static Log log = LogFactory.getLog(TopicManagerServiceImpl.class);
    private static final String AT_REPLACE_CHAR = "_";
    private static final String TOPIC_ROLE_PREFIX = "T_";
    private String topicStoragePath;
    private RegistryService registryService;
    /**
     * Permission value for changing permissions through UI.
     */
    private static final String UI_EXECUTE = "ui.execute";

    /**
     * permission path for adding a topic.
     */
    private static final String PERMISSION_ADMIN_MANAGE_TOPIC_ADD = "/permission/admin/manage/topic/add";

    /**
     * Permission path for deleting a topic.
     */
    private static final String PERMISSION_ADMIN_MANAGE_TOPIC_DELETE = "/permission/admin/manage/topic/delete";

    /**
     * Permission path for view topic details
     */
    private static final String PERMISSION_ADMIN_MANAGE_TOPIC_DETAILS = "/permission/admin/manage/topic/details";

    /**
     * Parent resource path of each topic
     */
    private static final String PARENT_RESOURCE_PATH = "\\bevent/topics/\\b";

    /**
     * Initializes Registry Topic Manager
     *
     * @param topicStoragePath the topic registry path
     */
    public TopicManagerServiceImpl(String topicStoragePath) {
        this.topicStoragePath = topicStoragePath;
        this.registryService = EventBrokerHolder.getInstance().getRegistryService();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TopicNode getTopicTree() throws EventBrokerException {
        try {
            UserRegistry userRegistry =
                    this.registryService.getGovernanceSystemRegistry(EventBrokerHolder.getInstance().getTenantId());
            if (!userRegistry.resourceExists(topicStoragePath)) {
                userRegistry.put(topicStoragePath, userRegistry.newCollection());
            }
            Resource root = userRegistry.get(this.topicStoragePath);
            TopicNode rootTopic = new TopicNode("/", "/");
            buildTopicTree(rootTopic, (Collection) root, userRegistry);
            return rootTopic;
        } catch (RegistryException e) {
            throw new EventBrokerException(e.getMessage(), e);
        }
    }

    /**
     * Building the topic tree
     *
     * @param topicNode    node of the topic
     * @param resource     the resource that holds child topics
     * @param userRegistry user registry
     * @throws EventBrokerException
     */
    private void buildTopicTree(TopicNode topicNode, Collection resource, UserRegistry userRegistry)
            throws EventBrokerException {
        try {
            String[] children = resource.getChildren();
            if (children != null) {
                List<TopicNode> nodes = new ArrayList<TopicNode>();
                for (String childTopic : children) {
                    Resource childResource = userRegistry.get(childTopic);
                    if (childResource instanceof Collection) {
                        if (childTopic.endsWith("/")) {
                            childTopic = childTopic.substring(0, childTopic.length() - 2);
                        }
                        String nodeName = childTopic.substring(childTopic.lastIndexOf("/") + 1);
                        if (!nodeName.equals(EventBrokerConstants.EB_CONF_WS_SUBSCRIPTION_COLLECTION_NAME) &&
                            !nodeName.equals(EventBrokerConstants.EB_CONF_JMS_SUBSCRIPTION_COLLECTION_NAME)) {
                            childTopic =
                                    childTopic.substring(childTopic.indexOf(this.topicStoragePath)
                                                         + this.topicStoragePath.length() + 1);
                            TopicNode childNode = new TopicNode(nodeName, childTopic);
                            nodes.add(childNode);
                            buildTopicTree(childNode, (Collection) childResource, userRegistry);
                        }
                    }
                }
                topicNode.setChildren(nodes.toArray(new TopicNode[nodes.size()]));
            }
        } catch (RegistryException e) {
            throw new EventBrokerException(e.getMessage(), e);
        }
    }

    /**
     * Adding topic, when creating topics from the user interface
     *
     * {@inheritDoc}
     */
    @Override
    public void addTopic(String topicName) throws EventBrokerException {

        String loggedInUser = CarbonContext.getThreadLocalCarbonContext().getUsername();
        UserRealm userRealm = CarbonContext.getThreadLocalCarbonContext().getUserRealm();

        try {
            UserRegistry userRegistry =
                    this.registryService.getGovernanceSystemRegistry(EventBrokerHolder.getInstance().getTenantId());
            String resourcePath = JavaUtil.getResourcePath(topicName, this.topicStoragePath);

            //we add the topic only if it does not exits. if the topic exists then
            //we don't do any thing.
            if (!userRegistry.resourceExists(resourcePath)) {
                Collection collection = userRegistry.newCollection();
                userRegistry.put(resourcePath, collection);

                //Internal role create by topic name and grant subscribe and publish permission to it
                //By this way we restricted permission to user who create topic and allow subscribe and publish
                //We avoid creating internal role if Admin user creating a topic
                //Admin has to give permission to other roles to subscribe and publish if necessary
                if (!JavaUtil.isAdmin(loggedInUser)) {
                    authorizePermissionsToLoggedInUser(loggedInUser, topicName, resourcePath, userRealm);
                } else {

                    //get admin role of admin user (super tenant admin or tenant admin)
                    String[] userRoles = userRealm.getUserStoreManager().getRoleListOfUser(loggedInUser);
                    String adminRole = userRealm.getRealmConfiguration().getAdminRoleName();
                    String role = null;
                    for (String userRole : userRoles) {
                        if (userRole.equals(adminRole)) {
                            role = userRole;
                            break;
                        }
                    }

                    // admin user who is in the same tenant domain get consume and publish permission for
                    // all hierarchy in topic
                    grantPermissionToHierarchyLevel(userRealm, resourcePath, role);
                }
            }
        } catch (RegistryException e) {
            throw new EventBrokerException("Cannot access the config registry", e);
        } catch (UserStoreException e) {
            throw new EventBrokerException("Error while granting user " + loggedInUser +
                                           ", permission " + EventBrokerConstants.EB_PERMISSION_CHANGE_PERMISSION +
                                           ", on topic " + topicName, e);
        }
    }

    /**
     * Admin user and user who had add topic permission create the hierarchy topic get permission to all level by default
     *
     * @param userRealm User's Realm
     * @param topicId topic id
     * @param role admin role
     * @throws UserStoreException
     */
    private static void grantPermissionToHierarchyLevel(UserRealm userRealm, String topicId, String role)
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
                // gives subscribe permissions to the internal role in the user store
                userRealm.getAuthorizationManager().authorizeRole(
                        role, resourcePathBuilder.toString(), EventBrokerConstants.EB_PERMISSION_SUBSCRIBE);
                // gives publish permissions to the internal role in the user store
                userRealm.getAuthorizationManager().authorizeRole(
                        role, resourcePathBuilder.toString(), EventBrokerConstants.EB_PERMISSION_PUBLISH);
                // gives change permissions to the internal role in the user store
                userRealm.getAuthorizationManager().authorizeRole(
                        role, resourcePathBuilder.toString(), EventBrokerConstants.EB_PERMISSION_CHANGE_PERMISSION);
            }
            count++;
            if (count < tokenCount) {
                resourcePathBuilder.append("/");
            }

        }
    }

    /**
     * Gets a topic name without the resource path
     *
     * @param topic topic name
     * @return a topic name
     */
    private String removeResourcePath(String topic) {
        String resourcePath = this.topicStoragePath;
        if (topic.contains(resourcePath)) {
            topic = topic.substring(topic.indexOf(resourcePath) + resourcePath.length());
        }
        return topic;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TopicRolePermission[] getTopicRolePermission(String topicName)
            throws EventBrokerException {
        String topicResourcePath = JavaUtil.getResourcePath(topicName, this.topicStoragePath);
        List<TopicRolePermission> topicRolePermissions = new ArrayList<TopicRolePermission>();
        UserRealm userRealm = CarbonContext.getThreadLocalCarbonContext().getUserRealm();
        String adminRole =
                EventBrokerHolder.getInstance().getRealmService().
                        getBootstrapRealmConfiguration().getAdminRoleName();
        TopicRolePermission topicRolePermission;
        try {
            for (String role : userRealm.getUserStoreManager().getRoleNames()) {
                // remove admin role and anonymous role related permissions
                if (!(role.equals(adminRole) ||
                      CarbonConstants.REGISTRY_ANONNYMOUS_ROLE_NAME.equals(role))) {
                    topicRolePermission = new TopicRolePermission();
                    topicRolePermission.setRoleName(role);
                    topicRolePermission.setAllowedToSubscribe(
                            userRealm.getAuthorizationManager().isRoleAuthorized(
                                    role, topicResourcePath, EventBrokerConstants.EB_PERMISSION_SUBSCRIBE));
                    topicRolePermission.setAllowedToPublish(
                            userRealm.getAuthorizationManager().isRoleAuthorized(
                                    role, topicResourcePath, EventBrokerConstants.EB_PERMISSION_PUBLISH));
                    topicRolePermissions.add(topicRolePermission);
                }
            }
            return topicRolePermissions.toArray(
                    new TopicRolePermission[topicRolePermissions.size()]);
        } catch (UserStoreException e) {
            throw new EventBrokerException("Cannot access the UserStore manager ", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updatePermissions(String topicName, TopicRolePermission[] topicRolePermissions)
            throws EventBrokerException {
        String topicResourcePath = JavaUtil.getResourcePath(topicName, this.topicStoragePath);
        UserRealm userRealm = CarbonContext.getThreadLocalCarbonContext().getUserRealm();
        String role;
        String loggedInUser = CarbonContext.getThreadLocalCarbonContext().getUsername();
        try {
            boolean isUserHasChangePermission = false;
            if (JavaUtil.isAdmin(loggedInUser)) {
                isUserHasChangePermission = true;
            } else {
                String[] userRoles = userRealm.getUserStoreManager().getRoleListOfUser(loggedInUser);
                for (String userRole : userRoles) {
                    if (userRealm.getAuthorizationManager().isRoleAuthorized(
                            userRole, topicResourcePath, EventBrokerConstants.EB_PERMISSION_CHANGE_PERMISSION)) {
                        isUserHasChangePermission = true;
                        break;
                    }
                }
            }
            if (!isUserHasChangePermission) {
                throw new EventBrokerException(" User " + loggedInUser + " cannot change" +
                        " the permissions of " + topicName);
            }
            for (TopicRolePermission topicRolePermission : topicRolePermissions) {
                role = topicRolePermission.getRoleName();
                if (topicRolePermission.isAllowedToSubscribe()) {
                    if (!userRealm.getAuthorizationManager().isRoleAuthorized(
                            role, topicResourcePath, EventBrokerConstants.EB_PERMISSION_SUBSCRIBE)) {
                        userRealm.getAuthorizationManager().authorizeRole(
                                role, topicResourcePath, EventBrokerConstants.EB_PERMISSION_SUBSCRIBE);
                    }
                } else {
                    if (userRealm.getAuthorizationManager().isRoleAuthorized(
                            role, topicResourcePath, EventBrokerConstants.EB_PERMISSION_SUBSCRIBE)) {
                        userRealm.getAuthorizationManager().denyRole(
                                role, topicResourcePath, EventBrokerConstants.EB_PERMISSION_SUBSCRIBE);
                    }
                }

                if (topicRolePermission.isAllowedToPublish()) {
                    if (!userRealm.getAuthorizationManager().isRoleAuthorized(
                            role, topicResourcePath, EventBrokerConstants.EB_PERMISSION_PUBLISH)) {
                        userRealm.getAuthorizationManager().authorizeRole(
                                role, topicResourcePath, EventBrokerConstants.EB_PERMISSION_PUBLISH);
                    }
                } else {
                    if (userRealm.getAuthorizationManager().isRoleAuthorized(
                            role, topicResourcePath, EventBrokerConstants.EB_PERMISSION_PUBLISH)) {
                        userRealm.getAuthorizationManager().denyRole(
                                role, topicResourcePath, EventBrokerConstants.EB_PERMISSION_PUBLISH);
                    }
                }
            }

        } catch (UserStoreException e) {
            throw new EventBrokerException("Cannot access the user store manager", e);
        }
    }

    /**
     * Gets the topic storage path
     *
     * @return the topic storage path
     */
    public String getTopicStoragePath() {
        return topicStoragePath;
    }

    /**
     * The topic storage path
     *
     * @param topicStoragePath path for topic storage
     */
    public void setTopicStoragePath(String topicStoragePath) {
        this.topicStoragePath = topicStoragePath;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Subscription[] getSubscriptions(String topicName,
                                           boolean withChildren) throws EventBrokerException {

        List<Subscription> subscriptions = new ArrayList<Subscription>();
        Queue<String> pathsQueue = new LinkedList<String>();
        String resourcePath = JavaUtil.getResourcePath(topicName, this.topicStoragePath);

        pathsQueue.add(resourcePath);
        while (!pathsQueue.isEmpty()) {
            addSubscriptions(pathsQueue.remove(), subscriptions, pathsQueue, withChildren);
        }

        return subscriptions.toArray(new Subscription[subscriptions.size()]);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Subscription[] getJMSSubscriptions(String topicName) throws EventBrokerException {
        try {
            Subscription[] subscriptionsArray = new Subscription[0];

            UserRegistry userRegistry =
                    this.registryService.getGovernanceSystemRegistry(EventBrokerHolder.getInstance().getTenantId());
            String resourcePath = JavaUtil.getResourcePath(topicName, this.topicStoragePath);
            if (!resourcePath.endsWith("/")) {
                resourcePath = resourcePath + "/";
            }
            resourcePath = resourcePath + EventBrokerConstants.EB_CONF_JMS_SUBSCRIPTION_COLLECTION_NAME;

            // Get subscriptions
            if (userRegistry.resourceExists(resourcePath)) {
                Collection subscriptionCollection = (Collection) userRegistry.get(resourcePath);
                subscriptionsArray =
                        new Subscription[subscriptionCollection.getChildCount()];

                int index = 0;
                for (String subs : subscriptionCollection.getChildren()) {
                    Collection subscription = (Collection) userRegistry.get(subs);

                    Subscription subscriptionDetails = new Subscription();
                    subscriptionDetails.setId(subscription.getProperty("Name"));
                    subscriptionDetails.setOwner(subscription.getProperty("Owner"));
                    subscriptionDetails.setCreatedTime(new Date(subscription.getCreatedTime().getTime()));
                    subscriptionsArray[index++] = subscriptionDetails;
                }
            }

            return subscriptionsArray;
        } catch (RegistryException e) {
            throw new EventBrokerException("Cannot read the registry resources ", e);
        }
    }

    /**
     * Adds a subscriptions to a list using the resource path provided
     *
     * @param resourcePath  the topic nam
     * @param subscriptions a list of subscriptions for the topic
     * @param pathsQueue    the topic folder
     * @param withChildren  to add subscriptions to children. i.e subtopics
     * @throws EventBrokerException
     */
    private void addSubscriptions(String resourcePath,
                                  List<Subscription> subscriptions,
                                  Queue<String> pathsQueue,
                                  boolean withChildren) throws EventBrokerException {

        try {
            UserRegistry userRegistry =
                    this.registryService.getGovernanceSystemRegistry(EventBrokerHolder.getInstance().getTenantId());
            String subscriptionsPath = getSubscriptionsPath(resourcePath);

            //first if there are subscriptions for this topic add them. else go to the other folders.
            if (userRegistry.resourceExists(subscriptionsPath)) {
                Collection collection = (Collection) userRegistry.get(subscriptionsPath);
                for (String subscriptionPath : collection.getChildren()) {
                    Resource subscriptionResource = userRegistry.get(subscriptionPath);
                    Subscription subscription = JavaUtil.getSubscription(subscriptionResource);
                    subscription.setTopicName(removeResourcePath(resourcePath));

                    if (subscriptionPath.endsWith("/")) {
                        subscriptionPath = subscriptionsPath.substring(0, subscriptionPath.lastIndexOf("/"));
                    }
                    subscription.setId(subscriptionPath.substring(subscriptionPath.lastIndexOf("/") + 1));
                    subscriptions.add(subscription);
                }
            }

            // add child subscriptions only for resource collections
            if (withChildren) {
                Resource resource = userRegistry.get(resourcePath);
                if (resource instanceof Collection) {
                    Collection childResources = (Collection) resource;
                    for (String childResourcePath : childResources.getChildren()) {
                        if ((!EventBrokerConstants.EB_CONF_WS_SUBSCRIPTION_COLLECTION_NAME
                                .contains(childResourcePath)) &&
                            (!EventBrokerConstants.EB_CONF_JMS_SUBSCRIPTION_COLLECTION_NAME
                                    .contains(childResourcePath))) {
                            // i.e. this folder is a topic folder
                            pathsQueue.add(childResourcePath);
                        }
                    }
                }
            }

        } catch (RegistryException e) {
            throw new EventBrokerException("Cannot access the registry", e);
        }
    }

    /**
     * Gets the subscription path for a topic
     *
     * @param topicName topic name
     * @return the subscription path as string
     */
    private String getSubscriptionsPath(String topicName) {

        if (!topicName.endsWith("/")) {
            topicName = topicName + "/";
        }

        topicName = topicName + EventBrokerConstants.EB_CONF_WS_SUBSCRIPTION_COLLECTION_NAME;
        return topicName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getBackendRoles() throws EventBrokerException {
        UserRealm userRealm = CarbonContext.getThreadLocalCarbonContext().getUserRealm();
        String[] cleanedRoles = new String[0];
        try {
            String adminRole =
                    EventBrokerHolder.getInstance().getRealmService().
                            getBootstrapRealmConfiguration().getAdminRoleName();
            String[] allRoles = userRealm.getUserStoreManager().getRoleNames();
            // check if there is only admin role exists.
            if (allRoles != null && allRoles.length > 1) {
                // check if more roles available than admin role and anonymous role
                List<String> allRolesArrayList = new ArrayList<>();
                Collections.addAll(allRolesArrayList, allRoles);

                Iterator<String> it = allRolesArrayList.iterator();
                while (it.hasNext()) {
                    String nextRole = it.next();
                    if (nextRole.equals(adminRole) || nextRole.equals(CarbonConstants.REGISTRY_ANONNYMOUS_ROLE_NAME)) {
                        it.remove();
                    }
                }

                cleanedRoles = allRolesArrayList.toArray(new String[allRolesArrayList.size()]);
            }

        } catch (UserStoreException e) {
            throw new EventBrokerException("Unable to get roles from user store", e);
        }

        return cleanedRoles;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean removeTopic(String topicName) throws EventBrokerException {

        try {
            UserRegistry userRegistry =
                    this.registryService.getGovernanceSystemRegistry(EventBrokerHolder.getInstance().getTenantId());
            String resourcePath = JavaUtil.getResourcePath(topicName, this.topicStoragePath);

            removeRoleCreateForLoggedInUser(topicName);

            if (userRegistry.resourceExists(resourcePath)) {
                userRegistry.delete(resourcePath);
                return true;
            } else {
                return false;
            }
        } catch (RegistryException e) {
            throw new EventBrokerException("Cannot access the config registry", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isTopicExists(String topicName) throws EventBrokerException {
        try {
            UserRegistry userRegistry =
                    this.registryService.getGovernanceSystemRegistry(EventBrokerHolder.getInstance().getTenantId());
            String resourcePath = JavaUtil.getResourcePath(topicName, this.topicStoragePath);
            return userRegistry.resourceExists(resourcePath);
        } catch (RegistryException e) {
            throw new EventBrokerException("Cannot access the config registry");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean checkUserHasAddTopicPermission(String username) throws EventBrokerException {
        boolean hasPermission = false;
        try {
            if (JavaUtil.isAdmin(username)) {
                hasPermission = true;
            } else if (CarbonContext.getThreadLocalCarbonContext().getUserRealm().getAuthorizationManager()
                    .isUserAuthorized(username, PERMISSION_ADMIN_MANAGE_TOPIC_ADD, UI_EXECUTE)) {
                hasPermission = true;
            }
        } catch (UserStoreException e) {
            String errorMessage = "Unable to get user store to check permissions.";
            log.error(errorMessage, e);
            throw new EventBrokerException(errorMessage, e);
        }
        return hasPermission;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean checkUserHasDeleteTopicPermission(String username) throws EventBrokerException {
        boolean hasPermission = false;
        try {
            if (JavaUtil.isAdmin(username)) {
                hasPermission = true;
            } else if (CarbonContext.getThreadLocalCarbonContext().getUserRealm().getAuthorizationManager()
                    .isUserAuthorized(username, PERMISSION_ADMIN_MANAGE_TOPIC_DELETE, UI_EXECUTE)) {
                hasPermission = true;
            }
        } catch (UserStoreException e) {
            String errorMessage = "Unable to get user store to check permissions.";
            log.error(errorMessage, e);
            throw new EventBrokerException(errorMessage, e);
        }
        return hasPermission;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean checkUserHasDetailsTopicPermission(String username) throws EventBrokerException {
        boolean hasPermission = false;
        try {
            if (JavaUtil.isAdmin(username)) {
                hasPermission = true;
            } else if (CarbonContext.getThreadLocalCarbonContext().getUserRealm().getAuthorizationManager()
                    .isUserAuthorized(username, PERMISSION_ADMIN_MANAGE_TOPIC_DETAILS, UI_EXECUTE)) {
                hasPermission = true;
            }
        } catch (UserStoreException e) {
            String errorMessage = "Unable to get user store to check permissions.";
            log.error(errorMessage, e);
            throw new EventBrokerException(errorMessage, e);
        }
        return hasPermission;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean checkUserHasPublishTopicPermission(String topicName, String username) throws EventBrokerException {
        boolean hasPermission = false;
        String topicResourcePath = JavaUtil.getResourcePath(topicName, this.topicStoragePath);
        try {
            if (JavaUtil.isAdmin(username)) {
                hasPermission = true;
            } else if (CarbonContext.getThreadLocalCarbonContext().getUserRealm().getAuthorizationManager()
                    .isUserAuthorized(username, topicResourcePath, EventBrokerConstants.EB_PERMISSION_PUBLISH)) {
                hasPermission = true;
            }
        } catch (UserStoreException e) {
            String errorMessage = "Unable to get user store to check permissions.";
            log.error(errorMessage, e);
            throw new EventBrokerException(errorMessage, e);
        }
        return hasPermission;
    }

    /**
     * Create a new role which has the same name as the destinationName and assign the logged in
     * user to the newly created role. Then, authorize the newly created role to subscribe and
     * publish to the destination.
     *
     * @param username        name of the logged in user
     * @param destinationName destination name. Either topic or queue name
     * @param destinationId   ID given to the destination
     * @param userRealm       the  user store
     * @throws org.wso2.carbon.user.api.UserStoreException
     */
    private static void authorizePermissionsToLoggedInUser(String username, String destinationName,
                                                           String destinationId,
                                                           UserRealm userRealm) throws
                                                                                UserStoreException {

        //For registry we use a modified queue name
        String roleName;
        String newDestinationName = destinationName.replace("@", AT_REPLACE_CHAR);
        String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();

        // creating the internal role name
        newDestinationName = newDestinationName.substring(0, 1)
                .equalsIgnoreCase("/") ? newDestinationName.replaceFirst("/", "") : newDestinationName;

        if (CarbonContext.getThreadLocalCarbonContext().getTenantId() >= 0) {
            String destinationWithTenantDomain = tenantDomain + "/" + newDestinationName;
            roleName = UserCoreUtil.addInternalDomainName(TOPIC_ROLE_PREFIX +
                    destinationWithTenantDomain.replace(".","-").replace("/", "-"));
        } else {
            roleName = UserCoreUtil.addInternalDomainName(TOPIC_ROLE_PREFIX +
                    newDestinationName.replace(".","-").replace("/", "-"));
        }

        // the interface to store user data
        UserStoreManager userStoreManager = CarbonContext.getThreadLocalCarbonContext().getUserRealm().getUserStoreManager();

        if (!userStoreManager.isExistingRole(roleName)) {
            String[] user = {MultitenantUtils.getTenantAwareUsername(username)};

            // adds the internal role to user store
            userStoreManager.addRole(roleName, user, null);
            // giving permissions to the topic and it's all hierarchy
            grantPermissionToHierarchyLevel(userRealm, destinationId, roleName);

        } else {
            log.warn("Unable to provide permissions to the user, " +
                     " " + username + ", to subscribe and publish to " + newDestinationName);
        }
    }

    /**
     * Every queue/topic has a role with the same name as the queue/topic name. This role is used
     * to store the permissions for the user who created the queue/topic.This role should be
     * deleted when the queue/topic is deleted.
     *
     * @param destinationName name of the queue or topic
     * @throws EventBrokerException
     */
    private void removeRoleCreateForLoggedInUser(String destinationName)
            throws EventBrokerException {
        //For registry we use a modified queue name
        String roleName;
        String newDestinationName = destinationName.replace("@", AT_REPLACE_CHAR);
        String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();

        if (CarbonContext.getThreadLocalCarbonContext().getTenantId() >= 0) {
            String destinationWithTenantDomain = tenantDomain + "/" + newDestinationName;
            roleName = UserCoreUtil.addInternalDomainName(TOPIC_ROLE_PREFIX +
                    destinationWithTenantDomain.replace(".","-").replace("/", "-"));
        } else {
            roleName = UserCoreUtil.addInternalDomainName(TOPIC_ROLE_PREFIX +
                    newDestinationName.replace(".","-").replace("/", "-"));
        }

        try {
            UserStoreManager userStoreManager = CarbonContext.getThreadLocalCarbonContext().getUserRealm().getUserStoreManager();
            AuthorizationManager authorizationManager = CarbonContext.getThreadLocalCarbonContext().getUserRealm().getAuthorizationManager();

            if (userStoreManager.isExistingRole(roleName)) {
                userStoreManager.deleteRole(roleName);
                authorizationManager.clearResourceAuthorizations(JavaUtil.getResourcePath(destinationName, getTopicStoragePath()));
            }
        } catch (UserStoreException e) {
            throw new EventBrokerException("Error while deleting " + newDestinationName, e);
        }
    }
}
