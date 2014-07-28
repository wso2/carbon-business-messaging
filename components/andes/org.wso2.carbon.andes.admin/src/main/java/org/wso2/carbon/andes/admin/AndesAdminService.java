/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
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

public class AndesAdminService extends AbstractAdmin {
    private static Log log = LogFactory.getLog(AndesAdminService.class);


    public void createQueue(String queueName) throws BrokerManagerAdminException {//done
        QueueManagerService queueManagerService = AndesBrokerManagerAdminServiceDSHolder.getInstance().getQueueManagerService();
        try {
            queueManagerService.createQueue(queueName);
        } catch (QueueManagerException e) {
            throw new BrokerManagerAdminException("Error in creating the queue. " + e.getMessage(), e);
        }
    }

    public org.wso2.carbon.andes.admin.internal.Queue[] getAllQueues() throws BrokerManagerAdminException {//done
        List<org.wso2.carbon.andes.admin.internal.Queue> allQueues
                = new ArrayList<org.wso2.carbon.andes.admin.internal.Queue>();
        org.wso2.carbon.andes.admin.internal.Queue[] queuesDTO = null;
        try {
            QueueManagerService queueManagerService =
                    AndesBrokerManagerAdminServiceDSHolder.getInstance().getQueueManagerService();
            List<org.wso2.carbon.andes.core.types.Queue> queues = queueManagerService.getAllQueues();
            queuesDTO = new org.wso2.carbon.andes.admin.internal.Queue[queues.size()];
            for (org.wso2.carbon.andes.core.types.Queue queue : queues) {
                org.wso2.carbon.andes.admin.internal.Queue queueDTO = new org.wso2.carbon.andes.admin.internal.Queue();
                queueDTO.setQueueName(queue.getQueueName());
                queueDTO.setMessageCount(queue.getMessageCount());
                queueDTO.setCreatedTime(queue.getCreatedTime());
                queueDTO.setUpdatedTime(queue.getUpdatedTime());
                allQueues.add(queueDTO);
            }
            CustomQueueComparator comparator = new CustomQueueComparator();
            Collections.sort(allQueues,Collections.reverseOrder(comparator));
            allQueues.toArray(queuesDTO);
        } catch (QueueManagerException e) {
            throw new BrokerManagerAdminException("Problem in getting queues from back end", e);
        }
        return queuesDTO;
    }

    public long getMessageCountForQueue(String queueName,String msgPattern) throws BrokerManagerAdminException {//done
        long messageCount = 0;
        try {
            QueueManagerService queueManagerService =
                    AndesBrokerManagerAdminServiceDSHolder.getInstance().getQueueManagerService();
            messageCount = queueManagerService.getMessageCountForQueue(queueName,msgPattern);
            return messageCount;
        } catch (Exception e) {
            throw new BrokerManagerAdminException("Error while getting message count for queue", e);
        }
    }

    public void deleteQueue(String queueName) throws BrokerManagerAdminException {//done
        try {
            QueueManagerService queueManagerService =
                    AndesBrokerManagerAdminServiceDSHolder.getInstance().getQueueManagerService();
            queueManagerService.deleteQueue(queueName);
        } catch (QueueManagerException e) {
            String message = e.getMessage();
            throw new BrokerManagerAdminException("Error in deleting queue. "+message, e);
        }

    }

    public void restoreMessagesFromDeadLetterQueue(String[] messageIDs) throws Exception {//done
        try {
            QueueManagerService queueManagerService =
                    AndesBrokerManagerAdminServiceDSHolder.getInstance().getQueueManagerService();
            queueManagerService.restoreMessagesFromDeadLetterQueue(messageIDs);
        } catch (QueueManagerException e) {
            String message = e.getMessage();
            throw new BrokerManagerAdminException("Error in restoring message from dead letter queue. " + message, e);
        }
    }

    public void restoreMessagesFromDeadLetterQueueWithDifferentDestination(String[] messageIDs, String destination) throws Exception {//done
        try {
            QueueManagerService queueManagerService =
                    AndesBrokerManagerAdminServiceDSHolder.getInstance().getQueueManagerService();
            queueManagerService.restoreMessagesFromDeadLetterQueueWithDifferentDestination(messageIDs, destination);
        } catch (QueueManagerException e) {
            String message = e.getMessage();
            throw new BrokerManagerAdminException("Error in restoring message from dead letter queue. " + message, e);
        }
    }

