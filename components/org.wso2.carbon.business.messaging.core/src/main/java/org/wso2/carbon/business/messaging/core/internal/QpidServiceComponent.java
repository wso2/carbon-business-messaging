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
package org.wso2.carbon.business.messaging.core.internal;

import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Set;
import java.util.Stack;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.naming.InitialContext;
import javax.sql.DataSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.jndi.JNDIContextManager;
import org.wso2.andes.configuration.AndesConfigurationManager;
import org.wso2.andes.configuration.enums.AndesConfiguration;
import org.wso2.andes.kernel.AndesContext;
import org.wso2.andes.kernel.AndesException;
import org.wso2.andes.server.BrokerOptions;
import org.wso2.andes.server.ClusterResourceHolder;
import org.wso2.andes.server.Main;
import org.wso2.andes.server.registry.ApplicationRegistry;
import org.wso2.andes.wso2.service.QpidNotificationService;
import org.wso2.carbon.business.messaging.core.service.QpidService;
import org.wso2.carbon.business.messaging.core.service.QpidServiceImpl;
import org.wso2.carbon.business.messaging.core.service.exception.ConfigurationException;
import org.wso2.carbon.business.messaging.core.utils.MessageBrokerDBUtil;
import org.wso2.carbon.datasource.core.api.DataSourceService;
import org.wso2.carbon.datasource.core.exception.DataSourceException;

@Component(
        name = "org.wso2.carbon.andes.internal.QpidServiceComponent",
        immediate = true
)
public class QpidServiceComponent {

    private static final Log log = LogFactory.getLog(QpidServiceComponent.class);

    private static final String CARBON_CONFIG_PORT_OFFSET = "Ports.Offset";
    private static final int CARBON_DEFAULT_PORT_OFFSET = 0;
    protected static final String MODE_STANDALONE = "standalone";
    protected static final String MODE_DEFAULT = "default";
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

    @Activate
    protected void activate(BundleContext context) throws AndesException {
        try {
            //Initialize AndesConfigurationManager
            AndesConfigurationManager.initialize(readPortOffset());

            //Load qpid specific configurations
            qpidServiceImpl
                    = new QpidServiceImpl(QpidServiceDataHolder.getInstance().getAccessKey());
            qpidServiceImpl.loadConfigurations();

            // Register tenant management listener for Message Broker
//            MessageBrokerTenantManagementListener tenantManagementListener = new
//                    MessageBrokerTenantManagementListener();
            //TODO tenancy ???
//            registrations.push(bundleContext.registerService(TenantMgtListener.class.getName(), tenantManagementListener, null));

            // set message store and andes context store related configurations
            AndesContext.getInstance().constructStoreConfiguration();

            // Read deployment mode
            String mode = AndesConfigurationManager.readValue(AndesConfiguration.DEPLOYMENT_MODE);

            // Start broker in standalone mode
            if (mode.equalsIgnoreCase(MODE_STANDALONE)) {
                // set clustering enabled to false because even though clustering enabled in axis2.xml, we are not
                // going to consider it in standalone mode
                AndesContext.getInstance().setClusteringEnabled(false);
                this.startAndesBroker(context);
            } else if (mode.equalsIgnoreCase(MODE_DEFAULT)) {
                // Start broker in HA mode
                if (!AndesContext.getInstance().isClusteringEnabled()) {
                    // If clustering is disabled, broker starts without waiting for hazelcastInstance
                    this.startAndesBroker(context);
                } else {
                    // Start broker in distributed mode
                    if (registeredHazelcast) {
                        // When clustering is enabled, starts broker only if the hazelcastInstance has also been
                        // registered.
                        this.startAndesBroker(context);
                    } else {
                        // If hazelcastInstance has not been registered yet, turn the brokerShouldBeStarted flag to
                        // true and wait for hazelcastInstance to be registered.
                        this.brokerShouldBeStarted = true;
                    }
                }
            } else {
                throw new ConfigurationException("Invalid value " + mode + " for deployment/mode in broker.xml");
            }

//            MBShutdownHandler mbShutdownHandler = new MBShutdownHandler();
//            registrations.push(bundleContext.registerService(
//                    ServerShutdownHandler.class.getName(), mbShutdownHandler, null));
//            registrations.push(bundleContext.registerService(
//                    ServerRestartHandler.class.getName(), mbShutdownHandler, null));

        } catch (ConfigurationException e) {
            log.error("Invalid configuration found in a configuration file", e);
//            this.shutdown();
        }

    }

