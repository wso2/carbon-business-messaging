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

package org.wso2.carbon.andes.core.resource.manager;

import org.wso2.carbon.andes.core.AndesException;
import org.wso2.carbon.andes.core.AndesMessage;
import org.wso2.carbon.andes.core.AndesQueue;
import org.wso2.carbon.andes.core.AndesSubscription;

import java.util.List;

/**
 * Interface for handling resource by the {@link AndesResourceManager}. This should be implemented by a transport to
 * expose its resources.
 */
public interface ResourceHandler {

    /**
     * Gets the collection of destinations(queues/topics)
     *
     * @param keyword Search keyword for destination name. "*" will return all destinations. Destinations that
     *                <strong>contains</strong> the keyword will be returned.
     * @param offset  The offset value for the collection of destination.
     * @param limit   The number of records to return from the collection of destinations.
     * @return A list of destinations.
     */
    List<AndesQueue> getDestinations(String keyword, int offset, int limit) throws AndesException;

    /**
     * Deletes all the destinations.
     */
    void deleteDestinations() throws AndesException;

    /**
     * Gets a destination.
     *
     * @param destinationName The name of the destination.
     * @return A destination object.
     */
    AndesQueue getDestination(String destinationName) throws AndesException;

    /**
     * Creates a new destination.
     *
     * @param destinationName The name of the destination.
     * @param currentUsername The username of the user who creates the destination.
     * @return A newly created destination object.
     */
    AndesQueue createDestination(String destinationName, String currentUsername) throws AndesException;

    /**
     * Deletes a destination.
     *
     * @param destinationName The name of the destination to be deleted.
     */
    void deleteDestination(String destinationName) throws AndesException;

    /**
     * Gets subscriptions belonging to a specific protocol type and destination type. The subscriptions can be filtered
     * by subscription name, destination name and whether they are active or not.
     *
     * @param subscriptionName The name of the subscription. If "*", all subscriptions are included. Else subscriptions
     *                         that <strong>contains</strong> the value are included.
     * @param destinationName  The name of the destination name. If "*", all destinations are included. Else
     *                         destinations that <strong>equals</strong> the value are included.
     * @param active           Filtering the subscriptions that are active or inactive. Supported values = "*", "true"
     *                         and "false".
     * @param offset           The starting index to return.
     * @param limit            The number of subscriptions to return.
     * @return A list subscriptions.
     */
    List<AndesSubscription> getSubscriptions(String subscriptionName, String destinationName, String active,
                                             int offset, int limit) throws AndesException;

    /**
     * Close/unsubscribe subscriptions forcefully belonging to a specific protocol type, destination type.
     *
     * @param destinationName The name of the destination to close/unsubscribe. If "*", all destinations are included.
     *                        Else destinations that <strong>contains</strong> the value are included.
     */
    void removeSubscriptions(String destinationName) throws AndesException;

    /**
     * Close/Remove/Unsubscribe subscriptions forcefully belonging to a specific protocol type, destination type.
     *
     * @param destinationName The name of the destination to close/unsubscribe. If "*", all destinations are included.
     *                        Else destinations that <strong>equals</strong> the value are included.
     */
    void removeSubscription(String destinationName, String subscriptionId) throws AndesException;

    /**
     * Browse message of a destination using message ID.
     *
     * @param destinationName The name of the destination
     * @param content         Whether to return message content or not.
     * @param nextMessageID   The starting message ID to return from.
     * @param limit           The number of messages to return.
     * @return A list of messages.
     */
    List<AndesMessage> browseDestinationWithMessageID(String destinationName, boolean content, long nextMessageID, int
            limit) throws AndesException;

    /**
     * Browse message of a destination. Please note this is time costly.
     *
     * @param destinationName The name of the destination
     * @param content         Whether to return message content or not.
     * @param offset          Starting index of the messages to return.
     * @param limit           The number of messages to return.
     * @return A list of messages.
     */
    List<AndesMessage> browseDestinationWithOffset(String destinationName, boolean content, int offset, int limit)
            throws AndesException;

    /**
     * Gets a message by message ID belonging to a particular protocol, destination type and destination name.
     *
     * @param destinationName The name of the destination to which the message belongs to.
     * @param andesMessageID  The message ID. This message is the andes metadata message ID.
     * @param content         Whether to return content or not.
     * @return A message.
     */
    AndesMessage getMessage(String destinationName, long andesMessageID, boolean content) throws
                                                                                          AndesException;

    /**
     * Purge all messages belonging to a destination.
     *
     * @param destinationName The name of the destination to purge messages.
     */
    void deleteMessages(String destinationName) throws AndesException;

    /**
     * Gets the destination names with a search keyword.
     * @param keyword         Search keyword for searching. Use "*" for all destination names, else it will use
     *                        contains.
     * @return A list of destination names.
     */
    List<String> getDestinationNames(String keyword);
}
