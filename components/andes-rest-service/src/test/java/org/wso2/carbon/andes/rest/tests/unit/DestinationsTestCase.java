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

package org.wso2.carbon.andes.rest.tests.unit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.andes.core.AndesException;
import org.wso2.carbon.andes.core.DestinationType;
import org.wso2.carbon.andes.core.ProtocolType;
import org.wso2.carbon.andes.rest.tests.Constants;
import org.wso2.carbon.andes.service.exceptions.DestinationManagerException;
import org.wso2.carbon.andes.service.exceptions.mappers.DestinationNotFoundMapper;
import org.wso2.carbon.andes.service.exceptions.mappers.InternalServerErrorMapper;
import org.wso2.carbon.andes.service.internal.AndesRESTService;
import org.wso2.carbon.andes.service.managers.DestinationManagerService;
import org.wso2.carbon.andes.service.types.Destination;
import org.wso2.carbon.andes.service.types.NewDestination;
import org.wso2.msf4j.MicroservicesRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The following test case compromise validation of rest guidelines for destinations related functions for the REST
 * service.
 */
public class DestinationsTestCase {
    private MicroservicesRunner microservicesRunner;
    private AndesRESTService andesRESTService;
    private DestinationNotFoundMapper destinationNotFoundMapper = new DestinationNotFoundMapper();
    private InternalServerErrorMapper internalServerErrorMapper = new InternalServerErrorMapper();

    /**
     * Initializes the tests.
     *
     * @throws AndesException
     */
    @BeforeClass
    public void setupService() throws AndesException {
        microservicesRunner = new MicroservicesRunner(Constants.PORT);
        andesRESTService = new AndesRESTService();
        microservicesRunner.addExceptionMapper(destinationNotFoundMapper, internalServerErrorMapper).deploy
                (andesRESTService).start();
    }

    /**
     * Cleans up the microservice runner and the andes service.
     */
    @AfterClass
    public void cleanUpServices() {
        microservicesRunner.stop();
        if (microservicesRunner.getMsRegistry().getHttpServices().contains(andesRESTService)) {
            microservicesRunner.getMsRegistry().removeService(andesRESTService);
        }
        andesRESTService = null;
        microservicesRunner = null;
    }

    /**
     * Tests that a 500 is received when an {@link DestinationManagerException} occurs while searching for
     * destinations.
     *
     * @throws AndesException
     * @throws IOException
     * @throws JSONException
     * @throws DestinationManagerException
     */
    @Test(groups = {"wso2.mb", "rest"})
    public void getDestinationsThrowDestinationManagerErrorTestCase() throws AndesException, IOException,
            JSONException, DestinationManagerException {
        DestinationManagerService destinationManagerService = mock(DestinationManagerService.class);
        when(destinationManagerService.getDestinationNames(Mockito.anyString(), Mockito.anyString(), Mockito
                .anyString())).thenReturn(new ArrayList<>());
        when(destinationManagerService.getDestinations(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyInt(), Mockito.anyInt())).thenThrow(new DestinationManagerException("Internal Error"));

        andesRESTService.setDestinationManagerService(destinationManagerService);

        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpGet getRequest = new HttpGet(Constants.BASE_URL + "/amqp-0-91/destination-type/queue");
        getRequest.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

        HttpResponse response = httpClient.execute(getRequest);

        validateExceptionHandling(response.getEntity());
        Assert.assertEquals(response.getStatusLine().getStatusCode(), Response.Status.INTERNAL_SERVER_ERROR
                .getStatusCode(), "500 not received");

        verify(destinationManagerService, atLeastOnce()).getDestinations(Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyInt(), Mockito.anyInt());

        httpClient.getConnectionManager().shutdown();
    }

