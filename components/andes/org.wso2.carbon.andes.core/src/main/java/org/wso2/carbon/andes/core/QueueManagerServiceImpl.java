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
package org.wso2.carbon.andes.core;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.andes.AMQException;
import org.wso2.andes.server.ClusterResourceHolder;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.andes.commons.CommonsUtil;
import org.wso2.carbon.andes.commons.registry.RegistryClient;
import org.wso2.carbon.andes.core.internal.ds.QueueManagerServiceValueHolder;
import org.wso2.carbon.andes.core.internal.registry.QueueManagementBeans;
import org.wso2.carbon.andes.core.internal.util.QueueManagementConstants;
import org.wso2.carbon.andes.core.internal.util.Utils;
import org.wso2.carbon.andes.core.types.*;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.api.UserStoreManager;
import org.wso2.carbon.user.core.Permission;
import org.wso2.carbon.user.core.authorization.TreeNode;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import javax.jms.*;
import javax.jms.Message;
import javax.jms.Queue;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.xml.stream.XMLStreamException;
import java.io.FileNotFoundException;
import java.util.*;


public class QueueManagerServiceImpl implements QueueManagerService {

    private static int DEFAULT_ANDES_PORT = 5672;
    private static Log log = LogFactory.getLog(QueueManagerServiceImpl.class);
    private static String CARBON_CONFIG_PORT_OFFSET = "Ports.Offset";
    private static int CARBON_DEFAULT_PORT_OFFSET = 0;
    private static final String PERMISSION_CHANGE_PERMISSION = "changePermission";
    private static String ROLE_EVERY_ONE = "everyone";
    public static final String QPID_ICF = "org.wso2.andes.jndi.PropertiesFileInitialContextFactory";
    private static final String CF_NAME_PREFIX = "connectionfactory.";
    private static final String QUEUE_NAME_PREFIX = "queue.";
    private static final String CF_NAME = "qpidConnectionfactory";
    public static final String UI_EXECUTE = "ui.execute";
    public static final String PERMISSION_ADMIN_MANAGE_DLC_BROWSE_DLC = "/permission/admin/manage/dlc/browseDlc";
    private Properties properties;
    private QueueConnection queueConnection;
    private QueueSession queueSession;
    private QueueBrowser queueBrowser;
    private QueueSender queueSender;

    public void createQueue(String queueName) throws QueueManagerException {
        try {
            String tenantBasedQueueName = Utils.getTenantBasedQueueName(queueName);
            String userName = getLoggedInUserName();
            if (!QueueManagementBeans.getInstance().queueExists(tenantBasedQueueName)) {
                RegistryClient.createQueue(tenantBasedQueueName, userName);
                QueueManagementBeans.getInstance().createQueue(tenantBasedQueueName, userName);

                //Adding change permissions to the current logged in user
                UserRealm userRealm =
                        QueueManagerServiceValueHolder.getInstance().getRealmService().getTenantUserRealm(CarbonContext.getThreadLocalCarbonContext().getTenantId() <= 0 ?
                                MultitenantConstants.SUPER_TENANT_ID : CarbonContext.getThreadLocalCarbonContext().getTenantId());

                String queueID = CommonsUtil.getQueueID(queueName);
                UserStoreManager userStoreManager = userRealm.getUserStoreManager();
               //Get all the roles of the logged in user and check whether the role is existing
                String[] roleNames =  userStoreManager.getRoleListOfUser(CarbonContext.getThreadLocalCarbonContext().getUsername());
                for(String role : roleNames){
                    if(!role.equalsIgnoreCase(ROLE_EVERY_ONE) &&  userStoreManager.isExistingRole(role)){
                        userRealm.getAuthorizationManager().authorizeRole(
                              role, queueID, PERMISSION_CHANGE_PERMISSION);
                    }
                }
            }else{
                // TODO : Can we use error code for cleaner error handling ? this will hard bind to
                //
                // the error message.
                // Queue exists in the system.
                throw new QueueManagerException("Queue with the name: " + queueName + " already exists!",
                        new RuntimeException("Queue with the name: " + queueName + " already exists!"));
            }
        } catch (Exception e) {
            if(e.getMessage().contains("illegal characters")){
                throw new QueueManagerException("Error in creating the queue:" +
                        queueName + " contains one or more invalid characters! ",
                        e);
            }else if(e.getMessage().contains("already exists")){
                throw new QueueManagerException("Queue with the name: " + queueName + " already exists!",
                        e);
            }
            // In case someone change the error message in the exception.
            throw new QueueManagerException("Error in creating the queue: " +
                    queueName + "! ", e);
        }
    }

