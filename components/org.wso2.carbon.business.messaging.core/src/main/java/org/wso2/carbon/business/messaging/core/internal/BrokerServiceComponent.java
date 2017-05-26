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
import org.wso2.andes.configuration.AndesConfigurationManager;
import org.wso2.andes.configuration.enums.AndesConfiguration;
import org.wso2.andes.kernel.AndesContext;
import org.wso2.andes.kernel.AndesException;
import org.wso2.andes.server.BrokerOptions;
import org.wso2.andes.server.ClusterResourceHolder;
import org.wso2.andes.server.Main;
import org.wso2.andes.server.registry.ApplicationRegistry;
import org.wso2.carbon.business.messaging.core.constants.BrokerConstants;
import org.wso2.carbon.business.messaging.core.service.BrokerService;
import org.wso2.carbon.business.messaging.core.service.BrokerServiceImpl;
import org.wso2.carbon.business.messaging.core.service.exception.ConfigurationException;
import org.wso2.carbon.business.messaging.core.utils.MessageBrokerDBUtil;
import org.wso2.carbon.datasource.core.api.DataSourceService;
import org.wso2.carbon.datasource.core.exception.DataSourceException;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Set;
import java.util.Stack;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.sql.DataSource;


/**
 * Service responsible for initializing  broker and registering data-sources and the relevant listeners
 *
 * @since 5.0.0
 */
@Component(
        name = "org.wso2.carbon.andes.internal.BrokerServiceComponent",
        immediate = true
)
@SuppressWarnings("unused")
public class BrokerServiceComponent {

    private static final Log log = LogFactory.getLog(BrokerServiceComponent.class);

    /**
     * Defines the default offset of carbon
     */
    private static final int CARBON_DEFAULT_PORT_OFFSET = 0;

    /**
     * Holds the list of services registered with the broker
     */
    private static Stack<ServiceRegistration> services = new Stack<>();

    /**
     * This is used in the situations where the Hazelcast instance is not registered but the activate method of the
     * BrokerServiceComponent is called when clustering is enabled.
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
    private BrokerServiceImpl brokerService;

    /**
     * Server startup modes
     */
    private enum Modes {
        /**
         * Starts as a stand-alone broker
         */
        STANDALONE,

        /**
         * Starts the server in default mode
         */
        DEFAULT;

    }

