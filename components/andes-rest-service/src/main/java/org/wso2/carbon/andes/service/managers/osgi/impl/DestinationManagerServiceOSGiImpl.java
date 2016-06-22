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

package org.wso2.carbon.andes.service.managers.osgi.impl;

import org.wso2.carbon.andes.core.AndesException;
import org.wso2.carbon.andes.core.AndesQueue;
import org.wso2.carbon.andes.core.DestinationType;
import org.wso2.carbon.andes.core.ProtocolType;
import org.wso2.carbon.andes.core.resource.manager.AndesResourceManager;
import org.wso2.carbon.andes.service.exceptions.DestinationManagerException;
import org.wso2.carbon.andes.service.internal.AndesRESTComponentDataHolder;
import org.wso2.carbon.andes.service.managers.DestinationManagerService;
import org.wso2.carbon.andes.service.types.Destination;
import org.wso2.carbon.andes.service.types.DestinationRolePermission;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Implementation for handling destination related resource through OSGi.
 */
public class DestinationManagerServiceOSGiImpl implements DestinationManagerService {
    private AndesResourceManager andesResourceManager;
    public DestinationManagerServiceOSGiImpl() {
        andesResourceManager = AndesRESTComponentDataHolder.getInstance().getAndesInstance().getAndesResourceManager();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Destination> getDestinations(String protocol, String destinationType, String keyword, int offset, int
            limit) throws DestinationManagerException {
        List<Destination> destinations = new ArrayList<>();
        try {
            ProtocolType protocolType = new ProtocolType(protocol);
            DestinationType destinationTypeEnum = DestinationType.valueOf(destinationType);
            List<AndesQueue> andesQueues = andesResourceManager.getDestinations(protocolType, destinationTypeEnum,
                    keyword, offset, limit);
            for (AndesQueue destination : andesQueues) {
                destinations.add(getDestinationFromAndesDestination(destination));
            }
        } catch (AndesException e) {
            throw new DestinationManagerException("Error occurred while getting destinations.", e);
        }
        return destinations;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteDestinations(String protocol, String destinationType) throws DestinationManagerException {
        try {
            ProtocolType protocolType = new ProtocolType(protocol);
            DestinationType destinationTypeEnum = DestinationType.valueOf(destinationType);
            andesResourceManager.deleteDestinations(protocolType, destinationTypeEnum);
        } catch (AndesException e) {
            throw new DestinationManagerException("Error occurred while deleting destinations.", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Destination getDestination(String protocol, String destinationType, String destinationName) throws
            DestinationManagerException {
        Destination destination;
        try {
            ProtocolType protocolType = new ProtocolType(protocol);
            DestinationType destinationTypeEnum = DestinationType.valueOf(destinationType);
            AndesQueue andesDestination = andesResourceManager.getDestination(protocolType, destinationTypeEnum,
                    destinationName);
            destination = getDestinationFromAndesDestination(andesDestination);
        } catch (AndesException e) {
            throw new DestinationManagerException("Error getting destination.", e);
        }
        return destination;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Destination createDestination(String protocol, String destinationType, String destinationName) throws
            DestinationManagerException {
        Destination destination;
        try {
            ProtocolType protocolType = new ProtocolType(protocol);
            DestinationType destinationTypeEnum = DestinationType.valueOf(destinationType);
            AndesQueue andesDestination = andesResourceManager.createDestination(protocolType, destinationTypeEnum,
                    destinationName, "admin");
            destination = getDestinationFromAndesDestination(andesDestination);
        } catch (AndesException e) {
            throw new DestinationManagerException("Error getting destination.", e);
        }
        return destination;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<DestinationRolePermission> getDestinationPermissions(String protocol, String destinationType,
                                                                    String destinationName)
                                                                    throws DestinationManagerException {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DestinationRolePermission createDestinationPermission(String protocol, String destinationType,
                                                                 String destinationName,
                                                                 DestinationRolePermission destinationRolePermission)
                                                                                    throws DestinationManagerException {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DestinationRolePermission updateDestinationPermission(String protocol, String destinationType, String
            destinationName, DestinationRolePermission destinationRolePermission) throws DestinationManagerException {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteDestination(String protocol, String destinationType, String destinationName)
                                                                                    throws DestinationManagerException {
        try {
            ProtocolType protocolType = new ProtocolType(protocol);
            DestinationType destinationTypeEnum = DestinationType.valueOf(destinationType);
            andesResourceManager.deleteDestination(protocolType, destinationTypeEnum, destinationName);
        } catch (AndesException e) {
            throw new DestinationManagerException("Error occurred while deleting destinations.", e);
        }
    }

    /**
     * Converts a {@link AndesQueue} to an {@link Destination}.
     *
     * @param andesQueue The andes queue object.
     * @return Converted destination.
     * @throws AndesException
     */
    private Destination getDestinationFromAndesDestination(AndesQueue andesQueue)
            throws AndesException {
        Destination destination = new Destination();
        //        destination.setId(andesQueue.getID());
        destination.setDestinationName(andesQueue.queueName);
        //        destination.setCreatedDate(andesQueue.getCreatedDate());
        destination.setDestinationType(andesQueue.getDestinationType());
        destination.setProtocol(andesQueue.getProtocolType());
        destination.setMessageCount(andesResourceManager.getMessageCountForStorageQueue(andesQueue.queueName));
        destination.setDurable(andesQueue.isDurable);
        destination.setOwner(andesQueue.queueOwner);
        destination.setSubscriptionCount(andesQueue.subscriptionCount);
        return destination;
    }
}