    public List<org.wso2.carbon.andes.core.types.Queue> getAllQueues() throws QueueManagerException {
        UserRealm userRealm;
        List<org.wso2.carbon.andes.core.types.Queue> allQueues = QueueManagementBeans.getInstance().getAllQueues();
        //show queues belonging to current domain of user
        //also set queue name used by user
        List<org.wso2.carbon.andes.core.types.Queue> queues = Utils.filterDomainSpecificQueues(allQueues);
        List<org.wso2.carbon.andes.core.types.Queue> filteredQueueByUser = new ArrayList<org.wso2.carbon.andes.core.types.Queue>();
        try {
            if(Utils.isAdmin(CarbonContext.getThreadLocalCarbonContext().getUsername())) {
                filteredQueueByUser.addAll(queues);
            } else {
                userRealm = QueueManagerServiceValueHolder.getInstance().getRealmService().getTenantUserRealm(CarbonContext.getThreadLocalCarbonContext().getTenantId() <= 0 ?
                        MultitenantConstants.SUPER_TENANT_ID : CarbonContext.getThreadLocalCarbonContext().getTenantId());
                UserStoreManager userStoreManager = userRealm.getUserStoreManager();
                //Get all the roles of the logged in user
                String[] roleNames =  userStoreManager.getRoleListOfUser(CarbonContext.getThreadLocalCarbonContext().getUsername());
                for (org.wso2.carbon.andes.core.types.Queue queue : queues) {
                    String queueName = queue.getQueueName();
                    String queueID = CommonsUtil.getQueueID(queueName);
                    for (String role : roleNames) {
                        if(userRealm.getAuthorizationManager().isRoleAuthorized(
                                role, queueID, TreeNode.Permission.CONSUME.toString().toLowerCase()) || userRealm.getAuthorizationManager().isRoleAuthorized(
                                role, queueID, TreeNode.Permission.PUBLISH.toString().toLowerCase()) || userRealm.getAuthorizationManager()
                                .isUserAuthorized(CarbonContext.getThreadLocalCarbonContext().getUsername(), PERMISSION_ADMIN_MANAGE_DLC_BROWSE_DLC, UI_EXECUTE)){
                            if(!filteredQueueByUser.contains(queue)){
                                filteredQueueByUser.add(queue);
                            }
                        }
                    }
                }
            }
        } catch (UserStoreException e) {
            String message = e.getMessage();
            throw new QueueManagerException("Unable to get all queues."+message, e);
        }
        return filteredQueueByUser;
    }

    public void deleteQueue(String queueName) throws QueueManagerException {
         try {
            UserRegistry userRegistry = Utils.getUserRegistry();
            String resourcePath = QueueManagementConstants.MB_QUEUE_STORAGE_PATH + "/" + queueName;
            if(QueueManagementBeans.getInstance().queueExists(queueName)){
                QueueManagementBeans.getInstance().deleteQueue(queueName);
                userRegistry.delete(resourcePath);
            }
        } catch (RegistryException e) {
             String message = e.getMessage();
            throw new QueueManagerException("Failed to delete queue: " + queueName+" "+message, e);
        }

    }

    public void restoreMessagesFromDeadLetterQueue(String[] messageIDs) throws Exception {
        try {
            QueueManagementBeans.getInstance().restoreMessagesFromDeadLetterQueue(messageIDs);
        } catch (Exception ex) {
            throw new Exception("Failed to restore the message :" + ex);
        }
    }

