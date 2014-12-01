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

package org.wso2.carbon.andes.internal;

import com.hazelcast.core.HazelcastInstance;
import org.apache.axis2.clustering.ClusteringAgent;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.wso2.andes.configuration.AndesConfigurationManager;
import org.wso2.andes.configuration.enums.AndesConfiguration;
import org.wso2.andes.kernel.AndesContext;
import org.wso2.andes.kernel.AndesException;
import org.wso2.andes.server.BrokerOptions;
import org.wso2.andes.server.Main;
import org.wso2.andes.server.cluster.coordination.hazelcast.HazelcastAgent;
import org.wso2.andes.server.registry.ApplicationRegistry;
import org.wso2.andes.wso2.service.QpidNotificationService;
import org.wso2.carbon.andes.authentication.service.AuthenticationService;
import org.wso2.carbon.andes.service.CoordinatedActivityImpl;
import org.wso2.carbon.andes.service.QpidService;
import org.wso2.carbon.andes.service.QpidServiceImpl;
import org.wso2.carbon.andes.service.exception.ConfigurationException;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.base.api.ServerConfigurationService;
import org.wso2.carbon.core.clustering.api.CoordinatedActivity;
import org.wso2.carbon.event.core.EventBundleNotificationService;
import org.wso2.carbon.event.core.qpid.QpidServerDetails;
import org.wso2.carbon.utils.ConfigurationContextService;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Set;

/**
 * @scr.component name="org.wso2.carbon.andes.internal.QpidServiceComponent"
 * immediate="true"
 * @scr.reference name="org.wso2.carbon.andes.authentication.service.AuthenticationService"
 * interface="org.wso2.carbon.andes.authentication.service.AuthenticationService"
 * cardinality="1..1"
 * policy="dynamic"
 * bind="setAccessKey"
 * unbind="unsetAccessKey"
 * @scr.reference name="org.wso2.andes.wso2.service.QpidNotificationService"
 * interface="org.wso2.andes.wso2.service.QpidNotificationService"
 * cardinality="1..1"
 * policy="dynamic"
 * bind="setQpidNotificationService"
 * unbind="unsetQpidNotificationService"
 * @scr.reference name="server.configuration"
 * interface="org.wso2.carbon.base.api.ServerConfigurationService"
 * cardinality="1..1"
 * policy="dynamic"
 * bind="setServerConfiguration"
 * unbind="unsetServerConfiguration"
 * @scr.reference name="event.broker"
 * interface="org.wso2.carbon.event.core.EventBundleNotificationService"
 * cardinality="1..1"
 * policy="dynamic"
 * bind="setEventBundleNotificationService"
 * unbind="unsetEventBundleNotificationService"
 * @scr.reference name="hazelcast.instance.service"
 * interface="com.hazelcast.core.HazelcastInstance"
 * cardinality="0..1"
 * policy="dynamic"
 * bind="setHazelcastInstance"
 * unbind="unsetHazelcastInstance"
 * @scr.reference name="config.context.service"
 * interface="org.wso2.carbon.utils.ConfigurationContextService"
 * cardinality="1..1" policy="dynamic"
 * bind="setConfigurationContextService"
 * unbind="unsetConfigurationContextService"
 */
public class QpidServiceComponent {

    private static final Log log = LogFactory.getLog(QpidServiceComponent.class);

    private static final int CASSANDRA_THRIFT_PORT = 9160;
    private static final String CARBON_CONFIG_PORT_OFFSET = "Ports.Offset";
    private static final String CARBON_CONFIG_HOST_NAME = "HostName";
    private static final int CARBON_DEFAULT_PORT_OFFSET = 0;
    private ComponentContext componentContext;

    private ServiceRegistration qpidService = null;

    /**
     * This is used in the situations where the Hazelcast instance is not registered but the activate method of the
     * QpidServiceComponent is called when clustering is enabled.
     * This property is used to block the process of starting the broker until the hazelcast instance getting
     * registered.
     */
    private boolean brokerShouldBeStarted = false;

    /**
     * This flag true if HazelcastInstance has been registered.
     */
    private boolean registeredHazelcast = false;

    /**
     * This holds the configuration values
     */
    private QpidServiceImpl qpidServiceImpl;

