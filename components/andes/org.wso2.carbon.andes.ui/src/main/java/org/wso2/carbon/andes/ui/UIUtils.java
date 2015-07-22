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
package org.wso2.carbon.andes.ui;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Options;
import org.apache.axis2.context.ConfigurationContext;
import org.wso2.andes.configuration.AndesConfigurationManager;
import org.wso2.andes.configuration.enums.AndesConfiguration;
import org.wso2.andes.configuration.modules.JKSStore;
import org.wso2.andes.kernel.AndesException;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.andes.stub.AndesAdminServiceStub;
import org.wso2.carbon.andes.stub.admin.types.Queue;
import org.wso2.carbon.andes.stub.admin.types.QueueRolePermission;
import org.wso2.carbon.andes.stub.admin.types.Subscription;
import org.wso2.carbon.ui.CarbonUIUtil;
import org.wso2.carbon.utils.ServerConstants;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;

/**
 * This class is used by the UI to connect to services and provides utilities. Used by JSP pages.
 */
public class UIUtils {

    private static final String ANDES_CONF_DIR = "/repository/conf/advanced/";
    private static final String ANDES_CONF_FILE = "qpid-config.xml";
    private static final String ANDES_CONF_CONNECTOR_NODE = "connector";
    private static final String ANDES_CONF_SSL_NODE = "ssl";
    private static final String CARBON_CLIENT_ID = "carbon";
    private static final String CARBON_VIRTUAL_HOST_NAME = "carbon";
    private static final String CARBON_DEFAULT_HOSTNAME = "localhost";
    private static final String ANDES_ADMIN_SERVER_NAME = "AndesAdminService";

    /**
     * Gets html string value encoded. i.e < becomes &lt; and > becomes &gt;
     * Suppressing warning of unused declaration as it used by the UI (JSP pages)
     *
     * @param message the string value
     * @return encoded string value
     */
    @SuppressWarnings("UnusedDeclaration")
    public static String getHtmlString(String message) {
        return message.replaceAll("<", "&lt;").replaceAll(">", "&gt;");

    }

