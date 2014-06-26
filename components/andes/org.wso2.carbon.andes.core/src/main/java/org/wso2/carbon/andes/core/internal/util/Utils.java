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
package org.wso2.carbon.andes.core.internal.util;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.commons.lang.StringEscapeUtils;
import org.wso2.carbon.andes.core.QueueManagerException;
import org.wso2.carbon.andes.core.internal.ds.QueueManagerServiceValueHolder;
import org.wso2.carbon.andes.core.types.Subscription;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.user.api.UserStoreException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

import org.wso2.carbon.andes.core.types.Queue;
import org.wso2.carbon.utils.ServerConstants;

import javax.jms.*;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;


public class Utils {

    public static final String DIRECT_EXCHANGE = "amq.direct";
    public static final String TOPIC_EXCHANGE = "amq.topic";
    private static final String QPID_CONF_DIR = "/repository/conf/advanced/";
    private static final String ANDES_CONF_FILE = "andes-config.xml";
    private static final String QPID_CONF_CONNECTOR_NODE = "connector";
    private static final String QPID_CONF_SSL_NODE = "ssl";
    private static final String QPID_CONF_SSL_ONLY_NODE = "sslOnly";
    private static final String QPID_CONF_SSL_KEYSTORE_PATH = "keystorePath";
    private static final String QPID_CONF_SSL_KEYSTORE_PASSWORD = "keystorePassword";
    private static final String QPID_CONF_SSL_TRUSTSTORE_PATH = "truststorePath";
    private static final String QPID_CONF_SSL_TRUSTSTORE_PASSWORD = "truststorePassword";

    public static String getTenantAwareCurrentUserName() {
        String username = CarbonContext.getThreadLocalCarbonContext().getUsername();
        if (CarbonContext.getThreadLocalCarbonContext().getTenantId() > 0) {
            return username + "@" + CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        }
        return username;
    }

    public static UserRegistry getUserRegistry() throws RegistryException {
        RegistryService registryService =
                QueueManagerServiceValueHolder.getInstance().getRegistryService();

        return registryService.getGovernanceSystemRegistry(CarbonContext.getThreadLocalCarbonContext().getTenantId());

    }

    public static org.wso2.carbon.user.api.UserRealm getUserRelam() throws UserStoreException {
        return QueueManagerServiceValueHolder.getInstance().getRealmService().
                getTenantUserRealm(CarbonContext.getThreadLocalCarbonContext().getTenantId());
    }

    public static String getTenantBasedQueueName(String queueName) {
        String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        if (tenantDomain != null && (!queueName.contains(tenantDomain)) && (!tenantDomain.equals(org.wso2.carbon.base.MultitenantConstants.SUPER_TENANT_DOMAIN_NAME))) {
            queueName = tenantDomain + "/" + queueName;
        }
        return queueName;
    }

    /**
     * Checks if a given user has admin privileges
     *
     * @param username Name of the user
     * @return true if the user has admin rights or false otherwise
     * @throws org.wso2.carbon.andes.core.QueueManagerException
     *          if getting roles for the user fails
     */
    public static boolean isAdmin(String username) throws QueueManagerException {
        boolean isAdmin = false;

        try {
            String[] userRoles = QueueManagerServiceValueHolder.getInstance().getRealmService().
                    getTenantUserRealm(CarbonContext.getThreadLocalCarbonContext().getTenantId()).
                    getUserStoreManager().getRoleListOfUser(username);
            String adminRole = QueueManagerServiceValueHolder.getInstance().getRealmService().
                    getBootstrapRealmConfiguration().getAdminUserName();
            for (String userRole : userRoles) {
                if (userRole.equals(adminRole)) {
                    isAdmin = true;
                    break;
                }
            }
        } catch (UserStoreException e) {
            throw new QueueManagerException("Failed to get list of user roles", e);
        }

        return isAdmin;
    }