    protected void activate(ComponentContext ctx) throws AndesException {
        this.componentContext = ctx;
        try {

            //Initialize AndesConfigurationManager
            AndesConfigurationManager.initialize(readPortOffset());

            //Load qpid specific configurations
            qpidServiceImpl
                    = new QpidServiceImpl(QpidServiceDataHolder.getInstance().getAccessKey());
            qpidServiceImpl.loadConfigurations();

            // set message store and andes context store related configurations
            AndesContext.getInstance().constructStoreConfiguration();
            if (!AndesContext.getInstance().isClusteringEnabled()) {
                // If clustering is disabled, broker starts without waiting for hazelcastInstance
                this.startAndesBroker();
            } else {
                if (registeredHazelcast) {
                    // When clustering is enabled, starts broker only if the hazelcastInstance has also been registered.
                    this.startAndesBroker();

                } else {
                    // If hazelcastInstance has not been registered yet, turn the brokerShouldBeStarted flag to true and
                    // wait for hazelcastInstance to be registered.
                    this.brokerShouldBeStarted = true;
                }
            }
        } catch (ConfigurationException e) {
            log.error("Invalid configuration found in a configuration file", e);
            throw new RuntimeException("Invalid configuration found in a configuration file", e);
        }

    }

    protected void deactivate(ComponentContext ctx) {
        // Unregister QpidService
        if (null != qpidService) {
            qpidService.unregister();
        }

        // Shutdown the Qpid broker
        ApplicationRegistry.remove();
    }

    protected void setAccessKey(AuthenticationService authenticationService) {
        QpidServiceDataHolder.getInstance().setAccessKey(authenticationService.getAccessKey());
    }

    protected void unsetAccessKey(AuthenticationService authenticationService) {
        QpidServiceDataHolder.getInstance().setAccessKey(null);
    }

    protected void setQpidNotificationService(QpidNotificationService qpidNotificationService) {
        // Qpid broker should not start until Qpid bundle is activated.
        // QpidNotificationService informs that the Qpid bundle has started.

    }

    protected void unsetQpidNotificationService(QpidNotificationService qpidNotificationService) {
    }

    protected void setServerConfiguration(ServerConfigurationService serverConfiguration) {
        QpidServiceDataHolder.getInstance().setCarbonConfiguration(serverConfiguration);
    }

    protected void unsetServerConfiguration(ServerConfigurationService serverConfiguration) {
        QpidServiceDataHolder.getInstance().setCarbonConfiguration(null);
    }

    protected void setEventBundleNotificationService(EventBundleNotificationService eventBundleNotificationService) {
        QpidServiceDataHolder.getInstance().registerEventBundleNotificationService(eventBundleNotificationService);
    }

    protected void unsetEventBundleNotificationService(EventBundleNotificationService eventBundleNotificationService) {
        // unsetting
    }

    /**
     * Access Hazelcast Instance, which is exposed as an OSGI service.
     *
     * @param hazelcastInstance hazelcastInstance found from the OSGI service
     */
    protected void setHazelcastInstance(HazelcastInstance hazelcastInstance) throws AndesException {
        HazelcastAgent.getInstance().init(hazelcastInstance);
        registeredHazelcast = true;

        if (brokerShouldBeStarted) {
            //Start the broker if the activate method of QpidServiceComponent is blocked until hazelcastInstance
            // getting registered
            try {
                this.startAndesBroker();
            } catch (ConfigurationException e) {
                log.error("Invalid configuration found in a configuration file", e);
                throw new RuntimeException("Invalid configuration found in a configuration file", e);
            }
        }
    }

    protected void unsetHazelcastInstance(HazelcastInstance hazelcastInstance) {
        // Do nothing
    }

    /**
     * Access ConfigurationContextService, which is exposed as an OSGI service, to read cluster configuration.
     *
     * @param configurationContextService ConfigurationContextService from the OSGI service
     */
    protected void setConfigurationContextService(ConfigurationContextService configurationContextService) {
        ClusteringAgent agent = configurationContextService.getServerConfigContext().getAxisConfiguration()
                .getClusteringAgent();
        AndesContext.getInstance().setClusteringAgent(agent);
        AndesContext.getInstance().setClusteringEnabled(agent != null);
    }

    protected void unsetConfigurationContextService(ConfigurationContextService configurationContextService) {
        // Do nothing
    }


