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
package org.wso2.carbon.andes.admin;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.andes.admin.internal.Exception.BrokerManagerAdminException;
import org.wso2.carbon.andes.admin.internal.Message;
import org.wso2.carbon.andes.admin.internal.Queue;
import org.wso2.carbon.andes.admin.internal.QueueRolePermission;
import org.wso2.carbon.andes.admin.internal.Subscription;
import org.wso2.carbon.andes.admin.util.AndesBrokerManagerAdminServiceDSHolder;
import org.wso2.carbon.andes.core.QueueManagerException;
import org.wso2.carbon.andes.core.QueueManagerService;
import org.wso2.carbon.andes.core.SubscriptionManagerException;
import org.wso2.carbon.andes.core.SubscriptionManagerService;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.core.AbstractAdmin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Provides all the andes admin services that is available through the UI(JSP).
 */
public class AndesAdminService extends AbstractAdmin {
    private static Log log = LogFactory.getLog(AndesAdminService.class);

    /**
     * Creates a queue.
     *
     * @param queueName New queue name.
     * @throws BrokerManagerAdminException
     */
    public void createQueue(String queueName) throws BrokerManagerAdminException {
        QueueManagerService queueManagerService = AndesBrokerManagerAdminServiceDSHolder.getInstance()
                                                                        .getQueueManagerService();
        try {
            queueManagerService.createQueue(queueName);
        } catch (QueueManagerException e) {
            log.error("Error in creating the queue. " + e.getMessage(), e);
            throw new BrokerManagerAdminException("Error in creating the queue. " + e.getMessage(), e);
        }
    }

    /**
     * Gets all queues.
     * Suppressing 'MismatchedQueryAndUpdateOfCollection' as 'allQueues' is used to sort and to
     * convert to an array.
     *
     * @return An array of queues.
     * @throws BrokerManagerAdminException
     */
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    public org.wso2.carbon.andes.admin.internal.Queue[] getAllQueues()
            throws BrokerManagerAdminException {
        List<org.wso2.carbon.andes.admin.internal.Queue> allQueues
                                        = new ArrayList<org.wso2.carbon.andes.admin.internal.Queue>();
        org.wso2.carbon.andes.admin.internal.Queue[] queuesDTO;
        try {
            QueueManagerService queueManagerService =
                    AndesBrokerManagerAdminServiceDSHolder.getInstance().getQueueManagerService();
            List<org.wso2.carbon.andes.core.types.Queue> queues = queueManagerService.getAllQueues();
            queuesDTO = new org.wso2.carbon.andes.admin.internal.Queue[queues.size()];
            for (org.wso2.carbon.andes.core.types.Queue queue : queues) {
                org.wso2.carbon.andes.admin.internal.Queue queueDTO =
                                                    new org.wso2.carbon.andes.admin.internal.Queue();
                queueDTO.setQueueName(queue.getQueueName());
                queueDTO.setMessageCount(queue.getMessageCount());
                queueDTO.setCreatedTime(queue.getCreatedTime());
                queueDTO.setUpdatedTime(queue.getUpdatedTime());
                allQueues.add(queueDTO);
            }
            CustomQueueComparator comparator = new CustomQueueComparator();
            Collections.sort(allQueues, Collections.reverseOrder(comparator));
            allQueues.toArray(queuesDTO);
        } catch (QueueManagerException e) {
            log.error("Problem in getting queues from back end", e);
            throw new BrokerManagerAdminException("Problem in getting queues from back-end", e);
        }
        return queuesDTO;
    }

    /**
     * Gets the message count for a queue
     * Suppressing 'UnusedDeclaration' as it is called by webservice
     *
     * @param destinationName Destination name.
     * @param msgPattern      Value should be either 'queue' or 'topic'.
     * @return Message count.
     * @throws BrokerManagerAdminException
     */
    @SuppressWarnings("UnusedDeclaration")
    public long getMessageCount(String destinationName, String msgPattern)
            throws BrokerManagerAdminException {
        long messageCount;
        try {
            QueueManagerService queueManagerService =
                    AndesBrokerManagerAdminServiceDSHolder.getInstance().getQueueManagerService();
            messageCount = queueManagerService.getMessageCount(destinationName, msgPattern);
            return messageCount;
        } catch (Exception e) {
            log.error("Error while getting message count for queue", e);
            throw new BrokerManagerAdminException("Error while getting message count for queue", e);
        }
    }