    @Reference(
            name = "org.wso2.carbon.datasource.DataSourceService",
            service = DataSourceService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unregisterDataSourceService"
    )
    private void registerDataService(DataSourceService dataSourceService) {
        try {
            DataSource dataSource = (HikariDataSource) dataSourceService.getDataSource("WSO2_MB_STORE_DB");
            ClusterResourceHolder.getInstance().setDatasource(dataSource);
        } catch (DataSourceException e) {
            e.printStackTrace();
        }
    }

    private void unregisterDataSourceService(DataSourceService dataSourceService) {
        QpidServiceDataHolder.getInstance().setDataSourceService(null);

    }

    @Reference(
            name = "org.wso2.carbon.datasource.jndi",
            service = JNDIContextManager.class,
            cardinality = ReferenceCardinality.AT_LEAST_ONE,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "onJNDIUnregister"
    )
    protected void onJNDIReady(JNDIContextManager jndiContextManager) {
//        this.jndiContextManager = jndiContextManager;

    }


    protected void onJNDIUnregister(JNDIContextManager jndiContextManager) {
//        this.jndiContextManager = null;
    }

    @Deactivate
    protected void deactivate(BundleContext ctx) {
        // By this time, through the AndesServerShutDownListener, All other services/ workers including this service,
        // have been closed.
        // Unregister services
        while (!registrations.empty()) {
            registrations.pop().unregister();
        }
    }

    //
//    protected void setAccessKey(AuthenticationService authenticationService) {
//        QpidServiceDataHolder.getInstance().setAccessKey(authenticationService.getAccessKey());
//    }
//
//    protected void unsetAccessKey(AuthenticationService authenticationService) {
//        QpidServiceDataHolder.getInstance().setAccessKey(null);
//    }
//
//    @Reference(
//            name = "org.wso2.andes.wso2.service",
//            service = QpidNotificationService.class,
//            cardinality = ReferenceCardinality.AT_LEAST_ONE,
//            policy = ReferencePolicy.DYNAMIC,
//            unbind = "unsetQpidNotificationService"
//    )
    protected void setQpidNotificationService(QpidNotificationService qpidNotificationService) {
        // Qpid broker should not start until Qpid bundle is activated.
        // QpidNotificationService informs that the Qpid bundle has started.

    }

