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
package org.wso2.carbon.andes.ui;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Options;
import org.apache.axis2.context.ConfigurationContext;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.andes.stub.AndesAdminServiceStub;
import org.wso2.carbon.andes.stub.admin.types.Queue;
import org.wso2.carbon.andes.stub.admin.types.Subscription;
import org.wso2.carbon.andes.ui.client.QueueReceiverClient;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.ui.CarbonUIUtil;
import org.wso2.carbon.utils.ServerConstants;

import javax.jms.JMSException;
import javax.naming.NamingException;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;

public class UIUtils {

    private static final String QPID_CONF_DIR = "/repository/conf/advanced/";
    private static final String ANDES_CONF_FILE = "andes-config.xml";
    private static final String QPID_CONF_CONNECTOR_NODE = "connector";
    private static final String QPID_CONF_SSL_NODE = "ssl";
    private static final String QPID_CONF_SSL_ONLY_NODE = "sslOnly";
    private static final String QPID_CONF_SSL_KEYSTORE_PATH = "keystorePath";
    private static final String QPID_CONF_SSL_KEYSTORE_PASSWORD = "keystorePassword";
    private static final String QPID_CONF_SSL_TRUSTSTORE_PATH = "truststorePath";
    private static final String QPID_CONF_SSL_TRUSTSTORE_PASSWORD = "truststorePassword";

    /** Maximum size a message will be displayed on UI */
    public static final int MESSAGE_DISPLAY_LENGTH_MAX = 4000;
    
    /** Shown to user has a indication that the particular message has more content than shown in UI */
    public static final String DISPLAY_CONTINUATION = "...";
    
    /** Message shown in UI if message content exceed the limit - Further enhancement, these needs to read from a resource bundle */
    public static final String DISPLAY_LENGTH_EXCEEDED = "Message Content is too large to display.";


    public static String getHtmlString(String message) {
        return message.replaceAll("<", "&lt;").replaceAll(">", "&gt;");

    }

    public static AndesAdminServiceStub getAndesAdminServiceStub(ServletConfig config,
                                                                           HttpSession session,
                                                                           HttpServletRequest request)
            throws AxisFault {
        String backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
        backendServerURL = backendServerURL + "AndesAdminService";
        ConfigurationContext configContext =
                (ConfigurationContext) config.getServletContext().getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
        AndesAdminServiceStub stub = new AndesAdminServiceStub(configContext,backendServerURL);
        String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
        if (cookie != null) {
            Options option = stub._getServiceClient().getOptions();
            option.setManageSession(true);
            option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, cookie);
        }

