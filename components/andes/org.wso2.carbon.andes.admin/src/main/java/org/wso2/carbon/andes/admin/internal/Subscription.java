/*
 * Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 */

package org.wso2.carbon.andes.admin.internal;

public class Subscription {

    private String subscriptionIdentifier;
    private String subscribedQueueOrTopicName;
    private String subscriberQueueBoundExchange;
    private String subscriberQueueName;
    private boolean isDurable;
    private boolean isActive;
    private int numberOfMessagesRemainingForSubscriber;
    private String subscriberNodeAddress;

    public Subscription(){

    }

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

    public int getNumberOfMessagesRemainingForSubscriber() {
        return numberOfMessagesRemainingForSubscriber;
    }

    public void setNumberOfMessagesRemainingForSubscriber(int numberOfMessagesRemainingForSubscriber) {
        this.numberOfMessagesRemainingForSubscriber = numberOfMessagesRemainingForSubscriber;
    }

    public String getSubscriberQueueBoundExchange() {
        return subscriberQueueBoundExchange;
    }

    public void setSubscriberQueueBoundExchange(String subscriberQueueBoundExchange) {
        this.subscriberQueueBoundExchange = subscriberQueueBoundExchange;
    }
}
