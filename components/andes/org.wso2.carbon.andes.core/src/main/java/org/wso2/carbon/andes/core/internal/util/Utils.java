/*
*  Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.wso2.andes.configuration.AndesConfigurationManager;
import org.wso2.andes.configuration.enums.AndesConfiguration;
import org.wso2.andes.configuration.modules.JKSStore;
import org.wso2.andes.kernel.AndesConstants;
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
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.io.FileNotFoundException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageEOFException;
import javax.jms.ObjectMessage;
import javax.jms.StreamMessage;
import javax.jms.TextMessage;
import javax.xml.stream.XMLStreamException;

/**
 * Provides common utilities for UI related functions and services.
 */
public class Utils {

    private static final String DIRECT_EXCHANGE = "amq.direct";
    private static final String TOPIC_EXCHANGE = "amq.topic";
    private static final String ANDES_CONF_DIR = "/repository/conf/advanced/";
    private static final String ANDES_CONF_FILE = "qpid-config.xml";
    private static final String ANDES_CONF_CONNECTOR_NODE = "connector";
    private static final String ANDES_CONF_SSL_NODE = "ssl";
    private static final String CARBON_CLIENT_ID = "carbon";
    private static final String CARBON_VIRTUAL_HOST_NAME = "carbon";
    private static final int CHARACTERS_TO_SHOW = 15;

