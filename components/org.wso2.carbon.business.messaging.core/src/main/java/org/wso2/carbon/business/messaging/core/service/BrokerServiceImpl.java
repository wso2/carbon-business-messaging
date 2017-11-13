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
*  Unless required by applicable law or agreed to in writing,
*  software distributed under the License is distributed on an
*  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*  KIND, either express or implied.  See the License for the
*  specific language governing permissions and limitations
*  under the License.
*/

package org.wso2.carbon.business.messaging.core.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.andes.configuration.models.transport.TransportConfiguration;
import org.wso2.andes.server.registry.ApplicationRegistry;
import org.wso2.carbon.business.messaging.core.constants.BrokerConstants;
import org.wso2.carbon.business.messaging.core.internal.BrokerServiceDataHolder;
import org.wso2.carbon.business.messaging.core.internal.DataHolder;
import org.wso2.carbon.business.messaging.core.listeners.BrokerLifecycleListener;
import org.wso2.carbon.business.messaging.core.service.exception.ServiceConfigurationException;
import org.wso2.carbon.config.ConfigurationException;
import org.wso2.carbon.utils.StringUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Paths;

/**
 * The following class contains mutator methods for configuration values the broker.
 */
public class BrokerServiceImpl implements BrokerService {

    private static final Log log = LogFactory.getLog(BrokerServiceImpl.class);

    /**
     * The client ID for AMQP URL.
     */
    private static final String CARBON_CLIENT_ID = "carbon";

    /**
     * The Virtual host name for AMQL URL.
     */
    private static final String CARBON_VIRTUAL_HOST_NAME = "carbon";

    /**
     * The default host name for connections.
     */
    private static final String CARBON_DEFAULT_HOSTNAME = "localhost";

    /**
     * The directory location of the qpid configuration file
     */
    private String qpidConfigurationDirectoryPath = "/conf/advanced/";

    /**
     * Default domain separator.
     */
    private static final String DOMAIN_NAME_SEPARATOR = "@";

    /**
     * Tenant domain separator.
     */
    private static final String DOMAIN_NAME_SEPARATOR_INTERNAL = "!";

    /**
     * The access key for internal VM connection.
     */
    private String accessKey = "";

    /**
     * The default host name for connection.
     */
    private String hostname = CARBON_DEFAULT_HOSTNAME;

    /**
     * The default AMQP port.
     */
    private Integer amqpPort = 5672;

    /**
     * The default AMQP SSL port.
     */
    private Integer amqpSSLPort = 8672;

    /**
     * Creates a new Qpid Service.
     *
     * @param accessKey The access key for in VM connection string.
     */
    public BrokerServiceImpl(String accessKey) {
        this.accessKey = accessKey;
    }