    public void deleteMessagesFromDeadLetterQueue(String[] messageIDs) throws Exception {//done
        try {
            QueueManagerService queueManagerService =
                    AndesBrokerManagerAdminServiceDSHolder.getInstance().getQueueManagerService();
            queueManagerService.deleteMessagesFromDeadLetterQueue(messageIDs);
        } catch (QueueManagerException e) {
            String message = e.getMessage();
            throw new BrokerManagerAdminException("Error in deleting message from queue. " + message, e);
        }
    }

    public void purgeMessagesOfQueue(String queueName) throws Exception {//done
        try {
            QueueManagerService queueManagerService =
                    AndesBrokerManagerAdminServiceDSHolder.getInstance().getQueueManagerService();
            queueManagerService.purgeMessagesOfQueue(queueName);
        } catch (QueueManagerException e) {
            String message = e.getMessage();
            throw new BrokerManagerAdminException("Error in purging message from queue. " + message, e);
        }
    }



    public Subscription[] getAllSubscriptions() throws BrokerManagerAdminException {//done
        List<Subscription> allSubscriptions= new ArrayList<Subscription>();
        Subscription[] subscriptionsDTO = null;
        try {
            SubscriptionManagerService subscriptionManagerService =
                    AndesBrokerManagerAdminServiceDSHolder.getInstance().getSubscriptionManagerService();
            List<org.wso2.carbon.andes.core.types.Subscription> subscriptions = subscriptionManagerService.getAllSubscriptions();
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
            Collections.sort(allSubscriptions,Collections.reverseOrder(comparator));
            allSubscriptions.toArray(subscriptionsDTO);
        } catch (SubscriptionManagerException e) {
            throw new BrokerManagerAdminException("Problem in getting subscriptions from back end", e);
        }
        return subscriptionsDTO;
    }

    public Subscription[] getAllDurableQueueSubscriptions() throws BrokerManagerAdminException {//done
        List<Subscription> allSubscriptions= new ArrayList<Subscription>();
        Subscription[] subscriptionsDTO = null;
        try {
            SubscriptionManagerService subscriptionManagerService =
                    AndesBrokerManagerAdminServiceDSHolder.getInstance().getSubscriptionManagerService();
            List<org.wso2.carbon.andes.core.types.Subscription> subscriptions = subscriptionManagerService.getAllDurableQueueSubscriptions();
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
            Collections.sort(allSubscriptions,Collections.reverseOrder(comparator));
            allSubscriptions.toArray(subscriptionsDTO);
        } catch (SubscriptionManagerException e) {
            throw new BrokerManagerAdminException("Problem in getting subscriptions from back end", e);
        }
        return subscriptionsDTO;
    }


    public Subscription[] getAllLocalTempQueueSubscriptions() throws BrokerManagerAdminException {//done
        List<Subscription> allSubscriptions= new ArrayList<Subscription>();
        Subscription[] subscriptionsDTO = null;
        try {
            SubscriptionManagerService subscriptionManagerService =
                    AndesBrokerManagerAdminServiceDSHolder.getInstance().getSubscriptionManagerService();
            List<org.wso2.carbon.andes.core.types.Subscription> subscriptions = subscriptionManagerService.getAllLocalTempQueueSubscriptions();
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
            Collections.sort(allSubscriptions,Collections.reverseOrder(comparator));
            allSubscriptions.toArray(subscriptionsDTO);
        } catch (SubscriptionManagerException e) {
            throw new BrokerManagerAdminException("Problem in getting subscriptions from back end", e);
        }
        return subscriptionsDTO;
    }

    public Subscription[] getAllDurableTopicSubscriptions() throws BrokerManagerAdminException {//done
        List<Subscription> allSubscriptions= new ArrayList<Subscription>();
        Subscription[] subscriptionsDTO = null;
        try {
            SubscriptionManagerService subscriptionManagerService =
                    AndesBrokerManagerAdminServiceDSHolder.getInstance().getSubscriptionManagerService();
            List<org.wso2.carbon.andes.core.types.Subscription> subscriptions = subscriptionManagerService.getAllDurableTopicSubscriptions();
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
            Collections.sort(allSubscriptions,Collections.reverseOrder(comparator));
            allSubscriptions.toArray(subscriptionsDTO);
        } catch (SubscriptionManagerException e) {
            throw new BrokerManagerAdminException("Problem in getting subscriptions from back end", e);
        }
        return subscriptionsDTO;
    }


