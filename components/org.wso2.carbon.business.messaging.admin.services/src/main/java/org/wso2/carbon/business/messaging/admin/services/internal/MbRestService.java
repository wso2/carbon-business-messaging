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
import org.wso2.andes.configuration.AndesConfigurationManager;
import org.wso2.andes.configuration.enums.AndesConfiguration;
import org.wso2.andes.kernel.Andes;
import org.wso2.andes.kernel.AndesConstants;
import org.wso2.andes.kernel.AndesMessage;
import org.wso2.andes.kernel.AndesMessageMetadata;
import org.wso2.carbon.business.messaging.admin.services.exceptions.BadRequestException;
import org.wso2.carbon.business.messaging.admin.services.exceptions.DestinationNotFoundException;
import org.wso2.carbon.business.messaging.admin.services.exceptions.InternalServerException;
import org.wso2.carbon.business.messaging.admin.services.managers.BrokerManagerService;
import org.wso2.carbon.business.messaging.admin.services.managers.DestinationManagerService;
import org.wso2.carbon.business.messaging.admin.services.managers.DlcManagerService;
import org.wso2.carbon.business.messaging.admin.services.managers.MessageManagerService;
import org.wso2.carbon.business.messaging.admin.services.managers.impl.BrokerManagerServiceImpl;
import org.wso2.carbon.business.messaging.admin.services.managers.impl.DestinationManagerServiceImpl;
import org.wso2.carbon.business.messaging.admin.services.managers.impl.DlcManagerServiceImpl;
import org.wso2.carbon.business.messaging.admin.services.managers.impl.MessageManagerServiceImpl;
import org.wso2.carbon.business.messaging.admin.services.types.ClusterInformation;
import org.wso2.carbon.business.messaging.admin.services.types.Destination;
import org.wso2.carbon.business.messaging.admin.services.types.DestinationNamesList;
import org.wso2.carbon.business.messaging.admin.services.types.ErrorResponse;
import org.wso2.carbon.business.messaging.admin.services.types.NewDestination;
import org.wso2.carbon.business.messaging.admin.services.types.Protocols;
import org.wso2.carbon.business.messaging.admin.services.types.RerouteDetails;
import org.wso2.msf4j.Microservice;
import org.wso2.msf4j.Request;

import java.util.ArrayList;
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
 * resources of the WSO2 message broker. Resources being destinations, dlc, subscriptions and messages.
 */
