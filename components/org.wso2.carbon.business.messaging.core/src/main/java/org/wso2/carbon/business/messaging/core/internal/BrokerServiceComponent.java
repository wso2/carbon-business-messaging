/*
 * Copyright (c) 2005-2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
import org.wso2.andes.configuration.models.BrokerConfiguration;
import org.wso2.andes.configuration.models.deployment.DeploymentConfiguration;
import org.wso2.andes.configuration.models.transport.TransportConfiguration;
import org.wso2.andes.kernel.AndesContext;
import org.wso2.andes.kernel.AndesException;
import org.wso2.andes.server.BrokerOptions;
import org.wso2.andes.server.ClusterResourceHolder;
import org.wso2.andes.server.Main;
import org.wso2.andes.server.registry.ApplicationRegistry;
import org.wso2.carbon.business.messaging.core.authentication.AuthenticationService;
import org.wso2.carbon.business.messaging.core.constants.BrokerConstants;
import org.wso2.carbon.business.messaging.core.service.BrokerService;
import org.wso2.carbon.business.messaging.core.service.BrokerServiceImpl;
import org.wso2.carbon.business.messaging.core.service.exception.ServiceConfigurationException;
import org.wso2.carbon.business.messaging.core.utils.MessageBrokerDBUtil;
import org.wso2.carbon.config.ConfigurationException;
import org.wso2.carbon.config.provider.ConfigProvider;
import org.wso2.carbon.datasource.core.api.DataSourceService;
import org.wso2.carbon.datasource.core.exception.DataSourceException;
import org.wso2.carbon.kernel.startupresolver.RequiredCapabilityListener;
import org.wso2.carbon.kernel.startupresolver.StartupServiceUtils;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.sql.DataSource;

/**
 * Service responsible for initializing  broker and registering data-sources and the relevant listeners.
 * This also acts as a capabilityListener for AuthenticationService where multiple implementations can be registered.
 *
 * @since 5.0.0
 */
@Component(name = "org.wso2.carbon.andes.internal.BrokerServiceComponent",
           immediate = true,
           service = RequiredCapabilityListener.class,
           property = {
                   "componentName=" + BrokerServiceComponent.COMPONENT_NAME
           })
@SuppressWarnings("unused")
public class BrokerServiceComponent implements RequiredCapabilityListener {

    private static final Log log = LogFactory.getLog(BrokerServiceComponent.class);
    /**
     * Unique component-name to act as the capability-listener.Any matching capability-provider
     * should use this component-name.
     */
    public static final String COMPONENT_NAME = "business-messaging-auth-mgt";
    /**
     * Defines the default offset of carbon
     */
    private static final int CARBON_DEFAULT_PORT_OFFSET = 0;

    /**
     * Holds the list of services registered with the broker
     */
    private static Stack<ServiceRegistration> services = new Stack<>();

    /**
     * This flag true if HazelcastInstance has been registered.
     */
    private boolean registeredHazelcast = false;

