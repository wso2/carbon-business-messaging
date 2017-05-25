/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.wso2.carbon.business.messaging.identity.impl;

import org.wso2.carbon.business.messaging.identity.Action;
import org.wso2.carbon.business.messaging.identity.AuthorizationService;
import org.wso2.carbon.business.messaging.identity.AuthorizationStore;
import org.wso2.carbon.business.messaging.identity.Group;
import org.wso2.carbon.business.messaging.identity.Permission;
import org.wso2.carbon.business.messaging.identity.Resource;
import org.wso2.carbon.business.messaging.identity.Role;
import org.wso2.carbon.business.messaging.identity.User;
import org.wso2.carbon.business.messaging.identity.connector.config.AuthorizationStoreConnectorConfig;
import org.wso2.carbon.business.messaging.identity.exception.AuthorizationStoreException;
import org.wso2.carbon.business.messaging.identity.exception.PermissionNotFoundException;
import org.wso2.carbon.business.messaging.identity.exception.RoleNotFoundException;

import java.util.List;
import java.util.Map;

/**
 * TODO: CredentialStore methods
 */
public class AuthorizationStoreImpl implements AuthorizationStore {

    @Override
    public void init(AuthorizationService authorizationService,
                     Map<String, AuthorizationStoreConnectorConfig> authorizationConnectorConfigs)
            throws AuthorizationStoreException {

    }

    @Override
    public boolean isUserAuthorized(String userId, Permission permission)
            throws AuthorizationStoreException {
        return false;
    }

    @Override
    public boolean isGroupAuthorized(String groupId, Permission permission)
            throws AuthorizationStoreException {
        return false;
    }

    @Override
    public boolean isRoleAuthorized(String roleId, String authorizationStoreId,
                                    Permission permission) throws AuthorizationStoreException {
        return false;
    }

    @Override
    public boolean isUserInRole(String userId, String roleName) throws AuthorizationStoreException {
        return false;
    }

    @Override
    public boolean isGroupInRole(String groupId, String roleName)
            throws AuthorizationStoreException {
        return false;
    }

    @Override
    public Role getRole(String roleName) throws RoleNotFoundException, AuthorizationStoreException {
        return null;
    }

    @Override
    public Permission getPermission(String resource, String action)
            throws PermissionNotFoundException, AuthorizationStoreException {
        return null;
    }

    @Override
    public List<Role> listRoles(String filterPattern, int offset, int length)
            throws AuthorizationStoreException {
        return null;
    }

    @Override
    public List<Permission> listPermissions(String resourcePattern, String actionPattern,
                                            int offset, int length)
            throws AuthorizationStoreException {
        return null;
    }

    @Override
    public List<Resource> listResources(String resourcePattern) throws AuthorizationStoreException {
        return null;
    }

    @Override
    public List<Action> listActions(String actionPattern) throws AuthorizationStoreException {
        return null;
    }

    @Override
    public List<Role> getRolesOfUser(String userId) throws AuthorizationStoreException {
        return null;
    }

    @Override
    public List<User> getUsersOfRole(String roleId, String authorizationStoreId)
            throws AuthorizationStoreException {
        return null;
    }

    @Override
    public List<Group> getGroupsOfRole(String roleId, String authorizationStoreId)
            throws AuthorizationStoreException {
        return null;
    }

    @Override
    public List<Role> getRolesOfGroup(String groupId) throws AuthorizationStoreException {
        return null;
    }

    @Override
    public List<Permission> getPermissionsOfRole(String roleId, String authorizationStoreId,
                                                 Resource resource)
            throws AuthorizationStoreException {
        return null;
    }

    @Override
    public List<Permission> getPermissionsOfRole(String roleId, String authorizationStoreId,
                                                 Action action) throws AuthorizationStoreException {
        return null;
    }

    @Override
    public List<Permission> getPermissionsOfRole(String roleId, String authorizationStoreId)
            throws AuthorizationStoreException {
        return null;
    }

    @Override
    public List<Permission> getPermissionsOfUser(String userId, Resource resource)
            throws AuthorizationStoreException {
        return null;
    }

    @Override
    public List<Permission> getPermissionsOfUser(String userId, Action action)
            throws AuthorizationStoreException {
        return null;
    }

    @Override
    public Role addRole(String roleName, List<Permission> permissions)
            throws AuthorizationStoreException {
        return null;
    }

    @Override
    public Role addRole(String roleName, List<Permission> permissions, String authorizationStoreId)
            throws AuthorizationStoreException {
        return null;
    }

    @Override
    public void deleteRole(Role role) throws AuthorizationStoreException {

    }

    @Override
    public Resource.ResourceBuilder addResource(String resourceNamespace, String resourceId,
                                                String userId) throws AuthorizationStoreException {
        return null;
    }

    @Override
    public Resource.ResourceBuilder addResource(String resourceNamespace, String resourceId,
                                                String authorizationStoreId, String userId)
            throws AuthorizationStoreException {
        return null;
    }

    @Override
    public void deleteResource(Resource resource) throws AuthorizationStoreException {

    }

    @Override
    public Action addAction(String actionNamespace, String actionName)
            throws AuthorizationStoreException {
        return null;
    }

