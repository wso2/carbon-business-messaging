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

package org.wso2.carbon.andes.internal;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.wso2.andes.server.BrokerOptions;
import org.wso2.andes.server.Main;
import org.wso2.andes.server.registry.ApplicationRegistry;
import org.wso2.andes.wso2.service.QpidNotificationService;
import org.wso2.carbon.andes.authentication.service.AuthenticationService;
import org.wso2.carbon.andes.service.QpidService;
import org.wso2.carbon.andes.service.QpidServiceImpl;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.base.api.ServerConfigurationService;
import org.wso2.carbon.cassandra.server.service.CassandraServerService;
import org.wso2.carbon.coordination.server.service.CoordinationServerService;
import org.wso2.carbon.event.core.EventBundleNotificationService;
import org.wso2.carbon.event.core.qpid.QpidServerDetails;
import org.wso2.carbon.utils.ServerConstants;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Set;

/**
 * @scr.component  name="org.wso2.carbon.andes.internal.QpidServiceComponent"
 *                              immediate="true"
 * @scr.reference    name="org.wso2.carbon.andes.authentication.service.AuthenticationService"
 *                              interface="org.wso2.carbon.andes.authentication.service.AuthenticationService"
 *                              cardinality="1..1"
 *                              policy="dynamic"
 *                              bind="setAccessKey"
 *                              unbind="unsetAccessKey"
 * @scr.reference    name="org.wso2.andes.wso2.service.QpidNotificationService"
 *                              interface="org.wso2.andes.wso2.service.QpidNotificationService"
 *                              cardinality="1..1"
 *                              policy="dynamic"
 *                              bind="setQpidNotificationService"
 *                              unbind="unsetQpidNotificationService"
 * @scr.reference    name="server.configuration"
 *                              interface="org.wso2.carbon.base.api.ServerConfigurationService"
 *                              cardinality="1..1"
 *                              policy="dynamic"
 *                              bind="setServerConfiguration"
 *                              unbind="unsetServerConfiguration"
 * @scr.reference    name="event.broker"
 *                              interface="org.wso2.carbon.event.core.EventBundleNotificationService"
 *                              cardinality="1..1"
 *                              policy="dynamic"
 *                              bind="setEventBundleNotificationService"
 *                              unbind="unsetEventBundleNotificationService"
 * @scr.reference    name="cassandra.service"
 *                              interface="org.wso2.carbon.cassandra.server.service.CassandraServerService"
 *                              cardinality="1..1"
 *                              policy="dynamic"
 *                              bind="setCassandraServerService"
 *                              unbind="unsetCassandraServerService"
 * @scr.reference    name="coordination.service"
 *                              interface="org.wso2.carbon.coordination.server.service.CoordinationServerService"
 *                              cardinality="1..1"
 *                              policy="dynamic"
 *                              bind="setCoordinationServerService"
 *                              unbind="unsetCoordinationServerService"
 */
public class QpidServiceComponent {

    private static final Log log = LogFactory.getLog(QpidServiceComponent.class);

    private static final String VM_BROKER_AUTO_CREATE = "amqj.AutoCreateVMBroker";
    private static final String DERBY_LOG_FILE = "derby.stream.error.file";
    private static final String QPID_DERBY_LOG_FILE = "/repository/logs/qpid-derby-store.log";
    private static final int CASSANDRA_THRIFT_PORT= 9160;
    private static String CARBON_CONFIG_PORT_OFFSET = "Ports.Offset";
    private static String CARBON_CONFIG_HOST_NAME = "HostName";
    private static int CARBON_DEFAULT_PORT_OFFSET = 0;


    private ServiceRegistration qpidService = null;


    private boolean activated = false;


