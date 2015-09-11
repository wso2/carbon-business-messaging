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

package org.wso2.carbon.andes.admin.internal;

/**
 * Defines a role and the associated permissions as to whether consuming and publishing is permitted.
 */
public class QueueRolePermission {

    /**
     * The name of the role.
     */
    private String roleName;

    /**
     * True if users with the role are allowed to publish.
     */
    private boolean isAllowedToConsume;

    /**
     * True if users with the role are allowed to consume.
     */
    private boolean isAllowedToPublish;

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public boolean isAllowedToConsume() {
        return isAllowedToConsume;
    }

    public void setAllowedToConsume(boolean isAllowedToConsume) {
        this.isAllowedToConsume = isAllowedToConsume;
    }

    public boolean isAllowedToPublish() {
        return isAllowedToPublish;
    }

    public void setAllowedToPublish(boolean isAllowedToPublish) {
        this.isAllowedToPublish = isAllowedToPublish;
    }

    /**
     * convert the QueueRolePermission into {@link org.wso2.carbon.andes.core.types.QueueRolePermission}
     * @return the converted {@link org.wso2.carbon.andes.core.types.QueueRolePermission} object
     */
    public org.wso2.carbon.andes.core.types.QueueRolePermission convert(){
        return new org.wso2.carbon.andes.core.types.QueueRolePermission(roleName, isAllowedToConsume,
                isAllowedToPublish);
    }
}
