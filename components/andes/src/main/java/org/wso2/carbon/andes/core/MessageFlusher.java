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

package org.wso2.carbon.andes.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.andes.core.internal.AndesContext;
import org.wso2.carbon.andes.core.internal.configuration.AndesConfigurationManager;
import org.wso2.carbon.andes.core.internal.configuration.enums.AndesConfiguration;
import org.wso2.carbon.andes.core.internal.configuration.util.TopicMessageDeliveryStrategy;
import org.wso2.carbon.andes.core.internal.outbound.DisruptorBasedFlusher;
import org.wso2.carbon.andes.core.internal.slot.Slot;
import org.wso2.carbon.andes.core.subscription.LocalSubscription;
import org.wso2.carbon.andes.core.subscription.SubscriptionEngine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


/**
 * <code>MessageFlusher</code> Handles the task of polling the user queues and flushing the
 * messages to subscribers There will be one Flusher per Queue Per Node
 */
public class MessageFlusher {

    private static Log log = LogFactory.getLog(MessageFlusher.class);

    private final DisruptorBasedFlusher flusherExecutor;

    //per destination
    private Integer maxNumberOfReadButUndeliveredMessages = 5000;

    /**
     * The map of objects which keeps delivery information with respect to their DestinationType and the
     * destination name. The destination name in this is NOT the storage queue name but the original destination.
     */
    private Map<DestinationType, Map<String, MessageDeliveryInfo>> subscriptionCursar4QueueMap =
            new HashMap<>();

    private SubscriptionEngine subscriptionEngine;

    /**
     * Message flusher for queue message delivery. Depending on the behaviour of the strategy
     * conditions to push messages to subscribers vary.
     */
    private MessageDeliveryStrategy queueMessageFlusher;

    /**
     * Message flusher for topic message delivery. Depending on the behaviour of the strategy
     * conditions to push messages to subscribers vary.
     */
    private MessageDeliveryStrategy topicMessageFlusher;


    /**
     * List of delivery rules to evaluate. Before scheduling message to protocol for
     * delivery we evaluate these and if failed we take necessary actions
     */
    private List<CommonDeliveryRule> deliveryRuleList = new ArrayList<>();


    private static MessageFlusher messageFlusher = new MessageFlusher();

    public MessageFlusher() {

        this.subscriptionEngine = AndesContext.getInstance().getSubscriptionEngine();
        flusherExecutor = new DisruptorBasedFlusher();

        this.maxNumberOfReadButUndeliveredMessages = AndesConfigurationManager.readValue
                (AndesConfiguration.PERFORMANCE_TUNING_DELIVERY_MAX_READ_BUT_UNDELIVERED_MESSAGES);

        //set queue message flusher
        this.queueMessageFlusher = new FlowControlledQueueMessageDeliveryImpl(subscriptionEngine);

        //set topic message flusher
        TopicMessageDeliveryStrategy topicMessageDeliveryStrategy = AndesConfigurationManager.readValue
                (AndesConfiguration.PERFORMANCE_TUNING_TOPIC_MESSAGE_DELIVERY_STRATEGY);
        if (topicMessageDeliveryStrategy.equals(TopicMessageDeliveryStrategy.DISCARD_ALLOWED)
                || topicMessageDeliveryStrategy.equals(TopicMessageDeliveryStrategy.DISCARD_NONE)) {
            this.topicMessageFlusher = new NoLossBurstTopicMessageDeliveryImpl(subscriptionEngine);
        } else if (topicMessageDeliveryStrategy.equals(TopicMessageDeliveryStrategy.SLOWEST_SUB_RATE)) {
            this.topicMessageFlusher = new SlowestSubscriberTopicMessageDeliveryImpl(subscriptionEngine);
        }

        initializeDeliveryRules();

    }

    public Integer getMaxNumberOfReadButUndeliveredMessages() {
        return maxNumberOfReadButUndeliveredMessages;
    }

    /**
     * Initialize common delivery rules. These delivery rules apply
     * irrespective of the protocol
     */
    private void initializeDeliveryRules() {

/*        // NOTE: Feature Message Expiration moved to a future release
        //checking message expiration deliver rule
        deliveryRulesList.add(new MessageExpiredRule());*/

        //checking message purged delivery rule
        deliveryRuleList.add(new MessagePurgeRule());
    }

    /**
     * Evaluating Delivery rules before sending the messages
     *
     * @param message         AMQ Message
     * @param protocolType    The protocol type of the message
     * @param destinationType The destination type of the message
     * @return IsOKToDelivery
     * @throws AndesException
     */
    private boolean evaluateDeliveryRules(DeliverableAndesMetadata message, ProtocolType protocolType,
                                          DestinationType destinationType) throws AndesException {
        boolean isOKToDelivery = true;

        for (CommonDeliveryRule rule : deliveryRuleList) {
            if (!rule.evaluate(message, protocolType, destinationType)) {
                isOKToDelivery = false;
                break;
            }
        }
        return isOKToDelivery;
    }

