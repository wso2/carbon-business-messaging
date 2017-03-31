/*
 * Copyright 2004,2015 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.andes.event.admin;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.andes.event.admin.exception.EventAdminException;
import org.wso2.carbon.andes.event.admin.internal.EventAdminHolder;
import org.wso2.carbon.andes.event.core.EventBroker;
import org.wso2.carbon.andes.event.core.Message;
import org.wso2.carbon.andes.event.core.TopicManagerService;
import org.wso2.carbon.andes.event.core.TopicNode;
import org.wso2.carbon.andes.event.core.TopicRolePermission;
import org.wso2.carbon.andes.event.core.exception.EventBrokerException;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.core.AbstractAdmin;

import java.util.Calendar;

/**
 * Provides topic related functions as a web service.
 */
public class AndesEventAdminService extends AbstractAdmin {

    /**
     * Logger to log information, warning, errors.
     */
    private static Log log = LogFactory.getLog(AndesEventAdminService.class);

    /**
     * Gets all the topics
     *
     * @return A topic node
     * @throws EventAdminException
     */
    public TopicNode getAllTopics() throws EventAdminException {
        EventBroker eventBroker = EventAdminHolder.getInstance().getEventBroker();
        try {
            return eventBroker.getTopicManagerService().getTopicTree();
        } catch (EventBrokerException e) {
            String errorMessage = e.getMessage();
            log.error(errorMessage, e);
            throw new EventAdminException(errorMessage, e);
        }
    }

    /**
     * Gets a subset of the topics
     *
     * @param startIndex             start index of the paginated tree
     * @param numberOfTopicsPerRound number of topics should be shown per round
     * @return a topic node which includes a subset of topic tree
     * @throws EventAdminException
     */
    public TopicNode getPaginatedTopicTree(String topicPath, int startIndex, int numberOfTopicsPerRound) throws
            EventAdminException {
        EventBroker eventBroker = EventAdminHolder.getInstance().getEventBroker();
        try {
            return eventBroker.getTopicManagerService().getPaginatedTopicTree(topicPath, startIndex,
                    numberOfTopicsPerRound);
        } catch (EventBrokerException e) {
            String errorMessage = e.getMessage();
            log.error(errorMessage, e);
            throw new EventAdminException(errorMessage, e);
        }
    }



    /**
     * Gets the permission roles for a topic
     * Suppressing warning as this is used as a web service
     *
     * @param topic Topic name
     * @return An array of TopicRolePermission
     * @throws EventAdminException Thrown when topic manager cannot be accessed.
     */
    @SuppressWarnings("UnusedDeclaration")
    public TopicRolePermission[] getTopicRolePermissions(String topic) throws EventAdminException {
        topic = setNameToLowerCase(topic);
        EventBroker eventBroker = EventAdminHolder.getInstance().getEventBroker();
        try {
            return eventBroker.getTopicManagerService().getTopicRolePermission(topic);
        } catch (EventBrokerException e) {
            String errorMessage = e.getMessage();
            log.error(errorMessage, e);
            throw new EventAdminException(errorMessage, e);
        }
    }

    /**
     * Adds a new topic
     *
     * @param topic New topic name
     * @throws EventAdminException Thrown when accessing registry or when providing permissions.
     */
    public void addTopic(String topic) throws EventAdminException {
        topic = setNameToLowerCase(topic);
        EventBroker eventBroker = EventAdminHolder.getInstance().getEventBroker();
        try {
            if (!eventBroker.getTopicManagerService().isTopicExists(topic)) {
                eventBroker.getTopicManagerService().addTopic(topic);
            } else {
                throw new EventAdminException("Topic with name : " + topic + " already exists!");
            }
        } catch (EventBrokerException e) {
            String errorMessage = e.getMessage();
            log.error(errorMessage, e);
            throw new EventAdminException(errorMessage, e);
        }
    }

    /**
     * Updates the permissions for roles of a topic
     * Suppressing warning as this is used as a web service
     *
     * @param topic                Topic name
     * @param topicRolePermissions New roles with permissions
     * @throws EventAdminException Thrown when updating topic permissions.
     */
    @SuppressWarnings("UnusedDeclaration")
    public void updatePermission(String topic, TopicRolePermission[] topicRolePermissions)
            throws EventAdminException {
        topic = setNameToLowerCase(topic);
        EventBroker eventBroker = EventAdminHolder.getInstance().getEventBroker();
        try {
            eventBroker.getTopicManagerService().updatePermissions(topic, topicRolePermissions);
        } catch (EventBrokerException e) {
            String errorMessage = e.getMessage();
            log.error(errorMessage, e);
            throw new EventAdminException(errorMessage, e);
        }
    }

