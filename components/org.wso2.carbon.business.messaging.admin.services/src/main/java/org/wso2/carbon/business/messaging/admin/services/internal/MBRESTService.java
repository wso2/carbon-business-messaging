/*
 * Copyright (c) 2017, WSO2 Inc. (http://wso2.com) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.business.messaging.admin.services.internal;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Contact;
import io.swagger.annotations.Info;
import io.swagger.annotations.License;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.annotations.Tag;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.business.messaging.admin.services.exceptions.BrokerManagerException;
import org.wso2.carbon.business.messaging.admin.services.exceptions.InternalServerException;
import org.wso2.carbon.business.messaging.admin.services.managers.BrokerManagerService;
import org.wso2.carbon.business.messaging.admin.services.managers.impl.BrokerManagerServiceImpl;
import org.wso2.carbon.business.messaging.admin.services.types.Hello;
import org.wso2.carbon.business.messaging.admin.services.types.Protocols;
import org.wso2.carbon.business.messaging.core.Greeter;
import org.wso2.msf4j.Microservice;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Andes REST service is a microservice built on top of WSO2 msf4j. The REST service provides the capability of managing
 * resources of the WSO2 message broker. Resources being destinations, subscriptions and messages.
 */
@Component(
        name = "org.wso2.mb.admin.services.MBRESTService",
        service = Microservice.class,
        immediate = true,
        property = {
                "componentName=mb-rest-microservice"
        })

@SwaggerDefinition(
        info = @Info(
                title = "WSO2 Message Broker REST Service",
                version = "v1.0.0",
                description = "WSO2 Message Broker REST Service for managing resources such as destinations, "
                        + "permissions, messages and subscriptions.",
                license = @License(name = "Apache 2.0",
                                   url = "http://www.apache.org/licenses/LICENSE-2.0"),
                contact = @Contact(
                        name = "WSO2",
                        url = "http://wso2.com")),
        tags = {
                @Tag(name = "Destinations",
                     description = "Operations on handling destination related resources."),
                @Tag(name = "Permissions",
                     description = "Operations on handling permission related resources."),
                @Tag(name = "Messages",
                     description = "Operations on handling message related resources."),
                @Tag(name = "Subscriptions",
                      description = "Operations on handling subscription related resources."),
                @Tag(name = "Broker Details",
                     description = "Operations on getting broker details."), @Tag(name = "Dead Letter Channel",
                     description = "Operations related to dead letter channel.")
        },
        schemes = SwaggerDefinition.Scheme.HTTP)
@Api(value = "mb/v1.0.0",
     description = "Endpoint to WSO2 message broker REST service.")
@Path("/mb/v1.0.0")
public class MBRESTService implements Microservice {
    private static final Logger log = LoggerFactory.getLogger(MBRESTService.class);
    /**
     * Bundle registration service for andes REST service.
     */
    private ServiceRegistration serviceRegistration;

    /**
     * Service class for retrieving broker information
     */
    private BrokerManagerService brokerManagerService;

    public MBRESTService() {

    }

    /**
     * Say hello to the admin service.
     * <p>
     * curl command :
     * <pre>
     *  curl -v http://127.0.0.1:9090/mb/v1.0.0/hello
     * </pre>
     *
     * @return Return a response saying hello. <p>
     * <ul>
     * <li>{@link Response.Status#OK} - Return a response saying hello.</li>
     * </ul>
     */
    @GET
    @Path("/hello")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @ApiOperation(
            value = "Say hello to WSO2 MB",
            notes = "Can be used to test the broker service",
            tags = "test broker service",
            response = Hello.class,
            responseContainer = "List")
    @ApiResponses(value = {
            @ApiResponse(code = 200,
                         message = "hello response")
    })
    public Response sayHello() throws InternalServerException {
        try {
            Hello helloResponse = brokerManagerService.sayHello();
            return Response.status(Response.Status.OK).entity(helloResponse).build();
        } catch (BrokerManagerException ex) {
            throw new InternalServerException(ex);
        }
    }

    /**
     * Gets the protocol types supported by the broker.
     * <p>
     * curl command :
     * <pre>
     *  curl -v http://127.0.0.1:9090/mb/v1.0.0/protocol-types
     * </pre>
     *
     * @return Return a collection of supported protocol. <p>
     * <ul>
     * <li>{@link Response.Status#OK} - Returns a collection of protocols as a
     * response.</li>
     * </ul>
     */
    @GET
    @Path("/protocol-types")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @ApiOperation(
            value = "Gets supported protocols.",
            notes = "Gets supported protocols by the broker.",
            tags = "Broker Details",
            response = Protocols.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200,
                         message = "List of protocols.")
    })
    public Response getProtocols() throws InternalServerException {
        try {
            Protocols protocols = brokerManagerService.getSupportedProtocols();
            return Response.status(Response.Status.OK).entity(protocols).build();
        } catch (BrokerManagerException ex) {
            throw new InternalServerException(ex);
        }
    }

    /**
     * Setter method for brokerManagerService instance
     * @param brokerManagerService
     */
    public void setBrokerManagerService(BrokerManagerService brokerManagerService) {
        this.brokerManagerService = brokerManagerService;
    }

    /**
     * Called when the bundle is activated.
     *
     * @param bundleContext Bundle of the component.
     */
    @Activate
    protected void start(BundleContext bundleContext) {
        serviceRegistration = bundleContext.registerService(MBRESTService.class.getName(), this, null);
        log.info("Andes REST Service has started successfully.");
    }

    /**
     * Called when the bundle is deactivated.
     *
     * @param bundleContext Bundle of the component.
     */
    @Deactivate
    protected void stop(BundleContext bundleContext) {
        serviceRegistration.unregister();
        log.info("Andes REST Service has been deactivated.");
    }

    /**
     * This bind method will be called when CarbonRuntime OSGi service is registered.
     *
     * @param messagingCore The MessagingCore instance registered by Carbon Kernel as an OSGi service
     */
    @Reference(
            name = "org.wso2.carbon.business.messaging.core.internal.ServiceComponent",
            service = Greeter.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetMessagingCore"
    )
    protected void setMessagingCore(Greeter messagingCore) {
        log.info("Setting business messaging core service. ");
        MBRESTServiceDataHolder.getInstance().setMessagingCore(messagingCore);
        brokerManagerService = new BrokerManagerServiceImpl();
    }

    /**
     * This is the unbind method which gets called at the un-registration of CarbonRuntime OSGi service.
     *
     * @param messagingCore The MessagingCore instance registered by Carbon Kernel as an OSGi service
     */
    protected void unsetMessagingCore(Greeter messagingCore) {
        MBRESTServiceDataHolder.getInstance().setMessagingCore(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "Andes-REST-Service-OSGi-Bundle";
    }

}
