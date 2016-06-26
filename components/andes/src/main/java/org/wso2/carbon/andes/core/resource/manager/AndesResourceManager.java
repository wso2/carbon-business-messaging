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

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.gs.collections.api.iterator.MutableLongIterator;
import com.gs.collections.impl.list.mutable.primitive.LongArrayList;
import com.gs.collections.impl.map.mutable.primitive.LongObjectHashMap;
import org.apache.commons.lang.BooleanUtils;
import org.wso2.carbon.andes.core.AMQPConstructStore;
import org.wso2.carbon.andes.core.Andes;
import org.wso2.carbon.andes.core.AndesChannel;
import org.wso2.carbon.andes.core.AndesConstants;
import org.wso2.carbon.andes.core.AndesException;
import org.wso2.carbon.andes.core.AndesMessage;
import org.wso2.carbon.andes.core.AndesMessageMetadata;
import org.wso2.carbon.andes.core.AndesMessagePart;
import org.wso2.carbon.andes.core.AndesQueue;
import org.wso2.carbon.andes.core.AndesSubscription;
import org.wso2.carbon.andes.core.AndesUtils;
import org.wso2.carbon.andes.core.DestinationType;
import org.wso2.carbon.andes.core.DisablePubAckImpl;
import org.wso2.carbon.andes.core.MessagingEngine;
import org.wso2.carbon.andes.core.ProtocolType;
import org.wso2.carbon.andes.core.internal.AndesContext;
import org.wso2.carbon.andes.core.internal.cluster.ClusterResourceHolder;
import org.wso2.carbon.andes.core.internal.inbound.FlowControlListener;
import org.wso2.carbon.andes.core.subscription.BasicSubscription;
import org.wso2.carbon.andes.core.subscription.LocalSubscription;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The resource manager class will hold all protocol handler that is registered. This manager will expose the resource
 * and operations of the broker.
 */
public class AndesResourceManager {
    /**
     * A 2 key map to store resource handlers. Keys are {@link ProtocolType} and {@link DestinationType}.
     */
    private Table<ProtocolType, DestinationType, ResourceHandler> resourceManagerTable = HashBasedTable.create();

    /**
     * A message decoder to decode messages belonging to a {@link ProtocolType}. Decodes messages for end user
     * applications.
     */
    private Map<ProtocolType, MessageDecoder> messageDecoderMap = new HashMap<>();

    /**
     * AndesChannel for this dead letter channel restore which implements flow control.
     */
    private AndesChannel andesChannel;

    /**
     * Publisher Acknowledgements are disabled for this MBean hence using DisablePubAckImpl to drop any pub ack request
     * by Andes.
     */
    private DisablePubAckImpl disablePubAck;

    /**
     * The message restore flowcontrol blocking state. If true message restore will be interrupted from dead letter
     * channel.
     */
    private boolean restoreBlockedByFlowControl = false;

    /**
     * Protocol type of the dead letter queue
     */
    private ProtocolType dlcProtocolType;

    /**
     * Initializing manager
     *
     * @throws AndesException
     */
    public AndesResourceManager() throws AndesException {
        andesChannel = Andes.getInstance().createChannel(new FlowControlListener() {
            @Override
            public void block() {
                restoreBlockedByFlowControl = true;
            }

            @Override
            public void unblock() {
                restoreBlockedByFlowControl = false;
            }

            @Override
            public void disconnect() {
                // Do nothing. since its not applicable.
            }
        });

        disablePubAck = new DisablePubAckImpl();
        dlcProtocolType = new ProtocolType(AndesConstants.DLC_PROTOCOL_NAME, AndesConstants.DLC_PROTOCOL_VERSION);
    }


    /**
     * Gets the collection of destinations(queues/topics)
     *
     * @param protocol        The protocol type matching for the destination type.
     * @param destinationType The destination type matching for the destination.
     * @param keyword         Search keyword for destination name. "*" will return all destinations. Destinations that
     *                        <strong>contains</strong> the keyword will be returned.
     * @param offset          The offset value for the collection of destination.
     * @param limit           The number of records to return from the collection of destinations.
     * @return A {@link AndesQueue} list with details of destinations.
     */
    public List<AndesQueue> getDestinations(ProtocolType protocol, DestinationType destinationType, String keyword,
                                            int offset, int limit) throws AndesException {
        return resourceManagerTable.get(protocol, destinationType).getDestinations(keyword, offset, limit);
    }

