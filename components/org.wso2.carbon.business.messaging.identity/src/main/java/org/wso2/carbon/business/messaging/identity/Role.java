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
package org.wso2.carbon.business.messaging.identity;

import org.wso2.carbon.business.messaging.identity.exception.AuthorizationStoreException;
import org.wso2.carbon.business.messaging.identity.exception.IdentityStoreException;
import org.wso2.carbon.business.messaging.identity.exception.StoreException;

import java.util.List;

/**
 * Role class to represent user-role definition
 */
public class Role {

    private String roleName;
    private String roleId;
    private String authorizationStoreId;
    private AuthorizationStore authorizationStore;

    private Role(String roleName, String roleId, String authorizationStoreId,
                 AuthorizationStore authorizationStore) {

        this.roleName = roleName;
        this.roleId = roleId;
        this.authorizationStoreId = authorizationStoreId;
        this.authorizationStore = authorizationStore;
    }

    /**
     * Get the name of this Role.
     *
     * @return Role name.
     */
    public String getName() {
        return roleName;
    }

    /**
     * Get the ID of the role.
     *
     * @return Id of the role.
     */
    public String getRoleId() {
        return roleId;
    }

    /**
     * Get the authorization store id.
     *
     * @return Id of the authorization store.
     */
    public String getAuthorizationStoreId() {
        return authorizationStoreId;
    }

    /**
     * Get the users assigned to this role.
     *
     * @return List of users assigned to this role.
     * @throws AuthorizationStoreException Authorization store exception.
     */
    public List<User> getUsers() throws AuthorizationStoreException {
        return authorizationStore.getUsersOfRole(roleId, authorizationStoreId);
    }

    /**
     * Get all Permissions assign to this Role.
     *
     * @return List of Permission.
     * @throws AuthorizationStoreException Authorization store exception.
     */
    public List<Permission> getPermissions() throws AuthorizationStoreException {
        return authorizationStore.getPermissionsOfRole(roleId, authorizationStoreId);
    }

    /**
     * Get all Permissions assign to this Role filtered from resource.
     *
     * @return List of Permission.
     * @throws AuthorizationStoreException Authorization store exception.
     */
    public List<Permission> getPermissions(Resource resource) throws AuthorizationStoreException {
        return authorizationStore.getPermissionsOfRole(roleId, authorizationStoreId, resource);
    }

    /**
     * Get all Permissions assign to this Role filtered from action.
     *
     * @return List of Permission.
     * @throws AuthorizationStoreException Authorization store exception.
     */
    public List<Permission> getPermissions(Action action) throws AuthorizationStoreException {
        return authorizationStore.getPermissionsOfRole(roleId, authorizationStoreId, action);
    }

    /**
     * Get all Groups assigned to this Role.
     *
     * @return List of Group.
     * @throws AuthorizationStoreException Authorization store exception.
     * @throws IdentityStoreException      Identity store exception.
     */
    public List<Group> getGroups() throws AuthorizationStoreException, IdentityStoreException {
        return authorizationStore.getGroupsOfRole(roleId, authorizationStoreId);
    }

    /**
     * Checks whether this Role is authorized for given Permission.
     *
     * @param permission Permission to be checked.
     * @return True if authorized.
     * @throws AuthorizationStoreException Authorization store exception.
     */
    public boolean isAuthorized(Permission permission) throws AuthorizationStoreException {
        return authorizationStore.isRoleAuthorized(roleId, authorizationStoreId, permission);
    }

    /**
     * Checks whether the User is in this Role.
     *
     * @param userId Id of the User to be checked.
     * @return True if User exists.
     * @throws AuthorizationStoreException Authorization store exception.
     */
    public boolean hasUser(String userId) throws AuthorizationStoreException {
        return authorizationStore.isUserInRole(userId, roleName);
    }

    /**
     * Checks whether the Group is in this Role.
     *
     * @param groupId Id of the Group to be checked.
     * @return True if the Group exists.
     * @throws AuthorizationStoreException Authorization store exception.
     */
    public boolean hasGroup(String groupId) throws AuthorizationStoreException {
        return authorizationStore.isGroupInRole(groupId, roleName);
    }

