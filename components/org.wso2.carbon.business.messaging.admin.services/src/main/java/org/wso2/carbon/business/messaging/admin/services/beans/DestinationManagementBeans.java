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

package org.wso2.carbon.business.messaging.admin.services.beans;

import org.wso2.carbon.business.messaging.admin.services.exceptions.DestinationManagerException;
import org.wso2.carbon.business.messaging.admin.services.types.Destination;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

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
     * @return A list of destination names.
     * @throws DestinationManagerException
     */
    public List<String> getDestinations(String protocol, String destinationType, String keyword, int offset, int limit)
            throws DestinationManagerException {
        try {
            List<String> destinationNames = new ArrayList<>();
            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
            ObjectName objectName = new ObjectName(
                    "org.wso2.andes:type=QueueManagementInformation,name=QueueManagementInformation");
            String operationName = "getAllQueueNames";
            Object[] parameters = new Object[] {};
            String[] signature = new String[] { String.class.getName() };
            Object result = mBeanServer.invoke(objectName, operationName, parameters, signature);
            if (result != null) {
                destinationNames = Arrays.asList((String[]) result);
            }
            return destinationNames;
        } catch (MalformedObjectNameException | ReflectionException | MBeanException | InstanceNotFoundException e) {
            throw new DestinationManagerException("Error finding destination names for to '" + protocol +
                    "' ,destination type '" + destinationType + "' ,keyword '" + keyword + "'", e);
        }
    }

    /**
     * Deletes all the destinations.
     *
     * @param protocol        The protocol type matching for the destination type. Example : amqp, mqtt.
     * @param destinationType The destination type matching for the destination. Example : queue, topic, durable_topic.
     * @throws DestinationManagerException
     */
    public void deleteDestinations(String protocol, String destinationType) throws DestinationManagerException {
        throw new UnsupportedOperationException();
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
        throw new UnsupportedOperationException();
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
            ObjectName objectName = new ObjectName(
                    "org.wso2.andes:type=VirtualHost.VirtualHostManager,VirtualHost=\"carbon\"");
            String operationName = "createNewQueue";

            Object[] parameters = new Object[] { destinationName, currentUsername, true };
            String[] signature = new String[] {
                    String.class.getName(), String.class.getName(), boolean.class.getName()
            };

            mBeanServer.invoke(objectName, operationName, parameters, signature);

            ObjectName bindingMBeanObjectName = new ObjectName(
                    "org.wso2.andes:type=VirtualHost.Exchange,VirtualHost=\"carbon\",name=\"" +
                            "amq.direct" + "\",ExchangeType=direct");
            String bindingOperationName = "createNewBinding";

            Object[] bindingParams = new Object[] { destinationName, destinationName };
            String[] bpSignatures = new String[] { String.class.getName(), String.class.getName() };

            mBeanServer.invoke(bindingMBeanObjectName, bindingOperationName, bindingParams, bpSignatures);
            return null;
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
    public void deleteDestination(String protocol, String destinationType, String destinationName)
            throws DestinationManagerException {
        // Todo: make use of protocol and destinationType params
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            ObjectName objectName = new ObjectName(
                    "org.wso2.andes:type=VirtualHost.VirtualHostManager,VirtualHost=\"carbon\"");
            String operationName = "deleteQueue";

            Object[] parameters = new Object[] { destinationName };
            String[] signature = new String[] { String.class.getName() };

            mBeanServer.invoke(objectName, operationName, parameters, signature);

        } catch (MalformedObjectNameException | ReflectionException | MBeanException | InstanceNotFoundException e) {
            throw new DestinationManagerException("Error deleting destination for to '" + protocol +
                    "' and destination type '" + destinationType + "' with name '" + destinationName + "'", e);
        }
    }

    /**
     * Check if the destination exists
     *
     * @param protocol        The protocol type matching for the destination type. Example : amqp, mqtt.
     * @param destinationType The destination type matching for the destination. Example : queue, topic, durable_topic.
     * @param destinationName The name of the destination to be checked.
     * @return true if the destination exists false otherwise
     */
    public boolean isDestinationExist(String protocol, String destinationType, String destinationName)
            throws DestinationManagerException {
        try {
            boolean status = false;
            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
            ObjectName objectName = new ObjectName(
                    "org.wso2.andes:type=QueueManagementInformation,name=QueueManagementInformation");
            String operationName = "isQueueExists";
            Object[] parameters = new Object[] { destinationName };
            String[] signature = new String[] { String.class.getName() };
            Object result = mBeanServer.invoke(objectName, operationName, parameters, signature);
            if (result != null) {
                status = (Boolean) result;
            }

            return status;
        } catch (MalformedObjectNameException | ReflectionException | MBeanException | InstanceNotFoundException e) {
            throw new DestinationManagerException("Error deleting destination for to '" + protocol +
                    "' and destination type '" + destinationType + "' with name '" + destinationName + "'", e);
        }
    }
}