    public void restoreMessagesFromDeadLetterQueueWithDifferentDestination(String[] messageIDs, String destination) throws Exception {
        try {
            QueueManagementBeans.getInstance().restoreMessagesFromDeadLetterQueueWithDifferentDestination(messageIDs, destination);
        } catch (Exception ex) {
            throw new Exception("Failed to restore the message :" + ex);
        }
    }

    public void deleteMessagesFromDeadLetterQueue(String[] messageIDs) throws Exception {
        try {
            QueueManagementBeans.getInstance().deleteMessagesFromDeadLetterQueue(messageIDs);
        } catch (Exception e) {
            throw new Exception("Failed to restore the message :" + e);
        }
    }

    public void purgeMessagesOfQueue(String queueName) throws Exception {
        try {
            QueueManagementBeans.getInstance().purgeMessagesFromQueue(queueName);
        } catch (Exception e) {
            throw new Exception("Failed to purge Queue :" + queueName + e);
        }
    }

    public long getMessageCountForQueue(String queueName,String msgPattern) throws Exception{
        long messageCount = 0;
        try {
            messageCount = QueueManagementBeans.getInstance().getMessageCount(queueName,msgPattern);
            return messageCount;
        } catch (Exception e) {
            throw new Exception("Failed to get message count for queue :" + queueName + e);
        }
    }

