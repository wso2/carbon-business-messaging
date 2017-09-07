/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.andes.admin.mqtt;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.andes.admin.mqtt.internal.AndesBrokerManagerMQTTAdminServiceDSHolder;
import org.wso2.carbon.andes.core.QueueManagerException;
import org.wso2.carbon.andes.core.QueueManagerService;
import org.wso2.carbon.andes.core.SubscriptionManagerException;
import org.wso2.carbon.andes.core.SubscriptionManagerService;
import org.wso2.carbon.andes.core.internal.util.Utils;
import org.wso2.carbon.andes.core.types.MQTTSubscription;
import org.wso2.carbon.andes.core.types.Subscription;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.core.AbstractAdmin;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Provides all the andes MQTT admin services.
 */
public class AndesMQTTAdminService extends AbstractAdmin {

    private static Log log = LogFactory.getLog(AndesMQTTAdminService.class);

    /**
     * Permission value for changing permissions through UI.
     */
    private static final String UI_EXECUTE = "ui.execute";

    /**
     * Permission path for forcibly close subscriptions for topics
     */
    private static final String PERMISSION_ADMIN_MANAGE_TOPIC_SUBSCRIPTION_CLOSE =
            "/permission/admin/manage/subscriptions/topic-close";

    /**
     * Gets the message count for a queue
     * @param destinationName the destination name. the name of the queue or topic.
     * @param msgPattern The exchange type used to transfer messages with the given destinationName.
     *                   e.g. "queue" or "topic"
     * @return the number of messages for a queue
     * @throws BrokerManagerAdminException
     */
    public long getMessageCount(String destinationName, String msgPattern) throws BrokerManagerAdminException {

        long messageCount;
        try {
            QueueManagerService queueManagerService =
                    AndesBrokerManagerMQTTAdminServiceDSHolder.getInstance().getQueueManagerService();
            messageCount = queueManagerService.getMessageCount(destinationName, msgPattern);
            return messageCount;
        } catch (QueueManagerException e) {
            throw new BrokerManagerAdminException("Error while retrieving message count "
                    + "by queue manager service", e);
        }
    }

    /**
     * Delete topic related resources from registry
     * @param topicName Topic Name
     * @param subscriptionId Subscription ID
     * @throws BrokerManagerAdminException
     */
    public void deleteTopicFromRegistry(String topicName, String subscriptionId) throws BrokerManagerAdminException {
        try {
            QueueManagerService queueManagerService =
                    AndesBrokerManagerMQTTAdminServiceDSHolder.getInstance().getQueueManagerService();
            queueManagerService.deleteTopicFromRegistry(topicName, subscriptionId);
        } catch (QueueManagerException e) {
            throw new BrokerManagerAdminException("Error occurred while deleting topic '" + topicName + "' from the " +
                    "registry", e);
        }
    }

    /**
     * Close subscription defined by subscription ID forcibly
     * @param subscriptionID ID of the subscription
     * @param destination queue / topic name of the subscribed destination
     * @param protocolType The protocol type of the subscriptions to close
     * @param destinationType The destination type of the subscriptions to close
     * @throws BrokerManagerAdminException
     */
    public void closeSubscription(String subscriptionID, String destination, String protocolType,
                                  String destinationType) throws BrokerManagerAdminException {
        try {
            SubscriptionManagerService subscriptionManagerService =
                    AndesBrokerManagerMQTTAdminServiceDSHolder.getInstance().getSubscriptionManagerService();
            subscriptionManagerService.closeSubscription(subscriptionID, destination, protocolType, destinationType);
        } catch (SubscriptionManagerException e) {
            throw new BrokerManagerAdminException("Error occurred while closing the subscription '" + destination +"'",
                    e);
        }
    }