    /**
     * Get the next subscription for the given destination. If at end of the subscriptions, it circles
     * around to the first one
     *
     * @param messageDeliveryInfo The message delivery information object
     * @param subscriptions4Queue subscriptions registered for the destination
     * @return subscription to deliver
     * @throws AndesException
     */
    public LocalSubscription findNextSubscriptionToSent(MessageDeliveryInfo messageDeliveryInfo,
                                                        Collection<LocalSubscription> subscriptions4Queue)
            throws AndesException {
        LocalSubscription localSubscription = null;
        boolean isValidLocalSubscription = false;
        if (subscriptions4Queue == null || subscriptions4Queue.size() == 0) {
            subscriptionCursar4QueueMap.get(messageDeliveryInfo.getDestinationType()).remove(
                    messageDeliveryInfo.getDestination());
            return null;
        }

        Iterator<LocalSubscription> it = messageDeliveryInfo.getIterator();
        while (it.hasNext()) {
            localSubscription = it.next();
            if (subscriptions4Queue.contains(localSubscription)) {
                isValidLocalSubscription = true;

                // We have to iterate through the collection to find the matching the local subscription since
                // the Collection does not have a get method
                for (LocalSubscription subscription : subscriptions4Queue) {
                    // Assign the matching object reference from subscriptions4Queue collection
                    // to local subscription variable
                    if (subscription.equals(localSubscription)) {
                        localSubscription = subscription;
                        break;
                    }
                }
                break;
            }
        }
        if (isValidLocalSubscription) {
            return localSubscription;
        } else {
            it = subscriptions4Queue.iterator();
            messageDeliveryInfo.setIterator(it);
            if (it.hasNext()) {
                return it.next();
            } else {
                return null;
            }
        }
    }


    /**
     * Will allow retrieval of information related to delivery of the message
     *
     * @param destination     where the message should be delivered to
     * @param protocolType    The protocol which the destination belongs to
     * @param destinationType The type of the destination
     * @return the information which holds of the message which should be delivered
     * @throws AndesException
     */
    public MessageDeliveryInfo getMessageDeliveryInfo(String destination, ProtocolType protocolType,
                                                      DestinationType destinationType) throws AndesException {

        Map<String, MessageDeliveryInfo> infoMap = subscriptionCursar4QueueMap.get(destinationType);


        if (null == infoMap) {
            infoMap = new HashMap<>();
        }

        MessageDeliveryInfo messageDeliveryInfo = infoMap.get(destination);

        if (messageDeliveryInfo == null) {
            messageDeliveryInfo = new MessageDeliveryInfo(this);
            messageDeliveryInfo.setDestination(destination);
            Collection<LocalSubscription> localSubscribersForQueue = subscriptionEngine.getActiveLocalSubscribers(
                    destination, protocolType, destinationType);

            messageDeliveryInfo.setIterator(localSubscribersForQueue.iterator());

            messageDeliveryInfo.setProtocolType(protocolType);
            messageDeliveryInfo.setDestinationType(destinationType);

            infoMap.put(destination, messageDeliveryInfo);

            subscriptionCursar4QueueMap.put(destinationType, infoMap);
        }
        return messageDeliveryInfo;
    }

    /**
     * send the messages to deliver
     *
     * @param messagesRead        AndesMetadata list
     * @param slot                these messages are belonged to
     * @param messageDeliveryInfo The delivery information object for messages
     */
    public void sendMessageToBuffer(List<DeliverableAndesMetadata> messagesRead, Slot slot
            , MessageDeliveryInfo messageDeliveryInfo) {
        try {
            slot.incrementPendingMessageCount(messagesRead.size());
            for (DeliverableAndesMetadata message : messagesRead) {
                messageDeliveryInfo.bufferMessage(message);
            }
        } catch (Throwable e) {
            log.fatal("Error scheduling messages for delivery", e);
        }
    }

    /**
     * Send the messages to deliver
     *
     * @param destination     message destination
     * @param protocolType    The protocol which the destination belongs to
     * @param destinationType The type of the destination
     * @param messages        message to add
     */
    public void addAlreadyTrackedMessagesToBuffer(String destination, ProtocolType protocolType,
                                                  DestinationType destinationType,
                                                  List<DeliverableAndesMetadata> messages) {
        try {
            MessageDeliveryInfo messageDeliveryInfo =
                    getMessageDeliveryInfo(destination, protocolType, destinationType);
            for (DeliverableAndesMetadata metadata : messages) {
                messageDeliveryInfo.reBufferMessage(metadata);
            }
        } catch (AndesException e) {
            log.fatal("Error scheduling messages for delivery", e);
        }
    }