    /**
     * Read configuration files and set ports and host names
     */
    public void loadConfigurations() throws ConfigurationException {
        // Get the hostname that Carbon runs on
        String andesConfigHostAddress = DataHolder.getInstance().getConfigProvider()
                .getConfigurationObject(TransportConfiguration.class).getAmqpConfiguration().getBindAddress();
        //AndesConfigurationManager.readValue(AndesConfiguration.TRANSPORTS_AMQP_BIND_ADDRESS);
        if (StringUtils.isNullOrEmpty(andesConfigHostAddress)) {
            try {
                hostname = InetAddress.getByName(andesConfigHostAddress).getHostAddress();
            } catch (UnknownHostException e) {
                hostname = CARBON_DEFAULT_HOSTNAME;
            }
        }

        // Read Qpid broker amqpPort from configuration file
        amqpPort = readPortFromConfig();

        // Read Qpid broker SSL amqpPort from configuration file
        amqpSSLPort = readSSLPortFromConfig();

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAccessKey() {
        return accessKey;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getClientID() {
        return CARBON_CLIENT_ID;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getVirtualHostName() {
        return CARBON_VIRTUAL_HOST_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getHostname() {
        return hostname;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer getAMQPPort() {
        return amqpPort;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAMQPPort(Integer amqpPort) {
        this.amqpPort = amqpPort;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Integer getAMQPSSLPort() {
        return amqpSSLPort;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAMQPSSLPort(Integer amqpSSLPort) {
        this.amqpSSLPort = amqpSSLPort;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getInVMConnectionURL(String username) {
        username = getInternalTenantUsername(username);

        // amqp://{username}:{accessKey}@carbon/carbon?brokerlist='vm://:1'
        return "amqp://" + username + ":" + accessKey + "@" + CARBON_CLIENT_ID + "/" + CARBON_VIRTUAL_HOST_NAME
                + "?brokerlist='vm://:1'";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTCPConnectionURL(String username, String password) {
        // amqp://{username}:{password}@carbon/carbon?brokerlist='tcp://{hostname}:{amqpPort}'
        return "amqp://" + username + ":" + password + "@" + CARBON_CLIENT_ID + "/" + CARBON_VIRTUAL_HOST_NAME
                + "?brokerlist='tcp://" + hostname + ":" + amqpPort + "'";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTCPConnectionURL(String username, String password, String clientID) {
        // amqp://{username}:{password}@{cliendID}/carbon?brokerlist='tcp://{hostname}:{amqpPort}'
        return "amqp://" + username + ":" + password + "@" + clientID + "/" + CARBON_VIRTUAL_HOST_NAME
                + "?brokerlist='tcp://" + hostname + ":" + amqpPort + "'";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getInternalTCPConnectionURL(String username, String password) {
        username = getInternalTenantUsername(username);

        // amqp://{username}:{password}@carbon/carbon?brokerlist='tcp://{hostname}:{amqpPort}'
        return "amqp://" + username + ":" + password + "@" + CARBON_CLIENT_ID + "/" + CARBON_VIRTUAL_HOST_NAME
                + "?brokerlist='tcp://" + hostname + ":" + amqpPort + "'";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getInternalTCPConnectionURL(String username, String password, String clientID) {
        username = getInternalTenantUsername(username);

        // amqp://{username}:{password}@{cliendID}/carbon?brokerlist='tcp://{hostname}:{amqpPort}'
        return "amqp://" + username + ":" + password + "@" + clientID + "/" + CARBON_VIRTUAL_HOST_NAME
                + "?brokerlist='tcp://" + hostname + ":" + amqpPort + "'";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getQpidHome() {
        String qpidPath = System.getProperty(BrokerConstants.QPID_CONF);

        if (qpidPath != null) {
            qpidConfigurationDirectoryPath = Paths.get(qpidPath).toString();
        }
        return System.getProperty("carbon.home") + qpidConfigurationDirectoryPath;
    }

    /**
     * Reads the AMQP amqpPort from configuration and calculates the offset amqpPort that should be used in the pack.
     *
     * @return Port used for AMQP transports with offset if specified.
     */
    private Integer readPortFromConfig() throws ConfigurationException {
        return DataHolder.getInstance().getConfigProvider().getConfigurationObject(TransportConfiguration.class)
                .getAmqpConfiguration().getDefaultConnection().getPort();
        //AndesConfigurationManager.readValue(AndesConfiguration.TRANSPORTS_AMQP_DEFAULT_CONNECTION_PORT);
    }

    /**
     * Reads the AMQP SSL port value from configuration.
     *
     * @return The port value
     */
    private Integer readSSLPortFromConfig() throws ConfigurationException {
        return DataHolder.getInstance().getConfigProvider().getConfigurationObject(TransportConfiguration.class)
                .getAmqpConfiguration().getSslConnection().getPort();
        //AndesConfigurationManager.readValue(AndesConfiguration.TRANSPORTS_AMQP_SSL_CONNECTION_PORT);
    }

    /**
     * Converts domain username to tenant format username by replacing "@" by "!".
     *
     * @param username The username.
     * @return The tenant username.
     */
    private String getInternalTenantUsername(String username) {
        // Replace @ with ! in tenant username as Qpid does not support @ in username
        // E.g. foo@bar.com -> foo!bar.com
        // Note : The Qpid authorization handler uses ! to extract domain name from username when authorizing
        return username.replace(DOMAIN_NAME_SEPARATOR, DOMAIN_NAME_SEPARATOR_INTERNAL);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getIfSSLOnly() throws ServiceConfigurationException {
        return ApplicationRegistry.getInstance().getConfiguration().getSSLOnly();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerBrokerLifecycleListener(BrokerLifecycleListener brokerLifecycleListener) {
        BrokerServiceDataHolder.getInstance().getBrokerLifecycleListeners().add(brokerLifecycleListener);

    }
}
