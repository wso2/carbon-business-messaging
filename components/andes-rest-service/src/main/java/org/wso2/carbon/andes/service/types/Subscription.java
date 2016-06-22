/*
 * Copyright (c) 2016, WSO2 Inc. (http://wso2.com) All Rights Reserved.
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

package org.wso2.carbon.andes.service.types;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/**
 * Representation class a subscription.
 */
@ApiModel(value = "Subscription", description = "The structure for a subscription.")
public class Subscription {
    @ApiModelProperty(value = "The unique identifier for a subscription.")
    private String subscriptionIdentifier;
    @ApiModelProperty(value = "Routing key of the binding to whom subscription is made.")
    private String subscribedQueueOrTopicName;
    @ApiModelProperty(value = "Exchange subscribed queue is bound.")
    private String subscriberQueueBoundExchange;
    @ApiModelProperty(value = "Subscribed queue name.")
    private String subscriberQueueName;
    @ApiModelProperty(value = ".")
    private String destination;
    @ApiModelProperty(value = "Whether subscribed to a durable queue/binding.")
    private boolean isDurable;
    @ApiModelProperty(value = "Whether the subscription is active or not..")
    private boolean isActive;
    @ApiModelProperty(value = "The number of messages to be delivered to the subscription.")
    private long numberOfMessagesRemainingForSubscriber;
    @ApiModelProperty(value = "The node ID which the subscription is in.")
    private String subscriberNodeAddress;
    @ApiModelProperty(value = "The protocol type for which the message belongs to.")
    private String protocolType;
    @ApiModelProperty(value = "The destination type for which the message belongs to.")
    private String destinationType;

    public String getSubscriptionIdentifier() {
        return subscriptionIdentifier;
    }

    public void setSubscriptionIdentifier(String subscriptionIdentifier) {
        this.subscriptionIdentifier = subscriptionIdentifier;
    }

    public String getSubscriberNodeAddress() {
        return subscriberNodeAddress;
    }

    public void setSubscriberNodeAddress(String subscriberNodeAddress) {
        this.subscriberNodeAddress = subscriberNodeAddress;
    }

    public String getSubscribedQueueOrTopicName() {
        return subscribedQueueOrTopicName;
    }

    public void setSubscribedQueueOrTopicName(String subscribedQueueOrTopicName) {
        this.subscribedQueueOrTopicName = subscribedQueueOrTopicName;
    }

    public String getSubscriberQueueName() {
        return subscriberQueueName;
    }

    public void setSubscriberQueueName(String subscriberQueueName) {
        this.subscriberQueueName = subscriberQueueName;
    }

    public boolean isDurable() {
        return isDurable;
    }

    public void setDurable(boolean durable) {
        isDurable = durable;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public long getNumberOfMessagesRemainingForSubscriber() {
        return numberOfMessagesRemainingForSubscriber;
    }

    public void setNumberOfMessagesRemainingForSubscriber(long numberOfMessagesRemainingForSubscriber) {
        this.numberOfMessagesRemainingForSubscriber = numberOfMessagesRemainingForSubscriber;
    }

    public String getSubscriberQueueBoundExchange() {
        return subscriberQueueBoundExchange;
    }

    public void setSubscriberQueueBoundExchange(String subscriberQueueBoundExchange) {
        this.subscriberQueueBoundExchange = subscriberQueueBoundExchange;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getProtocolType() {
        return protocolType;
    }

    public void setProtocolType(String protocolType) {
        this.protocolType = protocolType;
    }

    public String getDestinationType() {
        return destinationType;
    }

    public void setDestinationType(String destinationType) {
        this.destinationType = destinationType;
    }
}