    /**
     * This method will start the broker once all it's dependencies/references are satisfied
     *
     * @param context the carbon core bundle instance used for service registration
     * @throws AndesException this will be thrown if an error is encountered while the service is being active
     */
    @Activate
    public void start(BundleContext context) throws AndesException {
        try {
            //Initialize AndesConfigurationManager, this will inform broker on the relevant port offset
            AndesConfigurationManager.initialize(readPortOffset());

            //Load qpid specific configurations
            brokerService = new BrokerServiceImpl(BrokerServiceDataHolder.getInstance().getAccessKey());
            brokerService.loadConfigurations();

            // set message store and andes context store related configurations
            AndesContext.getInstance().constructStoreConfiguration();

            // Read deployment mode
            String mode = AndesConfigurationManager.readValue(AndesConfiguration.DEPLOYMENT_MODE);

            // Start broker in standalone mode
            if (Modes.STANDALONE.name().equalsIgnoreCase(mode)) {
                // set clustering enabled to false because even though clustering enabled in axis2.xml, we are not
                // going to consider it in standalone mode
                AndesContext.getInstance().setClusteringEnabled(false);
                this.startAndesBroker(context);
            } else if (mode.equalsIgnoreCase(Modes.DEFAULT.name())) {
                // Start broker in HA mode
                if (!AndesContext.getInstance().isClusteringEnabled()) {
                    // If clustering is disabled, broker starts without waiting for hazelcastInstance
                    this.startAndesBroker(context);
                } else {
                    //TODO this flow would change once clustering would be introduced
                    //TODO using RequiredCapabilityListener would be an option
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

        } catch (ConfigurationException e) {
            log.error("Invalid configuration found in a configuration file", e);
        }

    }

    /**
     * This method will be called once the osgi bundle is deactivated, this method will un-register all the services
     *
     * @param ctx bundle context information
     */
    @Deactivate
    protected void stop(BundleContext ctx) {
        // By this time, through the AndesServerShutDownListener, All other services/ workers including this service,
        // have been closed.
        // Unregister services
        while (!services.empty()) {
            services.pop().unregister();
        }
    }

    /**
     * This method will be called to add/register the data-sources with the broker
     *
     * @param dataSourceService the declarative service which allows access to db instance
     */
    @Reference(
            name = "org.wso2.carbon.datasource.DataSourceService",
            service = DataSourceService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unregisterDataSourceService"
    )
    protected void registerDataService(DataSourceService dataSourceService) {
        try {
            DataSource dataSource = (HikariDataSource) dataSourceService.getDataSource(BrokerConstants.MB_DS_NAME);
            ClusterResourceHolder.getInstance().setDatasource(dataSource);
        } catch (DataSourceException e) {
            log.error("Error occurred while registering data service ", e);
        }
    }

    /**
     * Unregister data-source service
     *
     * @param dataSourceService datasource service description
     */
    protected void unregisterDataSourceService(DataSourceService dataSourceService) {
        ClusterResourceHolder.getInstance().setDatasource(null);
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
            Set<ObjectName> set = mBeanServer.queryNames(new ObjectName(BrokerConstants.VIRTUAL_HOST_MBEAN),
                    null);

            if (set.size() > 0) { // Virtual hosts created, hence broker running.
                response = true;
            }
        } catch (MalformedObjectNameException e) {
            log.error("Error checking if broker is running.", e);
        }

        return response;
    }

    /**
     * <p>
     * Reads the port offset provided in the CLI/System Property
     * </p>
     *
     * @return the value of the the port offset
     */
    private int readPortOffset() {
        String portOffset = System.getProperty("portOffset", "0"); //todo carbonConfig.getFirstProperty
        // (CARBON_CONFIG_PORT_OFFSET)
        try {
            return ((portOffset != null) ? Integer.parseInt(portOffset.trim()) : CARBON_DEFAULT_PORT_OFFSET);
        } catch (NumberFormatException e) {
            return CARBON_DEFAULT_PORT_OFFSET;
        }
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
     * @param context the information containing OSGI bundle information
     * @throws ConfigurationException
     * @throws AndesException
     */
    private void startAndesBroker(BundleContext context) throws ConfigurationException, AndesException {
        brokerShouldBeStarted = false;

        String dSetupValue = System.getProperty(BrokerConstants.SYS_PROP_DATABASE_SETUP);
        if (dSetupValue != null) {
            // Source MB rdbms database if data source configurations and supported sql exist
            MessageBrokerDBUtil messageBrokerDBUtil = new MessageBrokerDBUtil();
            messageBrokerDBUtil.initialize();
        }
        // Start andes broker

        log.info("Activating Andes Message Broker Engine...");
        System.setProperty(BrokerOptions.ANDES_HOME, brokerService.getQpidHome());
        String[] args = {"-p" + brokerService.getAMQPPort(), "-s" + brokerService.getAMQPSSLPort(),
                "-q" + brokerService.getMqttPort()};

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
        services.push(context.registerService(BrokerService.class.getName(), brokerService, null));
    }

    /**
     * <p>
     * check whether the tcp port has started. some times the server started thread may return
     * before Qpid server actually bind to the tcp port. in that case there are some connection
     * time out issues.
     * </p>
     *
     * @throws ConfigurationException
     */
    private void startAMQPServer() throws ConfigurationException {

        boolean isServerStarted = false;
        int port;
        if (brokerService.getIfSSLOnly()) {
            port = brokerService.getAMQPSSLPort();
        } else {
            port = brokerService.getAMQPPort();
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
