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
package org.wso2.carbon.business.messaging.identity.connector;

import org.wso2.carbon.business.messaging.identity.Action;
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

/**
 * Connector for authorization store
 */
public interface AuthorizationStoreConnector {
    /**
     * Initialize the authorization store.
     *
     * @param storeId                           Id of this store.
     * @param authorizationStoreConnectorConfig Authorization store configurations for this connector.
     * @throws AuthorizationStoreException Authorization store exception.
     */
    void init(String storeId, AuthorizationStoreConnectorConfig authorizationStoreConnectorConfig)
            throws AuthorizationStoreException;

    /**
     * Get the role of from role id.
     *
     * @param roleId Id of the Role
     * @return Role.RoleBuilder.
     * @throws AuthorizationStoreException Authorization Store Exception.
     */
    Role.RoleBuilder getRole(String roleId)
            throws RoleNotFoundException, AuthorizationStoreException;

    /**
     * Get the count of the roles available in the authorization store.
     *
     * @return Number of roles.
     * @throws AuthorizationStoreException Authorization Store Exception
     */
    int getRoleCount() throws AuthorizationStoreException;

    /**
     * List the roles according to the filter pattern.
     *
     * @param filterPattern Filter pattern to be used.
     * @param offset        Offset to be used.
     * @param length        Number of roles from the offset.
     * @return List of roles.
     * @throws AuthorizationStoreException Authorization Store Exception
     */
    List<Role.RoleBuilder> listRoles(String filterPattern, int offset, int length)
            throws AuthorizationStoreException;

    /**
     * Get permission from the resource id and action.
     *
     * @param resource Resource of this permission.
     * @param action   Action of the permission.
     * @return Permission.PermissionBuilder.
     * @throws AuthorizationStoreException Authorization Store Exception
     */
    Permission.PermissionBuilder getPermission(Resource resource, Action action) throws
                                                                                 PermissionNotFoundException,
                                                                                 AuthorizationStoreException;

    /**
     * Get the count of the permissions available in the authorization store.
     *
     * @return Number of permissions.
     */
    int getPermissionCount() throws AuthorizationStoreException;

    /**
     * List the permissions according to the filter pattern.
     *
     * @param resourcePattern Resource pattern to be used.
     * @param actionPattern   Action pattern to be used.
     * @param offset          Offset to be used.
     * @param length          Number of permissions from the offset.
     * @return List of permissions.
     * @throws AuthorizationStoreException Authorization Store Exception
     */
    List<Permission.PermissionBuilder> listPermissions(String resourcePattern, String actionPattern,
                                                       int offset,
                                                       int length)
            throws AuthorizationStoreException;

    /**
     * Get a list of resources matched for given resource id pattern.
     *
     * @param resourcePattern Resource id pattern.
     * @return List of resources.
     * @throws AuthorizationStoreException Authorization Store Exception
     */
    List<Resource.ResourceBuilder> getResources(String resourcePattern)
            throws AuthorizationStoreException;

    /**
     * Get a list of actions matched for give action name pattern.
     *
     * @param actionPattern Action name pattern.
     * @return List of actions.
     * @throws AuthorizationStoreException Authorization Store Exception
     */
    List<Action.ActionBuilder> getActions(String actionPattern) throws AuthorizationStoreException;

    /**
     * Get roles for the user id.
     *
     * @param userId User id of the user.
     * @return Roles associated to the user.
     * @throws AuthorizationStoreException Authorization Store Exception.
     */
    List<Role.RoleBuilder> getRolesForUser(String userId) throws AuthorizationStoreException;

    /**
     * Get roles associated to the group.
     *
     * @param groupId Unique id of the group.
     * @return Roles associated to the group.
     * @throws AuthorizationStoreException Authorization Store Exception.
     */
    List<Role.RoleBuilder> getRolesForGroup(String groupId) throws AuthorizationStoreException;

    /**
     * Get permissions associated to the role.
     *
     * @param roleId   Role id of the required role.
     * @param resource Resource which the permissions should take.
     * @return List of permissions associated to the Role.
     * @throws AuthorizationStoreException Authorization Store Exception.
     */
    List<Permission.PermissionBuilder> getPermissionsForRole(String roleId, Resource resource)
            throws AuthorizationStoreException;