    /**
     * Deletes all the destinations.
     *
     * @param protocol        The protocol type matching for the destination type.
     * @param destinationType The destination type matching for the destination.
     */
    public void deleteDestinations(ProtocolType protocol, DestinationType destinationType) throws AndesException {
        resourceManagerTable.get(protocol, destinationType).deleteDestinations();
    }

    /**
     * Gets a destination.
     *
     * @param protocol        The protocol type matching for the destination type.
     * @param destinationType The destination type matching for the destination.
     * @param destinationName The name of the destination.
     * @return A {@link AndesQueue} with details of the destination.
     */
    public AndesQueue getDestination(ProtocolType protocol, DestinationType destinationType, String destinationName)
            throws AndesException {
        return resourceManagerTable.get(protocol, destinationType).getDestination(destinationName);
    }

    /**
     * Creates a new destination.
     *
     * @param protocol        The protocol type matching for the destination type.
     * @param destinationType The destination type matching for the destination.
     * @param destinationName The name of the destination.
     * @param currentUsername The username of the user who creates the destination.
     * @return A newly created {@link AndesQueue}.
     */
    public AndesQueue createDestination(ProtocolType protocol, DestinationType destinationType, String
            destinationName, String currentUsername) throws AndesException {
        return resourceManagerTable.get(protocol, destinationType).createDestination(destinationName, currentUsername);
    }

    /**
     * Deletes a destination.
     *
     * @param protocol        The protocol type matching for the destination type.
     * @param destinationType The destination type matching for the destination.
     * @param destinationName The name of the destination to be deleted.
     */
    public void deleteDestination(ProtocolType protocol, DestinationType destinationType, String destinationName)
            throws AndesException {
        resourceManagerTable.get(protocol, destinationType).deleteDestination(destinationName);
    }

    /**
     * Gets subscriptions belonging to a specific protocol type and destination type. The subscriptions can be filtered
     * by subscription name, destination name and whether they are active or not.
     *
     * @param protocol         The protocol type matching for the subscription.
     * @param destinationType  The destination type matching for the subscription.
     * @param subscriptionName The name of the subscription. If "*", all subscriptions are included. Else subscriptions
     *                         that <strong>contains</strong> the value are included.
     * @param destinationName  The name of the destination name. If "*", all destinations are included. Else
     *                         destinations that <strong>equals</strong> the value are included.
     * @param active           Filtering the subscriptions that are active or inactive. Supported values = "*", "true"
     *                         and "false".
     * @param offset           The starting index to return.
     * @param limit            The number of subscriptions to return.
     * @return An list of {@link AndesSubscription}.
     */
    public List<AndesSubscription> getSubscriptions(ProtocolType protocol, DestinationType destinationType, String
            subscriptionName, String destinationName, String active, int offset, int limit) throws AndesException {

        if (!("*".equals(active) || null != BooleanUtils.toBooleanObject(active))) {
            throw new AndesException("'Active' argument only allow values = \"*\", \"true\" and \"false\".");
        }
        return resourceManagerTable.get(protocol, destinationType).getSubscriptions(subscriptionName,
                                                                                    destinationName, active, offset,
                                                                                    limit);
    }

    /**
     * Close/unsubscribe subscriptions forcefully belonging to a specific protocol type, destination type.
     *
     * @param protocol        The protocol type matching for the subscription.
     * @param destinationType The subscription type matching for the subscription.
     * @param destinationName The name of the destination to close/unsubscribe. If "*", all destinations are included.
     *                        Else destinations that <strong>contains</strong> the value are included.
     */
    public void removeSubscriptions(ProtocolType protocol, DestinationType destinationType, String destinationName)
            throws AndesException {
        resourceManagerTable.get(protocol, destinationType).removeSubscriptions(destinationName);
    }

