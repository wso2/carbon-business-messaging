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

package org.wso2.carbon.andes.core;

import org.wso2.carbon.andes.core.types.Message;

import java.util.List;
import java.util.Set;

/**
 * This interface provides the base for managing all queue related services
 */
public interface QueueManagerService {

    /**
     * Creates a new queue
     *
     * @param queueName new queue name
     * @throws QueueManagerException
     */
    public void createQueue(String queueName) throws QueueManagerException;

    /**
     * Retrieve a queue with the number of the messages remaining by passing the name
     *
     * @param queueName the name of the queue to be retrieved
     * @return {@link org.wso2.carbon.andes.core.types.Queue} the queue
     */
    public org.wso2.carbon.andes.core.types.Queue getQueueByName(String queueName) throws QueueManagerException;


    /**
     * Retrieve names of all durable queues created
     * @return List of names
     * @throws QueueManagerException on an issue getting information
     */
    Set<String> getNamesOfAllDurableQueues() throws QueueManagerException;

    /**
     * Retrieve the dlc queue associated to a tenant
     *
     * @param tenantDomain The name of the tenant domain
     * @return {@link org.wso2.carbon.andes.core.types.Queue} the dlc queue
     */
    public org.wso2.carbon.andes.core.types.Queue getDLCQueue(String tenantDomain) throws QueueManagerException;

    /**
     * Gets all the queues
     *
     * @return a list of {@link org.wso2.carbon.andes.core.types.Queue} queues
     * @throws QueueManagerException
     */
    public List<org.wso2.carbon.andes.core.types.Queue> getAllQueues() throws QueueManagerException;

    /**
     * This method is triggered when deleting a queue through management console.
     *
     * @param queueName name of the queue
     * @throws QueueManagerException
     */
    public void deleteQueue(String queueName) throws QueueManagerException;

    /**
     * This method is triggered unsubscribe from a durable subscription to delete topic related
     * entries from registry
     * @param topicName Topic name
     * @param subscriptionId  Subscription ID
     * @throws QueueManagerException
     */
    public void deleteTopicFromRegistry(String topicName, String subscriptionId) throws QueueManagerException;

    /**
     * Restore messages from the Dead Letter Queue to their original queues.
     *
     * @param messageIDs          Browser Message Id / External Message Id list
     * @param destinationQueueName Dead Letter Queue name for the respective tenant
     * @return unavailable message count
     * @throws QueueManagerException
     */
    public long restoreMessagesFromDeadLetterQueue(long[] messageIDs, String destinationQueueName)
            throws QueueManagerException;

    /**
     * Restore messages from the Dead Letter Queue to another queue in the same tenant.
     *
     * @param messageIDs          Browser Message Id / External Message Id list
     * @param newDestinationQueueName         The new destination queue for the messages in the same tenant
     * @param destinationQueueName Dead Letter Queue name for the respective tenant
     * @return unavailable message count
     * @throws QueueManagerException
     */
    public long restoreMessagesFromDeadLetterQueueWithDifferentDestination(long[] messageIDs,
                                                                           String newDestinationQueueName,
                                                                           String destinationQueueName)
            throws QueueManagerException;

    /**
     * Delete messages from the Dead Letter Queue and delete their content.
     *
     * @param messageIDs          Browser Message Id / External Message Id list to be deleted
     * @param destinationQueueName Dead Letter Queue name for the respective tenant
     * @throws QueueManagerException
     */
    public void deleteMessagesFromDeadLetterQueue(long[] messageIDs, String destinationQueueName)
            throws QueueManagerException;

    /**
     * Request broker to clean all messages not awaiting acknowledgement from the given queue.
     *
     * @param queueName the queue name
     * @throws QueueManagerException
     */
    public void purgeMessagesOfQueue(String queueName) throws QueueManagerException;

