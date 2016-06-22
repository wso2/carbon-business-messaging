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

package org.wso2.carbon.andes.core.util;

import org.apache.commons.lang.StringUtils;
import org.wso2.carbon.andes.core.Andes;
import org.wso2.carbon.andes.core.AndesException;
import org.wso2.carbon.andes.core.AndesMessage;
import org.wso2.carbon.andes.core.AndesQueue;
import org.wso2.carbon.andes.core.AndesSubscription;
import org.wso2.carbon.andes.core.ProtocolType;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.management.openmbean.ArrayType;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;

/**
 * Composite data helper to convert objects into {@link CompositeType} format for MBeans.
 */
public class CompositeDataHelper {

    /**
     * Composite data helper inner class for destinations.
     */
    public class DestinationCompositeDataHelper {
        public static final String DESTINATION_NAME = "DESTINATION_NAME";
        public static final String DESTINATION_OWNER = "DESTINATION_OWNER";
        public static final String IS_DURABLE = "IS_DURABLE";
        public static final String SUBSCRIPTION_COUNT = "SUBSCRIPTION_COUNT";
        public static final String MESSAGE_COUNT = "MESSAGE_COUNT";
        public static final String PROTOCOL_TYPE = "PROTOCOL_TYPE";
        public static final String DESTINATION_TYPE = "DESTINATION_TYPE";
        private final  List<String> destinationCompositeItemNames = Collections.unmodifiableList(Arrays.asList
                (DESTINATION_NAME, DESTINATION_OWNER, IS_DURABLE, SUBSCRIPTION_COUNT, MESSAGE_COUNT, PROTOCOL_TYPE,
                 DESTINATION_TYPE));

        private OpenType[] destinationAttributeTypes = new OpenType[7];
        private CompositeType destinationCompositeType = null;

        /**
         * Converts an {@link AndesQueue} and the message count to a {@link CompositeDataSupport} object.
         *
         * @param destination  The {@link AndesQueue} object representing the destination.
         * @param messageCount The message count for the represented destination.
         * @return A {@link CompositeDataSupport} object. This can be casted to a {@link
         * javax.management.openmbean.CompositeData}.
         * @throws OpenDataException
         */
        public CompositeDataSupport getDestinationAsCompositeData(AndesQueue destination, long messageCount)
                throws OpenDataException {

            setDestinationCompositeType();

            return new CompositeDataSupport(
                    destinationCompositeType, destinationCompositeItemNames.toArray(
                    new String[destinationCompositeItemNames.size()]),
                    getDestinationItemValue(destination, messageCount));
        }

        /**
         * Gets an object array that includes the properties of the destination along with the message count.
         *
         * @param destination  The {@link AndesQueue} object representing the destination.
         * @param messageCount The message count for the represented destination.
         * @return An object array with the properties of the destination.
         */
        private Object[] getDestinationItemValue(AndesQueue destination, long messageCount) {
            return new Object[]{
                    destination.queueName,
                    destination.queueOwner,
                    destination.isDurable,
                    destination.subscriptionCount,
                    messageCount,
                    destination.getProtocolType().getProtocolName(),
                    destination.getDestinationType().name()
            };
        }

        /**
         * Initializes the composite type for destinations to support Composite Data
         *
         * @throws OpenDataException
         */
        private void setDestinationCompositeType() throws OpenDataException {
            destinationAttributeTypes[0] = SimpleType.STRING; // Destination Name
            destinationAttributeTypes[1] = SimpleType.STRING; // Destination Owner
            destinationAttributeTypes[2] = SimpleType.BOOLEAN; // Is Durable
            destinationAttributeTypes[3] = SimpleType.INTEGER; // Subscription Count
            destinationAttributeTypes[4] = SimpleType.LONG; // Message Count
            destinationAttributeTypes[5] = SimpleType.STRING; // Protocol Type
            destinationAttributeTypes[6] = SimpleType.STRING; // Destination Type
            destinationCompositeType = new CompositeType("Destination", "Destination details",
                                                         destinationCompositeItemNames.toArray(
                                                                 new String[destinationCompositeItemNames.size()]),
                                                         destinationCompositeItemNames.toArray(
                                                                 new String[destinationCompositeItemNames.size()]),
                                                         destinationAttributeTypes);
        }
    }

