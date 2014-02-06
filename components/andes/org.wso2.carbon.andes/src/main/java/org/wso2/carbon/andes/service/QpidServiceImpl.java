/*
 *  Copyright (c) 2008, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.wso2.carbon.andes.service;

import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.andes.commons.QueueDetails;
import org.wso2.carbon.andes.commons.SubscriptionDetails;
import org.wso2.carbon.andes.commons.registry.RegistryClient;
import org.wso2.carbon.andes.commons.registry.RegistryClientException;
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

public class QpidServiceImpl implements QpidService {

    private static final Log log = LogFactory.getLog(QpidServiceImpl.class);

    private static String CARBON_CLIENT_ID = "carbon";
    private static String CARBON_VIRTUAL_HOST_NAME = "carbon";
    private static String CARBON_DEFAULT_HOSTNAME = "localhost";
    private static String CARBON_DEFAULT_PORT = "5672";
    private static String CARBON_DEFAULT_SSL_PORT = "8672";
    private static int CARBON_DEFAULT_PORT_OFFSET = 0;

    private static final String QPID_CONF_DIR = "/repository/conf/advanced/";
    private static final String QPID_CONF_FILE = "qpid-config.xml";
    private static final String QPID_VIRTUALHOST_CONF_FILE = "qpid-virtualhosts.xml";
    private static final String QPID_CONF_CONNECTOR_NODE = "connector";
    private static final String QPID_CONF_PORT_NODE = "port";
    private static final String QPID_CONF_SSL_PORT_NODE = "sslport";
    private static final String QPID_CONF_CLUSTER_NODE="clustering";
    private static final String QPID_CONF_CLUSTER_ENABLE_NODE="enabled";
    private static final String QPID_CONF_CLUSTER_COORDINATION_NODE="coordination";
    private static final String QPID_CONF_CLUSTER_ZK_CONNECTION_NODE="ZooKeeperConnection";
    private static final String QPID_CONF_EXTERNAL_CASSANDRA_SERVER="externalCassandraServerRequired";
    private static final String QPID_CONF_EXTERNAL_ZOOKEEPER_SERVER="externalZookeeperServerRequired";

    private static String CARBON_CONFIG_QPID_PORT_NODE = "Ports.EmbeddedQpid.BrokerPort";
    private static String CARBON_CONFIG_QPID_SSL_PORT_NODE = "Ports.EmbeddedQpid.BrokerSSLPort";
    private static String CARBON_CONFIG_PORT_OFFSET_NODE = "Ports.Offset";

    private static final String QPID_VIRTUALHOST_NODE = "virtualhost";
    private static final String QPID_VIRTUALHOST_CARBON_NODE = "carbon";
    private static final String QPID_VIRTUALHOST_STORE_NODE = "store";
    private static final String QPID_VIRTUALHOST_STORE_CONNECTION_STRING_NODE = "connectionString";


    private static final String DOMAIN_NAME_SEPARATOR = "@";
    private static final String DOMAIN_NAME_SEPARATOR_INTERNAL = "!";

    private String accessKey = "";
    private String hostname = "";
    private String port = "";
    private String sslPort = "";
    private int portOffset = 0;
    private String cassandraConnection;
    private String zkConnection;

    private Boolean clsuterEnabled;
    private Boolean externalCassandraRequired;
    private Boolean externalZookeeperRequired;

    public QpidServiceImpl(String accessKey) {
        this.accessKey = accessKey;

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
        return new StringBuffer()
                .append("amqp://").append(username).append(":").append(accessKey)
                .append("@").append(CARBON_CLIENT_ID)
                .append("/").append(CARBON_VIRTUAL_HOST_NAME)
                .append("?brokerlist='vm://:1'").toString();
    }

    public String getTCPConnectionURL(String username, String password) {
        // amqp://{username}:{password}@carbon/carbon?brokerlist='tcp://{hostname}:{port}'
        return new StringBuffer()
                .append("amqp://").append(username).append(":").append(password)
                .append("@").append(CARBON_CLIENT_ID)
                .append("/").append(CARBON_VIRTUAL_HOST_NAME)
                .append("?brokerlist='tcp://").append(hostname).append(":").append(port).append("'")
                .toString();
    }

    public String getTCPConnectionURL(String username, String password, String clientID) {
        // amqp://{username}:{password}@{cliendID}/carbon?brokerlist='tcp://{hostname}:{port}'
        return new StringBuffer()
                .append("amqp://").append(username).append(":").append(password)
                .append("@").append(clientID)
                .append("/").append(CARBON_VIRTUAL_HOST_NAME)
                .append("?brokerlist='tcp://").append(hostname).append(":").append(port).append("'")
                .toString();
    }

    public String getInternalTCPConnectionURL(String username, String password) {
        username = getInternalTenantUsername(username);

        // amqp://{username}:{password}@carbon/carbon?brokerlist='tcp://{hostname}:{port}'
        return new StringBuffer()
                .append("amqp://").append(username).append(":").append(password)
                .append("@").append(CARBON_CLIENT_ID)
                .append("/").append(CARBON_VIRTUAL_HOST_NAME)
                .append("?brokerlist='tcp://").append(hostname).append(":").append(port).append("'")
                .toString();
    }

    public String getInternalTCPConnectionURL(String username, String password, String clientID) {
        username = getInternalTenantUsername(username);
        
        // amqp://{username}:{password}@{cliendID}/carbon?brokerlist='tcp://{hostname}:{port}'
        return new StringBuffer()
                .append("amqp://").append(username).append(":").append(password)
                .append("@").append(clientID)
                .append("/").append(CARBON_VIRTUAL_HOST_NAME)
                .append("?brokerlist='tcp://").append(hostname).append(":").append(port).append("'")
                .toString();
    }

    public String getQpidHome() {
        return System.getProperty(ServerConstants.CARBON_HOME) + QPID_CONF_DIR;
    }

    public QueueDetails[] getQueues(boolean isDurable) {
        QueueDetails[] queueDetails = null;

        try {
            queueDetails = RegistryClient.getQueues();
        } catch (RegistryClientException e) {
            log.warn("Erro while retrieving queue details : " + e.getMessage());
        }

        return queueDetails;
    }

    public SubscriptionDetails[] getSubscriptions(String topic, boolean isDurable) {
        SubscriptionDetails[] subsDetails = null;

        try {
            subsDetails = RegistryClient.getSubscriptions(topic);
        } catch (RegistryClientException e) {
            log.warn("Erro while retrieving subscription details : " + e.getMessage());
        }

        return subsDetails;
    }

    public String getSSLPort(){
        return sslPort;
    }

    public boolean isClusterEnabled() {

        if(clsuterEnabled==null) {
            clsuterEnabled = readClusterEnabledDisabledStatusFromQpidConfig();
            return clsuterEnabled;
        }

        return clsuterEnabled;
    }

    @Override
    public boolean isExternalCassandraServerRequired() {
        if(externalCassandraRequired == null){
            externalCassandraRequired = readCassandraServerRequirementStatusFromQpidConfig();
        }
        return externalCassandraRequired;
    }

    @Override
    public boolean isExternalZookeeperServerRequired() {
        if(externalZookeeperRequired == null) {
            externalZookeeperRequired = readExternalZookeeperServerRequiredStatusFromQpidConfig();
        }
        return externalZookeeperRequired;
    }

    private int readPortOffset() {
        ServerConfigurationService carbonConfig = QpidServiceDataHolder.getInstance().getCarbonConfiguration();
        String portOffset = carbonConfig.getFirstProperty(CARBON_CONFIG_PORT_OFFSET_NODE);

        try {
            return ((portOffset != null) ? Integer.parseInt(portOffset.trim()) : CARBON_DEFAULT_PORT_OFFSET);
        } catch (NumberFormatException e) {
            return CARBON_DEFAULT_PORT_OFFSET;
        }
    }

    private String readPortFromConfig() {
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

        // Offset
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
     * Read port from qpid-config.xml
     *
     * @return
     */
    private String readPortFromQpidConfig() {
        String port = "";

        try {
            File confFile = new File(getQpidHome() + QPID_CONF_FILE);

            OMElement docRootNode = new StAXOMBuilder(new FileInputStream(confFile)).
                    getDocumentElement();
            OMElement connectorNode = docRootNode.getFirstChildWithName(
                    new QName(QPID_CONF_CONNECTOR_NODE));
            OMElement portNode = connectorNode.getFirstChildWithName(
                    new QName(QPID_CONF_PORT_NODE));

            port = portNode.getText();
        } catch (FileNotFoundException e) {
            log.error(getQpidHome() + QPID_CONF_FILE + " not found");
        } catch (XMLStreamException e) {
            log.error("Error while reading " + getQpidHome() +
                      QPID_CONF_FILE + " : " + e.getMessage());
        } catch (NullPointerException e) {
            log.error("Invalid configuration : " + getQpidHome() + QPID_CONF_FILE);
        }

        return ((port != null) ? port.trim() : "");
    }

    /**
     * Read port from qpid-config.xml
     *
     * @return
     */
    private boolean readExternalZookeeperServerRequiredStatusFromQpidConfig() {
        String required = "";

        try {
            File confFile = new File(getQpidHome() + QPID_CONF_FILE);

            OMElement docRootNode = new StAXOMBuilder(new FileInputStream(confFile)).
                    getDocumentElement();
            OMElement clusteringNode = docRootNode.getFirstChildWithName(
                    new QName(QPID_CONF_CLUSTER_NODE));
            OMElement statusNode = clusteringNode.getFirstChildWithName(
                    new QName(QPID_CONF_EXTERNAL_ZOOKEEPER_SERVER));
            if(statusNode == null) {
                return false;
            }
            required = statusNode.getText();
        } catch (FileNotFoundException e) {
            log.error(getQpidHome() + QPID_CONF_FILE + " not found");
        } catch (XMLStreamException e) {
            log.error("Error while reading " + getQpidHome() +
                      QPID_CONF_FILE + " : " + e.getMessage());
        } catch (NullPointerException e) {
            log.error("Invalid configuration : " + getQpidHome() + QPID_CONF_FILE);
        }

        if("true".equals(required)) {
            return true;
        }

        return false;
    }

    private boolean readCassandraServerRequirementStatusFromQpidConfig() {
        String required = "";

        try {
            File confFile = new File(getQpidHome() + QPID_CONF_FILE);

            OMElement docRootNode = new StAXOMBuilder(new FileInputStream(confFile)).
                    getDocumentElement();
            OMElement clusteringNode = docRootNode.getFirstChildWithName(
                    new QName(QPID_CONF_CLUSTER_NODE));
            OMElement statusNode = clusteringNode.getFirstChildWithName(
                    new QName(QPID_CONF_EXTERNAL_CASSANDRA_SERVER));
            if(statusNode == null) {
                return false;
            }
            required = statusNode.getText();
        } catch (FileNotFoundException e) {
            log.error(getQpidHome() + QPID_CONF_FILE + " not found");
        } catch (XMLStreamException e) {
            log.error("Error while reading " + getQpidHome() +
                      QPID_CONF_FILE + " : " + e.getMessage());
        } catch (NullPointerException e) {
            log.error("Invalid configuration : " + getQpidHome() + QPID_CONF_FILE);
        }

        if("true".equals(required)) {
            return true;
        }

        return false;
    }



    private boolean readClusterEnabledDisabledStatusFromQpidConfig() {
        String enabled = "";

        try {
            File confFile = new File(getQpidHome() + QPID_CONF_FILE);

            OMElement docRootNode = new StAXOMBuilder(new FileInputStream(confFile)).
                    getDocumentElement();
            OMElement clusteringNode = docRootNode.getFirstChildWithName(
                    new QName(QPID_CONF_CLUSTER_NODE));
            OMElement enabledNode = clusteringNode.getFirstChildWithName(
                    new QName(QPID_CONF_CLUSTER_ENABLE_NODE));

            if(enabledNode == null) {
                return false;
            }
            enabled = enabledNode.getText();
        } catch (FileNotFoundException e) {
            log.error(getQpidHome() + QPID_CONF_FILE + " not found");
        } catch (XMLStreamException e) {
            log.error("Error while reading " + getQpidHome() +
                      QPID_CONF_FILE + " : " + e.getMessage());
        } catch (NullPointerException e) {
            log.error("Invalid configuration : " + getQpidHome() + QPID_CONF_FILE);
        }

        if("true".equals(enabled)) {
            return true;
        }

        return false;
    }

    private String readSSLPortFromConfig() {
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
        * Read port from qpid-config.xml
        *
        * @return
        */
    private String readSSLPortFromQpidConfig() {
        String port = "";

        try {
            File confFile = new File(getQpidHome() + QPID_CONF_FILE);

            OMElement docRootNode = new StAXOMBuilder(new FileInputStream(confFile)).
                    getDocumentElement();
            OMElement connectorNode = docRootNode.getFirstChildWithName(
                    new QName(QPID_CONF_CONNECTOR_NODE));
            OMElement portNode = connectorNode.getFirstChildWithName(
                    new QName(QPID_CONF_SSL_PORT_NODE));

            port = portNode.getText();
        } catch (FileNotFoundException e) {
            log.error(getQpidHome() + QPID_CONF_FILE + " not found");
        } catch (XMLStreamException e) {
            log.error("Error while reading " + getQpidHome() +
                      QPID_CONF_FILE + " : " + e.getMessage());
        } catch (NullPointerException e) {
            log.error("Invalid configuration : " + getQpidHome() + QPID_CONF_FILE);
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
    public String getCassandraConnectionString() {

        if(cassandraConnection != null) {
            return cassandraConnection.trim();
        }
         try {
            File confFile = new File(getQpidHome() + QPID_VIRTUALHOST_CONF_FILE);

            OMElement docRootNode = new StAXOMBuilder(new FileInputStream(confFile)).
                    getDocumentElement();
            OMElement virtualHostNode = docRootNode.getFirstChildWithName(
                    new QName(QPID_VIRTUALHOST_NODE));
            OMElement carbonVirtualHost = virtualHostNode.getFirstChildWithName(
                    new QName(QPID_VIRTUALHOST_CARBON_NODE));
            OMElement storeElem  = carbonVirtualHost.
                    getFirstChildWithName(new QName(QPID_VIRTUALHOST_STORE_NODE));
            OMElement connectionStr = storeElem.getFirstChildWithName(
                    new QName(QPID_VIRTUALHOST_STORE_CONNECTION_STRING_NODE));

            cassandraConnection = connectionStr.getText();
        } catch (FileNotFoundException e) {
            log.error(getQpidHome() + QPID_CONF_FILE + " not found");
        } catch (XMLStreamException e) {
            log.error("Error while reading " + getQpidHome() +
                      QPID_CONF_FILE + " : " + e.getMessage());
        } catch (NullPointerException e) {
            log.error("Invalid configuration : " + getQpidHome() + QPID_CONF_FILE);
        }

        return ((cassandraConnection != null) ? cassandraConnection.trim() : "");

    }

    @Override
    public String getZookeeperConnectionString() {

        if(zkConnection != null) {
            return zkConnection.trim();
        }

        try {
            File confFile = new File(getQpidHome() + QPID_CONF_FILE);

            OMElement docRootNode = new StAXOMBuilder(new FileInputStream(confFile)).
                    getDocumentElement();
            OMElement clusteringNode = docRootNode.getFirstChildWithName(
                    new QName(QPID_CONF_CLUSTER_NODE));
            OMElement coordinationNode = clusteringNode.getFirstChildWithName(
                    new QName(QPID_CONF_CLUSTER_COORDINATION_NODE));
            OMElement zkConnectionNode = coordinationNode.getFirstChildWithName(
                    new QName(QPID_CONF_CLUSTER_ZK_CONNECTION_NODE));

            zkConnection = zkConnectionNode.getText();
        } catch (FileNotFoundException e) {
            log.error(getQpidHome() + QPID_CONF_FILE + " not found");
        } catch (XMLStreamException e) {
            log.error("Error while reading " + getQpidHome() +
                    QPID_CONF_FILE + " : " + e.getMessage());
        } catch (NullPointerException e) {
            log.error("Invalid configuration : " + getQpidHome() + QPID_CONF_FILE);
        }

        return (zkConnection != null) ? zkConnection.trim() : "";
    }
}