    protected void activate(ComponentContext ctx) {


        if (ctx.getBundleContext().getServiceReference(QpidService.class.getName()) != null) {
            return;
        }

        // Make it possible to create VM brokers automatically
        System.setProperty(VM_BROKER_AUTO_CREATE, "true");
        // Set Derby log filename
        System.setProperty(DERBY_LOG_FILE, System.getProperty(ServerConstants.CARBON_HOME) + QPID_DERBY_LOG_FILE);
        
        QpidServiceImpl qpidServiceImpl =
                new QpidServiceImpl(QpidServiceDataHolder.getInstance().getAccessKey());

        CassandraServerService cassandraServerService = QpidServiceDataHolder.getInstance().getCassandraServerService();

        CoordinationServerService coordinationServerService = QpidServiceDataHolder.getInstance().
                getCoordinationServerService();

        if(coordinationServerService != null) {
            if(qpidServiceImpl.isClusterEnabled() && !qpidServiceImpl.isExternalZookeeperServerRequired()) {
                coordinationServerService.startServer();
                try {
                    Thread.sleep(2*1000);
                } catch (InterruptedException e) {

                }

                int count = 0;

                while (!isCoordinationServerStarted()) {
                    count++;
                    if(count > 60) {
                        break;
                    }
                    try {
                        Thread.sleep(10*1000);
                    } catch (InterruptedException e) {

                    }
                }
            }
        } else {
            log.error("Coordination Server service not set properly server will not start properly");
            throw new RuntimeException("Coordination Server service not set properly server will not start properly");
        }

        if(cassandraServerService != null) {
            if(!qpidServiceImpl.isExternalCassandraServerRequired()) {
                cassandraServerService.startServer();
                int count = 0;
                while (!isCassandraStarted()) {
                    count++;
                    if(count > 10) {
                        break;
                    }

                    try {
                        Thread.sleep(30*1000);
                    } catch (InterruptedException e) {

                    }
                }
            }
        } else {
            log.error("Cassandra Server service not set properly server will not start properly");
            throw new RuntimeException("Cassandra Server service not set properly server will not start properly");
        }

        // Start andes broker
        try {
            log.debug("Starting andes server");
            System.setProperty(BrokerOptions.QPID_HOME, qpidServiceImpl.getQpidHome());
            String[] args = {"-p" + qpidServiceImpl.getPort(), "-s" + qpidServiceImpl.getSSLPort()};
            //Main.setStandaloneMode(false);
            Main.main(args);

            // Remove Qpid shutdown hook so that I have control over shutting the broker down
            Runtime.getRuntime().removeShutdownHook(ApplicationRegistry.getShutdownHook());

            // Wait until the broker has started
            while (!isBrokerRunning()) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {}
            }

            //check whether the tcp port has started. some times the server started thread may return
            //before Qpid server actually bind to the tcp port. in that case there are some connection
            //time out issues.
            boolean isServerStarted = false;
            int port = Integer.parseInt(qpidServiceImpl.getPort());
            while (!isServerStarted) {
                Socket socket = null;
                try {
                    InetAddress address = InetAddress.getByName(getCarbonHostName());
                    socket = new Socket(address, port);
                    isServerStarted = socket.isConnected();
                    if (isServerStarted) {
                        log.info("Successfully connected to the server on port " + qpidServiceImpl.getPort());
                    }
                } catch (IOException e) {
                    log.info("Wait until Qpid server starts on port " + qpidServiceImpl.getPort());
                    Thread.sleep(500);
                } finally {
                    try {
                        if ((socket != null) && (socket.isConnected())) {
                            socket.close();
                        }
                    } catch (IOException e) {
                        log.error("Can not close the socket with is used to check the server status ");
                    }
                }
            }

        } catch (Exception e) {
            log.error("Failed to start Qpid broker : " + e.getMessage());
        } finally {
            // Publish Qpid properties
            qpidService = ctx.getBundleContext().registerService(
                    QpidService.class.getName(), qpidServiceImpl, null);
            QpidServerDetails qpidServerDetails =
                          new QpidServerDetails(qpidServiceImpl.getAccessKey(),
                                  qpidServiceImpl.getClientID(),
                                  qpidServiceImpl.getVirtualHostName(),
                                  qpidServiceImpl.getHostname(),
                                  qpidServiceImpl.getPort());
            QpidServiceDataHolder.getInstance().getEventBundleNotificationService().notifyStart(qpidServerDetails);
             activated =true;
        }
    }

    protected void deactivate(ComponentContext ctx) {
        // Unregister QpidService
        try {
            if (null != qpidService) {
                qpidService.unregister();
            }
        } catch (Exception e) {}

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

    protected void unsetQpidNotificationService(QpidNotificationService qpidNotificationService) {}

    protected void setServerConfiguration(ServerConfigurationService serverConfiguration) {
        QpidServiceDataHolder.getInstance().setCarbonConfiguration(serverConfiguration);
    }

    protected void unsetServerConfiguration(ServerConfigurationService serverConfiguration) {
        QpidServiceDataHolder.getInstance().setCarbonConfiguration(null);
    }

    protected void setEventBundleNotificationService(EventBundleNotificationService eventBundleNotificationService){
        QpidServiceDataHolder.getInstance().registerEventBundleNotificationService(eventBundleNotificationService);
    }

    protected void unsetEventBundleNotificationService(EventBundleNotificationService eventBundleNotificationService){
        // unsetting
    }


    protected void setCassandraServerService(CassandraServerService cassandraServerService){
        if (QpidServiceDataHolder.getInstance().getCassandraServerService() == null) {
            QpidServiceDataHolder.getInstance().registerCassandraServerService(cassandraServerService);
        }
    }

    protected void unsetCassandraServerService(CassandraServerService cassandraServerService){


    }

    protected void setCoordinationServerService(CoordinationServerService coordinationServerService) {
        if (QpidServiceDataHolder.getInstance().getCoordinationServerService() == null) {
            QpidServiceDataHolder.getInstance().setCoordinationServerService(coordinationServerService);
        }
    }

    protected void unsetCoordinationServerService(CoordinationServerService coordinationServerService) {


    }

    /**
        * Check if the broker is up and running
        *
        * @return
        *           true if the broker is running or false otherwise 
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
        }

        return response;
    }


    private boolean isCassandraStarted() {
        Socket socket = null;
        boolean status = false;
        try {
            int listenPort =  CASSANDRA_THRIFT_PORT + readPortOffset();
            socket = new Socket(InetAddress.getByName(getCarbonHostName()),listenPort);
        } catch (UnknownHostException e) {
            throw new RuntimeException("Unexpected Error while Checking for Cassandra Startup",e);
        } catch (IOException e) {
        } finally {
            if(socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {

                }
                status = true;
            }
        }
        log.debug("Checking for Cassandra server started status - status :" + status);
        return status;
    }


    private boolean isCoordinationServerStarted() {
         Socket socket = null;
        boolean status = false;
        try {

            CoordinationServerService coordinationServerService = QpidServiceDataHolder.getInstance().
                    getCoordinationServerService();

            String clientPortStr = coordinationServerService.getZKServerConfigurationProperties().
                    getProperty(CoordinationServerService.CLIENT_PORT);
            int listenPort =  clientPortStr!= null? Integer.parseInt(clientPortStr) : 2181 + readPortOffset();
            socket = new Socket(InetAddress.getByName(getCarbonHostName()),listenPort);
        } catch (UnknownHostException e) {
            throw new RuntimeException("Unexpected Error while Checking for Cassandra Startup",e);
        } catch (IOException e) {
        } finally {
            if(socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {

                }
                status = true;
            }
        }
        log.debug("Checking for Cassandra server started status - status :" + status);
        return status;
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


    private String getCarbonHostName() {
        ServerConfiguration carbonConfig = ServerConfiguration.getInstance();

        String hostName = carbonConfig.getFirstProperty(CARBON_CONFIG_HOST_NAME);

        return hostName != null ? hostName : "localhost";

    }
}
