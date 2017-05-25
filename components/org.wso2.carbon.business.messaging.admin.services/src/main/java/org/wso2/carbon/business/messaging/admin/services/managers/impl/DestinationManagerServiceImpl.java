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
import org.wso2.carbon.business.messaging.admin.services.beans.DestinationManagementBeans;
import org.wso2.carbon.business.messaging.admin.services.exceptions.DestinationManagerException;
import org.wso2.carbon.business.messaging.admin.services.internal.MBRESTServiceDataHolder;
import org.wso2.carbon.business.messaging.admin.services.managers.DestinationManagerService;
import org.wso2.carbon.business.messaging.admin.services.types.Destination;
import org.wso2.carbon.business.messaging.admin.services.types.DestinationRolePermission;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Implementation for handling destination related resource through OSGi.
 */
public class DestinationManagerServiceImpl implements DestinationManagerService {
    /**
     * Registered andes core instance through OSGi.
     */
    private Andes andesCore;

    public DestinationManagerServiceImpl() {
        andesCore = MBRESTServiceDataHolder.getInstance().getAndesCore();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getDestinations(String protocol, String destinationType, String keyword, int offset, int limit)
            throws DestinationManagerException {
        try {
            ProtocolType protocolType = ProtocolType.valueOf(protocol.toUpperCase(Locale.ENGLISH));
            DestinationType destinationTypeEnum = DestinationType.valueOf(destinationType.toUpperCase(Locale.ENGLISH));
            return andesCore.getAllQueueNames(protocolType, destinationTypeEnum);
        } catch (IllegalArgumentException e) {
            throw new DestinationManagerException("Invalid protocol or destination type.", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteDestinations(String protocol, String destinationType) throws DestinationManagerException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Destination getDestination(String protocol, String destinationType, String destinationName)
            throws DestinationManagerException {
        //TODO: Add other information to the Destination instance, use protocol and destinationType
        Destination destination = null;
        try {
//            ProtocolType protocolType = ProtocolType.valueOf(protocol.toUpperCase());
//            DestinationType destinationTypeEnum = DestinationType.valueOf(destinationType.toUpperCase());
            if (isDestinationExist(protocol, destinationType, destinationName)) {
                destination = new Destination();
                destination.setDestinationName(destinationName);
                destination.setMessageCount(andesCore.getMessageCountOfQueue(destinationName));
            }
            return destination;
        } catch (AndesException e) {
            throw new DestinationManagerException("Error getting destination information.", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createDestination(String protocol, String destinationType, String destinationName)
            throws DestinationManagerException {
        //        try {
        //            ProtocolType protocolType = ProtocolType.valueOf(protocol.toUpperCase());
        //            DestinationType destinationTypeEnum = DestinationType.valueOf(destinationType.toUpperCase());
        //            boolean isDurable = Boolean.TRUE;
        //            boolean isShared = Boolean.FALSE;
        //            String queueOwner = "admin";
        //            boolean isExclusive = Boolean.FALSE;
        //            String exchange = (destinationTypeEnum.equals(DestinationType.QUEUE) ?
        //                    "amq.direct" :
        //                    "amq.topic");
        //
        //    andesCore.createQueue(new InboundQueueEvent(destinationName, isDurable, isShared, "admin", isExclusive));
        //            andesCore.addBinding(new InboundBindingEvent(
        //                    new QueueInfo(destinationName, isDurable, isShared, queueOwner, isExclusive), exchange,
        //                    destinationName));
        //        } catch (AndesException | IllegalArgumentException e) {
        //            throw new DestinationManagerException("Error creating the destination.", e);
        //        }
        String currentUsername = "admin";
        DestinationManagementBeans destinationManagementBeans = new DestinationManagementBeans();
        destinationManagementBeans.createDestination(protocol, destinationType, destinationName, currentUsername);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<DestinationRolePermission> getDestinationPermissions(String protocol, String destinationType,
            String destinationName) throws DestinationManagerException {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DestinationRolePermission createDestinationPermission(String protocol, String destinationType,
            String destinationName, DestinationRolePermission destinationRolePermission)
            throws DestinationManagerException {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DestinationRolePermission updateDestinationPermission(String protocol, String destinationType,
            String destinationName, DestinationRolePermission destinationRolePermission)
            throws DestinationManagerException {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteDestination(String protocol, String destinationType, String destinationName)
            throws DestinationManagerException {
//        try {
//            ProtocolType protocolType = ProtocolType.valueOf(protocol.toUpperCase());
//            DestinationType destinationTypeEnum = DestinationType.valueOf(destinationType.toUpperCase());
//            andesCore.deleteQueue(
//                    new InboundQueueEvent(destinationName, Boolean.TRUE, Boolean.FALSE, "admin", Boolean.FALSE));
//        } catch (AndesException e) {
//            throw new DestinationManagerException("Error deleting the destination.", e);
//        }
        DestinationManagementBeans destinationManagementBeans = new DestinationManagementBeans();
        destinationManagementBeans.deleteDestination(protocol, destinationType, destinationName);
    }

    @Override
    public List<String> getDestinationNames(String protocol, String destinationType, String destinationName)
            throws DestinationManagerException {
        return null;
    }

    @Override
    public boolean isDestinationExist(String protocol, String destinationType, String destinationName)
            throws DestinationManagerException {
        return andesCore.isQueueExists(destinationName);
    }

}