    /**
     * Gets all subscriptions for a topic with limited results to return
     * Suppressing warning as this is used as a web service
     *
     * @param topic                Topic name
     * @param startingIndex        Starting index of which the results should be returned
     * @param maxSubscriptionCount The amount of results to be returned
     * @return An array of Subscriptions
     * @throws EventAdminException Thrown when accessing topic manager.
     */
    @SuppressWarnings("UnusedDeclaration")
    public Subscription[] getAllWSSubscriptionsForTopic(String topic, int startingIndex,
                                                        int maxSubscriptionCount)
            throws EventAdminException {
        topic = setNameToLowerCase(topic);
        EventBroker eventBroker = EventAdminHolder.getInstance().getEventBroker();
        try {
            TopicManagerService topicManager = eventBroker.getTopicManagerService();
            org.wso2.carbon.andes.event.core.subscription.Subscription[] subscriptions =
                    topicManager.getSubscriptions(topic, true);

            int resultSetSize = maxSubscriptionCount;
            if ((subscriptions.length - startingIndex) < maxSubscriptionCount) {
                resultSetSize = (subscriptions.length - startingIndex);
            }
            Subscription[] subscriptionsDTO = new Subscription[resultSetSize];

            int index = 0;
            int subscriptionIndex = 0;
            for (org.wso2.carbon.andes.event.core.subscription.Subscription backEndSubscription : subscriptions) {
                if (startingIndex == index || startingIndex < index) {
                    subscriptionsDTO[subscriptionIndex] = adaptSubscription(backEndSubscription);
                    subscriptionIndex++;
                    if (subscriptionIndex == maxSubscriptionCount) {
                        break;
                    }
                }
                index++;
            }
            return subscriptionsDTO;
        } catch (EventBrokerException e) {
            String errorMessage = e.getMessage();
            log.error(errorMessage, e);
            throw new EventAdminException(errorMessage, e);
        }
    }

    /**
     * Gets all the subscriptions
     * Suppressing warning as this is used as a web service
     *
     * @param topic Topic name
     * @return An array of subscriptions
     * @throws EventAdminException Thrown when accessing topic manager.
     */
    @SuppressWarnings("UnusedDeclaration")
    public Subscription[] getWsSubscriptionsForTopic(String topic) throws EventAdminException {
        topic = setNameToLowerCase(topic);
        EventBroker eventBroker = EventAdminHolder.getInstance().getEventBroker();
        try {
            return adaptSubscriptions(eventBroker.getTopicManagerService().getSubscriptions(topic, true));
        } catch (EventBrokerException e) {
            String errorMessage = e.getMessage();
            log.error(errorMessage, e);
            throw new EventAdminException(errorMessage, e);
        }
    }

    /**
     * Gets the total number of subscriptions for a topic
     * Suppressing warning as this is used as a web service
     *
     * @param topic Topic name
     * @return Number of subscriptions
     * @throws EventAdminException Thrown when accessing topic manager.
     */
    @SuppressWarnings("UnusedDeclaration")
    public int getAllWSSubscriptionCountForTopic(String topic) throws EventAdminException {
        topic = setNameToLowerCase(topic);
        EventBroker eventBroker = EventAdminHolder.getInstance().getEventBroker();
        try {
            return eventBroker.getTopicManagerService().getSubscriptions(topic, true).length;
        } catch (EventBrokerException e) {
            String errorMessage = e.getMessage();
            log.error(errorMessage, e);
            throw new EventAdminException(errorMessage, e);
        }
    }

    /**
     * Gets the JMS subscriptions for a topic
     * Suppressing warning as this is used as a web service
     *
     * @param topic Topic name
     * @return An array of subscriptions
     * @throws EventAdminException Thrown when getting JMS subscriptions details from registry.
     */
    @SuppressWarnings("UnusedDeclaration")
    public Subscription[] getJMSSubscriptionsForTopic(String topic)
            throws EventAdminException {
        topic = setNameToLowerCase(topic);
        EventBroker eventBroker = EventAdminHolder.getInstance().getEventBroker();
        try {
            return adaptSubscriptions(eventBroker.getTopicManagerService().getJMSSubscriptions(topic));
        } catch (EventBrokerException e) {
            String errorMessage = e.getMessage();
            log.error(errorMessage, e);
            throw new EventAdminException(errorMessage, e);
        }
    }

