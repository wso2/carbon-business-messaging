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

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.andes.configuration.VirtualHostsConfiguration;
import org.wso2.carbon.andes.commons.QueueDetails;
import org.wso2.carbon.andes.commons.SubscriptionDetails;
import org.wso2.carbon.andes.commons.registry.RegistryClient;
import org.wso2.carbon.andes.commons.registry.RegistryClientException;
import org.wso2.carbon.andes.service.exception.ConfigurationException;
import org.wso2.carbon.andes.internal.QpidServiceDataHolder;
import org.wso2.carbon.base.api.ServerConfigurationService;
import org.wso2.carbon.utils.NetworkUtils;
import org.wso2.carbon.utils.ServerConstants;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.SocketException;
import java.util.Iterator;

public class QpidServiceImpl implements QpidService {

    private static final Log log = LogFactory.getLog(QpidServiceImpl.class);

    private static String CARBON_CLIENT_ID = "carbon";
    private static String CARBON_VIRTUAL_HOST_NAME = "carbon";
    private static String CARBON_DEFAULT_HOSTNAME = "localhost";
    private static String CARBON_DEFAULT_PORT = "5672";
    private static String CARBON_DEFAULT_SSL_PORT = "8672";
    private static String CARBON_DEFAULT_MQTT_PORT = "1883";
    private static int CARBON_DEFAULT_PORT_OFFSET = 0;

    private static String THRIFT_DEFAULT_SERVER_HOST = "localhost";
    private static int THRIFT_DEFAULT_SERVER_PORT = 7611;

    private static final String QPID_CONF_DIR = "/repository/conf/advanced/";
    private static final String ANDES_CONF_FILE = "andes-config.xml";
    private static final String ANDES_VIRTUALHOST_CONF_FILE = "andes-virtualhosts.xml";
    private static final String QPID_CONF_CONNECTOR_NODE = "connector";
    private static final String QPID_CONF_SSL_NODE = "ssl";
    private static final String QPID_CONF_SSL_ONLY_NODE = "sslOnly";
    private static final String QPID_CONF_PORT_NODE = "port";
    private static final String QPID_CONF_SSL_PORT_NODE = "sslport";
    private static final String QPID_CONF_CLUSTER_NODE = "clustering";

    private static String CARBON_CONFIG_QPID_PORT_NODE = "Ports.EmbeddedQpid.BrokerPort";
    private static String CARBON_CONFIG_QPID_SSL_PORT_NODE = "Ports.EmbeddedQpid.BrokerSSLPort";
    private static String CARBON_CONFIG_PORT_OFFSET_NODE = "Ports.Offset";

    private static final String QPID_VIRTUALHOST_NODE = "virtualhost";
    private static final String QPID_VIRTUALHOST_NAME_NODE = "name";
    private static final String QPID_VIRTUALHOST_STORE_NODE = "store";
    private static final String QPID_VIRTUALHOST_COORDINATION_NODE = "coordination";
    private static final String QPID_VIRTUALHOST_MESSAGE_STORE_NODE = "messageStore";
    private static final String QPID_VIRTUALHOST_CLASS_ATTRIBUTE = "class";
    private static final String QPID_VIRTUALHOST_PROPERTY_NODE = "property";
    private static final String QPID_VIRTUALHOST_NAME_ATTRIBUTE = "name";
    private static final String QPID_VIRTUALHOST_CONTEXT_STORE_NODE = "andesContextStore";

    private static final String QPID_VIRTUALHOST_COORDINATION_THRIFT_SERVER_HOST_NODE = "thriftServerHost";
    private static final String QPID_VIRTUALHOST_COORDINATION_THRIFT_SERVER_PORT_NODE = "thriftServerPort";

    private static final String DOMAIN_NAME_SEPARATOR = "@";
    private static final String DOMAIN_NAME_SEPARATOR_INTERNAL = "!";

    private String accessKey = "";
    private String hostname = "";
    private String port = "";
    private String sslPort = "";
    private String mqttPort = "";
    private int portOffset = 0;
    private Boolean sslOnly;

    private String thriftServerHost;
    private int thriftServerPort;

    public QpidServiceImpl(String accessKey) {
        this.accessKey = accessKey;
    }