    /**
     * Close/Remove/Unsubscribe subscriptions forcefully belonging to a specific protocol type, destination type.
     *
     * @param protocol        The protocol type matching for the subscription.
     * @param destinationType The subscription type matching for the subscription.
     * @param destinationName The name of the destination to close/unsubscribe. If "*", all destinations are included.
     *                        Else destinations that <strong>equals</strong> the value are included.
     */
    public void removeSubscription(ProtocolType protocol, DestinationType destinationType, String destinationName,
                                   String subscriptionId) throws AndesException {
        resourceManagerTable.get(protocol, destinationType).removeSubscription(destinationName, subscriptionId);
    }

    /**
     * Browse message of a destination using message ID.
     *
     * @param protocol        The protocol type matching for the message.
     * @param destinationType The destination type matching for the message.
     * @param destinationName The name of the destination
     * @param content         Whether to return message content or not.
     * @param nextMessageID   The starting message ID to return from.
     * @param limit           The number of messages to return.
     * @return An list of {@link AndesMessage}.
     */
    public List<AndesMessage> browseDestinationWithMessageID(ProtocolType protocol, DestinationType destinationType,
                                                             String destinationName, boolean content,
                                                             long nextMessageID, int limit) throws AndesException {
        return resourceManagerTable.get(protocol, destinationType).browseDestinationWithMessageID(destinationName,
                                                                                                  content,
                                                                                                  nextMessageID, limit);
    }

    /**
     * Browse message of a destination. Please note this is time costly.
     *
     * @param protocol        The protocol type matching for the message.
     * @param destinationType The destination type matching for the message.
     * @param destinationName The name of the destination
     * @param content         Whether to return message content or not.
     * @param offset          Starting index of the messages to return.
     * @param limit           The number of messages to return.
     * @return An list of {@link AndesMessage}.
     */
    public List<AndesMessage> browseDestinationWithOffset(ProtocolType protocol, DestinationType destinationType,
                                                          String destinationName, boolean content, int offset,
                                                          int limit) throws AndesException {
        return resourceManagerTable.get(protocol, destinationType).
                browseDestinationWithOffset(destinationName, content, offset, limit);
    }

    /**
     * Gets a message by message ID belonging to a particular protocol, destination type and destination name.
     *
     * @param protocol        The protocol type matching for the message.
     * @param destinationType The destination type matching for the message.
     * @param destinationName The name of the destination to which the message belongs to.
     * @param andesMessageID  The message ID. This message is the andes metadata message ID.
     * @param content         Whether to return content or not.
     * @return A {@link AndesMessage}.
     */
    public AndesMessage getMessage(ProtocolType protocol, DestinationType destinationType, String destinationName,
                                   long andesMessageID, boolean content) throws AndesException {
        return resourceManagerTable.get(protocol, destinationType).getMessage(destinationName, andesMessageID, content);
    }

    /**
     * Purge all messages belonging to a destination.
     *
     * @param protocol        The protocol type matching for the message.
     * @param destinationType The destination type matching for the message.
     * @param destinationName The name of the destination to purge messages.
     */
    public void deleteMessages(ProtocolType protocol, DestinationType destinationType, String destinationName)
            throws AndesException {
        resourceManagerTable.get(protocol, destinationType).deleteMessages(destinationName);
    }

