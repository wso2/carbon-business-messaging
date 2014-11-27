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

package org.wso2.carbon.andes.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.andes.configuration.AndesConfigurationManager;
import org.wso2.andes.configuration.enums.AndesConfiguration;
import org.wso2.andes.kernel.AndesException;
import org.wso2.andes.server.registry.ApplicationRegistry;
import org.wso2.carbon.andes.commons.QueueDetails;
import org.wso2.carbon.andes.commons.SubscriptionDetails;
import org.wso2.carbon.andes.commons.registry.RegistryClient;
import org.wso2.carbon.andes.commons.registry.RegistryClientException;
import org.wso2.carbon.andes.service.exception.ConfigurationException;
import org.wso2.carbon.andes.internal.QpidServiceDataHolder;
import org.wso2.carbon.base.api.ServerConfigurationService;
import org.wso2.carbon.utils.NetworkUtils;
import org.wso2.carbon.utils.ServerConstants;
import java.net.SocketException;

public class QpidServiceImpl implements QpidService {

    private static final Log log = LogFactory.getLog(QpidServiceImpl.class);

    private static String CARBON_CLIENT_ID = "carbon";
    private static String CARBON_VIRTUAL_HOST_NAME = "carbon";
    private static String CARBON_DEFAULT_HOSTNAME = "localhost";
    private static int CARBON_DEFAULT_PORT_OFFSET = 0;

    private static final String QPID_CONF_DIR = "/repository/conf/advanced/";

    private static String CARBON_CONFIG_PORT_OFFSET_NODE = "Ports.Offset";

    private static final String DOMAIN_NAME_SEPARATOR = "@";
    private static final String DOMAIN_NAME_SEPARATOR_INTERNAL = "!";

    private String accessKey = "";
    private String hostname = "";
    private Integer amqpPort = 5672;
    private Integer amqpSSLPort = 8672;
    private Integer mqttPort = 1883;
    private Integer mqttSSLPort = 8883;

    public  QpidServiceImpl(String accessKey) {
        this.accessKey = accessKey;
    }

    /**
     * Read configuration files and set ports and host names
     */
    public void loadConfigurations() throws ConfigurationException, AndesException {
        // Get the hostname that Carbon runs on
        try {
            hostname = NetworkUtils.getLocalHostname();
        } catch (SocketException e) {
            hostname = CARBON_DEFAULT_HOSTNAME;
        }

        // Read Qpid broker amqpPort from configuration file
        amqpPort = readPortFromConfig();

        // Read Qpid broker SSL amqpPort from configuration file
        amqpSSLPort = readSSLPortFromConfig();

        // Read MQTT amqpPort from configuration file
        mqttPort = readMQTTPortFromConfig();

        mqttSSLPort = readMQTTSSLPortFromConfig();

    }

    public String getAccessKey() {
        return accessKey;
    }

    public String getClientID() {
        return CARBON_CLIENT_ID;
    }

    public String getVirtualHostName() {
        return CARBON_VIRTUAL_HOST_NAME;
    }

    public String getHostname() {
        return hostname;
    }

    public Integer getAMQPPort() {
        return amqpPort;
    }

    public void setAMQPPort(Integer amqpPort) {
        this.amqpPort = amqpPort;
    }

    public Integer getAMQPSSLPort() {
        return amqpSSLPort;
    }

    public void setAMQPSSLPort(Integer amqpSSLPort) {
        this.amqpSSLPort = amqpSSLPort;
    }

    public Integer getMqttPort() {
        return mqttPort;
    }

    public void setMqttPort(Integer mqttPort) {
        this.mqttPort = mqttPort;
    }


    public Integer getMqttSSLPort() {
        return mqttSSLPort;
    }

    public void setMqttSSLPort(Integer mqttSSLPort) {
        this.mqttSSLPort = mqttSSLPort;
    }

    public String getInVMConnectionURL(String username) {
        username = getInternalTenantUsername(username);

        // amqp://{username}:{accessKey}@carbon/carbon?brokerlist='vm://:1'
        return "amqp://" + username + ":" + accessKey + "@" + CARBON_CLIENT_ID + "/" +
                CARBON_VIRTUAL_HOST_NAME + "?brokerlist='vm://:1'";
    }

