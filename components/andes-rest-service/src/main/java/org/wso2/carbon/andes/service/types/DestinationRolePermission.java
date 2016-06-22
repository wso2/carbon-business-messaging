/*
 * Copyright (c) 2016, WSO2 Inc. (http://wso2.com) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.andes.service.types;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * Class for representing permissions for role to a specific destination.
 */
@ApiModel(value = "Destination Role Permission", description = "The structure for permissions assigned to a role " +
                                                              "which can be linked to a destination.")
public class DestinationRolePermission {
    @ApiModelProperty(value = "The name of the role which the permission is assigned to.", required = true)
    private String roleName;
    @ApiModelProperty(value = "Whether consumer permission is assigned for the role.", required = true)
    private boolean isAllowedToConsume;
    @ApiModelProperty(value = "Whether publisher permission is assigned for the role.", required = true)
    private boolean isAllowedToPublish;

    public DestinationRolePermission(String role, boolean allowedToConsume, boolean allowedToPublish) {
        roleName = role;
        isAllowedToConsume = allowedToConsume;
        isAllowedToPublish = allowedToPublish;
    }

    public String getRoleName() {
        return roleName;
    }

    public boolean isAllowedToConsume() {
        return isAllowedToConsume;
    }

    public boolean isAllowedToPublish() {
        return isAllowedToPublish;
    }

    @Override
    public int hashCode() {
        int result = roleName.hashCode();
        result = 31 * result + (isAllowedToConsume ? 1 : 0);
        result = 31 * result + (isAllowedToPublish ? 1 : 0);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DestinationRolePermission that = (DestinationRolePermission) o;

        return isAllowedToConsume == that.isAllowedToConsume && isAllowedToPublish == that.isAllowedToPublish &&
               roleName.equals(that.roleName);

    }
}
