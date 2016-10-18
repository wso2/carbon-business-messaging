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
import org.wso2.andes.kernel.AndesKernelBoot;
import org.wso2.andes.server.BrokerOptions;
import org.wso2.andes.server.Main;
import org.wso2.andes.server.cluster.coordination.hazelcast.HazelcastAgent;
import org.wso2.andes.server.registry.ApplicationRegistry;
import org.wso2.andes.wso2.service.QpidNotificationService;
import org.wso2.carbon.andes.authentication.service.AuthenticationService;
import org.wso2.carbon.andes.listeners.BrokerLifecycleListener;
import org.wso2.carbon.andes.listeners.MessageBrokerTenantManagementListener;
import org.wso2.carbon.andes.service.QpidService;
import org.wso2.carbon.andes.service.QpidServiceImpl;
import org.wso2.carbon.andes.service.exception.ConfigurationException;
import org.wso2.carbon.andes.utils.MessageBrokerDBUtil;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.base.api.ServerConfigurationService;
import org.wso2.carbon.andes.event.core.EventBundleNotificationService;
import org.wso2.carbon.andes.event.core.qpid.QpidServerDetails;
import org.wso2.carbon.server.admin.common.IServerAdmin;
import org.wso2.carbon.stratos.common.listeners.TenantMgtListener;
import org.wso2.carbon.utils.ConfigurationContextService;
import org.wso2.carbon.utils.WaitBeforeShutdownObserver;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Set;
import java.util.Stack;

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
 * interface="org.wso2.carbon.andes.event.core.EventBundleNotificationService"
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
 * @scr.reference name="org.wso2.carbon.server.admin.common.IServerAdmin"
 * interface="org.wso2.carbon.server.admin.common.IServerAdmin"
 * cardinality="1..1" policy="dynamic"
 * bind="setIServerAdmin"
 * unbind="unsetIServerAdmin"
 */
public class QpidServiceComponent {

    private static final Log log = LogFactory.getLog(QpidServiceComponent.class);

    private static final String CARBON_CONFIG_PORT_OFFSET = "Ports.Offset";
    private static final int CARBON_DEFAULT_PORT_OFFSET = 0;
    protected static final String MODE_STANDALONE = "standalone";
    protected static final String MODE_DEFAULT = "default";
    private static BundleContext bundleContext;
    private static Stack<ServiceRegistration> registrations = new Stack<ServiceRegistration>();

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

    protected void activate(ComponentContext context) throws AndesException {
        try {

            //Initialize AndesConfigurationManager
            AndesConfigurationManager.initialize(readPortOffset());

            //Load qpid specific configurations
            qpidServiceImpl
                    = new QpidServiceImpl(QpidServiceDataHolder.getInstance().getAccessKey());
            qpidServiceImpl.loadConfigurations();

            // Register tenant management listener for Message Broker
            bundleContext = context.getBundleContext();
            MessageBrokerTenantManagementListener tenantManagementListener = new
                    MessageBrokerTenantManagementListener();
            registrations.push(bundleContext.registerService(TenantMgtListener.class.getName(), tenantManagementListener, null));

            // set message store and andes context store related configurations
            AndesContext.getInstance().constructStoreConfiguration();

            // Read deployment mode
            String mode = AndesConfigurationManager.readValue(AndesConfiguration.DEPLOYMENT_MODE);

            // Start broker in standalone mode
            if (mode.equalsIgnoreCase(MODE_STANDALONE)) {
                // set clustering enabled to false because even though clustering enabled in axis2.xml, we are not
                // going to consider it in standalone mode
                AndesContext.getInstance().setClusteringEnabled(false);
                this.startAndesBroker();
            } else if (mode.equalsIgnoreCase(MODE_DEFAULT)) {
                // Start broker in HA mode
                if (!AndesContext.getInstance().isClusteringEnabled()) {
                    // If clustering is disabled, broker starts without waiting for hazelcastInstance
                    this.startAndesBroker();
                } else {
                    // Start broker in distributed mode
                    if (registeredHazelcast) {
                        // When clustering is enabled, starts broker only if the hazelcastInstance has also been
                        // registered.
                        this.startAndesBroker();
                    } else {
                        // If hazelcastInstance has not been registered yet, turn the brokerShouldBeStarted flag to
                        // true and wait for hazelcastInstance to be registered.
                        this.brokerShouldBeStarted = true;
                    }
                }
            } else {
                throw new ConfigurationException("Invalid value " + mode + " for deployment/mode in broker.xml");
            }

            registrations.push(bundleContext.registerService(
                    WaitBeforeShutdownObserver.class.getName(), new WaitBeforeShutdownObserver() {
                boolean status = false;

                public void startingShutdown() {
                    try {
                        //executing pre-shutdown work for registered listeners before shutting down the andes server
                        for(BrokerLifecycleListener listener: QpidServiceDataHolder.getInstance()
                                .getBrokerLifecycleListeners()){
                            listener.onShuttingdown();
                        }
                        AndesKernelBoot.shutDownAndesKernel();
                        //executing post-shutdown work for registered listeners after shutting down the andes server
                        for(BrokerLifecycleListener listener: QpidServiceDataHolder.getInstance()
                                        .getBrokerLifecycleListeners()){
                            listener.onShutdown();
                        }
                    } catch (AndesException e) {
                        log.error("Error while shutting down Andes kernel. ", e);
                    } finally {
                        status = true;
                    }
                }

                public boolean isTaskComplete() {
                    return status;
                }
            }, null));

        } catch (ConfigurationException e) {
            log.error("Invalid configuration found in a configuration file", e);
            this.shutdown();
        }

    }