    /**
     * Tests that a 200 is received when requested for destinations with default query params.
     *
     * @throws AndesException
     * @throws IOException
     * @throws JSONException
     * @throws DestinationManagerException
     */
    @Test(groups = {"wso2.mb", "rest"})
    public void getDestinationsWithNoQueryParamsTestCase() throws AndesException, IOException, JSONException,
            DestinationManagerException {
        DestinationManagerService destinationManagerService = mock(DestinationManagerService.class);
        when(destinationManagerService.getDestinations(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyInt(), Mockito.anyInt())).thenReturn(new ArrayList<>());

        andesRESTService.setDestinationManagerService(destinationManagerService);

        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpGet getRequest = new HttpGet(Constants.BASE_URL + "/amqp-0-91/destination-type/queue/");
        getRequest.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

        HttpResponse response = httpClient.execute(getRequest);
        Assert.assertEquals(response.getStatusLine().getStatusCode(), Response.Status.OK.getStatusCode(), "200 not "
                                                                                                          + "received");

        verify(destinationManagerService, atLeastOnce()).getDestinations(Mockito.anyString(), Mockito.anyString(), eq
                ("*"), eq(0), eq(20));

        httpClient.getConnectionManager().shutdown();
    }

    /**
     * Tests that a 200 is received when requested for destinations with a search keyword only.
     *
     * @throws AndesException
     * @throws IOException
     * @throws JSONException
     * @throws DestinationManagerException
     */
    @Test(groups = {"wso2.mb", "rest"})
    public void getDestinationsWithSearchOnlyTestCase() throws AndesException, IOException, JSONException,
            DestinationManagerException {
        Destination destinationQ1 = new Destination();
        destinationQ1.setDestinationName("Q-1");

        Destination destinationQ2 = new Destination();
        destinationQ2.setDestinationName("Q-2");

        DestinationManagerService destinationManagerService = mock(DestinationManagerService.class);
        when(destinationManagerService.getDestinationNames(Mockito.anyString(), Mockito.anyString(), Mockito
                .anyString())).thenReturn(Stream.of("Q-1", "Q-2").collect(Collectors.toList()));
        when(destinationManagerService.getDestinations(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyInt(), Mockito.anyInt())).thenReturn(Stream.of(destinationQ1, destinationQ2).collect
                (Collectors.toList()));

        andesRESTService.setDestinationManagerService(destinationManagerService);

        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpGet getRequest = new HttpGet(Constants.BASE_URL + "/amqp-0-91/destination-type/queue?name=Q-");
        getRequest.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

        HttpResponse response = httpClient.execute(getRequest);
        JSONObject jsonObject = new JSONObject(EntityUtils.toString(response.getEntity()));

        validateDestinationsContainer(jsonObject);
        Assert.assertEquals(jsonObject.get("totalDestinations"), 2, "The total destination count is missing.");
        Assert.assertEquals(jsonObject.getJSONArray("destinations").getJSONObject(0).get("destinationName"), "Q-1",
                "Q-1 is missing from the destination list.");
        Assert.assertEquals(jsonObject.getJSONArray("destinations").getJSONObject(1).get("destinationName"), "Q-2",
                "Q-2 is missing from the destination list.");
        Assert.assertEquals(response.getStatusLine().getStatusCode(), Response.Status.OK.getStatusCode(), "200 not "
                                                                                                          + "received");

        verify(destinationManagerService, atLeastOnce()).getDestinations(Mockito.anyString(), Mockito.anyString(), eq
                ("Q-"), eq(0), eq(20));

        httpClient.getConnectionManager().shutdown();
    }

