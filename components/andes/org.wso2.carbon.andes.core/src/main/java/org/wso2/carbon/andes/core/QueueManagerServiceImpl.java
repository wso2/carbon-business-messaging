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
import org.wso2.andes.configuration.AndesConfigurationManager;
import org.wso2.andes.configuration.enums.AndesConfiguration;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.andes.commons.CommonsUtil;
import org.wso2.carbon.andes.commons.registry.RegistryClient;
import org.wso2.carbon.andes.commons.registry.RegistryClientException;
import org.wso2.carbon.andes.core.internal.ds.QueueManagerServiceValueHolder;
import org.wso2.carbon.andes.core.internal.registry.QueueManagementBeans;
import org.wso2.carbon.andes.core.internal.util.QueueManagementConstants;
import org.wso2.carbon.andes.core.internal.util.Utils;
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

import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.xml.stream.XMLStreamException;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

/**
 * Works as the manager class for queue related tasks done from UI. (create queue, delete queue,
 * browse queue, getAllQueues etc.)
 */
public class QueueManagerServiceImpl implements QueueManagerService {

    private static Log log = LogFactory.getLog(QueueManagerServiceImpl.class);
    private static final String URLEncodingFormat = "UTF-8";
    private static final String PERMISSION_CHANGE_PERMISSION = "changePermission";
    private static final String ROLE_EVERY_ONE = "everyone";
    public static final String QPID_ICF = "org.wso2.andes.jndi.PropertiesFileInitialContextFactory";
    private static final String CF_NAME_PREFIX = "connectionfactory.";
    private static final String QUEUE_NAME_PREFIX = "queue.";
    private static final String CF_NAME = "qpidConnectionfactory";
    public static final String UI_EXECUTE = "ui.execute";

    public static final String PERMISSION_ADMIN_MANAGE_DLC_BROWSE_DLC = "/permission/admin/manage/dlc/browseDlc";
    private static final String AT_REPLACE_CHAR = "_";
    private static final String QUEUE_ROLE_PREFIX = "Q_";

