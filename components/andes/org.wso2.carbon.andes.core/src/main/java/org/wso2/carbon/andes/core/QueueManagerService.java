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


import javax.jms.JMSException;
import javax.xml.stream.XMLStreamException;
import java.io.FileNotFoundException;
import java.util.Enumeration;
import java.util.List;

public interface QueueManagerService {

    public void createQueue(String queueName) throws QueueManagerException;

    public List<org.wso2.carbon.andes.core.types.Queue> getAllQueues() throws QueueManagerException;

    public void deleteQueue(String queueName) throws QueueManagerException;

    /**
     * Restore messages from the Dead Letter Queue to their original queues.
     *
     * @param messageIDs          Browser Message Id / External Message Id list
     * @param deadLetterQueueName Dead Letter Queue name for the respective tenant
     * @throws Exception
     */
    public void restoreMessagesFromDeadLetterQueue(String[] messageIDs, String deadLetterQueueName) throws
            QueueManagerException;

    /**
     * Restore messages from the Dead Letter Queue to another queue in the same tenant.
     *
     * @param messageIDs          Browser Message Id / External Message Id list
     * @param destination         The new destination queue for the messages in the same tenant
     * @param deadLetterQueueName Dead Letter Queue name for the respective tenant
     * @throws Exception
     */
    public void restoreMessagesFromDeadLetterQueueWithDifferentDestination(String[] messageIDs, String destination,
                                                                           String deadLetterQueueName) throws
            QueueManagerException;

    /**
     * Delete messages from the Dead Letter Queue and delete their content.
     *
     * @param messageIDs          Browser Message Id / External Message Id list to be deleted
     * @param deadLetterQueueName Dead Letter Queue name for the respective tenant
     * @throws Exception
     */
    public void deleteMessagesFromDeadLetterQueue(String[] messageIDs, String deadLetterQueueName) throws
            QueueManagerException;

    public void purgeMessagesOfQueue(String queueName) throws QueueManagerException;

    public long getMessageCountForQueue(String queueName, String msgPattern) throws QueueManagerException;

    public void updatePermission(String queueName, org.wso2.carbon.andes.core.types.QueueRolePermission[]
            queueRolePermissions)
            throws QueueManagerException;

    public String[] getBackendRoles() throws QueueManagerException;

    public org.wso2.carbon.andes.core.types.QueueRolePermission[] getQueueRolePermission(String queueName) throws
            QueueManagerException;

    public org.wso2.carbon.andes.core.types.Message[] browseQueue(String nameOfQueue, String userName,
                                                                  String accessKey, int startingIndex, int maxMsgCount)
            throws QueueManagerException;

    public long getTotalMessagesInQueue(String queueName) throws QueueManagerException;

    public boolean sendMessage(String nameOfQueue, String userName, String accessKey, String jmsType,
                               String jmsCorrelationID, int numberOfMessages,
                               String message, int deliveryMode, int priority,
                               long expireTime) throws QueueManagerException;

}