    public Subscription[] getAllLocalTempTopicSubscriptions() throws BrokerManagerAdminException {//done
        List<Subscription> allSubscriptions= new ArrayList<Subscription>();
        Subscription[] subscriptionsDTO = null;
        try {
            SubscriptionManagerService subscriptionManagerService =
                    AndesBrokerManagerAdminServiceDSHolder.getInstance().getSubscriptionManagerService();
            List<org.wso2.carbon.andes.core.types.Subscription> subscriptions = subscriptionManagerService.getAllLocalTempTopicSubscriptions();
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
            Collections.sort(allSubscriptions,Collections.reverseOrder(comparator));
            allSubscriptions.toArray(subscriptionsDTO);
        } catch (SubscriptionManagerException e) {
            throw new BrokerManagerAdminException("Problem in getting subscriptions from back end", e);
        }
        return subscriptionsDTO;
    }

    /**
     * Update the permission of the given queue name
     *
     * @param queueName - Name of the queue
     * @param queueRolePermissionsDTO {@link org.wso2.carbon.andes.admin.internal.QueueRolePermission}
     * @throws BrokerManagerAdminException
     */
    public void updatePermission(String queueName, QueueRolePermission[] queueRolePermissionsDTO) throws BrokerManagerAdminException {//done
        QueueManagerService queueManagerService = AndesBrokerManagerAdminServiceDSHolder.getInstance().getQueueManagerService();
        org.wso2.carbon.andes.core.types.QueueRolePermission[] rolePermissions;
        try {
            if(queueRolePermissionsDTO != null && queueRolePermissionsDTO.length > 0){
                List<org.wso2.carbon.andes.core.types.QueueRolePermission> permissionList = new ArrayList<org.wso2.carbon.andes.core.types.QueueRolePermission>();
                for (QueueRolePermission queueRolePermission : queueRolePermissionsDTO) {
                    org.wso2.carbon.andes.core.types.QueueRolePermission permission = new org.wso2.carbon.andes.core.types.QueueRolePermission();
                    permission.setRoleName(queueRolePermission.getRoleName());
                    permission.setAllowedToConsume(queueRolePermission.isAllowedToConsume());
                    permission.setAllowedToPublish(queueRolePermission.isAllowedToPublish());
                    permissionList.add(permission);
                }
                rolePermissions = new org.wso2.carbon.andes.core.types.QueueRolePermission[permissionList.size()];
                permissionList.toArray(rolePermissions);
                queueManagerService.updatePermission(queueName, rolePermissions);
            }
        } catch (QueueManagerException e) {
            throw new BrokerManagerAdminException("Unable to update permission of the queue. "+e.getMessage(), e);
        }
    }

    /**
     * Get roles of the current logged user
     * If user has admin role, then all available roles will be return
     *
     * @return Array of user roles
     * @throws BrokerManagerAdminException
     */
    public String[] getUserRoles() throws BrokerManagerAdminException {//done
        String[] roles;
        QueueManagerService queueManagerService = AndesBrokerManagerAdminServiceDSHolder.getInstance().getQueueManagerService();
        try {
            roles = queueManagerService.getBackendRoles();
        } catch (QueueManagerException e) {
            throw new BrokerManagerAdminException("Unable to get roles from user store. "+e.getMessage(), e);
        }
        return roles;
    }

    /**
     * Get all permission of the given queue name
     *
     * @param queueName - Name of the queue
     * @return Array of {@link org.wso2.carbon.andes.admin.internal.QueueRolePermission}
     * @throws BrokerManagerAdminException
     */
    public QueueRolePermission[] getQueueRolePermission(String queueName) throws BrokerManagerAdminException {//done
        QueueManagerService queueManagerService = AndesBrokerManagerAdminServiceDSHolder.getInstance().getQueueManagerService();
        List<QueueRolePermission> queueRolePermissionDTOList = new ArrayList<QueueRolePermission>();
        try {
            org.wso2.carbon.andes.core.types.QueueRolePermission[] queueRolePermission = queueManagerService.getQueueRolePermission(queueName);
            for (org.wso2.carbon.andes.core.types.QueueRolePermission rolePermission : queueRolePermission) {
                QueueRolePermission queueRolePermissionDTO = new QueueRolePermission();
                queueRolePermissionDTO.setRoleName(rolePermission.getRoleName());
                queueRolePermissionDTO.setAllowedToConsume(rolePermission.isAllowedToConsume());
                queueRolePermissionDTO.setAllowedToPublish(rolePermission.isAllowedToPublish());
                queueRolePermissionDTOList.add(queueRolePermissionDTO);
            }
            return queueRolePermissionDTOList.toArray(new QueueRolePermission[queueRolePermissionDTOList.size()]);
        } catch (QueueManagerException e) {
            throw new BrokerManagerAdminException("Unable to retrieve queue permission. "+e.getMessage(), e);
        }
    }