    public String getTCPConnectionURL(String username, String password) {
        // amqp://{username}:{password}@carbon/carbon?brokerlist='tcp://{hostname}:{amqpPort}'
        return "amqp://" + username + ":" + password + "@" + CARBON_CLIENT_ID + "/" +
                CARBON_VIRTUAL_HOST_NAME + "?brokerlist='tcp://" + hostname + ":" + amqpPort + "'";
    }

    public String getTCPConnectionURL(String username, String password, String clientID) {
        // amqp://{username}:{password}@{cliendID}/carbon?brokerlist='tcp://{hostname}:{amqpPort}'
        return "amqp://" + username + ":" + password + "@" + clientID + "/" +
                CARBON_VIRTUAL_HOST_NAME + "?brokerlist='tcp://" + hostname + ":" + amqpPort + "'";
    }

    public String getInternalTCPConnectionURL(String username, String password) {
        username = getInternalTenantUsername(username);

        // amqp://{username}:{password}@carbon/carbon?brokerlist='tcp://{hostname}:{amqpPort}'
        return "amqp://" + username + ":" + password + "@" + CARBON_CLIENT_ID + "/" +
                CARBON_VIRTUAL_HOST_NAME + "?brokerlist='tcp://" + hostname + ":" + amqpPort + "'";
    }

    public String getInternalTCPConnectionURL(String username, String password, String clientID) {
        username = getInternalTenantUsername(username);

        // amqp://{username}:{password}@{cliendID}/carbon?brokerlist='tcp://{hostname}:{amqpPort}'
        return "amqp://" + username + ":" + password + "@" + clientID + "/" +
                CARBON_VIRTUAL_HOST_NAME + "?brokerlist='tcp://" + hostname + ":" + amqpPort + "'";
    }

    public String getQpidHome() {
        return System.getProperty(ServerConstants.CARBON_HOME) + QPID_CONF_DIR;
    }

    public QueueDetails[] getQueues(boolean isDurable) {
        QueueDetails[] queueDetails = null;

        try {
            queueDetails = RegistryClient.getQueues();
        } catch (RegistryClientException e) {
            log.error("Error while retrieving queue details.", e);
        }

        return queueDetails;
    }

    public SubscriptionDetails[] getSubscriptions(String topic, boolean isDurable) {
        SubscriptionDetails[] subsDetails = null;

        try {
            subsDetails = RegistryClient.getSubscriptions(topic);
        } catch (RegistryClientException e) {
            log.error("Error while retrieving subscription details", e);
        }

        return subsDetails;
    }

    /***
     * Reads the AMQP amqpPort fro configuration and calculates the offset amqpPort that should be used in the pack.
     * @return Port used for AMQP transports with offset if specified.
     * @throws AndesException
     */
    private Integer readPortFromConfig() throws AndesException {
        return AndesConfigurationManager.getInstance().readConfigurationValue(AndesConfiguration.TRANSPORTS_AMQP_PORT);
    }

    private Integer readSSLPortFromConfig() throws AndesException {
        return AndesConfigurationManager.getInstance().readConfigurationValue(AndesConfiguration.TRANSPORTS_AMQP_PORT);
    }

    private Integer readMQTTPortFromConfig() throws AndesException {
        return AndesConfigurationManager.getInstance().readConfigurationValue(AndesConfiguration.TRANSPORTS_MQTT_PORT);

    }

    private Integer readMQTTSSLPortFromConfig() throws AndesException {
        return AndesConfigurationManager.getInstance().readConfigurationValue(AndesConfiguration.TRANSPORTS_MQTT_SSL_PORT);

    }

    private String getInternalTenantUsername(String username) {
        // Replace @ with ! in tenant username as Qpid does not support @ in username
        // E.g. foo@bar.com -> foo!bar.com
        // Note : The Qpid authorization handler uses ! to extract domain name from username when authorizing
        return username.replace(DOMAIN_NAME_SEPARATOR, DOMAIN_NAME_SEPARATOR_INTERNAL);
    }

    public boolean getIfSSLOnly() throws ConfigurationException {

        return ApplicationRegistry.getInstance().getConfiguration().getSSLOnly();
    }

}
