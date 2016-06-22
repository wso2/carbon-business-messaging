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

import org.wso2.carbon.andes.core.AndesException;
import org.wso2.carbon.andes.core.DestinationType;
import org.wso2.carbon.andes.core.util.CompositeDataHelper;
import org.wso2.carbon.andes.service.exceptions.DestinationManagerException;
import org.wso2.carbon.andes.service.managers.bean.utils.DestinationManagementConstants;
import org.wso2.carbon.andes.service.types.Destination;

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
 * The following class contains the MBeans invoking services related to queue resources.
 */
public class DestinationManagementBeans {

    /**
     * Gets the collection of destinations(queues/topics)
     *
     * @param protocol        The protocol type matching for the destination type. Example : AMQP, amqp, MQTT, mqtt.
     * @param destinationType The destination type matching for the destination. Example : queue, topic, durable_topic.
     * @param keyword         Search keyword for destination name. "*" will return all destinations. Destinations that
     *                        <strong>contains</strong> the keyword will be returned.
     * @param offset          The offset value for the collection of destination.
     * @param limit           The number of records to return from the collection of destinations.
     * @return A list of {@link Destination}s.
     * @throws DestinationManagerException
     */
    public List<Destination> getDestinations(String protocol, String destinationType, String keyword,
                                             int offset, int limit) throws
            DestinationManagerException {
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        List<Destination> destinationList = new ArrayList<>();
        try {
            ObjectName objectName =
                    new ObjectName("org.wso2.andes:type=DestinationManagementInformation," +
                                   "name=DestinationManagementInformation");

            String operationName = DestinationManagementConstants.GET_DESTINATIONS_MBEAN_ATTRIBUTE;
            Object[] parameters = new Object[]{protocol, destinationType, keyword, offset, limit};
            String[] signature = new String[]{String.class.getName(), String.class.getName(), String.class.getName(),
                                              int.class.getName(), int.class.getName()};
            Object result = mBeanServer.invoke(
                    objectName,
                    operationName,
                    parameters,
                    signature);

            CompositeData[] messageDataList = (CompositeData[]) result;
            for (CompositeData messageData : messageDataList) {
                Destination destination = getDestinationInfo(messageData);
                destinationList.add(destination);
            }
            return destinationList;
        } catch (MalformedObjectNameException | ReflectionException | MBeanException | InstanceNotFoundException e) {
            throw new DestinationManagerException("Error getting destinations belonging to '" + protocol +
                                                  "' and destination type '" + destinationType + "'", e);
        }
    }

