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

import org.wso2.carbon.business.messaging.identity.exception.StoreException;

/**
 * Permission bean
 */
public class Permission {

    private String authorizationStoreId;
    private String permissionId;

    private Resource resource;
    private Action action;

    public Permission(String resource, String action) throws StoreException {
        this.resource = new Resource(resource);
        this.action = new Action(action);
    }

    public Permission(Resource resource, Action action) {
        this.resource = resource;
        this.action = action;
    }

    private Permission(Resource resource, Action action, String permissionId,
                       String authorizationStoreId) {
        this.resource = resource;
        this.action = action;
        this.permissionId = permissionId;
        this.authorizationStoreId = authorizationStoreId;
    }

    /**
     * Get the unique id of this identity.
     *
     * @return Permission id.
     */
    public String getPermissionId() {
        return permissionId;
    }

    /**
     * Get the authorization store id.
     *
     * @return Authorization store id.
     */
    public String getAuthorizationStoreId() {
        return authorizationStoreId;
    }

    /**
     * Get the identity String (Resource ID + Action).
     *
     * @return Permission string.
     */
    public String getPermissionString() {
        return resource.getResourceId() + action.getActionString();
    }

    /**
     * Get the resource id.
     *
     * @return Resource id.
     */
    public Resource getResource() {
        return resource;
    }

    /**
     * Get the action.
     *
     * @return Action.
     */
    public Action getAction() {
        return action;
    }

    @Override
    public boolean equals(Object permission) {
        return permission instanceof Permission
               && ((Permission) permission).getPermissionString().equals(this.getPermissionString());
    }

    @Override
    public int hashCode() {
        return getPermissionString().hashCode();
    }

    /**
     * Builder for the identity bean.
     */
    public static class PermissionBuilder {

        private Resource resource;
        private Action action;
        private String permissionId;
        private String authorizationStoreId;

        public PermissionBuilder(Resource resource, Action action, String permissionId,
                                 String authorizationStoreId) {
            this.resource = resource;
            this.action = action;
            this.permissionId = permissionId;
            this.authorizationStoreId = authorizationStoreId;
        }

        public Permission build() throws StoreException {

            if (resource == null || action == null || permissionId == null || authorizationStoreId == null) {
                throw new StoreException("Required data missing for building identity.");
            }

            return new Permission(resource, action, permissionId, authorizationStoreId);
        }
    }
}