    /**
     * filter queues to suit the tenant domain
     * @param fullList
     * @return List<Queue>
     */
    public static List<Queue> filterDomainSpecificQueues(List<Queue> fullList) {
        String domainName = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        ArrayList<Queue> tenantFilteredQueues = new ArrayList<Queue>();
        if(domainName != null && !CarbonContext.getThreadLocalCarbonContext().getTenantDomain().
                equals(org.wso2.carbon.base.MultitenantConstants.SUPER_TENANT_DOMAIN_NAME)) {
            for (Queue aQueue : fullList) {
                if(aQueue.getQueueName().startsWith(domainName)) {
                    tenantFilteredQueues.add(aQueue);
                }
            }
        }
        //for super tenant load all queues not specific to a domain. That means queues created by external
        //JMS clients are visible, and those names should not have "/" in their queue names
        else if(domainName != null && CarbonContext.getThreadLocalCarbonContext().getTenantDomain().
                equals(org.wso2.carbon.base.MultitenantConstants.SUPER_TENANT_DOMAIN_NAME)){
            for (Queue aQueue : fullList) {
                if(!aQueue.getQueueName().contains("/")) {
                    tenantFilteredQueues.add(aQueue);
                }
            }
        }

        return tenantFilteredQueues;
    }

    public static List<Subscription> filterDomainSpecificSubscribers(List<Subscription> allSubscriptions) {
        String domainName = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        ArrayList<Subscription> tenantFilteredSubscriptions = new ArrayList<Subscription>();

        //filter subscriptions belonging to the tenant domain
        if(domainName != null && !CarbonContext.getThreadLocalCarbonContext().getTenantDomain().
                equals(org.wso2.carbon.base.MultitenantConstants.SUPER_TENANT_DOMAIN_NAME)) {
            for (Subscription subscription : allSubscriptions) {
                //for temp queues filter by queue name queueName=<tenantDomain>/queueName
                if(!subscription.isDurable() && subscription.getSubscriberQueueBoundExchange().equals("amq.direct")) {
                    if(subscription.getSubscribedQueueOrTopicName().startsWith(domainName+"/")) {
                        tenantFilteredSubscriptions.add(subscription);
                    }
                }
                //for temp topics filter by topic name topicName=<tenantDomain>/topicName
                else if(!subscription.isDurable() && subscription.getSubscriberQueueBoundExchange().equals("amq.topic")) {
                    if(subscription.getSubscribedQueueOrTopicName().startsWith(domainName+"/")) {
                        tenantFilteredSubscriptions.add(subscription);
                    }
                }
                //if a queue subscription queueName = <tenantDomain>/queueName
                else if(subscription.isDurable() && subscription.getSubscriberQueueBoundExchange().equals("amq.direct")) {
                    if(subscription.getSubscriberQueueName().startsWith(domainName+"/")) {
                        tenantFilteredSubscriptions.add(subscription);
                    }
                }
                //if a durable topic subscription queueName = carbon:<tenantdomain>/subID
                else if(subscription.isDurable() && subscription.getSubscriberQueueBoundExchange().equals("amq.topic")) {
                    String durableTopicQueueName = subscription.getSubscriberQueueName();
                    String subscriptionID = durableTopicQueueName.split(":")[1];
                    if(subscriptionID.startsWith(domainName+"/")) {
                        tenantFilteredSubscriptions.add(subscription);
                    }
                }
            }
            //super tenant domain queue should not have '/'
        }  else if(domainName != null && CarbonContext.getThreadLocalCarbonContext().getTenantDomain().
                equals(org.wso2.carbon.base.MultitenantConstants.SUPER_TENANT_DOMAIN_NAME)){
            for (Subscription subscription : allSubscriptions) {
                if(subscription.isDurable()) {
                    if(!subscription.getSubscriberQueueName().contains("/")) {
                        tenantFilteredSubscriptions.add(subscription);
                    }
                } else if(!subscription.isDurable()) {
                    if(!subscription.getSubscribedQueueOrTopicName().contains("/")) {
                        tenantFilteredSubscriptions.add(subscription);
                    }
                }
            }
        }
        return tenantFilteredSubscriptions;
    }

    public static Subscription parseStringToASubscription(String subscriptionInfo) {

          //  subscriptionInfo =  subscriptionIdentifier |  subscribedQueueOrTopicName | subscriberQueueBoundExchange |
          // subscriberQueueName |  isDurable | isActive | numberOfMessagesRemainingForSubscriber | subscriberNodeAddress

        String[] subInfo = subscriptionInfo.split("\\|");
        Subscription subscription = new Subscription();
        subscription.setSubscriptionIdentifier(subInfo[0]);
        subscription.setSubscribedQueueOrTopicName(subInfo[1]);
        subscription.setSubscriberQueueBoundExchange(subInfo[2]);
        subscription.setSubscriberQueueName(subInfo[3]);
        subscription.setDurable(Boolean.parseBoolean(subInfo[4]));
        subscription.setActive(Boolean.parseBoolean(subInfo[5]));
        subscription.setNumberOfMessagesRemainingForSubscriber(Integer.parseInt(subInfo[6]));
        subscription.setSubscriberNodeAddress(subInfo[7]);

        return subscription;
    }