    /**
     * Tests that a 200 is received when requested for destinations with limit query param only.
     *
     * @throws AndesException
     * @throws IOException
     * @throws JSONException
     * @throws DestinationManagerException
     */
    @Test(groups = {"wso2.mb", "rest"})
    public void getDestinationsWithLimitOnlyTestCase() throws AndesException, IOException, JSONException,
            DestinationManagerException {
        Destination destinationQ1 = new Destination();
        destinationQ1.setDestinationName("Q-1");

        Destination destinationQ2 = new Destination();
        destinationQ2.setDestinationName("Q-2");

        DestinationManagerService destinationManagerService = mock(DestinationManagerService.class);
        when(destinationManagerService.getDestinationNames(Mockito.anyString(), Mockito.anyString(), Mockito
                .anyString())).thenReturn(Stream.of("Q-1", "Q-2").collect(Collectors.toList()));
        when(destinationManagerService.getDestinations(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyInt(), Mockito.anyInt())).thenReturn(Stream.of(destinationQ1, destinationQ2).collect
                (Collectors.toList()));

        andesRESTService.setDestinationManagerService(destinationManagerService);

        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpGet getRequest = new HttpGet(Constants.BASE_URL + "/amqp-0-91/destination-type/queue?limit=6");
        getRequest.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

        HttpResponse response = httpClient.execute(getRequest);
        JSONObject jsonObject = new JSONObject(EntityUtils.toString(response.getEntity()));

        validateDestinationsContainer(jsonObject);
        Assert.assertEquals(jsonObject.get("totalDestinations"), 2, "The total destination count is missing.");
        Assert.assertEquals(jsonObject.getJSONArray("destinations").getJSONObject(0).get("destinationName"), "Q-1",
                "Q-1 is missing from the destination list.");
        Assert.assertEquals(jsonObject.getJSONArray("destinations").getJSONObject(1).get("destinationName"), "Q-2",
                "Q-2 is missing from the destination list.");
        Assert.assertEquals(response.getStatusLine().getStatusCode(), Response.Status.OK.getStatusCode(), "200 not "
                                                                                                          + "received");

        verify(destinationManagerService, atLeastOnce()).getDestinations(Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString(), eq(0), eq(6));

        httpClient.getConnectionManager().shutdown();
    }

    /**
     * Tests that a 200 is received when requested for destinations with offset query param only.
     *
     * @throws AndesException
     * @throws IOException
     * @throws JSONException
     * @throws DestinationManagerException
     */
    @Test(groups = {"wso2.mb", "rest"})
    public void getDestinationsWithOffsetOnlyTestCase() throws AndesException, IOException, JSONException,
            DestinationManagerException {
        Destination destinationQ1 = new Destination();
        destinationQ1.setDestinationName("Q-1");

        Destination destinationQ2 = new Destination();
        destinationQ2.setDestinationName("Q-2");

        DestinationManagerService destinationManagerService = mock(DestinationManagerService.class);
        when(destinationManagerService.getDestinationNames(Mockito.anyString(), Mockito.anyString(), Mockito
                .anyString())).thenReturn(Stream.of("Q-1", "Q-2").collect(Collectors.toList()));
        when(destinationManagerService.getDestinations(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyInt(), Mockito.anyInt())).thenReturn(Stream.of(destinationQ1, destinationQ2).collect
                (Collectors.toList()));

        andesRESTService.setDestinationManagerService(destinationManagerService);

        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpGet getRequest = new HttpGet(Constants.BASE_URL + "/amqp-0-91/destination-type/queue?offset=3");
        getRequest.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

        HttpResponse response = httpClient.execute(getRequest);
        JSONObject jsonObject = new JSONObject(EntityUtils.toString(response.getEntity()));

        validateDestinationsContainer(jsonObject);
        Assert.assertEquals(jsonObject.get("totalDestinations"), 2, "The total destination count is missing.");
        Assert.assertEquals(jsonObject.getJSONArray("destinations").getJSONObject(0).get("destinationName"), "Q-1",
                "Q-1 is missing from the destination list.");
        Assert.assertEquals(jsonObject.getJSONArray("destinations").getJSONObject(1).get("destinationName"), "Q-2",
                "Q-2 is missing from the destination list.");
        Assert.assertEquals(response.getStatusLine().getStatusCode(), Response.Status.OK.getStatusCode(), "200 not "
                                                                                                          + "received");

        verify(destinationManagerService, atLeastOnce()).getDestinations(Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString(), eq(3), eq(20));

        httpClient.getConnectionManager().shutdown();
    }

