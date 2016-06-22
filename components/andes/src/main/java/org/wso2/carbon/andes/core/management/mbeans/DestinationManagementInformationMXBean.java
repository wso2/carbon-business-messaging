/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.andes.core.management.mbeans;

import javax.management.MBeanException;
import javax.management.openmbean.CompositeData;

/**
 * Interface for managing destinations - queues/topics.
 */
public interface DestinationManagementInformationMXBean {

    /**
     * Gets the collection of destinations(queues/topics)
     *
     * @param protocolTypeAsString    The protocol type matching for the destination type.
     * @param destinationTypeAsString The destination type matching for the destination.
     * @param keyword                 Search keyword for destination name. "*" will return all destinations.
     *                                Destinations that <strong>contains</strong> the keyword will be returned.
     * @param offset                  The offset value for the collection of destination.
     * @param limit                   The number of records to return from the collection of destinations.
     * @return A {@link CompositeData} array with details of destinations.
     * @throws MBeanException
     */
    CompositeData[] getDestinations(String protocolTypeAsString, String destinationTypeAsString, String keyword, int
            offset, int limit) throws MBeanException;

    /**
     * Deletes all the destinations.
     *
     * @param protocolTypeAsString    The protocol type matching for the destination type.
     * @param destinationTypeAsString The destination type matching for the destination.
     * @throws MBeanException
     */
    void deleteDestinations(String protocolTypeAsString, String destinationTypeAsString) throws MBeanException;

    /**
     * Gets a destination.
     *
     * @param protocolTypeAsString    The protocol type matching for the destination type.
     * @param destinationTypeAsString The destination type matching for the destination.
     * @param destinationName         The name of the destination.
     * @return A {@link CompositeData} with details of the destination.
     * @throws MBeanException
     */
    CompositeData getDestination(String protocolTypeAsString, String destinationTypeAsString, String destinationName)
            throws MBeanException;

    /**
     * Creates a new destination.
     *
     * @param protocolTypeAsString    The protocol type matching for the destination type.
     * @param destinationTypeAsString The destination type matching for the destination.
     * @param destinationName         The name of the destination.
     * @param currentUsername         The username of the user who creates the destination.
     * @return A {@link CompositeData} with details of the newly created destination.
     * @throws MBeanException
     */
    CompositeData createDestination(String protocolTypeAsString, String destinationTypeAsString, String
            destinationName, String currentUsername) throws MBeanException;

    /**
     * Deletes a destination.
     *
     * @param protocolTypeAsString    The protocol type matching for the destination type.
     * @param destinationTypeAsString The destination type matching for the destination.
     * @param destinationName         The name of the destination to be deleted.
     * @throws MBeanException
     */
    void deleteDestination(String protocolTypeAsString, String destinationTypeAsString, String destinationName)
            throws MBeanException;
}
