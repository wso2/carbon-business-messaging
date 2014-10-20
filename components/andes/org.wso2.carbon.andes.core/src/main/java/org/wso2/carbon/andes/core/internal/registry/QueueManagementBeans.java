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
package org.wso2.carbon.andes.core.internal.registry;

import org.wso2.carbon.andes.core.QueueManagerException;
import org.wso2.carbon.andes.core.internal.util.QueueManagementConstants;
import org.wso2.carbon.andes.core.types.Queue;

import javax.management.*;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;

public class QueueManagementBeans {

    public static QueueManagementBeans self;

    public static final String DIRECT_EXCHANGE = "amq.direct";

    public static QueueManagementBeans getInstance() {
        if (self == null) {
            self = new QueueManagementBeans();
        }
        return self;
    }


    public void createQueue(String queueName, String userName) throws QueueManagerException {
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {

            ObjectName objectName =
                    new ObjectName("org.wso2.andes:type=VirtualHost.VirtualHostManager,VirtualHost=\"carbon\"");
            String operationName = "createNewQueue";

            Object[] parameters = new Object[]{queueName, userName, true};
            String[] signature = new String[]{String.class.getName(), String.class.getName(),
                    boolean.class.getName()};

            mBeanServer.invoke(
                    objectName,
                    operationName,
                    parameters,
                    signature);

            ObjectName bindingMBeanObjectName =
                    new ObjectName("org.wso2.andes:type=VirtualHost.Exchange,VirtualHost=\"carbon\",name=\"" +
                            DIRECT_EXCHANGE + "\",ExchangeType=direct");
            String bindingOperationName = "createNewBinding";

            Object[] bindingParams = new Object[]{queueName, queueName};
            String[] bpSignatures = new String[]{String.class.getName(), String.class.getName()};

            mBeanServer.invoke(
                    bindingMBeanObjectName,
                    bindingOperationName,
                    bindingParams,
                    bpSignatures);

        } catch (Exception e) {
            throw new QueueManagerException("Cannot create Queue : " + queueName, e);
        }
    }