    /**
     * Gets user roles through topic manager
     *
     * @return A string array of roles
     * @throws EventAdminException Thrown when topic manager is unable to get user roles.
     */
    public String[] getUserRoles() throws EventAdminException {
        EventBroker eventBroker = EventAdminHolder.getInstance().getEventBroker();
        try {
            return eventBroker.getTopicManagerService().getBackendRoles();
        } catch (EventBrokerException e) {
            String errorMessage = e.getMessage();
            log.error(errorMessage, e);
            throw new EventAdminException(errorMessage, e);
        }
    }

    /**
     * Publish a message to given topic
     *
     * @param content message to publish
     * @param topicName topic
     * @throws EventAdminException
     */
    public void publishToTopic(String content, String topicName) throws EventAdminException {
        topicName = setNameToLowerCase(topicName);
        EventBroker eventBroker = EventAdminHolder.getInstance().getEventBroker();
        try {
            if (checkCurrentUserHasPublishTopicPermission(topicName)) {
                if (eventBroker.getTopicManagerService().isTopicExists(topicName)) {
                    Message message = new Message();
                    message.setMessage(content);
                    eventBroker.publish(message, topicName);
                } else {
                    throw new EventAdminException("Topic with name : " + topicName + " not exists!");
                }
            } else {
                throw new EventAdminException("Permission denied.");
            }
        } catch (EventBrokerException e) {
            String errorMessage = e.getMessage();
            log.error(errorMessage, e);
            throw new EventAdminException(errorMessage, e);
        }
    }

    /**
     * Evaluate current logged in user has add topic permission. This service mainly used to restrict UI control for
     * un-authorize users
     *
     * @return true/false based on permission
     * @throws EventAdminException
     */
    public boolean checkCurrentUserHasAddTopicPermission() throws EventAdminException {
        return checkUserHasAddTopicPermission(CarbonContext.getThreadLocalCarbonContext().getUsername());
    }

    /**
     * Evaluate given username has add topic permission. This service mainly used to restrict UI control for
     * un-authorize users
     *
     * @return true/false based on permission
     * @throws EventAdminException
     */
    public boolean checkUserHasAddTopicPermission(String username) throws EventAdminException {
        EventBroker eventBroker = EventAdminHolder.getInstance().getEventBroker();
        try {
            return eventBroker.getTopicManagerService().checkUserHasAddTopicPermission(username);
        } catch (EventBrokerException e) {
            String errorMessage = e.getMessage();
            log.error(errorMessage, e);
            throw new EventAdminException(errorMessage, e);
        }
    }

    /**
     * Evaluate current logged in user has delete topic permission. This service mainly used to restrict UI control for
     * un-authorize users
     *
     * @return true/false based on permission
     * @throws EventAdminException
     */
    public boolean checkCurrentUserHasDeleteTopicPermission() throws EventAdminException {
        return checkUserHasDeleteTopicPermission(CarbonContext.getThreadLocalCarbonContext().getUsername());
    }

    /**
     * Evaluate given username has delete topic permission. This service mainly used to restrict UI control for
     * un-authorize users
     *
     * @return true/false based on permission
     * @throws EventAdminException
     */
    public boolean checkUserHasDeleteTopicPermission(String username) throws EventAdminException {
        EventBroker eventBroker = EventAdminHolder.getInstance().getEventBroker();
        try {
            return eventBroker.getTopicManagerService().checkUserHasDeleteTopicPermission(username);
        } catch (EventBrokerException e) {
            String errorMessage = e.getMessage();
            log.error(errorMessage, e);
            throw new EventAdminException(errorMessage, e);
        }
    }

    /**
     * Evaluate current logged in user has view topic details permission. This service mainly used to restrict UI control for
     * un-authorize users
     *
     * @return true/false based on permission
     * @throws EventAdminException
     */
    public boolean checkCurrentUserHasDetailsTopicPermission() throws EventAdminException {
        return checkUserHasDetailsTopicPermission(CarbonContext.getThreadLocalCarbonContext().getUsername());
    }

    /**
     * Evaluate given username has view topic details permission. This service mainly used to restrict UI control for
     * un-authorize users
     *
     * @return true/false based on permission
     * @throws EventAdminException
     */
    public boolean checkUserHasDetailsTopicPermission(String username) throws EventAdminException {
        EventBroker eventBroker = EventAdminHolder.getInstance().getEventBroker();
        try {
            return eventBroker.getTopicManagerService().checkUserHasDetailsTopicPermission(username);
        } catch (EventBrokerException e) {
            String errorMessage = e.getMessage();
            log.error(errorMessage, e);
            throw new EventAdminException(errorMessage, e);
        }
    }

