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

import org.apache.http.HttpEntity;
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
import org.wso2.carbon.andes.core.AndesConstants;
import org.wso2.carbon.andes.core.AndesException;
import org.wso2.carbon.andes.core.ProtocolType;
import org.wso2.carbon.andes.rest.tests.Constants;
import org.wso2.carbon.andes.service.exceptions.DestinationManagerException;
import org.wso2.carbon.andes.service.exceptions.mappers.DLCQueueNotFoundMapper;
import org.wso2.carbon.andes.service.exceptions.mappers.DestinationNotFoundMapper;
import org.wso2.carbon.andes.service.exceptions.mappers.InternalServerErrorMapper;
import org.wso2.carbon.andes.service.internal.AndesRESTService;
import org.wso2.carbon.andes.service.managers.DestinationManagerService;
import org.wso2.carbon.andes.service.types.Destination;
import org.wso2.msf4j.MicroservicesRunner;

import java.io.IOException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The following test case compromise validation of rest guidelines for DLC related functions for the REST service.
 */
public class DLCTestCase {
    private MicroservicesRunner microservicesRunner;
    private AndesRESTService andesRESTService;
    private DestinationNotFoundMapper destinationNotFoundMapper = new DestinationNotFoundMapper();
    private InternalServerErrorMapper internalServerErrorMapper = new InternalServerErrorMapper();
    private DLCQueueNotFoundMapper dlcQueueNotFoundMapper = new DLCQueueNotFoundMapper();
    private ProtocolType dlcProtocol;

    /**
     * Initializes the tests.
     *
     * @throws AndesException
     */
    @BeforeClass
    public void setupService() throws AndesException {
        dlcProtocol = new ProtocolType(AndesConstants.DLC_PROTOCOL_NAME, AndesConstants.DLC_PROTOCOL_VERSION);
        microservicesRunner = new MicroservicesRunner(Constants.PORT);
        andesRESTService = new AndesRESTService();
        microservicesRunner.addExceptionMapper(destinationNotFoundMapper, internalServerErrorMapper,
                dlcQueueNotFoundMapper).deploy(andesRESTService).start();
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
     * Tests that a 500 http status code is received when an internal error while getting dlc destination.
     *
     * @throws DestinationManagerException
     * @throws IOException
     * @throws JSONException
     */
    @Test(groups = {"wso2.mb", "rest"})
    public void getDLCQueueThrowDestinationManagerErrorTestCase() throws DestinationManagerException, IOException,
            JSONException {
        DestinationManagerService destinationManagerService = mock(DestinationManagerService.class);

        when(destinationManagerService.getDestination(eq(dlcProtocol.toString()), eq(AndesConstants
                .DLC_DESTINATION_TYPE.toString()), Mockito.anyString())).thenThrow(new DestinationManagerException
                ("Internal Error"));

        andesRESTService.setDestinationManagerService(destinationManagerService);

        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpGet getRequest = new HttpGet(Constants.BASE_URL + "/dlc");
        getRequest.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

        HttpResponse response = httpClient.execute(getRequest);

        validateExceptionHandling(response.getEntity());
        Assert.assertEquals(response.getStatusLine().getStatusCode(), Response.Status.INTERNAL_SERVER_ERROR
                .getStatusCode(), "500 not received");

        verify(destinationManagerService, atLeastOnce()).getDestination(eq(dlcProtocol.toString()), eq(AndesConstants
                .DLC_DESTINATION_TYPE.toString()), Mockito.anyString());

        httpClient.getConnectionManager().shutdown();
    }

    /**
     * Tests that a 404 http status code is received when dlc does not exists.
     *
     * @throws DestinationManagerException
     * @throws IOException
     * @throws JSONException
     */
    @Test(groups = {"wso2.mb", "rest"})
    public void getNullDLCQueueTestCase() throws DestinationManagerException, IOException, JSONException {
        DestinationManagerService destinationManagerService = mock(DestinationManagerService.class);

        when(destinationManagerService.getDestination(eq(dlcProtocol.toString()), eq(AndesConstants
                .DLC_DESTINATION_TYPE.toString()), Mockito.anyString())).thenReturn(null);
        andesRESTService.setDestinationManagerService(destinationManagerService);

        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpGet getRequest = new HttpGet(Constants.BASE_URL + "/dlc");
        getRequest.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

        HttpResponse response = httpClient.execute(getRequest);

        JSONObject jsonObject = new JSONObject(EntityUtils.toString(response.getEntity()));
        Assert.assertTrue(((String) jsonObject.get("message")).contains("DLC does not exist for the user"),
                "DLC queue not found message is invalid.");

        Assert.assertEquals(response.getStatusLine().getStatusCode(), Response.Status.NOT_FOUND.getStatusCode(),
                "404 not received");

        verify(destinationManagerService, atLeastOnce()).getDestination(eq(dlcProtocol.toString()), eq(AndesConstants
                .DLC_DESTINATION_TYPE.toString()), Mockito.anyString());

        httpClient.getConnectionManager().shutdown();
    }

    /**
     * Tests that a 200 http status code is received when dlc queue exists.
     *
     * @throws DestinationManagerException
     * @throws IOException
     * @throws JSONException
     */
    @Test(groups = {"wso2.mb", "rest"})
    public void validGetDLCQueueTestCase() throws DestinationManagerException, IOException, JSONException {
        DestinationManagerService destinationManagerService = mock(DestinationManagerService.class);

        when(destinationManagerService.getDestination(eq(dlcProtocol.toString()), eq(AndesConstants
                .DLC_DESTINATION_TYPE.toString()), Mockito.anyString())).thenReturn(new Destination());

        andesRESTService.setDestinationManagerService(destinationManagerService);

        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpGet getRequest = new HttpGet(Constants.BASE_URL + "/dlc");
        getRequest.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);

        HttpResponse response = httpClient.execute(getRequest);

        Assert.assertEquals(response.getStatusLine().getStatusCode(), Response.Status.OK.getStatusCode(),
                "200 not received");

        verify(destinationManagerService, atLeastOnce()).getDestination(eq(dlcProtocol.toString()), eq(AndesConstants
                .DLC_DESTINATION_TYPE.toString()), Mockito.anyString());

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