@Component(
        name = "org.wso2.mb.admin.services.MbRestService",
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
@Api(value = "mb/v1.0.0", description = "Endpoint to WSO2 message broker REST service.")
@Path("/mb/v1.0.0")
public class MbRestService implements Microservice {
    private static final Logger log = LoggerFactory.getLogger(MbRestService.class);
    /**
     * Bundle registration service for andes REST service.
     */
    private ServiceRegistration serviceRegistration;

    /**
     * Service class for retrieving broker information.
     */
    private BrokerManagerService brokerManagerService;

    /**
     * Service class for retrieving messages related information.
     */
    private MessageManagerService messageManagerService;

    /**
     * Service class for processing destination related requests.
     */
    private DestinationManagerService destinationManagerService;

    /**
     * Service class for processing dlc queue related requests.
     */
    private DlcManagerService dlcManagerService;

    /**
     * DLC queue name of the node.
     */
    private static final String DLC_QUEUE_NAME = AndesConstants.DEAD_LETTER_QUEUE_SUFFIX;

    public MbRestService() {

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

        Protocols protocols = brokerManagerService.getSupportedProtocols();
        return Response.status(Response.Status.OK).entity(protocols).build();

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

        ClusterInformation clusterInformation = brokerManagerService.getClusterInformation();
        return Response.status(Response.Status.OK).entity(clusterInformation).build();
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
        boolean destinationExist = destinationManagerService.isDestinationExist(protocol, destinationType,
                newDestination.getDestinationName());
        if (!destinationExist) {
            destinationManagerService.createDestination(protocol, destinationType, newDestination.getDestinationName());
            return Response.status(Response.Status.ACCEPTED).header(HttpHeaders.LOCATION, request.getUri() + "/name/"
                    + newDestination.getDestinationName()).build();
        } else {
            throw new InternalServerException("Destination '" + newDestination.getDestinationName() + "' already "
                    + "exists.");
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

        boolean destinationExist = destinationManagerService.isDestinationExist(protocol, destinationType,
                destinationName);
        if (destinationExist) {
            destinationManagerService.deleteDestination(protocol, destinationType, destinationName);
            return Response.status(Response.Status.NO_CONTENT).build();
        } else {
            throw new DestinationNotFoundException("Destination '" + destinationName + "' not found.");
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
     * @throws BadRequestException Invalid Limit
     * @throws BadRequestException Invalid offset
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
            @Context org.wso2.msf4j.Request request) throws InternalServerException, BadRequestException {

        if (offset < 0) {
            throw new BadRequestException("offset is less than 0");
        }
        if (limit < 1) {
            throw new BadRequestException("limit is less than 1");
        }

        List<String> destinationList = destinationManagerService.getDestinations(protocol, destinationType,
                destinationName, offset, limit);

        DestinationNamesList destinationNamesList = new DestinationNamesList();
        destinationNamesList.setDestinationNames(destinationList);
        destinationNamesList.setDestinationType(destinationType);
        destinationNamesList.setProtocol(protocol);

        return Response.status(Response.Status.OK).entity(destinationNamesList).build();
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

        boolean destinationExist = destinationManagerService.isDestinationExist(protocol, destinationType,
                destinationName);
        if (destinationExist) {
            messageManagerService.deleteMessages(protocol, destinationType, destinationName);
            return Response.status(Response.Status.NO_CONTENT).build();
        } else {
            throw new DestinationNotFoundException("Destination '" + destinationName + "' not found to "
                    + "delete/purge messages.");
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

        Destination destination = destinationManagerService.getDestination(protocol, destinationType, destinationName);
        if (null != destination) {
            return Response.status(Response.Status.OK).entity(destination).build();
        } else {
            throw new DestinationNotFoundException("Destination '" + destinationName + "' not found.");
        }
    }

    /**
     * Gets the number of messages in the dlc.
     * <p>
     * curl command :
     * <pre>
     *  curl -v http://127.0.0.1:8080/mb/v1.0.0/dlc/messagecount
     * </pre>
     *
     * @return Return an instance of {@link Long}.  <p>
     * @throws InternalServerException Server error when processing the request
     * <ul>
     *     <li>{@link javax.ws.rs.core.Response.Status#OK} - Returns a {@link Long} as a JSON response.</li>
     *     <li>{@link javax.ws.rs.core.Response.Status#INTERNAL_SERVER_ERROR} - Error occurred when getting the
     *     destination from the broker.</li>
     * </ul>
     */
    @GET
    @Path("/dlc/messagecount")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @ApiOperation(
            value = "Get message count in DLC",
            notes = "Can be used to get the total message count in DLC",
            tags = "DLC")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Message count returned.", response = long.class),
            @ApiResponse(code = 500, message = "Server Error.", response = ErrorResponse.class)})
    public Response getMessagCountInDLC() throws InternalServerException {

        long numberOfMessages = dlcManagerService.getMessagCountInDLC(DLC_QUEUE_NAME);
        return Response.status(Response.Status.OK).entity(numberOfMessages).build();
    }


    /**
     * Gets the number of messages in a specific dlc queue.
     * <p>
     * curl command example :
     * <pre>
     *  curl -v http://127.0.0.1:8080/mb/v1.0.0/dlc/queue-name/queue1/messagecount
     * </pre>
     *
     * @param queueName The name of the dlc queue.
     * @return A JSON/XML representation of {@link Long}. <p>
     * @throws InternalServerException Server error when processing the request
     * @throws DestinationNotFoundException Request destination is not found
     * <ul>
     *     <li>{@link javax.ws.rs.core.Response.Status#OK} - Returns a {@link Long} as a JSON response.</li>
     *     <li>{@link javax.ws.rs.core.Response.Status#NOT_FOUND} - Queue does not exists.</li>
     *     <li>{@link javax.ws.rs.core.Response.Status#INTERNAL_SERVER_ERROR} - Error occurred when getting the
     *     number of messages from the dlc queue.</li>
     * </ul>
     */
    @GET
    @Path("/dlc/queue-name/{queue-name}/messagecount")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @ApiOperation(
            value = "Get message count in DLC for specific queue",
            notes = "Can be used to get the total message count in DLC",
            tags = "DLC")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Message count returned.", response = long.class),
            @ApiResponse(code = 404, message = "Queue does not exists in message broker.", response = ErrorResponse
                    .class),
            @ApiResponse(code = 500, message = "Server Error.", response = ErrorResponse.class)})
    public Response getMessagCountInDLCForQueue(
            @PathParam("queue-name") String queueName)
            throws InternalServerException, DestinationNotFoundException {

        boolean isQueueExists = dlcManagerService.isQueueExists(queueName);
        if (isQueueExists) {
            long numberOfMessages = dlcManagerService.getMessageCountInDLCForQueue(queueName, DLC_QUEUE_NAME);
            return Response.status(Response.Status.OK).entity(numberOfMessages).build();
        } else {
            throw new DestinationNotFoundException("Destination '" + queueName + "' not found to " + "get message "
                    + "count" + ".");
        }
    }

    /**
     * Gets messages in a specific dlc queue.
     * <p>
     * curl command example :
     * <pre>
     *  curl -v http://127.0.0.1:8080/mb/v1.0.0/dlc/queue-name/queue1/messages
     * </pre>
     *
     * @param queueName The name of the dlc queue.
     * @return A JSON/XML representation of {@link List}. <p>
     * @throws InternalServerException Server error when processing the request
     * @throws DestinationNotFoundException Request destination is not found
     * @throws BadRequestException starttingId is invalid
     * <ul>
     *     <li>{@link javax.ws.rs.core.Response.Status#OK} - Returns a {@link List} as a JSON response.</li>
     *     <li>{@link javax.ws.rs.core.Response.Status#NOT_FOUND} - Queue does not exists.</li>
     *     <li>{@link javax.ws.rs.core.Response.Status#INTERNAL_SERVER_ERROR} - Error occurred when getting
     *     messages from the dlc queue.</li>
     * </ul>
     */
    @GET
    @Path("/dlc/queue-name/{queue-name}/messages")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @ApiOperation(
            value = "Get message metadata list in DLC for specific queue",
            notes = "Can be used to get message metadata in DLC for specific queue",
            tags = "DLC")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Message count returned.", response = List.class),
            @ApiResponse(code = 400, message = "Query parameters are not valid.", response = ErrorResponse.class),
            @ApiResponse(code = 404, message = "Queue does not exists in broker.", response = ErrorResponse.class),
            @ApiResponse(code = 500, message = "Server Error.", response = ErrorResponse.class)})
    public Response getMessagesInDLCForQueue(
            @PathParam("queue-name") String queueName,
            @QueryParam("startingId") String firstMessageIdValue,
            @QueryParam("limit") String countValue,
            @QueryParam("include-content") boolean isContent)
            throws InternalServerException, DestinationNotFoundException, BadRequestException {

        boolean isQueueExists = dlcManagerService.isQueueExists(queueName);
        int count;
        long firstMessageId;
        try {
            count = Integer.parseInt(countValue);
            firstMessageId = Long.parseLong(firstMessageIdValue);
        } catch (NumberFormatException e) {
            throw new BadRequestException("Query parameter limit/starttingId is invalid", e);
        }
        if (isQueueExists) {
            if (isContent) {
                List<AndesMessage> andesMessageList = dlcManagerService.getMessageContentInDLCForQueue
                        (queueName, DLC_QUEUE_NAME, firstMessageId, count);
                return Response.status(Response.Status.OK).entity(andesMessageList).build();

            } else {
                List<AndesMessageMetadata> andesMessageMetadataList = dlcManagerService.getMessageMetadataInDLCForQueue
                        (queueName, DLC_QUEUE_NAME, firstMessageId, count);
                return Response.status(Response.Status.OK).entity(andesMessageMetadataList).build();
            }
        } else {
            throw new DestinationNotFoundException("Destination '" + queueName + "' not found to " + "get messages.");
        }
    }

    /**
     * Delete messages from DLC.
     * <p>
     * curl command example :
     * <pre>
     *  curl -v http://127.0.0.1:8080/mb/v1.0.0/dlc/messages
     * </pre>
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
    @Path("/dlc/messages")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @ApiOperation(
            value = "Deletes/Purge message.",
            notes = "Deletes/Purge message belonging to DLC.",
            tags = "Messages")
    @ApiResponses(value = {
            @ApiResponse(code = 204, message = "Messages purged successfully.")})
    public Response deleteMessagesFromDeadLetterQueue(
            @ApiParam(value = "RerouteDetails.") RerouteDetails rerouteDetails,
            @Context Request request) {

        long[] andesMetadataIDs = rerouteDetails.getAndesMetadataIDs();
        this.dlcManagerService.deleteMessagesFromDeadLetterQueue(andesMetadataIDs, DLC_QUEUE_NAME);
        return Response.status(Response.Status.NO_CONTENT).build();

    }

    /**
     * Reroute all messages specific to a queue in dlc.
     * <p>
     * curl command example :
     * <pre>
     *  curl -v POST http://127.0.0.1:8080/mb/v1.0.0/dlc/queue-name/q/rerouteall -H "Content-Type: application/json"
     *  -d '{
              "andesMetadataIDs": [ ],
              "destination": "testqueue"
            }
     * </pre>

     * @param sourceQueueName Name of the original queue in which the messages needed to be rerouted.
     * @return A JSON representation of the newly created {@link Integer}. <p>
     * @throws InternalServerException Server error when processing the request
     * <ul>
     *     <li>{@link javax.ws.rs.core.Response.Status#OK} - Returns a {@link Integer} as a JSON response.</li>
     *     <li>{@link javax.ws.rs.core.Response.Status#NOT_FOUND} - Queue does not exists.</li>
     *     <li>{@link javax.ws.rs.core.Response.Status#INTERNAL_SERVER_ERROR} - Error while creating new destination
     *     .</li>
     * </ul>
     */
    @POST
    @Path("/dlc/queue-name/{queue-name}/rerouteall")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @ApiOperation(
            value = "Reroute messages in dlc.",
            notes = "Reroute all messages specific to a queue in dlc.",
            tags = "DLC")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Rerouted messages successfully."),
            @ApiResponse(code = 500, message = "Server error on rerouting messages.", response = ErrorResponse.class)})
    public Response rerouteAllMessagesInDeadLetterChannelForQueue(
            @ApiParam(value = "Source queue name.")
            @PathParam("queue-name") String sourceQueueName,
            @ApiParam(value = "RerouteDetails.") RerouteDetails rerouteDetails
            ) throws InternalServerException, DestinationNotFoundException {

        String targetQueueName = rerouteDetails.getDestinationName();
        boolean isSourceQueueExists = dlcManagerService.isQueueExists(sourceQueueName);
        boolean isTargetQueueExists = dlcManagerService.isQueueExists(targetQueueName);
        boolean restoreToOriginalQueue = sourceQueueName.equals(targetQueueName);
        int internalBatchSize = AndesConfigurationManager.readValue(AndesConfiguration.RDBMS_INTERNAL_BATCH_SIZE);

        if (isSourceQueueExists && isTargetQueueExists) {
            int numberOfReroutedMessages = this.dlcManagerService.rerouteAllMessagesInDeadLetterChannelForQueue
                    (DLC_QUEUE_NAME, sourceQueueName, targetQueueName, internalBatchSize, restoreToOriginalQueue);
            return Response.status(Response.Status.OK).entity(numberOfReroutedMessages).build();
        } else {
            throw new DestinationNotFoundException("Destination '" + sourceQueueName + "' or" + targetQueueName
                    + "not found to reroute messages.");
        }
    }

    /**
     * Reroute set of selected messages in dlc.
     * <p>
     * curl command example :
     * <pre>
     *  curl -v POST http://127.0.0.1:8080/mb/v1.0.0/dlc/queue-name/q1/reroute -H
     *  "Content-Type: application/json"
     *  -d '{
     *         "andesMetadataIDs": [1234 ]
            }'
     * </pre>

     * @param sourceQueueName Name of the original queue in which the messages needed to be rerouted.
     * @return A JSON representation of the newly created {@link Integer}. <p>
     * @throws InternalServerException Server error when processing the request
     * <ul>
     *     <li>{@link javax.ws.rs.core.Response.Status#OK} - Returns a {@link Integer} as a JSON response.</li>
     *     <li>{@link javax.ws.rs.core.Response.Status#NOT_FOUND} - Queue does not exists.</li>
     *     <li>{@link javax.ws.rs.core.Response.Status#INTERNAL_SERVER_ERROR} - Error while creating new destination
     *     .</li>
     * </ul>
     */
    @POST
    @Path("/dlc/queue-name/{queue-name}/reroute")
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @ApiOperation(
            value = "Reroute set of selected messages in dlc.",
            notes = "Reroute selected messages specific to a queue in dlc.",
            tags = "DLC")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Rerouted messages successfully."),
            @ApiResponse(code = 500, message = "Server error on rerouting messages", response = ErrorResponse.class)})
    public Response rerouteSelectedMessagesInDeadLetterChannelForQueue(
            @ApiParam(value = "Specific queue in which the messages will be rerouted.")
            @PathParam("queue-name") String sourceQueueName,
            @ApiParam(value = "RerouteDetails.") RerouteDetails rerouteDetails,
            @Context org.wso2.msf4j.Request request) throws InternalServerException, DestinationNotFoundException {

        long[] andesMetadataIDs = rerouteDetails.getAndesMetadataIDs();
        String targetQueueName = rerouteDetails.getDestinationName();
        boolean isSourceQueueExists = dlcManagerService.isQueueExists(sourceQueueName);
        boolean isTargetQueueExists = dlcManagerService.isQueueExists(targetQueueName);
        List<Long> messageIdCollection = new ArrayList<>();
        if (isSourceQueueExists && isTargetQueueExists && andesMetadataIDs != null) {
            for (Long messageId : andesMetadataIDs) {
                messageIdCollection.add(messageId);
            }
            boolean restoreToOriginalQueue = sourceQueueName.equals(targetQueueName);
            int numberOfReroutedMessages = this.dlcManagerService.moveMessagesFromDLCToNewDestination
                    (messageIdCollection, sourceQueueName, targetQueueName, restoreToOriginalQueue);

            return Response.status(Response.Status.OK).entity(numberOfReroutedMessages).build();

        } else {
            throw new DestinationNotFoundException("Destination '" + sourceQueueName + "' or" + targetQueueName
                    + "not found to reroute messages.");
        }
    }

    /**
     * Setter method for brokerManagerService instance.
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
        serviceRegistration = bundleContext.registerService(MbRestService.class.getName(), this, null);
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
        MbRestServiceDataHolder.getInstance().setAndesCore(andesCore);
        brokerManagerService = new BrokerManagerServiceImpl();
        destinationManagerService = new DestinationManagerServiceImpl();
        messageManagerService = new MessageManagerServiceImpl();
        dlcManagerService = new DlcManagerServiceImpl();
    }

    /**
     * This is the unbind method which gets called at the un-registration of CarbonRuntime OSGi service.
     *
     * @param andesCore The MessagingCore instance registered by Carbon Kernel as an OSGi service
     */
    protected void unsetAndesCore(Andes andesCore) {
        MbRestServiceDataHolder.getInstance().setAndesCore(null);
    }


}
