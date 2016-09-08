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
import org.wso2.carbon.andes.commons.CommonsUtil;
import org.wso2.carbon.andes.core.QueueManagerException;
import org.wso2.carbon.andes.core.QueueManagerService;
import org.wso2.carbon.andes.core.SubscriptionManagerException;
import org.wso2.carbon.andes.core.SubscriptionManagerService;
import org.wso2.carbon.andes.core.internal.util.Utils;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.core.AbstractAdmin;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.authorization.TreeNode;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Provides all the andes admin services that is available through the UI(JSP).
 */
public class AndesAdminService extends AbstractAdmin {
    private static Log log = LogFactory.getLog(AndesAdminService.class);
    /**
     * Permission value for changing permissions through UI.
     */
    private static final String UI_EXECUTE = "ui.execute";

    /**
     * Permission path for adding a queue.
     */
    private static final String PERMISSION_ADMIN_MANAGE_QUEUE_ADD_QUEUE = "/permission/admin/manage/queue/add";

    /**
     * Permission path for deleting a queue.
     */
    private static final String PERMISSION_ADMIN_MANAGE_QUEUE_DELETE_QUEUE = "/permission/admin/manage/queue/delete";

    /**
     * Permission path for purging a queue messages.
     */
    private static final String PERMISSION_ADMIN_MANAGE_QUEUE_PURGE_QUEUE = "/permission/admin/manage/queue/purge";

    /**
     * Permission path for browsing a queue
     */
    private static final String PERMISSION_ADMIN_MANAGE_BROWSE_QUEUE = "/permission/admin/manage/queue/browse";

    /**
     * Permission path for browsing message in dlc
     */
    private static final String PERMISSION_ADMIN_MANAGE_BROWSE_DLC = "/permission/admin/manage/dlc/browse";

    /**
     * Permission path for delete message in dlc
     */
    private static final String PERMISSION_ADMIN_MANAGE_DLC_DELETE_MESSAGE = "/permission/admin/manage/dlc/delete";

    /**
     * Permission path for restore message in dlc
     */
    private static final String PERMISSION_ADMIN_MANAGE_DLC_RESTORE_MESSAGE = "/permission/admin/manage/dlc/restore";

    /**
     * Permission path for reroute message in dlc
     */
    private static final String PERMISSION_ADMIN_MANAGE_DLC_REROUTE_MESSAGE = "/permission/admin/manage/dlc/reroute";

	/**
	 * Permission path for forcibly close subscriptions for queues
	 */
	private static final String PERMISSION_ADMIN_MANAGE_QUEUE_SUBSCRIPTION_CLOSE =
			"/permission/admin/manage/subscriptions/queue-close";

	/**
	 * Permission path for forcibly close subscriptions for topics
	 */
	private static final String PERMISSION_ADMIN_MANAGE_TOPIC_SUBSCRIPTION_CLOSE =
			"/permission/admin/manage/subscriptions/topic-close";

    /**
     * Retrieve an {@link org.wso2.carbon.andes.admin.internal.Queue} with the number of messages remaining in the
     * database by passing a queue name
     *
     * @param queueName name of the queue
     * @return org.wso2.carbon.andes.admin.internal.Queue if the queue exists, null if it doesn't exists
     */
    public org.wso2.carbon.andes.admin.internal.Queue getQueueByName(String queueName)
            throws BrokerManagerAdminException {
        org.wso2.carbon.andes.admin.internal.Queue queue = null;
        try {

            QueueManagerService queueManagerService =
                    AndesBrokerManagerAdminServiceDSHolder.getInstance().getQueueManagerService();
            if (null != queueManagerService.getQueueByName(queueName)) {
                queue = new org.wso2.carbon.andes.admin.internal.Queue(queueManagerService.getQueueByName(queueName));
            }
        } catch (QueueManagerException e) {
            log.error("Problem in getting queues from back end", e);
            throw new BrokerManagerAdminException("Problem in getting queues from back-end", e);
        }
        return queue;
    }