    /**
     * Gets the AndesAdminServices stub.
     *
     * @param config  the servlet configuration
     * @param session the http session
     * @param request the http servlet request
     * @return an AndesAdminServiceStub
     * @throws AxisFault
     */
    public static AndesAdminServiceStub getAndesAdminServiceStub(ServletConfig config,
                                                                 HttpSession session,
                                                                 HttpServletRequest request)
            throws AxisFault {
        String backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
        backendServerURL = backendServerURL + ANDES_ADMIN_SERVER_NAME;
        ConfigurationContext configContext =
                (ConfigurationContext) config.getServletContext().getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
        AndesAdminServiceStub stub = new AndesAdminServiceStub(configContext, backendServerURL);
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
     * Suppressing warning of unused declaration as it used by the UI (JSP pages)
     *
     * @param fullList      a complete list of queues
     * @param startingIndex the starting index to start from the queue list
     * @param maxQueueCount the maximum queue count to limit
     * @return an array of queues
     */
    @SuppressWarnings("UnusedDeclaration")
    public static Queue[] getFilteredQueueList(Queue[] fullList, int startingIndex,
                                               int maxQueueCount) {
        Queue[] queueDetailsArray;
        int resultSetSize = maxQueueCount;

        ArrayList<Queue> resultList = new ArrayList<Queue>();
        Collections.addAll(resultList, fullList);

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

    /**
     * Gets filtered list of subscription list
     * Suppressing warning of unused declaration as it used by the UI (JSP pages)
     *
     * @param fullList             the complete list of subscriptions
     * @param startingIndex        the starting index to start from the subscription list
     * @param maxSubscriptionCount the maximum subscription count to limit
     * @return an array of subscriptions
     */
    @SuppressWarnings("UnusedDeclaration")
    public static Subscription[] getFilteredSubscriptionList(Subscription[] fullList,
                                                             int startingIndex,
                                                             int maxSubscriptionCount) {
        Subscription[] subscriptionDetailsArray;
        int resultSetSize = maxSubscriptionCount;

        ArrayList<Subscription> resultList = new ArrayList<Subscription>();
        Collections.addAll(resultList, fullList);

        if ((resultList.size() - startingIndex) < maxSubscriptionCount) {
            resultSetSize = (resultList.size() - startingIndex);
        }
        subscriptionDetailsArray = new Subscription[resultSetSize];
        int index = 0;
        int subscriptionDetailsIndex = 0;
        for (Subscription subscriptionDetail : resultList) {
            if (startingIndex == index || startingIndex < index) {
                subscriptionDetailsArray[subscriptionDetailsIndex] = new Subscription();

                subscriptionDetailsArray[subscriptionDetailsIndex].setSubscriptionIdentifier(subscriptionDetail
                                                                                                     .getSubscriptionIdentifier());
                subscriptionDetailsArray[subscriptionDetailsIndex].setSubscribedQueueOrTopicName(subscriptionDetail
                                                                                                         .getSubscribedQueueOrTopicName());
                subscriptionDetailsArray[subscriptionDetailsIndex].setSubscriberQueueBoundExchange(subscriptionDetail
                                                                                                           .getSubscriberQueueBoundExchange());
                subscriptionDetailsArray[subscriptionDetailsIndex].setSubscriberQueueName(subscriptionDetail
                                                                                                  .getSubscriberQueueName());
                subscriptionDetailsArray[subscriptionDetailsIndex].setSubscriptionIdentifier(subscriptionDetail
                                                                                                     .getSubscriptionIdentifier());
                subscriptionDetailsArray[subscriptionDetailsIndex].setDurable(subscriptionDetail
                                                                                      .getDurable());
                subscriptionDetailsArray[subscriptionDetailsIndex].setActive(subscriptionDetail
                                                                                     .getActive());
                subscriptionDetailsArray[subscriptionDetailsIndex].setNumberOfMessagesRemainingForSubscriber
                        (subscriptionDetail.getNumberOfMessagesRemainingForSubscriber());
                subscriptionDetailsArray[subscriptionDetailsIndex].setSubscriberNodeAddress(subscriptionDetail
                                                                                                    .getSubscriberNodeAddress());
                subscriptionDetailsArray[subscriptionDetailsIndex].setDestination
                        (subscriptionDetail.getDestination());

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
     * Gets the TCP connection url to reach the broker by using the currently logged in user and the access key for
     * the user, generated by andes Authentication Service
     * Suppressing warning of unused declaration as it used by the UI (JSP pages)
     *
     * @param userName  - currently logged in user
     * @param accessKey - the key (uuid) generated by authentication service
     * @return the tcp connection url
     */
    public static String getTCPConnectionURL(String userName, String accessKey)
            throws FileNotFoundException,
                   XMLStreamException, AndesException {
        // amqp://{username}:{accesskey}@carbon/carbon?brokerlist='tcp://{hostname}:{port}'

        String CARBON_PORT = String.valueOf(AndesConfigurationManager.readValue(AndesConfiguration.TRANSPORTS_AMQP_DEFAULT_CONNECTION_PORT));

        // these are the properties which needs to be passed when ssl is enabled
        String CARBON_SSL_PORT = String.valueOf(AndesConfigurationManager.readValue(AndesConfiguration.TRANSPORTS_AMQP_SSL_CONNECTION_PORT));

        File confFile = new File(System.getProperty(ServerConstants.CARBON_HOME) + ANDES_CONF_DIR + ANDES_CONF_FILE);
        OMElement docRootNode = new StAXOMBuilder(new FileInputStream(confFile)).
                getDocumentElement();
        OMElement connectorNode = docRootNode.getFirstChildWithName(
                new QName(ANDES_CONF_CONNECTOR_NODE));
        OMElement sslNode = connectorNode.getFirstChildWithName(
                new QName(ANDES_CONF_SSL_NODE));

        JKSStore keyStore = AndesConfigurationManager.readValue(AndesConfiguration
                .TRANSPORTS_AMQP_SSL_CONNECTION_KEYSTORE);
        JKSStore trustStore = AndesConfigurationManager.readValue(AndesConfiguration
                .TRANSPORTS_AMQP_SSL_CONNECTION_TRUSTSTORE);

        // as it is nt possible to obtain the password of for the given user, we use service generated access key
        // to authenticate the user

        if (isSSLOnly()) {
            //"amqp://admin:admin@carbon/carbon?brokerlist='tcp://{hostname}:{port}?ssl='true'&trust_store
            // ='{trust_store_path}'&trust_store_password='{trust_store_pwd}'&key_store='{keystore_path
            // }'&key_store_password='{key_store_pwd}''";

            return "amqp://" + userName + ":" + accessKey + "@" + CARBON_CLIENT_ID + "/" +
                   CARBON_VIRTUAL_HOST_NAME + "?brokerlist='tcp://" + CARBON_DEFAULT_HOSTNAME +
                   ":" + CARBON_SSL_PORT + "?ssl='true'&trust_store='" + trustStore.getStoreLocation() +
                   "'&trust_store_password='" + trustStore.getPassword() + "'&key_store='" +
                   keyStore.getStoreLocation() + "'&key_store_password='" + trustStore.getPassword() + "''";
        } else {
            return "amqp://" + userName + ":" + accessKey + "@" + CARBON_CLIENT_ID + "/" +
                   CARBON_VIRTUAL_HOST_NAME + "?brokerlist='tcp://" + CARBON_DEFAULT_HOSTNAME +
                   ":" + CARBON_PORT + "'";
        }
    }

    /**
     * Checks if its SSL
     *
     * @return true if its SSL, false otherwise.
     * @throws FileNotFoundException
     * @throws XMLStreamException
     */
    public static boolean isSSLOnly() throws FileNotFoundException, XMLStreamException {

        return (Boolean)AndesConfigurationManager.readValue(AndesConfiguration.TRANSPORTS_AMQP_SSL_CONNECTION_ENABLED) &&
                !(Boolean)AndesConfigurationManager.readValue(AndesConfiguration.TRANSPORTS_AMQP_DEFAULT_CONNECTION_ENABLED);
    }

    /**
     * Filter the full user-roles list to suit the range.
     * Suppressing warning of unused declaration as it used by the UI (JSP pages)
     *
     * @param allPermissions    full list of roles
     * @param startingIndex     starting index to filter
     * @param maxRolesCount     maximum number of roles that the filtered list can contain
     * @return ArrayList<QueueRolePermission>
     */
    @SuppressWarnings("UnusedDeclaration")
    public static ArrayList<QueueRolePermission> getFilteredRoleList
    (ArrayList<QueueRolePermission> allPermissions, int startingIndex, int maxRolesCount) {
        int numberOfPermissionsToShow = maxRolesCount;

        // Calculating the amount of permissions to show
        if ((allPermissions.size() - startingIndex) < maxRolesCount) {
            numberOfPermissionsToShow = (allPermissions.size() - startingIndex);
        }

        /*
         * Add permissions to list from the given starting index to the calculated amount of
         * permissions to show.
        */
        ArrayList<QueueRolePermission> permissionList = new ArrayList<QueueRolePermission>();
        for (int i = startingIndex; i < startingIndex + numberOfPermissionsToShow; i++) {
            permissionList.add(allPermissions.get(i));
        }

        return permissionList;
    }
}
