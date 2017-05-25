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
import io.swagger.annotations.ApiParam;
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
import org.wso2.andes.kernel.Andes;
import org.wso2.carbon.business.messaging.admin.services.exceptions.BrokerManagerException;
import org.wso2.carbon.business.messaging.admin.services.exceptions.DestinationManagerException;
import org.wso2.carbon.business.messaging.admin.services.exceptions.DestinationNotFoundException;
import org.wso2.carbon.business.messaging.admin.services.exceptions.InternalServerException;
import org.wso2.carbon.business.messaging.admin.services.exceptions.InvalidLimitValueException;
import org.wso2.carbon.business.messaging.admin.services.exceptions.InvalidOffsetValueException;
import org.wso2.carbon.business.messaging.admin.services.exceptions.MessageManagerException;
import org.wso2.carbon.business.messaging.admin.services.managers.BrokerManagerService;
import org.wso2.carbon.business.messaging.admin.services.managers.DestinationManagerService;
import org.wso2.carbon.business.messaging.admin.services.managers.MessageManagerService;
import org.wso2.carbon.business.messaging.admin.services.managers.impl.BrokerManagerServiceImpl;
import org.wso2.carbon.business.messaging.admin.services.managers.impl.DestinationManagerServiceImpl;
import org.wso2.carbon.business.messaging.admin.services.managers.impl.MessageManagerServiceImpl;
import org.wso2.carbon.business.messaging.admin.services.types.ClusterInformation;
import org.wso2.carbon.business.messaging.admin.services.types.Destination;
import org.wso2.carbon.business.messaging.admin.services.types.DestinationNamesList;
import org.wso2.carbon.business.messaging.admin.services.types.ErrorResponse;
import org.wso2.carbon.business.messaging.admin.services.types.Hello;
import org.wso2.carbon.business.messaging.admin.services.types.NewDestination;
import org.wso2.carbon.business.messaging.admin.services.types.Protocols;
import org.wso2.msf4j.Microservice;

