/*
 * Copyright (c) 2016, WSO2 Inc. (http://wso2.com) All Rights Reserved.
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

package org.wso2.carbon.andes.service.beans;

import org.apache.commons.lang.ArrayUtils;
import org.wso2.carbon.andes.core.util.CompositeDataHelper;
import org.wso2.carbon.andes.service.exceptions.DestinationManagerException;
import org.wso2.carbon.andes.service.exceptions.MessageManagerException;
import org.wso2.carbon.andes.service.managers.bean.utils.MessageManagementConstants;
import org.wso2.carbon.andes.service.types.Message;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.openmbean.CompositeData;

/**
 * The following class contains the MBeans invoking services related to message resources.
 */
public class MessageManagementBeans {

    /**
     * Browse message of a destination using message ID.
     * <p>
     * To browse messages without message ID, use {@link MessageManagementBeans#getMessagesOfDestinationByOffset(String,
     * String, String, boolean, int, int)}.
     *
     * @param protocol        The protocol type matching for the message. Example : amqp, mqtt.
     * @param destinationType The destination type matching for the message. Example : queue, topic, durable_topic.
     * @param destinationName The name of the destination
     * @param content         Whether to return message content or not.
     * @param nextMessageID   The starting message ID to return from.
     * @param limit           The number of messages to return.
     * @return A list of {@link Message}s.
     * @throws MessageManagerException
     */
    public List<Message> getMessagesOfDestinationByMessageID(String protocol, String destinationType,
                                                             String destinationName, boolean content,
                                                             long nextMessageID, int limit)
                                                            throws MessageManagerException {
        List<Message> browseMessageList = new ArrayList<>();
        try {
            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
            ObjectName objectName =
                    new ObjectName(MessageManagementConstants.MESSAGE_OBJECT_NAME);
            String operationName = MessageManagementConstants.BROWSE_DESTINATIONS_WITH_MESSAGE_ID_MBEAN_ATTRIBUTE;
            Object[] parameters = new Object[]{protocol, destinationType, destinationName, content, nextMessageID,
                                                                                                                limit};
            String[] signature = new String[]{String.class.getName(), String.class.getName(), String.class.getName(),
                                              boolean.class.getName(), long.class.getName(), int.class.getName()};
            Object result = mBeanServer.invoke(
                    objectName,
                    operationName,
                    parameters,
                    signature);
            if (null != result) {
                CompositeData[] messageDataList = (CompositeData[]) result;
                for (CompositeData messageData : messageDataList) {
                    browseMessageList.add(getMessageInfo(messageData));
                }
            }
        } catch (InstanceNotFoundException | MBeanException | ReflectionException | MalformedObjectNameException e) {
            throw new MessageManagerException("Cannot browse queue : " + destinationName, e);
        }
        return browseMessageList;
    }

    /**
     * Browse message of a destination. Please note this is time costly.
     * <p>
     * To browse messages with message ID, use {@link MessageManagementBeans#getMessagesOfDestinationByMessageID
     * (String, String, String, boolean, long, int)}.
     *
     * @param protocol        The protocol type matching for the message. Example : amqp, mqtt.
     * @param destinationType The destination type matching for the message. Example : queue, topic, durable_topic.
     * @param destinationName The name of the destination
     * @param content         Whether to return message content or not.
     * @param offset          Starting index of the messages to return.
     * @param limit           The number of messages to return.
     * @return A list of {@link Message}s.
     * @throws MessageManagerException
     */
    public List<Message> getMessagesOfDestinationByOffset(String protocol, String destinationType,
                                                          String destinationName, boolean content, int offset,
                                                          int limit) throws MessageManagerException {
        List<Message> browseMessageList = new ArrayList<>();
        try {
            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
            ObjectName objectName =
                    new ObjectName(MessageManagementConstants.MESSAGE_OBJECT_NAME);
            String operationName = MessageManagementConstants.BROWSE_DESTINATIONS_WITH_OFFSET_MBEAN_ATTRIBUTE;
            Object[] parameters = new Object[]{protocol, destinationType, destinationName, content, offset, limit};
            String[] signature = new String[]{String.class.getName(), String.class.getName(), String.class.getName(),
                                              boolean.class.getName(), int.class.getName(), int.class.getName()};
            Object result = mBeanServer.invoke(
                    objectName,
                    operationName,
                    parameters,
                    signature);
            if (null != result) {
                CompositeData[] messageDataList = (CompositeData[]) result;
                for (CompositeData messageData : messageDataList) {
                    browseMessageList.add(getMessageInfo(messageData));
                }
            }
        } catch (InstanceNotFoundException | MBeanException | ReflectionException | MalformedObjectNameException e) {
            throw new MessageManagerException("Cannot browse queue : " + destinationName, e);
        }
        return browseMessageList;
    }