    /**
     * Browse the given queue name by start index and max messages count
     *
     * @param queueName - Name of the queue
     * @param startingIndex - Start point of the queue messages
     * @param maxMsgCount - Maximum messages from start index
     * @return Array of {@link org.wso2.carbon.andes.admin.internal.Message}
     * @throws BrokerManagerAdminException
     */
    public Message[] browseQueue(String queueName, int startingIndex, int maxMsgCount) throws BrokerManagerAdminException {//done
        QueueManagerService queueManagerService = AndesBrokerManagerAdminServiceDSHolder.getInstance().getQueueManagerService();
        List<Message> messageDTOList = new ArrayList<Message>();
        try {
            org.wso2.carbon.andes.core.types.Message[] messages = queueManagerService.browseQueue(queueName, getCurrentUser(), getAccessKey(), startingIndex, maxMsgCount);
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
            log.error("Unable to browse queue. " + e.getMessage());
            throw new BrokerManagerAdminException("Unable to browse queue. "+e.getMessage(), e);
        }
    }

    /**
     * Total messages in the given queue
     *
     * @param queueName - Name of the queue
     * @return Total count of the messages
     * @throws BrokerManagerAdminException
     */
    public long getTotalMessagesInQueue(String queueName) throws BrokerManagerAdminException {//done
        QueueManagerService queueManagerService = AndesBrokerManagerAdminServiceDSHolder.getInstance().getQueueManagerService();
        try {
            return queueManagerService.getTotalMessagesInQueue(queueName);
        } catch (QueueManagerException e) {
            throw new BrokerManagerAdminException("Unable to get total message count. "+e.getMessage(), e);
        }
    }

    /**
     * Send messages to given queue
     *
     * @param queueName - Name of the queue
     * @param jmsType - JMS Type
     * @param jmsCorrelationID - JMS Correlation Id
     * @param numberOfMessages - Number of times
     * @param message - Message content
     * @param deliveryMode - Delivery mode
     * @param priority - Message priority
     * @param expireTime - Message expire time
     * @return
     * @throws BrokerManagerAdminException
     */
    public boolean sendMessage(String queueName, String jmsType, String jmsCorrelationID, int numberOfMessages, String message,
                               int deliveryMode, int priority, long expireTime)
            throws BrokerManagerAdminException {//done
        QueueManagerService queueManagerService = AndesBrokerManagerAdminServiceDSHolder.getInstance().getQueueManagerService();
        try {
            return queueManagerService.sendMessage(queueName, getCurrentUser(), getAccessKey(), jmsType, jmsCorrelationID,
                    numberOfMessages, message, deliveryMode, priority, expireTime);
        } catch (QueueManagerException e) {
            throw new BrokerManagerAdminException("Unable to send message. "+e.getMessage(), e);
        }
    }

    public String getAccessKey(){//done
        String accessKey = AndesBrokerManagerAdminServiceDSHolder.getInstance().getAccessKey();
        return accessKey;
    }

    private String getCurrentUser(){
        String userName = "";
        if (CarbonContext.getThreadLocalCarbonContext().getTenantId() != 0) {
            userName = CarbonContext.getThreadLocalCarbonContext().getUsername() + "!"
                    + CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        } else {
            userName = CarbonContext.getThreadLocalCarbonContext().getUsername();
        }
        return userName.trim();
    }

    public class CustomQueueComparator implements Comparator<Queue> {

        public int compare(Queue queue1, Queue queue2) {
            return (int) (queue1.getMessageCount()-queue2.getMessageCount());
        }
    }

    public class CustomSubscriptionComparator implements Comparator<Subscription> {

        public int compare(Subscription sub1, Subscription sub2) {
            return (int) (sub1.getNumberOfMessagesRemainingForSubscriber()-sub2.getNumberOfMessagesRemainingForSubscriber());
        }
    }
}
