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

import org.wso2.carbon.business.messaging.admin.services.exceptions.InternalServerException;
import org.wso2.carbon.business.messaging.admin.services.types.Destination;
import java.util.List;

/**
 * This interface provides the base for managing all messages related services.
 */
public interface DestinationManagerService {
    /**
     * Gets the collection of destinations(queues/topics).
     *
     * @param protocol        The protocol type matching for the destination type.
     * @param destinationType The destination type matching for the destination.
     * @param keyword         Search keyword for destination name. "*" will return all destinations. Destinations that
     *                        <strong>contains</strong> the keyword will be returned.
     * @param offset          The offset value for the collection of destination.
     * @param limit           The number of records to return from the collection of destinations.
     * @return A list of destination names
     * @throws InternalServerException Error in handling destination information
     */
    List<String> getDestinations(String protocol, String destinationType, String keyword, int offset, int limit)
            throws InternalServerException;

    /**
     * Gets a destination.
     *
     * @param protocol        The protocol type matching for the destination type.
     * @param destinationType The destination type matching for the destination.
     * @param destinationName The name of the destination.
     * @return A {@link Destination}.
     * @throws InternalServerException Error in handling destination information
     */
    Destination getDestination(String protocol, String destinationType, String destinationName)
            throws InternalServerException;

    /**
     * Creates a new destination.
     *
     * @param protocol        The protocol type matching for the destination type.
     * @param destinationType The destination type matching for the destination.
     * @param destinationName The name of the destination.
     * @throws InternalServerException Error in handling destination information
     */
    void createDestination(String protocol, String destinationType, String destinationName)
            throws InternalServerException;

    /**
     * Deletes a destination.
     *
     * @param protocol        The protocol type matching for the destination type.
     * @param destinationType The destination type matching for the destination.
     * @param destinationName The name of the destination to be deleted.
     * @throws InternalServerException Error in handling destination information
     */
    void deleteDestination(String protocol, String destinationType, String destinationName)
            throws InternalServerException;

    /**
     * Check if the given destination exits in the system.
     *
     * @param protocol        The protocol type matching for the destination type.
     * @param destinationType The destination type matching for the destination.
     * @param destinationName The name of the destination to be checked.
     * @return true if the destination exists false otherwise
     * @throws InternalServerException Error in handling destination information
     */
    boolean isDestinationExist(String protocol, String destinationType, String destinationName)
            throws InternalServerException;
}