    protected void unsetQpidNotificationService(QpidNotificationService qpidNotificationService) {
    }

//    protected void setServerConfiguration(ServerConfigurationService serverConfiguration) {
//        QpidServiceDataHolder.getInstance().setCarbonConfiguration(serverConfiguration);
//    }
//
//    protected void unsetServerConfiguration(ServerConfigurationService serverConfiguration) {
//        QpidServiceDataHolder.getInstance().setCarbonConfiguration(null);
//    }

//    protected void setEventBundleNotificationService(EventBundleNotificationService eventBundleNotificationService) {
//        QpidServiceDataHolder.getInstance().registerEventBundleNotificationService(eventBundleNotificationService);
//    }
//
//    protected void unsetEventBundleNotificationService(EventBundleNotificationService eventBundleNotificationService) {
//        // unsetting
//    }


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
        String portOffset = System.getProperty("portOffset",
                "0"); //todo carbonConfig.getFirstProperty(CARBON_CONFIG_PORT_OFFSET)
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
     * This applies the MQTTbindAddress from broker.xml instead of the hostname from carbon.xml within MB.
     * @return host name as derived from broker.xml
     */
    private String getMQTTTransportBindAddress() {
        return AndesConfigurationManager.readValue(AndesConfiguration.TRANSPORTS_MQTT_BIND_ADDRESS);

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
     * @param context
     * @throws ConfigurationException
     * @throws AndesException
     */
    private void startAndesBroker(BundleContext context) throws ConfigurationException, AndesException {
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

        // TODO: Have to re-structure how andes broker getting started.
        // there should be a separate andes-core component to initialize Andes Broker. Within
        // that component both Qpid and MQTT components should initialized.

        // Start AMQP server with given configurations
        startAMQPServer();

        // Start MQTT Server with given configurations
//        startMQTTServer();

        // Message broker is started with both AMQP and MQTT
        log.info("WSO2 Message Broker is started.");

        // Publish Qpid properties
        registrations.push(context.registerService(QpidService.class.getName(), qpidServiceImpl, null));
//        Integer brokerPort;
//        if (qpidServiceImpl.getIfSSLOnly()) {
//            brokerPort = qpidServiceImpl.getAMQPSSLPort();
//        } else {
//            brokerPort = qpidServiceImpl.getAMQPPort();
//        }
//        QpidServerDetails qpidServerDetails =
//                new QpidServerDetails(qpidServiceImpl.getAccessKey(),
//                        qpidServiceImpl.getClientID(),
//                        qpidServiceImpl.getVirtualHostName(),
//                        qpidServiceImpl.getHostname(),
//                        brokerPort.toString(), qpidServiceImpl.getIfSSLOnly());
//        QpidServiceDataHolder.getInstance().getEventBundleNotificationService().notifyStart(qpidServerDetails);

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

    /**
     * check whether the tcp port has started. some times the server started thread may return
     * before MQTT server actually bind to the tcp port. in that case there are some connection
     * time out issues.
     *
     * @throws ConfigurationException
     */
    private void startMQTTServer() throws ConfigurationException {

        boolean isServerStarted = false;
        int port;

        if (qpidServiceImpl.getMQTTSSLOnly()) {
            port = qpidServiceImpl.getMqttSSLPort();
        } else {
            port = qpidServiceImpl.getMqttPort();
        }

        if (AndesConfigurationManager.<Boolean>readValue(AndesConfiguration.TRANSPORTS_MQTT_ENABLED)) {
            while (!isServerStarted) {
                Socket socket = null;
                try {
                    InetAddress address = InetAddress.getByName(getMQTTTransportBindAddress());
                    socket = new Socket(address, port);
                    log.info("MQTT Host Address : " + address.getHostAddress() + " Port : " + port);
                    isServerStarted = socket.isConnected();
                    if (isServerStarted) {
                        log.info("Successfully connected to MQTT server on port " + port);
                    }
                } catch (IOException e) {
                    log.error("Wait until server starts on port " + port, e);
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
            log.warn("MQTT Transport is disabled as per configuration.");
        }
    }

//    /**
//     * Private class containing the tasks that need to be done at server shut down
//     */
//    private static class MBShutdownHandler implements ServerShutdownHandler, ServerRestartHandler {
//        @Override
//        public void invoke() {
//
//            try {
//                //executing pre-shutdown work for registered listeners before shutting down the andes server
//                for(BrokerLifecycleListener listener: QpidServiceDataHolder.getInstance()
//                                                                           .getBrokerLifecycleListeners()){
//                    listener.onShuttingdown();
//                }
//
//                AndesKernelBoot.shutDownAndesKernel();
//
//                //executing post-shutdown work for registered listeners after shutting down the andes server
//                for(BrokerLifecycleListener listener: QpidServiceDataHolder.getInstance()
//                                                                           .getBrokerLifecycleListeners()){
//                    listener.onShutdown();
//                }
//            } catch (AndesException e) {
//                log.error("Error while shutting down Andes kernel. ", e);
//            }
//        }
//    }

}
