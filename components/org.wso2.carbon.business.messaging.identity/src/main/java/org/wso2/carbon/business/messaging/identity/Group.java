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
import org.wso2.carbon.business.messaging.identity.exception.StoreException;

import java.util.List;

//import org.wso2.carbon.business.messaging.identity.exception.IdentityStoreException;

/**
 * Group representing a collection of Users.
 */
public class Group {
    /**
     * Unique group id.
     */
    private String uniqueGroupId;

    /**
     * Domain in which the group belongs.
     */
    private String domainName;

    /**
     * The IdentityStore this user originates from.
     */
//    private transient IdentityStore identityStore;

    /**
     * The AuthorizationStore this user originates from.
     */
    private transient AuthorizationStore authorizationStore;


    private Group(String uniqueGroupId, String domainName) {

        this.uniqueGroupId = uniqueGroupId;
        this.domainName = domainName;
    }

    /**
     * Get the group id.
     *
     * @return Group id.
     */
    public String getUniqueGroupId() {
        return uniqueGroupId;
    }

    /**
     * Get this group's domainName name.
     *
     * @return Domain name of this group.
     */
    public String getDomainName() {
        return domainName;
    }

    /**
     * Get claims of this group.
     *
     * @return List of Group claims.
     * @throws IdentityStoreException Identity store exception.
     */
//    public List<Claim> getClaims() throws IdentityStoreException, GroupNotFoundException {
//        return identityStore.getClaimsOfGroup(this.uniqueGroupId);
//    }

    /**
     * Get claims of this group for given URIs.
     *
     * @param metaClaims Claim URIs that needs to be retrieved.
     * @return List of Group claims.
     * @throws IdentityStoreException Identity store exception.
     */
//    public List<Claim> getClaims(List<MetaClaim> metaClaims) throws IdentityStoreException, GroupNotFoundException {
//        return identityStore.getClaimsOfGroup(uniqueGroupId, metaClaims);
//    }

    /**
     * Get the users assigned to this group.
     *
     * @return List of users assigned to this group.
     * @throws IdentityStoreException Identity store exception.
     */
//    public List<User> getUsers() throws IdentityStoreException, GroupNotFoundException {
//        return identityStore.getUsersOfGroup(uniqueGroupId);
//    }

    /**
     * Checks whether the User in this Group.
     *
     * @param userId Id of the User to be checked.
     * @return True if User is in this Group.
     * @throws IdentityStoreException Identity store exception.
     */
//    public boolean hasUser(String userId) throws IdentityStoreException,
// UserNotFoundException, GroupNotFoundException {
//        return identityStore.isUserInGroup(userId, uniqueGroupId);
//    }

    /**
     * Change the identity store
     * @param identityStore identity store instance
     */
//    public void setIdentityStore(IdentityStore identityStore) {
//        this.identityStore = identityStore;
//    }

    /**
     * Change the authorization store
     *
     * @param authorizationStore identity store instance
     */
    public void setAuthorizationStore(AuthorizationStore authorizationStore) {
        this.authorizationStore = authorizationStore;
    }

    @Override
    public String toString() {
        return "Group{" +
               "uniqueGroupId='" + uniqueGroupId + '\'' +
               ", domainName='" + domainName + '\'' +
               '}';
    }

    /**
     * Get Roles assigned to this Group.
     *
     * @return List of Roles.
     * @throws AuthorizationStoreException Authorization store exception.
     */
    public List<Role> getRoles() throws AuthorizationStoreException {
        return authorizationStore.getRolesOfGroup(uniqueGroupId);
    }

    /**
     * Checks whether this Group is authorized for given Permission.
     *
     * @param permission Permission to be checked.
     * @return True if authorized.
     * @throws AuthorizationStoreException Authorization store exception.
     */
    public boolean isAuthorized(Permission permission) throws AuthorizationStoreException {
        return authorizationStore.isGroupAuthorized(uniqueGroupId, permission);
    }

    /**
     * Checks whether this Group has the Role.
     *
     * @param roleName Name of the Role to be checked.
     * @return True if this Group has the Role.
     * @throws AuthorizationStoreException Authorization store exception.
     */
    public boolean hasRole(String roleName) throws AuthorizationStoreException {

        return authorizationStore.isGroupInRole(uniqueGroupId, roleName);
    }

    /**
     * Add a new Role list by <b>replacing</b> the existing Role list. (PUT)
     *
     * @param newRoleList List of Roles needs to be assigned to this Group.
     * @throws AuthorizationStoreException Authorization store exception.
     */
    public void updateRoles(List<Role> newRoleList) throws AuthorizationStoreException {
        authorizationStore.updateRolesInGroup(uniqueGroupId, newRoleList);
    }

    /**
     * Assign a new list of Roles to existing list and/or un-assign Roles from existing list. (PATCH)
     *
     * @param assignList   List to be added to the new list.
     * @param unAssignList List to be removed from the existing list.
     * @throws AuthorizationStoreException Authorization store exception.
     */
    public void updateRoles(List<Role> assignList, List<Role> unAssignList)
            throws AuthorizationStoreException {
        authorizationStore.updateRolesInGroup(uniqueGroupId, assignList, unAssignList);
    }


    /**
     * Builder for group bean.
     */
    public static class GroupBuilder {

        private String groupId;

        private String domainName;

//        private IdentityStore identityStore;

        private AuthorizationStore authorizationStore;

        public String getGroupId() {
            return groupId;
        }

        public String getDomainName() {
            return domainName;
        }

//        public IdentityStore getIdentityStore() {
//            return identityStore;
//        }

        public AuthorizationStore getAuthorizationStore() {
            return authorizationStore;
        }

        public GroupBuilder setGroupId(String groupId) {
            this.groupId = groupId;
            return this;
        }

        public GroupBuilder setDomainName(String domainName) {
            this.domainName = domainName;
            return this;
        }

//        public GroupBuilder setIdentityStore(IdentityStore identityStore) {
//            this.identityStore = identityStore;
//            return this;
//        }

        public GroupBuilder setAuthorizationStore(AuthorizationStore authorizationStore) {
            this.authorizationStore = authorizationStore;
            return this;
        }

        public Group build() {

            //TODO Add identityStore check
            if (groupId == null || authorizationStore == null || domainName == null) {
                throw new StoreException("Required data missing for building group.");
            }

            Group group = new Group(groupId, domainName);
            // group.setIdentityStore(identityStore);
            group.setAuthorizationStore(authorizationStore);
            return group;
        }
    }
}