    /**
     * Gets a message by message ID belonging to a particular protocol, destination type and destination name.
     *
     * @param protocol        The protocol type matching for the message. Example : amqp, mqtt.
     * @param destinationType The destination type matching for the message. Example : queue, topic, durable_topic.
     * @param destinationName The name of the destination to which the message belongs to.
     * @param andesMessageID  The message ID. This message is the andes metadata message ID.
     * @param content         Whether to return content or not.
     * @return A {@link Message}.
     * @throws MessageManagerException
     */
    public Message getMessage(String protocol, String destinationType, String destinationName, long andesMessageID,
                              boolean content) throws MessageManagerException {
        Message message = new Message();
        try {
            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
            ObjectName objectName =
                    new ObjectName(MessageManagementConstants.MESSAGE_OBJECT_NAME);
            String operationName = MessageManagementConstants.GET_MESSAGE_MBEAN_ATTRIBUTE;
            Object[] parameters = new Object[]{protocol, destinationType, destinationName, andesMessageID, content};
            String[] signature = new String[]{String.class.getName(), String.class.getName(), String.class.getName(),
                                              long.class.getName(), boolean.class.getName()};
            Object result = mBeanServer.invoke(
                    objectName,
                    operationName,
                    parameters,
                    signature);
            if (result != null) {
                CompositeData messageData = (CompositeData) result;
                message = getMessageInfo(messageData);
            }
        } catch (InstanceNotFoundException | MBeanException | ReflectionException | MalformedObjectNameException e) {
            throw new MessageManagerException("Cannot browse queue : " + destinationName, e);
        }
        return message;
    }

    /**
     * Purge all messages belonging to a destination.
     *
     * @param protocol        The protocol type matching for the message. Example : amqp, mqtt.
     * @param destinationType The destination type matching for the message. Example : queue, topic, durable_topic.
     * @param destinationName The name of the destination to purge messages.
     * @throws MessageManagerException
     */
    public void deleteMessages(String protocol, String destinationType, String destinationName)
                                                                                        throws MessageManagerException {
        try {
            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
            ObjectName objectName =
                    new ObjectName(MessageManagementConstants.MESSAGE_OBJECT_NAME);
            String operationName = MessageManagementConstants.DELETE_MESSAGE_MBEAN_OPERATION;
            Object[] parameters = new Object[]{protocol, destinationType, destinationName};
            String[] signature = new String[]{String.class.getName(), String.class.getName(), String.class.getName()};
            mBeanServer.invoke(
                    objectName,
                    operationName,
                    parameters,
                    signature);
        } catch (InstanceNotFoundException | MBeanException | ReflectionException | MalformedObjectNameException e) {
            throw new MessageManagerException("Cannot browse queue : " + destinationName, e);
        }
    }

    /**
     * Invoke service bean for permanently deleting messages from the Dead Letter Channel.
     *
     * @param messageIDs           Browser message Id / External message Id list to be deleted
     * @param destinationQueueName Dead Letter Queue name for the respective tenant
     * @throws DestinationManagerException
     */
    public void deleteMessagesFromDeadLetterQueue(long[] messageIDs, String destinationQueueName) throws
            DestinationManagerException {
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();

        try {
            ObjectName objectName =
                    new ObjectName("org.wso2.andes:type=QueueManagementInformation,name=QueueManagementInformation");

            String operationName = "deleteMessagesFromDeadLetterQueue";
            Object[] parameters = new Object[]{messageIDs, destinationQueueName};
            String[] signature = new String[]{long[].class.getName(), String.class.getName()};
            mBeanServer.invoke(
                    objectName,
                    operationName,
                    parameters,
                    signature);
        } catch (MalformedObjectNameException | ReflectionException | MBeanException | InstanceNotFoundException e) {
            throw new DestinationManagerException("Error deleting messages from Dead Letter Queue : " +
                                                  destinationQueueName, e);
        }
    }

