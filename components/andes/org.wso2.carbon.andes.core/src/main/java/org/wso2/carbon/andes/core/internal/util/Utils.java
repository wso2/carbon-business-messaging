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
package org.wso2.carbon.andes.core.internal.util;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.commons.lang.StringEscapeUtils;
import org.wso2.andes.configuration.AndesConfigurationManager;
import org.wso2.andes.configuration.enums.AndesConfiguration;
import org.wso2.carbon.andes.core.QueueManagerException;
import org.wso2.carbon.andes.core.internal.ds.QueueManagerServiceValueHolder;
import org.wso2.carbon.andes.core.types.Queue;
import org.wso2.carbon.andes.core.types.Subscription;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.utils.ServerConstants;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageEOFException;
import javax.jms.ObjectMessage;
import javax.jms.StreamMessage;
import javax.jms.TextMessage;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

/**
 * Provides
 */
public class Utils {

    private static final String DIRECT_EXCHANGE = "amq.direct";
    private static final String TOPIC_EXCHANGE = "amq.topic";
    private static final String ANDES_CONF_DIR = "/repository/conf/advanced/";
    private static final String ANDES_CONF_FILE = "qpid-config.xml";
    private static final String ANDES_CONF_CONNECTOR_NODE = "connector";
    private static final String ANDES_CONF_SSL_NODE = "ssl";
    private static final String ANDES_CONF_SSL_ONLY_NODE = "sslOnly";
    private static final String ANDES_CONF_SSL_KEYSTORE_PATH = "keystorePath";
    private static final String ANDES_CONF_SSL_KEYSTORE_PASSWORD = "keystorePassword";
    private static final String ANDES_CONF_SSL_TRUSTSTORE_PATH = "truststorePath";
    private static final String ANDES_CONF_SSL_TRUSTSTORE_PASSWORD = "truststorePassword";
    private static final String CARBON_CLIENT_ID = "carbon";
    private static final String CARBON_VIRTUAL_HOST_NAME = "carbon";

    /**
     * Maximum size a message will be displayed on UI
     */
    public static final int MESSAGE_DISPLAY_LENGTH_MAX = AndesConfigurationManager.readValue(AndesConfiguration.MANAGEMENT_CONSOLE_MAX_DISPLAY_LENGTH_FOR_MESSAGE_CONTENT);

    /**
     * Shown to user has a indication that the particular message has more content than shown in UI
     */
    public static final String DISPLAY_CONTINUATION = "...";

    /**
     * Message shown in UI if message content exceed the limit - Further enhancement,
     * these needs to read from a resource bundle
     */
    public static final String DISPLAY_LENGTH_EXCEEDED = "Message Content is too large to display.";

    /**
     * Gets a tenant's user name with domain. eg : tenant1user1@testtenant1.com
     *
     * @return A user name with domain as String
     */
    public static String getTenantAwareCurrentUserName() {
        String username = CarbonContext.getThreadLocalCarbonContext().getUsername();
        if (CarbonContext.getThreadLocalCarbonContext().getTenantId() > 0) {
            return username + "@" + CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        }
        return username;
    }

    /**
     * Gets the user registry for the current tenant
     *
     * @return a {@link org.wso2.carbon.registry.core.session.UserRegistry}
     * @throws RegistryException
     */
    public static UserRegistry getUserRegistry() throws RegistryException {
        RegistryService registryService =
                QueueManagerServiceValueHolder.getInstance().getRegistryService();

        return registryService.getGovernanceSystemRegistry(
                CarbonContext.getThreadLocalCarbonContext().getTenantId());

    }

    /**
     * Gets the user real for the current user. The user realm represents the user store.
     *
     * @return a {@link org.wso2.carbon.user.api.UserRealm}
     * @throws UserStoreException
     */
    public static org.wso2.carbon.user.api.UserRealm getUserRelam() throws UserStoreException {
        return QueueManagerServiceValueHolder.getInstance().getRealmService().
                getTenantUserRealm(CarbonContext.getThreadLocalCarbonContext().getTenantId());
    }