    /**
     * Deletes all the destinations.
     *
     * @param protocol        The protocol type matching for the destination type. Example : amqp, mqtt.
     * @param destinationType The destination type matching for the destination. Example : queue, topic, durable_topic.
     * @throws DestinationManagerException
     */
    public void deleteDestinations(String protocol, String destinationType) throws
            DestinationManagerException {
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            ObjectName objectName =
                    new ObjectName("org.wso2.andes:type=DestinationManagementInformation," +
                                   "name=DestinationManagementInformation");

            String operationName = DestinationManagementConstants.DELETE_DESTINATIONS_MBEAN_OPERATION;
            Object[] parameters = new Object[]{protocol, destinationType};
            String[] signature = new String[]{String.class.getName(), String.class.getName()};
            mBeanServer.invoke(
                    objectName,
                    operationName,
                    parameters,
                    signature);

        } catch (MalformedObjectNameException | ReflectionException | MBeanException | InstanceNotFoundException e) {
            throw new DestinationManagerException("Error deleting destinations belonging to '" + protocol +
                                                  "' and destination type '" + destinationType + "'", e);
        }
    }

    /**
     * Gets a destination.
     *
     * @param protocol        The protocol type matching for the destination type. Example : amqp, mqtt.
     * @param destinationType The destination type matching for the destination. Example : queue, topic, durable_topic.
     * @param destinationName The name of the destination.
     * @return A {@link Destination}.
     * @throws DestinationManagerException
     */
    public Destination getDestination(String protocol, String destinationType, String destinationName)
                                                                                    throws DestinationManagerException {
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            ObjectName objectName =
                    new ObjectName("org.wso2.andes:type=DestinationManagementInformation," +
                                   "name=DestinationManagementInformation");

            String operationName = DestinationManagementConstants.GET_DESTINATION_MBEAN_ATTRIBUTE;
            Object[] parameters = new Object[]{protocol, destinationType, destinationName};
            String[] signature = new String[]{String.class.getName(), String.class.getName(), String.class.getName()};
            Object result = mBeanServer.invoke(
                    objectName,
                    operationName,
                    parameters,
                    signature);

            if (null != result) {
                CompositeData destinationCompositeData = (CompositeData) result;
                return getDestinationInfo(destinationCompositeData);
            } else {
                return null;
            }
        } catch (MalformedObjectNameException | ReflectionException | MBeanException | InstanceNotFoundException e) {
            throw new DestinationManagerException("Error creating destination for to '" + protocol +
                                                  "' and destination type '" + destinationType + "' with name '"
                                                                                            + destinationName + "'", e);
        }
    }

    /**
     * Creates a new destination.
     *
     * @param protocol        The protocol type matching for the destination type. Example : amqp, mqtt.
     * @param destinationType The destination type matching for the destination. Example : queue, topic, durable_topic.
     * @param destinationName The name of the destination.
     * @param currentUsername The username of the user who creates the destination.
     * @return Newly created {@link Destination}.
     * @throws DestinationManagerException
     */
    public Destination createDestination(String protocol, String destinationType, String destinationName,
                                         String currentUsername) throws DestinationManagerException {
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            ObjectName objectName =
                    new ObjectName("org.wso2.andes:type=DestinationManagementInformation," +
                                   "name=DestinationManagementInformation");

            String operationName = DestinationManagementConstants.CREATE_DESTINATION_MBEAN_OPERATION;
            Object[] parameters = new Object[]{protocol, destinationType, destinationName, currentUsername};
            String[] signature = new String[]{String.class.getName(), String.class.getName(), String.class.getName(),
                                              String.class.getName()};
            Object result = mBeanServer.invoke(
                    objectName,
                    operationName,
                    parameters,
                    signature);

            if (null != result) {
                CompositeData destinationCompositeData = (CompositeData) result;
                return getDestinationInfo(destinationCompositeData);
            } else {
                return null;
            }
        } catch (MalformedObjectNameException | ReflectionException | MBeanException | InstanceNotFoundException e) {
            throw new DestinationManagerException("Error creating destination for to '" + protocol +
                      "' and destination type '" + destinationType + "' with name '" + destinationName + "'", e);
        }
    }

    /**
     * Deletes a destination.
     *
     * @param protocol        The protocol type matching for the destination type. Example : amqp, mqtt.
     * @param destinationType The destination type matching for the destination. Example : queue, topic, durable_topic.
     * @param destinationName The name of the destination to be deleted.
     * @throws DestinationManagerException
     */
    public void deleteDestination(String protocol, String destinationType, String destinationName) throws
            DestinationManagerException {
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            ObjectName objectName =
                    new ObjectName("org.wso2.andes:type=DestinationManagementInformation," +
                                   "name=DestinationManagementInformation");

            String operationName = DestinationManagementConstants.DELETE_DESTINATION_MBEAN_OPERATION;
            Object[] parameters = new Object[]{protocol, destinationType, destinationName};
            String[] signature = new String[]{String.class.getName(), String.class.getName(), String.class.getName()};
            mBeanServer.invoke(
                    objectName,
                    operationName,
                    parameters,
                    signature);

        } catch (MalformedObjectNameException | ReflectionException | MBeanException | InstanceNotFoundException e) {
            throw new DestinationManagerException("Error deleting destination for to '" + protocol +
                                                  "' and destination type '" + destinationType + "' with name '"
                                                  + destinationName + "'", e);
        }
    }

    /**
     * Converts a {@link CompositeData} to a {@link Destination}. The {@link CompositeData} should match the {@link
     * Destination} in attribute wise.
     *
     * @param compositeDestination The composite data object.
     * @return A {@link Destination}.
     * @throws DestinationManagerException
     */
    private Destination getDestinationInfo(CompositeData compositeDestination) throws DestinationManagerException {
        Destination destination;
        try {
            destination = new Destination();
            destination.setDestinationName((String) compositeDestination.get(CompositeDataHelper
                    .DestinationCompositeDataHelper.DESTINATION_NAME));
            destination.setOwner((String) compositeDestination.get(CompositeDataHelper.DestinationCompositeDataHelper
                    .DESTINATION_OWNER));
            destination.setDurable((Boolean) compositeDestination.get(CompositeDataHelper.DestinationCompositeDataHelper
                    .IS_DURABLE));
            destination.setSubscriptionCount((Integer) compositeDestination.get(CompositeDataHelper
                    .DestinationCompositeDataHelper.SUBSCRIPTION_COUNT));
            destination.setMessageCount((Long) compositeDestination.get(CompositeDataHelper
                    .DestinationCompositeDataHelper.MESSAGE_COUNT));
            destination.setProtocol((String) compositeDestination.get(CompositeDataHelper
                        .DestinationCompositeDataHelper.PROTOCOL_TYPE));
            destination.setDestinationType(DestinationType.valueOf((String) compositeDestination.get(CompositeDataHelper
                    .DestinationCompositeDataHelper.DESTINATION_TYPE)));
        } catch (AndesException e) {
            throw new DestinationManagerException("Error occurred while converting data.", e);
        }
        return destination;
    }
}