    /**
     * Evaluate current logged in user has publish permission for given topic. This service mainly used to restrict UI
     * control for un-authorize users
     *
     * @return true/false based on permission
     * @throws EventAdminException
     */
    public boolean checkCurrentUserHasPublishTopicPermission(String topicName) throws EventAdminException {
        topicName = setNameToLowerCase(topicName);
        return checkUserHasPublishTopicPermission(topicName, CarbonContext.getThreadLocalCarbonContext().getUsername());
    }

    /**
     * Evaluate given username has publish permission for given topic. This service mainly used to restrict UI control for
     * un-authorize users
     *
     * @return true/false based on permission
     * @throws EventAdminException
     */
    public boolean checkUserHasPublishTopicPermission(String topicName, String username) throws EventAdminException {
        topicName = setNameToLowerCase(topicName);
        EventBroker eventBroker = EventAdminHolder.getInstance().getEventBroker();
        try {
            return eventBroker.getTopicManagerService().checkUserHasPublishTopicPermission(topicName, username);
        } catch (EventBrokerException e) {
            String errorMessage = e.getMessage();
            log.error(errorMessage, e);
            throw new EventAdminException(errorMessage, e);
        }
    }

    /**
     * Removes a topic
     * Suppressing warning as this is used as a web service
     *
     * @param topic Topic name
     * @return true if topic existed to delete and deleted, false otherwise.
     * @throws EventAdminException
     */
    @SuppressWarnings("UnusedDeclaration")
    public boolean removeTopic(String topic) throws EventAdminException {
        topic = setNameToLowerCase(topic);
        EventBroker eventBroker = EventAdminHolder.getInstance().getEventBroker();
        try {
            return eventBroker.getTopicManagerService().removeTopic(topic);
        } catch (EventBrokerException e) {
            String errorMessage = e.getMessage();
            log.error(errorMessage, e);
            throw new EventAdminException(errorMessage, e);
        }
    }

    /**
     * Converting carbon event core subscription array to carbon event internal subscription array
     *
     * @param subscriptions A carbon event core subscriptions array
     * @return A carbon event internal subscription array
     */
    private Subscription[] adaptSubscriptions(
            org.wso2.carbon.andes.event.core.subscription.Subscription[] subscriptions) {
        Subscription[] adminSubscriptions = new Subscription[subscriptions.length];
        Calendar calendar = Calendar.getInstance();
        int index = 0;
        for (org.wso2.carbon.andes.event.core.subscription.Subscription coreSubscription : subscriptions) {
            calendar.setTime(coreSubscription.getCreatedTime());
            Subscription adminSubscription = new Subscription();
            adminSubscription.setCreatedTime(calendar);
            adminSubscription.setEventDispatcher(coreSubscription.getEventDispatcher());
            adminSubscription.setEventDispatcherName(coreSubscription.getEventDispatcherName());
            adminSubscription.setEventFilter(coreSubscription.getEventFilter());
            adminSubscription.setEventSinkURL(coreSubscription.getEventSinkURL());
            adminSubscription.setExpires(coreSubscription.getExpires());
            adminSubscription.setId(coreSubscription.getId());
            adminSubscription.setOwner(coreSubscription.getOwner());
            adminSubscription.setTopicName(coreSubscription.getTopicName());
            adminSubscription.setMode(coreSubscription.getMode());
            adminSubscriptions[index] = adminSubscription;
            index++;
        }
        return adminSubscriptions;
    }

    /**
     * Converting carbon event core subscription to carbon event internal subscription
     *
     * @param coreSubscription A carbon event core subscriptions
     * @return A carbon event internal subscription
     */
    private Subscription adaptSubscription(
            org.wso2.carbon.andes.event.core.subscription.Subscription coreSubscription) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(coreSubscription.getCreatedTime());
        Subscription adminSubscription = new Subscription();
        adminSubscription.setCreatedTime(calendar);
        adminSubscription.setEventDispatcher(coreSubscription.getEventDispatcher());
        adminSubscription.setEventDispatcherName(coreSubscription.getEventDispatcherName());
        adminSubscription.setEventFilter(coreSubscription.getEventFilter());
        adminSubscription.setEventSinkURL(coreSubscription.getEventSinkURL());
        adminSubscription.setExpires(coreSubscription.getExpires());
        adminSubscription.setId(coreSubscription.getId());
        adminSubscription.setOwner(coreSubscription.getOwner());
        adminSubscription.setTopicName(coreSubscription.getTopicName());
        adminSubscription.setMode(coreSubscription.getMode());
        return adminSubscription;
    }

    /**
     * Given a queue name this method returns the lower case representation of the queue name.
     *
     * @param queue the queue name to change the case
     * @return the lower case representation of the queue name
     */
    private String setNameToLowerCase(String queue) {
        return queue.toLowerCase();
    }
}
