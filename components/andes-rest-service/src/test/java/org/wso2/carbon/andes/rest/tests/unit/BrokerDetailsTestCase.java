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
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.andes.core.AndesException;
import org.wso2.carbon.andes.core.DestinationType;
import org.wso2.carbon.andes.rest.tests.Constants;
import org.wso2.carbon.andes.service.exceptions.BrokerManagerException;
import org.wso2.carbon.andes.service.exceptions.mappers.InternalServerErrorMapper;
import org.wso2.carbon.andes.service.internal.AndesRESTService;
import org.wso2.carbon.andes.service.managers.BrokerManagerService;
import org.wso2.carbon.andes.service.types.BrokerInformation;
import org.wso2.carbon.andes.service.types.ClusterInformation;
import org.wso2.carbon.andes.service.types.StoreInformation;
import org.wso2.msf4j.MicroservicesRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test cases related to broker information such as protocols, clustering, store health etc.
 */
public class BrokerDetailsTestCase {
    private MicroservicesRunner microservicesRunner;
    private AndesRESTService andesRESTService;
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
        microservicesRunner.addExceptionMapper(internalServerErrorMapper).deploy(andesRESTService).start();
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
     * Testing http response code when null is being returned.
     *
     * @throws BrokerManagerException
     * @throws AndesException
     * @throws IOException
     * @throws InterruptedException
     */
    @Test(groups = {"wso2.mb", "rest"})
    public void nullProtocolTypesTestCase() throws BrokerManagerException, AndesException, IOException,
            InterruptedException {
        BrokerManagerService brokerManagerService = mock(BrokerManagerService.class);
        when(brokerManagerService.getSupportedProtocols()).thenReturn(null);

        andesRESTService.setBrokerManagerService(brokerManagerService);

        // Test protocols returning null
        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpGet getRequest = new HttpGet(Constants.BASE_URL + "/protocol-types");
        getRequest.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        HttpResponse response = httpClient.execute(getRequest);

        HttpEntity responseEntity = response.getEntity();
        String content = StringUtils.EMPTY;
        if (responseEntity != null) {
            content = EntityUtils.toString(responseEntity);
        }
        Assert.assertEquals(content, "[]", "Unexpected protocols");
        Assert.assertEquals(response.getStatusLine().getStatusCode(), Response.Status.OK.getStatusCode(),
                                                                                                    "200 not received");

        httpClient.getConnectionManager().shutdown();
    }

    /**
     * Test to check when protocol test is empty.
     *
     * @throws AndesException
     * @throws IOException
     */
    @Test(groups = {"wso2.mb", "rest"})
    public void emptyProtocolListTestCase() throws AndesException, IOException, BrokerManagerException {
        BrokerManagerService brokerManagerService = mock(BrokerManagerService.class);
        when(brokerManagerService.getSupportedProtocols()).thenReturn(new ArrayList<>());

        andesRESTService.setBrokerManagerService(brokerManagerService);

        // Test protocols returning an empty list
        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpGet getRequest = new HttpGet(Constants.BASE_URL + "/protocol-types");
        getRequest.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

        HttpResponse response = httpClient.execute(getRequest);

        HttpEntity responseEntity = response.getEntity();
        String content = StringUtils.EMPTY;
        if (responseEntity != null) {
            content = EntityUtils.toString(responseEntity);
        }
        Assert.assertEquals(content, "[]", "Unexpected protocols");
        Assert.assertEquals(response.getStatusLine().getStatusCode(), Response.Status.OK.getStatusCode(),
                                                                                                    "200 not received");

        httpClient.getConnectionManager().shutdown();
    }

