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
import org.wso2.carbon.andes.core.subscription.LocalSubscription;
import org.wso2.carbon.andes.core.subscription.SubscriptionEngine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * This class implements topic message delivery. This implementation will deliver every message
 * to every subscriber without any message loss or without considering overflowing memory.
 */
public class NoLossBurstTopicMessageDeliveryImpl implements MessageDeliveryStrategy {

    private static Log log = LogFactory.getLog(NoLossBurstTopicMessageDeliveryImpl.class);
    private SubscriptionEngine subscriptionEngine;

    public NoLossBurstTopicMessageDeliveryImpl(SubscriptionEngine subscriptionEngine) {
        this.subscriptionEngine = subscriptionEngine;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int deliverMessageToSubscriptions(MessageDeliveryInfo messageDeliveryInfo, String storageQueueName)
            throws AndesException {

        Collection<DeliverableAndesMetadata> messages = messageDeliveryInfo.getReadButUndeliveredMessages();
        int sentMessageCount = 0;
        Iterator<DeliverableAndesMetadata> iterator = messages.iterator();
        List<DeliverableAndesMetadata> droppedTopicMessagesList = new ArrayList<>();

        /**
         * get all relevant type of subscriptions. This call does NOT
         * return hierarchical subscriptions for the destination. There
         * are duplicated messages for each different subscribed destination.
         * For durable topic subscriptions this should return queue subscription
         * bound to unique queue based on subscription id
         */
        Collection<LocalSubscription> subscriptions =
                subscriptionEngine.getActiveLocalSubscribers(messageDeliveryInfo.getDestination(),
                                                             messageDeliveryInfo.getProtocolType(),
                                                             messageDeliveryInfo.getDestinationType());

        while (iterator.hasNext()) {

            try {
                DeliverableAndesMetadata message = iterator.next();

                List<LocalSubscription> subscriptionsToDeliver = new ArrayList<>();


                //All subscription filtering logic for topics goes here
                for (LocalSubscription subscription : subscriptions) {
                    /*
                     * Consider the arrival time of the message. Only topic
                     * subscribers which appeared before publishing this message should receive it
                     */
                    if ((subscription.getSubscribeTime() > message.getArrivalTime())
                            || !subscription.getSubscribedDestination().equals(messageDeliveryInfo.getDestination())) {
                        // In wild cards, there can be others subscribers here as well
                        continue;
                    }

                    // Avoid sending if the selector of subscriber does not match
                    if (!subscription.isMessageAcceptedBySelector(message)) {
                        continue;
                    }

                    subscriptionsToDeliver.add(subscription);
                }

                if (subscriptionsToDeliver.size() == 0) {
                    iterator.remove(); // remove buffer
                    droppedTopicMessagesList.add(message);

                    continue; // skip this iteration if no subscriptions for the message
                }

                boolean allTopicSubscriptionsSaturated = true;
                for (LocalSubscription subscription : subscriptionsToDeliver) {
                    if (subscription.hasRoomToAcceptMessages()) {
                        allTopicSubscriptionsSaturated = false;
                        break;
                    }
                }

                /**
                 * if all topic subscriptions are saturated we skip sending. This is to prevent protocol buffers
                 * overfilling. These messages will be tried in order in next buff flush.
                 */
                if (allTopicSubscriptionsSaturated) {
                    break;
                }

                message.markAsScheduledToDeliver(subscriptionsToDeliver);

                for (LocalSubscription localSubscription : subscriptionsToDeliver) {
                    MessageFlusher.getInstance().deliverMessageAsynchronously(localSubscription, message);
                }

                iterator.remove();

                if (log.isDebugEnabled()) {
                    log.debug("Removing Scheduled to send message from buffer. MsgId= " + message.getMessageId());
                }

                sentMessageCount++;


            } catch (NoSuchElementException ex) {
                // This exception can occur because the iterator of ConcurrentSkipListSet loads the
                // at-the-time snapshot. Some records could be deleted by the time the iterator reaches them.
                // However, this can only happen at the tail of the collection, not in middle, and it would
                // cause the loop to blindly check for a batch of deleted records.
                // Given this situation, this loop should break so the sendFlusher can re-trigger it.
                // for tracing purposes can use this : log.warn("NoSuchElementException thrown",ex);
                log.warn("NoSuchElementException thrown. ", ex);
                break;
            }
        }


        /*
         * Here we do not need to have orphaned slot scenario (return slot). If there are no subscribers
         * slot will be consumed and metadata will be removed. We duplicate topic messages per node
         */


        /*
         * delete topic messages that were dropped due to no subscriptions
         * for the message and due to has no room to enqueue the message.
         */
        MessagingEngine.getInstance().deleteMessages(droppedTopicMessagesList);

        return sentMessageCount;
    }
}