    @Override
    public Action addAction(String actionNamespace, String actionName, String authorizationStoreId)
            throws AuthorizationStoreException {
        return null;
    }

    @Override
    public void deleteAction(Action action) throws AuthorizationStoreException {

    }

    @Override
    public Permission addPermission(Resource resource, Action action)
            throws AuthorizationStoreException {
        return null;
    }

    @Override
    public Permission addPermission(Resource resource, Action action, String authorizationStoreId)
            throws AuthorizationStoreException {
        return null;
    }

    @Override
    public void deletePermission(Permission permission) throws AuthorizationStoreException {

    }

    @Override
    public void updateRolesInUser(String userId, List<Role> newRoleList)
            throws AuthorizationStoreException {

    }

    @Override
    public void updateRolesInUser(String userId, List<Role> rolesToBeAssign,
                                  List<Role> rolesToBeUnassign) throws AuthorizationStoreException {

    }

    @Override
    public void updateUsersInRole(String roleId, String authorizationStoreId,
                                  List<User> newUserList) throws AuthorizationStoreException {

    }

    @Override
    public void updateUsersInRole(String roleId, String authorizationStoreId,
                                  List<User> usersToBeAssign, List<User> usersToBeUnassigned)
            throws AuthorizationStoreException {

    }

    @Override
    public void updateRolesInGroup(String groupId, List<Role> newRoleList)
            throws AuthorizationStoreException {

    }

    @Override
    public void updateRolesInGroup(String groupId, List<Role> rolesToBeAssign,
                                   List<Role> rolesToBeUnassigned)
            throws AuthorizationStoreException {

    }

    @Override
    public void updateGroupsInRole(String roleId, String authorizationStoreId,
                                   List<Group> newGroupList) throws AuthorizationStoreException {

    }

    @Override
    public void updateGroupsInRole(String roleId, String authorizationStoreId,
                                   List<Group> groupToBeAssign, List<Group> groupToBeUnassign)
            throws AuthorizationStoreException {

    }

    @Override
    public void updatePermissionsInRole(String roleId, String authorizationStoreId,
                                        List<Permission> newPermissionList)
            throws AuthorizationStoreException {

    }

    @Override
    public void updatePermissionsInRole(String roleId, String authorizationStoreId,
                                        List<Permission> permissionsToBeAssign,
                                        List<Permission> permissionsToBeUnassign)
            throws AuthorizationStoreException {

    }