    private QueueConnection queueConnection;
    private QueueSession queueSession;
    private QueueSender queueSender;

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
                authorizePermissionsToLoggedInUser(queueName, queueID, userRealm);
                UserStoreManager userStoreManager = userRealm.getUserStoreManager();
                //Get all the roles of the logged in user and check whether the role is existing
                String[] roleNames = userStoreManager.getRoleListOfUser(CarbonContext.getThreadLocalCarbonContext()
                                                                                .getUsername());
                for (String role : roleNames) {
                    if (!role.equalsIgnoreCase(ROLE_EVERY_ONE) && userStoreManager.isExistingRole(role)) {
                        userRealm.getAuthorizationManager().authorizeRole(
                                role, queueID, PERMISSION_CHANGE_PERMISSION);
                    }
                }
            } else {
                // TODO : Can we use error code for cleaner error handling ? this will hard bind to
                //
                // the error message.
                // Queue exists in the system.
                throw new QueueManagerException("Queue with the name: " + queueName + " already exists!");
            }
        } catch (UserStoreException e) {
            throw new QueueManagerException("Error in creating the queue : " + queueName, e);
        } catch (RegistryClientException e) {
            throw new QueueManagerException("Error in creating the queue : " + queueName, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<org.wso2.carbon.andes.core.types.Queue> getAllQueues()
            throws QueueManagerException {
        UserRealm userRealm;
        List<org.wso2.carbon.andes.core.types.Queue> allQueues = QueueManagementBeans.getInstance().getAllQueues();
        //show queues belonging to current domain of user
        //also set queue name used by user
        List<org.wso2.carbon.andes.core.types.Queue> queues = Utils.filterDomainSpecificQueues(allQueues);
        List<org.wso2.carbon.andes.core.types.Queue> filteredQueueByUser = new ArrayList<org.wso2.carbon.andes.core
                .types.Queue>();
        try {
            if (Utils.isAdmin(CarbonContext.getThreadLocalCarbonContext().getUsername())) {
                filteredQueueByUser.addAll(queues);
            } else {
                userRealm = QueueManagerServiceValueHolder.getInstance().getRealmService().getTenantUserRealm
                        (CarbonContext.getThreadLocalCarbonContext().getTenantId() <= 0 ?
                         MultitenantConstants.SUPER_TENANT_ID : CarbonContext.getThreadLocalCarbonContext()
                                .getTenantId());
                UserStoreManager userStoreManager = userRealm.getUserStoreManager();
                //Get all the roles of the logged in user
                String[] roleNames = userStoreManager.getRoleListOfUser(CarbonContext.getThreadLocalCarbonContext()
                                                                                .getUsername());
                for (org.wso2.carbon.andes.core.types.Queue queue : queues) {
                    String queueName = queue.getQueueName();
                    String queueID = CommonsUtil.getQueueID(queueName);
                    for (String role : roleNames) {

                        if (userRealm.getAuthorizationManager().isRoleAuthorized(
                                role, queueID, TreeNode.Permission.CONSUME.toString().toLowerCase()) ||
                            userRealm.getAuthorizationManager().isRoleAuthorized(
                                    role, queueID, TreeNode.Permission.PUBLISH.toString().toLowerCase()) ||
                            userRealm.getAuthorizationManager().isUserAuthorized(
                                    CarbonContext.getThreadLocalCarbonContext().getUsername(),
                                    PERMISSION_ADMIN_MANAGE_DLC_BROWSE_DLC, UI_EXECUTE)) {
                            if (!filteredQueueByUser.contains(queue)) {
                                filteredQueueByUser.add(queue);
                            }
                        }
                    }
                }
            }
        } catch (UserStoreException e) {
            throw new QueueManagerException("Unable to get all queues.", e);
        }
        return filteredQueueByUser;
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

            removeRoleCreateForLoggedInUser(queueName);
        } catch (RegistryException e) {
            throw new QueueManagerException("Failed to delete queue : " + queueName, e);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void restoreMessagesFromDeadLetterQueue(String[] messageIDs, String deadLetterQueueName)
            throws
            QueueManagerException {
        QueueManagementBeans.getInstance().restoreMessagesFromDeadLetterQueue(messageIDs, deadLetterQueueName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void restoreMessagesFromDeadLetterQueueWithDifferentDestination(String[] messageIDs,
                                                                           String destination,
                                                                           String deadLetterQueueName)
            throws
            QueueManagerException {
        QueueManagementBeans.getInstance().restoreMessagesFromDeadLetterQueueWithDifferentDestination(messageIDs,
                                                                                                      destination, deadLetterQueueName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteMessagesFromDeadLetterQueue(String[] messageIDs, String deadLetterQueueName)
            throws
            QueueManagerException {
        QueueManagementBeans.getInstance().deleteMessagesFromDeadLetterQueue(messageIDs, deadLetterQueueName);
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
            throws
            QueueManagerException {
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
                if (!userRealm.getAuthorizationManager().isUserAuthorized(
                        loggedInUser, queueID, PERMISSION_CHANGE_PERMISSION) && !Utils.isAdmin(CarbonContext.getThreadLocalCarbonContext().getUsername())) {
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
            throw new QueueManagerException("Queue with the name: " + queueName + " not already exists!",
                                            new RuntimeException("Queue with the name: " + queueName + " not already exists!"));
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
    public QueueRolePermission[] getQueueRolePermission(String queueName)
            throws QueueManagerException {
        String tenantBasedQueueName = Utils.getTenantBasedQueueName(queueName);
        if (QueueManagementBeans.queueExists(tenantBasedQueueName)) {
            queueName = CarbonContext.getThreadLocalCarbonContext().getTenantId() <= 0 ?
                        queueName : queueName.replace(CarbonContext.getThreadLocalCarbonContext().getTenantDomain() +
                                                      "/", "");
            String queueID = CommonsUtil.getQueueID(queueName);
            UserRealm userRealm;
            List<QueueRolePermission> queueRolePermissions = new ArrayList<QueueRolePermission>();
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
                        QueueRolePermission queueRolePermission = new QueueRolePermission();
                        queueRolePermission.setRoleName(role);
                        queueRolePermission.setAllowedToConsume(userRealm.getAuthorizationManager().isRoleAuthorized(
                                role, queueID, TreeNode.Permission.CONSUME.toString().toLowerCase()));
                        queueRolePermission.setAllowedToPublish(userRealm.getAuthorizationManager().isRoleAuthorized(
                                role, queueID, TreeNode.Permission.PUBLISH.toString().toLowerCase()));
                        queueRolePermissions.add(queueRolePermission);
                    }
                }
                return queueRolePermissions.toArray(new QueueRolePermission[queueRolePermissions.size()]);
            } catch (UserStoreException e) {
                throw new QueueManagerException("Unable to retrieve permission of the queue.", e);
            }
        } else {
            throw new QueueManagerException("Queue with the name: " + queueName + " not already " +
                                            "exists!");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public org.wso2.carbon.andes.core.types.Message[] browseQueue(String nameOfQueue,
                                                                  String userName, String accessKey,
                                                                  int startingIndex, int maxMsgCount
    ) throws QueueManagerException {

        List<org.wso2.carbon.andes.core.types.Message> messageList =
                new ArrayList<org.wso2.carbon.andes.core.types.Message>();

        try {
            // User name may contain the domain name and the user name. eg: WSO2/admin
            // having this username with domain name containing '/' character violates the
            // amqp url user name. Therefore escaping it according to url standards
            javax.jms.Queue queue = getQueue(
                    nameOfQueue,
                    URLEncoder.encode(userName, URLEncodingFormat),
                    accessKey
            );
            queueSession = queueConnection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            QueueBrowser queueBrowser = queueSession.createBrowser(queue);
            queueConnection.start();
            if (queueBrowser != null) {
                Enumeration queueContentsEnu = queueBrowser.getEnumeration();
                ArrayList msgArrayList = Collections.list(queueContentsEnu);
                Integer messageBatchSizeForBrowserSubscriptions = AndesConfigurationManager.readValue
                        (AndesConfiguration.MANAGEMENT_CONSOLE_MESSAGE_BATCH_SIZE_FOR_BROWSER_SUBSCRIPTIONS);
                if (startingIndex < messageBatchSizeForBrowserSubscriptions) {
                    Object[] filteredMsgArray = Utils.getFilteredMsgsList(msgArrayList, startingIndex, maxMsgCount);
                    for (Object message : filteredMsgArray) {
                        //cast to jms message
                        Message queueMessage = (Message) message;
                        //assign jms message properties to org.wso2.carbon.andes.core.types.Message
                        org.wso2.carbon.andes.core.types.Message msg = new org.wso2.carbon.andes.core.types.Message();
                        if (queueMessage != null) {
                            msg.setMsgProperties(Utils.getMsgProperties(queueMessage));
                            msg.setContentType(Utils.getMsgContentType(queueMessage));
                            msg.setMessageContent(Utils.getMessageContentAsString(queueMessage));
                            msg.setJMSMessageId(queueMessage.getJMSMessageID());
                            msg.setJMSCorrelationId(queueMessage.getJMSCorrelationID());
                            msg.setJMSType(queueMessage.getJMSType());
                            msg.setJMSReDelivered(queueMessage.getJMSRedelivered());
                            msg.setJMSDeliveredMode(queueMessage.getJMSDeliveryMode());
                            msg.setJMSPriority(queueMessage.getJMSPriority());
                            msg.setJMSTimeStamp(queueMessage.getJMSTimestamp());
                            msg.setJMSExpiration(queueMessage.getJMSExpiration());
                            Destination destination = queueMessage.getJMSDestination();
                            if (destination != null && destination.toString().contains("routingkey=")) {
                                String[] word = destination.toString().split("routingkey=");
                                if (word.length > 0) {
                                    msg.setDlcMsgDestination(word[1]);
                                }
                            }
                            messageList.add(msg);
                        }
                    }
                } else {
                    throw new QueueManagerException("Please increase the " +
                                                    "messageBatchSizeForBrowserSubscriptions in broker.xml");
                }
            }
            return messageList.toArray(new org.wso2.carbon.andes.core.types.Message[messageList.size()]);
        } catch (NamingException e) {
            throw new QueueManagerException("Unable to browse queue.", e);
        } catch (JMSException e) {
            throw new QueueManagerException("Unable to browse queue.", e);
        } catch (FileNotFoundException e) {
            throw new QueueManagerException("Unable to browse queue.", e);
        } catch (XMLStreamException e) {
            throw new QueueManagerException("Unable to browse queue.", e);
        } catch (UnsupportedEncodingException e) {
            throw new QueueManagerException("Unable to encode user name to url safe format", e);
        } finally {
            try {
                // There is no need to close the sessions, producers, and consumers of a
                // closed connection
                queueConnection.close();
            } catch (JMSException e) {
                log.error("Failed to close queue connection", e);
            }
        }
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
        try {
            Queue queue = getQueue(nameOfQueue, userName, accessKey);
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
            return true;
        } catch (NamingException e) {
            throw new QueueManagerException("Unable to send message.", e);
        } catch (JMSException e) {
            throw new QueueManagerException("Unable to send message.", e);
        } catch (FileNotFoundException e) {
            throw new QueueManagerException("Unable to send message.", e);
        } catch (XMLStreamException e) {
            throw new QueueManagerException("Unable to send message.", e);
        } finally {
            try {
                queueConnection.close();
            } catch (JMSException e) {
                log.error("Unable to close queue connection", e);
            }
            try {
                queueSession.close();
            } catch (JMSException e) {
                log.error("Unable to close queue session", e);
            }
            try {
                queueSender.close();
            } catch (JMSException e) {
                log.error("Unable to close queue sender", e);
            }
        }
    }

    /**
     * Gets a JMS queue object
     *
     * @param nameOfQueue name of the user
     * @param userName    username for the amqp url
     * @param accessKey   access key for the amqp url
     * @return a queue
     * @throws FileNotFoundException
     * @throws XMLStreamException
     * @throws NamingException
     * @throws JMSException
     */
    private Queue getQueue(String nameOfQueue, String userName, String accessKey)
            throws FileNotFoundException,
                   XMLStreamException, NamingException, JMSException {
        Properties properties = new Properties();
        properties.put(Context.INITIAL_CONTEXT_FACTORY, QPID_ICF);
        properties.put(CF_NAME_PREFIX + CF_NAME, Utils.getTCPConnectionURL(userName, accessKey));
        properties.put(QUEUE_NAME_PREFIX + nameOfQueue, nameOfQueue);
        properties.put(CarbonConstants.REQUEST_BASE_CONTEXT, "true");
        InitialContext ctx = new InitialContext(properties);
        QueueConnectionFactory connFactory = (QueueConnectionFactory) ctx.lookup(CF_NAME);
        queueConnection = connFactory.createQueueConnection();
        return (Queue) ctx.lookup(nameOfQueue);
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
                                                           UserRealm userRealm) throws
                                                                                QueueManagerException {
        //For registry we use a modified queue name
        String newQueueName = queueName.replace("@", AT_REPLACE_CHAR);

        String username = CarbonContext.getThreadLocalCarbonContext().getUsername();
        try {
            String roleName = UserCoreUtil.addInternalDomainName(QUEUE_ROLE_PREFIX +
                                                                 queueName.replace("/", "-"));
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
                log.warn("Unable to provide permissions to the user, " +
                         " " + username + ", to subscribe and publish to " + newQueueName);
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
    private static void removeRoleCreateForLoggedInUser(String queueName)
            throws QueueManagerException {
        //For registry we use a modified queue name
        String newQueueName = queueName.replace("@", AT_REPLACE_CHAR);

        String roleName = UserCoreUtil.addInternalDomainName(QUEUE_ROLE_PREFIX +
                                                             newQueueName.replace("/", "-"));

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
