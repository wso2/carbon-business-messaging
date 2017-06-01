/*
 * Copyright (c) 2017, WSO2 Inc. (http://wso2.com) All Rights Reserved.
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

package org.wso2.carbon.business.messaging.admin.services.managers;

import org.wso2.andes.kernel.AndesMessage;
import org.wso2.andes.kernel.AndesMessageMetadata;
import org.wso2.carbon.business.messaging.admin.services.exceptions.InternalServerException;

import java.util.List;

/**
 * This interface provides the base for managing all dead letter channel related services.
 */
public interface DlcManagerService {

    /**
     * Delete a selected message list from a given Dead Letter Queue of a tenant.
     *
     * @param andesMetadataIDs The browser message Ids
     * @param dlcQueueName     The Dead Letter Queue Name for the tenant
     */
    void deleteMessagesFromDeadLetterQueue(long[] andesMetadataIDs, String dlcQueueName);

    /**
     * Get number of messages in DLC queue under specific queue.
     *
     * @param queueName the queue name which need to get message count
     * @param dlcQueueName     The Dead Letter Queue Name for the tenant
     */
    long getMessageCountInDLCForQueue(String queueName, String dlcQueueName) throws InternalServerException;

    /**
     * Get number of messages in DLC queue.
     *
     * @param dlcQueueName     The Dead Letter Queue Name for the tenant
     */
    long getMessagCountInDLC(String dlcQueueName) throws InternalServerException;

    /**
     * Get metadata of messages in DLC queue under specific queue.
     *
     * @param queueName The browser message Ids
     * @param dlcQueueName     The Dead Letter Queue Name for the
     * @param firstMsgId The starting message id
     * @param count     Number of messages which metadata is required
     */
    List<AndesMessageMetadata> getMessageMetadataInDLCForQueue(final String queueName,
                                                                    final String dlcQueueName, long firstMsgId, int
                                                                            count) throws InternalServerException;

    /**
     * Get content of messages in DLC queue under specific queue.
     *
     * @param queueName The browser message Ids
     * @param dlcQueueName     The Dead Letter Queue Name
     * @param firstMsgId The starting message id
     * @param count     Number of messages which metadata is required
     */
    List<AndesMessage> getMessageContentInDLCForQueue(final String queueName, final String dlcQueueName, long
            firstMsgId, int count) throws InternalServerException;

    /**
     * Reroute all the messages in a specific queue to a new destination.
     *
     * @param sourceQueue The queue in which messages need to be routed
     * @param dlcQueueName     The Dead Letter Queue Name
     * @param targetQueue The rerouting destination
     * @param internalBatchSize     Number of messages to be read in single database query
     * @param restoreToOriginalQueue To determine messages should be restored or rerouted
     */
    int rerouteAllMessagesInDeadLetterChannelForQueue(String dlcQueueName, String sourceQueue, String targetQueue,
                                                      int internalBatchSize, boolean restoreToOriginalQueue) throws
            InternalServerException;

    /**
     * Reroute selected set of messages in a specific queue to a new destination.
     *
     * @param sourceQueue The queue in which messages need to be routed
     * @param messageIds     The Ids of selected messages to be rerouted
     * @param targetQueue The rerouting destination
     * @param restoreToOriginalQueue To determine messages should be restored or rerouted
     */
    int moveMessagesFromDLCToNewDestination(List<Long> messageIds, String sourceQueue, String targetQueue, boolean
            restoreToOriginalQueue) throws InternalServerException;

    /**
     * To check whether the asking destination is available in broker.
     *
     * @param queueName The browser message Ids
     */
    boolean isQueueExists(String queueName);

}