    /**
     * Composite data helper inner class for subscriptions.
     */
    public class SubscriptionCompositeDataHelper {
        public static final String SUBSCRIPTION_ID = "SUBSCRIPTION_ID";
        public static final String DESTINATION_NAME = "DESTINATION_NAME";
        public static final String TARGET_QUEUE_BOUND_EXCHANGE_NAME = "TARGET_QUEUE_BOUND_EXCHANGE_NAME";
        public static final String TARGET_QUEUE = "TARGET_QUEUE";
        public static final String IS_DURABLE = "IS_DURABLE";
        public static final String HAS_EXTERNAL_SUBSCRIPTIONS = "HAS_EXTERNAL_SUBSCRIPTIONS";
        public static final String PENDING_MESSAGE_COUNT = "PENDING_MESSAGE_COUNT";
        public static final String SUBSCRIBED_NODE = "SUBSCRIBED_NODE";
        public static final String PROTOCOL_TYPE = "PROTOCOL_TYPE";
        public static final String DESTINATION_TYPE = "DESTINATION_TYPE";
        private final List<String> subscriptionCompositeItemNames = Collections.unmodifiableList(Arrays.asList
                (SUBSCRIPTION_ID, DESTINATION_NAME, TARGET_QUEUE_BOUND_EXCHANGE_NAME, TARGET_QUEUE, IS_DURABLE,
                 HAS_EXTERNAL_SUBSCRIPTIONS, PENDING_MESSAGE_COUNT, SUBSCRIBED_NODE, PROTOCOL_TYPE,
                 DESTINATION_TYPE));

        private OpenType[] subscriptionAttributeTypes = new OpenType[10];
        private CompositeType subscriptionCompositeType = null;

        /**
         * Converts an {@link AndesSubscription} and its pending message count to a {@link CompositeDataSupport}
         * object.
         *
         * @param subscription        The {@link AndesSubscription} object.
         * @param pendingMessageCount The pending messages for the specific subscription.
         * @return A {@link CompositeDataSupport} object. This can be casted to a {@link
         * javax.management.openmbean.CompositeData}.
         * @throws OpenDataException
         */
        public CompositeDataSupport getSubscriptionAsCompositeData(AndesSubscription subscription, long
                pendingMessageCount) throws OpenDataException {
            setSubscriptionCompositeType();

            return new CompositeDataSupport(subscriptionCompositeType,
                                            subscriptionCompositeItemNames.toArray(
                                                    new String[subscriptionCompositeItemNames.size()]),
                                            getSubscriptionItemValue(subscription, pendingMessageCount));
        }

        /**
         * Gets an object array that includes the properties of the subscription along with the pending message count.
         *
         * @param subscription        The {@link AndesSubscription} object representing the subscription.
         * @param pendingMessageCount The pending message count for the represented subscription.
         * @return An object array with the properties of the subscription.
         */
        private Object[] getSubscriptionItemValue(AndesSubscription subscription, long pendingMessageCount) {
            return new Object[]{
                    subscription.getSubscriptionID(),
                    subscription.getSubscribedDestination(),
                    subscription.getTargetQueueBoundExchangeName(),
                    subscription.getTargetQueue(),
                    subscription.isDurable(),
                    subscription.hasExternalSubscriptions(),
                    pendingMessageCount,
                    subscription.getSubscribedNode(),
                    subscription.getProtocolType().getProtocolName(),
                    subscription.getDestinationType().name()
            };
        }

        /**
         * Initializes the composite type for subscription to support Composite Data.
         *
         * @throws OpenDataException
         */
        private void setSubscriptionCompositeType() throws OpenDataException {
            subscriptionAttributeTypes[0] = SimpleType.STRING; // SubscriptionID
            subscriptionAttributeTypes[1] = SimpleType.STRING; // SubscribedDestination
            subscriptionAttributeTypes[2] = SimpleType.STRING; // TargetQueueBoundExchangeName
            subscriptionAttributeTypes[3] = SimpleType.STRING; // TargetQueue
            subscriptionAttributeTypes[4] = SimpleType.BOOLEAN; // Durable
            subscriptionAttributeTypes[5] = SimpleType.BOOLEAN; // ExternalSubscriptions
            subscriptionAttributeTypes[6] = SimpleType.LONG; // Pending Message Count
            subscriptionAttributeTypes[7] = SimpleType.STRING; // SubscribedNode
            subscriptionAttributeTypes[8] = SimpleType.STRING; // ProtocolType
            subscriptionAttributeTypes[9] = SimpleType.STRING; // DestinationType
            subscriptionCompositeType = new CompositeType("Subscription", "Subscription details",
                                                          subscriptionCompositeItemNames.toArray(
                                                                  new String[subscriptionCompositeItemNames.size()]),
                                                          subscriptionCompositeItemNames.toArray(
                                                                  new String[subscriptionCompositeItemNames.size()]),
                                                          subscriptionAttributeTypes);
        }
    }