    /**
     * Test to validate content and http codes when valid data is returned.
     *
     * @throws BrokerManagerException
     * @throws AndesException
     * @throws IOException
     * @throws JSONException
     */
    @Test(groups = {"wso2.mb", "rest"})
    public void validProtocolTypesTestCase() throws BrokerManagerException, AndesException, IOException, JSONException {
        BrokerManagerService brokerManagerService = mock(BrokerManagerService.class);
        when(brokerManagerService.getSupportedProtocols()).thenReturn(
                Stream.of("Prot-1", "Prot-2")
                        .collect(Collectors.toList()));

        andesRESTService.setBrokerManagerService(brokerManagerService);

        // Test valid protocol details.
        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpGet getRequest = new HttpGet(Constants.BASE_URL + "/protocol-types");
        getRequest.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

        HttpResponse response = httpClient.execute(getRequest);

        HttpEntity responseEntity = response.getEntity();
        JSONArray jsonArray = new JSONArray();
        if (responseEntity != null) {
            jsonArray = new JSONArray(EntityUtils.toString(responseEntity));
        }
        Assert.assertEquals(jsonArray.toString(), "[\"Prot-1\",\"Prot-2\"]", "Unexpected protocols");
        Assert.assertEquals(response.getStatusLine().getStatusCode(), Response.Status.OK.getStatusCode(),
                                                                                                    "200 not received");

        httpClient.getConnectionManager().shutdown();
    }

    /**
     * Test that 200 is received when valid cluster details are responded.
     * @throws BrokerManagerException
     * @throws IOException
     * @throws JSONException
     */
    @Test(groups = {"wso2.mb", "rest"})
    public void validClusterDetailsTestCase() throws BrokerManagerException, IOException, JSONException {
        BrokerManagerService brokerManagerService = mock(BrokerManagerService.class);
        ClusterInformation clusterInformationMock = new ClusterInformation();
        clusterInformationMock.setNodeID("node/0.0.0.1");
        clusterInformationMock.setCoordinatorAddress("6.6.6.6:9999");
        clusterInformationMock.setNodeAddresses(new ArrayList<>());

        when(brokerManagerService.getClusterInformation()).thenReturn(clusterInformationMock);

        andesRESTService.setBrokerManagerService(brokerManagerService);

        // Test valid clustering details
        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpGet getRequest = new HttpGet(Constants.BASE_URL + "/information/cluster");
        getRequest.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

        HttpResponse response = httpClient.execute(getRequest);

        HttpEntity responseEntity = response.getEntity();
        JSONObject jsonObject = new JSONObject();
        if (responseEntity != null) {
            jsonObject = new JSONObject(EntityUtils.toString(responseEntity));
        }
        Assert.assertTrue(StringUtils.isNotBlank((String) jsonObject.get("nodeID")), "Unexpected cluster details " +
                                                                                     "received");
        Assert.assertEquals(response.getStatusLine().getStatusCode(), Response.Status.OK.getStatusCode(),
                                                                                                    "200 not received");

        httpClient.getConnectionManager().shutdown();
    }

    /**
     * Test that 200 is received when valid store details are responded.
     *
     * @throws BrokerManagerException
     * @throws IOException
     * @throws JSONException
     */
    @Test(groups = {"wso2.mb", "rest"})
    public void validStoreDetailsTestCase() throws BrokerManagerException, IOException, JSONException {
        BrokerManagerService brokerManagerService = mock(BrokerManagerService.class);
        when(brokerManagerService.getStoreInformation()).thenReturn(new StoreInformation());

        andesRESTService.setBrokerManagerService(brokerManagerService);

        // Test valid store information
        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpGet getRequest = new HttpGet(Constants.BASE_URL + "/information/store");
        getRequest.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

        HttpResponse response = httpClient.execute(getRequest);

        HttpEntity responseEntity = response.getEntity();
        JSONObject jsonObject = new JSONObject();
        if (responseEntity != null) {
            jsonObject = new JSONObject(EntityUtils.toString(responseEntity));
        }
        Assert.assertFalse((boolean) jsonObject.get("healthy"), "Unexpected cluster details " + "received");
        Assert.assertEquals(response.getStatusLine().getStatusCode(), Response.Status.OK.getStatusCode(),
                                                                                                    "200 not received");

        httpClient.getConnectionManager().shutdown();
    }