    /**
     * Deletes a queue.
     *
     * @param queueName Queue name.
     * @throws BrokerManagerAdminException
     */
    public void deleteQueue(String queueName) throws BrokerManagerAdminException {
        try {
            QueueManagerService queueManagerService =
                    AndesBrokerManagerAdminServiceDSHolder.getInstance().getQueueManagerService();
            queueManagerService.deleteQueue(queueName);
        } catch (QueueManagerException e) {
            log.error(e.getMessage(), e);
            throw new BrokerManagerAdminException(e.getMessage(), e);
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
                    AndesBrokerManagerAdminServiceDSHolder.getInstance().getQueueManagerService();
            queueManagerService.deleteTopicFromRegistry(topicName, subscriptionId);
        } catch (QueueManagerException e) {
            String message = e.getMessage();
            throw new BrokerManagerAdminException("Error in deleting topic from registry. " +
                    "" + message, e);
        }
    }

    /**
     * Restore messages from the Dead Letter Queue to their original queues.
     *
     * @param messageIDs          Browser Message Id / External Message Id list.
     * @param deadLetterQueueName Dead Letter Queue name for the respective tenant.
     * @throws BrokerManagerAdminException
     */
    public void restoreMessagesFromDeadLetterQueue(String[] messageIDs, String deadLetterQueueName)
            throws BrokerManagerAdminException {
        try {
            QueueManagerService queueManagerService =
                    AndesBrokerManagerAdminServiceDSHolder.getInstance().getQueueManagerService();
            queueManagerService.restoreMessagesFromDeadLetterQueue(messageIDs, deadLetterQueueName);
        } catch (QueueManagerException e) {
            log.error("Error in restoring message from dead letter queue", e);
            throw new BrokerManagerAdminException("Error in restoring message from dead letter queue.", e);
        }
    }

    /**
     * Restore messages from the Dead Letter Queue to another queue in the same tenant.
     *
     * @param messageIDs          Browser Message Id / External Message Id list.
     * @param destination         The new destination queue for the messages in the same tenant.
     * @param deadLetterQueueName Dead Letter Queue name for the respective tenant.
     * @throws BrokerManagerAdminException
     */
    public void restoreMessagesFromDeadLetterQueueWithDifferentDestination(String[] messageIDs,
                                                                           String destination,
                                                                           String deadLetterQueueName)
            throws BrokerManagerAdminException {
        try {
            QueueManagerService queueManagerService =
                    AndesBrokerManagerAdminServiceDSHolder.getInstance().getQueueManagerService();
            queueManagerService.restoreMessagesFromDeadLetterQueueWithDifferentDestination(messageIDs,
                                                                   destination, deadLetterQueueName);
        } catch (QueueManagerException e) {
            log.error("Error in restoring message from dead letter queue", e);
            throw new BrokerManagerAdminException("Error in restoring message from dead letter queue.", e);
        }
    }

    /**
     * Delete messages from the Dead Letter Queue and delete their content.
     *
     * @param messageIDs          Browser Message Id / External Message Id list to be deleted.
     * @param deadLetterQueueName Dead Letter Queue name for the respective tenant.
     * @throws BrokerManagerAdminException
     */
    public void deleteMessagesFromDeadLetterQueue(String[] messageIDs, String deadLetterQueueName)
            throws BrokerManagerAdminException {
        try {
            QueueManagerService queueManagerService =
                    AndesBrokerManagerAdminServiceDSHolder.getInstance().getQueueManagerService();
            queueManagerService.deleteMessagesFromDeadLetterQueue(messageIDs, deadLetterQueueName);
        } catch (QueueManagerException e) {
            log.error("Error in deleting message from queue", e);
            throw new BrokerManagerAdminException("Error in deleting message from queue.", e);
        }
    }

    /**
     * Deletes all messages from a queue.
     *
     * @param queueName Queue name.
     * @throws BrokerManagerAdminException
     */
    public void purgeMessagesOfQueue(String queueName) throws BrokerManagerAdminException {
        try {
            QueueManagerService queueManagerService =
                    AndesBrokerManagerAdminServiceDSHolder.getInstance().getQueueManagerService();
            queueManagerService.purgeMessagesOfQueue(queueName);
        } catch (QueueManagerException e) {
            log.error("Error in purging message from queue. " + e.getMessage(), e);
            throw new BrokerManagerAdminException("Error in purging message from queue. " + e.getMessage(), e);
        }
    }