    /**
     * Retrieve subscriptions matching the given criteria.
     *
     * @param isDurable Are the subscriptions to be retrieved durable (true/false)
     * @param isActive Are the subscriptions to be retrieved active (true/false/*, * meaning any)
     * @param protocolType The protocol type of the subscriptions to be retrieved
     * @param destinationType The destination type of the subscriptions to be retrieved
     *
     * @return The list of subscriptions matching the given criteria
     * @throws BrokerManagerAdminException
     */
    public Subscription[] getSubscriptions(String isDurable, String isActive, String protocolType,
                                           String destinationType) throws BrokerManagerAdminException {

        List<Subscription> allSubscriptions = new ArrayList<Subscription>();
        Subscription[] subscriptionsDTO;
        try {
            SubscriptionManagerService subscriptionManagerService =
                    AndesBrokerManagerMQTTAdminServiceDSHolder.getInstance().getSubscriptionManagerService();
            List<org.wso2.carbon.andes.core.types.Subscription> subscriptions = subscriptionManagerService
                    .getSubscriptions(isDurable, isActive, protocolType, destinationType);
            subscriptionsDTO = new Subscription[subscriptions.size()];
            for (org.wso2.carbon.andes.core.types.Subscription sub : subscriptions) {
                Subscription subscriptionDTO = new Subscription();
                subscriptionDTO.setSubscriptionIdentifier(sub.getSubscriptionIdentifier());
                subscriptionDTO.setSubscribedQueueOrTopicName(sub.getSubscribedQueueOrTopicName());
                subscriptionDTO.setSubscriberQueueBoundExchange(sub.getSubscriberQueueBoundExchange());
                subscriptionDTO.setSubscriberQueueName(sub.getSubscriberQueueName());
                subscriptionDTO.setDurable(sub.isDurable());
                subscriptionDTO.setActive(sub.isActive());
                subscriptionDTO.setNumberOfMessagesRemainingForSubscriber(sub.getNumberOfMessagesRemainingForSubscriber());
                subscriptionDTO.setConnectedNodeAddress(sub.getConnectedNodeAddress());
                subscriptionDTO.setProtocolType(sub.getProtocolType());
                subscriptionDTO.setDestinationType(sub.getDestinationType());
                subscriptionDTO.setOriginHostAddress(sub.getOriginHostAddress());

                allSubscriptions.add(subscriptionDTO);
            }
            CustomSubscriptionComparator comparator = new CustomSubscriptionComparator();
            Collections.sort(allSubscriptions, Collections.reverseOrder(comparator));
            allSubscriptions.toArray(subscriptionsDTO);
        } catch (SubscriptionManagerException e) {
            throw new BrokerManagerAdminException("Error occurred while retrieving subscriptions", e);
        }
        return subscriptionsDTO;
    }

    /**
     * Retrieve subscriptions matching to the given search criteria.
     *
     * @param subscription  is the the details of subscription object
     * @param tenantDomain  is the Domain of a particular tenant
     * @return retruns if a list of subscriptions which match to the search criteria, else returns null
     * @throws BrokerManagerAdminException throws when an error occurs
     */
    public Subscription[] getFilteredSubscriptions(MQTTSubscription subscription, String tenantDomain)
            throws BrokerManagerAdminException {
        List<Subscription> allSubscriptions = new ArrayList<>();
        Subscription[] subscriptionsDTO;

        try {
            PrivilegedCarbonContext.startTenantFlow();
            PrivilegedCarbonContext privilegedCarbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
            privilegedCarbonContext.setTenantDomain(tenantDomain);
            SubscriptionManagerService subscriptionManagerService =
                    AndesBrokerManagerMQTTAdminServiceDSHolder.getInstance().getSubscriptionManagerService();
            List<org.wso2.carbon.andes.core.types.Subscription> subscriptions = subscriptionManagerService
                    .getFilteredMQTTSubscriptions(subscription, tenantDomain);
            subscriptionsDTO = new Subscription[subscriptions.size()];
            for (org.wso2.carbon.andes.core.types.Subscription sub : subscriptions) {
                Subscription subscriptionDTO = new Subscription();
                subscriptionDTO.setSubscriptionIdentifier(sub.getSubscriptionIdentifier());
                subscriptionDTO.setSubscribedQueueOrTopicName(sub.getSubscribedQueueOrTopicName());
                subscriptionDTO.setSubscriberQueueBoundExchange(sub.getSubscriberQueueBoundExchange());
                subscriptionDTO.setSubscriberQueueName(sub.getSubscriberQueueName());
                subscriptionDTO.setDurable(sub.isDurable());
                subscriptionDTO.setActive(sub.isActive());
                subscriptionDTO.setNumberOfMessagesRemainingForSubscriber(sub
                        .getNumberOfMessagesRemainingForSubscriber());
                subscriptionDTO.setConnectedNodeAddress(sub.getConnectedNodeAddress());
                subscriptionDTO.setProtocolType(sub.getProtocolType());
                subscriptionDTO.setDestinationType(sub.getDestinationType());
                subscriptionDTO.setOriginHostAddress(sub.getOriginHostAddress());
                allSubscriptions.add(subscriptionDTO);
            }
            CustomSubscriptionComparator comparator = new CustomSubscriptionComparator();
            Collections.sort(allSubscriptions, Collections.reverseOrder(comparator));
            allSubscriptions.toArray(subscriptionsDTO);
        } catch (SubscriptionManagerException e) {
            throw new BrokerManagerAdminException("Error occurred while retrieving subscriptions for the tenant '" +
                    tenantDomain + "'", e);
        }
        return subscriptionsDTO;
    }