    /**
     * Read configuration files and set ports and host names
     */
    public void loadConfigurations() throws ConfigurationException {
        // Get the hostname that Carbon runs on
        try {
            hostname = NetworkUtils.getLocalHostname();
        } catch (SocketException e) {
            hostname = CARBON_DEFAULT_HOSTNAME;
        }

        // Read Port Offset
        portOffset = readPortOffset();

        // Read Qpid broker port from configuration file
        port = readPortFromConfig();

        // Read Qpid broker SSL port from configuration file
        sslPort = readSSLPortFromConfig();

        // Read MQTT port from configuration file
        mqttPort = readMQTTPortFromConfig();

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

    public String getPort() {
        return port;
    }

    public String getInVMConnectionURL(String username) {
        username = getInternalTenantUsername(username);

        // amqp://{username}:{accessKey}@carbon/carbon?brokerlist='vm://:1'
        return "amqp://" + username + ":" + accessKey + "@" + CARBON_CLIENT_ID + "/" +
                CARBON_VIRTUAL_HOST_NAME + "?brokerlist='vm://:1'";
    }

    public String getTCPConnectionURL(String username, String password) {
        // amqp://{username}:{password}@carbon/carbon?brokerlist='tcp://{hostname}:{port}'
        return "amqp://" + username + ":" + password + "@" + CARBON_CLIENT_ID + "/" +
                CARBON_VIRTUAL_HOST_NAME + "?brokerlist='tcp://" + hostname + ":" + port + "'";
    }

    public String getTCPConnectionURL(String username, String password, String clientID) {
        // amqp://{username}:{password}@{cliendID}/carbon?brokerlist='tcp://{hostname}:{port}'
        return "amqp://" + username + ":" + password + "@" + clientID + "/" +
                CARBON_VIRTUAL_HOST_NAME + "?brokerlist='tcp://" + hostname + ":" + port + "'";
    }

    public String getInternalTCPConnectionURL(String username, String password) {
        username = getInternalTenantUsername(username);

        // amqp://{username}:{password}@carbon/carbon?brokerlist='tcp://{hostname}:{port}'
        return "amqp://" + username + ":" + password + "@" + CARBON_CLIENT_ID + "/" +
                CARBON_VIRTUAL_HOST_NAME + "?brokerlist='tcp://" + hostname + ":" + port + "'";
    }

    public String getInternalTCPConnectionURL(String username, String password, String clientID) {
        username = getInternalTenantUsername(username);

        // amqp://{username}:{password}@{cliendID}/carbon?brokerlist='tcp://{hostname}:{port}'
        return "amqp://" + username + ":" + password + "@" + clientID + "/" +
                CARBON_VIRTUAL_HOST_NAME + "?brokerlist='tcp://" + hostname + ":" + port + "'";
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

    public String getSSLPort() {
        return sslPort;
    }

    public String getMQTTPort() {
        return mqttPort;
    }

    private int readPortOffset() {
        ServerConfigurationService carbonConfig = QpidServiceDataHolder.getInstance().getCarbonConfiguration();
        String portOffset = System.getProperty("portOffset",
                carbonConfig.getFirstProperty(CARBON_CONFIG_PORT_OFFSET_NODE));

        try {
            return ((portOffset != null) ? Integer.parseInt(portOffset.trim()) : CARBON_DEFAULT_PORT_OFFSET);
        } catch (NumberFormatException e) {
            return CARBON_DEFAULT_PORT_OFFSET;
        }
    }

    public VirtualHostsConfiguration readVirtualHostConfig() throws ConfigurationException {

        VirtualHostsConfiguration virtualHostsConfiguration = new VirtualHostsConfiguration();
        String vHostFilePath = getQpidHome() + ANDES_VIRTUALHOST_CONF_FILE;
        try {
            File confFile = new File(vHostFilePath);

            OMElement docRootNode = new StAXOMBuilder(new FileInputStream(confFile)).
                    getDocumentElement();
            OMElement virtualHostNode = docRootNode.getFirstChildWithName(
                    new QName(QPID_VIRTUALHOST_NODE));
            OMElement virtualHostNameNode = virtualHostNode.getFirstChildWithName(
                    new QName(QPID_VIRTUALHOST_NAME_NODE));
            String virtualHostName = virtualHostNameNode.getText().trim();
            OMElement carbonVirtualHost = virtualHostNode.getFirstChildWithName(
                    new QName(virtualHostName));
            OMElement storeElem = carbonVirtualHost.
                    getFirstChildWithName(new QName(QPID_VIRTUALHOST_STORE_NODE));

            // get message store class name
            OMElement storeClassElem = storeElem.getFirstChildWithName(
                    new QName(QPID_VIRTUALHOST_MESSAGE_STORE_NODE));
            String messageStoreClassName = storeClassElem.getAttributeValue(
                    new QName(QPID_VIRTUALHOST_CLASS_ATTRIBUTE)).trim();
            virtualHostsConfiguration.setMessageStoreClassName(messageStoreClassName);

            // get properties for message store
            Iterator itr = storeClassElem.getChildrenWithName(new QName
                    (QPID_VIRTUALHOST_PROPERTY_NODE));
            while (itr.hasNext()) {
                OMElement propertyElem = (OMElement) itr.next();
                String attribute = propertyElem.getAttributeValue(new QName(QPID_VIRTUALHOST_NAME_ATTRIBUTE));
                String value = propertyElem.getText().trim();
                virtualHostsConfiguration.addMessageStoreProperty(attribute, value);
            }

            // get andes context store class name
            OMElement contextStoreElem = storeElem.getFirstChildWithName(
                    new QName(QPID_VIRTUALHOST_CONTEXT_STORE_NODE));
            String andesContextStoreClassName = contextStoreElem.getAttributeValue(
                    new QName(QPID_VIRTUALHOST_CLASS_ATTRIBUTE)).trim();
            virtualHostsConfiguration.setAndesContextStoreClassName(andesContextStoreClassName);

            // get properties for andes context store
            itr = contextStoreElem.getChildrenWithName(new QName(QPID_VIRTUALHOST_PROPERTY_NODE));
            while (itr.hasNext()) {
                OMElement propertyElem = (OMElement) itr.next();
                String attribute = propertyElem.getAttributeValue(new QName(QPID_VIRTUALHOST_NAME_ATTRIBUTE));
                String value = propertyElem.getText().trim();
                virtualHostsConfiguration.addAndesContextStoreProperty(attribute, value);
            }

        } catch (FileNotFoundException e) {
            throw new ConfigurationException(vHostFilePath + " not found", e);
        } catch (XMLStreamException e) {
            throw new ConfigurationException("Error while reading " + vHostFilePath, e);
        }
        return virtualHostsConfiguration;
    }

    private String readPortFromConfig() throws ConfigurationException {
        String port = CARBON_DEFAULT_PORT;

        // Port defined in carbon.xml overrides others
        String portInCarbonConfig = readPortFromCarbonConfig();
        if (!portInCarbonConfig.isEmpty()) {
            port = portInCarbonConfig;
        } else {
            String portInQpidConfig = readPortFromQpidConfig();
            if (!portInQpidConfig.isEmpty()) {
                port = portInQpidConfig;
            }
        }

        // If the port offset is not valid set the default port value
        try {
            port = Integer.toString(Integer.parseInt(port) + portOffset);
        } catch (NumberFormatException e) {
            port = CARBON_DEFAULT_PORT;
        }

        return port;
    }

    /**
     * Read port from carbon.xml
     *
     * @return
     */
    private String readPortFromCarbonConfig() {
        ServerConfigurationService carbonConfig = QpidServiceDataHolder.getInstance().getCarbonConfiguration();
        String port = carbonConfig.getFirstProperty(CARBON_CONFIG_QPID_PORT_NODE);

        return ((port != null) ? port.trim() : "");
    }

    /**
     * Read port from andes-config.xml
     *
     * @return
     */
    private String readPortFromQpidConfig() throws ConfigurationException {
        String port = "";

        try {
            File confFile = new File(getQpidHome() + ANDES_CONF_FILE);

            OMElement docRootNode = new StAXOMBuilder(new FileInputStream(confFile)).
                    getDocumentElement();
            OMElement connectorNode = docRootNode.getFirstChildWithName(
                    new QName(QPID_CONF_CONNECTOR_NODE));
            OMElement portNode = connectorNode.getFirstChildWithName(
                    new QName(QPID_CONF_PORT_NODE));

            port = portNode.getText();
        } catch (FileNotFoundException e) {
            throw new ConfigurationException(getQpidHome() + ANDES_CONF_FILE + " not found", e);
        } catch (XMLStreamException e) {
            throw new ConfigurationException("Error while reading " + getQpidHome() + ANDES_CONF_FILE, e);
        }

        return ((port != null) ? port.trim() : "");
    }

    private String readSSLPortFromConfig() throws ConfigurationException {
        String port = CARBON_DEFAULT_SSL_PORT;

        // Port defined in carbon.xml overrides others
        String portInCarbonConfig = readSSLPortFromCarbonConfig();
        if (!portInCarbonConfig.isEmpty()) {
            port = portInCarbonConfig;
        } else {
            String portInQpidConfig = readSSLPortFromQpidConfig();
            if (!portInQpidConfig.isEmpty()) {
                port = portInQpidConfig;
            }
        }

        // Offset
        try {
            port = Integer.toString(Integer.parseInt(port) + portOffset);
        } catch (NumberFormatException e) {
            port = CARBON_DEFAULT_SSL_PORT;
        }

        return port;
    }

    private String readMQTTPortFromConfig() {
        String port = CARBON_DEFAULT_MQTT_PORT;

        // Offset
        try {
            port = Integer.toString(Integer.parseInt(port) + portOffset);
        } catch (NumberFormatException exception) {
            port = Integer.toString(Integer.parseInt(CARBON_DEFAULT_MQTT_PORT) + portOffset);
            log.error(
                    "Specified MQTT port is not in correct format. Using default MQTT port with " +
                            "offset (" + port + "). Please check MQTT configurations.", exception);
        }

        return port;
    }

    /**
     * Read port from carbon.xml
     *
     * @return
     */
    private String readSSLPortFromCarbonConfig() {
        ServerConfigurationService carbonConfig = QpidServiceDataHolder.getInstance().getCarbonConfiguration();
        String port = carbonConfig.getFirstProperty(CARBON_CONFIG_QPID_SSL_PORT_NODE);

        return ((port != null) ? port.trim() : "");
    }

    /**
     * Read port from andes-config.xml
     *
     * @return
     */
    private String readSSLPortFromQpidConfig() throws ConfigurationException {
        String port = "";

        try {
            File confFile = new File(getQpidHome() + ANDES_CONF_FILE);

            OMElement docRootNode = new StAXOMBuilder(new FileInputStream(confFile)).
                    getDocumentElement();
            OMElement connectorNode = docRootNode.getFirstChildWithName(
                    new QName(QPID_CONF_CONNECTOR_NODE));
            OMElement portNode = connectorNode.getFirstChildWithName(
                    new QName(QPID_CONF_SSL_PORT_NODE));

            port = portNode.getText();
        } catch (FileNotFoundException e) {
            throw new ConfigurationException(getQpidHome() + ANDES_CONF_FILE + " not found", e);
        } catch (XMLStreamException e) {
            throw new ConfigurationException("Error while reading " + getQpidHome() + ANDES_CONF_FILE, e);
        }

        return ((port != null) ? port.trim() : "");
    }

    private String getInternalTenantUsername(String username) {
        // Replace @ with ! in tenant username as Qpid does not support @ in username
        // E.g. foo@bar.com -> foo!bar.com
        // Note : The Qpid authorization handler uses ! to extract domain name from username when authorizing
        return username.replace(DOMAIN_NAME_SEPARATOR, DOMAIN_NAME_SEPARATOR_INTERNAL);
    }

    @Override
    public String getThriftServerHost() throws ConfigurationException {
        try {
            File confFile = new File(getQpidHome() + ANDES_VIRTUALHOST_CONF_FILE);
            OMElement docRootNode = new StAXOMBuilder(new FileInputStream(confFile)).
                    getDocumentElement();
            OMElement virtualHostNode = docRootNode.getFirstChildWithName(
                    new QName(QPID_VIRTUALHOST_NODE));
            OMElement virtualHostNameNode = virtualHostNode.getFirstChildWithName(
                    new QName(QPID_VIRTUALHOST_NAME_NODE));
            String virtualHostName = virtualHostNameNode.getText();
            OMElement carbonVirtualHost = virtualHostNode.getFirstChildWithName(
                    new QName(virtualHostName));
            OMElement coordinationElem = carbonVirtualHost.
                    getFirstChildWithName(new QName(QPID_VIRTUALHOST_COORDINATION_NODE));
            OMElement thriftServerHostStr = coordinationElem.getFirstChildWithName(
                    new QName(QPID_VIRTUALHOST_COORDINATION_THRIFT_SERVER_HOST_NODE));

            thriftServerHost = thriftServerHostStr.getText();
            if (thriftServerHost == null) {
                thriftServerHost = THRIFT_DEFAULT_SERVER_HOST;
            }
        } catch (FileNotFoundException e) {
            throw new ConfigurationException(getQpidHome() + ANDES_VIRTUALHOST_CONF_FILE + " not " +
                    "found", e);
        } catch (XMLStreamException e) {
            throw new ConfigurationException("Error while reading " + getQpidHome() + ANDES_VIRTUALHOST_CONF_FILE, e);
        }

        return thriftServerHost;
    }

    @Override
    public int getThriftServerPort() throws ConfigurationException {
        try {
            File confFile = new File(getQpidHome() + ANDES_VIRTUALHOST_CONF_FILE);
            OMElement docRootNode = new StAXOMBuilder(new FileInputStream(confFile)).
                    getDocumentElement();
            OMElement virtualHostNode = docRootNode.getFirstChildWithName(
                    new QName(QPID_VIRTUALHOST_NODE));
            OMElement virtualHostNameNode = virtualHostNode.getFirstChildWithName(
                    new QName(QPID_VIRTUALHOST_NAME_NODE));
            String virtualHostName = virtualHostNameNode.getText();
            OMElement carbonVirtualHost = virtualHostNode.getFirstChildWithName(
                    new QName(virtualHostName));
            OMElement coordinationElem = carbonVirtualHost.
                    getFirstChildWithName(new QName(QPID_VIRTUALHOST_COORDINATION_NODE));
            OMElement thriftServerPortStr = coordinationElem.getFirstChildWithName(
                    new QName(QPID_VIRTUALHOST_COORDINATION_THRIFT_SERVER_PORT_NODE));

            thriftServerPort = Integer.parseInt(thriftServerPortStr.getText()) + portOffset;
            if (thriftServerHost == null) {
                thriftServerPort = THRIFT_DEFAULT_SERVER_PORT;
            }
        } catch (FileNotFoundException e) {
            throw new ConfigurationException(getQpidHome() + ANDES_VIRTUALHOST_CONF_FILE + " not " +
                    "found", e);
        } catch (XMLStreamException e) {
            throw new ConfigurationException("Error while reading " + getQpidHome() + ANDES_VIRTUALHOST_CONF_FILE, e);
        }

        return thriftServerPort;
    }

    public boolean getIfSSLOnly() throws ConfigurationException {

        if (sslOnly != null) {
            return sslOnly;
        }

        try {
            File confFile = new File(getQpidHome() + ANDES_CONF_FILE);

            OMElement docRootNode = new StAXOMBuilder(new FileInputStream(confFile)).
                    getDocumentElement();
            OMElement connectorNode = docRootNode.getFirstChildWithName(
                    new QName(QPID_CONF_CONNECTOR_NODE));
            OMElement sslNode = connectorNode.getFirstChildWithName(
                    new QName(QPID_CONF_SSL_NODE));
            OMElement sslOnlyNode = sslNode.getFirstChildWithName(
                    new QName(QPID_CONF_SSL_ONLY_NODE));
            if (sslOnlyNode == null) {
                throw new ConfigurationException("Returned null for sslOnly");
            }
            sslOnly = Boolean.parseBoolean(sslOnlyNode.getText());

        } catch (FileNotFoundException e) {
            throw new ConfigurationException(getQpidHome() + ANDES_CONF_FILE + " not found", e);
        } catch (XMLStreamException e) {
            throw new ConfigurationException("Error while reading " + getQpidHome() + ANDES_CONF_FILE, e);
        }

        return sslOnly;
    }
}