    /**
     * Restore a given browser message Id list from the Dead Letter Queue to the same queue it was previous in before
     * moving to the Dead Letter Queue and remove them from the Dead Letter Queue.
     *
     * @param andesMetadataIDs The browser message Ids
     * @param dlcQueueName     The Dead Letter Queue Name for the tenant
     */
    public void restoreMessagesFromDeadLetterQueue(long[] andesMetadataIDs, String dlcQueueName) {
        if (null != andesMetadataIDs) {
            LongArrayList andesMessageIdList = new LongArrayList(andesMetadataIDs.length);
            andesMessageIdList.addAll(andesMetadataIDs);
            List<AndesMessageMetadata> messagesToRemove = new ArrayList<>(andesMessageIdList.size());

            try {
                LongObjectHashMap<List<AndesMessagePart>> messageContent = Andes.getInstance()
                        .getContent(andesMessageIdList);

                boolean interruptedByFlowControl = false;

                MutableLongIterator iterator = andesMessageIdList.longIterator();

                while (iterator.hasNext()) {
                    long messageId = iterator.next();
                    if (restoreBlockedByFlowControl) {
                        interruptedByFlowControl = true;
                        break;
                    }
                    AndesMessageMetadata metadata = Andes.getInstance().getMessageMetaData(messageId);
                    String destination = metadata.getDestination();

                    metadata.setStorageDestination(AndesUtils.getStorageQueueForDestination(destination,
                        ClusterResourceHolder.getInstance().getClusterManager().getMyNodeID(), DestinationType.QUEUE));

                    messagesToRemove.add(metadata);

                    AndesMessageMetadata clonedMetadata = metadata.shallowCopy(metadata.getMessageID());
                    AndesMessage andesMessage = new AndesMessage(clonedMetadata);

                    // Update Andes message with all the chunk details
                    List<AndesMessagePart> messageParts = messageContent.get(messageId);
                    messageParts.forEach(andesMessage::addMessagePart);

                    // Handover message to Andes. This will generate a new message ID and store it
                    Andes.getInstance().messageReceived(andesMessage, andesChannel, disablePubAck);
                }

                // Delete old messages
                Andes.getInstance().deleteMessagesFromDLC(messagesToRemove);

                if (interruptedByFlowControl) {
                    // Throw this out so UI will show this to the user as an error message.
                    throw new RuntimeException("Message restore from dead letter queue has been interrupted by flow "
                                                       + "control. Please try again later.");
                }

            } catch (AndesException e) {
                throw new RuntimeException("Error restoring messages from " + dlcQueueName, e);
            }
        }
    }

    /**
     * Restore a given browser message Id list from the Dead Letter Queue to a different given queue in the same tenant
     * and remove them from the Dead Letter Queue.
     *
     * @param andesMetadataIDs        The browser message Ids
     * @param newDestinationQueueName The new destination
     * @param dlcQueueName            The Dead Letter Queue Name for the tenant
     */
    public void restoreMessagesFromDeadLetterQueue(long[] andesMetadataIDs, String newDestinationQueueName,
                                                   String dlcQueueName) {
        if (null != andesMetadataIDs) {

            LongArrayList andesMessageIdList = new LongArrayList(andesMetadataIDs.length);
            andesMessageIdList.addAll(andesMetadataIDs);
            List<AndesMessageMetadata> messagesToRemove = new ArrayList<>(andesMessageIdList.size());

            try {
                LongObjectHashMap<List<AndesMessagePart>> messageContent =
                        Andes.getInstance().getContent(andesMessageIdList);

                boolean interruptedByFlowControl = false;

                MutableLongIterator iterator = andesMessageIdList.longIterator();
                while (iterator.hasNext()) {

                    long messageId = iterator.next();
                    if (restoreBlockedByFlowControl) {
                        interruptedByFlowControl = true;
                        break;
                    }

                    AndesMessageMetadata metadata = Andes.getInstance().getMessageMetaData(messageId);

                    // Set the new destination queue
                    metadata.setDestination(newDestinationQueueName);
                    metadata.setStorageDestination(
                            AndesUtils.getStorageQueueForDestination(newDestinationQueueName,
                                                                     ClusterResourceHolder.getInstance()
                                                                             .getClusterManager().getMyNodeID(),
                                                                     DestinationType.QUEUE));

                    metadata.setDestination(newDestinationQueueName);
                    metadata.setDeliveryStrategy(AndesUtils.QUEUE_DELIVERY_STRATEGY);
                    AndesMessageMetadata clonedMetadata = metadata.shallowCopy(metadata.getMessageID());
                    AndesMessage andesMessage = new AndesMessage(clonedMetadata);

                    messagesToRemove.add(metadata);

                    // Update Andes message with all the chunk details
                    List<AndesMessagePart> messageParts = messageContent.get(messageId);
                    messageParts.forEach(andesMessage::addMessagePart);

                    // Handover message to Andes. This will generate a new message ID and store it
                    Andes.getInstance().messageReceived(andesMessage, andesChannel, disablePubAck);
                }

                // Delete old messages
                Andes.getInstance().deleteMessagesFromDLC(messagesToRemove);

                if (interruptedByFlowControl) {
                    // Throw this out so UI will show this to the user as an error message.
                    throw new RuntimeException("Message restore from dead letter queue has been interrupted by flow " +
                                                       "control. Please try again later.");
                }

            } catch (AndesException e) {
                throw new RuntimeException("Error restoring messages from " + dlcQueueName, e);
            }
        }
    }