    /**
     * Check if the broker is up and running
     *
     * @return true if the broker is running or false otherwise
     */
    private boolean isBrokerRunning() {
        boolean response = false;

        try {
            MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
            Set<ObjectName> set = mBeanServer.queryNames(
                    new ObjectName("org.wso2.andes:type=VirtualHost.VirtualHostManager,*"), null);

            if (set.size() > 0) { // Virtual hosts created, hence broker running.
                response = true;
            }
        } catch (MalformedObjectNameException e) {
            log.error("Error checking if broker is running.", e);
        }

        return response;
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


    /***
     * This applies the bindAddress from broker.xml instead of the hostname from carbon.xml within MB.
     * @return host name as derived from broker.xml
     */
    private String getCarbonHostName() throws AndesException {
        return AndesConfigurationManager.getInstance().readConfigurationValue(AndesConfiguration.TRANSPORTS_BIND_ADDRESS);

    }

    private void startAndesBroker() throws ConfigurationException, AndesException {
        brokerShouldBeStarted = false;

        // Start andes broker

        //register coordinatedActivityImpl to get coordinator changes notification.
        //When is node is appointed as the coordinator execute method of coordinatedActivityImpl will be called
        CoordinatedActivity coordinatedActivity = new CoordinatedActivityImpl();

        BundleContext bundleContext = componentContext.getBundleContext();
        bundleContext.registerService(CoordinatedActivity.class.getName(), coordinatedActivity, null);

        log.info("Activating Andes Message Broker Engine...");
        System.setProperty(BrokerOptions.ANDES_HOME, qpidServiceImpl.getQpidHome());
        String[] args = {"-p" + qpidServiceImpl.getAMQPPort(), "-s" + qpidServiceImpl.getAMQPSSLPort(),
                "-q" + qpidServiceImpl.getMqttPort()};

        //TODO: Change the functionality in andes main method to an API
        //Main.setStandaloneMode(false);
        Main.main(args);

        // Remove Qpid shutdown hook so that I have control over shutting the broker down
        Runtime.getRuntime().removeShutdownHook(ApplicationRegistry.getShutdownHook());

        // Wait until the broker has started
        while (!isBrokerRunning()) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignore) {
                //ignore
            }
        }

        //check whether the tcp port has started. some times the server started thread may return
        //before Qpid server actually bind to the tcp port. in that case there are some connection
        //time out issues.
        boolean isServerStarted = false;
        int port;
        if (qpidServiceImpl.getIfSSLOnly()) {
            port = qpidServiceImpl.getAMQPSSLPort();
        } else {
            port = qpidServiceImpl.getAMQPPort();
        }
        while (!isServerStarted) {
            Socket socket = null;
            try {
                log.info("Carbon Host Name : " + getCarbonHostName());
                InetAddress address = InetAddress.getByName(getCarbonHostName());
                socket = new Socket(address, port);
                log.info("Host : " + address.getHostAddress() + " port : " + port);
                isServerStarted = socket.isConnected();
                if (isServerStarted) {
                    log.info("WSO2 Message Broker is Started. Successfully connected to the server on port " +
                            port);
                }
            } catch (IOException e) {
                log.error("Wait until Qpid server starts on port " + port, e);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ignore) {
                    // Ignore
                }
            } finally {
                try {
                    if ((socket != null) && (socket.isConnected())) {
                        socket.close();
                    }
                } catch (IOException e) {
                    log.error("Can not close the socket which is used to check the server " +
                            "status ", e);
                }
            }
        }

        // Publish Qpid properties
        qpidService = this.componentContext.getBundleContext().registerService(
                QpidService.class.getName(), qpidServiceImpl, null);
        Integer brokerPort;
        if (qpidServiceImpl.getIfSSLOnly()) {
            brokerPort = qpidServiceImpl.getAMQPSSLPort();
        } else {
            brokerPort = qpidServiceImpl.getAMQPPort();
        }
        QpidServerDetails qpidServerDetails =
                new QpidServerDetails(qpidServiceImpl.getAccessKey(),
                        qpidServiceImpl.getClientID(),
                        qpidServiceImpl.getVirtualHostName(),
                        qpidServiceImpl.getHostname(),
                        brokerPort.toString(), qpidServiceImpl.getIfSSLOnly());
        QpidServiceDataHolder.getInstance().getEventBundleNotificationService().notifyStart(qpidServerDetails);

    }
}