    /**
     * Test that 200 is received when valid broker details are responded.
     *
     * @throws BrokerManagerException
     * @throws IOException
     * @throws JSONException
     */
    @Test(groups = {"wso2.mb", "rest"})
    public void validBrokerDetailsTestCase() throws BrokerManagerException, IOException, JSONException {
        BrokerManagerService brokerManagerService = mock(BrokerManagerService.class);
        BrokerInformation brokerInformationMock = new BrokerInformation();
        Map<String, String> brokerProps = new HashMap<>();
        brokerProps.put("AMQP", "5672");
        brokerInformationMock.setProperties(brokerProps);

        when(brokerManagerService.getBrokerInformation()).thenReturn(brokerInformationMock);

        andesRESTService.setBrokerManagerService(brokerManagerService);

        // Test valid broker information
        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpGet getRequest = new HttpGet(Constants.BASE_URL + "/information/broker");
        getRequest.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

        HttpResponse response = httpClient.execute(getRequest);

        HttpEntity responseEntity = response.getEntity();
        JSONObject jsonObject = new JSONObject();
        if (responseEntity != null) {
            jsonObject = new JSONObject(EntityUtils.toString(responseEntity));
        }
        Assert.assertTrue(jsonObject.getJSONObject("properties").length() == 1, "Unexpected cluster details " +
                                                                                "received");
        Assert.assertEquals(response.getStatusLine().getStatusCode(), Response.Status.OK.getStatusCode(),
                                                                                                    "200 not received");

        httpClient.getConnectionManager().shutdown();
    }

    /**
     * Testing whether valid destination types are received.
     *
     * @throws AndesException
     * @throws IOException
     * @throws JSONException
     */
    @Test(groups = {"wso2.mb", "rest"})
    public void validDestinationTypesTestCase() throws AndesException, IOException, JSONException {
        // Testing destination types
        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpGet getRequest = new HttpGet(Constants.BASE_URL + "/destination-types");
        getRequest.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

        HttpResponse response = httpClient.execute(getRequest);

        HttpEntity responseEntity = response.getEntity();
        JSONArray jsonArray = new JSONArray();
        if (responseEntity != null) {
            jsonArray = new JSONArray(EntityUtils.toString(responseEntity));
        }
        for (int i = 0; i < jsonArray.length(); i++) {
            // Check if valid destination types received.
            DestinationType.valueOf(jsonArray.getString(i).toUpperCase());
        }

        Assert.assertEquals(response.getStatusLine().getStatusCode(), Response.Status.OK.getStatusCode(),
                                                                                                    "200 not received");

        httpClient.getConnectionManager().shutdown();
    }