    /**
     * Add a new Permission list by <b>replacing</b> the existing Permission list. (PUT)
     *
     * @param newPermissionList New Permission list that needs to replace the existing list.
     * @throws AuthorizationStoreException Authorization store exception.
     */
    public void updatePermissions(List<Permission> newPermissionList)
            throws AuthorizationStoreException {
        authorizationStore.updatePermissionsInRole(roleId, authorizationStoreId, newPermissionList);
    }

    /**
     * Assign a new list of Permissions to existing list and/or un-assign Permission from existing Permission. (PATCH)
     *
     * @param assignList   List to be added to the new list.
     * @param unAssignList List to be removed from the existing list.
     * @throws AuthorizationStoreException Authorization store exception.
     */
    public void updatePermissions(List<Permission> assignList, List<Permission> unAssignList)
            throws AuthorizationStoreException {
        authorizationStore.updatePermissionsInRole(roleId, authorizationStoreId, assignList, unAssignList);
    }

    /**
     * Add a new User list by <b>replacing</b> the existing User list. (PUT)
     *
     * @param newUserList New User list that needs to replace the existing list.
     * @throws AuthorizationStoreException Authorization store exception.
     */
    public void updateUsers(List<User> newUserList) throws AuthorizationStoreException {
        authorizationStore.updateUsersInRole(roleId, authorizationStoreId, newUserList);
    }

    /**
     * Assign a new list of User to existing list and/or un-assign Permission from existing User. (PATCH)
     *
     * @param assignList   List to be added to the new list.
     * @param unAssignList List to be removed from the existing list.
     * @throws AuthorizationStoreException Authorization store exception.
     */
    public void updateUsers(List<User> assignList, List<User> unAssignList)
            throws AuthorizationStoreException {
        authorizationStore.updateUsersInRole(roleName, authorizationStoreId, assignList, unAssignList);
    }

    /**
     * Add a new Group list by <b>replacing</b> the existing Group list. (PUT)
     *
     * @param newGroupList New Group list that needs to replace the existing list.
     * @throws AuthorizationStoreException Authorization store exception.
     */
    public void updateGroups(List<Group> newGroupList) throws AuthorizationStoreException {
        authorizationStore.updateGroupsInRole(roleName, authorizationStoreId, newGroupList);
    }

    /**
     * Assign a new list of Group to existing list and/or un-assign Group from existing Group. (PATCH)
     *
     * @param assignList   List to be added to the new list.
     * @param unAssignList List to be removed from the existing list.
     * @throws AuthorizationStoreException Authorization store exception.
     */
    public void updateGroups(List<Group> assignList, List<Group> unAssignList)
            throws AuthorizationStoreException {
        authorizationStore.updateGroupsInRole(roleId, authorizationStoreId, assignList, unAssignList);
    }

    /**
     * Builder for role bean.
     */
    public static class RoleBuilder {

        private static final long serialVersionUID = -7097267952117338236L;

        private String roleName;
        private String roleId;
        private String authorizationStoreId;

        private transient AuthorizationStore authorizationStore;

        public RoleBuilder setRoleName(String roleName) {
            this.roleName = roleName;
            return this;
        }

        public RoleBuilder setRoleId(String roleId) {
            this.roleId = roleId;
            return this;
        }

        public RoleBuilder setAuthorizationStoreId(String authorizationStoreId) {
            this.authorizationStoreId = authorizationStoreId;
            return this;
        }

        public RoleBuilder setAuthorizationStore(AuthorizationStore authorizationStore) {
            this.authorizationStore = authorizationStore;
            return this;
        }

        public Role build() {

            if (roleName == null || roleId == null || authorizationStoreId == null || authorizationStore == null) {
                throw new StoreException("Required data missing for building role.");
            }

            return new Role(roleName, roleId, authorizationStoreId, authorizationStore);
        }
    }
}

