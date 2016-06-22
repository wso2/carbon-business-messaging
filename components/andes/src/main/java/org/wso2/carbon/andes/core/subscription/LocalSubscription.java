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

package org.wso2.carbon.andes.core.subscription;

import com.gs.collections.impl.map.mutable.ConcurrentHashMap;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.andes.core.AndesContent;
import org.wso2.carbon.andes.core.AndesException;
import org.wso2.carbon.andes.core.AndesMessageMetadata;
import org.wso2.carbon.andes.core.DeliverableAndesMetadata;
import org.wso2.carbon.andes.core.DestinationType;
import org.wso2.carbon.andes.core.MessageStatus;
import org.wso2.carbon.andes.core.MessagingEngine;
import org.wso2.carbon.andes.core.ProtocolMessage;
import org.wso2.carbon.andes.core.internal.configuration.AndesConfigurationManager;
import org.wso2.carbon.andes.core.internal.configuration.enums.AndesConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * This is the class that represents a subscription in andes kernel. It is responsible
 * for handling both inbound (protocol > kernel) and outbound (kernel > protocol) subscription
 * events. For handling outbound events it keeps a OutboundSubscription object and forward
 * requests
 */
public class LocalSubscription extends BasicSubscription implements InboundSubscription {

    private static Log log = LogFactory.getLog(LocalSubscription.class);

    /**
     * Outbound subscription reference. We forward outbound events to this object. Get its response
     * and act upon (make kernel side changes)
     */
    private OutboundSubscription subscription;

    /**
     * Map to track messages being sent <message id, MsgData reference>. This map bares message
     * reference at kernel side
     */
    private final ConcurrentHashMap<Long, DeliverableAndesMetadata> messageSendingTracker = new ConcurrentHashMap<>();

    private Integer maxNumberOfUnAcknowledgedMessages = 100000;

    /**
     * Create a new local subscription object in andes kernel
     *
     * @param subscription                            protocol subscription to send messages
     * @param subscriptionID                          ID of the subscription (unique)
     * @param destination                             subscription bound destination
     * @param isExclusive                             true is the subscription is exclusive
     * @param isDurable                               true if subscription is durable
     * @param subscribedNode                          identifier of the node actual subscription exists
     * @param subscribeTime                           timestamp of the subscription made
     * @param targetQueue                             name of underlying queue subscription is bound to
     * @param targetQueueOwner                        name of the owner of underlying queue subscription is bound to
     * @param targetQueueBoundExchange                name of exchange of underlying queue subscription is bound to
     * @param targetQueueBoundExchangeType            type of exchange of underlying queue subscription is bound to
     * @param isTargetQueueBoundExchangeAutoDeletable is queue subscription is bound to is auto deletable (this can
     *                                                be true if subscription is non durable)
     * @param hasExternalSubscriptions                true if subscription is active (has a live TCP connection)
     * @param destinationType                         The type of the destination
     */
    public LocalSubscription(OutboundSubscription subscription, String subscriptionID, String destination,
                             boolean isExclusive, boolean isDurable, String subscribedNode, long subscribeTime,
                             String targetQueue, String targetQueueOwner, String targetQueueBoundExchange,
                             String targetQueueBoundExchangeType, Short isTargetQueueBoundExchangeAutoDeletable,
                             boolean hasExternalSubscriptions, DestinationType destinationType) {

        super(subscriptionID, destination, isExclusive, isDurable, subscribedNode, subscribeTime, targetQueue,
              targetQueueOwner, targetQueueBoundExchange, targetQueueBoundExchangeType,
              isTargetQueueBoundExchangeAutoDeletable, hasExternalSubscriptions, destinationType);

        this.subscription = subscription;

        // In case of mock subscriptions, this becomes null
        if (null != subscription) {
            //We need to keep the protocolType in basic subscription for notification
            setProtocolType(subscription.getProtocolType());

            setStorageQueueName(subscription.getStorageQueueName(destination, subscribedNode));
        }

        this.maxNumberOfUnAcknowledgedMessages = AndesConfigurationManager
                .readValue(AndesConfiguration.PERFORMANCE_TUNING_ACK_HANDLING_MAX_UNACKED_MESSAGES);

    }

    /**
     * Forcefully disconnects protocol subscriber from server. This is initiated by a server admin using the management
     * console.
     *
     * @throws AndesException
     */
    public void forcefullyDisconnect() throws AndesException {
        log.info("Forcefully disconnecting subscription: " + this.toString());
        subscription.forcefullyDisconnect();
    }

    /**
     * Send message to the underlying protocol subscriber
     *
     * @param messageMetadata metadata of the message
     * @param content         content of the message
     * @return true if the send is a success
     * @throws AndesException
     */
    public boolean sendMessageToSubscriber(ProtocolMessage messageMetadata, AndesContent content)
            throws AndesException {

        //It is needed to add the message reference to the tracker and increase un-ack message count BEFORE
        // actual message send because if it is not done ack can come BEFORE executing those lines in parallel world
        addMessageToSendingTracker(messageMetadata);
        return subscription.sendMessageToSubscriber(messageMetadata, content);
    }

