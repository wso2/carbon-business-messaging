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

package org.wso2.carbon.andes.core.internal.slot;

/**
 * This interface is responsible for coordinating with the SlotManagerClusterMode
 */
public interface SlotCoordinator {

    /**
     * Get a slot from SlotManagerClusterMode
     *
     * @param queueName Name of the queue
     * @return New Slot
     * @throws ConnectionException
     */
    Slot getSlot(String queueName) throws ConnectionException;

    /**
     * Record Slot's last message ID related to a particular queue
     *
     * @param queueName      Name of the queue
     * @param startMessageId Start of message ID of the slot
     * @param endMessageId   End message ID of the slot
     * @param localSafeZone
     * @throws ConnectionException
     */
    void updateMessageId(String queueName, long startMessageId, long endMessageId, long localSafeZone)
            throws ConnectionException;

    /**
     * Record safe zone to delete slots by node. This ping comes from nodes as messages are not
     * published by them so that safe zone value keeps moving ahead.
     *
     * @param currentSlotDeleteSafeZone Safe zone value of the node
     */
    void updateSlotDeletionSafeZone(long currentSlotDeleteSafeZone) throws ConnectionException;

    /**
     * Delete slot records from SlotManagerClusterMode
     *
     * @param queueName Name of the queue
     * @param slot      Slot to be deleted
     * @return Whether the slot deletion is successful or not
     * @throws ConnectionException
     */
    boolean deleteSlot(String queueName, Slot slot) throws ConnectionException;

    /**
     * Re-assign slot to SlotManagerClusterMode when there are no subscribers
     *
     * @param queueName Name of the queue
     * @throws ConnectionException
     */
    void reAssignSlotWhenNoSubscribers(String queueName) throws ConnectionException;

    /**
     * Delete all slot associations with a given queue. This is required to handle a queue purge event.
     *
     * @param queueName Name of the queue
     */
    void clearAllActiveSlotRelationsToQueue(String queueName) throws ConnectionException;
}