    /**
     * Tests that a 200 is received when requested for messages with all query params.
     *
     * @throws AndesException
     * @throws IOException
     * @throws JSONException
     * @throws DestinationManagerException
     */
    @Test(groups = {"wso2.mb", "rest"})
    public void getDestinationsWithAllParamsTestCase() throws AndesException, IOException, JSONException,
            DestinationManagerException {
        Destination destinationQ1 = new Destination();
        destinationQ1.setDestinationName("Q-1");

        Destination destinationQ2 = new Destination();
        destinationQ2.setDestinationName("Q-2");

        DestinationManagerService destinationManagerService = mock(DestinationManagerService.class);
        when(destinationManagerService.getDestinationNames(Mockito.anyString(), Mockito.anyString(), Mockito
                .anyString())).thenReturn(Stream.of("Q-1", "Q-2").collect(Collectors.toList()));
        when(destinationManagerService.getDestinations(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyInt(), Mockito.anyInt())).thenReturn(Stream.of(destinationQ1, destinationQ2).collect
                (Collectors.toList()));

        andesRESTService.setDestinationManagerService(destinationManagerService);

        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpGet getRequest = new HttpGet(Constants.BASE_URL +
                                         "/amqp-0-91/destination-type/queue?offset=3&limit=4&name=Q");
        getRequest.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

        HttpResponse response = httpClient.execute(getRequest);
        JSONObject jsonObject = new JSONObject(EntityUtils.toString(response.getEntity()));

        validateDestinationsContainer(jsonObject);
        Assert.assertEquals(jsonObject.get("totalDestinations"), 2, "The total destination count is missing.");
        Assert.assertEquals(jsonObject.getJSONArray("destinations").getJSONObject(0).get("destinationName"), "Q-1",
                "Q-1 is missing from the destination list.");
        Assert.assertEquals(jsonObject.getJSONArray("destinations").getJSONObject(1).get("destinationName"), "Q-2",
                "Q-2 is missing from the destination list.");
        Assert.assertEquals(response.getStatusLine().getStatusCode(), Response.Status.OK.getStatusCode(), "200 not "
                                                                                                          + "received");

        verify(destinationManagerService, atLeastOnce()).getDestinations(Mockito.anyString(), Mockito.anyString(), eq
                ("Q"), eq(3), eq(4));

        httpClient.getConnectionManager().shutdown();
    }

    /**
     * Tests that a 500 http status code is received when a server error is occurred when deleting destinations.
     *
     * @throws DestinationManagerException
     * @throws IOException
     * @throws JSONException
     */
    @Test(groups = {"wso2.mb", "rest"})
    public void deleteDestinationsThrowDestinationManagerErrorTestCase() throws DestinationManagerException,
            IOException, JSONException {
        DestinationManagerService destinationManagerService = mock(DestinationManagerService.class);
        doThrow(new DestinationManagerException("Internal Error")).when(destinationManagerService).deleteDestinations
                (Mockito.anyString(), Mockito.anyString());

        andesRESTService.setDestinationManagerService(destinationManagerService);

        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpDelete deleteRequest = new HttpDelete(Constants.BASE_URL + "/amqp-0-91/destination-type/queue");
        deleteRequest.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

        HttpResponse response = httpClient.execute(deleteRequest);

        validateExceptionHandling(response.getEntity());
        Assert.assertEquals(response.getStatusLine().getStatusCode(), Response.Status.INTERNAL_SERVER_ERROR
                .getStatusCode(), "500 not received");

        verify(destinationManagerService, atLeastOnce()).deleteDestinations(Mockito.anyString(), Mockito.anyString());

        httpClient.getConnectionManager().shutdown();
    }