    /**
     * Gets all subscriptions.
     * Suppressing 'MismatchedQueryAndUpdateOfCollection' as 'allSubscriptions' is used to convert
     * to an array.
     *
     * @return An array of {@link Subscription}.
     * @throws BrokerManagerAdminException
     */
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    public Subscription[] getAllSubscriptions() throws BrokerManagerAdminException {
        List<Subscription> allSubscriptions = new ArrayList<Subscription>();
        Subscription[] subscriptionsDTO;
        try {
            SubscriptionManagerService subscriptionManagerService =
                AndesBrokerManagerAdminServiceDSHolder.getInstance().getSubscriptionManagerService();
            List<org.wso2.carbon.andes.core.types.Subscription> subscriptions =
                                                    subscriptionManagerService.getAllSubscriptions();
            subscriptionsDTO = new Subscription[subscriptions.size()];
            for (org.wso2.carbon.andes.core.types.Subscription sub : subscriptions) {
                Subscription subscriptionDTO = new Subscription();
                subscriptionDTO.setSubscriptionIdentifier(sub.getSubscriptionIdentifier());
                subscriptionDTO.setSubscribedQueueOrTopicName(sub.getSubscribedQueueOrTopicName());
                subscriptionDTO.setSubscriberQueueBoundExchange(sub.getSubscriberQueueBoundExchange());
                subscriptionDTO.setSubscriberQueueName(sub.getSubscriberQueueName());
                subscriptionDTO.setDurable(sub.isDurable());
                subscriptionDTO.setActive(sub.isActive());
                subscriptionDTO.setNumberOfMessagesRemainingForSubscriber(
                                                    sub.getNumberOfMessagesRemainingForSubscriber());
                subscriptionDTO.setSubscriberNodeAddress(sub.getSubscriberNodeAddress());

                allSubscriptions.add(subscriptionDTO);
            }
            CustomSubscriptionComparator comparator = new CustomSubscriptionComparator();
            Collections.sort(allSubscriptions, Collections.reverseOrder(comparator));
            allSubscriptions.toArray(subscriptionsDTO);
        } catch (SubscriptionManagerException e) {
            log.error("Problem in getting subscriptions from back end", e);
            throw new BrokerManagerAdminException("Problem in getting subscriptions from back end", e);
        }
        return subscriptionsDTO;
    }

