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

package org.wso2.carbon.andes.core.management.mbeans;

import java.util.Map;
import javax.management.MBeanException;

/**
 * This interface contains all operations invoked by the UI console with relation to queues. (addition, deletion,
 * purging, etc.)
 */
public interface QueueManagementInformationMXBean {

    /***
     * Retrieve all destination queue names.
     *
     * @return List of queue names.
     */
    String[] getAllQueueNames();

    /**
     * Retrieve all queues with message counts
     *
     * @return List of all queues with the messageCounts
     */
    Map<String, Integer> getAllQueueCounts();

    /**
     * Retrieve current message count of a queue. This may be only a rough estimate in a fast pub/sub scenario.
     *
     * @param queueName  name of queue
     * @param msgPattern The exchange type used to transfer messages with the given queueName.
     * @return Count of messages in store for the given queue.
     */
    long getMessageCount(String queueName, String msgPattern) throws MBeanException;

    /***
     * Retrieve number of subscribers (active/inactive) for a given queue.
     *
     * @param queueName name of queue
     * @return Number of subscriptions listening to the given queue.
     */
    int getSubscriptionCount(String queueName);

    /***
     * Verify whether the given queue exists in broker.
     *
     * @param queueName name of queue
     * @return true if the queue exists in the server.
     */
    boolean isQueueExists(String queueName);

    /**
     * Purge the given queue both in terms of stored messages and in-memory messages. Ideally, all messages not awaiting
     * acknowledgement at the time of purge should be cleared from the broker.
     *
     * @param queueName name of queue
     */
    void deleteAllMessagesInQueue(String queueName, String ownerName) throws MBeanException;

}
