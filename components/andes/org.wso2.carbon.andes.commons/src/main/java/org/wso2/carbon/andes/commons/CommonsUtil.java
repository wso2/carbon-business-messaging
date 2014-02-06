/*
 *  Copyright (c) 2008, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.wso2.carbon.andes.commons;

import org.wso2.carbon.context.CarbonContext;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Utility functions for Registry operations
 */
public class CommonsUtil {

    private static final String JMS_QUEUES = "event/queues/jms";
    private static final String TOPICS = "event/topics";
    private static final String JMS_SUBSCRIPTIONS = "jms.subscriptions";

    private static final String DATE_FORMAT = "yyyy/MM/dd HH:mm:ss";

    /**
         * Get unique id for a queue
         *
         * @param queueName
         *                  Name of the queue
         * @return
         *                  Queue id
         */
    public static String getQueueID(String queueName) {

        //if the queue name has the tenant domain prefix we need to remove it
        if (CarbonContext.getThreadLocalCarbonContext().getTenantId() > 0) {
            String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
            tenantDomain = tenantDomain.replace(".", "-");
            if (queueName.startsWith(tenantDomain)){
                queueName = queueName.substring(tenantDomain.length() + 1);
            }
        }

        if (queueName.indexOf(";") > -1){
            queueName = queueName.substring(0, queueName.indexOf(";"));
        }
        return JMS_QUEUES + "/" + queueName;
    }

    /**
        * Get unique is for queue root
        *
        * @return
        *           Unique string id 
        */
    public static String getQueuesID() {
        return JMS_QUEUES;
    }

    /**
         * Get unique id for a topic
         *
         * @param topicName
         *                  Name of the topic
         * @return
         *                  Topic id
         */
    public static String getTopicID(String topicName) {

        String topicID = TOPICS;

        topicName = topicName.replaceAll("\\.","/");

        if (!topicName.startsWith("/")) {
            topicID += "/";
        }

        // this topic name can have # and * marks if the user wants to subscribes to the
        // child topics as well. but we consider the topic here as the topic name just before any
        // special charactor.
        // eg. if topic name is myTopic/*/* then topic name is myTopic
        if (topicName.indexOf("*") > -1){
            topicName = topicName.substring(0, topicName.indexOf("*"));
        } else if (topicName.indexOf("#") > -1){
            topicName = topicName.substring(0, topicName.indexOf("#"));
        }

        return topicID + topicName;
    }

    /**
         * Get unique id for a topic subscription
         *
         * @param topicName
         *                  Name of the topic
         * @param subscriptionName
         *                  Unique name of the subscription
         * @return
         *                  Subscription id
         */
    public static String getSubscriptionID(String topicName, String subscriptionName) {
        return getTopicID(topicName) + "/" + JMS_SUBSCRIPTIONS + "/" + subscriptionName;
    }

    /**
        * Get root id for subscriptions
        *
        * @param topicName
        *               Name of the topic
        * @return
        *               Array of subscriptions 
        */
    public static String getSubscriptonsID(String topicName) {
        return getTopicID(topicName) + "/" + JMS_SUBSCRIPTIONS;
    }

    /**
        * Get current date and time
        *
        * @return
        *           Current data and time
        */
    public static String getCurrentTime() {
        DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);

        return dateFormat.format(new Date());
    }
}
