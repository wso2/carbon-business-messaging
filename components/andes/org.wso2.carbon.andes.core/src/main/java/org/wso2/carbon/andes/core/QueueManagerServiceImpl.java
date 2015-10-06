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
package org.wso2.carbon.andes.core;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.andes.management.common.mbeans.QueueManagementInformation;
import org.wso2.andes.server.queue.DLCQueueUtils;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.andes.commons.CommonsUtil;
import org.wso2.carbon.andes.commons.registry.RegistryClient;
import org.wso2.carbon.andes.commons.registry.RegistryClientException;
import org.wso2.carbon.andes.core.internal.ds.QueueManagerServiceValueHolder;
import org.wso2.carbon.andes.core.internal.registry.QueueManagementBeans;
import org.wso2.carbon.andes.core.internal.util.QueueManagementConstants;
import org.wso2.carbon.andes.core.internal.util.Utils;
import org.wso2.carbon.andes.core.types.Message;
import org.wso2.carbon.andes.core.types.QueueRolePermission;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.api.UserStoreManager;
import org.wso2.carbon.user.core.authorization.TreeNode;
import org.wso2.carbon.user.core.util.UserCoreUtil;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.openmbean.CompositeData;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.xml.stream.XMLStreamException;
import java.io.FileNotFoundException;
import java.lang.management.ManagementFactory;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Works as the manager class for queue related tasks done from UI. (create queue, delete queue,
 * browse queue, getAllQueues etc.)
 */
public class QueueManagerServiceImpl implements QueueManagerService {

    private static Log log = LogFactory.getLog(QueueManagerServiceImpl.class);
    private static final String PERMISSION_CHANGE_PERMISSION = "changePermission";
    private static final String ROLE_EVERY_ONE = "everyone";
    private static final String ANDES_ICF = "org.wso2.andes.jndi.PropertiesFileInitialContextFactory";
    private static final String CF_NAME_PREFIX = "connectionfactory.";
    private static final String QUEUE_NAME_PREFIX = "queue.";
    private static final String CF_NAME = "qpidConnectionfactory";

    private static final String AT_REPLACE_CHAR = "_";
    private static final String QUEUE_ROLE_PREFIX = "Q_";
    /**
     * Tenant domain name separator
     */
    public static final String DOMAIN_NAME_SEPARATOR = "!";