    /**
     * Get permissions associated to the role.
     *
     * @param roleId Role id of the required role.
     * @param action Action which the permissions should take.
     * @return List of permissions associated to the Role.
     * @throws AuthorizationStoreException Authorization Store Exception.
     */
    List<Permission.PermissionBuilder> getPermissionsForRole(String roleId, Action action)
            throws AuthorizationStoreException;

    /**
     * Add new resource.
     *
     * @param resourceNamespace Namespace of the resource.
     * @param resourceId        Id of the resource.
     * @param userId            User id of the owner.
     * @return New Resource.
     * @throws AuthorizationStoreException
     */
    Resource.ResourceBuilder addResource(String resourceNamespace, String resourceId, String userId)
            throws AuthorizationStoreException;

    /**
     * Add new action.
     *
     * @param actionNamespace Namespace of the action.
     * @param actionName      Name of the action.
     * @return New action.
     * @throws AuthorizationStoreException
     */
    Action addAction(String actionNamespace, String actionName) throws AuthorizationStoreException;

    /**
     * Add new permission.
     *
     * @param resource Resource.
     * @param action   Action.
     * @return New permission.
     * @throws AuthorizationStoreException Authorization Store Exception.
     */
    Permission.PermissionBuilder addPermission(Resource resource, Action action)
            throws AuthorizationStoreException;

    /**
     * Add new role.
     *
     * @param roleName    Name of the new role.
     * @param permissions List of permissions to be assign.
     * @return New Role.RoleBuilder.
     * @throws AuthorizationStoreException Authorization Store Exception.
     */
    Role.RoleBuilder addRole(String roleName, List<Permission> permissions)
            throws AuthorizationStoreException;

    /**
     * Checks whether the users is in the role.
     *
     * @param userId   Id of the user.
     * @param roleName Name of the role.
     * @return True if user is in the role.
     * @throws AuthorizationStoreException Authorization Store Exception.
     */
    boolean isUserInRole(String userId, String roleName) throws AuthorizationStoreException;

    /**
     * Checks whether the group is in the role.
     *
     * @param groupId  Id of the group.
     * @param roleName Name of the role.
     * @return True if the group is in the role.
     * @throws AuthorizationStoreException Authorization Store Exception.
     */
    boolean isGroupInRole(String groupId, String roleName) throws AuthorizationStoreException;

    /**
     * Get the users of the role.
     *
     * @param roleId Id of the role.
     * @return List of @see User.UserBuilder.
     * @throws AuthorizationStoreException Authorization Store Exception.
     */
    List<User.UserBuilder> getUsersOfRole(String roleId) throws AuthorizationStoreException;

    /**
     * Get the groups of the role.
     *
     * @param roleId Id of the role.
     * @return List of @see Group.GroupBuilder.
     * @throws AuthorizationStoreException Authorization Store Exception.
     */
    List<Group.GroupBuilder> getGroupsOfRole(String roleId) throws AuthorizationStoreException;

    /**
     * Delete the specified role.
     *
     * @param roleId Id of the role.
     * @throws AuthorizationStoreException Authorization Store Exception.
     */
    void deleteRole(String roleId) throws AuthorizationStoreException;

    /**
     * Delete the specified permission.
     *
     * @param permissionId Id of the permission.
     * @throws AuthorizationStoreException Authorization Store Exception.
     */
    void deletePermission(String permissionId) throws AuthorizationStoreException;

    /**
     * Deletes the given resource.
     *
     * @param resource Resource to be deleted.
     * @throws AuthorizationStoreException
     */
    void deleteResource(Resource resource) throws AuthorizationStoreException;

    /**
     * Delete the given action.
     *
     * @param action Action to be deleted.
     * @throws AuthorizationStoreException
     */
    void deleteAction(Action action) throws AuthorizationStoreException;

    /**
     * Update the roles of the user by replacing existing roles. (PUT)
     *
     * @param userId      Id of the user.
     * @param newRoleList Role list to replace the existing.
     * @throws AuthorizationStoreException Authorization Store Exception.
     */
    void updateRolesInUser(String userId, List<Role> newRoleList)
            throws AuthorizationStoreException;