    /**
     * filter the whole message list to fit for the page range
     *
     * @param msgArrayList  - total message list
     * @param startingIndex -  index of the first message of given page
     * @param maxMsgCount   - max messages count per a page
     * @return filtered message object array for the given page
     */
    public static Object[] getFilteredMsgsList(ArrayList msgArrayList, int startingIndex, int maxMsgCount) {
        Object[] messageArray;
        int resultSetSize = maxMsgCount;

        ArrayList resultList = new ArrayList();
        for (Object aMsg : msgArrayList) {
            resultList.add(aMsg);
        }

        if ((resultList.size() - startingIndex) < maxMsgCount) {
            resultSetSize = (resultList.size() - startingIndex);
        }

        messageArray = new Object[resultSetSize];
        int index = 0;
        int msgDetailsIndex = 0;
        for (Object msgDetailOb : resultList) {
            if (startingIndex == index || startingIndex < index) {
                messageArray[msgDetailsIndex] = msgDetailOb;
                msgDetailsIndex++;
                if (msgDetailsIndex == maxMsgCount) {
                    break;
                }
            }
            index++;
        }
        return messageArray;
    }

    /**
     * Reads the post offset value defined in carbon.xml file
     * @return offset value
     */
    public static int getPortOffset(){
        String CARBON_CONFIG_PORT_OFFSET = "Ports.Offset";
        int CARBON_DEFAULT_PORT_OFFSET = 0;
        ServerConfiguration carbonConfig = ServerConfiguration.getInstance();
        String portOffset = carbonConfig.getFirstProperty(CARBON_CONFIG_PORT_OFFSET);

        try {
            return ((portOffset != null) ? Integer.parseInt(portOffset.trim()) : CARBON_DEFAULT_PORT_OFFSET);
        } catch (NumberFormatException e) {
            return CARBON_DEFAULT_PORT_OFFSET;
        }
    }

