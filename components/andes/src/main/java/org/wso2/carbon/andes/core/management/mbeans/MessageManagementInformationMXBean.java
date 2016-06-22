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
 * Interface for managing messages.
 */
public interface MessageManagementInformationMXBean {

    /**
     * Browse message of a destination using message ID.
     * <p>
     * To browse messages without message ID, use {@link MessageManagementInformationMXBean#browseDestinationWithOffset
     * (String, String, String, boolean, int, int)}.
     *
     * @param protocolTypeAsString    The protocol type matching for the message.
     * @param destinationTypeAsString The destination type matching for the message.
     * @param destinationName         The name of the destination
     * @param getContentFlag          Whether to return message content or not.
     * @param nextMessageID           The starting message ID to return from.
     * @param limit                   The number of messages to return.
     * @return An array of {@link CompositeData} representing a collection messages.
     * @throws MBeanException
     * @see
     */
    CompositeData[] browseDestinationWithMessageID(String protocolTypeAsString, String destinationTypeAsString,
                                                   String destinationName, boolean getContentFlag, long
                                                           nextMessageID, int limit) throws MBeanException;

    /**
     * Browse message of a destination. Please note this is time costly.
     * <p>
     * To browse messages with message ID, use {@link MessageManagementInformationMXBean#browseDestinationWithMessageID
     * (String, String, String, boolean, long, int)}.
     *
     * @param protocolTypeAsString    The protocol type matching for the message.
     * @param destinationTypeAsString The destination type matching for the message.
     * @param destinationName         The name of the destination
     * @param getContentFlag          Whether to return message content or not.
     * @param offset                  Starting index of the messages to return.
     * @param limit                   The number of messages to return.
     * @return An array of {@link CompositeData} representing a collection messages.
     * @throws MBeanException
     * @see
     */
    CompositeData[] browseDestinationWithOffset(String protocolTypeAsString, String destinationTypeAsString, String
            destinationName, boolean getContentFlag, int offset, int limit) throws MBeanException;

    /**
     * Gets a message by message ID belonging to a particular protocol, destination type and destination name.
     *
     * @param protocolTypeAsString    The protocol type matching for the message.
     * @param destinationTypeAsString The destination type matching for the message.
     * @param destinationName         The name of the destination to which the message belongs to.
     * @param andesMessageID          The message ID. This message is the andes metadata message ID.
     * @param getContentFlag          Whether to return content or not.
     * @return A {@link CompositeData} representing a message.
     * @throws MBeanException
     */
    CompositeData getMessage(String protocolTypeAsString, String destinationTypeAsString, String destinationName,
                             long andesMessageID, boolean getContentFlag) throws MBeanException;

    /**
     * Purge all messages belonging to a destination.
     *
     * @param protocolTypeAsString    The protocol type matching for the message.
     * @param destinationTypeAsString The destination type matching for the message.
     * @param destinationName         The name of the destination to purge messages.
     * @throws MBeanException
     */
    void deleteMessages(String protocolTypeAsString, String destinationTypeAsString, String destinationName)
            throws MBeanException;
}