    /**
     * Composite data helper inner class for messages.
     */
    public class MessagesCompositeDataHelper {
        public static final String ANDES_METADATA_MESSAGE_ID = "ANDES_METADATA_MESSAGE_ID";
        public static final String DESTINATION_NAME = "DESTINATION_NAME";
        public static final String MESSAGE_PROPERTIES = "MESSAGE_PROPERTIES";
        public static final String MESSAGE_CONTENT = "MESSAGE_CONTENT";
        private final List<String> messageCompositeItemNames = Collections.unmodifiableList(Arrays.asList
                (ANDES_METADATA_MESSAGE_ID, DESTINATION_NAME, MESSAGE_PROPERTIES, MESSAGE_CONTENT));

        private OpenType[] messageAttributeTypes = new OpenType[5];
        private CompositeType messageCompositeType = null;

        /**
         * Converts an {@link org.wso2.carbon.andes.core.AndesMessageMetadata} and its pending message
         * count to a {@link CompositeDataSupport} object.
         *
         * @param protocolType      The protocol for the message.
         * @param andesMessage      The {@link org.wso2.carbon.andes.core.AndesMessageMetadata}
         *                          object representing the message.
         * @param getMessageContent Whether to return content or not.
         * @return A {@link CompositeDataSupport} object. This can be casted to a {@link
         * javax.management.openmbean.CompositeData}.
         * @throws OpenDataException
         */
        public CompositeDataSupport getMessageAsCompositeData(
                ProtocolType protocolType,
                AndesMessage andesMessage,
                boolean getMessageContent) throws OpenDataException,
                                                  AndesException {
            setMessageCompositeType();

            return new CompositeDataSupport(messageCompositeType,
                                            messageCompositeItemNames.toArray(
                                                    new String[messageCompositeItemNames.size()]),
                                            getMessageItemValue(protocolType, andesMessage, getMessageContent));
        }

        /**
         * Gets an object array that includes the properties of the message along with the pending message count.
         *
         * @param protocolType      The protocol for the message.
         * @param andesMessage      The {@link org.wso2.carbon.andes.core.AndesMessageMetadata}
         *                          object representing the message.
         * @param getMessageContent Whether to return content or not.
         * @return An object array with the properties of the message.
         */
        private Object[] getMessageItemValue(ProtocolType protocolType, AndesMessage andesMessage, boolean
                getMessageContent) throws AndesException {
            Map<String, String> properties = Andes.getInstance().getAndesResourceManager().getMessageDecoder
                    (protocolType).getMessageProperties(andesMessage.getMetadata());

            // Converting map to array
            String[][] propertiesArray = new String[properties.size()][2];
            int count = 0;
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                propertiesArray[count][0] = entry.getKey();
                propertiesArray[count][1] = entry.getValue();
                count++;
            }

            String messageContent = StringUtils.EMPTY;
            if (getMessageContent) {
                messageContent = Andes.getInstance().getAndesResourceManager().getMessageDecoder(protocolType)
                        .getMessageContent(andesMessage);
            }

            return new Object[]{andesMessage.getMetadata().getDestination(), andesMessage.getMetadata()
                    .getDestination(), propertiesArray, messageContent};
        }

        /**
         * Initializes the composite type for a message to support Composite Data.
         *
         * @throws OpenDataException
         */
        private void setMessageCompositeType() throws OpenDataException {
            messageAttributeTypes[0] = SimpleType.LONG; // Andes Metadata Message ID
            messageAttributeTypes[1] = SimpleType.STRING; // Destination
            messageAttributeTypes[2] = new ArrayType(2, SimpleType.STRING); // Message Properties as 2-D array
            messageAttributeTypes[3] = SimpleType.STRING; // Message Content
            messageCompositeType = new CompositeType("Message", "Message details", messageCompositeItemNames
                    .toArray(new String[messageCompositeItemNames.size()]), messageCompositeItemNames.toArray
                    (new String[messageCompositeItemNames.size()]), messageAttributeTypes);
        }
    }
}