    /**
     * Read messages from the buffer and send messages to subscribers.
     *
     * @param messageDeliveryInfo The delivery information object
     * @param storageQueue        Storage Queue related to destination of messages
     */
    public boolean sendMessagesInBuffer(MessageDeliveryInfo messageDeliveryInfo, String storageQueue) throws
                                                                                                      AndesException {

        boolean sentFromBuffer = false;

        if (!messageDeliveryInfo.isMessageBufferEmpty()) {
            /**
             * Now messages are read to the memory. Send the read messages to subscriptions
             */
            if (log.isDebugEnabled()) {
                log.debug("Sending " + messageDeliveryInfo.getSizeOfMessageBuffer() + " messages from buffer "
                                  + " for destination : " + messageDeliveryInfo.getDestination());

                for (Map.Entry<DestinationType, Map<String, MessageDeliveryInfo>> infoMap :
                        subscriptionCursar4QueueMap.entrySet()) {

                    for (Map.Entry<String, MessageDeliveryInfo> entry : infoMap.getValue().entrySet()) {
                        log.debug("Queue size of destination " + entry.getKey() + " is :"
                                          + entry.getValue().getSizeOfMessageBuffer());
                    }
                }
            }

            sendMessagesToSubscriptions(messageDeliveryInfo, storageQueue);
            sentFromBuffer = true;
        }

        return sentFromBuffer;
    }

    /**
     * Check whether there are active subscribers and send
     *
     * @param messageDeliveryInfo The delivery information object of the messages
     * @param storageQueue        storage queue of messages
     * @return how many messages sent
     * @throws AndesException
     */
    public int sendMessagesToSubscriptions(MessageDeliveryInfo messageDeliveryInfo, String storageQueue)
            throws AndesException {

        int noOfSentMessages;
        if (DestinationType.TOPIC == messageDeliveryInfo.getDestinationType()) {
            noOfSentMessages = topicMessageFlusher.deliverMessageToSubscriptions(messageDeliveryInfo, storageQueue);
        } else {
            noOfSentMessages = queueMessageFlusher.deliverMessageToSubscriptions(messageDeliveryInfo, storageQueue);
        }

        return noOfSentMessages;
    }

    /**
     * Clear up all the buffered messages for delivery
     *
     * @param destination     destination of messages to delete
     * @param destinationType the destination type of the messages
     */
    public void clearUpAllBufferedMessagesForDelivery(String destination, DestinationType destinationType) {
        subscriptionCursar4QueueMap.get(destinationType).get(destination).clearReadButUndeliveredMessages();
    }

    /**
     * Schedule to deliver message for the subscription
     *
     * @param subscription subscription to send
     * @param message      message to send
     */
    public void scheduleMessageForSubscription(LocalSubscription subscription,
                                               final DeliverableAndesMetadata message) throws AndesException {
        deliverMessageAsynchronously(subscription, message);
    }

    /**
     * Submit the messages to a thread pool to deliver asynchronously
     *
     * @param subscription local subscription
     * @param message      metadata of the message
     */
    public void deliverMessageAsynchronously(LocalSubscription subscription, DeliverableAndesMetadata message)
            throws AndesException {

        if (evaluateDeliveryRules(message, subscription.getProtocolType(), subscription.getDestinationType())) {
            if (log.isDebugEnabled()) {
                log.debug(
                        "Scheduled message id= " + message.getMessageId() + " to be sent to subscription= " +
                                subscription);
            }
            //mark message as came into the subscription for deliver

            message.markAsDispatchedToDeliver(subscription.getChannelID());

            ProtocolMessage protocolMessage = message.generateProtocolDeliverableMessage(subscription.getChannelID());
            flusherExecutor.submit(subscription, protocolMessage);
        } else {
            log.warn("Common delivery rules failed for message id " + message.getMessageId()
                             + " Hence not delivered.");
        }
    }

    /**
     * Re-queue message to andes core. This message will be delivered to
     * any eligible subscriber to receive later. in multiple subscription case this
     * can cause message duplication.
     *
     * @param message         message to reschedule
     * @param destinationType the destination type of the messages
     */
    public void reQueueMessage(DeliverableAndesMetadata message, DestinationType destinationType) {
        String destination = message.getDestination();
        subscriptionCursar4QueueMap.get(destinationType).get(destination).reBufferMessage(message);
    }

    public static MessageFlusher getInstance() {
        return messageFlusher;
    }

    public DisruptorBasedFlusher getFlusherExecutor() {
        return flusherExecutor;
    }
}