    protected void deactivate(ComponentContext ctx) {
        // By this time, through the AndesServerShutDownListener, All other services/ workers including this service,
        // have been closed.
        // Unregister services
        while (!registrations.empty()) {
            registrations.pop().unregister();
        }
        bundleContext = null;
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
                this.shutdown();
            }
        }
    }

    protected void unsetHazelcastInstance(HazelcastInstance hazelcastInstance) {
        //stop thrift server before hazelcast shutting down
        AndesKernelBoot.stopThriftServer();
    }

    /**
     * Access ConfigurationContextService, which is exposed as an OSGI service, to read cluster configuration.
     *
     * @param configurationContextService ConfigurationContextService from the OSGI service
     */
    protected void setConfigurationContextService(ConfigurationContextService configurationContextService) {
        ClusteringAgent agent = configurationContextService.getServerConfigContext().getAxisConfiguration()
                .getClusteringAgent();
        AndesContext.getInstance().setClusteringEnabled(agent != null);
    }

    protected void unsetConfigurationContextService(ConfigurationContextService configurationContextService) {
        // Do nothing
    }

    /**
     * Access IServerAdmin, which is exposed as an OSGi service, to call the graceful shutdown method in the carbon
     * kernel.
     */
    protected void setIServerAdmin(IServerAdmin iServerAdmin) {
        QpidServiceDataHolder.getInstance().setService(iServerAdmin);
    }

    /**
     * Unset IServerAdmin OSGi service
     */
    protected void unsetIServerAdmin(IServerAdmin iServerAdmin) {
        QpidServiceDataHolder.getInstance().setService(null);
    }

    /**
     * Shutdown from the carbon kernel level.
     */
    private void shutdown() throws AndesException {
        //Calling carbon kernel shutdown method, inside the ServerAdmin component
        try {
            QpidServiceDataHolder.getInstance().getService().shutdownGracefully();
        } catch (Exception e) {
            log.error("Error occurred while shutting down", e);
            throw new AndesException("Error occurred while shutting down", e);
        }
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
    private String getTransportBindAddress() {
        return AndesConfigurationManager.readValue(AndesConfiguration.TRANSPORTS_BIND_ADDRESS);

    }

    /***
     * This applies the AMQPbindAddress from broker.xml instead of the hostname from carbon.xml within MB.
     * @return host name as derived from broker.xml
     */
    private String getAMQPTransportBindAddress() {
        return AndesConfigurationManager.readValue(AndesConfiguration.TRANSPORTS_AMQP_BIND_ADDRESS);

    }

    /**
     * Start Andes Broker and related components with given configurations.
     *
     * @throws ConfigurationException
     * @throws AndesException
     */
    private void startAndesBroker() throws ConfigurationException, AndesException {
        brokerShouldBeStarted = false;

        String dSetupValue = System.getProperty("setup");
        if (dSetupValue != null) {
            // Source MB rdbms database if data source configurations and supported sql exist
            MessageBrokerDBUtil messageBrokerDBUtil = new MessageBrokerDBUtil();
            messageBrokerDBUtil.initialize();
        }
        // Start andes broker

        log.info("Activating Andes Message Broker Engine...");
        System.setProperty(BrokerOptions.ANDES_HOME, qpidServiceImpl.getQpidHome());
        String[] args = {"-p" + qpidServiceImpl.getAMQPPort(), "-s" + qpidServiceImpl.getAMQPSSLPort()};

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

        // TODO: Have to re-structure how andes broker getting started.
        // there should be a separate andes-core component to initialize Andes Broker. Within
        // that component both Qpid and MQTT components should initialized.

        // Start AMQP server with given configurations
        startAMQPServer();

        // Message broker is started with both AMQP and MQTT
        log.info("WSO2 Message Broker is started.");

        // Publish Qpid properties
        registrations.push(bundleContext.registerService(
                QpidService.class.getName(), qpidServiceImpl, null));
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

    /**
     * check whether the tcp port has started. some times the server started thread may return
     * before Qpid server actually bind to the tcp port. in that case there are some connection
     * time out issues.
     *
     * @throws ConfigurationException
     */
    private void startAMQPServer() throws ConfigurationException {

        boolean isServerStarted = false;
        int port;
        if (qpidServiceImpl.getIfSSLOnly()) {
            port = qpidServiceImpl.getAMQPSSLPort();
        } else {
            port = qpidServiceImpl.getAMQPPort();
        }

        if (AndesConfigurationManager.<Boolean>readValue(AndesConfiguration.TRANSPORTS_AMQP_ENABLED)) {
            while (!isServerStarted) {
                Socket socket = null;
                try {
                    InetAddress address = InetAddress.getByName(getAMQPTransportBindAddress());
                    socket = new Socket(address, port);
                    log.info("AMQP Host Address : " + address.getHostAddress() + " Port : " + port);
                    isServerStarted = socket.isConnected();
                    if (isServerStarted) {
                        log.info("Successfully connected to AMQP server "
                                 + "on port " + port);
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
                        log.error("Can not close the socket which is used to check the server "
                                  + "status ", e);
                    }
                }
            }
        } else {
            log.warn("AMQP Transport is disabled as per configuration.");
        }
    }


}
