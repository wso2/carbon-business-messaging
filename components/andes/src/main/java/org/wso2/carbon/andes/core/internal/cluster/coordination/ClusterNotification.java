/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.andes.core.internal.cluster.coordination;

import java.io.Serializable;

/**
 * This class represents a cluster notification to be transfer via HazelCast
 */
public class ClusterNotification implements Serializable {

    private String changeType;
    private String encodedObjectAsString;
    private String description;

    /**
     * create an instance of cluster notification
     *
     * @param encodedAsString encoded string to transfer thro
     * @param changeType      change happened (added/deleted etc)
     * @param description     description what this notification is
     */
    public ClusterNotification(String encodedAsString, String changeType, String description) {
        this.encodedObjectAsString = encodedAsString;
        this.changeType = changeType;
        this.description = description;
    }

    /**
     * Get encoded string notification carries
     *
     * @return encoded notification
     */
    public String getEncodedObjectAsString() {
        return encodedObjectAsString;
    }

    /**
     * get the change notification carries
     *
     * @return change
     */
    public String getChangeType() {
        return changeType;
    }

    /**
     * get notification description
     *
     * @return description
     */
    public String getDescription() {
        return description;
    }
}