    /**
     * Gets the TCP connection url to reach the broker by using the currently logged in user and the accesskey for
     * the user, generated by andes Authentication Service
     * @param userName - currently logged in user
     * @param accessKey - the key (uuid) generated by authentication service
     * @return
     */
    public static String getTCPConnectionURL(String userName, String accessKey) throws FileNotFoundException, XMLStreamException {
        // amqp://{username}:{accesskey}@carbon/carbon?brokerlist='tcp://{hostname}:{port}'
        String CARBON_CLIENT_ID = "carbon";
        String CARBON_VIRTUAL_HOST_NAME = "carbon";
        String CARBON_DEFAULT_HOSTNAME = "localhost";
        String CARBON_DEFAULT_PORT = "5672";
        int portOffset = getPortOffset();
        int carbonPort = Integer.valueOf(CARBON_DEFAULT_PORT)+portOffset;
        String CARBON_PORT = String.valueOf(carbonPort);

        // these are the properties which needs to be passed when ssl is enabled
        String CARBON_DEFAULT_SSL_PORT = "8672";
        int carbonSslPort = Integer.valueOf(CARBON_DEFAULT_SSL_PORT)+portOffset;
        String CARBON_SSL_PORT = String.valueOf(carbonSslPort);

        File confFile = new File(System.getProperty(ServerConstants.CARBON_HOME) + QPID_CONF_DIR + ANDES_CONF_FILE);
        OMElement docRootNode = new StAXOMBuilder(new FileInputStream(confFile)).
                getDocumentElement();
        OMElement connectorNode = docRootNode.getFirstChildWithName(
                new QName(QPID_CONF_CONNECTOR_NODE));
        OMElement sslNode = connectorNode.getFirstChildWithName(
                new QName(QPID_CONF_SSL_NODE));
        OMElement sslKeyStorePath = sslNode.getFirstChildWithName(
                new QName(QPID_CONF_SSL_KEYSTORE_PATH));
        OMElement sslKeyStorePwd = sslNode.getFirstChildWithName(
                new QName(QPID_CONF_SSL_KEYSTORE_PASSWORD));
        OMElement sslTrustStorePath = sslNode.getFirstChildWithName(
                new QName(QPID_CONF_SSL_TRUSTSTORE_PATH));
        OMElement sslTrustStorePwd = sslNode.getFirstChildWithName(
                new QName(QPID_CONF_SSL_TRUSTSTORE_PASSWORD));

        String KEY_STORE_PATH= sslKeyStorePath.getText();
        String TRUST_STORE_PATH= sslTrustStorePath.getText();
        String SSL_KEYSTORE_PASSWORD=sslKeyStorePwd.getText();
        String SSL_TRUSTSTORE_PASSWORD=sslTrustStorePwd.getText();

        // as it is nt possible to obtain the password of for the given user, we use service generated access key
        // to authenticate the user

        if(isSSLOnly()){
            //"amqp://admin:admin@carbon/carbon?brokerlist='tcp://{hostname}:{port}?ssl='true'&trust_store='{trust_store_path}'&trust_store_password='{trust_store_pwd}'&key_store='{keystore_path}'&key_store_password='{key_store_pwd}''";

            return new StringBuffer()
                    .append("amqp://").append(userName).append(":").append(accessKey)
                    .append("@").append(CARBON_CLIENT_ID)
                    .append("/").append(CARBON_VIRTUAL_HOST_NAME)
                    .append("?brokerlist='tcp://").append(CARBON_DEFAULT_HOSTNAME).append(":").append(CARBON_SSL_PORT).append("?ssl='true'&trust_store='").append(TRUST_STORE_PATH)
                    .append("'&trust_store_password='").append(SSL_TRUSTSTORE_PASSWORD).append("'&key_store='").append(KEY_STORE_PATH)
                    .append("'&key_store_password='").append(SSL_KEYSTORE_PASSWORD).append("''")
                    .toString();
        } else {
            return new StringBuffer()
                    .append("amqp://").append(userName).append(":").append(accessKey)
                    .append("@").append(CARBON_CLIENT_ID)
                    .append("/").append(CARBON_VIRTUAL_HOST_NAME)
                    .append("?brokerlist='tcp://").append(CARBON_DEFAULT_HOSTNAME).append(":").append(CARBON_PORT).append("'")
                    .toString();
        }
    }

    public static String getMsgProperties(Message queueMessage) throws JMSException {

        Enumeration propertiesEnu = queueMessage.getPropertyNames();
        StringBuilder sb = new StringBuilder("");
        if (propertiesEnu != null) {
            while (propertiesEnu.hasMoreElements()) {
                String propName = (String) propertiesEnu.nextElement();
                sb.append(propName + " = " + queueMessage.getStringProperty(propName));
                sb.append(", ");
            }
        }

        return sb.toString();
    }

    /**
     * Determines the type of the JMS message
     *
     * @param queueMessage - input message
     * @return type of the message as a string
     */
    public static String getMsgContentType(Message queueMessage) {

        String contentType = "";
        if (queueMessage instanceof TextMessage) {
            contentType = "Text";
        } else if (queueMessage instanceof ObjectMessage) {
            contentType = "Object";
        } else if (queueMessage instanceof MapMessage) {
            contentType = "Map";
        } else if (queueMessage instanceof StreamMessage) {
            contentType = "Stream";
        } else if (queueMessage instanceof BytesMessage) {
            contentType = "Byte";
        }

        return contentType;
    }