    @Override
    public void updatePermission(String queueName, QueueRolePermission[] queueRolePermissions) throws QueueManagerException {
        String tenantBasedQueueName = Utils.getTenantBasedQueueName(queueName);
        if (QueueManagementBeans.getInstance().queueExists(tenantBasedQueueName)) {
            String queueID = CommonsUtil.getQueueID(queueName);
            UserRealm userRealm;
            String role;
            String loggedInUser = CarbonContext.getThreadLocalCarbonContext().getUsername();
            try {
                userRealm = QueueManagerServiceValueHolder.getInstance().getRealmService().getTenantUserRealm(CarbonContext.getThreadLocalCarbonContext().getTenantId() <= 0 ?
                        MultitenantConstants.SUPER_TENANT_ID : CarbonContext.getThreadLocalCarbonContext().getTenantId());
                if (!userRealm.getAuthorizationManager().isUserAuthorized(
                        loggedInUser, queueID, PERMISSION_CHANGE_PERMISSION)) {
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
                throw new QueueManagerException("Unable to update permission of the queue "+e.getMessage(), e);
            }
        } else {
            throw new QueueManagerException("Queue with the name: " + queueName + " not already exists!",
                    new RuntimeException("Queue with the name: " + queueName + " not already exists!"));
        }
    }

    @Override
    public String[] getBackendRoles() throws QueueManagerException {
        UserRealm userRealm;
        try {
            userRealm = QueueManagerServiceValueHolder.getInstance().getRealmService().getTenantUserRealm(CarbonContext.getThreadLocalCarbonContext().getTenantId() <= 0 ?
                    MultitenantConstants.SUPER_TENANT_ID : CarbonContext.getThreadLocalCarbonContext().getTenantId());
            //Get the admin role
            String adminRole = QueueManagerServiceValueHolder.getInstance().getRealmService().getBootstrapRealm().getRealmConfiguration().getAdminRoleName();
            UserStoreManager userStoreManager = userRealm.getUserStoreManager();
            //Get all the roles of the logged in user
            String[] roleNames =  userStoreManager.getRoleListOfUser(CarbonContext.getThreadLocalCarbonContext().getUsername());
            //Check current user has admin role
            String[] rolesExceptAdminRole = null;
            boolean adminRoleExistInAllRoles = false;
            if(Utils.isAdmin(CarbonContext.getThreadLocalCarbonContext().getUsername())) {
                String[] allRoles = userRealm.getUserStoreManager().getRoleNames();
                for (String aRole : allRoles) {
                    if(adminRole.equals(aRole)) {
                        adminRoleExistInAllRoles = true;
                    }
                }
                if (allRoles != null && allRoles.length > 1) {
                    if(adminRoleExistInAllRoles) {
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
            if(rolesExceptAdminRole != null && rolesExceptAdminRole.length > 0){
                return rolesExceptAdminRole;
            } else {
                return new String[0];
            }
        } catch (UserStoreException e) {
            throw new QueueManagerException("Unable to get roles from user store "+ e.getMessage(), e);
        }
    }

    @Override
    public QueueRolePermission[] getQueueRolePermission(String queueName) throws QueueManagerException {
        String tenantBasedQueueName = Utils.getTenantBasedQueueName(queueName);
        if (QueueManagementBeans.getInstance().queueExists(tenantBasedQueueName)) {
            queueName = CarbonContext.getThreadLocalCarbonContext().getTenantId() <= 0 ?
                    queueName : queueName.replace(CarbonContext.getThreadLocalCarbonContext().getTenantDomain()+"/", "");
            String queueID = CommonsUtil.getQueueID(queueName);
            UserRealm userRealm;
            List<QueueRolePermission> queueRolePermissions = new ArrayList<QueueRolePermission>();
            try {
                userRealm = QueueManagerServiceValueHolder.getInstance().getRealmService().getTenantUserRealm(CarbonContext.getThreadLocalCarbonContext().getTenantId() <= 0 ?
                        MultitenantConstants.SUPER_TENANT_ID : CarbonContext.getThreadLocalCarbonContext().getTenantId());
                String adminRole = QueueManagerServiceValueHolder.getInstance().getRealmService().getBootstrapRealm().getRealmConfiguration().getAdminRoleName();
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
                throw new QueueManagerException("Unable to retrieve permission of the queue "+ e.getMessage(), e);
            }
        } else {
            throw new QueueManagerException("Queue with the name: " + queueName + " not already exists!",
                    new RuntimeException("Queue with the name: " + queueName + " not already exists!"));
        }
    }

    @Override
    public org.wso2.carbon.andes.core.types.Message[] browseQueue(String nameOfQueue, String userName, String accessKey, int startingIndex, int maxMsgCount)
            throws QueueManagerException {
        List<org.wso2.carbon.andes.core.types.Message> messageList = new ArrayList<org.wso2.carbon.andes.core.types.Message>();
        try {
            javax.jms.Queue queue = getQueue(nameOfQueue, userName, accessKey);
            queueSession = queueConnection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            queueBrowser = queueSession.createBrowser(queue);
            queueConnection.start();
            if(queueBrowser != null){
                Enumeration queueContentsEnu = queueBrowser.getEnumeration();
                ArrayList msgArrayList = Collections.list(queueContentsEnu);
                int messageBatchSizeForBrowserSubscriptions = ClusterResourceHolder.getInstance().getClusterConfiguration().
                        getMessageBatchSizeForBrowserSubscriptions();
                if(startingIndex < messageBatchSizeForBrowserSubscriptions){
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
                            if(destination!= null && destination.toString().contains("routingkey=")) {
                                String[] word = destination.toString().split("routingkey=");
                                if(word != null && word.length > 0){
                                    msg.setDlcMsgDestination(word[1]);
                                }
                            }
                            messageList.add(msg);
                        }
                    }
                } else {
                    throw new QueueManagerException("Please increase the messageBatchSizeForBrowserSubscriptions in andes-config.xml");
                }
            }
        } catch (NamingException e) {
            throw new QueueManagerException("Unable to browsing queue. "+e.getMessage(), e);
        } catch (JMSException e) {
            throw new QueueManagerException("Unable to browsing queue. "+e.getMessage(), e);
        } catch (FileNotFoundException e) {
            throw new QueueManagerException("Unable to browsing queue. "+e.getMessage(), e);
        } catch (XMLStreamException e) {
            throw new QueueManagerException("Unable to browsing queue. "+e.getMessage(), e);
        } finally {
            try {
                queueConnection.close();
                queueSession.close();
                queueBrowser.close();
            } catch (Exception e) {
                throw new QueueManagerException("Unable to browsing queue.", e);
            }
        }
        return messageList.toArray(new org.wso2.carbon.andes.core.types.Message[messageList.size()]);
    }

    @Override
    public long getTotalMessagesInQueue(String nameOfQueue) throws QueueManagerException {
        int messageCount = QueueManagementBeans.getInstance().getMessageCount(nameOfQueue,"queue");
        return messageCount;
    }

    @Override
    public boolean sendMessage(String nameOfQueue, String userName, String accessKey, String jmsType, String jmsCorrelationID,
                               int numberOfMessages, String message, int deliveryMode, int priority, long expireTime) throws QueueManagerException {
        try {
            Queue queue = getQueue(nameOfQueue, userName, accessKey);
            queueSession = queueConnection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            queueSender = queueSession.createSender(queue);
            queueConnection.start();
            TextMessage textMessage = queueSession.createTextMessage();
            if(queueSender != null && textMessage != null){
                if(jmsType != null){
                    textMessage.setJMSType(jmsType);
                }
                if(jmsCorrelationID != null){
                    textMessage.setJMSCorrelationID(jmsCorrelationID);
                }
                if(message != null){
                    textMessage.setText(message);
                } else {
                    textMessage.setText("Type message here..");
                }
                for(int i = 0; i < numberOfMessages; i++) {
                    queueSender.send(textMessage, deliveryMode, priority, expireTime);
                }
            }
            return true;
        } catch (NamingException e) {
            throw new QueueManagerException("Unable to send message. "+e.getMessage(), e);
        } catch (JMSException e) {
            throw new QueueManagerException("Unable to send message. "+e.getMessage(), e);
        } catch (FileNotFoundException e) {
            throw new QueueManagerException("Unable to send message. "+e.getMessage(), e);
        } catch (XMLStreamException e) {
            throw new QueueManagerException("Unable to send message. "+e.getMessage(), e);
        } finally {
            try {
                queueConnection.close();
                queueSession.close();
                queueSender.close();
            } catch (Exception e) {
                throw new QueueManagerException("Unable to send message.", e);
            }
        }
    }

    private Queue getQueue(String nameOfQueue, String userName, String accessKey) throws FileNotFoundException, XMLStreamException, NamingException, JMSException {
        this.properties = new Properties();
        properties.put(Context.INITIAL_CONTEXT_FACTORY, QPID_ICF);
        properties.put(CF_NAME_PREFIX + CF_NAME, Utils.getTCPConnectionURL(userName, accessKey));
        properties.put(QUEUE_NAME_PREFIX + nameOfQueue, nameOfQueue);
        properties.put(CarbonConstants.REQUEST_BASE_CONTEXT, "true");
        InitialContext ctx = new InitialContext(properties);
        QueueConnectionFactory connFactory = (QueueConnectionFactory) ctx.lookup(CF_NAME);
        queueConnection = connFactory.createQueueConnection();
        return (Queue) ctx.lookup(nameOfQueue);
    }

    private int readPortOffset() {
        ServerConfiguration carbonConfig = ServerConfiguration.getInstance();
        String portOffset = System.getProperty("portOffset",
                carbonConfig.getFirstProperty(CARBON_CONFIG_PORT_OFFSET));

        try {
            return ((portOffset != null) ? Integer.parseInt(portOffset.trim()) : CARBON_DEFAULT_PORT_OFFSET);
        } catch (NumberFormatException e) {
            return CARBON_DEFAULT_PORT_OFFSET;
        }
    }

    private static String getLoggedInUserName() {
       String userName = "";
       if (CarbonContext.getThreadLocalCarbonContext().getTenantId() != MultitenantConstants.SUPER_TENANT_ID) {
           userName = CarbonContext.getThreadLocalCarbonContext().getUsername() + "!"
                   + CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
       } else {
           userName = CarbonContext.getThreadLocalCarbonContext().getUsername();
       }
       return userName.trim();
   }


}
