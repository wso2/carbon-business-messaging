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

package org.wso2.carbon.business.messaging.admin.services.tests.unit;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.carbon.business.messaging.admin.services.exceptions.InternalServerException;
import org.wso2.carbon.business.messaging.admin.services.exceptions.mappers.InternalServerErrorMapper;
import org.wso2.carbon.business.messaging.admin.services.internal.MbRestService;
import org.wso2.carbon.business.messaging.admin.services.managers.BrokerManagerService;
import org.wso2.carbon.business.messaging.admin.services.types.Protocols;
import org.wso2.msf4j.MicroservicesRunner;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
    private MbRestService mbRESTService;

    private InternalServerErrorMapper internalServerErrorMapper = new InternalServerErrorMapper();

    /**
     * Initializes the tests.
     */
    @BeforeClass
    public void setupService() {
        microservicesRunner = new MicroservicesRunner(Constants.PORT);
        mbRESTService = new MbRestService();
        microservicesRunner.addExceptionMapper(internalServerErrorMapper).deploy(mbRESTService).start();
    }

    /**
     * Cleans up the microservice runner and the andes service.
     */
    @AfterClass
    public void cleanUpServices() {
        microservicesRunner.stop();
        if (microservicesRunner.getMsRegistry().getHttpServices().contains(mbRESTService)) {
            microservicesRunner.getMsRegistry().removeService(mbRESTService);
        }
        mbRESTService = null;
        microservicesRunner = null;
    }

    /**
     * Test /protocol-types API method whether it contains the required protocols
     *
     * @throws IOException
     * @throws InterruptedException
     */
    @Test(groups = { "wso2.mb", "rest" })
    public void protocolTypesTestCase() throws IOException, InterruptedException, InternalServerException {
        // Creation of mocking objects
        BrokerManagerService brokerManagerService = mock(BrokerManagerService.class);
        List<String> protocolsList = new ArrayList<>();
        protocolsList.add("amqp-v0.91");

        Protocols protocols = new Protocols();
        protocols.setProtocol(protocolsList);
        when(brokerManagerService.getSupportedProtocols()).thenReturn(protocols);

        mbRESTService.setBrokerManagerService(brokerManagerService);

        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpGet getRequest = new HttpGet(Constants.BASE_URL + "/protocol-types");
        getRequest.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        HttpResponse response = httpClient.execute(getRequest);

        HttpEntity responseEntity = response.getEntity();
        String content = StringUtils.EMPTY;
        if (responseEntity != null) {
            content = EntityUtils.toString(responseEntity);
        }
        Assert.assertTrue(content != null && content.contains("amqp-v0.91"),
                "Unexpected protocols");
        Assert.assertEquals(response.getStatusLine().getStatusCode(), Response.Status.OK.getStatusCode(),
                "200 not received");

        httpClient.getConnectionManager().shutdown();
    }
}