import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
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
                     description = "Operations on getting broker details."),
                @Tag(name = "Dead Letter Channel",
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

    /**
     * Service class for retrieving messages related information
     */
    private MessageManagerService messageManagerService;

    /**
     * Service class for processing destination related requests
     */
    private DestinationManagerService destinationManagerService;

    public MBRESTService() {

    }

    /**
     * Say hello to the admin service.
     * <p>
     * curl command :
     * <pre>
     *  curl -v http://127.0.0.1:8080/mb/v1.0.0/hello
     * </pre>
     *
     * @return Return a response saying hello. <p>
     * @throws InternalServerException Server error when processing the request
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
     *  curl -v http://127.0.0.1:8080/mb/v1.0.0/protocol-types
     * </pre>
     *
     * @return Return a collection of supported protocol. <p>
     * @throws InternalServerException Server error when processing the request
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
     * Gets Clustering information of the broker.
     * <p>
     * curl command :
     * <pre>
     * curl -v http://127.0.0.1:8080/mb/v1.0.0/cluster-info
     * </pre>
     *
     * @return Return information of the clustering . <p>
     * @throws InternalServerException Server error when processing the request
     * <ul>
     * <li>{@link Response.Status#OK} - Returns a whether the clustering is enabled of not,
     * if do enabled further information regarding the cluster is returned.</li>
     * </ul>
     */
    @GET
    @Path("/cluster-info")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @ApiOperation(
            value = "Get cluster information.",
            notes = "Get cluster information of the broker.",
            tags = "Broker Details",
            response = ClusterInformation.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200,
                         message = "Cluster information.")
    })
    public Response getClusterInfo() throws InternalServerException {
        try {
            ClusterInformation clusterInformation = brokerManagerService.getClusterInformation();
            return Response.status(Response.Status.OK).entity(clusterInformation).build();
        } catch (BrokerManagerException ex) {
            throw new InternalServerException(ex);
        }
    }

    /**
     * Creates a new destination. A topic will be created even if "durable_topic" is requested as the destination type.
     * <p>
     * curl command example :
     * <pre>
     *  curl -v POST http://127.0.0.1:8080/mb/v1.0.0/amqp/destination-type/queue -H "Content-Type: application/json"
     *  -d '{"destinationName": "Q12"}'
     * </pre>
     *
     * @param protocol        The protocol type of the destination
     * @param destinationType The destination type of the destination
     *                        "durable_topic" is considered as a topic.
     * @param newDestination  A {@link NewDestination} object.
     * @return A JSON representation of the newly created {@link Destination}. <p>
     * @throws InternalServerException Server error when processing the request
     * <ul>
     *     <li>{@link javax.ws.rs.core.Response.Status#OK} - Returns a {@link Destination} as a JSON response.</li>
     *     <li>{@link javax.ws.rs.core.Response.Status#INTERNAL_SERVER_ERROR} - Error while creating new destination
     *     .</li>
     * </ul>
     */
    @POST
    @Path("/{protocol}/destination-type/{destination-type}")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @ApiOperation(
            value = "Creates a destination.",
            notes = "Creates a destination that belongs to a specific protocol and destination type.",
            tags = "Destinations")
    @ApiResponses(value = {
            @ApiResponse(code = 202, message = "Destination creation event has been triggered."),
            @ApiResponse(code = 500, message = "Server error on creating destination", response = ErrorResponse.class)})
    public Response createDestination(
            @ApiParam(value = "Protocol for the destination.")
            @PathParam("protocol") String protocol,
            @ApiParam(value = "Destination type for the destination. \"durable_topic\" is considered as a topic.")
            @PathParam("destination-type") String destinationType,
            @ApiParam(value = "New destination object.") NewDestination newDestination,
            @Context org.wso2.msf4j.Request request) throws InternalServerException {
        //TODO: Add other details on queue? durable, exclusive, username ?
        try {
            boolean destinationExist = destinationManagerService.isDestinationExist(protocol, destinationType,
                    newDestination.getDestinationName());
            if (!destinationExist) {
                destinationManagerService
                        .createDestination(protocol, destinationType, newDestination.getDestinationName());
                return Response.status(Response.Status.ACCEPTED)
                        .header(HttpHeaders.LOCATION, request.getUri() + "/name/" + newDestination.getDestinationName())
                        .build();
            } else {
                throw new DestinationManagerException("Destination '" + newDestination.getDestinationName()
                        + "' already exists.");
            }
        } catch (DestinationManagerException e) {
            throw new InternalServerException(e);
        }
    }


    /**
     * Deletes destination. A topic will be deleted even if "durable_topic" is requested as the destination type.
     * <p>
     * curl command example :
     * <pre>
     *  curl -v -X DELETE http://127.0.0.1:8080/mb/v1.0.0/amqp/destination-type/queue/name/MyQueue
     *  curl -v -X DELETE http://127.0.0.1:8080/mb/v1.0.0/amqp/destination-type/topic/name/MyTopic
     *  curl -v -X DELETE http://127.0.0.1:8080/mb/v1.0.0/amqp/destination-type/durable_topic/name/MyDurable
     *  curl -v -X DELETE http://127.0.0.1:8080/mb/v1.0.0/mqtt-default/destination-type/topic/name/MyMQTTTopic
     * </pre>
     *
     * @param protocol        The protocol type of the destination
     * @param destinationType The destination type of the destination
     *                        "durable_topic" is considered as a topic.
     * @param destinationName The name of the destination to delete.
     * @return No response body. <p>
     * @throws InternalServerException Server error when processing the request
     * @throws DestinationNotFoundException Request destination is not found
     * <ul>
     *     <li>{@link javax.ws.rs.core.Response.Status#OK} - Destination was successfully deleted.</li>
     *     <li>{@link javax.ws.rs.core.Response.Status#INTERNAL_SERVER_ERROR} - Error occurred while deleting
     *     destination from the broker.</li>
     * </ul>
     */
    @DELETE
    @Path("/{protocol}/destination-type/{destination-type}/name/{destination-name}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @ApiOperation(
            value = "Deletes a destination.",
            notes = "Deletes a destination that belongs to a specific protocol and destination type.",
            tags = "Destinations")
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Destination deleted."),
            @ApiResponse(code = 404, message = "Invalid protocol or destination type or Destination not found.",
                         response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "Server Error.", response = ErrorResponse.class)})
    public Response deleteDestination(
            @ApiParam(value = "Protocol for the destination.")
            @PathParam("protocol") String protocol,
            @ApiParam(value = "Destination type for the destination. \"durable_topic\" is considered as a topic.")
            @PathParam("destination-type") String destinationType,
            @ApiParam(value = "The name of the destination")
            @PathParam("destination-name") String destinationName)
            throws InternalServerException, DestinationNotFoundException {
        try {
            boolean destinationExist = destinationManagerService.isDestinationExist(protocol, destinationType,
                    destinationName);
            if (destinationExist) {
                destinationManagerService.deleteDestination(protocol, destinationType, destinationName);
            return Response.status(Response.Status.NO_CONTENT).build();
            } else {
                throw new DestinationNotFoundException("Destination '" + destinationName + "' not found.");
            }
        } catch (DestinationManagerException e) {
            throw new InternalServerException(e);
        }
    }

    /**
     * Gets destinations that belongs to a specific protocol and destination type.
     * <p>
     * curl command :
     * <pre>
     *  curl -v http://127.0.0.1:8080/mb/v1.0.0/amqp/destination-type/queue
     *  curl -v http://127.0.0.1:8080/mb/v1.0.0/amqp/destination-type/topic
     * </pre>
     *
     * @param protocol        The protocol type of the destination
     * @param destinationType The destination type of the destination
     * @param destinationName The name of the destination. If "*", all destinations are returned, else destinations that
     *                        <strong>contains</strong> the value will be returned.
     * @param offset          The starting index of the return destination list for pagination. Default value is 0.
     * @param limit           The number of destinations to return for pagination. Default value is 20.
     * @return Return an instance of {@link DestinationNamesList}.  <p>
     * @throws InternalServerException Server error when processing the request
     * @throws InvalidLimitValueException Invalid Limit
     * @throws InvalidOffsetValueException Invalid offset
     */
    @GET
    @Path("/{protocol}/destination-type/{destination-type}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @ApiOperation(
            value = "Gets destinations.",
            notes = "Gets destinations that belongs to a specific protocol and destination type. Supports pagination.",
            tags = "Destinations",
            response = DestinationNamesList.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "A list of destinations.", response = DestinationNamesList.class),
            @ApiResponse(code = 400, message = "Invalid values for offset or limit.", response = ErrorResponse.class),
            @ApiResponse(code = 404, message = "Invalid protocol or destination type.", response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "Server Error.", response = ErrorResponse.class)})
    public Response getDestinations(
            @ApiParam(value = "Protocol for the destination.")
            @PathParam("protocol") String protocol,
            @ApiParam(value = "Destination type for the destination.")
            @PathParam("destination-type") String destinationType,
            @ApiParam(value = "The name of the destination. If \"*\", all destinations are returned, else " +
                    "destinations that contains the value will be returned.")
            @DefaultValue("*") @QueryParam("name") String destinationName,
            @ApiParam(value = "The starting index of the return destination list for pagination.",
                      allowableValues = "range[0, infinity]")
            @DefaultValue("0") @QueryParam("offset") int offset,
            @ApiParam(value = "The number of destinations to return for pagination.",
                      allowableValues = "range[1, infinity]")
            @DefaultValue("20") @QueryParam("limit") int limit,
            @Context org.wso2.msf4j.Request request)
            throws InternalServerException, InvalidLimitValueException, InvalidOffsetValueException {
        try {
            //TODO: Add pagination support, Add search by part of the destination name
            if (offset < 0) {
                throw new InvalidOffsetValueException();
            }
            if (limit < 1) {
                throw new InvalidLimitValueException();
            }

            List<String> destinationList = destinationManagerService
                    .getDestinations(protocol, destinationType, destinationName, offset, limit);

            DestinationNamesList destinationNamesList = new DestinationNamesList();
            destinationNamesList.setDestinationNames(destinationList);
            destinationNamesList.setDestinationType(destinationType);
            destinationNamesList.setProtocol(protocol);

            return Response.status(Response.Status.OK).entity(destinationNamesList).build();
        } catch (DestinationManagerException e) {
            throw new InternalServerException(e);
        }
    }

    /**
     * Purge all messages belonging to a destination.
     * <p>
     * curl command example :
     * <pre>
     *  curl -X DELETE http://127.0.0.1:8080/mb/v1.0.0/amqp/destination-type/queue/name/MyQueue/messages
     * </pre>
     *
     * @param protocol        The protocol type matching for the message.
     * @param destinationType The destination type matching for the message.
     * @param destinationName The name of the destination to purge messages.
     * @return No response body. <p>
     * @throws DestinationNotFoundException Request destination is not found
     * @throws InternalServerException Server error when processing the request
     * <ul>
     *     <li>{@link javax.ws.rs.core.Response.Status#OK} - Messages were successfully deleted.</li>
     *     <li>{@link javax.ws.rs.core.Response.Status#INTERNAL_SERVER_ERROR} - Error occurred while deleting
     *     messages from the broker.</li>
     * </ul>
     */
    @DELETE
    @Path("/{protocol}/destination-type/{destination-type}/name/{destination-name}/messages")
    @ApiOperation(
            value = "Deletes/Purge message.",
            notes = "Deletes/Purge message belonging to a specific destination.",
            tags = "Messages")
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Messages purged successfully."),
            @ApiResponse(code = 404,
                         message = "Invalid protocol, destination type or destination name, Message not found."),
            @ApiResponse(code = 500, message = "Server Error.", response = ErrorResponse.class)})
    public Response deleteMessages(
            @ApiParam(value = "Protocol for the message.")
            @PathParam("protocol") String protocol,
            @ApiParam(value = "Destination type for the message.")
            @PathParam("destination-type") String destinationType,
            @ApiParam(value = "The name of the destination of the message.")
            @PathParam("destination-name") String destinationName)
            throws DestinationNotFoundException, InternalServerException {

        try {
            boolean destinationExist = destinationManagerService.isDestinationExist(protocol, destinationType,
                    destinationName);
            if (destinationExist) {
                messageManagerService.deleteMessages(protocol, destinationType, destinationName);
                return Response.status(Response.Status.NO_CONTENT).build();
            } else {
                throw new DestinationNotFoundException("Destination '" + destinationName + "' not found to " +
                        "delete/purge messages.");
            }
        } catch (DestinationManagerException | MessageManagerException e) {
            throw new InternalServerException(e);
        }
    }

    /**
     * Gets a specific destination belonging to a specific protocol and destination type.
     * <p>
     * curl command example :
     * <pre>
     *  curl -v http://127.0.0.1:8080/mb/v1.0.0/amqp/destination-type/queue/name/MyQueue
     * </pre>
     *
     * @param protocol        The protocol type of the destination
     * @param destinationType The destination type of the destination
     * @param destinationName The name of the destination.
     * @return A JSON/XML representation of {@link Destination}. <p>
     * @throws InternalServerException Server error when processing the request
     * @throws DestinationNotFoundException Request destination is not found
     * <ul>
     *     <li>{@link javax.ws.rs.core.Response.Status#OK} - Returns a {@link Destination} as a JSON response.</li>
     *     <li>{@link javax.ws.rs.core.Response.Status#NOT_FOUND} - Such destination does not exists.</li>
     *     <li>{@link javax.ws.rs.core.Response.Status#INTERNAL_SERVER_ERROR} - Error occurred when getting the
     *     destination from the broker.</li>
     * </ul>
     */
    @GET
    @Path("/{protocol}/destination-type/{destination-type}/name/{destination-name}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @ApiOperation(
            value = "Gets a destination.",
            notes = "Gets a destination that belongs to a specific protocol and destination type.",
            tags = "Destinations")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Destination returned.", response = Destination.class),
            @ApiResponse(code = 404, message = "Invalid protocol or destination type or destination not found.",
                         response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "Server Error.", response = ErrorResponse.class)})
    public Response getDestination(
            @ApiParam(value = "Protocol for the destination.")
            @PathParam("protocol") String protocol,
            @ApiParam(value = "Destination type for the destination.")
            @PathParam("destination-type") String destinationType,
            @ApiParam(value = "The name of the destination.")
            @PathParam("destination-name") String destinationName)
            throws InternalServerException, DestinationNotFoundException {
        try {
            Destination destination = destinationManagerService.getDestination(protocol, destinationType,
                    destinationName);
            if (null != destination) {
                return Response.status(Response.Status.OK).entity(destination).build();
            } else {
                throw new DestinationNotFoundException("Destination '" + destinationName + "' not found.");
            }
        } catch (DestinationManagerException e) {
            throw new InternalServerException(e);
        }
    }

    /**
     * Setter method for brokerManagerService instance
     * @param brokerManagerService
     */
    public void setBrokerManagerService(BrokerManagerService brokerManagerService) {
        this.brokerManagerService = brokerManagerService;
    }

    /************** OSGi Methods ********************************************************************************/

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
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "Andes-REST-Service-OSGi-Bundle";
    }


    @Reference(
            name = "org.wso2.andes.kernel",
            service = Andes.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetAndesCore"
    )
    protected void setAndesCore(Andes andesCore) {
        log.info("Setting andes core service. ");
        MBRESTServiceDataHolder.getInstance().setAndesCore(andesCore);
        brokerManagerService = new BrokerManagerServiceImpl();
        destinationManagerService = new DestinationManagerServiceImpl();
        messageManagerService = new MessageManagerServiceImpl();
    }

    /**
     * This is the unbind method which gets called at the un-registration of CarbonRuntime OSGi service.
     *
     * @param andesCore The MessagingCore instance registered by Carbon Kernel as an OSGi service
     */
    protected void unsetAndesCore(Andes andesCore) {
        MBRESTServiceDataHolder.getInstance().setAndesCore(null);
    }

}