    public ArrayList<Queue> getAllQueues() throws QueueManagerException {
        ArrayList<Queue> queueDetailsList = new ArrayList<Queue>();
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            ObjectName objectName =
                    new ObjectName("org.wso2.andes:type=QueueManagementInformation,name=QueueManagementInformation");
            Object result = mBeanServer.getAttribute(objectName, QueueManagementConstants.QUEUES_MBEAN_ATTRIBUTE);

            if (result != null) {
                String[] queueNamesList = (String[]) result;

                for (String queueName : queueNamesList) {
                    Queue queue = new Queue();
                    queue.setQueueName(queueName);
                    queue.setMessageCount(getMessageCount(queueName, "queue"));
                    queueDetailsList.add(queue);
                }

            }
            return queueDetailsList;

        } catch (MalformedObjectNameException e) {
            throw new QueueManagerException("Cannot access mBean operations to get queue list.", e);
        } catch (ReflectionException e) {
            throw new QueueManagerException("Cannot access mBean operations to get queue list.", e);
        } catch (MBeanException e) {
            throw new QueueManagerException("Cannot access mBean operations to get queue list.", e);
        } catch (InstanceNotFoundException e) {
            throw new QueueManagerException("Cannot access mBean operations to get queue list.", e);
        } catch (AttributeNotFoundException e) {
            throw new QueueManagerException("Cannot access mBean operations to get queue list.", e);
        }
    }

    public long getMessageCount(String queueName, String msgPattern) throws QueueManagerException {
        long messageCount = 0;
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            ObjectName objectName =
                    new ObjectName("org.wso2.andes:type=QueueManagementInformation,name=QueueManagementInformation");

            String operationName = "getMessageCount";
            Object[] parameters = new Object[]{queueName, msgPattern};
            String[] signature = new String[]{String.class.getName(), String.class.getName()};
            Object result = mBeanServer.invoke(
                    objectName,
                    operationName,
                    parameters,
                    signature);
            if (result != null) {
                messageCount = (Long) result;
            }

            return messageCount;

        } catch (MalformedObjectNameException e) {
            throw new QueueManagerException("Cannot access mBean operations for message count:" + queueName, e);
        } catch (ReflectionException e) {
            throw new QueueManagerException("Cannot access mBean operations for message count:" + queueName, e);
        } catch (MBeanException e) {
            throw new QueueManagerException("Cannot access mBean operations for message count:" + queueName, e);
        } catch (InstanceNotFoundException e) {
            throw new QueueManagerException("Cannot access mBean operations for message count:" + queueName, e);
        }
    }

    public void deleteQueue(String queueName) throws QueueManagerException {
        try {
            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

            ObjectName bindingMBeanObjectName =
                    new ObjectName("org.wso2.andes:type=VirtualHost.Exchange,VirtualHost=\"carbon\",name=\"" +
                            DIRECT_EXCHANGE + "\",ExchangeType=direct");
            String bindingOperationName = "removeBinding";

            Object[] bindingParams = new Object[]{queueName, queueName};
            String[] bpSignatures = new String[]{String.class.getName(), String.class.getName()};

            mBeanServer.invoke(
                    bindingMBeanObjectName,
                    bindingOperationName,
                    bindingParams,
                    bpSignatures);
            ObjectName objectName =
                    new ObjectName("org.wso2.andes:type=VirtualHost.VirtualHostManager,VirtualHost=\"carbon\"");
            String operationName = "deleteQueue";

            Object[] parameters = new Object[]{queueName};
            String[] signature = new String[]{String.class.getName()};

            mBeanServer.invoke(
                    objectName,
                    operationName,
                    parameters,
                    signature);

        } catch (MalformedObjectNameException e) {
            throw new QueueManagerException("Cannot delete Queue : " + queueName, e);
        } catch (InstanceNotFoundException e) {
            throw new QueueManagerException("Cannot delete Queue : " + queueName, e);
        } catch (MBeanException e) {
            throw new QueueManagerException("Cannot delete Queue : " + queueName, e);
        } catch (JMException e) {
            throw new QueueManagerException("Cannot delete Queue : " + queueName, e);
        }
    }

    /**
     * Invoke service bean for permanently deleting messages from the Dead Letter Channel.
     *
     * @param messageIDs          Browser message Id / External message Id list to be deleted
     * @param deadLetterQueueName Dead Letter Queue name for the respective tenant
     * @throws Exception
     */
    public void deleteMessagesFromDeadLetterQueue(String[] messageIDs, String deadLetterQueueName) throws
            QueueManagerException {
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

        try {
            ObjectName objectName =
                    new ObjectName("org.wso2.andes:type=QueueManagementInformation,name=QueueManagementInformation");

            String operationName = "deleteMessagesFromDeadLetterQueue";
            Object[] parameters = new Object[]{messageIDs, deadLetterQueueName};
            String[] signature = new String[]{String[].class.getName(), String.class.getName()};
            mBeanServer.invoke(
                    objectName,
                    operationName,
                    parameters,
                    signature);
        } catch (MalformedObjectNameException e) {
            throw new QueueManagerException("Error deleting messages from Dead Letter Queue : " +
                    deadLetterQueueName, e);
        } catch (ReflectionException e) {
            throw new QueueManagerException("Error deleting messages from Dead Letter Queue : " +
                    deadLetterQueueName, e);
        } catch (MBeanException e) {
            throw new QueueManagerException("Error deleting messages from Dead Letter Queue : " +
                    deadLetterQueueName, e);
        } catch (InstanceNotFoundException e) {
            throw new QueueManagerException("Error deleting messages from Dead Letter Queue : " +
                    deadLetterQueueName, e);
        }
    }

    /**
     * Invoke service bean for restoring messages from Dead Letter Channel to their original destinations.
     *
     * @param messageIDs          Browser message Id / External message Id list to be deleted
     * @param deadLetterQueueName Dead Letter Queue name for the respective tenant
     * @throws Exception
     */
    public void restoreMessagesFromDeadLetterQueue(String[] messageIDs, String deadLetterQueueName) throws
            QueueManagerException {
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            ObjectName objectName =
                    new ObjectName("org.wso2.andes:type=QueueManagementInformation,name=QueueManagementInformation");

            String operationName = "restoreMessagesFromDeadLetterQueue";
            Object[] parameters = new Object[]{messageIDs, deadLetterQueueName};
            String[] signature = new String[]{String[].class.getName(), String.class.getName()};
            mBeanServer.invoke(
                    objectName,
                    operationName,
                    parameters,
                    signature);
        } catch (MalformedObjectNameException e) {
            throw new QueueManagerException("Error restoring messages from Dead Letter Queue : " +
                    deadLetterQueueName, e);
        } catch (ReflectionException e) {
            throw new QueueManagerException("Error restoring messages from Dead Letter Queue : " +
                    deadLetterQueueName, e);
        } catch (MBeanException e) {
            throw new QueueManagerException("Error restoring messages from Dead Letter Queue : " +
                    deadLetterQueueName, e);
        } catch (InstanceNotFoundException e) {
            throw new QueueManagerException("Error restoring messages from Dead Letter Queue : " +
                    deadLetterQueueName, e);
        }
    }

    /**
     * Invoke service bean for restoring messages from Dead Letter Channel to a given destination.
     *
     * @param messageIDs          Browser message Id / External message Id list to be deleted
     * @param destination         The new destination for the messages in the same tenant
     * @param deadLetterQueueName Dead Letter Queue name for the respective tenant
     * @throws Exception
     */
    public void restoreMessagesFromDeadLetterQueueWithDifferentDestination(String[] messageIDs, String destination,
                                                                           String deadLetterQueueName) throws
            QueueManagerException {
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            ObjectName objectName =
                    new ObjectName("org.wso2.andes:type=QueueManagementInformation,name=QueueManagementInformation");

            String operationName = "restoreMessagesFromDeadLetterQueue";
            Object[] parameters = new Object[]{messageIDs, destination, deadLetterQueueName};
            String[] signature = new String[]{String[].class.getName(), String.class.getName(), String.class.getName()};
            mBeanServer.invoke(
                    objectName,
                    operationName,
                    parameters,
                    signature);
        } catch (MalformedObjectNameException e) {
            throw new QueueManagerException("Error restoring messages from Dead Letter Queue : " +
                    deadLetterQueueName + " to " + destination, e);
        } catch (ReflectionException e) {
            throw new QueueManagerException("Error restoring messages from Dead Letter Queue : " +
                    deadLetterQueueName + " to " + destination, e);
        } catch (MBeanException e) {
            throw new QueueManagerException("Error restoring messages from Dead Letter Queue : " +
                    deadLetterQueueName + " to " + destination, e);
        } catch (InstanceNotFoundException e) {
            throw new QueueManagerException("Error restoring messages from Dead Letter Queue : " +
                    deadLetterQueueName + " to " + destination, e);
        }
    }

    public void purgeMessagesFromQueue(String queueName) throws QueueManagerException {
        try {
            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

            ObjectName bindingMBeanObjectName =
                    new ObjectName("org.wso2.andes:type=QueueManagementInformation,name=QueueManagementInformation");
            String bindingOperationName = "deleteAllMessagesInQueue";

            Object[] bindingParams = new Object[]{queueName};
            String[] bpSignatures = new String[]{String.class.getName()};

            mBeanServer.invoke(
                    bindingMBeanObjectName,
                    bindingOperationName,
                    bindingParams,
                    bpSignatures);

        } catch (MalformedObjectNameException e) {
            throw new QueueManagerException("Cannot purge Queue : " + queueName, e);
        } catch (InstanceNotFoundException e) {
            throw new QueueManagerException("Cannot purge Queue : " + queueName, e);
        } catch (MBeanException e) {
            throw new QueueManagerException("Cannot purge Queue : " + queueName, e);
        } catch (JMException e) {
            throw new QueueManagerException("Cannot purge Queue : " + queueName, e);
        }
    }

    public static boolean queueExists(String queueName) throws QueueManagerException {
        try {
            boolean status = false;
            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
            ObjectName objectName =
                    new ObjectName("org.wso2.andes:type=QueueManagementInformation,name=QueueManagementInformation");
            String operationName = "isQueueExists";
            Object[] parameters = new Object[]{queueName};
            String[] signature = new String[]{String.class.getName()};
            Object result = mBeanServer.invoke(
                    objectName,
                    operationName,
                    parameters,
                    signature);
            if (result != null) {
                status = (Boolean) result;
            }

            return status;
        } catch (MalformedObjectNameException e) {
            throw new QueueManagerException("Error checking if queue " + queueName + " exists.", e);
        } catch (JMException e) {
            throw new QueueManagerException("Error checking if queue " + queueName + " exists.", e);
        }
    }


}
