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
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
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
import org.wso2.carbon.andes.rest.tests.Constants;
import org.wso2.carbon.andes.service.exceptions.DestinationManagerException;
import org.wso2.carbon.andes.service.exceptions.mappers.DestinationNotFoundMapper;
import org.wso2.carbon.andes.service.exceptions.mappers.InternalServerErrorMapper;
import org.wso2.carbon.andes.service.internal.AndesRESTService;
import org.wso2.carbon.andes.service.managers.DestinationManagerService;
import org.wso2.carbon.andes.service.types.Destination;
import org.wso2.carbon.andes.service.types.DestinationRolePermission;
import org.wso2.msf4j.MicroservicesRunner;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The following test case compromise validation of rest guidelines for destination permission related functions for the
 * REST service.
 */
public class DestinationPermissionTestCase {
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
     * Tests that a 500 http status code is received when an internal error while getting permissions for a
     * destination.
     *
     * @throws DestinationManagerException
     * @throws IOException
     * @throws JSONException
     */
    @Test(groups = {"wso2.mb", "rest"})
    public void getDestinationPermissionThrowDestinationManagerErrorTestCase() throws DestinationManagerException,
            IOException, JSONException {
        DestinationManagerService destinationManagerService = mock(DestinationManagerService.class);

        when(destinationManagerService.getDestination(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(mock(Destination.class));
        when(destinationManagerService.getDestinationPermissions(Mockito.anyString(), Mockito.anyString(), Mockito
                .anyString())).thenThrow(new DestinationManagerException("Internal Error"));

        andesRESTService.setDestinationManagerService(destinationManagerService);

        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpGet getRequest = new HttpGet(Constants.BASE_URL + "/amqp-0-91/permissions/destination-type/queue/name/Q-1");
        getRequest.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

        HttpResponse response = httpClient.execute(getRequest);

        validateExceptionHandling(response.getEntity());
        Assert.assertEquals(response.getStatusLine().getStatusCode(), Response.Status.INTERNAL_SERVER_ERROR
                .getStatusCode(), "500 not received");

        verify(destinationManagerService, atLeastOnce()).getDestinationPermissions(Mockito.anyString(), Mockito
                .anyString(), Mockito.anyString());

        httpClient.getConnectionManager().shutdown();
    }

    /**
     * Tests that a 404 http status code is received when getting permissions for a destination.
     *
     * @throws DestinationManagerException
     * @throws IOException
     * @throws JSONException
     */
    @Test(groups = {"wso2.mb", "rest"})
    public void getPermissionOnNullDestinationTestCase() throws DestinationManagerException, IOException,
            JSONException {
        DestinationManagerService destinationManagerService = mock(DestinationManagerService.class);

        when(destinationManagerService.getDestination(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(null);
        andesRESTService.setDestinationManagerService(destinationManagerService);

        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpGet getRequest = new HttpGet(Constants.BASE_URL + "/amqp-0-91/permissions/destination-type/queue/name/Q-1");
        getRequest.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

        HttpResponse response = httpClient.execute(getRequest);

        Assert.assertEquals(response.getStatusLine().getStatusCode(), Response.Status.NOT_FOUND.getStatusCode(),
                "404 not received");

        verify(destinationManagerService, atLeastOnce()).getDestination(Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString());

        httpClient.getConnectionManager().shutdown();
    }

    /**
     * Tests that a 204 http status code is received when getting permissions for a destination.
     *
     * @throws DestinationManagerException
     * @throws IOException
     * @throws JSONException
     */
    @Test(groups = {"wso2.mb", "rest"})
    public void validGetDestinationPermissionTestCase() throws DestinationManagerException, IOException, JSONException {
        DestinationManagerService destinationManagerService = mock(DestinationManagerService.class);
        Set<DestinationRolePermission> destinationRolePermissions = new HashSet<>();
        destinationRolePermissions.add(new DestinationRolePermission("abc", true, true));
        destinationRolePermissions.add(new DestinationRolePermission("xyz", false, true));

        when(destinationManagerService.getDestination(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(new Destination());
        when(destinationManagerService.getDestinationPermissions(Mockito.anyString(), Mockito.anyString(), Mockito
                .anyString())).thenReturn(destinationRolePermissions);

        andesRESTService.setDestinationManagerService(destinationManagerService);

        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpGet getRequest = new HttpGet(Constants.BASE_URL + "/amqp-0-91/permissions/destination-type/queue/name/Q-1");
        getRequest.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

        HttpResponse response = httpClient.execute(getRequest);

        Assert.assertEquals(response.getStatusLine().getStatusCode(), Response.Status.OK.getStatusCode(), "200 not "
                                                                                                          + "received");

        verify(destinationManagerService, atLeastOnce()).getDestinationPermissions(Mockito.anyString(), Mockito
                .anyString(), Mockito.anyString());

        httpClient.getConnectionManager().shutdown();
    }

    /**
     * Tests that a 500 http status code is received when an internal error while creating permissions for a
     * destination.
     *
     * @throws DestinationManagerException
     * @throws IOException
     * @throws JSONException
     */
    @Test(groups = {"wso2.mb", "rest"})
    public void createDestinationPermissionThrowDestinationManagerErrorTestCase() throws DestinationManagerException,
            IOException, JSONException {
        DestinationRolePermission destinationRolePermission = new DestinationRolePermission("abc", true, false);
        StringEntity destinationPermissionAsJsonString = new StringEntity(new ObjectMapper().writeValueAsString
                (destinationRolePermission));

        DestinationManagerService destinationManagerService = mock(DestinationManagerService.class);

        when(destinationManagerService.getDestination(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(mock(Destination.class));
        when(destinationManagerService.createDestinationPermission(Mockito.anyString(), Mockito.anyString(), Mockito
                .anyString(), Mockito.any())).thenThrow(new DestinationManagerException("Internal Error"));

        andesRESTService.setDestinationManagerService(destinationManagerService);

        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpPost postRequest = new HttpPost(Constants.BASE_URL +
                                            "/amqp-0-91/permissions/destination-type/queue/name/Q-1");
        postRequest.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        postRequest.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        postRequest.setEntity(destinationPermissionAsJsonString);
        HttpResponse response = httpClient.execute(postRequest);

        validateExceptionHandling(response.getEntity());
        Assert.assertEquals(response.getStatusLine().getStatusCode(), Response.Status.INTERNAL_SERVER_ERROR
                .getStatusCode(), "500 not received");

        verify(destinationManagerService, atLeastOnce()).createDestinationPermission(Mockito.anyString(), Mockito
                .anyString(), Mockito.anyString(), Mockito.any());

        httpClient.getConnectionManager().shutdown();
    }

    /**
     * Tests that a 404 http status code is received when creating permissions for a destination.
     *
     * @throws DestinationManagerException
     * @throws IOException
     * @throws JSONException
     */
    @Test(groups = {"wso2.mb", "rest"})
    public void createPermissionOnNullDestinationTestCase() throws DestinationManagerException, IOException,
            JSONException {
        DestinationRolePermission destinationRolePermission = new DestinationRolePermission("abc", true, false);
        StringEntity destinationPermissionAsJsonString = new StringEntity(new ObjectMapper().writeValueAsString
                (destinationRolePermission));

        DestinationManagerService destinationManagerService = mock(DestinationManagerService.class);

        when(destinationManagerService.getDestination(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(null);
        andesRESTService.setDestinationManagerService(destinationManagerService);

        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpPost postRequest = new HttpPost(Constants.BASE_URL +
                                            "/amqp-0-91/permissions/destination-type/queue/name/Q-1");
        postRequest.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        postRequest.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        postRequest.setEntity(destinationPermissionAsJsonString);
        HttpResponse response = httpClient.execute(postRequest);

        Assert.assertEquals(response.getStatusLine().getStatusCode(), Response.Status.NOT_FOUND.getStatusCode(),
                "404 not received");

        verify(destinationManagerService, atLeastOnce()).getDestination(Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString());

        httpClient.getConnectionManager().shutdown();
    }

    /**
     * Tests that a 204 http status code is received when creating permissions for a destination.
     *
     * @throws DestinationManagerException
     * @throws IOException
     * @throws JSONException
     */
    @Test(groups = {"wso2.mb", "rest"})
    public void validCreateDestinationPermissionTestCase() throws DestinationManagerException, IOException,
            JSONException {
        DestinationRolePermission destinationRolePermission = new DestinationRolePermission("abc", true, false);
        StringEntity destinationPermissionAsJsonString = new StringEntity(new ObjectMapper().writeValueAsString
                (destinationRolePermission));

        DestinationManagerService destinationManagerService = mock(DestinationManagerService.class);

        when(destinationManagerService.getDestination(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(new Destination());
        when(destinationManagerService.createDestinationPermission(Mockito.anyString(), Mockito.anyString(), Mockito
                .anyString(), Mockito.any())).thenReturn(destinationRolePermission);

        andesRESTService.setDestinationManagerService(destinationManagerService);

        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpPost postRequest = new HttpPost(Constants.BASE_URL +
                                            "/amqp-0-91/permissions/destination-type/queue/name/Q-1");
        postRequest.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        postRequest.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        postRequest.setEntity(destinationPermissionAsJsonString);
        HttpResponse response = httpClient.execute(postRequest);

        Assert.assertEquals(response.getStatusLine().getStatusCode(), Response.Status.OK.getStatusCode(), "200 not "
                                                                                                          + "received");

        verify(destinationManagerService, atLeastOnce()).createDestinationPermission(Mockito.anyString(), Mockito
                .anyString(), Mockito.anyString(), Mockito.any());

        httpClient.getConnectionManager().shutdown();
    }

    /**
     * Tests that a 500 http status code is received when an internal error while updating permission of a destination.
     *
     * @throws DestinationManagerException
     * @throws IOException
     * @throws JSONException
     */
    @Test(groups = {"wso2.mb", "rest"})
    public void updateDestinationPermissionThrowDestinationManagerErrorTestCase() throws DestinationManagerException,
            IOException, JSONException {
        DestinationRolePermission destinationRolePermission = new DestinationRolePermission("abc", true, false);
        StringEntity destinationPermissionAsJsonString = new StringEntity(new ObjectMapper().writeValueAsString
                (destinationRolePermission));

        DestinationManagerService destinationManagerService = mock(DestinationManagerService.class);

        when(destinationManagerService.getDestination(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(mock(Destination.class));
        when(destinationManagerService.updateDestinationPermission(Mockito.anyString(), Mockito.anyString(), Mockito
                .anyString(), Mockito.any())).thenThrow(new DestinationManagerException("Internal Error"));

        andesRESTService.setDestinationManagerService(destinationManagerService);

        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpPut putRequest = new HttpPut(Constants.BASE_URL + "/amqp-0-91/permissions/destination-type/queue/name/Q-1");
        putRequest.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        putRequest.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        putRequest.setEntity(destinationPermissionAsJsonString);
        HttpResponse response = httpClient.execute(putRequest);

        validateExceptionHandling(response.getEntity());
        Assert.assertEquals(response.getStatusLine().getStatusCode(), Response.Status.INTERNAL_SERVER_ERROR
                .getStatusCode(), "500 not received");

        verify(destinationManagerService, atLeastOnce()).updateDestinationPermission(Mockito.anyString(), Mockito
                .anyString(), Mockito.anyString(), Mockito.any());

        httpClient.getConnectionManager().shutdown();
    }

    /**
     * Tests that a 404 http status code is received when updating permission of a destination.
     *
     * @throws DestinationManagerException
     * @throws IOException
     * @throws JSONException
     */
    @Test(groups = {"wso2.mb", "rest"})
    public void updatePermissionOnNullDestinationTestCase() throws DestinationManagerException, IOException,
            JSONException {
        DestinationRolePermission destinationRolePermission = new DestinationRolePermission("abc", true, false);
        StringEntity destinationPermissionAsJsonString = new StringEntity(new ObjectMapper().writeValueAsString
                (destinationRolePermission));

        DestinationManagerService destinationManagerService = mock(DestinationManagerService.class);

        when(destinationManagerService.getDestination(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(null);
        andesRESTService.setDestinationManagerService(destinationManagerService);

        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpPut putRequest = new HttpPut(Constants.BASE_URL + "/amqp-0-91/permissions/destination-type/queue/name/Q-1");
        putRequest.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        putRequest.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        putRequest.setEntity(destinationPermissionAsJsonString);
        HttpResponse response = httpClient.execute(putRequest);

        Assert.assertEquals(response.getStatusLine().getStatusCode(), Response.Status.NOT_FOUND.getStatusCode(),
                "404 not received");

        verify(destinationManagerService, atLeastOnce()).getDestination(Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString());

        httpClient.getConnectionManager().shutdown();
    }

    /**
     * Tests that a 204 http status code is received when updating permission of a destination.
     *
     * @throws DestinationManagerException
     * @throws IOException
     * @throws JSONException
     */
    @Test(groups = {"wso2.mb", "rest"})
    public void validUpdateDestinationPermissionTestCase() throws DestinationManagerException, IOException,
            JSONException {
        DestinationRolePermission destinationRolePermission = new DestinationRolePermission("abc", true, false);
        StringEntity destinationPermissionAsJsonString = new StringEntity(new ObjectMapper().writeValueAsString
                (destinationRolePermission));

        DestinationManagerService destinationManagerService = mock(DestinationManagerService.class);

        when(destinationManagerService.getDestination(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(new Destination());
        when(destinationManagerService.updateDestinationPermission(Mockito.anyString(), Mockito.anyString(), Mockito
                .anyString(), Mockito.any())).thenReturn(destinationRolePermission);

        andesRESTService.setDestinationManagerService(destinationManagerService);

        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpPut putRequest = new HttpPut(Constants.BASE_URL + "/amqp-0-91/permissions/destination-type/queue/name/Q-1");
        putRequest.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        putRequest.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        putRequest.setEntity(destinationPermissionAsJsonString);
        HttpResponse response = httpClient.execute(putRequest);

        Assert.assertEquals(response.getStatusLine().getStatusCode(), Response.Status.OK.getStatusCode(), "200 not "
                                                                                                          + "received");

        verify(destinationManagerService, atLeastOnce()).updateDestinationPermission(Mockito.anyString(), Mockito
                .anyString(), Mockito.anyString(), Mockito.any());

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
}
