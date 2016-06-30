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

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
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
import org.wso2.carbon.andes.rest.tests.Constants;
import org.wso2.carbon.andes.service.exceptions.DestinationManagerException;
import org.wso2.carbon.andes.service.exceptions.mappers.DestinationNotFoundMapper;
import org.wso2.carbon.andes.service.exceptions.mappers.InvalidLimitValueMapper;
import org.wso2.carbon.andes.service.exceptions.mappers.InvalidOffsetValueMapper;
import org.wso2.carbon.andes.service.internal.AndesRESTService;
import org.wso2.carbon.andes.service.managers.DestinationManagerService;
import org.wso2.carbon.andes.service.types.Destination;
import org.wso2.msf4j.MicroservicesRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test cases that relates to pagination when getting list of objects through the rest service.
 */
public class PaginationTestCases {
    private MicroservicesRunner microservicesRunner;
    private AndesRESTService andesRESTService;
    private DestinationNotFoundMapper destinationNotFoundMapper = new DestinationNotFoundMapper();
    private InvalidLimitValueMapper invalidLimitValueMapper = new InvalidLimitValueMapper();
    private InvalidOffsetValueMapper invalidOffsetValueMapper = new InvalidOffsetValueMapper();

    /**
     * Initializes the tests.
     *
     * @throws AndesException
     */
    @BeforeClass
    public void setupService() throws AndesException {
        microservicesRunner = new MicroservicesRunner(Constants.PORT);
        andesRESTService = new AndesRESTService();
        microservicesRunner.addExceptionMapper(destinationNotFoundMapper, invalidLimitValueMapper,
                invalidOffsetValueMapper).deploy(andesRESTService).start();
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
     * Tests whether valid values are returned from response when requested for destinations with limit and offset.
     *
     * @throws DestinationManagerException
     * @throws IOException
     * @throws JSONException
     */
    @Test(groups = {"wso2.mb", "rest"})
    public void getDestinationsPaginationTestCase() throws DestinationManagerException, IOException, JSONException {
        List<Destination> destinations = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            destinations.add(new Destination());
        }

        DestinationManagerService destinationManagerService = mock(DestinationManagerService.class);
        when(destinationManagerService.getDestinationNames(Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString())).thenReturn(destinations.stream()
                .map(Destination::getDestinationName).collect(Collectors.toList()));
        when(destinationManagerService.getDestinations(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyInt(), Mockito.anyInt())).thenReturn(destinations);

        andesRESTService.setDestinationManagerService(destinationManagerService);

        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpGet getRequest = new HttpGet(Constants.BASE_URL + "/amqp-0-91/destination-type/queue?offset=5&limit=20");
        getRequest.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

        HttpResponse response = httpClient.execute(getRequest);

        JSONObject jsonObject = new JSONObject(EntityUtils.toString(response.getEntity()));

        Assert.assertTrue(((String) jsonObject.get("next")).contains("offset=25&limit=20"), "Invalid url for next " +
                                                                                            "pagination");
        Assert.assertTrue(((String) jsonObject.get("previous")).contains("offset=0&limit=5"), "Invalid url for " +
                                                                                              "previous pagination");

        httpClient = new DefaultHttpClient();
        getRequest = new HttpGet(Constants.BASE_URL + "/amqp-0-91/destination-type/queue?offset=30&limit=5");
        getRequest.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

        response = httpClient.execute(getRequest);

        jsonObject = new JSONObject(EntityUtils.toString(response.getEntity()));

        Assert.assertTrue(((String) jsonObject.get("next")).contains("offset=35&limit=5"), "Invalid url for next " +
                                                                                           "pagination");
        Assert.assertTrue(((String) jsonObject.get("previous")).contains("offset=25&limit=5"), "Invalid url for " +
                                                                                               "previous pagination");

        httpClient = new DefaultHttpClient();
        getRequest = new HttpGet(Constants.BASE_URL + "/amqp-0-91/destination-type/queue?offset=40&limit=20");
        getRequest.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

        response = httpClient.execute(getRequest);

        jsonObject = new JSONObject(EntityUtils.toString(response.getEntity()));

        Assert.assertEquals(jsonObject.get("next"), StringUtils.EMPTY, "Invalid url for next pagination");
        Assert.assertTrue(((String) jsonObject.get("previous")).contains("offset=20&limit=20"), "Invalid url for " +
                                                                                                "previous pagination");

        httpClient = new DefaultHttpClient();
        getRequest = new HttpGet(Constants.BASE_URL + "/amqp-0-91/destination-type/queue");
        getRequest.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

        response = httpClient.execute(getRequest);

        jsonObject = new JSONObject(EntityUtils.toString(response.getEntity()));

        Assert.assertTrue(((String) jsonObject.get("next")).contains("offset=20&limit=20"), "Invalid url for next " +
                                                                                            "pagination");
        Assert.assertEquals(jsonObject.get("previous"), StringUtils.EMPTY, "Invalid url for previous pagination");

        httpClient = new DefaultHttpClient();
        getRequest = new HttpGet(Constants.BASE_URL + "/amqp-0-91/destination-type/queue?offset=0&limit=1");
        getRequest.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

        response = httpClient.execute(getRequest);

        jsonObject = new JSONObject(EntityUtils.toString(response.getEntity()));

        Assert.assertTrue(((String) jsonObject.get("next")).contains("offset=1&limit=1"), "Invalid url for next " +
                                                                                          "pagination");
        Assert.assertEquals(jsonObject.get("previous"), StringUtils.EMPTY, "Invalid url for previous pagination");

        httpClient = new DefaultHttpClient();
        getRequest = new HttpGet(Constants.BASE_URL + "/amqp-0-91/destination-type/queue?offset=50&limit=1");
        getRequest.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

        response = httpClient.execute(getRequest);

        jsonObject = new JSONObject(EntityUtils.toString(response.getEntity()));

        Assert.assertEquals(jsonObject.get("next"), StringUtils.EMPTY, "Invalid url for next pagination");
        Assert.assertTrue(((String) jsonObject.get("previous")).contains("offset=49&limit=1"), "Invalid url for " +
                                                                                               "previous pagination");

        httpClient = new DefaultHttpClient();
        getRequest = new HttpGet(Constants.BASE_URL + "/amqp-0-91/destination-type/queue?offset=0&limit=50");
        getRequest.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

        response = httpClient.execute(getRequest);

        jsonObject = new JSONObject(EntityUtils.toString(response.getEntity()));

        Assert.assertEquals(jsonObject.get("next"), StringUtils.EMPTY, "Invalid url for next pagination");
        Assert.assertEquals(jsonObject.get("previous"), StringUtils.EMPTY, "Invalid url for previous pagination");

        httpClient = new DefaultHttpClient();
        getRequest = new HttpGet(Constants.BASE_URL + "/amqp-0-91/destination-type/queue?offset=0&limit=100");
        getRequest.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

        response = httpClient.execute(getRequest);

        jsonObject = new JSONObject(EntityUtils.toString(response.getEntity()));

        Assert.assertEquals(jsonObject.get("next"), StringUtils.EMPTY, "Invalid url for next pagination");
        Assert.assertEquals(jsonObject.get("previous"), StringUtils.EMPTY, "Invalid url for previous pagination");

        httpClient.getConnectionManager().shutdown();
    }