    @Override
    public Map<String, String> getAllAuthorizationStoreNames() {
        return null;
    }
}
//
//    private static final Logger log = LoggerFactory.getLogger(AuthorizationStoreImpl.class);
//
//    private AuthorizationService authorizationService;
//    private Map<String, AuthorizationStoreConnector> authorizationStoreConnectors = new HashMap<>();
//    private RealmService realmService;
//
//    @Override
//    public void init(AuthorizationService authorizationService,
//                     Map<String, AuthorizationStoreConnectorConfig>
//                             authorizationConnectorConfigs) throws AuthorizationStoreException {
//
//        this.authorizationService = authorizationService;
//        realmService = IdentityMgtDataHolder.getInstance().getRealmService();
//
//        if (authorizationConnectorConfigs.isEmpty()) {
//            throw new AuthorizationStoreException("At least one authorization store configuration must present.");
//        }
//
//        for (Map.Entry<String, AuthorizationStoreConnectorConfig> authorizationStoreConfig :
//                authorizationConnectorConfigs.entrySet()) {
//
//            String connectorType = authorizationStoreConfig.getValue().getConnectorType();
//            AuthorizationStoreConnectorFactory authorizationStoreConnectorFactory =
//                    IdentityMgtDataHolder.getInstance().getAuthorizationStoreConnectorFactoryMap().get(connectorType);
//
//            if (authorizationStoreConnectorFactory == null) {
//                throw new AuthorizationStoreException("No credential store connector factory found for given type.");
//            }
//
//            AuthorizationStoreConnector authorizationStoreConnector = authorizationStoreConnectorFactory
//                    .getInstance();
//            authorizationStoreConnector.init(authorizationStoreConfig.getKey(), authorizationStoreConfig.getValue());
//
//            authorizationStoreConnectors.put(authorizationStoreConfig.getKey(), authorizationStoreConnector);
//        }
//
//        if (log.isDebugEnabled()) {
//            log.debug("Authorization store successfully initialized.");
//        }
//    }
//
//    @Override
//    public boolean isUserAuthorized(String userId, Permission permission)
//            throws AuthorizationStoreException {
//
//        // If this user owns this resource, we assume this user has all permissions.
//        if (permission.getResource().getOwner().getUserId().equals(userId)) {
//            return true;
//        }
//
//        // Get the roles directly associated to the user.
//        List<Role> roles = new ArrayList<>();
//        for (AuthorizationStoreConnector authorizationStoreConnector : authorizationStoreConnectors.values()) {
//            roles.addAll(authorizationStoreConnector.getRolesForUser(userId)
//                                 .stream()
//                                 .map(roleBuilder -> roleBuilder
//                                         .setAuthorizationStore(authorizationService.getAuthorizationStore())
//                                         .build())
//                                 .collect(Collectors.toList()));
//        }
//
//        // Get the roles associated through groups.
//        List<Group> groups = null;
//        try {
//            groups = realmService.getIdentityStore().getGroupsOfUser(userId);
//        } catch (IdentityStoreException e) {
//            throw new AuthorizationStoreException(String.format("Error while receiving groups for user %s.", userId));
//        } catch (UserNotFoundException e) {
//            throw new AuthorizationStoreException(String.format("User with userid %s not found", userId));
//        }
//        for (Group group : groups) {
//            roles.addAll(getRolesOfGroup(group.getUniqueGroupId()));
//        }
//
//        if (roles.isEmpty()) {
//            throw new AuthorizationStoreException("No roles assigned for this user");
//        }
//
//        for (Role role : roles) {
//            if (isRoleAuthorized(role.getRoleId(), role.getAuthorizationStoreId(), permission)) {
//                return true;
//            }
//        }
//
//        return false;
//    }
//
//    @Override
//    public boolean isGroupAuthorized(String groupId, Permission permission)
//            throws AuthorizationStoreException {
//
//        List<Role> roles = getRolesOfGroup(groupId);
//
//        for (Role role : roles) {
//            if (isRoleAuthorized(role.getRoleId(), role.getAuthorizationStoreId(), permission)) {
//                return true;
//            }
//        }
//
//        return false;
//    }
//
//    @Override
//    public boolean isRoleAuthorized(String roleId, String authorizationStoreId,
//                                    Permission permission)
//            throws AuthorizationStoreException {
//
//        AuthorizationStoreConnector authorizationStoreConnector = authorizationStoreConnectors
//                .get(authorizationStoreId);
//
//        if (authorizationStoreConnector == null) {
//            throw new AuthorizationStoreException(String.format("No authorization store found for the given id: %s.",
//                                                                authorizationStoreId));
//        }
//
//        List<Permission.PermissionBuilder> permissionBuilders = authorizationStoreConnector
//                .getPermissionsForRole(roleId, permission.getResource());
//
//        if (permissionBuilders.isEmpty()) {
//            throw new AuthorizationStoreException("No permissions assigned for this role.");
//        }
//
//        for (Permission.PermissionBuilder permissionBuilder : permissionBuilders) {
//            if (permissionBuilder.build().equals(permission)) {
//                return true;
//            }
//        }
//
//        return false;
//    }
//
//    @Override
//    public boolean isUserInRole(String userId, String roleName)
//            throws AuthorizationStoreException {
//
//        for (AuthorizationStoreConnector authorizationStoreConnector : authorizationStoreConnectors.values()) {
//            if (authorizationStoreConnector.isUserInRole(userId, roleName)) {
//                return true;
//            }
//        }
//
//        return false;
//    }
//
//    @Override
//    public boolean isGroupInRole(String groupId, String roleName)
//            throws AuthorizationStoreException {
//
//        for (AuthorizationStoreConnector authorizationStoreConnector : authorizationStoreConnectors.values()) {
//            if (authorizationStoreConnector.isGroupInRole(groupId, roleName)) {
//                return true;
//            }
//        }
//
//        return false;
//    }
//
//    @Override
//    public Role getRole(String roleName) throws RoleNotFoundException, AuthorizationStoreException {
//
//        RoleNotFoundException roleNotFoundException = new RoleNotFoundException("Role not found for the given name.");
//
//        for (AuthorizationStoreConnector authorizationStoreConnector : authorizationStoreConnectors.values()) {
//            try {
//                return authorizationStoreConnector.getRole(roleName)
//                        .setAuthorizationStore(authorizationService.getAuthorizationStore())
//                        .build();
//            } catch (RoleNotFoundException e) {
//                roleNotFoundException.addSuppressed(e);
//            }
//        }
//        throw roleNotFoundException;
//    }
//
//    @Override
//    public Permission getPermission(String resource, String action) throws
//                                                                    PermissionNotFoundException,
//                                                                    AuthorizationStoreException {
//
//        PermissionNotFoundException permissionNotFoundException =
//                new PermissionNotFoundException("Permission not found for the given resource id and the action.");
//
//        for (AuthorizationStoreConnector authorizationStoreConnector : authorizationStoreConnectors.values()) {
//            try {
//                return authorizationStoreConnector.getPermission(new Resource(resource), new Action(action)).build();
//            } catch (PermissionNotFoundException e) {
//                permissionNotFoundException.addSuppressed(e);
//            }
//        }
//        throw permissionNotFoundException;
//    }
//
//    @Override
//    public List<Role> listRoles(String filterPattern, int offset, int length)
//            throws AuthorizationStoreException {
//
//        List<Role> roles = new ArrayList<>();
//
//        for (AuthorizationStoreConnector authorizationStoreConnector : authorizationStoreConnectors.values()) {
//
//            // Get the total count of roles in the authorization store.
//            int roleCount;
//            try {
//                roleCount = authorizationStoreConnector.getRoleCount();
//            } catch (UnsupportedOperationException e) {
//                log.warn("Count operation is not supported by this authorization store. Running the operation in " +
//                         "performance intensive mode.");
//                roleCount = authorizationStoreConnector.listRoles("*", 0, -1).size();
//            }
//
//            // If there are roles in this user store more than the offset, we can get roles from this offset.
//            // If this offset exceeds the available count of the current authorization store, move to the next
//            // user store.
//            if (roleCount > offset) {
//                roles.addAll(authorizationStoreConnector.listRoles(filterPattern, offset, length)
//                                     .stream()
//                                     .map(roleBuilder -> roleBuilder.setAuthorizationStore(
//                                             authorizationService.getAuthorizationStore()).build())
//                                     .collect(Collectors.toList()));
//                length -= roles.size();
//                offset = 0;
//            } else {
//                offset -= roleCount;
//            }
//
//            // If we retrieved all the required roles.
//            if (length == 0) {
//                break;
//            }
//        }
//
//        return roles;
//    }
//
//    @Override
//    public List<Permission> listPermissions(String resourcePattern, String actionPattern,
//                                            int offset, int length)
//            throws AuthorizationStoreException {
//
//        List<Permission> permissions = new ArrayList<>();
//
//        for (AuthorizationStoreConnector authorizationStoreConnector : authorizationStoreConnectors.values()) {
//
//            // Get the total count of permissions in the authorization store.
//            int permissionCount;
//            try {
//                permissionCount = authorizationStoreConnector.getPermissionCount();
//            } catch (UnsupportedOperationException e) {
//                log.warn("Count operation is not supported by this authorization store. Running the operation in " +
//                         "performance intensive mode.");
//                permissionCount = authorizationStoreConnector.listRoles("*", 0, -1).size();
//            }
//
//            // If there are permissions in this user store more than the offset, we can get permissions from this
//            // offset. If this offset exceeds the available count of the current authorization store, move to the next
//            // authorization store.
//            if (permissionCount > offset) {
//                permissions.addAll(authorizationStoreConnector
//                                           .listPermissions(resourcePattern, actionPattern, offset, length)
//                                           .stream()
//                                           .map(Permission.PermissionBuilder::build)
//                                           .collect(Collectors.toList()));
//                length -= permissions.size();
//                offset = 0;
//            } else {
//                offset -= permissionCount;
//            }
//
//            // If we retrieved all the required permissions.
//            if (length == 0) {
//                break;
//            }
//        }
//
//        return permissions;
//    }
//
//    @Override
//    public List<Resource> listResources(String resourcePattern) throws AuthorizationStoreException {
//
//        List<Resource> resources = new ArrayList<>();
//
//        for (AuthorizationStoreConnector authorizationStoreConnector : authorizationStoreConnectors.values()) {
//            resources.addAll(authorizationStoreConnector.getResources(resourcePattern)
//                                     .stream()
//                                     .map(Resource.ResourceBuilder::build)
//                                     .collect(Collectors.toList()));
//        }
//
//        return resources;
//    }
//
//    @Override
//    public List<Action> listActions(String actionPattern) throws AuthorizationStoreException {
//
//        List<Action> actions = new ArrayList<>();
//
//        for (AuthorizationStoreConnector authorizationStoreConnector : authorizationStoreConnectors.values()) {
//            actions.addAll(authorizationStoreConnector.getActions(actionPattern)
//                                   .stream()
//                                   .map(Action.ActionBuilder::build)
//                                   .collect(Collectors.toList()));
//        }
//
//        return actions;
//    }
//
//    @Override
//    public List<Role> getRolesOfUser(String userId) throws AuthorizationStoreException {
//
//        List<Role> roles = new ArrayList<>();
//        for (AuthorizationStoreConnector authorizationStoreConnector : authorizationStoreConnectors.values()) {
//            roles.addAll(authorizationStoreConnector.getRolesForUser(userId)
//                                 .stream()
//                                 .map(roleBuilder -> roleBuilder.setAuthorizationStore(
//                                         authorizationService.getAuthorizationStore()).build())
//                                 .collect(Collectors.toList()));
//        }
//
//        return roles;
//    }
//
//    @Override
//    public List<User> getUsersOfRole(String roleId, String authorizationStoreId)
//            throws AuthorizationStoreException {
//
//        AuthorizationStoreConnector authorizationStoreConnector = authorizationStoreConnectors
//                .get(authorizationStoreId);
//
//        if (authorizationStoreConnector == null) {
//            throw new AuthorizationStoreException(String.format("No authorization store found for the given id: %s.",
//                                                                authorizationStoreId));
//        }
//
//        return authorizationStoreConnector.getUsersOfRole(roleId)
//                .stream()
//                .map(LambdaExceptionUtils.rethrowFunction(userBuilder -> userBuilder
//                        .setIdentityStore(realmService.getIdentityStore())
//                        .setAuthorizationStore(authorizationService.getAuthorizationStore())
//                        .build()))
//                .collect(Collectors.toList());
//    }
//
//    @Override
//    public List<Group> getGroupsOfRole(String roleId, String authorizationStoreId)
//            throws AuthorizationStoreException {
//
//        AuthorizationStoreConnector authorizationStoreConnector = authorizationStoreConnectors
//                .get(authorizationStoreId);
//
//        if (authorizationStoreConnector == null) {
//            throw new AuthorizationStoreException(String.format("No authorization store found for the given id: %s.",
//                                                                authorizationStoreId));
//        }
//
//        return authorizationStoreConnector.getGroupsOfRole(roleId)
//                .stream()
//                .map(LambdaExceptionUtils.rethrowFunction(groupBuilder -> groupBuilder
//                        .setIdentityStore(realmService.getIdentityStore())
//                        .setAuthorizationStore(authorizationService.getAuthorizationStore())
//                        .build()))
//                .collect(Collectors.toList());
//    }
//
//    @Override
//    public List<Role> getRolesOfGroup(String groupId) throws AuthorizationStoreException {
//
//        List<Role> roles = new ArrayList<>();
//
//        for (AuthorizationStoreConnector authorizationStoreConnector : authorizationStoreConnectors.values()) {
//            roles.addAll(authorizationStoreConnector.getRolesForGroup(groupId)
//                                 .stream()
//                                 .map(roleBuilder -> roleBuilder
//                                         .setAuthorizationStore(authorizationService.getAuthorizationStore())
//                                         .build())
//                                 .collect(Collectors.toList()));
//        }
//
//        return roles;
//    }
//
//    @Override
//    public List<Permission> getPermissionsOfRole(String roleId, String authorizationStoreId,
//                                                 Resource resource)
//            throws AuthorizationStoreException {
//
//        AuthorizationStoreConnector authorizationStoreConnector = authorizationStoreConnectors
//                .get(authorizationStoreId);
//
//        if (authorizationStoreConnector == null) {
//            throw new AuthorizationStoreException(String.format("No authorization store found for the given id: %s.",
//                                                                authorizationStoreId));
//        }
//
//        return authorizationStoreConnector.getPermissionsForRole(roleId, resource)
//                .stream()
//                .map(Permission.PermissionBuilder::build)
//                .collect(Collectors.toList());
//    }
//
//    @Override
//    public List<Permission> getPermissionsOfRole(String roleId, String authorizationStoreId,
//                                                 Action action)
//            throws AuthorizationStoreException {
//
//        AuthorizationStoreConnector authorizationStoreConnector = authorizationStoreConnectors
//                .get(authorizationStoreId);
//
//        if (authorizationStoreConnector == null) {
//            throw new AuthorizationStoreException(String.format("No authorization store found for the given id: %s.",
//                                                                authorizationStoreId));
//        }
//
//        return authorizationStoreConnector.getPermissionsForRole(roleId, action)
//                .stream()
//                .map(Permission.PermissionBuilder::build)
//                .collect(Collectors.toList());
//    }
//
//    @Override
//    public List<Permission> getPermissionsOfRole(String roleId, String authorizationStoreId)
//            throws AuthorizationStoreException {
//
//        return getPermissionsOfRole(roleId, authorizationStoreId, Resource.getUniversalResource());
//    }
//
//    @Override
//    public List<Permission> getPermissionsOfUser(String userId, Resource resource)
//            throws AuthorizationStoreException {
//
//        return getRolesOfUser(userId)
//                .stream()
//                .map(LambdaExceptionUtils.rethrowFunction(role ->
//                getPermissionsOfRole(role.getRoleId(), role.getAuthorizationStoreId(), resource)))
//                .flatMap(Collection::stream)
//                .collect(Collectors.toList());
//    }
//
//    @Override
//    public List<Permission> getPermissionsOfUser(String userId, Action action)
//            throws AuthorizationStoreException {
//
//        return getRolesOfUser(userId)
//                .stream()
//                .map(LambdaExceptionUtils.rethrowFunction(role ->
//                getPermissionsOfRole(role.getRoleId(), role.getAuthorizationStoreId(), action)))
//                .flatMap(Collection::stream)
//                .collect(Collectors.toList());
//    }
//
//    @Override
//    public Role addRole(String roleName, List<Permission> permissions)
//            throws AuthorizationStoreException {
//
//        String authorizationStoreId = getPrimaryAuthorizationStoreId();
//        return addRole(roleName, permissions, authorizationStoreId);
//    }
//
//    @Override
//    public Role addRole(String roleName, List<Permission> permissions, String authorizationStoreId)
//            throws AuthorizationStoreException {
//
//        AuthorizationStoreConnector authorizationStoreConnector = authorizationStoreConnectors
//                .get(authorizationStoreId);
//
//        if (authorizationStoreConnector == null) {
//            throw new AuthorizationStoreException(String.format("No authorization store found for the given id: %s.",
//                                                                authorizationStoreId));
//        }
//
//        Role.RoleBuilder roleBuilder = authorizationStoreConnector.addRole(roleName, permissions);
//
//        if (roleBuilder == null) {
//            throw new AuthorizationStoreException("Role builder is null.");
//        }
//
//        return roleBuilder.setAuthorizationStore(authorizationService.getAuthorizationStore()).build();
//    }
//
//    @Override
//    public void deleteRole(Role role) throws AuthorizationStoreException {
//
//        AuthorizationStoreConnector authorizationStoreConnector = authorizationStoreConnectors.get(role
//                                                             .getAuthorizationStoreId());
//
//        if (authorizationStoreConnector == null) {
//            throw new AuthorizationStoreException(String.format("No authorization store found for the given id: %s.",
//                                                                role.getAuthorizationStoreId()));
//        }
//
//        authorizationStoreConnector.deleteRole(role.getRoleId());
//    }
//
//    @Override
//    public Resource.ResourceBuilder addResource(String resourceNamespace, String resourceId,
//                                                String userId)
//            throws AuthorizationStoreException {
//
//        String authorizationStoreId = getPrimaryAuthorizationStoreId();
//        return addResource(resourceNamespace, resourceId, authorizationStoreId, userId);
//    }
//
//    @Override
//    public Resource.ResourceBuilder addResource(String resourceNamespace, String resourceId,
//                                                String authorizationStoreId,
//                                                String userId) throws AuthorizationStoreException {
//
//        AuthorizationStoreConnector authorizationStoreConnector = authorizationStoreConnectors
//                .get(authorizationStoreId);
//
//        if (authorizationStoreConnector == null) {
//            throw new AuthorizationStoreException(String.format("No authorization store found for the given id: %s.",
//                                                                authorizationStoreId));
//        }
//
//        return authorizationStoreConnector.addResource(resourceNamespace, resourceId, userId);
//    }
//
//    @Override
//    public void deleteResource(Resource resource) throws AuthorizationStoreException {
//
//        AuthorizationStoreConnector authorizationStoreConnector = authorizationStoreConnectors
//                .get(resource.getAuthorizationStore());
//
//        if (authorizationStoreConnector == null) {
//            throw new AuthorizationStoreException(String.format("No authorization store found for the given id: %s.",
//                                                                resource.getAuthorizationStore()));
//        }
//
//        authorizationStoreConnector.deleteResource(resource);
//    }
//
//    @Override
//    public Action addAction(String actionNamespace, String actionName)
//            throws AuthorizationStoreException {
//
//        String authorizationStoreId = getPrimaryAuthorizationStoreId();
//        return addAction(actionNamespace, actionName, authorizationStoreId);
//    }
//
//    @Override
//    public Action addAction(String actionNamespace, String actionName, String authorizationStoreId)
//            throws AuthorizationStoreException {
//
//        AuthorizationStoreConnector authorizationStoreConnector = authorizationStoreConnectors
//                .get(authorizationStoreId);
//
//        if (authorizationStoreConnector == null) {
//            throw new AuthorizationStoreException(String.format("No authorization store found for the given id: %s.",
//                                                                authorizationStoreId));
//        }
//
//        return authorizationStoreConnector.addAction(actionNamespace, actionName);
//    }
//
//    @Override
//    public void deleteAction(Action action) throws AuthorizationStoreException {
//
//        AuthorizationStoreConnector authorizationStoreConnector = authorizationStoreConnectors
//                .get(action.getAuthorizationStore());
//
//        if (authorizationStoreConnector == null) {
//            throw new AuthorizationStoreException(String.format("No authorization store found for the given id: %s.",
//                                                                action.getAuthorizationStore()));
//        }
//
//        authorizationStoreConnector.deleteAction(action);
//    }
//
//    @Override
//    public Permission addPermission(Resource resource, Action action)
//            throws AuthorizationStoreException {
//
//        String authorizationStoreId = getPrimaryAuthorizationStoreId();
//        return addPermission(resource, action, authorizationStoreId);
//    }
//
//    @Override
//    public Permission addPermission(Resource resource, Action action, String authorizationStoreId)
//            throws AuthorizationStoreException {
//
//        AuthorizationStoreConnector authorizationStoreConnector = authorizationStoreConnectors
//                .get(authorizationStoreId);
//
//        if (authorizationStoreConnector == null) {
//            throw new AuthorizationStoreException(String.format("No authorization store found for the given id: %s.",
//                                                                authorizationStoreId));
//        }
//
//        return authorizationStoreConnector.addPermission(resource, action).build();
//    }
//
//    @Override
//    public void deletePermission(Permission permission) throws AuthorizationStoreException {
//
//        AuthorizationStoreConnector authorizationStoreConnector = authorizationStoreConnectors
//                .get(permission.getAuthorizationStoreId());
//
//        if (authorizationStoreConnector == null) {
//            throw new AuthorizationStoreException(String.format("No authorization store found for the given id: %s.",
//                                                                permission.getAuthorizationStoreId()));
//        }
//
//        authorizationStoreConnector.deletePermission(permission.getPermissionId());
//    }
//
//    @Override
//    public void updateRolesInUser(String userId, List<Role> newRoleList)
//            throws AuthorizationStoreException {
//
//        if (newRoleList == null || newRoleList.isEmpty()) {
//            for (AuthorizationStoreConnector authorizationStoreConnector : authorizationStoreConnectors.values()) {
//                authorizationStoreConnector.updateRolesInUser(userId, newRoleList);
//            }
//            return;
//        }
//
//        Map<String, List<Role>> roleMap = this.getRolesWithAuthorizationStore(newRoleList);
//
//        for (Map.Entry<String, List<Role>> roleEntry : roleMap.entrySet()) {
//            AuthorizationStoreConnector authorizationStoreConnector = authorizationStoreConnectors
//                    .get(roleEntry.getKey());
//            if (authorizationStoreConnector == null) {
//                throw new AuthorizationStoreException(
//                        String.format("No authorization store found for the given id: %s.", roleEntry.getKey()));
//            }
//            authorizationStoreConnector.updateRolesInUser(userId, roleEntry.getValue());
//        }
//    }
//
//    @Override
//    public void updateRolesInUser(String userId, List<Role> rolesToBeAssign,
//                                  List<Role> rolesToBeUnassign) throws AuthorizationStoreException {
//
//        Map<String, List<Role>> rolesToBeAssignWithStoreId = this.getRolesWithAuthorizationStore(rolesToBeAssign);
//        Map<String, List<Role>> rolesToBeUnAssignWithStoreId = this.getRolesWithAuthorizationStore(rolesToBeUnassign);
//
//        Set<String> keys = new HashSet<>();
//        keys.addAll(rolesToBeAssignWithStoreId.keySet());
//        keys.addAll(rolesToBeUnAssignWithStoreId.keySet());
//
//        for (String key : keys) {
//
//            AuthorizationStoreConnector authorizationStoreConnector = authorizationStoreConnectors.get(key);
//
//            if (authorizationStoreConnector == null) {
//                throw new AuthorizationStoreException(
//                        String.format("No authorization store found for the given id: %s.", key));
//            }
//
//            authorizationStoreConnector.updateRolesInUser(userId,
//                 rolesToBeAssignWithStoreId.get(key), rolesToBeUnAssignWithStoreId.get(key));
//        }
//    }
//
//    @Override
//    public void updateUsersInRole(String roleId, String authorizationStoreId,
//                                  List<User> newUserList)
//            throws AuthorizationStoreException {
//
//        AuthorizationStoreConnector authorizationStoreConnector = authorizationStoreConnectors
//                .get(authorizationStoreId);
//
//        if (authorizationStoreConnector == null) {
//            throw new AuthorizationStoreException(String.format("No authorization store found for the given id: %s.",
//                                                                authorizationStoreId));
//        }
//
//        authorizationStoreConnector.updateUsersInRole(roleId, newUserList);
//    }
//
//    @Override
//    public void updateUsersInRole(String roleId, String authorizationStoreId,
//                                  List<User> usersToBeAssign,
//                                  List<User> usersToBeUnassign) throws AuthorizationStoreException {
//
//        AuthorizationStoreConnector authorizationStoreConnector = authorizationStoreConnectors
//                .get(authorizationStoreId);
//
//        if (authorizationStoreConnector == null) {
//            throw new AuthorizationStoreException(String.format("No authorization store found for the given id: %s.",
//                                                                authorizationStoreId));
//        }
//
//        authorizationStoreConnector.updateUsersInRole(roleId, usersToBeAssign, usersToBeUnassign);
//    }
//
//    @Override
//    public void updateRolesInGroup(String groupId, List<Role> newRoleList)
//            throws AuthorizationStoreException {
//
//        if (newRoleList == null || newRoleList.isEmpty()) {
//            for (AuthorizationStoreConnector authorizationStoreConnector : authorizationStoreConnectors.values()) {
//                authorizationStoreConnector.updateRolesInGroup(groupId, newRoleList);
//            }
//            return;
//        }
//
//        Map<String, List<Role>> roleMap = this.getRolesWithAuthorizationStore(newRoleList);
//
//        for (Map.Entry<String, List<Role>> roleEntry : roleMap.entrySet()) {
//            AuthorizationStoreConnector authorizationStoreConnector = authorizationStoreConnectors
//                    .get(roleEntry.getKey());
//            if (authorizationStoreConnector == null) {
//                throw new AuthorizationStoreException(
//                        String.format("No authorization store found for the given id: %s.", roleEntry.getKey()));
//            }
//            authorizationStoreConnector.updateRolesInGroup(groupId, roleEntry.getValue());
//        }
//    }
//
//    @Override
//    public void updateRolesInGroup(String groupId, List<Role> rolesToBeAssign,
//                                   List<Role> rolesToBeUnassigned)
//            throws AuthorizationStoreException {
//
//        Map<String, List<Role>> rolesToBeAssignWithStoreId = this.getRolesWithAuthorizationStore(rolesToBeAssign);
//        Map<String, List<Role>> rolesToBeUnAssignWithStoreId = this
//                .getRolesWithAuthorizationStore(rolesToBeUnassigned);
//
//        Set<String> keys = new HashSet<>();
//        keys.addAll(rolesToBeAssignWithStoreId.keySet());
//        keys.addAll(rolesToBeUnAssignWithStoreId.keySet());
//
//        for (String key : keys) {
//
//            AuthorizationStoreConnector authorizationStoreConnector = authorizationStoreConnectors.get(key);
//
//            if (authorizationStoreConnector == null) {
//                throw new AuthorizationStoreException(
//                        String.format("No authorization store found for the given id: %s.", key));
//            }
//
//            authorizationStoreConnector.updateRolesInGroup(groupId,
//                      rolesToBeAssignWithStoreId.get(key), rolesToBeUnAssignWithStoreId.get(key));
//        }
//    }
//
//    @Override
//    public void updateGroupsInRole(String roleId, String authorizationStoreId,
//                                   List<Group> newGroupList)
//            throws AuthorizationStoreException {
//
//        AuthorizationStoreConnector authorizationStoreConnector = authorizationStoreConnectors
//                .get(authorizationStoreId);
//
//        if (authorizationStoreConnector == null) {
//            throw new AuthorizationStoreException(String.format("No authorization store found for the given id: %s.",
//                                                                authorizationStoreId));
//        }
//
//        authorizationStoreConnector.updateGroupsInRole(roleId, newGroupList);
//    }
//
//    @Override
//    public void updateGroupsInRole(String roleId, String authorizationStoreId,
//                                   List<Group> groupToBeAssign,
//                                   List<Group> groupToBeUnassign)
//            throws AuthorizationStoreException {
//
//        AuthorizationStoreConnector authorizationStoreConnector = authorizationStoreConnectors
//                .get(authorizationStoreId);
//
//        if (authorizationStoreConnector == null) {
//            throw new AuthorizationStoreException(String.format("No authorization store found for the given id: %s.",
//                                                                authorizationStoreId));
//        }
//
//        authorizationStoreConnector.updateGroupsInRole(roleId, groupToBeAssign, groupToBeUnassign);
//    }
//
//    @Override
//    public void updatePermissionsInRole(String roleId, String authorizationStoreId,
//                                        List<Permission> newPermissionList)
//            throws AuthorizationStoreException {
//
//        AuthorizationStoreConnector authorizationStoreConnector = authorizationStoreConnectors
//                .get(authorizationStoreId);
//
//        if (authorizationStoreConnector == null) {
//            throw new AuthorizationStoreException(String.format("No authorization store found for the given id: %s.",
//                                                                authorizationStoreId));
//        }
//
//        authorizationStoreConnector.updatePermissionsInRole(roleId, newPermissionList);
//    }
//
//    @Override
//    public void updatePermissionsInRole(String roleId, String authorizationStoreId,
//                                        List<Permission> permissionsToBeAssign,
//                                        List<Permission> permissionsToBeUnassign)
//            throws AuthorizationStoreException {
//
//        AuthorizationStoreConnector authorizationStoreConnector = authorizationStoreConnectors
//                .get(authorizationStoreId);
//
//        if (authorizationStoreConnector == null) {
//            throw new AuthorizationStoreException(String.format("No authorization store found for the given id: %s.",
//                                                                authorizationStoreId));
//        }
//
//        authorizationStoreConnector.updatePermissionsInRole(roleId, permissionsToBeAssign, permissionsToBeUnassign);
//    }
//
//    @Override
//    public Map<String, String> getAllAuthorizationStoreNames() {
//
//        return authorizationStoreConnectors.entrySet()
//                .stream()
//                .collect(Collectors.toMap(Map.Entry::getKey,
//                                          entry -> entry.getValue().getAuthorizationStoreConfig().getStoreProperties()
//                                                  .getProperty(IdentityMgtConstants.USERSTORE_DISPLAY_NAME, "")));
//    }
//
//    /**
//     * Get the roles with there respective authorization store id.
//     *
//     * @param roles List of roles.
//     * @return Roles grouped from there authorization store id.
//     */
//    private Map<String, List<Role>> getRolesWithAuthorizationStore(List<Role> roles) {
//
//        Map<String, List<Role>> roleMap = new HashMap<>();
//
//        if (roles == null) {
//            return roleMap;
//        }
//
//        for (Role role : roles) {
//            List<Role> roleList = roleMap.get(role.getAuthorizationStoreId());
//            if (roleList == null) {
//                roleList = new ArrayList<>();
//            }
//            roleList.add(role);
//            roleMap.put(role.getAuthorizationStoreId(), roleList);
//        }
//
//        return roleMap;
//    }
//
//    /**
//     * Get the primary authorization store id.
//     *
//     * @return Id of the primary authorization store.
//     */
//    private String getPrimaryAuthorizationStoreId() {
//
//        // To get the primary authorization store, first check whether the primary property is set to true,
//        // the connectors by there priority. If non of above properties were set, then get the first connector id from
//        // the list.
//        return authorizationStoreConnectors.entrySet()
//                .stream()
//                .filter(connectorEntry -> Boolean.parseBoolean((connectorEntry.getValue()
//                                                                        .getAuthorizationStoreConfig()
//                                                                        .getStoreProperties())
//                                            .getProperty(IdentityMgtConstants.PRIMARY_USERSTORE)))
//                .findFirst()
//                .map(Map.Entry::getKey)
//                .orElse(authorizationStoreConnectors.entrySet()
//                                .stream()
//                                .sorted((c1, c2) ->
//                      Integer.compare(Integer.parseInt(c1.getValue().getAuthorizationStoreConfig()
//                                                                                         .getStoreProperties()
//                                                         .getProperty(IdentityMgtConstants.USERSTORE_PRIORITY)),
//                                               Integer.parseInt(c2.getValue().getAuthorizationStoreConfig()
//                                                                                         .getStoreProperties()
//                                                .getProperty(IdentityMgtConstants.USERSTORE_PRIORITY))))
//                                .findFirst()
//                                .map(Map.Entry::getKey)
//                                .orElse(authorizationStoreConnectors.entrySet()
//                                                .stream()
//                                                .findFirst()
//                                                .map(Map.Entry::getKey)
//                                                .orElse(null)));
//    }
//}