    /**
     * Maximum size a message will be displayed on UI
     */
    public static final Integer MESSAGE_DISPLAY_LENGTH_MAX =
            AndesConfigurationManager.readValue(AndesConfiguration.MANAGEMENT_CONSOLE_MAX_DISPLAY_LENGTH_FOR_MESSAGE_CONTENT);

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
            queueName = tenantDomain + AndesConstants.TENANT_SEPARATOR + queueName;
        }
        return queueName;
    }

    /**
     * Method to retrieve the tenant domain of the current user.
     *
     * @return the domain name of the tenant
     */
    public static String getTenantDomain() {
        return CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
    }

    /**
     * Checks if a given user has admin privileges.
     *
     * @param username Name of the user
     * @return true if the user has admin rights or false otherwise
     * @throws org.wso2.carbon.andes.core.QueueManagerException if getting roles for the user fails
     */
    public static boolean isAdmin(String username) throws QueueManagerException {
        boolean isAdmin = false;

        try {
            UserRealm userRealm = QueueManagerServiceValueHolder.getInstance().getRealmService().getTenantUserRealm
                    (CarbonContext.getThreadLocalCarbonContext().getTenantId() <= 0 ?
                            MultitenantConstants.SUPER_TENANT_ID : CarbonContext.getThreadLocalCarbonContext()
                            .getTenantId());

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
     * Filter queues to suit the tenant domain.
     *
     * @param fullList Full queue list
     * @return List<Queue>
     */
    public static List<Queue> filterDomainSpecificQueues(List<Queue> fullList) {
        ArrayList<Queue> tenantFilteredQueues = new ArrayList<>();
        for (Queue queue : fullList) {
            if (isQueueInDomain(queue)) {
                tenantFilteredQueues.add(queue);
            }
        }
        return tenantFilteredQueues;
    }

    /**
     * Method to check if the queue is allowed in the tenant domain.
     *
     * @param queue The queue to check if allowed
     * @return true is queue is allowed
     */
    public static boolean isQueueInDomain(Queue queue) {
        String domainName = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        // If the current tenant is not the super tenant,
        if (!(domainName.equals(org.wso2.carbon.base.MultitenantConstants.SUPER_TENANT_DOMAIN_NAME))) {
            if (queue.getQueueName().startsWith(domainName)) {
                return true;
            }
        }
        //for super tenant load all queues not specific to a domain. That means queues created by external
        //JMS clients are visible, and those names should not have the tenant separator "/" in their queue names
        else {
            if (!queue.getQueueName().contains(AndesConstants.TENANT_SEPARATOR)) {
                return true;
            }
        }
        return false;
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
        ArrayList<Subscription> tenantFilteredSubscriptions = new ArrayList<>();

        //filter subscriptions belonging to the tenant domain
        if (domainName != null && !CarbonContext.getThreadLocalCarbonContext().getTenantDomain().
                equals(org.wso2.carbon.base.MultitenantConstants.SUPER_TENANT_DOMAIN_NAME)) {
            for (Subscription subscription : allSubscriptions) {

                //for queues filter by queue name queueName=<tenantDomain>/queueName
                //for temp topics filter by topic name topicName=<tenantDomain>/topicName
                //for durable topic subs filter by topic name topicName=<tenantDomain>/topicName
                if (subscription.getSubscribedQueueOrTopicName().startsWith(domainName + AndesConstants.TENANT_SEPARATOR)) {
                    tenantFilteredSubscriptions.add(subscription);
                }
            }
            //super tenant domain queue should not have '/'
        } else if (domainName != null && CarbonContext.getThreadLocalCarbonContext().getTenantDomain().
                equals(org.wso2.carbon.base.MultitenantConstants.SUPER_TENANT_DOMAIN_NAME)) {
            for (Subscription subscription : allSubscriptions) {
                if (!subscription.getSubscribedQueueOrTopicName().contains(AndesConstants.TENANT_SEPARATOR)) {
                    tenantFilteredSubscriptions.add(subscription);
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
        subscription.setDestination(subInfo[8]);
        subscription.setProtocolType(subInfo[9]);
        subscription.setDestinationType(subInfo[10]);


        return subscription;
    }

    /**
     * Filter the whole message list to fit for the page range
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

        ArrayList<Object> resultList = new ArrayList<>();
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
        String andesConfigHostAddress =
                String.valueOf(AndesConfigurationManager.readValue(AndesConfiguration.TRANSPORTS_AMQP_BIND_ADDRESS));
        String hostAddress = InetAddress.getByName(andesConfigHostAddress).getHostAddress();

        // getting port from andes configuration mentioned in broker.xml
        Integer carbonPort = AndesConfigurationManager.readValue(AndesConfiguration.TRANSPORTS_AMQP_DEFAULT_CONNECTION_PORT);
        String port = String.valueOf(carbonPort);

        // getting ssl port from andes configuration mentioned in broker.xml
        // these are the properties which needs to be passed when ssl is enabled
        String sslPort = String.valueOf(AndesConfigurationManager.readValue(AndesConfiguration.TRANSPORTS_AMQP_SSL_CONNECTION_PORT));

        JKSStore keyStore = AndesConfigurationManager.readValue(AndesConfiguration.TRANSPORTS_AMQP_SSL_CONNECTION_KEYSTORE);
        JKSStore trustStore = AndesConfigurationManager.readValue(AndesConfiguration.TRANSPORTS_AMQP_SSL_CONNECTION_TRUSTSTORE);

        // as it is not possible to obtain the password of for the given user, we use service generated access key
        // to authenticate the user
        if (isSSLOnly()) {
            //"amqp://admin:admin@carbon/carbon?brokerlist='tcp://{hostname}:{port}?ssl='true'&trust_store
            // ='{trust_store_path}'&trust_store_password='{trust_store_pwd}'&key_store='{keystore_path
            // }'&key_store_password='{key_store_pwd}''";

            return "amqp://" + userName + ":" + accessKey + "@" + CARBON_CLIENT_ID + "/" +
                   CARBON_VIRTUAL_HOST_NAME + "?brokerlist='tcp://" + hostAddress +
                   ":" + sslPort + "?ssl='true'&trust_store='" + trustStore.getStoreLocation() +
                   "'&trust_store_password='" + trustStore.getPassword() + "'&key_store='" +
                   keyStore.getStoreLocation() + "'&key_store_password='" + keyStore.getPassword() + "''";
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
                if (StringUtils.isNotEmpty(textMessage)) {
                    wholeMsg = StringEscapeUtils.escapeHtml(textMessage).trim();
                    if (wholeMsg.length() >= CHARACTERS_TO_SHOW) {
                        summaryMsg = wholeMsg.substring(0, CHARACTERS_TO_SHOW);
                    } else {
                        summaryMsg = wholeMsg;
                    }
                    if (wholeMsg.length() > MESSAGE_DISPLAY_LENGTH_MAX) {
                        wholeMsg = wholeMsg.substring(0, MESSAGE_DISPLAY_LENGTH_MAX - 3) +
                                   DISPLAY_CONTINUATION + DISPLAY_LENGTH_EXCEEDED;
                    }
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
                if (wholeMsg.length() >= CHARACTERS_TO_SHOW) {
                    summaryMsg = wholeMsg.substring(0, CHARACTERS_TO_SHOW);
                } else {
                    summaryMsg = wholeMsg;
                }

            } else if (message instanceof StreamMessage) {
                ((StreamMessage) message).reset();
                wholeMsg = getContentFromStreamMessage((StreamMessage) message, sb).trim();
                if (wholeMsg.length() >= CHARACTERS_TO_SHOW) {
                    summaryMsg = wholeMsg.substring(0, CHARACTERS_TO_SHOW);
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

                if (wholeMsg.length() >= CHARACTERS_TO_SHOW) {
                    summaryMsg = wholeMsg.substring(0, CHARACTERS_TO_SHOW);
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
     * A stream message can have java primitives plus objects, as its content. This method is used
     * for getting the valid message content from the stream.
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

        return (Boolean)AndesConfigurationManager.readValue(AndesConfiguration.TRANSPORTS_AMQP_SSL_CONNECTION_ENABLED) &&
                !(Boolean)AndesConfigurationManager.readValue(AndesConfiguration.TRANSPORTS_AMQP_DEFAULT_CONNECTION_ENABLED);
    }

    /**
     * Check whether a queue/topic belongs to given domain in order to avoid other tenant domains'
     * users operate on the given queue/topic
     *
     * @param tenantDomain - domain name of tenant
     * @param routingKey   - queue/topic name to be verified against tenantDomain
     * @return true if queue/topic belongs to given domain and false otherwise
     */
    public static boolean isOwnDomain(String tenantDomain, String routingKey) {
        boolean isOwnDomain = false;
        if (tenantDomain != null) {
            if ((routingKey.length() >= tenantDomain.length() + 1) && routingKey.substring(0,
                    tenantDomain.length() + 1).equals(tenantDomain + AndesConstants.TENANT_SEPARATOR)) {
                isOwnDomain = true;
            } else if (tenantDomain.equalsIgnoreCase("carbon.super")) {
                if (!routingKey.contains(AndesConstants.TENANT_SEPARATOR)) {
                    isOwnDomain = true;
                }
            }
        } else {
            // tenantDomain is null,this implies this is a normal user.
            if (!routingKey.contains(AndesConstants.TENANT_SEPARATOR)) {
                isOwnDomain = true;
            }
        }

        return isOwnDomain;
    }
}