    /**
     * Tests that 400 is received as response when invalid values for limit are passed in the request for getting
     * destinations.
     *
     * @throws DestinationManagerException
     * @throws IOException
     * @throws JSONException
     */
    @Test(groups = {"wso2.mb", "rest"})
    public void invalidLimitValueForDestinationsTestCase() throws DestinationManagerException, IOException,
            JSONException {
        DestinationManagerService destinationManagerService = mock(DestinationManagerService.class);
        when(destinationManagerService.getDestinations(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyInt(), Mockito.anyInt())).thenReturn(new ArrayList<>());

        andesRESTService.setDestinationManagerService(destinationManagerService);

        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpGet getRequest = new HttpGet(Constants.BASE_URL + "/amqp-0-91/destination-type/queue?limit=-5");
        getRequest.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

        HttpResponse response = httpClient.execute(getRequest);

        JSONObject jsonObject = new JSONObject(EntityUtils.toString(response.getEntity()));
        validateExceptionHandling(jsonObject);
        Assert.assertEquals(response.getStatusLine().getStatusCode(), Response.Status.BAD_REQUEST.getStatusCode(),
                "400 not received");

        httpClient = new DefaultHttpClient();
        getRequest = new HttpGet(Constants.BASE_URL + "/amqp-0-91/destination-type/queue?limit=0");
        getRequest.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

        response = httpClient.execute(getRequest);

        jsonObject = new JSONObject(EntityUtils.toString(response.getEntity()));

        validateExceptionHandling(jsonObject);
        Assert.assertEquals(response.getStatusLine().getStatusCode(), Response.Status.BAD_REQUEST.getStatusCode(),
                "400 not received");
    }

    /**
     * Tests that 400 is received as response when invalid values for offset are passed in the request for getting
     * destinations.
     *
     * @throws DestinationManagerException
     * @throws IOException
     * @throws JSONException
     */
    @Test(groups = {"wso2.mb", "rest"})
    public void invalidOffsetValueForDestinationsTestCase() throws DestinationManagerException, IOException,
            JSONException {
        DestinationManagerService destinationManagerService = mock(DestinationManagerService.class);
        when(destinationManagerService.getDestinations(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                Mockito.anyInt(), Mockito.anyInt())).thenReturn(new ArrayList<>());

        andesRESTService.setDestinationManagerService(destinationManagerService);

        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpGet getRequest = new HttpGet(Constants.BASE_URL + "/amqp-0-91/destination-type/queue?offset=-5");
        getRequest.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

        HttpResponse response = httpClient.execute(getRequest);

        JSONObject jsonObject = new JSONObject(EntityUtils.toString(response.getEntity()));
        validateExceptionHandling(jsonObject);
        Assert.assertEquals(response.getStatusLine().getStatusCode(), Response.Status.BAD_REQUEST.getStatusCode(),
                "400 not received");
    }
    
    /**
     * Validates that required content are there in error response.
     *
     * @param jsonObject The response content as a JSON object.
     * @throws IOException
     * @throws JSONException
     */
    public void validateExceptionHandling(JSONObject jsonObject) throws IOException, JSONException {
        Assert.assertTrue(jsonObject.has("title"), "Title for the error is missing.");
        Assert.assertTrue(jsonObject.has("code"), "Error code is missing.");
        Assert.assertTrue(jsonObject.has("message"), "A message is required for the error.");
    }
}