    /**
     * Invoke service bean for restoring messages from Dead Letter Channel to their original destinations.
     *
     * @param messageIDs           Browser message Id / External message Id list to be deleted
     * @param destinationQueueName Dead Letter Queue name for the respective tenant
     * @throws DestinationManagerException
     */
    public void restoreMessagesFromDeadLetterQueue(long[] messageIDs, String destinationQueueName) throws
            DestinationManagerException {
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            ObjectName objectName =
                    new ObjectName("org.wso2.andes:type=QueueManagementInformation,name=QueueManagementInformation");

            String operationName = "restoreMessagesFromDeadLetterQueue";
            Object[] parameters = new Object[]{messageIDs, destinationQueueName};
            String[] signature = new String[]{long[].class.getName(), String.class.getName()};
            mBeanServer.invoke(
                    objectName,
                    operationName,
                    parameters,
                    signature);
        } catch (MalformedObjectNameException | ReflectionException | MBeanException | InstanceNotFoundException e) {
            throw new DestinationManagerException("Error restoring messages from Dead Letter Queue : " +
                                                  destinationQueueName, e);
        }
    }

    /**
     * Invoke service bean for restoring messages from Dead Letter Channel to a given destination.
     *
     * @param messageIDs              Browser message Id / External message Id list to be deleted
     * @param newDestinationQueueName The new destination for the messages in the same tenant
     * @param destinationQueueName    Dead Letter Queue name for the respective tenant
     * @throws DestinationManagerException
     */
    public void restoreMessagesFromDeadLetterQueueWithDifferentDestination(long[] messageIDs,
                                                                           String newDestinationQueueName, String
                                                                                   destinationQueueName) throws
            DestinationManagerException {
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            ObjectName objectName =
                    new ObjectName("org.wso2.andes:type=QueueManagementInformation,name=QueueManagementInformation");

            String operationName = "restoreMessagesFromDeadLetterQueue";
            Object[] parameters = new Object[]{messageIDs, newDestinationQueueName, destinationQueueName};
            String[] signature = new String[]{long[].class.getName(), String.class.getName(), String.class.getName()};
            mBeanServer.invoke(
                    objectName,
                    operationName,
                    parameters,
                    signature);
        } catch (MalformedObjectNameException | ReflectionException | MBeanException | InstanceNotFoundException e) {
            throw new DestinationManagerException("Error restoring messages from Dead Letter Queue : " +
                                                  destinationQueueName + " to " + newDestinationQueueName, e);
        }
    }

    /**
     * Converts a {@link CompositeData} to a {@link Message}. The {@link CompositeData} should match the {@link
     * Message} in attribute wise.
     *
     * @param compositeMessage The composite data object.
     * @return A {@link Message}.
     */
    private Message getMessageInfo(CompositeData compositeMessage) {
        Message message = new Message();
        message.setAndesMsgMetadataId((Long) compositeMessage.get(CompositeDataHelper.MessagesCompositeDataHelper
                .ANDES_METADATA_MESSAGE_ID));
        message.setDestination((String) compositeMessage.get(CompositeDataHelper.MessagesCompositeDataHelper
                .DESTINATION_NAME));
        message.setMessageProperties(ArrayUtils.toMap((String[][]) compositeMessage.get(CompositeDataHelper
                .MessagesCompositeDataHelper.MESSAGE_PROPERTIES)));
        message.setMessageContent((String) compositeMessage.get(CompositeDataHelper.MessagesCompositeDataHelper
                .MESSAGE_CONTENT));
        return message;
    }
}