    /**
     * Retrieve the DLC queue for the domain
     *
     * @return {@link org.wso2.carbon.andes.admin.internal.Queue} with the number of messages in the dlc
     */
    public org.wso2.carbon.andes.admin.internal.Queue getDLCQueue() throws BrokerManagerAdminException {
        org.wso2.carbon.andes.admin.internal.Queue queue;
        try {

            String tenantDomain = Utils.getTenantDomain();
            QueueManagerService queueManagerService =
                    AndesBrokerManagerAdminServiceDSHolder.getInstance().getQueueManagerService();
            queue = new org.wso2.carbon.andes.admin.internal.Queue(queueManagerService.getDLCQueue(tenantDomain));

        } catch (QueueManagerException e) {
            log.error("Problem in getting queues from back end", e);
            throw new BrokerManagerAdminException("Problem in getting queues from back-end", e);
        }
        return queue;
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
            String errorMessage = e.getMessage();
            log.error(errorMessage, e);
            throw new BrokerManagerAdminException(errorMessage, e);
        }
        return queuesDTO;
    }

    /**
     * Get a list of names of durable queues created in the broker
     *
     * @return a set of queue names
     * @throws BrokerManagerAdminException on error while retrieving information
     */
    public Set<String> getNamesOfAllDurableQueues() throws BrokerManagerAdminException {
        QueueManagerService queueManagerService =
                AndesBrokerManagerAdminServiceDSHolder.getInstance().getQueueManagerService();
        Set<String> namesOfDurableQueues;
        try {
            namesOfDurableQueues = queueManagerService.getNamesOfAllDurableQueues();
        } catch (QueueManagerException e) {
            log.error("Error while retrieving names of durable queues ", e);
            throw new BrokerManagerAdminException("Error while retrieving names of durable queues", e);
        }
        return namesOfDurableQueues;
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
            log.error("Error while retrieving message count by queue manager service", e);
            throw new BrokerManagerAdminException("Error while retrieving message count "
                    + "by queue manager service", e);
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
            String errorMessage = e.getMessage();
            log.error(errorMessage, e);
            throw new BrokerManagerAdminException(errorMessage, e);
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
            String errorMessage = e.getMessage();
            log.error(errorMessage, e);
            throw new BrokerManagerAdminException(errorMessage, e);
        }
    }

    /**
     * Restore messages from the Dead Letter Queue to their original queues.
     *
     * @param messageIDs          Browser Message Id / External Message Id list.
     * @param destinationQueueName Dead Letter Queue name for the respective tenant.
     * @throws BrokerManagerAdminException
     */
    public void restoreMessagesFromDeadLetterQueue(long[] messageIDs, String destinationQueueName)
            throws BrokerManagerAdminException {
        try {
            QueueManagerService queueManagerService =
                    AndesBrokerManagerAdminServiceDSHolder.getInstance().getQueueManagerService();
            queueManagerService.restoreMessagesFromDeadLetterQueue(messageIDs, destinationQueueName);
        } catch (QueueManagerException e) {
            String errorMessage = e.getMessage();
            log.error(errorMessage, e);
            throw new BrokerManagerAdminException(errorMessage, e);
        }
    }

    /**
     * Restore messages from the Dead Letter Queue to another queue in the same tenant.
     *
     * @param messageIDs          Browser Message Id / External Message Id list.
     * @param newDestinationQueueName         The new destination queue for the messages in the same tenant.
     * @param destinationQueueName Dead Letter Queue name for the respective tenant.
     * @throws BrokerManagerAdminException
     */
    public void restoreMessagesFromDeadLetterQueueWithDifferentDestination(long[] messageIDs,
                                                                           String newDestinationQueueName,
                                                                           String destinationQueueName)
            throws BrokerManagerAdminException {
        try {
            QueueManagerService queueManagerService =
                    AndesBrokerManagerAdminServiceDSHolder.getInstance().getQueueManagerService();
            queueManagerService.restoreMessagesFromDeadLetterQueueWithDifferentDestination(messageIDs,
                    newDestinationQueueName, destinationQueueName);
        } catch (QueueManagerException e) {
            String errorMessage = e.getMessage();
            log.error(errorMessage, e);
            throw new BrokerManagerAdminException(errorMessage, e);
        }
    }

    /**
     * Delete messages from the Dead Letter Queue and delete their content.
     *
     * @param messageIDs          Browser Message Id / External Message Id list to be deleted.
     * @param destinationQueueName Dead Letter Queue name for the respective tenant.
     * @throws BrokerManagerAdminException
     */
    public void deleteMessagesFromDeadLetterQueue(long[] messageIDs, String destinationQueueName)
            throws BrokerManagerAdminException {
        try {
            QueueManagerService queueManagerService =
                    AndesBrokerManagerAdminServiceDSHolder.getInstance().getQueueManagerService();
            queueManagerService.deleteMessagesFromDeadLetterQueue(messageIDs, destinationQueueName);
        } catch (QueueManagerException e) {
            String errorMessage = e.getMessage();
            log.error(errorMessage, e);
            throw new BrokerManagerAdminException(errorMessage, e);
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
            String errorMessage = e.getMessage();
            log.error(errorMessage, e);
            throw new BrokerManagerAdminException(errorMessage, e);
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
					AndesBrokerManagerAdminServiceDSHolder.getInstance().getSubscriptionManagerService();
			subscriptionManagerService.closeSubscription(subscriptionID, destination, protocolType, destinationType);
		} catch (SubscriptionManagerException e) {
			String errorMessage = e.getMessage();
			log.error(errorMessage, e);
			throw new BrokerManagerAdminException(errorMessage, e);
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
            String errorMessage = e.getMessage();
            log.error(errorMessage, e);
            throw new BrokerManagerAdminException(errorMessage, e);
        }
        return subscriptionsDTO;
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
                    AndesBrokerManagerAdminServiceDSHolder.getInstance().getSubscriptionManagerService();
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
            String errorMessage = e.getMessage();
            log.error(errorMessage, e);
            throw new BrokerManagerAdminException(errorMessage, e);
        }
        return subscriptionsDTO;
    }

    /**
     * Retrieve subscriptions matching to the given search criteria.
     *
     * @param isDurable  are the subscriptions to be retrieve durable (true/ false)
     * @param isActive   are the subscriptions to be retrieved active (true/false)
     * @param protocolType  the protocol type of the subscriptions to be retrieved
     * @param destinationType the destination type of the subscriptions to be retrieved
     * @param filteredNamePattern queue or topic name pattern to search the subscriptions ("" for all)
     * @param isFilteredNameByExactMatch exactly match the name or not
     * @param identifierPattern  identifier pattern to search the subscriptions ("" for all)
     * @param isIdentifierPatternByExactMatch  exactly match the identifier or not
     * @param ownNodeId node Id of the node which own the subscriptions
     * @param pageNumber  page number in the pagination table
     * @param subscriptionCountPerPage  number of subscriptions to be shown in the UI per page
     * @return a list of subscriptions which match to the search criteria
     * @throws BrokerManagerAdminException throws when an error occurs
     */
    public Subscription[] getFilteredSubscriptions(boolean isDurable, boolean isActive, String protocolType,
                                                   String destinationType, String filteredNamePattern, boolean
                                                   isFilteredNameByExactMatch, String identifierPattern, boolean
                                                   isIdentifierPatternByExactMatch, String ownNodeId, int pageNumber,
                                                   int subscriptionCountPerPage) throws BrokerManagerAdminException {
        List<Subscription> allSubscriptions = new ArrayList<>();
        Subscription[] subscriptionsDTO;

        try {
            SubscriptionManagerService subscriptionManagerService =
                    AndesBrokerManagerAdminServiceDSHolder.getInstance().getSubscriptionManagerService();
            List<org.wso2.carbon.andes.core.types.Subscription> subscriptions = subscriptionManagerService
                    .getFilteredSubscriptions(isDurable, isActive, protocolType, destinationType,
                            filteredNamePattern, isFilteredNameByExactMatch, identifierPattern,
                            isIdentifierPatternByExactMatch, ownNodeId, pageNumber, subscriptionCountPerPage);
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
            String errorMessage = "An error occurred while retrieving subscriptions from backend " + e.getMessage();
            log.error(errorMessage, e);
            throw new BrokerManagerAdminException(errorMessage, e);
        }
        return subscriptionsDTO;
    }

    /**
     * Returns the total subscription count relevant to a particular search criteria.
     *
     * @param isDurable are the subscriptions to be retrieve durable (true/ false)
     * @param isActive are the subscriptions to be retrieved active (true/false)
     * @param protocolType the protocol type of the subscriptions to be retrieved
     * @param destinationType the destination type of the subscriptions to be retrieved
     * @param filteredNamePattern queue or topic name pattern to search the subscriptions ("" for all)
     * @param isFilteredNameByExactMatch exactly match the name or not
     * @param identifierPattern identifier pattern to search the subscriptions ("" for all)
     * @param isIdentifierPatternByExactMatch exactly match the identifier or not
     * @param ownNodeId node Id of the node which own the subscriptions
     * @return total subscription count matching to the given criteria
     * @throws BrokerManagerAdminException hrows when an error occurs
     */
    public int getTotalSubscriptionCountForSearchResult(boolean isDurable, boolean isActive, String protocolType,
                                                        String destinationType, String filteredNamePattern, boolean
                                                        isFilteredNameByExactMatch, String identifierPattern, boolean
                                                        isIdentifierPatternByExactMatch, String ownNodeId) throws
                                                        BrokerManagerAdminException {
        int subscriptionCountForSearchResult = 0;
        try {
            SubscriptionManagerService subscriptionManagerService =
                    AndesBrokerManagerAdminServiceDSHolder.getInstance().getSubscriptionManagerService();
            subscriptionCountForSearchResult = subscriptionManagerService
                    .getTotalSubscriptionCountForSearchResult(isDurable, isActive, protocolType, destinationType,
                            filteredNamePattern, isFilteredNameByExactMatch, identifierPattern,
                            isIdentifierPatternByExactMatch,ownNodeId);

        } catch (SubscriptionManagerException e) {
            String errorMessage = e.getMessage();
            log.error(errorMessage, e);
            throw new BrokerManagerAdminException(errorMessage, e);
        }
        return subscriptionCountForSearchResult;
    }


    /**
     * Gets the number of messages in DLC belonging to a specific queue.
     *
     * @param queueName The name of the queue.
     * @return The number of messages.
     * @throws BrokerManagerAdminException
     */
    public long getNumberOfMessagesInDLCForQueue(String queueName)
            throws BrokerManagerAdminException {
        QueueManagerService queueManagerService =
                AndesBrokerManagerAdminServiceDSHolder.getInstance().getQueueManagerService();
        try {
            return queueManagerService.getNumberOfMessagesInDLCForQueue(queueName);
        } catch (QueueManagerException e) {
            String errorMessage = e.getMessage();
            log.error(errorMessage, e);
            throw new BrokerManagerAdminException(errorMessage, e);
        }
    }

    /**
     * Gets the number of messages in DLC fora given queue name by start index and max messages
     * count
     *
     * @param queueName           Name of the queue
     * @param nextMessageIdToRead Start point of the queue message id to start reading
     * @param maxMsgCount         Maximum messages from start index
     * @return Array of {@link org.wso2.carbon.andes.admin.internal.Message}
     * @throws BrokerManagerAdminException
     */
    public Message[] getMessageInDLCForQueue(String queueName, long nextMessageIdToRead,
                                             int maxMsgCount) throws BrokerManagerAdminException {
        QueueManagerService queueManagerService = AndesBrokerManagerAdminServiceDSHolder.getInstance()
                .getQueueManagerService();
        List<Message> messageDTOList = new ArrayList<>();
        try {
            org.wso2.carbon.andes.core.types.Message[] messages =
                    queueManagerService.getMessageInDLCForQueue(queueName, nextMessageIdToRead, maxMsgCount);
            for (org.wso2.carbon.andes.core.types.Message message : messages) {
                Message messageDTO = new Message();
                messageDTO.setMsgProperties(message.getMsgProperties());
                messageDTO.setContentType(message.getContentType());
                messageDTO.setMessageContent(message.getMessageContent());
                messageDTO.setJMSMessageId(message.getJMSMessageId());
                messageDTO.setJMSReDelivered(message.getJMSReDelivered());
                // Delivery mode is received is not set when received from backend as its always persisted mode.
                messageDTO.setJMSDeliveredMode("null");
                messageDTO.setJMSTimeStamp(message.getJMSTimeStamp());
                messageDTO.setDlcMsgDestination(message.getDlcMsgDestination());
                messageDTO.setAndesMsgMetadataId(message.getAndesMsgMetadataId());
                messageDTOList.add(messageDTO);
            }
        } catch (QueueManagerException e) {
            String errorMessage = e.getMessage();
            log.error(errorMessage, e);
            throw new BrokerManagerAdminException(errorMessage, e);
        }
        return messageDTOList.toArray(new Message[messageDTOList.size()]);
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

                rolePermissions =
                        new org.wso2.carbon.andes.core.types.QueueRolePermission[queueRolePermissionsDTO.length];
                for (int i = 0; i < queueRolePermissionsDTO.length; i++) {
                    rolePermissions[i] = queueRolePermissionsDTO[i].convert();
                }
                queueManagerService.updatePermission(queueName, rolePermissions);
            }
        } catch (QueueManagerException e) {
            String errorMessage = e.getMessage();
            log.error(errorMessage, e);
            throw new BrokerManagerAdminException(errorMessage, e);
        }
    }

    /**
     * Add a queue with the given name and assign permissions
     *
     * @param queueName               Name of the queue
     * @param queueRolePermissionsDTO A {@link org.wso2.carbon.andes.admin.internal.QueueRolePermission}
     * @throws BrokerManagerAdminException
     */
    public void addQueueAndAssignPermission(String queueName, QueueRolePermission[] queueRolePermissionsDTO)
            throws BrokerManagerAdminException {
        QueueManagerService queueManagerService = AndesBrokerManagerAdminServiceDSHolder.getInstance()
                .getQueueManagerService();
        org.wso2.carbon.andes.core.types.QueueRolePermission[] rolePermissions;
        try {
            if (null != queueRolePermissionsDTO && queueRolePermissionsDTO.length > 0) {

                rolePermissions =
                        new org.wso2.carbon.andes.core.types.QueueRolePermission[queueRolePermissionsDTO.length];
                for (int i = 0; i < queueRolePermissionsDTO.length; i++) {
                    rolePermissions[i] = queueRolePermissionsDTO[i].convert();
                }
                queueManagerService.addQueueAndAssignPermission(queueName, rolePermissions);
            }
            else{
                queueManagerService.createQueue(queueName);
            }
        } catch (QueueManagerException e) {
            String errorMessage = e.getMessage();
            log.error(errorMessage, e);
            throw new BrokerManagerAdminException(errorMessage, e);
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
            String errorMessage = e.getMessage();
            log.error(errorMessage, e);
            throw new BrokerManagerAdminException(errorMessage, e);
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
            String errorMessage = e.getMessage();
            log.error(errorMessage, e);
            throw new BrokerManagerAdminException(errorMessage, e);
        }
    }

    /**
     * Browse the given queue name by start index and max messages count
     *
     * @param queueName     Name of the queue
     * @param nextMessageIdToRead Start point of the queue message id to start reading
     * @param maxMsgCount   Maximum messages from start index
     * @return Array of {@link org.wso2.carbon.andes.admin.internal.Message}
     * @throws BrokerManagerAdminException
     */
    public Message[] browseQueue(String queueName, long nextMessageIdToRead,
                                 int maxMsgCount) throws BrokerManagerAdminException {
        QueueManagerService queueManagerService = AndesBrokerManagerAdminServiceDSHolder.getInstance()
                .getQueueManagerService();
        List<Message> messageDTOList = new ArrayList<Message>();
        try {
            org.wso2.carbon.andes.core.types.Message[] messages =
                    queueManagerService.browseQueue(queueName, nextMessageIdToRead, maxMsgCount);
            for (org.wso2.carbon.andes.core.types.Message message : messages) {
                Message messageDTO = new Message();
                messageDTO.setMsgProperties(message.getMsgProperties());
                messageDTO.setContentType(message.getContentType());
                messageDTO.setMessageContent(message.getMessageContent());
                messageDTO.setJMSMessageId(message.getJMSMessageId());
                messageDTO.setJMSCorrelationId(message.getJMSCorrelationId());
                messageDTO.setJMSType(message.getJMSType());
                messageDTO.setJMSReDelivered(message.getJMSReDelivered());
                messageDTO.setMsgProperties(message.getMsgProperties());
                messageDTO.setJMSTimeStamp(message.getJMSTimeStamp());
                messageDTO.setJMSExpiration(message.getJMSExpiration());
                messageDTO.setDlcMsgDestination(message.getDlcMsgDestination());
                messageDTO.setAndesMsgMetadataId(message.getAndesMsgMetadataId());
                messageDTOList.add(messageDTO);
            }
        } catch (QueueManagerException e) {
            String errorMessage = e.getMessage();
            log.error(errorMessage, e);
            throw new BrokerManagerAdminException(errorMessage, e);
        }
        return messageDTOList.toArray(new Message[messageDTOList.size()]);
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
            String errorMessage = e.getMessage();
            log.error(errorMessage, e);
            throw new BrokerManagerAdminException(errorMessage, e);
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
            String errorMessage = e.getMessage();
            log.error(errorMessage, e);
            throw new BrokerManagerAdminException(errorMessage, e);
        }
    }

    /**
     * Gets the access key for a amqp url.
     * @return The access key.
     */
    private String getAccessKey() {
        return AndesBrokerManagerAdminServiceDSHolder.getInstance().getAccessKey();
    }

    /**
     * Gets the number of messages remaining for a subscriber.
     *
     * @param subscriptionID   The subscription ID.
     * @param durable          Whether the subscription is durable or not.
     * @param protocolType     The protocol type which the subscription belongs to
     * @param destinationType  The destination type which the subscription belongs to
     *
     * @return The number of remaining messages.
     * @throws BrokerManagerAdminException
     */
    public int getMessageCountForSubscriber(String subscriptionID, boolean durable, String protocolType,
                                            String destinationType) throws BrokerManagerAdminException {
        int remainingMessages = 0;
        try {
            SubscriptionManagerService subscriptionManagerService =
                    AndesBrokerManagerAdminServiceDSHolder.getInstance().getSubscriptionManagerService();
            List<org.wso2.carbon.andes.core.types.Subscription> subscriptions;
            if (durable) {
                subscriptions = subscriptionManagerService.getSubscriptions("true", "*", protocolType, destinationType);
            } else {
                subscriptions = subscriptionManagerService.getSubscriptions("false", "*", protocolType,
                        destinationType);
            }

            for (org.wso2.carbon.andes.core.types.Subscription subscription : subscriptions) {
                if (subscription.getSubscriptionIdentifier().equals(subscriptionID)) {
                    remainingMessages = subscription.getNumberOfMessagesRemainingForSubscriber();
                    break;
                }
            }
        } catch (SubscriptionManagerException e) {
            String errorMessage = e.getMessage();
            log.error("Admin service exception while getting message for subscriber "
                    + subscriptionID, e);
            throw new BrokerManagerAdminException(errorMessage, e);
        }

        return remainingMessages;
    }

    /**
     * Check if current user is has publish permission for a queue.
     * Suppressing "unused" warning as this is an API.
     *
     * @param queueName The name of the queue
     * @return true if current user has publishing permissions, false otherwise.
     * @throws BrokerManagerAdminException
     */
    @SuppressWarnings("unused")
    public boolean checkCurrentUserHasPublishPermission(String queueName) throws BrokerManagerAdminException {
        return checkUserHasPublishPermission(queueName, CarbonContext.getThreadLocalCarbonContext().getUsername());
    }

    /**
     * Check if a user has publishing permission for a queue.
     *
     * @param queueName The name of the queue
     * @param userName  The username of the user
     * @return true if current user has publishing permissions, false otherwise.
     * @throws BrokerManagerAdminException
     */
    public boolean checkUserHasPublishPermission(String queueName, String userName) throws BrokerManagerAdminException {
        boolean hasPermission = false;
        String queueID = CommonsUtil.getQueueID(queueName);
        try {
            if (Utils.isAdmin(userName)) {
                hasPermission = true;
            } else if (CarbonContext.getThreadLocalCarbonContext().getUserRealm().getAuthorizationManager()
                    .isUserAuthorized(userName, queueID, TreeNode.Permission.PUBLISH.toString().toLowerCase())) {
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
     * Check if current user has add permission. This is global level permission. Usage of service is to control UI
     * action.
     *
     * @return true/false based on permission
     * @throws BrokerManagerAdminException
     */
    public boolean checkCurrentUserHasAddQueuePermission() throws BrokerManagerAdminException {
        return checkUserHasAddQueuePermission(CarbonContext.getThreadLocalCarbonContext().getUsername());
    }

    /**
     * Check if given user has add permission. This is global level permission. Usage of service is to control UI
     * action.
     *
     * @param username username
     * @return true/false based on permission
     * @throws BrokerManagerAdminException
     */
    public boolean checkUserHasAddQueuePermission(String username) throws  BrokerManagerAdminException {
        boolean hasPermission = false;
        try {
            if (Utils.isAdmin(username)) {
                hasPermission = true;
            } else if (CarbonContext.getThreadLocalCarbonContext().getUserRealm().getAuthorizationManager()
                    .isUserAuthorized(username, PERMISSION_ADMIN_MANAGE_QUEUE_ADD_QUEUE, UI_EXECUTE)) {
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
     * Check if current user has browse permission. This is global level permission. Usage of service is to control UI
     * action.
     *
     * @return true/false based on permission
     * @throws BrokerManagerAdminException
     */
    public boolean checkCurrentUserHasBrowseQueuePermission() throws BrokerManagerAdminException {
        return checkUserHasBrowseQueuePermission(CarbonContext.getThreadLocalCarbonContext().getUsername());
    }

    /**
     * Check if given user has browse permission. This is global level permission. Usage of service is to control UI
     * action.
     *
     * @param username username
     * @return true/false based on permission
     * @throws BrokerManagerAdminException
     */
    public boolean checkUserHasBrowseQueuePermission(String username) throws  BrokerManagerAdminException {
        boolean hasPermission = false;
        try {
            if (Utils.isAdmin(username)) {
                hasPermission = true;
            } else if (CarbonContext.getThreadLocalCarbonContext().getUserRealm().getAuthorizationManager()
                    .isUserAuthorized(username, PERMISSION_ADMIN_MANAGE_BROWSE_QUEUE, UI_EXECUTE)) {
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
     * Check if current user has delete permission. This is global level permission. Usage of service is to control UI
     * action.
     *
     * @return true/false based on permission
     * @throws BrokerManagerAdminException
     */
    public boolean checkCurrentUserHasDeleteQueuePermission() throws BrokerManagerAdminException {
        return checkUserHasDeleteQueuePermission(CarbonContext.getThreadLocalCarbonContext().getUsername());
    }

    /**
     * Check if given user has delete permission. This is global level permission. Usage of service is to control UI
     * action.
     *
     * @param username username
     * @return true/false based on permission
     * @throws BrokerManagerAdminException
     */
    public boolean checkUserHasDeleteQueuePermission(String username) throws  BrokerManagerAdminException {
        boolean hasPermission = false;
        try {
            if (Utils.isAdmin(username)) {
                hasPermission = true;
            } else if (CarbonContext.getThreadLocalCarbonContext().getUserRealm().getAuthorizationManager()
                    .isUserAuthorized(username, PERMISSION_ADMIN_MANAGE_QUEUE_DELETE_QUEUE, UI_EXECUTE)) {
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
     * Check if current user has purge permission. This is global level permission. Usage of service is to control UI
     * action.
     *
     * @return true/false based on permission
     * @throws BrokerManagerAdminException
     */
    public boolean checkCurrentUserHasPurgeQueuePermission() throws BrokerManagerAdminException {
        return checkUserHasPurgeQueuePermission(CarbonContext.getThreadLocalCarbonContext().getUsername());
    }

    /**
     * Check if given user has purge permission. This is global level permission. Usage of service is to control UI
     * action.
     *
     * @param username username
     * @return true/false based on permission
     * @throws BrokerManagerAdminException
     */
    public boolean checkUserHasPurgeQueuePermission(String username) throws  BrokerManagerAdminException {
        boolean hasPermission = false;
        try {
            if (Utils.isAdmin(username)) {
                hasPermission = true;
            } else if (CarbonContext.getThreadLocalCarbonContext().getUserRealm().getAuthorizationManager()
                    .isUserAuthorized(username, PERMISSION_ADMIN_MANAGE_QUEUE_PURGE_QUEUE, UI_EXECUTE)) {
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
     * Check if current user has browse messages in dlc permission. This is global level permission. Usage of service
     * is to control UI action.
     *
     * @return true/false based on permission
     * @throws BrokerManagerAdminException
     */
    public boolean checkCurrentUserHasBrowseMessagesInDLCPermission() throws BrokerManagerAdminException {
        return checkUserHasBrowseMessagesInDLCPermission(CarbonContext.getThreadLocalCarbonContext().getUsername());
    }

    /**
     * Check if given user has browse messages in dlc permission. This is global level permission. Usage of service
     * is to control UI action.
     *
     * @param username username
     * @return true/false based on permission
     * @throws BrokerManagerAdminException
     */
    public boolean checkUserHasBrowseMessagesInDLCPermission(String username) throws  BrokerManagerAdminException {
        boolean hasPermission = false;
        try {
            if (Utils.isAdmin(username)) {
                hasPermission = true;
            } else if (CarbonContext.getThreadLocalCarbonContext().getUserRealm().getAuthorizationManager()
                    .isUserAuthorized(username, PERMISSION_ADMIN_MANAGE_BROWSE_DLC, UI_EXECUTE)) {
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
     * Check if current user has delete messages in dlc permission. This is global level permission. Usage of service
     * is to control UI action.
     *
     * @return true/false based on permission
     * @throws BrokerManagerAdminException
     */
    public boolean checkCurrentUserHasDeleteMessagesInDLCPermission() throws BrokerManagerAdminException {
        return checkUserHasDeleteMessagesInDLCPermission(CarbonContext.getThreadLocalCarbonContext().getUsername());
    }

    /**
     * Check if given user has delete messages in dlc permission. This is global level permission. Usage of service
     * is to control UI action.
     *
     * @param username username
     * @return true/false based on permission
     * @throws BrokerManagerAdminException
     */
    public boolean checkUserHasDeleteMessagesInDLCPermission(String username) throws  BrokerManagerAdminException {
        boolean hasPermission = false;
        try {
            if (Utils.isAdmin(username)) {
                hasPermission = true;
            } else if (CarbonContext.getThreadLocalCarbonContext().getUserRealm().getAuthorizationManager()
                    .isUserAuthorized(username, PERMISSION_ADMIN_MANAGE_DLC_DELETE_MESSAGE, UI_EXECUTE)) {
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
     * Check if current user has restore messages in dlc permission. This is global level permission. Usage of service
     * is to control UI action.
     *
     * @return true/false based on permission
     * @throws BrokerManagerAdminException
     */
    public boolean checkCurrentUserHasRestoreMessagesInDLCPermission() throws BrokerManagerAdminException {
        return checkUserHasRestoreMessagesInDLCPermission(CarbonContext.getThreadLocalCarbonContext().getUsername());
    }

    /**
     * Check if given user has restore messages in dlc permission. This is global level permission. Usage of service
     * is to control UI action.
     *
     * @param username username
     * @return true/false based on permission
     * @throws BrokerManagerAdminException
     */
    public boolean checkUserHasRestoreMessagesInDLCPermission(String username) throws  BrokerManagerAdminException {
        boolean hasPermission = false;
        try {
            if (Utils.isAdmin(username)) {
                hasPermission = true;
            } else if (CarbonContext.getThreadLocalCarbonContext().getUserRealm().getAuthorizationManager()
                    .isUserAuthorized(username, PERMISSION_ADMIN_MANAGE_DLC_RESTORE_MESSAGE, UI_EXECUTE)) {
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
     * Check if current user has reroute messages in dlc permission. This is global level permission. Usage of service
     * is to control UI action.
     *
     * @return true/false based on permission
     * @throws BrokerManagerAdminException
     */
    public boolean checkCurrentUserHasRerouteMessagesInDLCPermission() throws BrokerManagerAdminException {
        return checkUserHasRerouteMessagesInDLCPermission(CarbonContext.getThreadLocalCarbonContext().getUsername());
    }

    /**
     * Check if given user has reroute messages in dlc permission. This is global level permission. Usage of service
     * is to control UI action.
     *
     * @param username username
     * @return true/false based on permission
     * @throws BrokerManagerAdminException
     */
    public boolean checkUserHasRerouteMessagesInDLCPermission(String username) throws  BrokerManagerAdminException {
        boolean hasPermission = false;
        try {
            if (Utils.isAdmin(username)) {
                hasPermission = true;
            } else if (CarbonContext.getThreadLocalCarbonContext().getUserRealm().getAuthorizationManager()
                    .isUserAuthorized(username, PERMISSION_ADMIN_MANAGE_DLC_REROUTE_MESSAGE, UI_EXECUTE)) {
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
	 * Evaluate current logged in user has close subscription permission for queue subscriptions . This service mainly
	 * used to restrict UI
	 * control for un-authorize users
	 *
	 * @return true/false based on permission
	 * @throws BrokerManagerAdminException
	 */
	public boolean checkCurrentUserHasQueueSubscriptionClosePermission() throws BrokerManagerAdminException {
		boolean hasPermission = false;
		String username = getCurrentUser();
		try {
			if (Utils.isAdmin(username)) {
				hasPermission = true;
			} else if (CarbonContext.getThreadLocalCarbonContext().getUserRealm().getAuthorizationManager()
					.isUserAuthorized(username, PERMISSION_ADMIN_MANAGE_QUEUE_SUBSCRIPTION_CLOSE, UI_EXECUTE)) {
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