    /**
     * Returns the total subscription count relevant to a particular search criteria.
     *
     * @param subscription is the the details of subscription object
     * @return total subscription count matching to the given criteria
     * @throws BrokerManagerAdminException hrows when an error occurs
     */
    public int getTotalSubscriptionCountForSearchResult(MQTTSubscription subscription) throws
            BrokerManagerAdminException {
        int subscriptionCountForSearchResult = 0;
        try {
            SubscriptionManagerService subscriptionManagerService =
                    AndesBrokerManagerMQTTAdminServiceDSHolder.getInstance().getSubscriptionManagerService();
            subscriptionCountForSearchResult = subscriptionManagerService
                    .getTotalSubscriptionCountForSearchResult(
                            subscription.isDurable(),
                            subscription.isActive(),
                            subscription.getProtocolType(),
                            subscription.getDestinationType(),
                            subscription.getFilteredNamePattern(),
                            subscription.isFilteredNameByExactMatch(),
                            subscription.getIdentifierPattern(),
                            subscription.isIdentifierPatternByExactMatch(),
                            subscription.getOwnNodeId());

        } catch (SubscriptionManagerException e) {
            throw new BrokerManagerAdminException("Error occurred while retrieving total count of subscriptions", e);
        }
        return subscriptionCountForSearchResult;
    }

    /**
     * Evaluate current logged in user has close subscription permission for topic subscriptions. This service mainly
     * used to restrict UI
     * control for un-authorize users
     * @return true/false based on permission
     * @throws BrokerManagerAdminException
     */
    public boolean checkCurrentUserHasTopicSubscriptionClosePermission() throws BrokerManagerAdminException {
        boolean hasPermission = false;
        String username = getCurrentUser();
        try {
            if (Utils.isAdmin(username)) {
                hasPermission = true;
            } else if (CarbonContext.getThreadLocalCarbonContext().getUserRealm().getAuthorizationManager()
                    .isUserAuthorized(username, PERMISSION_ADMIN_MANAGE_TOPIC_SUBSCRIPTION_CLOSE, UI_EXECUTE)) {
                hasPermission = true;
            }
        } catch (UserStoreException | QueueManagerException e) {
            String errorMessage = e.getMessage();
            log.error(errorMessage, e);
            throw new BrokerManagerAdminException(errorMessage, e);
        }
        return hasPermission;
    }

    /**
     * Get current user's username.
     * @return The user name.
     */
    private String getCurrentUser() {
        String userName;
        if (CarbonContext.getThreadLocalCarbonContext().getTenantId() > MultitenantConstants.INVALID_TENANT_ID) {
            userName = CarbonContext.getThreadLocalCarbonContext().getUsername() + "!"
                    + CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        } else {
            userName = CarbonContext.getThreadLocalCarbonContext().getUsername();
        }
        return userName.trim();
    }

    /**
     * A comparator class to order subscriptions.
     */
    public class CustomSubscriptionComparator implements Comparator<Subscription> {

        /**
         * {@inheritDoc}
         */
        @Override
        public int compare(Subscription sub1, Subscription sub2) {
            return sub1.getNumberOfMessagesRemainingForSubscriber() - sub2.getNumberOfMessagesRemainingForSubscriber();
        }
    }
}