    /**
     * This holds the configuration values
     */
    private BrokerServiceImpl brokerService;
    /**
     * This holds authenticationServiceInstance that has been registered.
     */
    private AuthenticationService authService;
    /**
     * Holds the bundleContext acquired at start() method
     */
    private BundleContext bundleContext;
    /**
     * Holds the authentication service based implementations
     */
    private static List<AuthenticationService> authList = new ArrayList<>();

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
        CLUSTERED;

    }

    /**
     * This will store all the auth implementations of AuthenticationService class
     *
     * @param service implementation of Authentication Service interface.
     */
    @Reference(name = "auth.service.reference",
               service = AuthenticationService.class,
               cardinality = ReferenceCardinality.AT_LEAST_ONE,
               policy = ReferencePolicy.DYNAMIC,
               unbind = "unregisterAuthService")
    public void registerAuthService(AuthenticationService service) {
        authList.add(service);
        //This is required to sync the list of OSGI services known by OSGI framework and OSGI components.
        StartupServiceUtils.updateServiceCache(COMPONENT_NAME, AuthenticationService.class);
    }

    /**
     * Clear auth List when unbinding AuthenticationService
     *
     * @param service
     */
    public void unregisterAuthService(AuthenticationService service) {
        authList.remove(service);
    }

    /**
     * Get the ConfigProvider service.
     * This is the bind method that gets called for ConfigProvider service registration that satisfy the policy.
     *
     * @param configProvider the ConfigProvider service that is registered as a service.
     */
    @Reference(name = "carbon.config.provider",
               service = ConfigProvider.class,
               cardinality = ReferenceCardinality.MANDATORY,
               policy = ReferencePolicy.DYNAMIC,
               unbind = "unregisterConfigProvider")
    protected void registerConfigProvider(ConfigProvider configProvider) {
        DataHolder.getInstance().setConfigProvider(configProvider);
    }

    /**
     * This is the unbind method for the above reference that gets called for ConfigProvider instance un-registrations.
     *
     * @param configProvider the ConfigProvider service that get unregistered.
     */
    protected void unregisterConfigProvider(ConfigProvider configProvider) {
        DataHolder.getInstance().setConfigProvider(null);
    }

    /**
     * This method will store the bundlecontext at component start-up
     *
     * @param context the carbon core bundle instance used for service registration
     */
    @Activate
    public void start(BundleContext context) {
        this.bundleContext = context;
    }

    /**
     * This method will wait till all the registered implementations of AuthenticationService interface are available.
     * It will map the registered list of auth implementations with the class name mapped in broker.xml,
     * and set the matching implementation for further user.Finally it will start the broker once,
     * all it's dependencies/references are satisfied.
     */
    @Override
    public void onAllRequiredCapabilitiesAvailable() {
        try {
            ConfigProvider configProvider = DataHolder.getInstance().getConfigProvider();
            //Initialize AndesConfigurationManager, this will inform broker on the relevant port offset
            AndesConfigurationManager.initialize(readPortOffset());

            //Load qpid specific configurations
            brokerService = new BrokerServiceImpl(BrokerServiceDataHolder.getInstance().getAccessKey());
            brokerService.loadConfigurations();

            // set message store and andes context store related configurations
            AndesContext.getInstance().constructStoreConfiguration();

            // Read deployment mode
            String mode = configProvider.getConfigurationObject(DeploymentConfiguration.class).getMode();
            // String mode = AndesConfigurationManager.readValue(AndesConfiguration.DEPLOYMENT_MODE);
            // Read authentication service implementation class name from broker.xml
            String authenticatorName = configProvider.getConfigurationObject(BrokerConfiguration.class)
                    .getAuthenticator();
            //AndesConfigurationManager.readValue(AndesConfiguration.AUTHENTICATOR_CLASS);
            // All auth implementations that are registered , are available.
            //Get relevant authentication service implementation the user has specified.
            authList.forEach(service -> {
                if (authenticatorName.equals(service.getClass().getName())) {
                    authService = service;
                    BrokerServiceDataHolder.getInstance().setAuthenticationService(authService);
                }
            });
            if (BrokerServiceDataHolder.getInstance().getAuthenticationService() == null) {
                log.error("No matching authentication implementation was found for:" + authenticatorName);
            }

            // Start broker in standalone mode
            if (Modes.STANDALONE.name().equalsIgnoreCase(mode)) {
                // set clustering enabled to false because even though clustering enabled in axis2.xml, we are not
                // going to consider it in standalone mode
                AndesContext.getInstance().setClusteringEnabled(false);
                this.startAndesBroker(bundleContext);
            } else if (mode.equalsIgnoreCase(Modes.CLUSTERED.name())) {
                //TODO this flow would change once clustering would be introduced
                //TODO using RequiredCapabilityListener would be an option
                // Start broker in HA mode
                AndesContext.getInstance().setClusteringEnabled(true);
                this.startAndesBroker(bundleContext);
            } else {
                throw new ServiceConfigurationException("Invalid value " + mode + " for deployment/mode in broker.xml");
            }

        } catch (ServiceConfigurationException | ConfigurationException e) {
            //We do not propagate the exception further, to avoid the bundle to be attempted to be made active in cycle
            String error = "Invalid configuration found in a configuration file";
            log.error(error, e);
        } catch (AndesException e) {
            log.error("An error occurred while broker startup", e);
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
    @Reference(name = "org.wso2.carbon.datasource.DataSourceService",
               service = DataSourceService.class,
               cardinality = ReferenceCardinality.MANDATORY,
               policy = ReferencePolicy.DYNAMIC,
               unbind = "unregisterDataSourceService")
    protected void registerDataService(DataSourceService dataSourceService) {
        try {
            DataSource dataSource = (HikariDataSource) dataSourceService.getDataSource(BrokerConstants.MB_DS_NAME);
            ClusterResourceHolder.getInstance().setDatasource(dataSource);
        } catch (DataSourceException e) {
            //We do not propagate the exception further since the framework would re attempt to activate the bundle
            String error = "Error occurred while registering data service ";
            log.error(error, e);
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
            Set<ObjectName> set = mBeanServer.queryNames(new ObjectName(BrokerConstants.VIRTUAL_HOST_MBEAN), null);

            if (set.size() > 0) { // Virtual hosts created, hence broker running.
                response = true;
            }
        } catch (MalformedObjectNameException e) {
            //We do not propagate the exception further since the framework would re attempt to activate the bundle
            String error = "Error checking if broker is running.";
            log.error(error, e);
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
            String error = "Non-numeric value provided in the configuration as port offset, using the default "
                    + CARBON_DEFAULT_PORT_OFFSET;
            log.error(error, e);
            //We do not propagate the exception further
            return CARBON_DEFAULT_PORT_OFFSET;
        }
    }

    /***
     * This applies the AMQPbindAddress from broker.xml instead of the hostname from carbon.xml within MB.
     * @return host name as derived from broker.xml
     */
    private String getAMQPTransportBindAddress() {
        String bindAddress = "";
        try {
            bindAddress = DataHolder.getInstance().getConfigProvider()
                    .getConfigurationObject(TransportConfiguration.class).getAmqpConfiguration().getBindAddress();
        } catch (ConfigurationException e) {
            log.error("Configuration issue occurred..", e);
        }
        //AndesConfigurationManager.readValue(AndesConfiguration.TRANSPORTS_AMQP_BIND_ADDRESS);
        return bindAddress;
    }

    /**
     * Start Andes Broker and related components with given configurations.
     *
     * @param context the information containing OSGI bundle information
     * @throws ServiceConfigurationException
     * @throws AndesException
     */
    private void startAndesBroker(BundleContext context)
            throws ServiceConfigurationException, ConfigurationException, AndesException {
        String dSetupValue = System.getProperty(BrokerConstants.SYS_PROP_DATABASE_SETUP);
        if (dSetupValue != null) {
            // Source MB rdbms database if data source configurations and supported sql exist
            MessageBrokerDBUtil messageBrokerDBUtil = new MessageBrokerDBUtil();
            messageBrokerDBUtil.initialize();
        }
        // Start andes broker

        log.info("Activating Andes Message Broker Engine...");
        System.setProperty(BrokerOptions.ANDES_HOME, brokerService.getQpidHome());
        String[] args = {
                "-p" + brokerService.getAMQPPort(), "-s" + brokerService.getAMQPSSLPort(),
                "-q" + brokerService.getMqttPort()
        };

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
     * @throws ServiceConfigurationException
     */
    private void startAMQPServer() throws ServiceConfigurationException, ConfigurationException {

        boolean isServerStarted = false;
        int port;
        if (brokerService.getIfSSLOnly()) {
            port = brokerService.getAMQPSSLPort();
        } else {
            port = brokerService.getAMQPPort();
        }

        if (DataHolder.getInstance().getConfigProvider().getConfigurationObject(TransportConfiguration.class)
                .getAmqpConfiguration().getEnabled()) {
            while (!isServerStarted) {
                Socket socket = null;
                try {
                    InetAddress address = InetAddress.getByName(getAMQPTransportBindAddress());
                    socket = new Socket(address, port);
                    log.info("AMQP Host Address : " + address.getHostAddress() + " Port : " + port);
                    isServerStarted = socket.isConnected();
                    if (isServerStarted) {
                        log.info("Successfully connected to AMQP server " + "on port " + port);
                    }
                } catch (IOException e) {
                    //There will be a continuous retry effort to start the qpid server, if an error occurs the
                    // exception will not be propagated further
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
                        //There will be a continuous retry effort to close the socket, if an error occurs the
                        // exception will not be propagated further
                        log.error("Can not close the socket which is used to check the server status ", e);
                    }
                }
            }
        } else {
            log.warn("AMQP Transport is disabled as per configuration.");
        }
    }
}