    /**
     * Gets all durable queue subscriptions.
     * Suppressing 'MismatchedQueryAndUpdateOfCollection' as 'allSubscriptions' is used to convert
     * to an array.
     *
     * @return An array of {@link Subscription}.
     * @throws BrokerManagerAdminException
     */
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    public Subscription[] getAllDurableQueueSubscriptions() throws BrokerManagerAdminException {
        List<Subscription> allSubscriptions = new ArrayList<Subscription>();
        Subscription[] subscriptionsDTO;
        try {
            SubscriptionManagerService subscriptionManagerService =
                    AndesBrokerManagerAdminServiceDSHolder.getInstance().getSubscriptionManagerService();
            List<org.wso2.carbon.andes.core.types.Subscription> subscriptions = subscriptionManagerService
                    .getAllDurableQueueSubscriptions();
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
                subscriptionDTO.setSubscriberNodeAddress(sub.getSubscriberNodeAddress());

                allSubscriptions.add(subscriptionDTO);
            }
            CustomSubscriptionComparator comparator = new CustomSubscriptionComparator();
            Collections.sort(allSubscriptions, Collections.reverseOrder(comparator));
            allSubscriptions.toArray(subscriptionsDTO);
        } catch (SubscriptionManagerException e) {
            log.error("Problem in getting subscriptions from back end", e);
            throw new BrokerManagerAdminException("Problem in getting subscriptions from back end", e);
        }
        return subscriptionsDTO;
    }

    /**
     * Gets all local temporary queue subscriptions
     * Suppressing 'MismatchedQueryAndUpdateOfCollection' as 'allSubscriptions' is used to convert
     * to an array.
     *
     * @return An array of {@link Subscription}.
     * @throws BrokerManagerAdminException
     */
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    public Subscription[] getAllLocalTempQueueSubscriptions() throws BrokerManagerAdminException {
        List<Subscription> allSubscriptions = new ArrayList<Subscription>();
        Subscription[] subscriptionsDTO;
        try {
            SubscriptionManagerService subscriptionManagerService =
                    AndesBrokerManagerAdminServiceDSHolder.getInstance().getSubscriptionManagerService();
            List<org.wso2.carbon.andes.core.types.Subscription> subscriptions = subscriptionManagerService
                    .getAllLocalTempQueueSubscriptions();
            subscriptionsDTO = new Subscription[subscriptions.size()];
            for (org.wso2.carbon.andes.core.types.Subscription sub : subscriptions) {
                Subscription subscriptionDTO = new Subscription();
                subscriptionDTO.setSubscriptionIdentifier(sub.getSubscriptionIdentifier());
                subscriptionDTO.setSubscribedQueueOrTopicName(sub.getSubscribedQueueOrTopicName());
                subscriptionDTO.setSubscriberQueueBoundExchange(sub.getSubscriberQueueBoundExchange());
                subscriptionDTO.setSubscriberQueueName(sub.getSubscriberQueueName());
                subscriptionDTO.setDurable(sub.isDurable());
                subscriptionDTO.setActive(sub.isActive());
                subscriptionDTO.setNumberOfMessagesRemainingForSubscriber(
                                                    sub.getNumberOfMessagesRemainingForSubscriber());
                subscriptionDTO.setSubscriberNodeAddress(sub.getSubscriberNodeAddress());

                allSubscriptions.add(subscriptionDTO);
            }
            CustomSubscriptionComparator comparator = new CustomSubscriptionComparator();
            Collections.sort(allSubscriptions, Collections.reverseOrder(comparator));
            allSubscriptions.toArray(subscriptionsDTO);
        } catch (SubscriptionManagerException e) {
            log.error("Problem in getting subscriptions from back end", e);
            throw new BrokerManagerAdminException("Problem in getting subscriptions from back end", e);
        }
        return subscriptionsDTO;
    }

    /**
     * Gets all durable topic subscriptions
     * Suppressing 'MismatchedQueryAndUpdateOfCollection' as 'allSubscriptions' is used to convert
     * to an array.
     *
     * @return An array of {@link Subscription}.
     * @throws BrokerManagerAdminException
     */
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    public Subscription[] getAllDurableTopicSubscriptions() throws BrokerManagerAdminException {
        List<Subscription> allSubscriptions = new ArrayList<Subscription>();
        Subscription[] subscriptionsDTO;
        try {
            SubscriptionManagerService subscriptionManagerService =
                    AndesBrokerManagerAdminServiceDSHolder.getInstance().getSubscriptionManagerService();
            List<org.wso2.carbon.andes.core.types.Subscription> subscriptions = subscriptionManagerService
                    .getAllDurableTopicSubscriptions();
            subscriptionsDTO = new Subscription[subscriptions.size()];
            for (org.wso2.carbon.andes.core.types.Subscription sub : subscriptions) {
                Subscription subscriptionDTO = new Subscription();
                subscriptionDTO.setSubscriptionIdentifier(sub.getSubscriptionIdentifier());
                subscriptionDTO.setSubscribedQueueOrTopicName(sub.getSubscribedQueueOrTopicName());
                subscriptionDTO.setSubscriberQueueBoundExchange(sub.getSubscriberQueueBoundExchange());
                subscriptionDTO.setSubscriberQueueName(sub.getSubscriberQueueName());
                subscriptionDTO.setDurable(sub.isDurable());
                subscriptionDTO.setActive(sub.isActive());
                subscriptionDTO.setNumberOfMessagesRemainingForSubscriber(
                                                    sub.getNumberOfMessagesRemainingForSubscriber());
                subscriptionDTO.setSubscriberNodeAddress(sub.getSubscriberNodeAddress());
                subscriptionDTO.setDestination(sub.getDestination());

                allSubscriptions.add(subscriptionDTO);
            }
            CustomSubscriptionComparator comparator = new CustomSubscriptionComparator();
            Collections.sort(allSubscriptions, Collections.reverseOrder(comparator));
            allSubscriptions.toArray(subscriptionsDTO);
        } catch (SubscriptionManagerException e) {
            log.error("Problem in getting subscriptions from back end", e);
            throw new BrokerManagerAdminException("Problem in getting subscriptions from back end", e);
        }
        return subscriptionsDTO;
    }

    /**
     * Gets all local temporary topic subscriptions.
     * Suppressing 'MismatchedQueryAndUpdateOfCollection' as 'allSubscriptions' is used to convert
     * to an array.
     *
     * @return An array of {@link Subscription}.
     * @throws BrokerManagerAdminException
     */
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    public Subscription[] getAllLocalTempTopicSubscriptions() throws BrokerManagerAdminException {
        List<Subscription> allSubscriptions = new ArrayList<Subscription>();
        Subscription[] subscriptionsDTO;
        try {
            SubscriptionManagerService subscriptionManagerService =
                    AndesBrokerManagerAdminServiceDSHolder.getInstance().getSubscriptionManagerService();
            List<org.wso2.carbon.andes.core.types.Subscription> subscriptions = subscriptionManagerService
                    .getAllLocalTempTopicSubscriptions();
            subscriptionsDTO = new Subscription[subscriptions.size()];
            for (org.wso2.carbon.andes.core.types.Subscription sub : subscriptions) {
                Subscription subscriptionDTO = new Subscription();
                subscriptionDTO.setSubscriptionIdentifier(sub.getSubscriptionIdentifier());
                subscriptionDTO.setSubscribedQueueOrTopicName(sub.getSubscribedQueueOrTopicName());
                subscriptionDTO.setSubscriberQueueBoundExchange(sub.getSubscriberQueueBoundExchange());
                subscriptionDTO.setSubscriberQueueName(sub.getSubscriberQueueName());
                subscriptionDTO.setDurable(sub.isDurable());
                subscriptionDTO.setActive(sub.isActive());
                subscriptionDTO.setNumberOfMessagesRemainingForSubscriber(
                                                    sub.getNumberOfMessagesRemainingForSubscriber());
                subscriptionDTO.setSubscriberNodeAddress(sub.getSubscriberNodeAddress());

                allSubscriptions.add(subscriptionDTO);
            }
            CustomSubscriptionComparator comparator = new CustomSubscriptionComparator();
            Collections.sort(allSubscriptions, Collections.reverseOrder(comparator));
            allSubscriptions.toArray(subscriptionsDTO);
        } catch (SubscriptionManagerException e) {
            log.error("Problem in getting subscriptions from back end", e);
            throw new BrokerManagerAdminException("Problem in getting subscriptions from back end", e);
        }
        return subscriptionsDTO;
    }

    /**
     * Update the permission of the given queue name
     *
     * @param queueName               Name of the queue
     * @param queueRolePermissionsDTO A {@link org.wso2.carbon.andes.admin.internal.QueueRolePermission}
     * @throws BrokerManagerAdminException
     */
    public void updatePermission(String queueName, QueueRolePermission[] queueRolePermissionsDTO)
            throws
            BrokerManagerAdminException {
        QueueManagerService queueManagerService = AndesBrokerManagerAdminServiceDSHolder.getInstance()
                .getQueueManagerService();
        org.wso2.carbon.andes.core.types.QueueRolePermission[] rolePermissions;
        try {
            if (queueRolePermissionsDTO != null && queueRolePermissionsDTO.length > 0) {
                List<org.wso2.carbon.andes.core.types.QueueRolePermission> permissionList =
                                new ArrayList<org.wso2.carbon.andes.core.types.QueueRolePermission>();
                for (QueueRolePermission queueRolePermission : queueRolePermissionsDTO) {
                    org.wso2.carbon.andes.core.types.QueueRolePermission permission =
                                        new org.wso2.carbon.andes.core.types.QueueRolePermission();
                    permission.setRoleName(queueRolePermission.getRoleName());
                    permission.setAllowedToConsume(queueRolePermission.isAllowedToConsume());
                    permission.setAllowedToPublish(queueRolePermission.isAllowedToPublish());
                    permissionList.add(permission);
                }
                rolePermissions =
                    new org.wso2.carbon.andes.core.types.QueueRolePermission[permissionList.size()];
                permissionList.toArray(rolePermissions);
                queueManagerService.updatePermission(queueName, rolePermissions);
            }
        } catch (QueueManagerException e) {
            log.error("Unable to update permission of the queue.", e);
            throw new BrokerManagerAdminException("Unable to update permission of the queue.", e);
        }
    }

    /**
     * Get roles of the current logged user
     * If user has admin role, then all available roles will be return
     *
     * @return Array of user roles.
     * @throws BrokerManagerAdminException
     */
    public String[] getUserRoles() throws BrokerManagerAdminException {
        String[] roles;
        QueueManagerService queueManagerService = AndesBrokerManagerAdminServiceDSHolder.getInstance()
                .getQueueManagerService();
        try {
            roles = queueManagerService.getBackendRoles();
        } catch (QueueManagerException e) {
            log.error("Unable to get roles from user store", e);
            throw new BrokerManagerAdminException("Unable to get roles from user store.", e);
        }
        return roles;
    }

    /**
     * Get all permission of the given queue name
     *
     * @param queueName Name of the queue
     * @return Array of {@link org.wso2.carbon.andes.admin.internal.QueueRolePermission}
     * @throws BrokerManagerAdminException
     */
    public QueueRolePermission[] getQueueRolePermission(String queueName)
            throws BrokerManagerAdminException {
        QueueManagerService queueManagerService = AndesBrokerManagerAdminServiceDSHolder.getInstance()
                .getQueueManagerService();
        List<QueueRolePermission> queueRolePermissionDTOList = new ArrayList<QueueRolePermission>();
        try {
            org.wso2.carbon.andes.core.types.QueueRolePermission[] queueRolePermission = queueManagerService
                    .getQueueRolePermission(queueName);
            for (org.wso2.carbon.andes.core.types.QueueRolePermission rolePermission : queueRolePermission) {
                QueueRolePermission queueRolePermissionDTO = new QueueRolePermission();
                queueRolePermissionDTO.setRoleName(rolePermission.getRoleName());
                queueRolePermissionDTO.setAllowedToConsume(rolePermission.isAllowedToConsume());
                queueRolePermissionDTO.setAllowedToPublish(rolePermission.isAllowedToPublish());
                queueRolePermissionDTOList.add(queueRolePermissionDTO);
            }
            return queueRolePermissionDTOList.toArray(
                                        new QueueRolePermission[queueRolePermissionDTOList.size()]);
        } catch (QueueManagerException e) {
            log.error("Unable to retrieve queue permission", e);
            throw new BrokerManagerAdminException("Unable to retrieve queue permission.", e);
        }
    }

    /**
     * Browse the given queue name by start index and max messages count
     *
     * @param queueName     Name of the queue
     * @param startingIndex Start point of the queue messages
     * @param maxMsgCount   Maximum messages from start index
     * @return Array of {@link org.wso2.carbon.andes.admin.internal.Message}
     * @throws BrokerManagerAdminException
     */
    public Message[] browseQueue(String queueName, int startingIndex,
                                 int maxMsgCount) throws BrokerManagerAdminException {
        QueueManagerService queueManagerService = AndesBrokerManagerAdminServiceDSHolder.getInstance()
                .getQueueManagerService();
        List<Message> messageDTOList = new ArrayList<Message>();
        try {
            org.wso2.carbon.andes.core.types.Message[] messages =
                    queueManagerService.browseQueue(queueName, getCurrentUser(), getAccessKey(),
                                                                        startingIndex, maxMsgCount);
            for (org.wso2.carbon.andes.core.types.Message message : messages) {
                Message messageDTO = new Message();
                messageDTO.setMsgProperties(message.getMsgProperties());
                messageDTO.setContentType(message.getContentType());
                messageDTO.setMessageContent(message.getMessageContent());
                messageDTO.setJMSMessageId(message.getJMSMessageId());
                messageDTO.setJMSCorrelationId(message.getJMSCorrelationId());
                messageDTO.setJMSType(message.getJMSType());
                messageDTO.setJMSReDelivered(message.getJMSReDelivered());
                messageDTO.setJMSDeliveredMode(message.getJMSDeliveredMode());
                messageDTO.setMsgProperties(message.getMsgProperties());
                messageDTO.setJMSTimeStamp(message.getJMSTimeStamp());
                messageDTO.setJMSExpiration(message.getJMSExpiration());
                messageDTO.setDlcMsgDestination(message.getDlcMsgDestination());
                messageDTOList.add(messageDTO);
            }
            return messageDTOList.toArray(new Message[messageDTOList.size()]);
        } catch (QueueManagerException e) {
            log.error("Unable to browse queue", e);
            throw new BrokerManagerAdminException("Unable to browse queue.", e);
        }
    }

    /**
     * Total messages in the given queue
     *
     * @param queueName Name of the queue
     * @return Total count of the messages
     * @throws BrokerManagerAdminException
     */
    public long getTotalMessagesInQueue(String queueName) throws BrokerManagerAdminException {
        QueueManagerService queueManagerService = AndesBrokerManagerAdminServiceDSHolder.getInstance()
                .getQueueManagerService();
        try {
            return queueManagerService.getTotalMessagesInQueue(queueName);
        } catch (QueueManagerException e) {
            log.error("Unable to get total message count", e);
            throw new BrokerManagerAdminException("Unable to get total message count.", e);
        }
    }

    /**
     * Send messages to given queue
     *
     * @param queueName        Name of the queue
     * @param jmsType          JMS Type
     * @param jmsCorrelationID JMS Correlation Id
     * @param numberOfMessages Number of times
     * @param message          Message content
     * @param deliveryMode     Delivery mode
     * @param priority         Message priority
     * @param expireTime       Message expire time
     * @return true if send message successful and false otherwise
     * @throws BrokerManagerAdminException
     */
    public boolean sendMessage(String queueName, String jmsType, String jmsCorrelationID,
                               int numberOfMessages,
                               String message,
                               int deliveryMode, int priority, long expireTime)
            throws BrokerManagerAdminException {
        QueueManagerService queueManagerService = AndesBrokerManagerAdminServiceDSHolder.getInstance()
                .getQueueManagerService();
        try {
            return queueManagerService.sendMessage(queueName, getCurrentUser(), getAccessKey(), jmsType,
                                                   jmsCorrelationID,
                                                   numberOfMessages, message, deliveryMode, priority, expireTime);
        } catch (QueueManagerException e) {
            log.error("Unable to send message", e);
            throw new BrokerManagerAdminException("Unable to send message.", e);
        }
    }

    /**
     * Gets the access key for a amqp url.
     * @return The access key.
     */
    public String getAccessKey() {
        return AndesBrokerManagerAdminServiceDSHolder.getInstance().getAccessKey();
    }

    /**
     * Gets the number of messages remaining for a subscriber.
     *
     * @param subscriptionID The subscription ID.
     * @param durable        Whether the subscription is durable or not.
     * @return The number of remaining messages.
     * @throws BrokerManagerAdminException
     */
    public int getMessageCountForSubscriber(String subscriptionID, boolean durable) throws BrokerManagerAdminException {
        int remainingMessages = 0;
        try {
            SubscriptionManagerService subscriptionManagerService =
                    AndesBrokerManagerAdminServiceDSHolder.getInstance().getSubscriptionManagerService();
            List<org.wso2.carbon.andes.core.types.Subscription> subscriptions;
            if (durable) {
                subscriptions = subscriptionManagerService.getAllDurableTopicSubscriptions();
            } else {
                subscriptions = subscriptionManagerService.getAllLocalTempTopicSubscriptions();
            }

            for (org.wso2.carbon.andes.core.types.Subscription subscription : subscriptions) {
                if (subscription.getSubscriptionIdentifier().equals(subscriptionID)) {
                    remainingMessages = subscription.getNumberOfMessagesRemainingForSubscriber();
                    break;
                }
            }
        } catch (SubscriptionManagerException e) {
            String errorMessage = "Unable to get remaining messages for subscription.";
            log.error(errorMessage, e);
            throw new BrokerManagerAdminException(errorMessage, e);
        }

        return remainingMessages;
    }

    /**
     * Get current user's username.
     * @return The user name.
     */
    private String getCurrentUser() {
        String userName;
        if (CarbonContext.getThreadLocalCarbonContext().getTenantId() != 0) {
            userName = CarbonContext.getThreadLocalCarbonContext().getUsername() + "!"
                       + CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        } else {
            userName = CarbonContext.getThreadLocalCarbonContext().getUsername();
        }
        return userName.trim();
    }

    /**
     * A comparator class to order queues.
     */
    public class CustomQueueComparator implements Comparator<Queue> {

        /**
         * {@inheritDoc}
         */
        @Override
        public int compare(Queue queue1, Queue queue2) {
            long comparedValue = queue1.getMessageCount() - queue2.getMessageCount();
            int returnValue = 0;
            if (comparedValue < 0) {
                returnValue = -1;
            } else if (comparedValue > 0) {
                returnValue = 1;
            }
            return returnValue;
        }
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