    /**
     * {@inheritDoc}
     */
    @Override
    public void createQueue(String queueName) throws QueueManagerException {
        try {
            String tenantBasedQueueName = Utils.getTenantBasedQueueName(queueName);
            String userName = getLoggedInUserName();
            if (!QueueManagementBeans.queueExists(tenantBasedQueueName)) {
                RegistryClient.createQueue(tenantBasedQueueName, userName);
                QueueManagementBeans.getInstance().createQueue(tenantBasedQueueName, userName);
                //Adding change permissions to the current logged in user
                UserRealm userRealm =
                        QueueManagerServiceValueHolder.getInstance().getRealmService().getTenantUserRealm
                                (CarbonContext.getThreadLocalCarbonContext().getTenantId() <= 0 ?
                                 MultitenantConstants.SUPER_TENANT_ID : CarbonContext
                                        .getThreadLocalCarbonContext()
                                        .getTenantId());

                String queueID = CommonsUtil.getQueueID(queueName);
                authorizePermissionsToLoggedInUser(tenantBasedQueueName, queueID, userRealm);

            } else {
                // TODO : Can we use error code for cleaner error handling ? this will hard bind to
                //
                // the error message.
                // Queue exists in the system.
                throw new QueueManagerException("Queue with the name: " + queueName + " already exists!");
            }
        } catch (UserStoreException | RegistryClientException e) {
            throw new QueueManagerException("Error in creating the queue : " + queueName, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public org.wso2.carbon.andes.core.types.Queue getQueueByName(String queueName) throws QueueManagerException {

        if (QueueManagementBeans.getInstance().queueExists(queueName)) {

            // create a queue with the message count
            org.wso2.carbon.andes.core.types.Queue queue = new org.wso2.carbon.andes.core.types.Queue(queueName);
            queue.setMessageCount(QueueManagementBeans.getInstance().getMessageCount(queueName, "queue"));

            //show queue only if it belongs to the current tenant domain of user
            if (Utils.isQueueInDomain(queue)) {
                return queue;
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public org.wso2.carbon.andes.core.types.Queue getDLCQueue(String tenantDomain) throws QueueManagerException {
        return getQueueByName(DLCQueueUtils.generateDLCQueueNameFromTenant(tenantDomain));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<org.wso2.carbon.andes.core.types.Queue> getAllQueues() throws QueueManagerException {
        UserRealm userRealm;
        List<org.wso2.carbon.andes.core.types.Queue> allQueues = QueueManagementBeans.getInstance().getAllQueueCounts();
        //show queues belonging to current domain
        return Utils.filterDomainSpecificQueues(allQueues);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteQueue(String queueName) throws QueueManagerException {
        try {
            UserRegistry userRegistry = Utils.getUserRegistry();
            String resourcePath = QueueManagementConstants.MB_QUEUE_STORAGE_PATH + "/" + queueName;
            if (QueueManagementBeans.queueExists(queueName)) {
                QueueManagementBeans.getInstance().deleteQueue(queueName);
                userRegistry.delete(resourcePath);
            }

            removeRoleCreatedForLoggedInUser(queueName);
        } catch (RegistryException e) {
            throw new QueueManagerException("Failed to delete queue : " + queueName, e);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteTopicFromRegistry(String topicName, String subscriptionId) throws QueueManagerException {
        try {
            UserRegistry userRegistry = Utils.getUserRegistry();

            String resourcePathForQueue = QueueManagementConstants.MB_QUEUE_STORAGE_PATH + "/" +
                    subscriptionId.split(":")[1];
            String resourcePathForTopic = CommonsUtil.getSubscriptionID(topicName,
                    subscriptionId.split(":")[1]);
            userRegistry.delete(resourcePathForTopic);
            userRegistry.delete(resourcePathForQueue);

        } catch (RegistryException e) {
            String message = e.getMessage();
            throw new QueueManagerException("Failed to delete topic: " + topicName + " from " +
                    "registry " +
                    message, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void restoreMessagesFromDeadLetterQueue(long[] messageIDs, String destinationQueueName)
            throws
            QueueManagerException {
        QueueManagementBeans.getInstance().restoreMessagesFromDeadLetterQueue(messageIDs, destinationQueueName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void restoreMessagesFromDeadLetterQueueWithDifferentDestination(long[] messageIDs,
                                                                           String newDestinationQueueName,
                                                                           String destinationQueueName)
            throws
            QueueManagerException {
        QueueManagementBeans.getInstance().restoreMessagesFromDeadLetterQueueWithDifferentDestination(messageIDs,
                newDestinationQueueName, destinationQueueName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteMessagesFromDeadLetterQueue(long[] messageIDs, String destinationQueueName)
            throws
            QueueManagerException {
        QueueManagementBeans.getInstance().deleteMessagesFromDeadLetterQueue(messageIDs, destinationQueueName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void purgeMessagesOfQueue(String queueName) throws
                                                       QueueManagerException {

        QueueManagementBeans.getInstance().purgeMessagesFromQueue(queueName, CarbonContext.getThreadLocalCarbonContext()
                .getUsername());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getMessageCount(String destinationName, String msgPattern)
            throws QueueManagerException {
        long messageCount;
        messageCount = QueueManagementBeans.getInstance().getMessageCount(destinationName, msgPattern);
        return messageCount;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updatePermission(String queueName, QueueRolePermission[] queueRolePermissions)
            throws QueueManagerException {
        String tenantBasedQueueName = Utils.getTenantBasedQueueName(queueName);

        if (QueueManagementBeans.queueExists(tenantBasedQueueName)) {
            String queueID = CommonsUtil.getQueueID(queueName);
            UserRealm userRealm;
            String role;
            String loggedInUser = CarbonContext.getThreadLocalCarbonContext().getUsername();
            try {
                userRealm = QueueManagerServiceValueHolder.getInstance().getRealmService().getTenantUserRealm
                        (CarbonContext.getThreadLocalCarbonContext().getTenantId() <= 0 ?
                         MultitenantConstants.SUPER_TENANT_ID : CarbonContext.getThreadLocalCarbonContext()
                                .getTenantId());
                boolean isUserHasChangePermission = false;
                if (Utils.isAdmin(loggedInUser)) {
                    isUserHasChangePermission = true;
                } else {
                    String[] userRoles = userRealm.getUserStoreManager().getRoleListOfUser(loggedInUser);
                    for (String userRole : userRoles) {
                        if (userRealm.getAuthorizationManager().isRoleAuthorized(
                                userRole, queueID, PERMISSION_CHANGE_PERMISSION)) {
                            isUserHasChangePermission = true;
                        }
                    }
                }
                if (!isUserHasChangePermission) {
                    throw new QueueManagerException(" User " + loggedInUser + " can not change" +
                            " the permissions of " + queueName);
                }

                for (QueueRolePermission queueRolePermission : queueRolePermissions) {
                    role = queueRolePermission.getRoleName();
                    if (queueRolePermission.isAllowedToConsume()) {
                        userRealm.getAuthorizationManager().authorizeRole(
                                role, queueID, TreeNode.Permission.CONSUME.toString().toLowerCase());
                    } else {
                        userRealm.getAuthorizationManager().denyRole(
                                role, queueID, TreeNode.Permission.CONSUME.toString().toLowerCase());
                    }
                    if (queueRolePermission.isAllowedToPublish()) {
                        userRealm.getAuthorizationManager().authorizeRole(
                                role, queueID, TreeNode.Permission.PUBLISH.toString().toLowerCase());
                    } else {
                        userRealm.getAuthorizationManager().denyRole(
                                role, queueID, TreeNode.Permission.PUBLISH.toString().toLowerCase());
                    }
                }
            } catch (UserStoreException e) {
                throw new QueueManagerException("Unable to update permission of the queue.", e);
            }

        } else {
            throw new QueueManagerException("Queue with the name: " + queueName + " not already exists!");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addQueueAndAssignPermission(String queueName, QueueRolePermission[] queueRolePermissions)
            throws QueueManagerException {
        String tenantBasedQueueName = Utils.getTenantBasedQueueName(queueName);

        if (!QueueManagementBeans.queueExists(tenantBasedQueueName)) {

            // create a queue with the provided name and assign permissions for the current user as well as for the
            // explicitly provided roles
            createQueue(tenantBasedQueueName);
            updatePermission(tenantBasedQueueName, queueRolePermissions);

        } else {
            throw new QueueManagerException("Queue with the name: " + queueName + " already exists!");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getBackendRoles() throws QueueManagerException {
        UserRealm userRealm;
        try {
            userRealm = QueueManagerServiceValueHolder.getInstance().getRealmService().getTenantUserRealm
                    (CarbonContext.getThreadLocalCarbonContext().getTenantId() <= 0 ?
                     MultitenantConstants.SUPER_TENANT_ID : CarbonContext.getThreadLocalCarbonContext()
                            .getTenantId());
            //Get the admin role
            String adminRole = QueueManagerServiceValueHolder.getInstance().getRealmService().getBootstrapRealm()
                    .getRealmConfiguration().getAdminRoleName();
            UserStoreManager userStoreManager = userRealm.getUserStoreManager();
            //Get all the roles of the logged in user
            String[] roleNames = userStoreManager.getRoleListOfUser(CarbonContext.getThreadLocalCarbonContext()
                    .getUsername());
            //Check current user has admin role
            String[] rolesExceptAdminRole = null;
            boolean adminRoleExistInAllRoles = false;
            if (Utils.isAdmin(CarbonContext.getThreadLocalCarbonContext().getUsername())) {
                String[] allRoles = userRealm.getUserStoreManager().getRoleNames();
                for (String aRole : allRoles) {
                    if (adminRole.equals(aRole)) {
                        adminRoleExistInAllRoles = true;
                    }
                }
                if (allRoles.length > 1) {
                    if (adminRoleExistInAllRoles) {
                        rolesExceptAdminRole = new String[allRoles.length - 1];
                    } else {
                        rolesExceptAdminRole = new String[allRoles.length];
                    }
                    int index = 0;
                    for (String role : allRoles) {
                        if (!(role.equals(adminRole) || CarbonConstants.REGISTRY_ANONNYMOUS_ROLE_NAME.equals(role))) {
                            rolesExceptAdminRole[index] = role;
                            index++;
                        }
                    }
                }
            } else {
                if (roleNames != null && roleNames.length > 0) {
                    rolesExceptAdminRole = new String[roleNames.length];
                    int index = 0;
                    for (String role : roleNames) {
                        rolesExceptAdminRole[index] = role;
                        index++;
                    }
                }
            }
            if (rolesExceptAdminRole != null && rolesExceptAdminRole.length > 0) {
                return rolesExceptAdminRole;
            } else {
                return new String[0];
            }
        } catch (UserStoreException e) {
            throw new QueueManagerException("Unable to get roles from user store.", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public QueueRolePermission[] getQueueRolePermission(String queueName) throws QueueManagerException {
        String tenantBasedQueueName = Utils.getTenantBasedQueueName(queueName);
        if (QueueManagementBeans.queueExists(tenantBasedQueueName)) {
            queueName = CarbonContext.getThreadLocalCarbonContext().getTenantId() <= 0 ?
                        queueName : queueName.replace(CarbonContext.getThreadLocalCarbonContext().getTenantDomain() +
                                                      "/", "");
            String queueID = CommonsUtil.getQueueID(queueName);
            UserRealm userRealm;
            List<QueueRolePermission> queueRolePermissions = new ArrayList<>();
            try {
                userRealm = QueueManagerServiceValueHolder.getInstance().getRealmService().getTenantUserRealm
                        (CarbonContext.getThreadLocalCarbonContext().getTenantId() <= 0 ?
                         MultitenantConstants.SUPER_TENANT_ID : CarbonContext.getThreadLocalCarbonContext()
                                .getTenantId());
                String adminRole = QueueManagerServiceValueHolder.getInstance().getRealmService().getBootstrapRealm()
                        .getRealmConfiguration().getAdminRoleName();
                for (String role : userRealm.getUserStoreManager().getRoleNames()) {
                    if (!(role.equals(adminRole) ||
                          CarbonConstants.REGISTRY_ANONNYMOUS_ROLE_NAME.equals(role))) {
                        QueueRolePermission queueRolePermission = new QueueRolePermission(role,
                                userRealm.getAuthorizationManager().isRoleAuthorized(
                                        role, queueID, TreeNode.Permission.CONSUME.toString().toLowerCase()),
                                userRealm.getAuthorizationManager().isRoleAuthorized(
                                        role, queueID, TreeNode.Permission.PUBLISH.toString().toLowerCase()));
                        queueRolePermissions.add(queueRolePermission);
                    }
                }
                return queueRolePermissions.toArray(new QueueRolePermission[queueRolePermissions.size()]);
            } catch (UserStoreException e) {
                throw new QueueManagerException("Unable to retrieve permission of the queue.", e);
            }
        } else {
            throw new QueueManagerException("Queue with the name: " + queueName + " not already exists!");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public org.wso2.carbon.andes.core.types.Message[] browseQueue(String nameOfQueue,
                                                                  long nextMessageIdToRead, int maxMsgCount)
            throws QueueManagerException {

        List<org.wso2.carbon.andes.core.types.Message> messageList =
                QueueManagementBeans.getInstance().browseQueue(nameOfQueue, nextMessageIdToRead, maxMsgCount);

        return messageList.toArray(new org.wso2.carbon.andes.core.types.Message[messageList.size()]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getTotalMessagesInQueue(String nameOfQueue) throws QueueManagerException {
        return QueueManagementBeans.getInstance().getMessageCount(nameOfQueue, "queue");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean sendMessage(String nameOfQueue, String userName, String accessKey,
                               String jmsType,
                               String jmsCorrelationID,
                               int numberOfMessages, String message, int deliveryMode, int priority,
                               long expireTime) throws QueueManagerException {
        UserRealm userRealm;
        String queueID = CommonsUtil.getQueueID(nameOfQueue);
        boolean isSend = false;
        try {
            userRealm = QueueManagerServiceValueHolder.getInstance().getRealmService().getTenantUserRealm
                    (CarbonContext.getThreadLocalCarbonContext().getTenantId() <= 0 ?
                            MultitenantConstants.SUPER_TENANT_ID : CarbonContext.getThreadLocalCarbonContext()
                            .getTenantId());
            String tenantDomain = QueueManagerServiceValueHolder.getInstance().getRealmService().getTenantManager()
                        .getDomain(CarbonContext.getThreadLocalCarbonContext().getTenantId() <= 0 ?
                                MultitenantConstants.SUPER_TENANT_ID : CarbonContext.getThreadLocalCarbonContext()
                                .getTenantId());
            String usernameWithoutTenant = userName;
            int domainNameSeparatorIndex = userName.indexOf(DOMAIN_NAME_SEPARATOR);
            if (-1 != domainNameSeparatorIndex) {
                usernameWithoutTenant = userName.substring(0, domainNameSeparatorIndex);
            }
            if (!Utils.isOwnDomain(tenantDomain, nameOfQueue)) {
                throw new QueueManagerException("Permission denied.");
            } else if (Utils.isAdmin(usernameWithoutTenant)) { // Authorize admin user
                send(nameOfQueue, userName, accessKey, jmsType, jmsCorrelationID, numberOfMessages, message,
                        deliveryMode, priority, expireTime);
               isSend = true;
            } else if (userRealm.getAuthorizationManager().isUserAuthorized(
                    usernameWithoutTenant, queueID,
                    TreeNode.Permission.PUBLISH.toString().toLowerCase())) {
                send(nameOfQueue, userName, accessKey, jmsType, jmsCorrelationID, numberOfMessages, message,
                        deliveryMode, priority, expireTime);
                isSend = true;
            }
        } catch (UserStoreException e) {
            throw new QueueManagerException("Unable to send message.", e);
        }
        return isSend;
    }

    /**
     * Publish message to given JMS queue
     *
     * @param nameOfQueue queue name
     * @param userName username
     * @param accessKey access key
     * @param jmsType jms type
     * @param jmsCorrelationID message correlation id
     * @param numberOfMessages number of messages to publish
     * @param message message body
     * @param deliveryMode delivery mode
     * @param priority message priority
     * @param expireTime message expire time
     * @throws QueueManagerException
     */
    private void send(String nameOfQueue, String userName, String accessKey, String jmsType, String jmsCorrelationID,
                      int numberOfMessages, String message, int deliveryMode, int priority, long expireTime)
            throws QueueManagerException {
        QueueConnectionFactory connFactory;
        QueueConnection queueConnection = null;
        QueueSession queueSession = null;
        QueueSender queueSender = null;
        try {
            Properties properties = new Properties();
            properties.put(Context.INITIAL_CONTEXT_FACTORY, ANDES_ICF);
            properties.put(CF_NAME_PREFIX + CF_NAME, Utils.getTCPConnectionURL(userName, accessKey));
            properties.put(QUEUE_NAME_PREFIX + nameOfQueue, nameOfQueue);
            properties.put(CarbonConstants.REQUEST_BASE_CONTEXT, "true");
            InitialContext ctx = new InitialContext(properties);
            connFactory = (QueueConnectionFactory) ctx.lookup(CF_NAME);
            queueConnection = connFactory.createQueueConnection();
            Queue queue = (Queue) ctx.lookup(nameOfQueue);
            queueSession = queueConnection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            queueSender = queueSession.createSender(queue);
            queueConnection.start();
            TextMessage textMessage = queueSession.createTextMessage();
            if (queueSender != null && textMessage != null) {
                if (jmsType != null) {
                    textMessage.setJMSType(jmsType);
                }
                if (jmsCorrelationID != null) {
                    textMessage.setJMSCorrelationID(jmsCorrelationID);
                }

                if (message != null) {
                    textMessage.setText(message);
                } else {
                    textMessage.setText("Type message here..");
                }

                for (int i = 0; i < numberOfMessages; i++) {
                    queueSender.send(textMessage, deliveryMode, priority, expireTime);
                }
            }
        } catch (FileNotFoundException | NamingException | UnknownHostException | XMLStreamException | JMSException e) {
            throw new QueueManagerException("Unable to send message.", e);
        } finally {
            try {
                if (queueConnection != null) {
                    queueConnection.close();
                }
            } catch (JMSException e) {
                log.error("Unable to close queue connection", e);
            }
            try {
                if (queueSession != null) {
                    queueSession.close();
                }
            } catch (JMSException e) {
                log.error("Unable to close queue session", e);
            }
            try {
                if (queueSender != null) {
                    queueSender.close();
                }
            } catch (JMSException e) {
                log.error("Unable to close queue sender", e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getNumberOfMessagesInDLCForQueue(String queueName) throws QueueManagerException {
        long messageCount = 0;
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            ObjectName objectName =
                    new ObjectName("org.wso2.andes:type=QueueManagementInformation,name=QueueManagementInformation");

            String operationName = "getNumberOfMessagesInDLCForQueue";
            Object[] parameters = new Object[]{queueName};
            String[] signature = new String[]{String.class.getName()};
            Object result = mBeanServer.invoke(
                    objectName,
                    operationName,
                    parameters,
                    signature);
            if (result != null) {
                messageCount = (Long) result;
            }

            return messageCount;

        } catch (MalformedObjectNameException | ReflectionException | MBeanException |
                                                                    InstanceNotFoundException e) {
            throw new QueueManagerException("Cannot access mBean operations for message count in " +
                                            "DLC for a queue:" + queueName, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Message[] getMessageInDLCForQueue(String queueName, long nextMessageIdToRead,
                                             int maxMessageCount) throws QueueManagerException{
        List<Message> messageList = new ArrayList<>();
        try {
            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
            ObjectName objectName =
                    new ObjectName("org.wso2.andes:type=QueueManagementInformation,name=QueueManagementInformation");
            String operationName = "getMessageInDLCForQueue";
            Object[] parameters = new Object[]{queueName, nextMessageIdToRead, maxMessageCount};
            String[] signature = new String[]{String.class.getName(), long.class.getName(), int.class.getName()};
            Object result = mBeanServer.invoke(
                    objectName,
                    operationName,
                    parameters,
                    signature);
            if (result != null) {
                CompositeData[] messageDataList = (CompositeData[]) result;
                for (CompositeData messageData : messageDataList) {
                    Message message = new Message();
                    message.setMsgProperties((String) messageData.get(QueueManagementInformation.JMS_PROPERTIES));
                    message.setContentType((String) messageData.get(QueueManagementInformation.CONTENT_TYPE));
                    message.setMessageContent((String[]) messageData.get(QueueManagementInformation.CONTENT));
                    message.setJMSMessageId((String) messageData.get(QueueManagementInformation.JMS_MESSAGE_ID));
                    message.setJMSReDelivered((Boolean) messageData.get(QueueManagementInformation.JMS_REDELIVERED));
                    message.setJMSDeliveredMode((Integer) messageData.get(QueueManagementInformation.JMS_DELIVERY_MODE));
                    message.setJMSTimeStamp((Long) messageData.get(QueueManagementInformation.TIME_STAMP));
                    message.setDlcMsgDestination((String) messageData.get(QueueManagementInformation.MSG_DESTINATION));
                    message.setAndesMsgMetadataId((Long) messageData.get(QueueManagementInformation.ANDES_MSG_METADATA_ID));
                    messageList.add(message);
                }
            }
        } catch (InstanceNotFoundException | MBeanException | ReflectionException | MalformedObjectNameException e) {
            throw new QueueManagerException("Cannot get message in DLC for a queue : " + queueName, e);
        }

        return messageList.toArray(new org.wso2.carbon.andes.core.types.Message[messageList.size()]);
    }

    /**
     * Gets logged in user's username
     *
     * @return username
     */
    private static String getLoggedInUserName() {
        String userName;
        if (CarbonContext.getThreadLocalCarbonContext().getTenantId() != MultitenantConstants.SUPER_TENANT_ID) {
            userName = CarbonContext.getThreadLocalCarbonContext().getUsername() + "!"
                       + CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        } else {
            userName = CarbonContext.getThreadLocalCarbonContext().getUsername();
        }
        return userName.trim();
    }

    /**
     * Create a new role which has the same name as the queueName and assign the logged in
     * user to the newly created role. Then, authorize the newly created role to subscribe and* * publish to the queue.
     *
     * @param queueName queue name
     * @param queueId   Id given to the queue
     * @param userRealm User's Realm
     * @throws QueueManagerException
     */
    private static void authorizePermissionsToLoggedInUser(String queueName,
                                                           String queueId,
                                                           UserRealm userRealm)
                                                                        throws QueueManagerException {
        //For registry we use a modified queue name
        String newQueueName = queueName.replace("@", AT_REPLACE_CHAR);

        String username = CarbonContext.getThreadLocalCarbonContext().getUsername();
        try {
            String roleName = UserCoreUtil.addInternalDomainName(QUEUE_ROLE_PREFIX +
                                                                 queueName.replace(".","-").replace("/", "-"));
            UserStoreManager userStoreManager = CarbonContext.getThreadLocalCarbonContext()
                    .getUserRealm().getUserStoreManager();

            if (!userStoreManager.isExistingRole(roleName)) {
                String[] user = {MultitenantUtils.getTenantAwareUsername(username)};

                userStoreManager.addRole(roleName, user, null);
                userRealm.getAuthorizationManager().authorizeRole(roleName, queueId,
                                                                  PERMISSION_CHANGE_PERMISSION);
                userRealm.getAuthorizationManager().authorizeRole(
                        roleName, queueId, TreeNode.Permission.CONSUME.toString().toLowerCase());
                userRealm.getAuthorizationManager().authorizeRole(
                        roleName, queueId, TreeNode.Permission.PUBLISH.toString().toLowerCase());
            } else {
                throw new QueueManagerException("Unable to provide permissions to the user, " +
                                                " " + username + ", to subscribe and publish to " +
                                                newQueueName);
            }
        } catch (UserStoreException e) {
            throw new QueueManagerException("Error while creating " + newQueueName, e);
        }
    }


    /**
     * Every queue has a role with the same name as the queue name. This role is used to store
     * the permissions for the user who created the queue.This role should be deleted when the
     * queue is deleted.
     *
     * @param queueName name of the queue
     * @throws QueueManagerException
     */
    private static void removeRoleCreatedForLoggedInUser(String queueName)
            throws QueueManagerException {
        //For registry we use a modified queue name
        String newQueueName = queueName.replace("@", AT_REPLACE_CHAR);

        String roleName = UserCoreUtil.addInternalDomainName(QUEUE_ROLE_PREFIX +
                                                             newQueueName.replace(".","-").replace("/", "-"));

        try {
            UserStoreManager userStoreManager = CarbonContext.getThreadLocalCarbonContext()
                    .getUserRealm().getUserStoreManager();
            if (userStoreManager.isExistingRole(roleName)) {
                userStoreManager.deleteRole(roleName);
            }
        } catch (UserStoreException e) {
            throw new QueueManagerException("Error while deleting " + newQueueName, e);
        }
    }
}