    /**
     * Delete a selected message list from a given Dead Letter Queue of a tenant.
     *
     * @param andesMetadataIDs The browser message Ids
     * @param dlcQueueName     The Dead Letter Queue Name for the tenant
     */
    public void deleteMessagesFromDeadLetterQueue(long[] andesMetadataIDs, String dlcQueueName) {
        List<AndesMessageMetadata> messageMetadataList = new ArrayList<>(andesMetadataIDs.length);

        for (long andesMetadataID : andesMetadataIDs) {
            AndesMessageMetadata messageToRemove =
                    new AndesMessageMetadata(andesMetadataID, dlcQueueName, dlcProtocolType);
            messageMetadataList.add(messageToRemove);
        }

        // Deleting messages which are in the list.
        try {
            Andes.getInstance().deleteMessagesFromDLC(messageMetadataList);
        } catch (AndesException e) {
            throw new RuntimeException("Error deleting messages from Dead Letter Channel", e);
        }
    }

    /**
     * Gets the pending message count for a given storage queue name.
     *
     * @param storageQueueName Storage queue name.
     * @return Number of pending messages.
     */
    public long getMessageCountForStorageQueue(String storageQueueName) throws AndesException {
        return MessagingEngine.getInstance().getMessageCountOfQueue(storageQueueName);
    }

    /**
     * Gets all the destination names as a list of strings.
     *
     * @return A list of destination names.
     * @throws AndesException
     */
    public List<String> getAllDestinationNames() throws AndesException {
        return AndesContext.getInstance().getAmqpConstructStore().getQueueNames();
    }

    /**
     * Registers a {@link ResourceHandler} for resource managing.
     *
     * @param protocolType    The {@link ProtocolType} for the resource handler.
     * @param destinationType The {@link DestinationType} for the resource handler.
     * @param resourceHandler The resource handler.
     */
    public void registerResourceHandler(ProtocolType protocolType, DestinationType destinationType, ResourceHandler
            resourceHandler) {
        resourceManagerTable.put(protocolType, destinationType, resourceHandler);
    }

    /**
     * Unregistered a resource manager.
     *
     * @param protocolType    The {@link ProtocolType} of the resource handler.
     * @param destinationType The {@link DestinationType} of the resource handler.
     */
    public void unregisterResourceHandler(ProtocolType protocolType, DestinationType destinationType) {
        resourceManagerTable.remove(protocolType, destinationType);
    }

    /**
     * Registers a message decoder with given protocol type.
     *
     * @param protocolType   The protocol type.
     * @param messageDecoder The message decoder instance.
     */
    public void registerMessageDecoder(ProtocolType protocolType, MessageDecoder messageDecoder) {
        messageDecoderMap.put(protocolType, messageDecoder);
    }

    /**
     * Unregisters a message decoder based on protocol type.
     *
     * @param protocolType The protocol type.
     */
    public void unregisterMessageDecoder(ProtocolType protocolType) {
        messageDecoderMap.remove(protocolType);
    }

    /**
     * Gets the message decoder from protocol type.
     *
     * @param protocolType The protocol type.
     * @return The message decoder instance.
     */
    public MessageDecoder getMessageDecoder(ProtocolType protocolType) {
        return messageDecoderMap.get(protocolType);
    }

}