        return stub;
    }

    /**
     * filter the full queue list to suit the range
     * @param fullList
     * @param startingIndex
     * @param maxQueueCount
     * @return  Queue[]
     */
    public static Queue[] getFilteredQueueList(Queue[] fullList ,int startingIndex, int maxQueueCount) {
        Queue[] queueDetailsArray;
        int resultSetSize = maxQueueCount;

        ArrayList<Queue> resultList = new ArrayList<Queue>();
        for(Queue aQueue : fullList) {
            resultList.add(aQueue);
        }

        if ((resultList.size() - startingIndex) < maxQueueCount) {
            resultSetSize = (resultList.size() - startingIndex);
        }
        queueDetailsArray = new Queue[resultSetSize];
        int index = 0;
        int queueDetailsIndex = 0;
        for (Queue queueDetail : resultList) {
            if (startingIndex == index || startingIndex < index) {
                queueDetailsArray[queueDetailsIndex] = new Queue();

                queueDetailsArray[queueDetailsIndex].setQueueName(queueDetail.getQueueName());
                queueDetailsArray[queueDetailsIndex].setMessageCount(queueDetail.getMessageCount());
                queueDetailsIndex++;
                if (queueDetailsIndex == maxQueueCount) {
                    break;
                }
            }
            index++;
        }

        return queueDetailsArray;
    }


    public static Subscription[] getFilteredSubscriptionList(Subscription[] fullList ,int startingIndex, int maxSubscriptionCount) {
        Subscription[] subscriptionDetailsArray;
        int resultSetSize = maxSubscriptionCount;

        ArrayList<Subscription> resultList = new ArrayList<Subscription>();
        for(Subscription sub : fullList) {
            resultList.add(sub);
        }

        if ((resultList.size() - startingIndex) < maxSubscriptionCount) {
            resultSetSize = (resultList.size() - startingIndex);
        }
        subscriptionDetailsArray = new Subscription[resultSetSize];
        int index = 0;
        int subscriptionDetailsIndex = 0;
        for (Subscription subscriptionDetail : resultList) {
            if (startingIndex == index || startingIndex < index) {
                subscriptionDetailsArray[subscriptionDetailsIndex] = new Subscription();

                subscriptionDetailsArray[subscriptionDetailsIndex].setSubscriptionIdentifier(subscriptionDetail.getSubscriptionIdentifier());
                subscriptionDetailsArray[subscriptionDetailsIndex].setSubscribedQueueOrTopicName(subscriptionDetail.getSubscribedQueueOrTopicName());
                subscriptionDetailsArray[subscriptionDetailsIndex].setSubscriberQueueBoundExchange(subscriptionDetail.getSubscriberQueueBoundExchange());
                subscriptionDetailsArray[subscriptionDetailsIndex].setSubscriberQueueName(subscriptionDetail.getSubscriberQueueName());
                subscriptionDetailsArray[subscriptionDetailsIndex].setSubscriptionIdentifier(subscriptionDetail.getSubscriptionIdentifier());
                subscriptionDetailsArray[subscriptionDetailsIndex].setDurable(subscriptionDetail.getDurable());
                subscriptionDetailsArray[subscriptionDetailsIndex].setActive(subscriptionDetail.getActive());
                subscriptionDetailsArray[subscriptionDetailsIndex].setNumberOfMessagesRemainingForSubscriber(subscriptionDetail.getNumberOfMessagesRemainingForSubscriber());
                subscriptionDetailsArray[subscriptionDetailsIndex].setSubscriberNodeAddress(subscriptionDetail.getSubscriberNodeAddress());

                subscriptionDetailsIndex++;
                if (subscriptionDetailsIndex == maxSubscriptionCount) {
                    break;
                }
            }
            index++;
        }

        return subscriptionDetailsArray;
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
        String portOffset = System.getProperty("portOffset",
                carbonConfig.getFirstProperty(CARBON_CONFIG_PORT_OFFSET));

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

    /**
     * Gets the message count of a given queue at that moment
     *
     * @param queueList - list of all queues available
     * @param queuename - the given queue
     * @return - message count of the given queue
     */

    public static long getCurrentMessageCountInQueue(Queue[] queueList, String queuename) {
        long messageCount = 0;
        if (queueList != null) {
            for (Queue queue : queueList) {
                if (queue.getQueueName().equals(queuename)) {
                    messageCount = queue.getMessageCount();
                }
            }
        }

        return messageCount;
    }

    public static void purgeQueue(String queuename, String username, String accesskey, Queue[] queueList) throws NamingException, JMSException, FileNotFoundException, XMLStreamException {
        QueueReceiverClient qrClient;
        int purgedMessageCount;
        long currentMsgCount = UIUtils.getCurrentMessageCountInQueue(queueList, queuename);
        int time_out = 0;
        while (currentMsgCount != 0 && time_out != 15) {
            qrClient = new QueueReceiverClient();
            javax.jms.Queue queue = qrClient.registerReceiver(queuename, username, accesskey);
            purgedMessageCount = qrClient.purgeQueue(queue);
            currentMsgCount = currentMsgCount - purgedMessageCount;
            qrClient.closeReceiver();
            time_out ++;

        }
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
