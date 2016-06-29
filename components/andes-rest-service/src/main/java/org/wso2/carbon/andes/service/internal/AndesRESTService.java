/*
 * Copyright (c) 2016, WSO2 Inc. (http://wso2.com) All Rights Reserved.
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

package org.wso2.carbon.andes.service.internal;

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
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
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
import org.wso2.carbon.andes.core.Andes;
import org.wso2.carbon.andes.core.AndesConstants;
import org.wso2.carbon.andes.core.AndesException;
import org.wso2.carbon.andes.core.DestinationType;
import org.wso2.carbon.andes.core.ProtocolType;
import org.wso2.carbon.andes.core.util.DLCQueueUtils;
import org.wso2.carbon.andes.service.exceptions.BrokerManagerException;
import org.wso2.carbon.andes.service.exceptions.DestinationManagerException;
import org.wso2.carbon.andes.service.exceptions.DestinationNotFoundException;
import org.wso2.carbon.andes.service.exceptions.InternalServerException;
import org.wso2.carbon.andes.service.exceptions.InvalidLimitValueException;
import org.wso2.carbon.andes.service.exceptions.InvalidOffsetValueException;
import org.wso2.carbon.andes.service.exceptions.MessageManagerException;
import org.wso2.carbon.andes.service.exceptions.MessageNotFoundException;
import org.wso2.carbon.andes.service.exceptions.SubscriptionManagerException;
import org.wso2.carbon.andes.service.managers.BrokerManagerService;
import org.wso2.carbon.andes.service.managers.DLCManagerService;
import org.wso2.carbon.andes.service.managers.DestinationManagerService;
import org.wso2.carbon.andes.service.managers.MessageManagerService;
import org.wso2.carbon.andes.service.managers.SubscriptionManagerService;
import org.wso2.carbon.andes.service.managers.bean.impl.BrokerManagerServiceBeanImpl;
import org.wso2.carbon.andes.service.managers.bean.impl.DLCManagerServiceBeanImpl;
import org.wso2.carbon.andes.service.managers.bean.impl.DestinationManagerServiceBeanImpl;
import org.wso2.carbon.andes.service.managers.bean.impl.MessageManagerServiceBeanImpl;
import org.wso2.carbon.andes.service.managers.bean.impl.SubscriptionManagerServiceBeanImpl;
import org.wso2.carbon.andes.service.managers.osgi.impl.BrokerManagerServiceOSGiImpl;
import org.wso2.carbon.andes.service.managers.osgi.impl.DLCManagerServiceOSGiImpl;
import org.wso2.carbon.andes.service.managers.osgi.impl.DestinationManagerServiceOSGiImpl;
import org.wso2.carbon.andes.service.managers.osgi.impl.MessageManagerServiceOSGiImpl;
import org.wso2.carbon.andes.service.managers.osgi.impl.SubscriptionManagerServiceOSGiImpl;
import org.wso2.carbon.andes.service.types.BrokerInformation;
import org.wso2.carbon.andes.service.types.ClusterInformation;
import org.wso2.carbon.andes.service.types.Destination;
import org.wso2.carbon.andes.service.types.DestinationRolePermission;
import org.wso2.carbon.andes.service.types.DestinationsContainer;
import org.wso2.carbon.andes.service.types.ErrorResponse;
import org.wso2.carbon.andes.service.types.Message;
import org.wso2.carbon.andes.service.types.NewDestination;
import org.wso2.carbon.andes.service.types.StoreInformation;
import org.wso2.carbon.andes.service.types.Subscription;
import org.wso2.msf4j.Microservice;
import org.wso2.msf4j.Request;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
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
        name = "org.wso2.carbon.andes.AndesService",
        service = Microservice.class,
        immediate = true,
        property = {
                "componentName=andes-rest-microservice"
        }
)

@SwaggerDefinition(
        info = @Info(
                title = "WSO2 Message Broker REST Service", version = "v1.0.0",
                description = "WSO2 Message Broker REST Service for managing resources such as destinations, " +
                              "permissions, messages and subscriptions.",
                license = @License(name = "Apache 2.0", url = "http://www.apache.org/licenses/LICENSE-2.0"),
                contact = @Contact(
                        name = "WSO2",
                        url = "http://wso2.com"
                )),
        tags = {@Tag(name = "Destinations", description = "Operations on handling destination related resources."),
                @Tag(name = "Permissions", description = "Operations on handling permission related resources."),
                @Tag(name = "Messages", description = "Operations on handling message related resources."),
                @Tag(name = "Subscriptions", description = "Operations on handling subscription related resources."),
                @Tag(name = "Broker Details", description = "Operations on getting broker details."),
                @Tag(name = "Dead Letter Channel", description = "Operations related to dead letter channel.")},
        schemes = SwaggerDefinition.Scheme.HTTP)
@Api(value = "mb/v1.0.0", description = "Endpoint to WSO2 message broker REST service.")
@Path("/mb/v1.0.0")
public class AndesRESTService implements Microservice {
    private static final Logger log = LoggerFactory.getLogger(AndesRESTService.class);
    /**
     * Bundle registration service for andes REST service.
     */
    private ServiceRegistration serviceRegistration;

    /**
     * Service class for managing destinations.
     */
    private DestinationManagerService destinationManagerService;

    /**
     * Service class for managing subscriptions.
     */
    private SubscriptionManagerService subscriptionManagerService;

    /**
     * Service class for managing message information.
     */
    private MessageManagerService messageManagerService;

    /**
     * Service class for managing broker details.
     */
    private BrokerManagerService brokerManagerService;

    /**
     * Service class for managing dead letter channel functions.
     */
    private DLCManagerService dlcManagerService;

    /**
     * The protocol for the dead letter channel.
     */
    private ProtocolType dlcProtocol;

    /**
     * Initializes the service classes for resources.
     */
    public AndesRESTService() throws AndesException {
        destinationManagerService = new DestinationManagerServiceBeanImpl();
        subscriptionManagerService = new SubscriptionManagerServiceBeanImpl();
        messageManagerService = new MessageManagerServiceBeanImpl();
        brokerManagerService = new BrokerManagerServiceBeanImpl();
        dlcManagerService = new DLCManagerServiceBeanImpl();

        // Setting DLC queue information
        dlcProtocol = new ProtocolType(AndesConstants.DLC_PROTOCOL_NAME, AndesConstants.DLC_PROTOCOL_VERSION);
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
     *     <li>{@link javax.ws.rs.core.Response.Status#OK} - Returns a collection of protocols as a
     *     response.</li>
     * </ul>
     */
    @GET
    @Path("/protocol-types")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Gets supported protocols.",
            notes = "Gets supported protocols by the broker.",
            tags = "Broker Details",
            response = String.class,
            responseContainer = "List")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "List of protocols.")})
    public Response getProtocols() throws InternalServerException {
        try {
            List<String> supportedProtocols = brokerManagerService.getSupportedProtocols();
            if (null == supportedProtocols) {
                supportedProtocols = new ArrayList<>();
            }
            return Response.status(Response.Status.OK).entity(supportedProtocols).build();
        } catch (BrokerManagerException e) {
            throw new InternalServerException(e);
        }
    }

    /**
     * Gets the destination types supported by the broker.
     * <p>
     * curl command :
     * <pre>
     *  curl -v http://127.0.0.1:9090/mb/v1.0.0/destination-types
     * </pre>
     *
     * @return Return a collection of supported destinations. <p>
     * <ul>
     *     <li>{@link javax.ws.rs.core.Response.Status#OK} - Returns a collection of destinations as a
     *     response.</li>
     * </ul>
     */
    @GET
    @Path("/destination-types")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Gets supported destinations.",
            notes = "Gets supported destinations by the broker.",
            tags = {"Broker Details", "Destinations"},
            response = String.class,
            responseContainer = "List")
    @ApiResponses(@ApiResponse(code = 200, message = "List of destinations."))
    public List<String> getDestinationTypes() {
        return Stream.of(DestinationType.values())
                .map(e -> e.name().toLowerCase())
                .collect(Collectors.toList());
    }

    /**
     * Gets destinations that belongs to a specific protocol and destination type.
     * <p>
     * curl command :
     * <pre>
     *  curl -v http://127.0.0.1:9090/mb/v1.0.0/amqp-0-91/destination-type/queue
     *  curl -v http://127.0.0.1:9090/mb/v1.0.0/amqp-0-91/destination-type/topic
     *  curl -v http://127.0.0.1:9090/mb/v1.0.0/amqp-0-91/destination-type/durable_topic
     *  curl -v http://127.0.0.1:9090/mb/v1.0.0/amqp-0-91/destination-type/queue?name=MyQueue&offset=5&limit=3
     * </pre>
     *
     * @param protocol        The protocol type of the destination as {@link ProtocolType}.
     * @param destinationType The destination type of the destination as {@link DestinationType}.
     * @param destinationName The name of the destination. If "*", all destinations are returned, else destinations that
     *                        <strong>contains</strong> the value will be returned.
     * @param offset          The starting index of the return destination list for pagination. Default value is 0.
     * @param limit           The number of destinations to return for pagination. Default value is 20.
     * @return Return a collection of {@link Destination}. <p>
     * <ul>
     *     <li>{@link javax.ws.rs.core.Response.Status#OK} - Returns a collection of {@link Destination} as a
     *     response.</li>
     *     <li>{@link javax.ws.rs.core.Response.Status#INTERNAL_SERVER_ERROR} - Error occurred when getting the
     *     destinations from the server.</li>
     * </ul>
     */
    @GET
    @Path("/{protocol}/destination-type/{destination-type}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Gets destinations.",
            notes = "Gets destinations that belongs to a specific protocol and destination type. Supports pagination.",
            tags = "Destinations",
            response = Destination.class,
            responseContainer = "List")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "A list of destinations.", response = DestinationsContainer.class),
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
            @Context Request request)
            throws InternalServerException, InvalidLimitValueException, InvalidOffsetValueException {
        try {
            if (offset < 0) {
                throw new InvalidOffsetValueException();
            }
            if (limit < 1) {
                throw new InvalidLimitValueException();
            }

            DestinationsContainer destinationsContainer = new DestinationsContainer();
            // Get total destination count
            destinationsContainer.setTotalDestinations(destinationManagerService.getDestinationNames(protocol,
                                                                            destinationType, destinationName).size());
            URIBuilder uriBuilder = new URIBuilder(request.getUri());
            // Next set calculation
            if (offset + limit < destinationsContainer.getTotalDestinations()) {
                destinationsContainer.setNext(uriBuilder
                        .removeQuery()
                        .setParameter("name", destinationName)
                        .setParameter("offset", Integer.toString(offset + limit))
                        .setParameter("limit", Integer.toString(Math.min(limit,
                                                                        destinationsContainer.getTotalDestinations())))
                        .build().toString());
            }

            // Previous set calculation
            if (offset - limit > 0) {
                destinationsContainer.setPrevious(uriBuilder
                        .removeQuery()
                        .setParameter("name", destinationName)
                        .setParameter("offset", Integer.toString(offset - limit))
                        .setParameter("limit", Integer.toString(limit))
                        .build().toString());
            } else {
                if (offset != 0) {
                    destinationsContainer.setPrevious(uriBuilder
                            .removeQuery()
                            .setParameter("name", destinationName)
                            .setParameter("offset", "0")
                            .setParameter("limit", Integer.toString(offset))
                            .build().toString());
                }
            }

            List<Destination> destinations = destinationManagerService.getDestinations(protocol, destinationType,
                    destinationName, offset, limit);
            destinationsContainer.setDestinations(destinations);
            return Response.status(Response.Status.OK).entity(destinationsContainer).build();
        } catch (DestinationManagerException | URISyntaxException e) {
            throw new InternalServerException(e);
        }
    }

    /**
     * Deletes all destinations belonging to a specific protocol and destination type.
     * <p>
     * curl command example :
     * <pre>
     *  curl -v -X DELETE http://127.0.0.1:9090/mb/v1.0.0/amqp-0-91/destination-type/queue
     *  curl -v -X DELETE http://127.0.0.1:9090/mb/v1.0.0/amqp-0-91/destination-type/topic
     *  curl -v -X DELETE http://127.0.0.1:9090/mb/v1.0.0/amqp-0-91/destination-type/durable_topic
     *  curl -v -X DELETE http://127.0.0.1:9090/mb/v1.0.0/mqtt-default/destination-type/topic
     * </pre>
     *
     * @param protocol        The protocol type of the destination as {@link ProtocolType}.
     * @param destinationType The destination type of the destination as {@link DestinationType}.
     * @return No response body. <p>
     * <ul>
     *     <li>{@link javax.ws.rs.core.Response.Status#OK} - Destinations were successfully deleted.</li>
     *     <li>{@link javax.ws.rs.core.Response.Status#INTERNAL_SERVER_ERROR} - Error occurred while deleting
     *     destinations from the broker.</li>
     * </ul>
     */
    @DELETE
    @Path("/{protocol}/destination-type/{destination-type}")
    @ApiOperation(
            value = "Deletes destinations.",
            notes = "Deletes destinations that belongs to a specific protocol and destination type.",
            tags = "Destinations")
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Destinations deleted.", response = Destination.class),
            @ApiResponse(code = 404, message = "Invalid protocol or destination type.", response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "Server Error.", response = ErrorResponse.class)})
    public Response deleteDestinations(
            @ApiParam(value = "Protocol for the destination")
            @PathParam("protocol") String protocol,
            @ApiParam(value = "Destination type for the destination")
            @PathParam("destination-type") String destinationType) throws InternalServerException {
        try {
            destinationManagerService.deleteDestinations(protocol, destinationType);
            return Response.status(Response.Status.NO_CONTENT).build();
        } catch (DestinationManagerException e) {
            throw new InternalServerException(e);
        }
    }

    /**
     * Gets a specific destination belonging to a specific protocol and destination type. Topic
     * <p>
     * curl command example :
     * <pre>
     *  curl -v http://127.0.0.1:9090/mb/v1.0.0/amqp-0-91/destination-type/queue/name/MyQueue
     *  curl -v http://127.0.0.1:9090/mb/v1.0.0/amqp-0-91/destination-type/topic/name/MyTopic
     *  curl -v http://127.0.0.1:9090/mb/v1.0.0/amqp-0-91/destination-type/durable_topic/name/MyDurable
     *  curl -v http://127.0.0.1:9090/mb/v1.0.0/mqtt-default/destination-type/topic/name/MyMQTTTopic
     * </pre>
     *
     * @param protocol        The protocol type of the destination as {@link ProtocolType}.
     * @param destinationType The destination type of the destination as {@link DestinationType}.
     * @param destinationName The name of the destination.
     * @return A JSON representation of {@link Destination}. <p>
     * <ul>
     *     <li>{@link javax.ws.rs.core.Response.Status#OK} - Returns a {@link Destination} as a JSON response.</li>
     *     <li>{@link javax.ws.rs.core.Response.Status#NOT_FOUND} - Such destination does not exists.</li>
     *     <li>{@link javax.ws.rs.core.Response.Status#INTERNAL_SERVER_ERROR} - Error occurred when getting the
     *     destination from the broker.</li>
     * </ul>
     */
    @GET
    @Path("/{protocol}/destination-type/{destination-type}/name/{destination-name}")
    @Produces(MediaType.APPLICATION_JSON)
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
     * Creates a new destination. A topic will be created even if "durable_topic" is requested as the destination type.
     * <p>
     * curl command example :
     * <pre>
     *  curl -v -X POST http://127.0.0.1:9090/mb/v1.0.0/amqp-0-91/destination-type/queue -d {"destinationName": "Q1"}
     * </pre>
     *
     * @param protocol        The protocol type of the destination as {@link ProtocolType}.
     * @param destinationType The destination type of the destination as {@link DestinationType}.
     *                        "durable_topic" is considered as a topic.
     * @param newDestination  A {@link NewDestination} object.
     * @return A JSON representation of the newly created {@link Destination}. <p>
     * <ul>
     *     <li>{@link javax.ws.rs.core.Response.Status#OK} - Returns a {@link Destination} as a JSON response.</li>
     *     <li>{@link javax.ws.rs.core.Response.Status#INTERNAL_SERVER_ERROR} - Error while creating new destination
     *     .</li>
     * </ul>
     */
    @POST
    @Path("/{protocol}/destination-type/{destination-type}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Creates a destination.",
            notes = "Creates a destination that belongs to a specific protocol and destination type.",
            tags = "Destinations",
            response = Destination.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "New destination successfully created.", response = Destination.class),
            @ApiResponse(code = 500, message = "Server error on creating destination", response = ErrorResponse.class)})
    public Response createDestination(
            @ApiParam(value = "Protocol for the destination.")
            @PathParam("protocol") String protocol,
            @ApiParam(value = "Destination type for the destination. \"durable_topic\" is considered as a topic.")
            @PathParam("destination-type") String destinationType,
            @ApiParam(value = "New destination object.")
            NewDestination newDestination,
            @Context Request request) throws InternalServerException {
        try {
            Destination destination = destinationManagerService.createDestination(protocol, destinationType,
                    newDestination.getDestinationName());

            return Response.status(Response.Status.OK)
                    .entity(newDestination)
                    .header(HttpHeaders.LOCATION, request.getUri() + "/name/" + destination.getDestinationName())
                    .build();
        } catch (DestinationManagerException e) {
            throw new InternalServerException(e);
        }
    }

    /**
     * Deletes destination. A topic will be deleted even if "durable_topic" is requested as the destination type.
     * <p>
     * curl command example :
     * <pre>
     *  curl -v -X DELETE http://127.0.0.1:9090/mb/v1.0.0/amqp-0-91/destination-type/queue/name/MyQueue
     *  curl -v -X DELETE http://127.0.0.1:9090/mb/v1.0.0/amqp-0-91/destination-type/topic/name/MyTopic
     *  curl -v -X DELETE http://127.0.0.1:9090/mb/v1.0.0/amqp-0-91/destination-type/durable_topic/name/MyDurable
     *  curl -v -X DELETE http://127.0.0.1:9090/mb/v1.0.0/mqtt-default/destination-type/topic/name/MyMQTTTopic
     * </pre>
     *
     * @param protocol        The protocol type of the destination as {@link ProtocolType}.
     * @param destinationType The destination type of the destination a {@link DestinationType}.
     *                        "durable_topic" is considered as a topic.
     * @param destinationName The name of the destination to delete.
     * @return No response body. <p>
     * <ul>
     *     <li>{@link javax.ws.rs.core.Response.Status#OK} - Destination was successfully deleted.</li>
     *     <li>{@link javax.ws.rs.core.Response.Status#INTERNAL_SERVER_ERROR} - Error occurred while deleting
     *     destination from the broker.</li>
     * </ul>
     */
    @DELETE
    @Path("/{protocol}/destination-type/{destination-type}/name/{destination-name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Deletes a destination.",
            notes = "Deletes a destination that belongs to a specific protocol and destination type.",
            tags = "Destinations")
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Destination deleted.", response = Destination.class),
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
            Destination destination = destinationManagerService.getDestination(protocol, destinationType,
                    destinationName);
            if (null != destination) {
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
     * Gets the permissions available for a destination.
     * <p>
     * curl command example :
     * <pre>
     *  curl -v http://127.0.0.1:9090/mb/v1.0.0/amqp-0-91/permissions/destination-type/queue/name/MyQueue
     *  curl -v http://127.0.0.1:9090/mb/v1.0.0/amqp-0-91/permissions/destination-type/topic/name/MyTopic
     *  curl -v http://127.0.0.1:9090/mb/v1.0.0/amqp-0-91/permissions/destination-type/durable_topic/name/MyDurable
     *  curl -v http://127.0.0.1:9090/mb/v1.0.0/mqtt-default/permissions/destination-type/topic/name/MyMQTTTopic
     * </pre>
     *
     * @param protocol        The protocol type of the destination as {@link ProtocolType}.
     * @param destinationType The destination type of the destination as {@link DestinationType}.
     *                        "durable_topic" is considered as a topic.
     * @param destinationName The name of the destination.
     * @return Return a collection of {@link DestinationRolePermission}. <p>
     * <ul>
     *     <li>{@link javax.ws.rs.core.Response.Status#OK} - Returns a collection of {@link DestinationRolePermission}
     *     as a JSON response.</li>
     *     <li>{@link javax.ws.rs.core.Response.Status#INTERNAL_SERVER_ERROR} - Error occurred when getting the
     *     permissions from the server.</li>
     * </ul>
     */
    @GET
    @Path("/{protocol}/permissions/destination-type/{destination-type}/name/{destination-name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Gets permission assigned to a destination.",
            notes = "Gets all the role based permissions assigned to a specific destination.",
            tags = {"Destinations", "Permissions"})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Gets list of permissions.", response = DestinationRolePermission.class),
            @ApiResponse(code = 404, message = "Protocol or destination type or destination is not found.",
                         response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "Server Error.", response = ErrorResponse.class)})
    public Response getDestinationPermissions(
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
                Set<DestinationRolePermission> permissions =
                        destinationManagerService.getDestinationPermissions(protocol, destinationType, destinationName);
                return Response.status(Response.Status.OK).entity(permissions).build();
            } else {
                throw new DestinationNotFoundException("Destination '" + destinationName + "' not found to get " +
                                                       "permissions.");
            }
        } catch (DestinationManagerException e) {
            throw new InternalServerException(e);
        }
    }

    /**
     * Creates the permission assigned to a roles.
     * <p>
     * curl command example :
     * <pre>
     *  curl -v -X POST \
     *          -H "Content-Type:application/json" \
     *          -d '{"role" : "abc", "consume" : true, "publish" : false}' \
     *          http://127.0.0.1:9090/mb/v1.0.0/amqp-0-91/permissions/destination-type/queue/name/MyQueue
     *
     * </pre>
     *
     * @param protocol                      The protocol type of the destination as
     *                                      {@link ProtocolType}.
     * @param destinationType               The destination type of the destination as
     *                                      {@link DestinationType}. "durable_topic" is considered
     *                                      as a topic.
     * @param destinationName               The name of the destination.
     * @param newDestinationRolePermissions The new permission assigned to the role.
     * @return Return the newly created {@link DestinationRolePermission}. <p>
     * <ul>
     *     <li>{@link javax.ws.rs.core.Response.Status#OK} - Returns a {@link DestinationRolePermission} as a JSON
     *     response.</li>
     *     <li>{@link javax.ws.rs.core.Response.Status#INTERNAL_SERVER_ERROR} - Error occurred when creating
     *     the permission from the server.</li>
     * </ul>
     */
    @POST
    @Path("/{protocol}/permissions/destination-type/{destination-type}/name/{destination-name}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Creates new role permissions.",
            notes = "Creates new role permissions for a destination.",
            tags = {"Destinations", "Permissions"})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "New permission created.", response = DestinationRolePermission.class),
            @ApiResponse(code = 404, message = "Protocol or destination type or destination is not found.",
                         response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "Server Error.", response = ErrorResponse.class)})
    public Response createDestinationPermission(
            @ApiParam(value = "Protocol for the destination.")
            @PathParam("protocol") String protocol,
            @ApiParam(value = "Destination type for the destination.")
            @PathParam("destination-type") String destinationType,
            @ApiParam(value = "The name of the destination.")
            @PathParam("destination-name") String destinationName,
            // Payload
            @ApiParam(value = "New role permission payload.")
            DestinationRolePermission newDestinationRolePermissions)
            throws DestinationNotFoundException, InternalServerException {
        try {
            Destination destination = destinationManagerService.getDestination(protocol, destinationType,
                    destinationName);
            if (null != destination) {
                DestinationRolePermission newPermission = destinationManagerService.createDestinationPermission
                        (protocol, destinationType, destinationName, newDestinationRolePermissions);
                return Response.status(Response.Status.OK).entity(newPermission).build();
            } else {
                throw new DestinationNotFoundException("Destination '" + destinationName + "' not found to get " +
                                                       "permissions.");
            }
        } catch (DestinationManagerException e) {
            throw new InternalServerException(e);
        }
    }

    /**
     * Updates the permission assigned to a roles.
     * <p>
     * curl command example :
     * <pre>
     *  curl -v -X PUT \
     *          -H "Content-Type:application/json" \
     *          -d '{"role" : "abc", "consume" : true, "publish" : false}' \
     *          http://127.0.0.1:9090/mb/v1.0.0/amqp-0-91/permissions/destination-type/queue/name/MyQueue
     *
     * </pre>
     *
     * @param protocol                          The protocol type of the destination as
     *                                          {@link ProtocolType}.
     * @param destinationType                   The destination type of the destination as
     *                                          {@link DestinationType}. "durable_topic" is
     *                                          considered as a topic.
     * @param destinationName                   The name of the destination.
     * @param updatedDestinationRolePermissions The updates permission assigned to the role.
     * @return Return the updated {@link DestinationRolePermission}. <p>
     * <ul>
     *     <li>{@link javax.ws.rs.core.Response.Status#OK} - Returns a {@link DestinationRolePermission} as a JSON
     *     response.</li>
     *     <li>{@link javax.ws.rs.core.Response.Status#INTERNAL_SERVER_ERROR} - Error occurred when updating
     *     the permission from the server.</li>
     * </ul>
     */
    @PUT
    @Path("/{protocol}/permissions/destination-type/{destination-type}/name/{destination-name}")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Updates role permissions.",
            notes = "Updates role permissions for a destination.",
            tags = {"Destinations", "Permissions"})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Permission updated.", response = DestinationRolePermission.class),
            @ApiResponse(code = 404, message = "Protocol or destination type or destination is not found.",
                         response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "Server Error.", response = ErrorResponse.class)})
    public Response updateDestinationPermission(
            @ApiParam(value = "Protocol for the destination.")
            @PathParam("protocol") String protocol,
            @ApiParam(value = "Destination type for the destination.")
            @PathParam("destination-type") String destinationType,
            @ApiParam(value = "The name of the destination.")
            @PathParam("destination-name") String destinationName,
            // Payload
            @ApiParam(value = "New role permission payload.")
            DestinationRolePermission updatedDestinationRolePermissions)
            throws InternalServerException, DestinationNotFoundException {
        try {
            Destination destination = destinationManagerService.getDestination(protocol, destinationType,
                    destinationName);
            if (null != destination) {
                DestinationRolePermission updatedPermission = destinationManagerService.updateDestinationPermission
                        (protocol, destinationType, destinationName, updatedDestinationRolePermissions);
                return Response.status(Response.Status.OK).entity(updatedPermission).build();
            } else {
                throw new DestinationNotFoundException("Destination '" + destinationName + "' not found to get " +
                                                       "permissions.");
            }
        } catch (DestinationManagerException e) {
            throw new InternalServerException(e);
        }
    }

    /**
     * Gets the DLC queue for the current tenant.
     * <p>
     * curl command example :
     * <pre>
     *  curl -v http://127.0.0.1:9090/mb/v1.0.0/dlc
     * </pre>
     *
     * @return A JSON representation of {@link Destination}. <p>
     * <ul>
     *     <li>{@link javax.ws.rs.core.Response.Status#OK} - Returns a {@link Destination} as a JSON response.</li>
     *     <li>{@link javax.ws.rs.core.Response.Status#NOT_FOUND} - Such destination does not exists.</li>
     *     <li>{@link javax.ws.rs.core.Response.Status#INTERNAL_SERVER_ERROR} - Error occurred when getting the
     *     destination from the broker.</li>
     * </ul>
     */
    @GET
    @Path("/dlc")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Gets DLC queue.",
            notes = "Gets the DLC queue for the current user.",
            tags = "Destinations")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Destination returned.", response = Destination.class),
            @ApiResponse(code = 400, message = "Invalid protocol or destination type.", response = ErrorResponse.class),
            @ApiResponse(code = 404, message = "Destination not found.", response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "Server Error.", response = ErrorResponse.class)})
    public Response getDLCQueue() throws InternalServerException {
        try {
            //TODO : get queue name for tenant.
            String dlcQueueName = DLCQueueUtils.generateDLCQueueNameFromTenant("carbon.super");
            Destination dlcQueue = destinationManagerService.getDestination(dlcProtocol.toString(),
                                                        AndesConstants.DLC_DESTINATION_TYPE.toString(), dlcQueueName);
            if (null != dlcQueue) {
                return Response.status(Response.Status.OK).entity(dlcQueue).build();
            } else {
                return Response.status(Response.Status.NOT_FOUND).build();
            }
        } catch (DestinationManagerException e) {
            throw new InternalServerException(e);
        }
    }

    /**
     * Gets messages from a dead letter channel using a message ID as the offset.
     * <p>
     * curl command example :
     * <pre>
     *  curl -v -X GET http://127.0.0.1:9090/mb/v1.0.0/dlc/DeadLetterChannel/messages?content=true
     *  curl -v -X GET http://127.0.0.1:9090/mb/v1.0.0/dlc/DeadLetterChannel/messages
     *  curl -v -X GET http://127.0.0.1:9090/mb/v1.0.0/dlc/DeadLetterChannel/messages?next-message-id=1234&limit=10
     * </pre>
     *
     * @param dlcQueueName    The DLC queue name.
     * @param content         Whether to return message content or not.
     * @param nextMessageID   The starting message ID to return from.
     * @param limit           The number of messages to return.
     * @return Return a collection of {@link Message}s. <p>
     * <ul>
     *     <li>{@link javax.ws.rs.core.Response.Status#OK} - Returns a collection of {@link Message}s
     *     as a JSON response.</li>
     *     <li>{@link javax.ws.rs.core.Response.Status#INTERNAL_SERVER_ERROR} - Error occurred when getting the
     *     messages from the server.</li>
     * </ul>
     */
    @GET
    @Path("/dlc/{dlc-queue-name}/messages")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Gets DLC messages.",
            notes = "Gets messages of a dead letter channel using message IDs.",
            tags = {"Dead Letter Channel", "Messages"},
            response = Message.class,
            responseContainer = "List")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful list of messages.", response = Message.class),
            @ApiResponse(code = 404, message = "DLC not found.", response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "Server Error.", response = ErrorResponse.class)})
    public Response getDLCMessagesByMessageID(
            @ApiParam(value = "The DLC queue name.")
            @PathParam("dlc-queue-name") String dlcQueueName,
            @ApiParam(value = "Whether to return message content or not.", allowableValues = "[true, false]")
            @DefaultValue("false") @QueryParam("content") boolean content,
            @ApiParam(value = "The starting message ID to return from.")
            @DefaultValue("0") @QueryParam("next-message-id") long nextMessageID,
            @ApiParam(value = "The number of messages to return for pagination.",
                      allowableValues = "range[1, infinity]")
            @DefaultValue("100") @QueryParam("limit") int limit) throws InternalServerException {
        try {
            List<Message> messages = messageManagerService.getMessagesOfDestinationByMessageID(dlcProtocol.toString(),
                    AndesConstants.DLC_DESTINATION_TYPE.toString(), dlcQueueName, content, nextMessageID, limit);
            return Response.status(Response.Status.OK).entity(messages).build();
        } catch (MessageManagerException e) {
            throw new InternalServerException(e);
        }
    }

    /**
     * Restore or reroute messages from DLC to a queue or same queue which originated.
     * <p>
     * curl command example :
     * <pre>
     *  curl -v -X POST http://127.0.0.1:9090/mb/v1.0.0/dlc/DeadLetterChannel/messages/restore
     *  curl -v -X POST http://127.0.0.1:9090/mb/v1.0.0/dlc/DeadLetterChannel/messages/restore?new-queue-name=MyQueue
     * </pre>
     *
     * @param dlcQueueName       The DLC queue name.
     * @param newDestinationName The name of the new queue to which the messages to be rerouted. If "" is received,
     *                           messages will be restored to the same queue.
     * @param andesMessageIDs    The andes message metadata IDs of the messages.
     * @return No response body. <p>
     * <ul>
     *     <li>{@link javax.ws.rs.core.Response.Status#OK} - When messages are successfully restored/rerouted.</li>
     *     <li>{@link javax.ws.rs.core.Response.Status#INTERNAL_SERVER_ERROR} - Error occurred when rerouting/restoring
     *     messages from the server.</li>
     * </ul>
     */
    @POST
    @Path("/dlc/{dlc-queue-name}/messages/restore")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Reroute or Restores messages from DLC.",
            notes = "Reroute or Restores messages from DLC with a given list of message IDs.",
            tags = {"Dead Letter Channel", "Messages"})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Messages successfully rerouted/restored..", response = Message.class),
            @ApiResponse(code = 500, message = "Server Error.", response = ErrorResponse.class)})
    public Response restoreMessagesFromDeadLetterQueue(
            @ApiParam(value = "The DLC queue name.")
            @PathParam("dlc-queue-name") String dlcQueueName,
            // Payload
            @ApiParam(value = "List of message IDs to restore.")
            List<Long> andesMessageIDs,
            @ApiParam(value = "The new queue name to redirect messages.")
            @DefaultValue("") @QueryParam("new-queue-name") String newDestinationName) {
        if (StringUtils.isEmpty(newDestinationName)) {
            dlcManagerService.restoreMessagesFromDeadLetterQueue(andesMessageIDs, dlcQueueName);
        } else {
            dlcManagerService.restoreMessagesFromDeadLetterQueue(andesMessageIDs, newDestinationName, dlcQueueName);
        }
        return Response.status(Response.Status.OK).build();
    }

    /**
     * Deletes messages from DLC with given list of message IDs.
     * <p>
     * curl command example :
     * <pre>
     *  curl -v -X DELETE http://127.0.0.1:9090/mb/v1.0.0/dlc/DeadLetterChannel/messages
     * </pre>
     *
     * @param dlcQueueName    The DLC queue name.
     * @param andesMessageIDs Message IDs to delete.
     * @return No response body. <p>
     * <ul>
     *     <li>{@link javax.ws.rs.core.Response.Status#OK} - If messages were successfully deleted.</li>
     *     <li>{@link javax.ws.rs.core.Response.Status#INTERNAL_SERVER_ERROR} - Error occurred when deleting dlc
     *     messages from the server.</li>
     * </ul>
     */
    @DELETE
    @Path("/dlc/{dlc-queue-name}/messages")
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Deletes messages from DLC.",
            notes = "Deletes messages from DLC with given list of message IDs.",
            tags = {"Dead Letter Channel", "Messages"})
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Messages were successfully deleted.", response = Message.class),
            @ApiResponse(code = 404, message = "DLC queue not found.", response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "Server Error.", response = ErrorResponse.class)})
    public Response deleteMessagesFromDeadLetterQueue(
            @ApiParam(value = "The DLC queue name.")
            @PathParam("dlc-queue-name") String dlcQueueName,
            // Payload
            @ApiParam(value = "List of message IDs to delete.")
            List<Long> andesMessageIDs) {
        dlcManagerService.deleteMessagesFromDeadLetterQueue(andesMessageIDs, dlcQueueName);
        return Response.status(Response.Status.OK).build();
    }

    /**
     * Gets subscriptions belonging to a specific protocol type and destination type. The subscriptions can be filtered
     * by subscription name, destination name and whether they are active or not.
     * <p>
     * curl command example :
     * <pre>
     *  curl -v http://127.0.0.1:9090/mb/v1.0.0/amqp-0-91/subscription-type/queue?destination=MyQueue&offset=2&limit=5
     *  curl -v http://127.0.0.1:9090/mb/v1.0.0/amqp-0-91/subscription-type/topic
     *  curl -v http://127.0.0.1:9090/mb/v1.0.0/amqp-0-91/subscription-type/durable_topic?active=true
     *  curl -v http://127.0.0.1:9090/mb/v1.0.0/amqp-0-91/subscription-type/durable_topic?active=false&name=subID01
     * </pre>
     *
     * @param protocol         The protocol type matching for the subscription.
     * @param subscriptionType The destination type matching for the subscription. Example : queue, topic,
     *                         durable_topic.
     * @param subscriptionName The name of the subscription. If "*", all subscriptions are included. Else subscriptions
     *                         that <strong>contains</strong> the value are included.
     * @param destinationName  The name of the destination name. If "*", all destinations are included. Else
     *                         destinations that <strong>equals</strong> the value are included.
     * @param active           Filtering the subscriptions that are active or inactive. Supported values = "*", "true"
     *                         and "false".
     * @param offset           The starting index to return.
     * @param limit            The number of subscriptions to return.
     * @return Return a collection of {@link Subscription}. <p>
     * <ul>
     *     <li>{@link javax.ws.rs.core.Response.Status#OK} - Returns a collection of {@link Subscription}
     *     as a JSON response.</li>
     *     <li>{@link javax.ws.rs.core.Response.Status#INTERNAL_SERVER_ERROR} - Error occurred when getting the
     *     subscriptions from the server.</li>
     * </ul>
     */
    @GET
    @Path("/{protocol}/subscription-type/{subscription-type}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Get subscriptions.",
            notes = "Get subscriptions that belongs to a specific protocol and subscription type. Supports pagination.",
            tags = "Subscriptions")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successfully gets subscriptions.", response = Subscription.class),
            @ApiResponse(code = 400, message = "Invalid protocol or subscription type.",
                         response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "Server Error.", response = ErrorResponse.class)})
    public Response getSubscriptions(
            @ApiParam(value = "Protocol for the subscription.")
            @PathParam("protocol") String protocol,
            @ApiParam(value = "The type of subscription.")
            @PathParam("subscription-type") String subscriptionType,
            @ApiParam(value = "The name of the subscription. If \"*\", all subscriptions are included. Else " +
                              "subscriptions that CONTAINS the value are included.")
            @DefaultValue("*") @QueryParam("name") String subscriptionName,
            @ApiParam(value = "The name of the destination name. If \"*\", all destinations are included. Else" +
                              "destinations that EQUALS the value are included.")
            @DefaultValue("*") @QueryParam("destination") String destinationName,
            @ApiParam(value = "Filtering the subscriptions that are active or inactive.",
                      allowableValues = "[*, true, false]")
            @DefaultValue("*") @QueryParam("active") String active,
            @ApiParam(value = "The starting index of the return destination list for pagination.",
                      allowableValues = "range[1, infinity]")
            @DefaultValue("0") @QueryParam("offset") int offset,
            @ApiParam(value = "The number of destinations to return for pagination.",
                      allowableValues = "range[1, infinity]")
            @DefaultValue("20") @QueryParam("limit") int limit) throws InternalServerException {
        try {
            List<Subscription> subscriptions = subscriptionManagerService.getSubscriptions
                    (protocol, subscriptionType, subscriptionName, destinationName, active, offset, limit);
            return Response.status(Response.Status.OK).entity(subscriptions).build();
        } catch (SubscriptionManagerException e) {
            throw new InternalServerException(e);
        }
    }

    /**
     * Close/Unsubscribe subscriptions forcefully belonging to a specific protocol type, destination type.
     * <p>
     * curl command example :
     * <pre>
     *  curl -X DELETE http://127.0.0.1:9090/mb/v1.0.0/amqp-0-91/subscription-type/queue
     *  curl -X DELETE http://127.0.0.1:9090/mb/v1.0.0/amqp-0-91/subscription-type/durable_topic?unsubscribe-only=true
     * </pre>
     *
     * @param protocol         The protocol type matching for the subscription.
     * @param subscriptionType The subscription type matching for the subscription. Example : queue, topic,
     *                         durable_topic.
     * @param destinationName  The name of the destination to close/unsubscribe. If "*", all destinations are included.
     *                         Else destinations that <strong>contains</strong> the value are included.
     * @return No response body. <p>
     * <ul>
     *     <li>{@link javax.ws.rs.core.Response.Status#OK} - Subscriptions were successfully closed/disconnected.</li>
     *     <li>{@link javax.ws.rs.core.Response.Status#INTERNAL_SERVER_ERROR} - Error occurred while
     *     closing/disconnecting a subscriptions from the broker.</li>
     * </ul>
     */
    @DELETE
    @Path("/{protocol}/subscription-type/{subscription-type}")
    @ApiOperation(
            value = "Close subscriptions.",
            notes = "Closes subscriptions that belongs to a specific protocol and subscription type.",
            tags = "Subscriptions")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Subscriptions successfully close.", response = Subscription.class),
        @ApiResponse(code = 400, message = "Invalid protocol or subscription type.", response = ErrorResponse.class),
        @ApiResponse(code = 404, message = "Destinations not found.", response = ErrorResponse.class),
        @ApiResponse(code = 500, message = "Server Error.", response = ErrorResponse.class)})
    public Response closeSubscriptions(
            @ApiParam(value = "Protocol for the subscription.")
            @PathParam("protocol") String protocol,
            @ApiParam(value = "The type of subscription.")
            @PathParam("subscription-type") String subscriptionType,
            @ApiParam(value = "The name of the destination to close/unsubscribe. If \"*\", all destinations are " +
                              "included. Else destinations that CONTAINS the value are included.")
            @DefaultValue("*") @QueryParam("destination") String destinationName,
            @DefaultValue("false") @QueryParam("unsubscribe-only") boolean unsubscribeOnly)
            throws InternalServerException {
        try {
            subscriptionManagerService.closeSubscriptions(protocol, subscriptionType, destinationName, unsubscribeOnly);
            return Response.status(Response.Status.OK).build();
        } catch (SubscriptionManagerException e) {
            throw new InternalServerException(e);
        }
    }

    /**
     * Close/Unsubscribe subscription forcefully belonging to a specific protocol type, destination type.
     * <p>
     * curl command example :
     * <pre>
     *  curl -v -X DELETE http://127.0.0.1:9090/mb/v1.0.0/amqp-0-91/subscription-type/queue/subscription-id/sub1
     *  curl -v -X DELETE \
     *      http://127.0.0.1:9090/mb/v1.0.0/amqp-0-91/subscription-type/queue/subscription-id/sub2?unsubscribe-only=true
     *  curl -v -X DELETE http://127.0.0.1:9090/mb/v1.0.0/amqp-0-91/subscription-type/durable_topic/subscription-id/sub1
     * </pre>
     *
     * @param protocol         The protocol type matching for the subscription.
     * @param subscriptionType The subscription type matching for the subscription. Example : queue, topic,
     *                         durable_topic.
     * @param subscriptionID  The subscription ID to close/unsubscribe.
     * @return No response body. <p>
     * <ul>
     *     <li>{@link javax.ws.rs.core.Response.Status#OK} - Subscription was successfully closed/disconnected.</li>
     *     <li>{@link javax.ws.rs.core.Response.Status#NOT_FOUND} - Subscription was not found to close/disconnect.</li>
     *     <li>{@link javax.ws.rs.core.Response.Status#INTERNAL_SERVER_ERROR} - Error occurred while
     *     closing/disconnecting a subscriptions from the broker.</li>
     * </ul>
     */
    @DELETE
    @Path("/{protocol}/subscription-type/{subscription-type}/subscription-id/{subscription-id}")
    @ApiOperation(
            value = "Close subscription.",
            notes = "Closes subscription that belongs to a specific protocol and subscription type.",
            tags = "Subscriptions")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "Subscription successfully close.", response = Subscription.class),
        @ApiResponse(code = 400, message = "Invalid protocol or subscription type.", response = ErrorResponse.class),
        @ApiResponse(code = 404, message = "Subscription not found.", response = ErrorResponse.class),
        @ApiResponse(code = 500, message = "Server Error.", response = ErrorResponse.class)})
    public Response closeSubscription(
            @ApiParam(value = "Protocol for the subscription.")
            @PathParam("protocol") String protocol,
            @ApiParam(value = "The type of subscription.")
            @PathParam("subscription-type") String subscriptionType,
            @ApiParam(value = "The subscription ID.")
            @PathParam("subscription-id") String subscriptionID,
            @DefaultValue("false") @QueryParam("unsubscribe-only") boolean unsubscribeOnly)
            throws InternalServerException {
        try {
            subscriptionManagerService.closeSubscription(protocol, subscriptionType, subscriptionID,
                    unsubscribeOnly);
            return Response.status(Response.Status.OK).build();
        } catch (SubscriptionManagerException e) {
            throw new InternalServerException(e);
        }
    }

    /**
     * Browse message of a destination using message ID.
     * curl command example :
     * <pre>
     *  curl http://127.0.0.1:9090/mb/v1.0.0/amqp-0-91/destination-type/queue/name/MyQueue/messages?content=true
     *  curl http://127.0.0.1:9090/mb/v1.0.0/amqp-0-91/destination-type/queue/name/mq1/messages?next-message-id=12345678
     *  curl \
     *     http://127.0.0.1:9090/mb/v1.0.0/amqp-0-91/destination-type/durable_topic/name/carbon:MySub/messages?limit=100
     * </pre>
     *
     * @param protocol        The protocol type matching for the message.
     * @param destinationType The destination type matching for the message.
     * @param destinationName The name of the destination.
     * @param content         Whether to return message content or not.
     * @param nextMessageID   The starting message ID to return from.
     * @param limit           The number of messages to return.
     * @return Return a collection of {@link Message}s. <p>
     * <ul>
     *     <li>{@link javax.ws.rs.core.Response.Status#OK} - Returns a collection of {@link Message}s
     *     as a JSON response.</li>
     *     <li>{@link javax.ws.rs.core.Response.Status#NOT_FOUND} - Invalid protocol or destination type. Destination
     *     does not exists.</li>
     *     <li>{@link javax.ws.rs.core.Response.Status#INTERNAL_SERVER_ERROR} - Error occurred when getting the
     *     messages from the server.</li>
     * </ul>
     */
    @GET
    @Path("/{protocol}/destination-type/{destination-type}/name/{destination-name}/messages")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Gets message by message ID.",
            notes = "Gets message that belongs to a specific protocol,destination type and destination name. " +
                    "Supports pagination.",
            tags = "Messages",
            response = Message.class,
            responseContainer = "List")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful list of messages.", response = Message.class),
            @ApiResponse(code = 404, message = "Invalid protocol or destination type or destination not found.",
                         response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "Server Error.", response = ErrorResponse.class)})
    public Response getMessagesOfDestination(
            @ApiParam(value = "Protocol for the message.")
            @PathParam("protocol") String protocol,
            @ApiParam(value = "Destination type for the message.")
            @PathParam("destination-type") String destinationType,
            @ApiParam(value = "The name of the destination of the message.")
            @PathParam("destination-name") String destinationName,
            @ApiParam(value = "Whether to return message content or not.", allowableValues = "[true, false]")
            @DefaultValue("false") @QueryParam("content") boolean content,
            @ApiParam(value = "The starting message ID to return from.")
            @DefaultValue("0") @QueryParam("next-message-id") long nextMessageID,
            @ApiParam(value = "The number of messages to return for pagination.",
                      allowableValues = "range[1, infinity]")
            @DefaultValue("100") @QueryParam("limit") int limit)
            throws DestinationNotFoundException, InternalServerException {
        try {
            Destination destination = destinationManagerService.getDestination(protocol, destinationType,
                    destinationName);
            if (null != destination) {
                List<Message> messages = messageManagerService.getMessagesOfDestinationByMessageID(protocol,
                        destinationType, destinationName, content, nextMessageID, limit);
                return Response.status(Response.Status.OK).entity(messages).build();
            } else {
                throw new DestinationNotFoundException("Destination '" + destinationName + "' not found to " +
                                                       "browse messages.");
            }
        } catch (DestinationManagerException | MessageManagerException e) {
            throw new InternalServerException(e);
        }
    }

    /**
     * Gets a message by message ID belonging to a particular protocol, destination type and destination name.
     * <p>
     * curl command example :
     * <pre>
     *  curl -v http://127.0.0.1:9090/mb/v1.0.0/amqp-0-91/destination-type/queue/name/MyQueue/messages/12345678
     *  curl \
     *      http://127.0.0.1:9090/mb/v1.0.0/amqp-0-91/destination-type/durable_topic/name/carbon:MySub/messages/9876543
     * </pre>
     *
     * @param protocol        The protocol type matching for the message.
     * @param destinationType The destination type matching for the message.
     * @param destinationName The name of the destination to which the message belongs to.
     * @param andesMessageID  The message ID. This message is the andes metadata message ID.
     * @param content         Whether to return content or not.
     * @return A JSON representation of {@link Message}. <p>
     * <ul>
     *     <li>{@link javax.ws.rs.core.Response.Status#OK} - Returns a {@link Message} as a JSON response.</li>
     *     <li>{@link javax.ws.rs.core.Response.Status#NOT_FOUND} - Invalid protocol, destination type or destination
     *     name, Message not found.</li>
     *     <li>{@link javax.ws.rs.core.Response.Status#INTERNAL_SERVER_ERROR} - Error occurred when getting the
     *     message from the broker.</li>
     * </ul>
     */
    @GET
    @Path("/{protocol}/destination-type/{destination-type}/name/{destination-name}/messages/{message-id}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Gets a message.",
            notes = "Gets a message using message ID.",
            tags = "Messages",
            response = Message.class,
            responseContainer = "List")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successful received message.", response = Message.class),
            @ApiResponse(code = 404,
                         message = "Invalid protocol, destination type or destination name, Message not found.",
                         response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "Server Error.", response = ErrorResponse.class)})
    public Response getMessage(
            @ApiParam(value = "Protocol for the message.")
            @PathParam("protocol") String protocol,
            @ApiParam(value = "Destination type for the message.")
            @PathParam("destination-type") String destinationType,
            @ApiParam(value = "The name of the destination of the message.")
            @PathParam("destination-name") String destinationName,
            @ApiParam(value = "The andes message ID.")
            @PathParam("message-id") long andesMessageID,
            @ApiParam(value = "Whether to return message content or not.", allowableValues = "[true, false]")
            @DefaultValue("false") @QueryParam("content") boolean content)
            throws DestinationNotFoundException, InternalServerException, MessageNotFoundException {
        try {
            Destination destination = destinationManagerService.getDestination(protocol, destinationType,
                    destinationName);
            if (null != destination) {
                Message message = messageManagerService.getMessage(protocol, destinationType, destinationName,
                        andesMessageID, content);
                if (null != message) {
                    return Response.status(Response.Status.OK).entity(message).build();
                } else {
                    throw new MessageNotFoundException("Message with '" + Long.toString(andesMessageID) + "'");
                }
            } else {
                throw new DestinationNotFoundException("Destination with '" + destinationName + "' could not be found" +
                                                       " to find message '" + Long.toString(andesMessageID) + "'.");
            }
        } catch (DestinationManagerException | MessageManagerException exception) {
            throw new InternalServerException(exception);
        }
    }

    /**
     * Purge all messages belonging to a destination.
     * <p>
     * curl command example :
     * <pre>
     *  curl -X DELETE http://127.0.0.1:9090/mb/v1.0.0/amqp-0-91/destination-type/queue/name/MyQueue/messages
     *  curl -X DELETE http://127.0.0.1:9090/mb/v1.0.0/amqp-0-91/destination-type/durable_topic/name/carbon:Sub/messages
     * </pre>
     *
     * @param protocol        The protocol type matching for the message.
     * @param destinationType The destination type matching for the message.
     * @param destinationName The name of the destination to purge messages.
     * @return No response body. <p>
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
            @ApiResponse(code = 204, message = "Messages purged successfully.", response = Message.class),
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
            Destination destination = destinationManagerService.getDestination(protocol, destinationType,
                    destinationName);
            if (null != destination) {
                messageManagerService.deleteMessages(protocol, destinationType, destinationName);
                return Response.status(Response.Status.NO_CONTENT).build();
            } else {
                throw new DestinationNotFoundException("Destination '" + destinationName + "' not found to " +
                                                       "delete/purge messages.");
            }
        } catch (DestinationManagerException | MessageManagerException exception) {
            throw new InternalServerException(exception);
        }
    }

    /**
     * Gets clustering related information of the broker.
     * <p>
     * curl command :
     * <pre>
     *  curl -v http://127.0.0.1:9090/mb/v1.0.0/information/cluster
     * </pre>
     *
     * @return Return a {@link ClusterInformation}. <p>
     * <ul>
     *      <li>{@link javax.ws.rs.core.Response.Status#OK} - Returns a {@link ClusterInformation} as a response.</li>
     *      <li>{@link javax.ws.rs.core.Response.Status#NOT_FOUND} - Such destination does not exists.</li>
     *      <li>{@link javax.ws.rs.core.Response.Status#INTERNAL_SERVER_ERROR} - Error occurred when getting the
     *      clustering details from the server.</li>
     * </ul>
     */
    @GET
    @Path("/information/cluster")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Gets cluster details.",
            notes = "Gets cluster details which includes node details as well..",
            tags = "Node Details")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successfully received cluster information.",
                         response = ClusterInformation.class),
            @ApiResponse(code = 404, message = "Clustering information not found.", response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "Server Error.", response = ErrorResponse.class)})
    public Response getClusterInformation() throws InternalServerException {
        try {
            ClusterInformation clusterInformation = brokerManagerService.getClusterInformation();
            if (null != clusterInformation) {
                return Response.status(Response.Status.OK).entity(clusterInformation).build();
            } else {
                throw new BrokerManagerException("Unable to find cluster information.");
            }
        } catch (BrokerManagerException e) {
            throw new InternalServerException(e);
        }
    }

    /**
     * Gets message store related information of the broker.
     * <p>
     * curl command :
     * <pre>
     *  curl -v http://127.0.0.1:9090/mb/v1.0.0/information/store
     * </pre>
     *
     * @return Return a {@link StoreInformation}. <p>
     * <ul>
     *      <li>{@link javax.ws.rs.core.Response.Status#OK} - Returns a {@link StoreInformation} as a response.</li>
     *      <li>{@link javax.ws.rs.core.Response.Status#NOT_FOUND} - Such destination does not exists.</li>
     *      <li>{@link javax.ws.rs.core.Response.Status#INTERNAL_SERVER_ERROR} - Error occurred when getting the
     *      message store details from the server.</li>
     * </ul>
     */
    @GET
    @Path("/information/store")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Gets store details.",
            notes = "Gets message store details.",
            tags = "Node Details")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successfully received store information.",
                         response = StoreInformation.class),
            @ApiResponse(code = 404, message = "Store information not found.", response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "Server Error.", response = ErrorResponse.class)})
    public Response getStoreInformation() throws InternalServerException {
        try {
            StoreInformation storeInformation = brokerManagerService.getStoreInformation();
            if (null != storeInformation) {
                return Response.status(Response.Status.OK).entity(storeInformation).build();
            } else {
                throw new BrokerManagerException("Unable to find store information.");
            }
        } catch (BrokerManagerException e) {
            throw new InternalServerException(e);
        }
    }

    /**
     * Gets broker configuration related information.
     * <p>
     * curl command :
     * <pre>
     *  curl -v http://127.0.0.1:9090/mb/v1.0.0/information/broker
     * </pre>
     *
     * @return Return a {@link BrokerInformation}. <p>
     * <ul>
     *      <li>{@link javax.ws.rs.core.Response.Status#OK} - Returns a {@link BrokerInformation} as a response.</li>
     *      <li>{@link javax.ws.rs.core.Response.Status#NOT_FOUND} - Such destination does not exists.</li>
     *      <li>{@link javax.ws.rs.core.Response.Status#INTERNAL_SERVER_ERROR} - Error occurred when getting the
     *      broker details from the server.</li>
     * </ul>
     */
    @GET
    @Path("/information/broker")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
            value = "Gets broker properties.",
            notes = "Gets current broker node details.",
            tags = "Node Details")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successfully received broker information.",
                         response = BrokerInformation.class),
            @ApiResponse(code = 404, message = "Broker properties not found.", response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "Server Error.", response = ErrorResponse.class)})
    public Response getBrokerInformation() throws InternalServerException {
        try {
            BrokerInformation brokerInformation = brokerManagerService.getBrokerInformation();
            if (null != brokerInformation) {
                return Response.status(Response.Status.OK).entity(brokerInformation).build();
            } else {
                throw new BrokerManagerException("Unable to find broker information.");
            }
        } catch (BrokerManagerException e) {
            throw new InternalServerException(e);
        }
    }

    /**
     * Called when the bundle is activated.
     *
     * @param bundleContext Bundle of the component.
     */
    @Activate
    protected void start(BundleContext bundleContext) {
        serviceRegistration = bundleContext.registerService(AndesRESTService.class.getName(), this, null);
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

    /**
     * This bind method will be called when CarbonRuntime OSGi service is registered.
     *
     * @param andesInstance The CarbonRuntime instance registered by Carbon Kernel as an OSGi service
     */
    @Reference(
            name = "carbon.andes.service",
            service = Andes.class,
            cardinality = ReferenceCardinality.OPTIONAL,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetAndesRuntime"
    )
    protected void setAndesRuntime(Andes andesInstance) {
        AndesRESTComponentDataHolder.getInstance().setAndesInstance(andesInstance);
        destinationManagerService = new DestinationManagerServiceOSGiImpl();
        subscriptionManagerService = new SubscriptionManagerServiceOSGiImpl();
        messageManagerService = new MessageManagerServiceOSGiImpl();
        brokerManagerService = new BrokerManagerServiceOSGiImpl();
        dlcManagerService = new DLCManagerServiceOSGiImpl();
    }

    /**
     * This is the unbind method which gets called at the un-registration of CarbonRuntime OSGi service.
     *
     * @param andesInstance The CarbonRuntime instance registered by Carbon Kernel as an OSGi service
     */
    @SuppressWarnings("unused")
    protected void unsetAndesRuntime(Andes andesInstance) {
        AndesRESTComponentDataHolder.getInstance().setAndesInstance(null);
        // Setting bean implementations for services.
        destinationManagerService = new DestinationManagerServiceBeanImpl();
        subscriptionManagerService = new SubscriptionManagerServiceBeanImpl();
        messageManagerService = new MessageManagerServiceBeanImpl();
        brokerManagerService = new BrokerManagerServiceBeanImpl();
        dlcManagerService = new DLCManagerServiceBeanImpl();
    }

    public void setDestinationManagerService(DestinationManagerService destinationManagerService) {
        this.destinationManagerService = destinationManagerService;
    }

    public void setBrokerManagerService(BrokerManagerService brokerManagerService) {
        this.brokerManagerService = brokerManagerService;
    }

    public void setMessageManagerService(MessageManagerService messageManagerService) {
        this.messageManagerService = messageManagerService;
    }
}
