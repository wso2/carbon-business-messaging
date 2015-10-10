/*
*  Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.andes.event.ui;

import org.wso2.carbon.andes.event.stub.core.TopicRolePermission;

import java.util.ArrayList;

/**
 * This class is used by the UI to connect to services and provides utilities. Used by JSP pages.
 */
public class UIUtils {

    /**
     * Parent resource path where topic exist in registry
     */
    private static final String TOPICS = "/_system/governance/event/topics";

    /**
     * Gets subscription mode description.
     * Suppressing warning of unused declaration as it used by the UI (JSP pages)
     *
     * @param serverMode subscription mode
     * @return subscription mode description
     */
    @SuppressWarnings("UnusedDeclaration")
    public static String getSubscriptionMode(String serverMode) {
        if (serverMode.equals(UIConstants.SUBSCRIPTION_MODE_1)) {
            return UIConstants.SUBSCRIPTION_MODE_1_DESCRIPTION;
        } else if (serverMode.equals(UIConstants.SUBSCRIPTION_MODE_2)) {
            return UIConstants.SUBSCRIPTION_MODE_2_DESCRIPTION;
        } else {
            return UIConstants.SUBSCRIPTION_MODE_0_DESCRIPTION;
        }
    }

    /**
     * Filter the full user-roles list to suit the range.
     * Suppressing warning of unused declaration as it used by the UI (JSP pages)
     *
     * @param fullList      full list of roles
     * @param startingIndex starting index to filter
     * @param maxRolesCount maximum number of roles that the filtered list can contain
     * @return ArrayList<TopicRolePermission>
     */
    @SuppressWarnings("UnusedDeclaration")
    public static ArrayList<TopicRolePermission> getFilteredRoleList
    (ArrayList<TopicRolePermission> fullList, int startingIndex, int maxRolesCount) {
        int resultSetSize = maxRolesCount;

        if ((fullList.size() - startingIndex) < maxRolesCount) {
            resultSetSize = (fullList.size() - startingIndex);
        }

        ArrayList<TopicRolePermission> resultList = new ArrayList<TopicRolePermission>();
        for (int i = startingIndex; i < startingIndex + resultSetSize; i++) {
            resultList.add(fullList.get(i));
        }

        return resultList;
    }

    /**
     * Get unique id for a topic
     *
     * @param topicName Name of the topic
     * @return Topic id
     */
    public static String getTopicID(String topicName) {

        String topicID = TOPICS;

        topicName = topicName.replaceAll("\\.", "/");

        if (!topicName.startsWith("/")) {
            topicID += "/";
        }

        // this topic name can have # and * marks if the user wants to subscribes to the
        // child topics as well. but we consider the topic here as the topic name just before any
        // special character.
        // eg. if topic name is myTopic/*/* then topic name is myTopic
        if (topicName.contains("*")) {
            topicName = topicName.substring(0, topicName.indexOf("*"));
        } else if (topicName.contains("#")) {
            topicName = topicName.substring(0, topicName.indexOf("#"));
        }

        return topicID + topicName;
    }
}