    /**
     * Gets the message count for a queue
     *
     * @param destinationName the destination name. the name of the queue or topic
     * @param msgPattern      The exchange type used to transfer messages with the given destinationName. e.g. "queue" or "topic"
     * @return the number of messages for a queue
     * @throws QueueManagerException
     */
    public long getMessageCount(String destinationName, String msgPattern)
            throws QueueManagerException;

    /**
     * Updates permission for a queue. Can be pub, consume, change permission and etc.
     *
     * @param queueName            the queue name
     * @param queueRolePermissions the new permissions for the queue
     * @throws QueueManagerException
     */
    public void updatePermission(String queueName,
                                 org.wso2.carbon.andes.core.types.QueueRolePermission[]
                                         queueRolePermissions)
            throws QueueManagerException;

    /**
     * Create a queue and assign permissions which could be be pub, consume, change permission and etc.
     *
     * @param queueName            the queue name
     * @param queueRolePermissions the new permissions for the queue
     * @throws QueueManagerException
     */
    public void addQueueAndAssignPermission(String queueName,
            org.wso2.carbon.andes.core.types.QueueRolePermission[] queueRolePermissions) throws QueueManagerException;

    /**
     * Gets roles except for admin
     *
     * @return an array of roles
     * @throws QueueManagerException
     */
    public String[] getBackendRoles() throws QueueManagerException;

    /**
     * Gets role permissions assigned to a queue
     *
     * @param queueName the queue name
     * @return an array of queue role permissions
     * @throws QueueManagerException
     */
    public org.wso2.carbon.andes.core.types.QueueRolePermission[] getQueueRolePermission(
            String queueName) throws QueueManagerException;

    /**
     * Gets the messages of a queue
     *
     * @param nameOfQueue   name of the queue
     * @param nextMessageIdToRead next start message id to get message list
     * @param maxMsgCount   the maximum messages to return
     * @return an array of messages
     * @throws QueueManagerException
     */
    public org.wso2.carbon.andes.core.types.Message[] browseQueue(String nameOfQueue,
                                                                  long nextMessageIdToRead,
                                                                  int maxMsgCount)
            throws QueueManagerException;

    /**
     * Gets total message count in a queue
     *
     * @param queueName the queue name
     * @return the number messages in a queue
     * @throws QueueManagerException
     */
    public long getTotalMessagesInQueue(String queueName) throws QueueManagerException;

    /**
     * Send a message to the queue
     *
     * @param nameOfQueue      name of the queue
     * @param userName         the user name for the amqp url
     * @param accessKey        the access key for the amqp url
     * @param jmsType          the JMS type of the message
     * @param jmsCorrelationID the correlation ID of the JMS message
     * @param numberOfMessages number of messages to send
     * @param message          the message content/body
     * @param deliveryMode     the delivery mode
     * @param priority         priority of the message
     * @param expireTime       message expire time
     * @return true if message sent successfully, false otherwise.
     * @throws QueueManagerException
     */
    public boolean sendMessage(String nameOfQueue, String userName, String accessKey,
                               String jmsType,
                               String jmsCorrelationID, int numberOfMessages,
                               String message, int deliveryMode, int priority,
                               long expireTime) throws QueueManagerException;

    /**
     * Gets the number of messages in DLC for a specific queue.
     *
     * @param queueName The name of the queue.
     * @return The number of messages.
     * @throws QueueManagerException
     */
    long getNumberOfMessagesInDLCForQueue(String queueName) throws QueueManagerException;

    /**
     * Gets the messages in the DLC for a specific queue.
     *
     * @param queueName           name of the queue
     * @param nextMessageIdToRead next start message id to get message list
     * @param maxMessageCount     the maximum messages to return
     * @return an array of messages
     * @throws QueueManagerException
     */
    Message[] getMessageInDLCForQueue(String queueName,
                                      long nextMessageIdToRead, int maxMessageCount)
            throws QueueManagerException;

    /**
     * Dump message status to a default file. This is used for troubleshooting
     */
    void dumpMessageStatus() throws AndesException;
}