    /**
     * Gets tenant based queue name. eg : testtenant1.com/tenant1QueueName
     *
     * @param queueName queue name
     * @return tenant based queue name
     */
    public static String getTenantBasedQueueName(String queueName) {
        String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        if (tenantDomain != null && (!queueName.contains(tenantDomain)) &&
            (!tenantDomain.equals(org.wso2.carbon.base.MultitenantConstants.
                                          SUPER_TENANT_DOMAIN_NAME))) {
            queueName = tenantDomain + "/" + queueName;
        }
        return queueName;
    }

    /**
     * Checks if a given user has admin privileges
     *
     * @param username Name of the user
     * @return true if the user has admin rights or false otherwise
     * @throws org.wso2.carbon.andes.core.QueueManagerException if getting roles for the user fails
     */
    public static boolean isAdmin(String username) throws QueueManagerException {
        boolean isAdmin = false;

        try {
            UserRealm userRealm = QueueManagerServiceValueHolder.getInstance().getRealmService()
                    .getTenantUserRealm(CarbonContext.getThreadLocalCarbonContext().getTenantId());

            String[] userRoles = userRealm.getUserStoreManager().getRoleListOfUser(username);
            String adminRole = userRealm.getRealmConfiguration().getAdminRoleName();
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
     * Filter queues to suit the tenant domain
     *
     * @param fullList Full queue list
     * @return List<Queue>
     */
    public static List<Queue> filterDomainSpecificQueues(List<Queue> fullList) {
        String domainName = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        ArrayList<Queue> tenantFilteredQueues = new ArrayList<Queue>();
        if (domainName != null && !CarbonContext.getThreadLocalCarbonContext().getTenantDomain().
                equals(org.wso2.carbon.base.MultitenantConstants.SUPER_TENANT_DOMAIN_NAME)) {
            for (Queue aQueue : fullList) {
                if (aQueue.getQueueName().startsWith(domainName)) {
                    tenantFilteredQueues.add(aQueue);
                }
            }
        }
        //for super tenant load all queues not specific to a domain. That means queues created by external
        //JMS clients are visible, and those names should not have "/" in their queue names
        else if (domainName != null && CarbonContext.getThreadLocalCarbonContext().getTenantDomain().
                equals(org.wso2.carbon.base.MultitenantConstants.SUPER_TENANT_DOMAIN_NAME)) {
            for (Queue aQueue : fullList) {
                if (!aQueue.getQueueName().contains("/")) {
                    tenantFilteredQueues.add(aQueue);
                }
            }
        }

        return tenantFilteredQueues;
    }

    /**
     * Filters the domain specific subscriptions from a list of subscriptions
     *
     * @param allSubscriptions input subscription list
     * @return filtered list of {@link org.wso2.carbon.andes.core.types.Subscription}
     */
    public static List<Subscription> filterDomainSpecificSubscribers(
            List<Subscription> allSubscriptions) {
        String domainName = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        ArrayList<Subscription> tenantFilteredSubscriptions = new ArrayList<Subscription>();

        //filter subscriptions belonging to the tenant domain
        if (domainName != null && !CarbonContext.getThreadLocalCarbonContext().getTenantDomain().
                equals(org.wso2.carbon.base.MultitenantConstants.SUPER_TENANT_DOMAIN_NAME)) {
            for (Subscription subscription : allSubscriptions) {
                //for temp queues filter by queue name queueName=<tenantDomain>/queueName
                if (!subscription.isDurable() && subscription.getSubscriberQueueBoundExchange().equals(DIRECT_EXCHANGE)) {
                    if (subscription.getSubscribedQueueOrTopicName().startsWith(domainName + "/")) {
                        tenantFilteredSubscriptions.add(subscription);
                    }
                }
                //for temp topics filter by topic name topicName=<tenantDomain>/topicName
                else if (!subscription.isDurable() && subscription.getSubscriberQueueBoundExchange().equals(TOPIC_EXCHANGE)) {
                    if (subscription.getSubscribedQueueOrTopicName().startsWith(domainName + "/")) {
                        tenantFilteredSubscriptions.add(subscription);
                    }
                }
                //if a queue subscription queueName = <tenantDomain>/queueName
                else if (subscription.isDurable() && subscription.getSubscriberQueueBoundExchange().equals(DIRECT_EXCHANGE)) {
                    if (subscription.getSubscriberQueueName().startsWith(domainName + "/")) {
                        tenantFilteredSubscriptions.add(subscription);
                    }
                }
                //if a durable topic subscription queueName = carbon:<tenantdomain>/subID
                else if (subscription.isDurable() && subscription.getSubscriberQueueBoundExchange().equals(TOPIC_EXCHANGE)) {
                    String durableTopicQueueName = subscription.getSubscriberQueueName();
                    String subscriptionID = durableTopicQueueName.split(":")[1];
                    if (subscriptionID.startsWith(domainName + "/")) {
                        tenantFilteredSubscriptions.add(subscription);
                    }
                }
            }
            //super tenant domain queue should not have '/'
        } else if (domainName != null && CarbonContext.getThreadLocalCarbonContext().getTenantDomain().
                equals(org.wso2.carbon.base.MultitenantConstants.SUPER_TENANT_DOMAIN_NAME)) {
            for (Subscription subscription : allSubscriptions) {
                if (subscription.isDurable()) {
                    if (!subscription.getSubscriberQueueName().contains("/")) {
                        tenantFilteredSubscriptions.add(subscription);
                    }
                } else {
                    if (!subscription.getSubscribedQueueOrTopicName().contains("/")) {
                        tenantFilteredSubscriptions.add(subscription);
                    }
                }
            }
        }
        return tenantFilteredSubscriptions;
    }

    /**
     * Parsing a string to a {@link org.wso2.carbon.andes.core.types.Subscription}
     *
     * @param subscriptionInfo subscription string
     * @return a {@link org.wso2.carbon.andes.core.types.Subscription}
     */
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
    public static Object[] getFilteredMessagesList(ArrayList msgArrayList, int startingIndex,
                                                   int maxMsgCount) {
        Object[] messageArray;
        int resultSetSize = maxMsgCount;

        ArrayList<Object> resultList = new ArrayList<Object>();
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
     * Gets the TCP connection url to reach the broker by using the currently logged in user and the
     * access key for the user, generated by andes Authentication Service
     *
     * @param userName  - currently logged in user
     * @param accessKey - the key (uuid) generated by authentication service
     * @return connection url
     */
    public static String getTCPConnectionURL(String userName, String accessKey)
            throws FileNotFoundException,
                   XMLStreamException, UnknownHostException {

        // getting host address from andes configuration mentioned in broker.xml
        String andesConfigHostAddress = String.valueOf(AndesConfigurationManager.readValue(AndesConfiguration.TRANSPORTS_BIND_ADDRESS));
        String hostAddress = InetAddress.getByName(andesConfigHostAddress).getHostAddress();

        // getting port from andes configuration mentioned in broker.xml
        Integer carbonPort = AndesConfigurationManager.readValue(AndesConfiguration.TRANSPORTS_AMQP_PORT);
        String port = String.valueOf(carbonPort);

        // getting ssl port from andes configuration mentioned in broker.xml
        // these are the properties which needs to be passed when ssl is enabled
        String sslPort = String.valueOf(AndesConfigurationManager.readValue(AndesConfiguration.TRANSPORTS_AMQP_SSL_PORT));

        File confFile = new File(System.getProperty(ServerConstants.CARBON_HOME) + ANDES_CONF_DIR + ANDES_CONF_FILE);
        OMElement docRootNode = new StAXOMBuilder(new FileInputStream(confFile)).
                getDocumentElement();
        OMElement connectorNode = docRootNode.getFirstChildWithName(
                new QName(ANDES_CONF_CONNECTOR_NODE));
        OMElement sslNode = connectorNode.getFirstChildWithName(
                new QName(ANDES_CONF_SSL_NODE));
        OMElement sslKeyStorePath = sslNode.getFirstChildWithName(
                new QName(ANDES_CONF_SSL_KEYSTORE_PATH));
        OMElement sslKeyStorePwd = sslNode.getFirstChildWithName(
                new QName(ANDES_CONF_SSL_KEYSTORE_PASSWORD));
        OMElement sslTrustStorePath = sslNode.getFirstChildWithName(
                new QName(ANDES_CONF_SSL_TRUSTSTORE_PATH));
        OMElement sslTrustStorePwd = sslNode.getFirstChildWithName(
                new QName(ANDES_CONF_SSL_TRUSTSTORE_PASSWORD));

        String keyStorePath = sslKeyStorePath.getText();
        String trustStorePath = sslTrustStorePath.getText();
        String sslKeyStorePassword = sslKeyStorePwd.getText();
        String sslTrustStorePassword = sslTrustStorePwd.getText();

        // as it is not possible to obtain the password of for the given user, we use service generated access key
        // to authenticate the user
        if (isSSLOnly()) {
            //"amqp://admin:admin@carbon/carbon?brokerlist='tcp://{hostname}:{port}?ssl='true'&trust_store
            // ='{trust_store_path}'&trust_store_password='{trust_store_pwd}'&key_store='{keystore_path
            // }'&key_store_password='{key_store_pwd}''";

            return "amqp://" + userName + ":" + accessKey + "@" + CARBON_CLIENT_ID + "/" +
                   CARBON_VIRTUAL_HOST_NAME + "?brokerlist='tcp://" + hostAddress +
                   ":" + sslPort + "?ssl='true'&trust_store='" + trustStorePath +
                   "'&trust_store_password='" + sslTrustStorePassword + "'&key_store='" +
                   keyStorePath + "'&key_store_password='" + sslKeyStorePassword + "''";
        } else {
            // amqp://{username}:{accesskey}@carbon/carbon?brokerlist='tcp://{hostname}:{port}'

            return "amqp://" + userName + ":" + accessKey + "@" + CARBON_CLIENT_ID + "/" +
                   CARBON_VIRTUAL_HOST_NAME + "?brokerlist='tcp://" +
                   hostAddress + ":" + port + "'";
        }
    }

    /**
     * Gets properties of a JMS message
     *
     * @param message message object
     * @return String value of all the properties of the message
     * @throws JMSException
     */
    public static String getMsgProperties(Message message) throws JMSException {

        Enumeration propertiesEnu = message.getPropertyNames();
        StringBuilder sb = new StringBuilder("");
        if (propertiesEnu != null) {
            while (propertiesEnu.hasMoreElements()) {
                String propName = (String) propertiesEnu.nextElement();
                sb.append(propName).append(" = ").append(message.getStringProperty(propName));
                sb.append(", ");
            }
        }

        return sb.toString();
    }

    /**
     * Determines the type of the JMS message
     *
     * @param message - input message
     * @return type of the message as a string
     */
    public static String getMsgContentType(Message message) {

        String contentType = "";
        if (message instanceof TextMessage) {
            contentType = "Text";
        } else if (message instanceof ObjectMessage) {
            contentType = "Object";
        } else if (message instanceof MapMessage) {
            contentType = "Map";
        } else if (message instanceof StreamMessage) {
            contentType = "Stream";
        } else if (message instanceof BytesMessage) {
            contentType = "Byte";
        }

        return contentType;
    }

    /**
     * Gets the message content as a string, after verifying its type
     *
     * @param message - JMS Message
     * @return a string array of message content; a summary and the whole message
     * @throws JMSException
     */
    public static String[] getMessageContentAsString(Message message) throws JMSException {

        String messageContent[] = new String[2];
        String summaryMsg = "";
        String wholeMsg = "";

        StringBuilder sb = new StringBuilder();
        if (message != null) {
            if (message instanceof TextMessage) {
                String textMessage = ((TextMessage) message).getText();
                wholeMsg = StringEscapeUtils.escapeHtml(textMessage).trim();
                if (wholeMsg.length() >= 15) {
                    summaryMsg = wholeMsg.substring(0, 15);
                } else {
                    summaryMsg = wholeMsg;
                }
                if (wholeMsg.length() > MESSAGE_DISPLAY_LENGTH_MAX) {
                    wholeMsg = wholeMsg.substring(0, MESSAGE_DISPLAY_LENGTH_MAX - 3) + DISPLAY_CONTINUATION +
                               DISPLAY_LENGTH_EXCEEDED;
                }
            } else if (message instanceof ObjectMessage) {
                wholeMsg = "This Operation is Not Supported!";
                summaryMsg = "Not Supported";

            } else if (message instanceof MapMessage) {
                MapMessage mapMessage = ((MapMessage) message);
                Enumeration mapEnu = mapMessage.getMapNames();
                while (mapEnu.hasMoreElements()) {
                    String mapName = (String) mapEnu.nextElement();
                    String mapVal = mapMessage.getObject(mapName).toString();
                    wholeMsg = StringEscapeUtils.escapeHtml(sb.append(mapName).append(": ")
                                                                    .append(mapVal).append(", ").toString()).trim();

                }
                if (wholeMsg.length() >= 15) {
                    summaryMsg = wholeMsg.substring(0, 15);
                } else {
                    summaryMsg = wholeMsg;
                }

            } else if (message instanceof StreamMessage) {
                ((StreamMessage) message).reset();
                wholeMsg = getContentFromStreamMessage((StreamMessage) message, sb).trim();
                if (wholeMsg.length() >= 15) {
                    summaryMsg = wholeMsg.substring(0, 15);
                } else {
                    summaryMsg = wholeMsg;
                }

            } else if (message instanceof BytesMessage) {
                ((BytesMessage) message).reset();
                long messageLength = ((BytesMessage) message).getBodyLength();
                byte[] byteMsgArr = new byte[(int) messageLength];

                int index = ((BytesMessage) message).readBytes(byteMsgArr);
                for (int i = 0; i < index; i++) {
                    wholeMsg = sb.append(byteMsgArr[i]).append(" ").toString().trim();
                }

                if (wholeMsg.length() >= 15) {
                    summaryMsg = wholeMsg.substring(0, 15);
                } else {
                    summaryMsg = wholeMsg;
                }
            }
        }
        messageContent[0] = summaryMsg;
        messageContent[1] = wholeMsg;
        return messageContent;
    }

    /**
     * A stream message can have java primitives plus objects, as its content. This message it used to retrieve the
     *
     * @param streamMessage - input message
     * @param sb            - a string builder to build the whole message content
     * @return - complete message content inside the stream message
     * @throws JMSException
     */
    private static String getContentFromStreamMessage(StreamMessage streamMessage,
                                                      StringBuilder sb) throws JMSException {

        boolean eofReached = false;

        while (!eofReached) {

            try {
                Object obj = streamMessage.readObject();
                // obj could be null if the wire type is AbstractBytesTypedMessage.NULL_STRING_TYPE
                if (null != obj) {
                    sb.append(obj.toString()).append(", ");
                }
            } catch (MessageEOFException ex) {
                eofReached = true;
            }

        }

        return StringEscapeUtils.escapeHtml(sb.toString());
    }

    /**
     * Finds whether only SSL is allowed
     *
     * @return true if SSL is enabled, false otherwise.
     * @throws FileNotFoundException
     * @throws XMLStreamException
     */
    public static boolean isSSLOnly() throws FileNotFoundException, XMLStreamException {

        File confFile = new File(System.getProperty(ServerConstants.CARBON_HOME) + ANDES_CONF_DIR + ANDES_CONF_FILE);
        OMElement docRootNode = new StAXOMBuilder(new FileInputStream(confFile)).
                getDocumentElement();
        OMElement connectorNode = docRootNode.getFirstChildWithName(
                new QName(ANDES_CONF_CONNECTOR_NODE));
        OMElement sslNode = connectorNode.getFirstChildWithName(
                new QName(ANDES_CONF_SSL_NODE));
        OMElement sslOnlyNode = sslNode.getFirstChildWithName(
                new QName(ANDES_CONF_SSL_ONLY_NODE));

        return Boolean.parseBoolean(sslOnlyNode.getText());
    }
}
