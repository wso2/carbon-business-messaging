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

import java.util.List;

/**
 * This interface provides the base for managing all dead letter channel related services.
 */
public interface DLCManagerService {
    /**
     * Restore a given browser message Id list from the Dead Letter Queue to the same queue it was previous in before
     * moving to the Dead Letter Queue and remove them from the Dead Letter Queue.
     *
     * @param andesMetadataIDs The browser message Ids
     * @param dlcQueueName     The Dead Letter Queue Name for the tenant
     */
    void restoreMessagesFromDeadLetterQueue(List<Long> andesMetadataIDs, String dlcQueueName);

    /**
     * Restore a given browser message Id list from the Dead Letter Queue to a different given queue in the same tenant
     * and remove them from the Dead Letter Queue.
     *
     * @param andesMetadataIDs        The browser message Ids
     * @param newDestinationQueueName The new destination
     * @param dlcQueueName            The Dead Letter Queue Name for the tenant
     */
    void restoreMessagesFromDeadLetterQueue(List<Long> andesMetadataIDs, String newDestinationQueueName,
            String dlcQueueName);

    /**
     * Delete a selected message list from a given Dead Letter Queue of a tenant.
     *
     * @param andesMetadataIDs The browser message Ids
     * @param dlcQueueName     The Dead Letter Queue Name for the tenant
     */
    void deleteMessagesFromDeadLetterQueue(List<Long> andesMetadataIDs, String dlcQueueName);
}