    /**
     * Gets the message content as a string, after verifying its type
     *
     * @param queueMessage - JMS Message
     * @return a string array of message content; a summary and the whole message
     * @throws JMSException
     */
    public static String[] getMessageContentAsString(Message queueMessage) throws JMSException {

        String messageContent[] = new String[2];
        String summaryMsg = "";
        String wholeMsg = "";

        StringBuilder sb = new StringBuilder();
        if (queueMessage instanceof TextMessage) {
            wholeMsg = StringEscapeUtils.escapeHtml(((TextMessage) queueMessage).getText()).trim();
            if (wholeMsg.length() >= 15) {
                summaryMsg = wholeMsg.substring(0, 15);
            } else {
                summaryMsg = wholeMsg;
            }
            if(wholeMsg.length() > 200){
                wholeMsg = "Message Content is too large to display";
            }


        } else if (queueMessage instanceof ObjectMessage) {
            wholeMsg = "This Operation is Not Supported!" ;
            summaryMsg = "Not Supported";

        } else if (queueMessage instanceof MapMessage) {
            MapMessage mapMessage = ((MapMessage) queueMessage);
            Enumeration mapEnu = mapMessage.getMapNames();
            while (mapEnu.hasMoreElements()) {
                String mapName = (String) mapEnu.nextElement();
                String mapVal = mapMessage.getObject(mapName).toString();
                wholeMsg = StringEscapeUtils.escapeHtml(sb.append(mapName + ": " + mapVal + ", ").toString()).trim();

            }
            if (wholeMsg.length() >= 15) {
                summaryMsg = wholeMsg.substring(0, 15);
            } else {
                summaryMsg = wholeMsg;
            }

        } else if (queueMessage instanceof StreamMessage) {
            ((StreamMessage) queueMessage).reset();
            wholeMsg = getContentFromStreamMessage((StreamMessage) queueMessage, sb).trim();
            if (wholeMsg.length() >= 15) {
                summaryMsg = wholeMsg.substring(0, 15);
            } else {
                summaryMsg = wholeMsg;
            }

        } else if (queueMessage instanceof BytesMessage) {
            ((BytesMessage) queueMessage).reset();
            long msglength = ((BytesMessage) queueMessage).getBodyLength();
            byte[] byteMsgArr = new byte[(int) msglength];

            int index = ((BytesMessage) queueMessage).readBytes(byteMsgArr);
            for (int i = 0; i < index; i++) {
                wholeMsg = sb.append(byteMsgArr[i] + " ").toString().trim();
            }

            if (wholeMsg.length() >= 15) {
                summaryMsg = wholeMsg.substring(0, 15);
            } else {
                summaryMsg = wholeMsg;
            }
        }
        messageContent[0] = summaryMsg;
        messageContent[1] = wholeMsg;
        return messageContent;
    }

    /**
     * A stream message can have java primitives plus objects, as its content. This message it used to retrieve the
     *
     * @param queueMessage   - input message
     * @param sb   - a string builder to build the whole message content
     * @return  - complete message content inside the stream message
     * @throws JMSException
     */
    private static String getContentFromStreamMessage(StreamMessage queueMessage, StringBuilder sb) throws JMSException {

        while (true) {

            try {
                Object obj = queueMessage.readObject();
                if (obj == null) {
                    break;
                } else {
                    if (obj instanceof Double) {
                        sb.append(obj + ", ");

                    } else if (obj instanceof Integer) {
                        sb.append(obj + ", ");

                    } else if (obj instanceof String) {
                        sb.append(obj + ", ");

                    } else if (obj instanceof Character) {
                        sb.append(obj + ", ");

                    } else if (obj instanceof Long) {
                        sb.append(obj + ", ");

                    } else if (obj instanceof Short) {
                        sb.append(obj + ", ");

                    } else if (obj instanceof Byte) {
                        sb.append(obj + ", ");

                    } else if (obj instanceof Boolean) {
                        sb.append(obj + ", ");
                    } else if (obj instanceof Float) {
                        sb.append(obj + ", ");
                    } else {
                        sb.append(obj.toString() + ", ");
                    }
                }
            } catch (MessageEOFException ex) {
                return sb.toString();
            }

        }

        return StringEscapeUtils.escapeHtml(sb.toString());
    }

    public static boolean isSSLOnly() throws FileNotFoundException, XMLStreamException {

        File confFile = new File(System.getProperty(ServerConstants.CARBON_HOME) + QPID_CONF_DIR + ANDES_CONF_FILE);
        OMElement docRootNode = new StAXOMBuilder(new FileInputStream(confFile)).
                getDocumentElement();
        OMElement connectorNode = docRootNode.getFirstChildWithName(
                new QName(QPID_CONF_CONNECTOR_NODE));
        OMElement sslNode = connectorNode.getFirstChildWithName(
                new QName(QPID_CONF_SSL_NODE));
        OMElement sslOnlyNode = sslNode.getFirstChildWithName(
                new QName(QPID_CONF_SSL_ONLY_NODE));

        boolean sslOnly = Boolean.parseBoolean(sslOnlyNode.getText());

        return sslOnly;
    }
}