    /**
     * Tests whether 500 http code is received when an internal server error is occurred.
     *
     * @throws BrokerManagerException
     * @throws AndesException
     * @throws IOException
     * @throws JSONException
     */
    @Test(groups = {"wso2.mb", "rest"})
    public void throwBrokerManagerErrorTestCase() throws BrokerManagerException, AndesException, IOException,
            JSONException {
        BrokerManagerService brokerManagerService = mock(BrokerManagerService.class);
        when(brokerManagerService.getSupportedProtocols()).thenThrow(new BrokerManagerException("ProtoInfo - Error"));
        when(brokerManagerService.getClusterInformation()).thenThrow(new BrokerManagerException("ClusterInfo - Error"));
        when(brokerManagerService.getStoreInformation()).thenThrow(new BrokerManagerException("StoreInfo - Error"));
        when(brokerManagerService.getBrokerInformation()).thenThrow(new BrokerManagerException("BrokerInfo - Error"));

        andesRESTService.setBrokerManagerService(brokerManagerService);

        // Test protocols returning null
        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpGet getRequest = new HttpGet(Constants.BASE_URL + "/protocol-types");
        getRequest.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

        HttpResponse response = httpClient.execute(getRequest);
        validateExceptionHandling(response.getEntity());
        Assert.assertEquals(response.getStatusLine().getStatusCode(),
                                            Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "500 not received");

        // Test cluster information returning null
        httpClient = new DefaultHttpClient();
        getRequest = new HttpGet(Constants.BASE_URL + "/information/cluster");
        getRequest.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

        response = httpClient.execute(getRequest);
        validateExceptionHandling(response.getEntity());
        Assert.assertEquals(response.getStatusLine().getStatusCode(),
                                            Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "500 not received");

        // Test store information returning null
        httpClient = new DefaultHttpClient();
        getRequest = new HttpGet(Constants.BASE_URL + "/information/store");
        getRequest.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

        response = httpClient.execute(getRequest);
        validateExceptionHandling(response.getEntity());
        Assert.assertEquals(response.getStatusLine().getStatusCode(),
                                            Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "500 not received");

        // Test broker information returning null
        httpClient = new DefaultHttpClient();
        getRequest = new HttpGet(Constants.BASE_URL + "/information/broker");
        getRequest.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

        response = httpClient.execute(getRequest);
        validateExceptionHandling(response.getEntity());
        Assert.assertEquals(response.getStatusLine().getStatusCode(),
                                            Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "500 not received");

        httpClient.getConnectionManager().shutdown();
    }

    /**
     * Tests whether 500 http code is received when null is returned by the services.
     *
     * @throws BrokerManagerException
     * @throws AndesException
     * @throws IOException
     * @throws JSONException
     */
    @Test(groups = {"wso2.mb", "rest"})
    public void nullInformationTestCase() throws BrokerManagerException, AndesException, IOException,
            JSONException {
        BrokerManagerService brokerManagerService = mock(BrokerManagerService.class);
        when(brokerManagerService.getClusterInformation()).thenReturn(null);
        when(brokerManagerService.getStoreInformation()).thenReturn(null);
        when(brokerManagerService.getBrokerInformation()).thenReturn(null);

        andesRESTService.setBrokerManagerService(brokerManagerService);

        // Test cluster information returning null
        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpGet getRequest = new HttpGet(Constants.BASE_URL + "/information/cluster");
        getRequest.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

        HttpResponse response = httpClient.execute(getRequest);
        JSONObject jsonObject = new JSONObject(EntityUtils.toString(response.getEntity()));
        Assert.assertTrue(StringUtils.contains((String)jsonObject.get("message"),
                                    "Unable to find cluster information."), "Invalid cluster error message received.");
        Assert.assertEquals(response.getStatusLine().getStatusCode(),
                                            Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "500 not received");

        // Test store information returning null
        httpClient = new DefaultHttpClient();
        getRequest = new HttpGet(Constants.BASE_URL + "/information/store");
        getRequest.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

        response = httpClient.execute(getRequest);
        jsonObject = new JSONObject(EntityUtils.toString(response.getEntity()));
        Assert.assertTrue(StringUtils.contains((String)jsonObject.get("message"),
                                    "Unable to find store information."), "Invalid cluster error message received.");
        Assert.assertEquals(response.getStatusLine().getStatusCode(),
                                            Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "500 not received");

        // Test broker information returning null
        httpClient = new DefaultHttpClient();
        getRequest = new HttpGet(Constants.BASE_URL + "/information/broker");
        getRequest.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

        response = httpClient.execute(getRequest);
        jsonObject = new JSONObject(EntityUtils.toString(response.getEntity()));
        Assert.assertTrue(StringUtils.contains((String)jsonObject.get("message"),
                                    "Unable to find broker information."), "Invalid cluster error message received.");
        Assert.assertEquals(response.getStatusLine().getStatusCode(),
                                            Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "500 not received");

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
        Assert.assertTrue(null != jsonObject.get("title"), "Title for the error is missing.");
        Assert.assertTrue(null != jsonObject.get("code"), "Error code is missing.");
        Assert.assertTrue(null != jsonObject.get("message"), "A message is required for the error.");
    }
}