    /**
     * Add a new User list by <b>replacing</b> the existing User list. (PUT)
     *
     * @param roleId      Id of the role.
     * @param newUserList New user list.
     * @throws AuthorizationStoreException Authorization Store Exception.
     */
    void updateUsersInRole(String roleId, List<User> newUserList)
            throws AuthorizationStoreException;

    /**
     * Add a new Role list by <b>replacing</b> the existing Role list. (PUT)
     *
     * @param groupId     Id of the group.
     * @param newRoleList List
     * @throws AuthorizationStoreException Authorization Store Exception.
     */
    void updateRolesInGroup(String groupId, List<Role> newRoleList)
            throws AuthorizationStoreException;

    /**
     * Add a new Group list by <b>replacing</b> the existing Group list. (PUT)
     *
     * @param roleId       Id of the role.
     * @param newGroupList New group list.
     * @throws AuthorizationStoreException Authorization Store Exception.
     */
    void updateGroupsInRole(String roleId, List<Group> newGroupList)
            throws AuthorizationStoreException;

    /**
     * Add a new Permission list by <b>replacing</b> the existing Permission list. (PUT)
     *
     * @param roleId            Id of the role.
     * @param newPermissionList New permissions list.
     * @throws AuthorizationStoreException Authorization Store Exception.
     */
    void updatePermissionsInRole(String roleId, List<Permission> newPermissionList) throws
                                                                                    AuthorizationStoreException;

    /**
     * Assign a new list of Permissions to existing list and/or un-assign Permission from existing Permission. (PATCH)
     *
     * @param roleId                  Id of the role.
     * @param permissionsToBeAssign   List of permissions to be assign.
     * @param permissionsToBeUnassign List of permissions to be un assign.
     * @throws AuthorizationStoreException Authorization Store Exception.
     */
    void updatePermissionsInRole(String roleId, List<Permission> permissionsToBeAssign,
                                 List<Permission> permissionsToBeUnassign)
            throws AuthorizationStoreException;

    /**
     * Assign a new list of Roles to existing list and/or un-assign Roles from existing list. (PATCH)
     *
     * @param userId            Id of the user.
     * @param rolesToBeAssign   List of roles to be assign.
     * @param rolesToBeUnassign List of roles to be un assign.
     * @throws AuthorizationStoreException Authorization Store Exception.
     */
    void updateRolesInUser(String userId, List<Role> rolesToBeAssign, List<Role> rolesToBeUnassign)
            throws AuthorizationStoreException;

    /**
     * Assign a new list of User to existing list and/or un-assign Permission from existing User. (PATCH)
     *
     * @param roleId            Id of the role.
     * @param usersToBeAssign   List of users to be assign.
     * @param usersToBeUnassign List of users to un assign.
     * @throws AuthorizationStoreException Authorization Store Exception.
     */
    void updateUsersInRole(String roleId, List<User> usersToBeAssign, List<User> usersToBeUnassign)
            throws AuthorizationStoreException;

    /**
     * Assign a new list of Group to existing list and/or un-assign Group from existing Group. (PATCH)
     *
     * @param roleId            Id of the role.
     * @param groupToBeAssign   List of groups to be assign.
     * @param groupToBeUnassign List of groups to be un assign.
     * @throws AuthorizationStoreException Authorization Store Exception.
     */
    void updateGroupsInRole(String roleId, List<Group> groupToBeAssign,
                            List<Group> groupToBeUnassign)
            throws AuthorizationStoreException;

    /**
     * Assign a new list of Roles to existing list and/or un-assign Roles from existing list. (PATCH)
     *
     * @param groupId             Id of the group.
     * @param rolesToBeAssign     List of roles to be assign.
     * @param rolesToBeUnassigned List of roles to be un assign.
     * @throws AuthorizationStoreException Authorization Store Exception.
     */
    void updateRolesInGroup(String groupId, List<Role> rolesToBeAssign,
                            List<Role> rolesToBeUnassigned)
            throws AuthorizationStoreException;

    /**
     * Get the authorization store config.
     *
     * @return AuthorizationStoreConnectorConfig.
     */
    AuthorizationStoreConnectorConfig getAuthorizationStoreConfig();

    /**
     * Get the id of this authorization store.
     *
     * @return Id of the authorization store.
     */
    String getAuthorizationStoreId();

}
