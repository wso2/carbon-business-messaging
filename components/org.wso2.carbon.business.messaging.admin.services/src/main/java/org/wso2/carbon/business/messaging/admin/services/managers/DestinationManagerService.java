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

package org.wso2.carbon.business.messaging.admin.services.managers;

import org.wso2.carbon.business.messaging.admin.services.exceptions.DestinationManagerException;
import org.wso2.carbon.business.messaging.admin.services.types.Destination;
import org.wso2.carbon.business.messaging.admin.services.types.DestinationRolePermission;

import java.util.List;
import java.util.Set;

/**
 * This interface provides the base for managing all messages related services.
 */
public interface DestinationManagerService {
    /**
     * Gets the collection of destinations(queues/topics)
     *
     * @param protocol        The protocol type matching for the destination type.
     * @param destinationType The destination type matching for the destination.
     * @param keyword         Search keyword for destination name. "*" will return all destinations. Destinations that
     *                        <strong>contains</strong> the keyword will be returned.
     * @param offset          The offset value for the collection of destination.
     * @param limit           The number of records to return from the collection of destinations.
     * @return A list of {@link Destination}s.
     * @throws DestinationManagerException
     */
    List<Destination> getDestinations(String protocol, String destinationType, String keyword, int offset, int limit)
            throws DestinationManagerException;

    /**
     * Deletes all the destinations.
     *
     * @param protocol        The protocol type matching for the destination type.
     * @param destinationType The destination type matching for the destination.
     * @throws DestinationManagerException
     */
    void deleteDestinations(String protocol, String destinationType) throws DestinationManagerException;

    /**
     * Gets a destination.
     *
     * @param protocol        The protocol type matching for the destination type.
     * @param destinationType The destination type matching for the destination.
     * @param destinationName The name of the destination.
     * @return A {@link Destination}.
     * @throws DestinationManagerException
     */
    Destination getDestination(String protocol, String destinationType, String destinationName)
            throws DestinationManagerException;

    /**
     * Creates a new destination.
     *
     * @param protocol        The protocol type matching for the destination type.
     * @param destinationType The destination type matching for the destination.
     * @param destinationName The name of the destination.
     * @throws DestinationManagerException
     */
    void createDestination(String protocol, String destinationType, String destinationName)
            throws DestinationManagerException;

    /**
     * Gets permission assigned to specific destination.
     *
     * @param protocol        The protocol type matching for the destination type.
     * @param destinationType The destination type matching for the destination.
     * @param destinationName The name of the destination.
     * @return A set of {@link DestinationRolePermission}s. Null if invalid destination.
     * @throws DestinationManagerException
     */
    Set<DestinationRolePermission> getDestinationPermissions(String protocol, String destinationType,
            String destinationName) throws DestinationManagerException;

    /**
     * Create permissions on a destination.
     *
     * @param protocol                  The protocol type matching for the destination type.
     * @param destinationType           The destination type matching for the destination.
     * @param destinationName           The name of the destination.
     * @param destinationRolePermission New permission.
     * @return Newly created permission.
     * @throws DestinationManagerException
     */
    DestinationRolePermission createDestinationPermission(String protocol, String destinationType,
            String destinationName, DestinationRolePermission destinationRolePermission)
            throws DestinationManagerException;

    /**
     * Updates permissions on a destination.
     *
     * @param protocol                  The protocol type matching for the destination type.
     * @param destinationType           The destination type matching for the destination.
     * @param destinationName           The name of the destination.
     * @param destinationRolePermission Updated permission.
     * @return Updated permission.
     * @throws DestinationManagerException
     */
    DestinationRolePermission updateDestinationPermission(String protocol, String destinationType,
            String destinationName, DestinationRolePermission destinationRolePermission)
            throws DestinationManagerException;

    /**
     * Deletes a destination.
     *
     * @param protocol        The protocol type matching for the destination type.
     * @param destinationType The destination type matching for the destination.
     * @param destinationName The name of the destination to be deleted.
     * @throws DestinationManagerException
     */
    void deleteDestination(String protocol, String destinationType, String destinationName)
            throws DestinationManagerException;

    /**
     * Gets names of destinations as strings.
     *
     * @param protocol        The protocol type matching for the destination type.
     * @param destinationType The destination type matching for the destination.
     * @param destinationName The name of the destination to be deleted.
     * @throws DestinationManagerException
     */
    List<String> getDestinationNames(String protocol, String destinationType, String destinationName)
            throws DestinationManagerException;
}