    /**
     * Tests that a 204 http status code is received when destinations are deleted.
     *
     * @throws DestinationManagerException
     * @throws IOException
     * @throws JSONException
     */
    @Test(groups = {"wso2.mb", "rest"})
    public void validDeleteDestinationsTestCase() throws DestinationManagerException, IOException, JSONException {
        DestinationManagerService destinationManagerService = mock(DestinationManagerService.class);

        andesRESTService.setDestinationManagerService(destinationManagerService);

        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpDelete deleteRequest = new HttpDelete(Constants.BASE_URL + "/amqp-0-91/destination-type/queue");
        deleteRequest.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

        HttpResponse response = httpClient.execute(deleteRequest);

        Assert.assertEquals(response.getStatusLine().getStatusCode(), Response.Status.NO_CONTENT.getStatusCode(),
                "204 not received");

        verify(destinationManagerService, atLeastOnce()).deleteDestinations(Mockito.anyString(), Mockito.anyString());

        httpClient.getConnectionManager().shutdown();
    }

    /**
     * Tests that a 500 http status code is received when an internal error is occurred when getting a destination.
     *
     * @throws DestinationManagerException
     * @throws IOException
     * @throws JSONException
     */
    @Test(groups = {"wso2.mb", "rest"})
    public void getDestinationThrowDestinationManagerErrorTestCase() throws DestinationManagerException, IOException,
            JSONException {
        DestinationManagerService destinationManagerService = mock(DestinationManagerService.class);
        when(destinationManagerService.getDestination(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenThrow(new DestinationManagerException("Internal Error"));

        andesRESTService.setDestinationManagerService(destinationManagerService);

        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpGet getRequest = new HttpGet(Constants.BASE_URL + "/amqp-0-91/destination-type/queue/name/Q1");
        getRequest.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

        HttpResponse response = httpClient.execute(getRequest);

        validateExceptionHandling(response.getEntity());
        Assert.assertEquals(response.getStatusLine().getStatusCode(), Response.Status.INTERNAL_SERVER_ERROR
                .getStatusCode(), "500 not received");

        verify(destinationManagerService, atLeastOnce()).getDestination(Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString());

        httpClient.getConnectionManager().shutdown();
    }

    /**
     * Tests that a 404 http status code is received when getting a non existing destination it requested.
     *
     * @throws DestinationManagerException
     * @throws IOException
     * @throws JSONException
     */
    @Test(groups = {"wso2.mb", "rest"})
    public void getDestinationNullTestCase() throws DestinationManagerException, IOException, JSONException {
        DestinationManagerService destinationManagerService = mock(DestinationManagerService.class);
        when(destinationManagerService.getDestination(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(null);

        andesRESTService.setDestinationManagerService(destinationManagerService);

        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpGet getRequest = new HttpGet(Constants.BASE_URL + "/amqp-0-91/destination-type/queue/name/Q1");
        getRequest.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

        HttpResponse response = httpClient.execute(getRequest);

        validateExceptionHandling(response.getEntity());
        Assert.assertEquals(response.getStatusLine().getStatusCode(), Response.Status.NOT_FOUND.getStatusCode(), "404 not received");

        verify(destinationManagerService, atLeastOnce()).getDestination(Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString());

        httpClient.getConnectionManager().shutdown();
    }

    /**
     * Tests that a 200 http status code is received when a valid destination is requested.
     *
     * @throws DestinationManagerException
     * @throws IOException
     * @throws JSONException
     * @throws AndesException
     */
    @Test(groups = {"wso2.mb", "rest"})
    public void validGetDestinationTestCase() throws DestinationManagerException, IOException, JSONException,
            AndesException {
        Destination destination = new Destination();
        destination.setId(29);
        destination.setDestinationName("Q1");
        destination.setCreatedDate(new Date());
        destination.setDestinationType(DestinationType.QUEUE);
        destination.setProtocol(new ProtocolType("amqp", "0-9-1"));
        destination.setMessageCount(30);
        destination.setDurable(true);
        destination.setOwner("admin");
        destination.setSubscriptionCount(5);

        DestinationManagerService destinationManagerService = mock(DestinationManagerService.class);
        when(destinationManagerService.getDestination(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(destination);

        andesRESTService.setDestinationManagerService(destinationManagerService);

        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpGet getRequest = new HttpGet(Constants.BASE_URL + "/amqp-0-91/destination-type/queue/name/Q1");
        getRequest.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

        HttpResponse response = httpClient.execute(getRequest);

        JSONObject jsonObject = new JSONObject(EntityUtils.toString(response.getEntity()));
        Assert.assertEquals(jsonObject.get("id"), 29, "Invalid destination ID received.");
        Assert.assertEquals(jsonObject.get("destinationName"), "Q1", "Invalid destination name received.");
        Assert.assertTrue(jsonObject.has("createdDate"), "Created date did not exist for the destination.");
        Assert.assertEquals(jsonObject.get("destinationType"), "QUEUE", "Invalid destination type received.");
        Assert.assertEquals(jsonObject.getJSONObject("protocol").get("protocolName"), "amqp", "Invalid protocol name " +
                                                                                              "" + "received.");
        Assert.assertEquals(jsonObject.getJSONObject("protocol").get("version"), "0-9-1", "Invalid protocol version "
                                                                                          + "received.");
        Assert.assertEquals(jsonObject.get("messageCount"), 30, "Invalid destination message count received.");
        Assert.assertEquals(jsonObject.get("isDurable"), true, "Invalid destination durable value received.");
        Assert.assertEquals(jsonObject.get("owner"), "admin", "Invalid destination owner received.");
        Assert.assertEquals(jsonObject.get("subscriptionCount"), 5, "Invalid destination subscription count received.");

        Assert.assertEquals(response.getStatusLine().getStatusCode(), Response.Status.OK.getStatusCode(), "200 not "
                                                                                                          + "received");

        verify(destinationManagerService, atLeastOnce()).getDestination(Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString());

        httpClient.getConnectionManager().shutdown();
    }

    /**
     * Tests that a 500 http status code is received when an internal error is occurred when creating a destination.
     *
     * @throws DestinationManagerException
     * @throws IOException
     * @throws JSONException
     * @throws AndesException
     */
    @Test(groups = {"wso2.mb", "rest"})
    public void createDestinationThrowDestinationManagerErrorTestCase() throws DestinationManagerException,
            IOException, JSONException, AndesException {
        NewDestination destination = new NewDestination();
        destination.setDestinationName("Q1");
        StringEntity destinationAsJsonString = new StringEntity(new ObjectMapper().writeValueAsString(destination));

        DestinationManagerService destinationManagerService = mock(DestinationManagerService.class);
        when(destinationManagerService.createDestination(Mockito.anyString(), Mockito.anyString(), Mockito.anyString
                ())).thenThrow(new DestinationManagerException("Internal Error"));

        andesRESTService.setDestinationManagerService(destinationManagerService);

        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpPost postRequest = new HttpPost(Constants.BASE_URL + "/amqp-0-91/destination-type/queue");
        postRequest.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        postRequest.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        postRequest.setEntity(destinationAsJsonString);
        HttpResponse response = httpClient.execute(postRequest);

        validateExceptionHandling(response.getEntity());
        Assert.assertEquals(response.getStatusLine().getStatusCode(), Response.Status.INTERNAL_SERVER_ERROR
                .getStatusCode(), "500 not received");

        verify(destinationManagerService, atLeastOnce()).createDestination(Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString());

        httpClient.getConnectionManager().shutdown();
    }

    /**
     * Test case for creating a valid destination.
     *
     * @throws DestinationManagerException
     * @throws IOException
     * @throws JSONException
     * @throws AndesException
     */
    @Test(groups = {"wso2.mb", "rest"})
    public void validCreateDestinationTestCase() throws DestinationManagerException, IOException, JSONException,
            AndesException {
        NewDestination requestDestination = new NewDestination();
        requestDestination.setDestinationName("Q1");
        StringEntity destinationAsJsonString = new StringEntity(new ObjectMapper().writeValueAsString
                (requestDestination));

        Destination responseDestination = new Destination();
        responseDestination.setId(29);
        responseDestination.setDestinationName("Q1");
        responseDestination.setCreatedDate(new Date());
        responseDestination.setDestinationType(DestinationType.QUEUE);
        responseDestination.setProtocol(new ProtocolType("amqp", "0-9-1"));
        responseDestination.setMessageCount(30);
        responseDestination.setDurable(true);
        responseDestination.setOwner("admin");
        responseDestination.setSubscriptionCount(5);

        DestinationManagerService destinationManagerService = mock(DestinationManagerService.class);
        when(destinationManagerService.createDestination(Mockito.anyString(), Mockito.anyString(), eq("Q1")))
                .thenReturn(responseDestination);

        andesRESTService.setDestinationManagerService(destinationManagerService);

        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpPost postRequest = new HttpPost(Constants.BASE_URL + "/amqp-0-91/destination-type/queue/");
        postRequest.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        postRequest.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        postRequest.setEntity(destinationAsJsonString);
        HttpResponse response = httpClient.execute(postRequest);

        JSONObject jsonObject = new JSONObject(EntityUtils.toString(response.getEntity()));
        Assert.assertEquals(jsonObject.get("id"), 29, "Invalid destination ID received.");
        Assert.assertEquals(jsonObject.get("destinationName"), "Q1", "Invalid destination name received.");
        Assert.assertTrue(jsonObject.has("createdDate"), "Created date did not exist for the destination.");
        Assert.assertEquals(jsonObject.get("destinationType"), "QUEUE", "Invalid destination type received.");
        Assert.assertEquals(jsonObject.getJSONObject("protocol").get("protocolName"), "amqp", "Invalid protocol name " +
                                                                                              "" + "received.");
        Assert.assertEquals(jsonObject.getJSONObject("protocol").get("version"), "0-9-1", "Invalid protocol version "
                                                                                          + "received.");
        Assert.assertEquals(jsonObject.get("messageCount"), 30, "Invalid destination message count received.");
        Assert.assertEquals(jsonObject.get("isDurable"), true, "Invalid destination durable value received.");
        Assert.assertEquals(jsonObject.get("owner"), "admin", "Invalid destination owner received.");
        Assert.assertEquals(jsonObject.get("subscriptionCount"), 5, "Invalid destination subscription count received.");

        Assert.assertEquals(response.getStatusLine().getStatusCode(), Response.Status.OK.getStatusCode(), "200 not "
                                                                                                          + "received");

        verify(destinationManagerService, atLeastOnce()).createDestination(Mockito.anyString(), Mockito.anyString(),
                eq("Q1"));

        httpClient.getConnectionManager().shutdown();
    }

    /**
     * Tests that a 500 http status code is received when an internal error while deleting a destination.
     *
     * @throws DestinationManagerException
     * @throws IOException
     * @throws JSONException
     */
    @Test(groups = {"wso2.mb", "rest"})
    public void deleteDestinationThrowDestinationManagerErrorTestCase() throws DestinationManagerException,
            IOException, JSONException {
        DestinationManagerService destinationManagerService = mock(DestinationManagerService.class);

        when(destinationManagerService.getDestination(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(mock(Destination.class));
        doThrow(new DestinationManagerException("Internal Error")).when(destinationManagerService).deleteDestination
                (Mockito.anyString(), Mockito.anyString(), Mockito.anyString());

        andesRESTService.setDestinationManagerService(destinationManagerService);

        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpDelete deleteRequest = new HttpDelete(Constants.BASE_URL + "/amqp-0-91/destination-type/queue/name/Q-1");
        deleteRequest.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

        HttpResponse response = httpClient.execute(deleteRequest);

        validateExceptionHandling(response.getEntity());
        Assert.assertEquals(response.getStatusLine().getStatusCode(), Response.Status.INTERNAL_SERVER_ERROR
                .getStatusCode(), "500 not received");

        verify(destinationManagerService, atLeastOnce()).getDestination(Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString());

        httpClient.getConnectionManager().shutdown();
    }

    /**
     * Tests that a 404 http status code is received when deleting a non existing destination.
     *
     * @throws DestinationManagerException
     * @throws IOException
     * @throws JSONException
     */
    @Test(groups = {"wso2.mb", "rest"})
    public void deleteNullDestinationTestCase() throws DestinationManagerException, IOException, JSONException {
        DestinationManagerService destinationManagerService = mock(DestinationManagerService.class);

        when(destinationManagerService.getDestination(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(null);
        andesRESTService.setDestinationManagerService(destinationManagerService);

        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpDelete deleteRequest = new HttpDelete(Constants.BASE_URL + "/amqp-0-91/destination-type/queue/name/Q-1");
        deleteRequest.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

        HttpResponse response = httpClient.execute(deleteRequest);

        Assert.assertEquals(response.getStatusLine().getStatusCode(), Response.Status.NOT_FOUND.getStatusCode(), "404 not received");

        verify(destinationManagerService, atLeastOnce()).getDestination(Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString());

        httpClient.getConnectionManager().shutdown();
    }

    /**
     * Tests that a 204 http status code is received when deleting a valid destination.
     *
     * @throws DestinationManagerException
     * @throws IOException
     * @throws JSONException
     */
    @Test(groups = {"wso2.mb", "rest"})
    public void validDeleteDestinationTestCase() throws DestinationManagerException, IOException, JSONException {
        DestinationManagerService destinationManagerService = mock(DestinationManagerService.class);

        when(destinationManagerService.getDestination(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(mock(Destination.class));

        andesRESTService.setDestinationManagerService(destinationManagerService);

        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpDelete deleteRequest = new HttpDelete(Constants.BASE_URL + "/amqp-0-91/destination-type/queue/name/Q-1");
        deleteRequest.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

        HttpResponse response = httpClient.execute(deleteRequest);

        Assert.assertEquals(response.getStatusLine().getStatusCode(), Response.Status.NO_CONTENT.getStatusCode(),
                "204 not received");

        verify(destinationManagerService, atLeastOnce()).deleteDestination(Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString());

        httpClient.getConnectionManager().shutdown();
    }

    /**
     * Validates that required content are there in error response.
     *
     * @param responseEntity The response content.
     * @throws IOException
     * @throws JSONException
     */
    public void validateExceptionHandling(HttpEntity responseEntity) throws IOException, JSONException {
        JSONObject jsonObject = new JSONObject();
        if (responseEntity != null) {
            jsonObject = new JSONObject(EntityUtils.toString(responseEntity));
        }
        Assert.assertTrue(jsonObject.has("title"), "Title for the error is missing.");
        Assert.assertTrue(jsonObject.has("code"), "Error code is missing.");
        Assert.assertTrue(jsonObject.has("message"), "A message is required for the error.");
    }

    /**
     * Validates destination container content.
     *
     * @param jsonObject The response content as JSON.
     * @throws IOException
     * @throws JSONException
     */
    public void validateDestinationsContainer(JSONObject jsonObject) throws IOException, JSONException {
        Assert.assertTrue(jsonObject.has("totalDestinations"), "The total destination count is missing.");
        Assert.assertTrue(jsonObject.has("next"), "The url for the next set.");
        Assert.assertTrue(jsonObject.has("previous"), "The url of the previous set.");
        Assert.assertTrue(jsonObject.has("destinations"), "The destination list attribute is missing.");
    }
}
