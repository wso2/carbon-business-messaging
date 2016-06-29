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

package org.wso2.carbon.andes.service.managers.bean.impl;

import org.wso2.carbon.andes.service.beans.DestinationManagementBeans;
import org.wso2.carbon.andes.service.exceptions.DestinationManagerException;
import org.wso2.carbon.andes.service.managers.DestinationManagerService;
import org.wso2.carbon.andes.service.types.Destination;
import org.wso2.carbon.andes.service.types.DestinationRolePermission;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This implementation provides the base for managing all messages related services through JMX.
 */
public class DestinationManagerServiceBeanImpl implements DestinationManagerService {

    /**
     * TODO : Remove this and use user-core
     * Sample permission storage.
     */
    private static Set<DestinationRolePermission> destinationRolePermissions = new HashSet<>();
    private DestinationManagementBeans destinationManagementBeans;
    
    public DestinationManagerServiceBeanImpl() {
        destinationManagementBeans = new DestinationManagementBeans();
    }

    public DestinationManagerServiceBeanImpl(DestinationManagementBeans destinationManagementBeans) {
        this.destinationManagementBeans = destinationManagementBeans;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public List<Destination> getDestinations(String protocol, String destinationType, String keyword,
                                             int offset, int limit) throws DestinationManagerException {
        return destinationManagementBeans.getDestinations(protocol, destinationType, keyword, offset, limit);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteDestinations(String protocol, String destinationType) throws DestinationManagerException {
        destinationManagementBeans.deleteDestinations(protocol, destinationType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Destination getDestination(String protocol, String destinationType, String
            destinationName) throws DestinationManagerException {
        return destinationManagementBeans.getDestination(protocol, destinationType, destinationName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Destination createDestination(String protocol, String destinationType, String
            destinationName) throws DestinationManagerException {
        String currentUsername = "admin";
        return destinationManagementBeans.createDestination(protocol, destinationType, destinationName,
                                                                                                    currentUsername);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<DestinationRolePermission> getDestinationPermissions(String protocol, String destinationType,
                                                                    String destinationName) throws
            DestinationManagerException {
        // Null if invalid destination.
        destinationRolePermissions.add(new DestinationRolePermission("admin-role", true, true));
        return destinationRolePermissions;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DestinationRolePermission createDestinationPermission(
            String protocol, String destinationType,
            String destinationName,
            DestinationRolePermission destinationRolePermission) throws DestinationManagerException {
        destinationRolePermissions.add(destinationRolePermission);
        // Get permissions for the destination.
        // Update the permissions for the destination.
        // Return the newD list of permissions.
        return new DestinationRolePermission("admin-role", true, true);
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
        destinationManagementBeans.deleteDestination(protocol, destinationType, destinationName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getDestinationNames(String protocol, String destinationType, String destinationName)
                                                                                    throws DestinationManagerException {
        throw new UnsupportedOperationException();
    }
}
