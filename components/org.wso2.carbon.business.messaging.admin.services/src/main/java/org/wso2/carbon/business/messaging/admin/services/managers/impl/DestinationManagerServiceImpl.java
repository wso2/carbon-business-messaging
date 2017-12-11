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

package org.wso2.carbon.business.messaging.admin.services.managers.impl;

import org.wso2.andes.kernel.Andes;
import org.wso2.andes.kernel.AndesException;
import org.wso2.andes.kernel.DestinationType;
import org.wso2.andes.kernel.ProtocolType;
import org.wso2.carbon.business.messaging.admin.services.exceptions.InternalServerException;
import org.wso2.carbon.business.messaging.admin.services.internal.MbRestServiceDataHolder;
import org.wso2.carbon.business.messaging.admin.services.managers.DestinationManagerService;
import org.wso2.carbon.business.messaging.admin.services.types.Destination;

import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Locale;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

/**
 * Implementation for handling destination related resource through OSGi.
 */
public class DestinationManagerServiceImpl implements DestinationManagerService {
    /**
     * Registered andes core instance through OSGi.
     */
    private Andes andesCore;

    public DestinationManagerServiceImpl() {
        andesCore = MbRestServiceDataHolder.getInstance().getAndesCore();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getDestinations(String protocol, String destinationType, String keyword, int offset, int limit)
            throws InternalServerException {
        try {
            ProtocolType protocolType = ProtocolType.valueOf(protocol.toUpperCase(Locale.ENGLISH));
            DestinationType destinationTypeEnum = DestinationType.valueOf(destinationType.toUpperCase(Locale.ENGLISH));
            return andesCore.getAllQueueNames(protocolType, destinationTypeEnum);
        } catch (IllegalArgumentException e) {
            throw new InternalServerException("Invalid protocol or destination type.", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Destination getDestination(String protocol, String destinationType, String destinationName)
            throws InternalServerException {
        Destination destination = null;
        try {
            if (isDestinationExist(protocol, destinationType, destinationName)) {
                destination = new Destination();
                destination.setDestinationName(destinationName);
                destination.setMessageCount(andesCore.getMessageCountOfQueue(destinationName));
            }
            return destination;
        } catch (AndesException e) {
            throw new InternalServerException("Error getting destination information.", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createDestination(String protocol, String destinationType, String destinationName)
            throws InternalServerException {
        String currentUsername = "admin";
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
        } catch (MalformedObjectNameException | ReflectionException | MBeanException | InstanceNotFoundException e) {
            throw new InternalServerException("Error creating destination for to '" + protocol
                    + "' and destination type '" + destinationType + "' with name '" + destinationName + "'", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteDestination(String protocol, String destinationType, String destinationName)
            throws InternalServerException {
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            ObjectName objectName = new ObjectName(
                    "org.wso2.andes:type=VirtualHost.VirtualHostManager,VirtualHost=\"carbon\"");
            String operationName = "deleteQueue";

            Object[] parameters = new Object[] { destinationName };
            String[] signature = new String[] { String.class.getName() };

            mBeanServer.invoke(objectName, operationName, parameters, signature);

        } catch (MalformedObjectNameException | ReflectionException | MBeanException | InstanceNotFoundException e) {
            throw new InternalServerException("Error deleting destination for to '" + protocol
                    + "' and destination type '" + destinationType + "' with name '" + destinationName + "'", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDestinationExist(String protocol, String destinationType, String destinationName)
            throws InternalServerException {
        return andesCore.isQueueExists(destinationName);
    }

}