    /**
     * Check if message is accepted by 'selector' set to the subscription.
     *
     * @param messageMetadata message to be checked
     * @return true if message is selected, false otherwise
     * @throws AndesException on an error
     */
    public boolean isMessageAcceptedBySelector(AndesMessageMetadata messageMetadata) throws AndesException {
        return subscription.isMessageAcceptedBySelector(messageMetadata);
    }

    /**
     * Get all sent but not acknowledged messages for the subscriber
     *
     * @return list of messages
     */
    public List<DeliverableAndesMetadata> getUnackedMessages() {
        return new ArrayList<>(messageSendingTracker.values());
    }

    /**
     * Remove message from sending tracker. This is called when a send
     * error happens at the Outbound subscriber protocol level. ACK or
     * REJECT can never be received for that message
     *
     * @param messageID Id of the message
     */
    public void removeSentMessageFromTracker(long messageID) {
        messageSendingTracker.remove(messageID);
    }

    /**
     * Get message metadata reference by message ID. Returns null if the reference
     * is not found
     *
     * @param messageID ID of the message
     * @return message metadata reference
     */
    public DeliverableAndesMetadata getMessageByMessageID(long messageID) {
        DeliverableAndesMetadata metadata = messageSendingTracker.get(messageID);
        if (null == metadata) {
            log.error("Message reference has been already cleared for message id " + messageID
                              + ". Acknowledge or Nak is already received");
        }
        return metadata;
    }

    /**
     * Is the underlying protocol subscription active and can accept
     * messages
     *
     * @return true if subscription is active
     */
    public boolean isActive() {
        return subscription.isActive();
    }

    /**
     * ID of the protocol channel this subscription holds
     *
     * @return unique id if the subscription channel
     */
    public UUID getChannelID() {
        return subscription.getChannelID();
    }

    /**
     * Check if this subscription has ability to accept messages
     * If pending ack count is high it does not have ability to accept new messages
     *
     * @return true if able to accept messages
     */
    public boolean hasRoomToAcceptMessages() {
        int notAcknowledgedMsgCount = messageSendingTracker.size();
        if (notAcknowledgedMsgCount < maxNumberOfUnAcknowledgedMessages) {
            return true;
        } else {

            if (log.isDebugEnabled()) {
                log.debug("Not selected. Too much pending acks, subscription = " + this + " pending count =" +
                                  (notAcknowledgedMsgCount));
            }

            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void ackReceived(long messageID) throws AndesException {
        messageSendingTracker.remove(messageID);
        if (log.isDebugEnabled()) {
            log.debug(
                    "Ack. Removed message reference. Message Id = " + messageID + " subscriptionID= " + subscriptionID);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void msgRejectReceived(long messageID) throws AndesException {
        if (log.isDebugEnabled()) {
            log.debug("Reject. Removed message reference. Message Id = " + messageID + " subscriptionID= "
                              + subscriptionID);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void close() throws AndesException {

        for (DeliverableAndesMetadata andesMetadata : messageSendingTracker.values()) {
            andesMetadata.markDeliveredChannelAsClosed(getChannelID());
            //re-evaluate ACK if a topic subscriber has closed
            if (!isDurable()) {
                List<DeliverableAndesMetadata> messagesToRemove = new ArrayList<>();
                andesMetadata.evaluateMessageAcknowledgement();
                //for topic messages see if we can delete the message
                if ((!andesMetadata.isOKToDispose()) && (andesMetadata.isTopic())) {
                    if (andesMetadata.getLatestState().equals(MessageStatus.ACKED_BY_ALL)) {
                        messagesToRemove.add(andesMetadata);
                    }
                }
                MessagingEngine.getInstance().deleteMessages(messagesToRemove);
            }
        }
    }

    /**
     * Add message to sending tracker which keeps messages delivered to this channel
     *
     * @param messageData message to add
     */
    private void addMessageToSendingTracker(ProtocolMessage messageData) {

        if (log.isDebugEnabled()) {
            log.debug("Adding message to sending tracker channel id = " + getChannelID() + " message id = " +
                              messageData.getMessageID());
        }

        DeliverableAndesMetadata messageDataToAdd = messageSendingTracker.get(messageData.getMessageID());

        if (null == messageDataToAdd) {
            //we need to put message reference to the sending tracker
            messageSendingTracker.put(messageData.getMessageID(), messageData.getMessage());
            if (log.isDebugEnabled()) {
                log.debug("Added message reference. Message Id = " + messageData.getMessageID() + " subscriptionID= "
                                  + subscriptionID);
            }
        }
    }

    public boolean equals(Object o) {
        if (o instanceof LocalSubscription) {
            LocalSubscription c = (LocalSubscription) o;
            if (this.subscriptionID.equals(c.subscriptionID) &&
                    this.getSubscribedNode().equals(c.getSubscribedNode()) &&
                    this.targetQueue.equals(c.targetQueue) &&
                    this.targetQueueBoundExchange.equals(c.targetQueueBoundExchange)) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public int hashCode() {
        return new HashCodeBuilder(17, 31).
                append(subscriptionID).
                append(getSubscribedNode()).
                append(targetQueue).
                append(targetQueueBoundExchange).
                toHashCode();
    }

    public OutboundSubscription getOutboundSubscription() {
        return subscription;
    }
}
