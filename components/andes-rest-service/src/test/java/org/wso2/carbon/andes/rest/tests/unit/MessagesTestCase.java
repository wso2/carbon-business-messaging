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
import org.apache.http.client.methods.HttpDelete;
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
import org.wso2.carbon.andes.service.exceptions.MessageManagerException;
import org.wso2.carbon.andes.service.exceptions.mappers.DestinationNotFoundMapper;
import org.wso2.carbon.andes.service.exceptions.mappers.InternalServerErrorMapper;
import org.wso2.carbon.andes.service.exceptions.mappers.MessageNotFoundMapper;
import org.wso2.carbon.andes.service.internal.AndesRESTService;
import org.wso2.carbon.andes.service.managers.DestinationManagerService;
import org.wso2.carbon.andes.service.managers.MessageManagerService;
import org.wso2.carbon.andes.service.types.Destination;
import org.wso2.carbon.andes.service.types.Message;
import org.wso2.msf4j.MicroservicesRunner;

import java.io.IOException;
import java.util.ArrayList;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The following test case compromise validation of rest guidelines for message related functions for the REST service.
 */
public class MessagesTestCase {
    private MicroservicesRunner microservicesRunner;
    private AndesRESTService andesRESTService;
    private DestinationNotFoundMapper destinationNotFoundMapper = new DestinationNotFoundMapper();
    private InternalServerErrorMapper internalServerErrorMapper = new InternalServerErrorMapper();
    private MessageNotFoundMapper messageNotFoundMapper = new MessageNotFoundMapper();
    
    /**
     * Initializes the tests.
     *
     * @throws AndesException
     */
    @BeforeClass
    public void setupService() throws AndesException {
        microservicesRunner = new MicroservicesRunner(Constants.PORT);
        andesRESTService = new AndesRESTService();
        microservicesRunner.addExceptionMapper(destinationNotFoundMapper,
                internalServerErrorMapper, messageNotFoundMapper).deploy(andesRESTService).start();
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
     * Tests that a 404 is received when an invalid destination is passed when browsing for messages.
     *
     * @throws AndesException
     * @throws IOException
     * @throws JSONException
     * @throws MessageManagerException
     * @throws DestinationManagerException
     */
    @Test(groups = {"wso2.mb", "rest"})
    public void getMessagesWithInvalidDestinationTestCase() throws AndesException, IOException, JSONException,
            MessageManagerException, DestinationManagerException {
        DestinationManagerService destinationManagerService = mock(DestinationManagerService.class);
        when(destinationManagerService.getDestination(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(null);
        
        andesRESTService.setDestinationManagerService(destinationManagerService);
        
        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpGet getRequest = new HttpGet(Constants.BASE_URL +
                                                            "/amqp-0-91/destination-type/queue/name/MyQueue/messages");
        getRequest.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        
        HttpResponse response = httpClient.execute(getRequest);
        
        validateExceptionHandling(response.getEntity());
        Assert.assertEquals(response.getStatusLine().getStatusCode(), Response.Status.NOT_FOUND.getStatusCode(), 
                                                                                                    "404 not received");
        
        verify(destinationManagerService, atLeastOnce()).getDestination(Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString());
        
        httpClient.getConnectionManager().shutdown();
    }
    
    /**
     * Tests that a 500 is received when an {@link DestinationManagerException} occurs while browsing messages.
     *
     * @throws AndesException
     * @throws IOException
     * @throws JSONException
     * @throws MessageManagerException
     * @throws DestinationManagerException
     */
    @Test(groups = {"wso2.mb", "rest"})
    public void getMessagesThrowDestinationManagerErrorTestCase() throws AndesException, IOException, JSONException,
            MessageManagerException, DestinationManagerException {
        DestinationManagerService destinationManagerService = mock(DestinationManagerService.class);
        when(destinationManagerService.getDestination(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenThrow(new DestinationManagerException("Internal Error"));
        
        andesRESTService.setDestinationManagerService(destinationManagerService);
        
        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpGet getRequest = new HttpGet(Constants.BASE_URL +
                                                            "/amqp-0-91/destination-type/queue/name/MyQueue/messages");
        getRequest.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        
        HttpResponse response = httpClient.execute(getRequest);
        
        validateExceptionHandling(response.getEntity());
        Assert.assertEquals(response.getStatusLine().getStatusCode(), 
                                            Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "500 not received");
        
        verify(destinationManagerService, atLeastOnce()).getDestination(Mockito.anyString(), Mockito.anyString(),
                                                                                                Mockito.anyString());
        
        httpClient.getConnectionManager().shutdown();
    }
    
    /**
     * * Tests that a 200 is received when requested for messages with default query params.
     *
     * @throws AndesException
     * @throws IOException
     * @throws JSONException
     * @throws MessageManagerException
     * @throws DestinationManagerException
     */
    @Test(groups = {"wso2.mb", "rest"})
    public void getMessagesWithNoQueryParamsTestCase() throws AndesException, IOException, JSONException,
            MessageManagerException, DestinationManagerException {
        DestinationManagerService destinationManagerService = mock(DestinationManagerService.class);
        when(destinationManagerService.getDestination(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(new Destination());
        
        MessageManagerService messageManagerService = mock(MessageManagerService.class);
        when(messageManagerService.getMessagesOfDestinationByMessageID(Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyBoolean(), Mockito.anyLong(), Mockito.anyInt()))
                .thenReturn(new ArrayList<>());
        
        andesRESTService.setDestinationManagerService(destinationManagerService);
        andesRESTService.setMessageManagerService(messageManagerService);
        
        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpGet getRequest = new HttpGet(Constants.BASE_URL +
                                                            "/amqp-0-91/destination-type/queue/name/MyQueue/messages");
        getRequest.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        
        HttpResponse response = httpClient.execute(getRequest);
        Assert.assertEquals(response.getStatusLine().getStatusCode(), Response.Status.OK.getStatusCode(), 
                                                                                                    "200 not received");
        
        verify(messageManagerService, atLeastOnce()).getMessagesOfDestinationByMessageID(Mockito.anyString(), 
                                                Mockito.anyString(), Mockito.anyString(), eq(false), eq(0L), eq(100));
        
        httpClient.getConnectionManager().shutdown();
    }
    
    /**
     * Tests that a 200 is received when requested for messages with content query param only.
     *
     * @throws AndesException
     * @throws IOException
     * @throws JSONException
     * @throws MessageManagerException
     * @throws DestinationManagerException
     */
    @Test(groups = {"wso2.mb", "rest"})
    public void getMessagesWithContentOnlyTestCase() throws AndesException, IOException, JSONException,
            MessageManagerException, DestinationManagerException {
        DestinationManagerService destinationManagerService = mock(DestinationManagerService.class);
        when(destinationManagerService.getDestination(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(new Destination());
        MessageManagerService messageManagerService = mock(MessageManagerService.class);
        when(messageManagerService.getMessagesOfDestinationByMessageID(Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyBoolean(), Mockito.anyLong(), Mockito.anyInt()))
                .thenReturn(new ArrayList<>());
        
        andesRESTService.setDestinationManagerService(destinationManagerService);
        andesRESTService.setMessageManagerService(messageManagerService);
        
        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpGet getRequest = new HttpGet(Constants.BASE_URL +
                                            "/amqp-0-91/destination-type/queue/name/MyQueue/messages?content=true");
        getRequest.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        
        HttpResponse response = httpClient.execute(getRequest);
        Assert.assertEquals(response.getStatusLine().getStatusCode(), Response.Status.OK.getStatusCode(), 
                                                                                                    "200 not received");
        
        verify(messageManagerService, atLeastOnce()).getMessagesOfDestinationByMessageID(Mockito.anyString(), 
                            Mockito.anyString(), Mockito.anyString(), eq(true), Mockito.anyLong(), Mockito.anyInt());
        
        httpClient.getConnectionManager().shutdown();
    }
    
    /**
     * Tests that a 200 is received when requested for messages with message ID query param only.
     *
     * @throws AndesException
     * @throws IOException
     * @throws JSONException
     * @throws MessageManagerException
     * @throws DestinationManagerException
     */
    @Test(groups = {"wso2.mb", "rest"})
    public void getMessagesWithMessageIDTestCase() throws AndesException, IOException, JSONException,
            MessageManagerException, DestinationManagerException {
        DestinationManagerService destinationManagerService = mock(DestinationManagerService.class);
        when(destinationManagerService.getDestination(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(new Destination());
        
        MessageManagerService messageManagerService = mock(MessageManagerService.class);
        when(messageManagerService.getMessagesOfDestinationByMessageID(Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyBoolean(), Mockito.anyLong(), Mockito.anyInt()))
                .thenReturn(new ArrayList<>());
        
        andesRESTService.setDestinationManagerService(destinationManagerService);
        andesRESTService.setMessageManagerService(messageManagerService);
        
        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpGet getRequest = new HttpGet(Constants.BASE_URL +
                                     "/amqp-0-91/destination-type/queue/name/mq1/messages?next-message-id=12345678");
        getRequest.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        
        HttpResponse response = httpClient.execute(getRequest);
        Assert.assertEquals(response.getStatusLine().getStatusCode(), Response.Status.OK.getStatusCode(), 
                                                                                                    "200 not received");
        
        verify(messageManagerService, atLeastOnce()).getMessagesOfDestinationByMessageID(Mockito.anyString(), 
                    Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean(), eq(12345678L), Mockito.anyInt());
        
        httpClient.getConnectionManager().shutdown();
    }
    
    /**
     * Tests that a 200 is received when requested for messages with limit query param only.
     *
     * @throws AndesException
     * @throws IOException
     * @throws JSONException
     * @throws MessageManagerException
     * @throws DestinationManagerException
     */
    @Test(groups = {"wso2.mb", "rest"})
    public void getMessagesWithLimitTestCase() throws AndesException, IOException, JSONException,
            MessageManagerException, DestinationManagerException {
        DestinationManagerService destinationManagerService = mock(DestinationManagerService.class);
        when(destinationManagerService.getDestination(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(new Destination());
        
        MessageManagerService messageManagerService = mock(MessageManagerService.class);
        when(messageManagerService.getMessagesOfDestinationByMessageID(Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyBoolean(), Mockito.anyLong(), Mockito.anyInt()))
                .thenReturn(new ArrayList<>());
        
        andesRESTService.setDestinationManagerService(destinationManagerService);
        andesRESTService.setMessageManagerService(messageManagerService);
        
        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpGet getRequest = new HttpGet(Constants.BASE_URL +
                                                    "/amqp-0-91/destination-type/queue/name/mq1/messages?limit=50000");
        getRequest.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        
        HttpResponse response = httpClient.execute(getRequest);
        Assert.assertEquals(response.getStatusLine().getStatusCode(), Response.Status.OK.getStatusCode(), 
                                                                                                    "200 not received");
        
        verify(messageManagerService, atLeastOnce()).getMessagesOfDestinationByMessageID(Mockito.anyString(), 
                        Mockito.anyString(), Mockito.anyString(), Mockito.anyBoolean(), Mockito.anyLong(), eq(50000));
        
        httpClient.getConnectionManager().shutdown();
    }
    
    /**
     * Tests that a 200 is received when a requested for messages with all query params.
     *
     * @throws AndesException
     * @throws IOException
     * @throws JSONException
     * @throws MessageManagerException
     * @throws DestinationManagerException
     */
    @Test(groups = {"wso2.mb", "rest"})
    public void getMessagesWithAllQueryParamsTestCase() throws AndesException, IOException, JSONException,
            MessageManagerException, DestinationManagerException {
        DestinationManagerService destinationManagerService = mock(DestinationManagerService.class);
        when(destinationManagerService.getDestination(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(new Destination());
        
        MessageManagerService messageManagerService = mock(MessageManagerService.class);
        when(messageManagerService.getMessagesOfDestinationByMessageID(Mockito.anyString(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyBoolean(), Mockito.anyLong(), Mockito.anyInt()))
                .thenReturn(new ArrayList<>());
        
        andesRESTService.setDestinationManagerService(destinationManagerService);
        andesRESTService.setMessageManagerService(messageManagerService);
        
        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpGet getRequest = new HttpGet(Constants.BASE_URL +
             "/amqp-0-91/destination-type/queue/name/mq1/messages?next-message-id=12345678&limit=7000&content=true");
        getRequest.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        
        HttpResponse response = httpClient.execute(getRequest);
        Assert.assertEquals(response.getStatusLine().getStatusCode(), Response.Status.OK.getStatusCode(), 
                                                                                                    "200 not received");
        
        verify(messageManagerService, atLeastOnce()).getMessagesOfDestinationByMessageID(Mockito.anyString(), 
                                        Mockito.anyString(), Mockito.anyString(), eq(true), eq(12345678L), eq(7000));
        
        httpClient.getConnectionManager().shutdown();
    }
    
    /**
     * Tests that a 200 is received when requested for a message with default query params.
     *
     * @throws MessageManagerException
     * @throws AndesException
     * @throws IOException
     * @throws DestinationManagerException
     */
    @Test(groups = {"wso2.mb", "rest"})
    public void getMessageWithDefaultQueryParamsTestCase() throws MessageManagerException, AndesException,
            IOException, DestinationManagerException {
        DestinationManagerService destinationManagerService = mock(DestinationManagerService.class);
        when(destinationManagerService.getDestination(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(new Destination());
        
        MessageManagerService messageManagerService = mock(MessageManagerService.class);
        Message messageMock = new Message();
        messageMock.setAndesMsgMetadataId(789789L);
        when(messageManagerService.getMessage(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), 
                                                    Mockito.anyLong(), Mockito.anyBoolean())).thenReturn(messageMock);
        
        andesRESTService.setDestinationManagerService(destinationManagerService);
        andesRESTService.setMessageManagerService(messageManagerService);
        
        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpGet getRequest = new HttpGet(Constants.BASE_URL +
                                                        "/amqp-0-91/destination-type/queue/name/mq1/messages/789789");
        getRequest.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        
        HttpResponse response = httpClient.execute(getRequest);
        Assert.assertEquals(response.getStatusLine().getStatusCode(), Response.Status.OK.getStatusCode(), 
                                                                                                    "200 not received");
        
        verify(messageManagerService, atLeastOnce()).getMessage(Mockito.anyString(), Mockito.anyString(), 
                                                                        Mockito.anyString(), eq(789789L), eq(false));
        
        httpClient.getConnectionManager().shutdown();
    }
    
    /**
     * Test that a 200 is received when requested for a message without content.
     *
     * @throws MessageManagerException
     * @throws AndesException
     * @throws IOException
     * @throws DestinationManagerException
     */
    @Test(groups = {"wso2.mb", "rest"})
    public void getMessageWithoutContentTestCase() throws MessageManagerException, AndesException, IOException,
            DestinationManagerException {
        DestinationManagerService destinationManagerService = mock(DestinationManagerService.class);
        when(destinationManagerService.getDestination(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(new Destination());
        
        MessageManagerService messageManagerService = mock(MessageManagerService.class);
        when(messageManagerService.getMessage(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), 
                                                Mockito.anyLong(), Mockito.anyBoolean())).thenReturn(new Message());
        
        andesRESTService.setDestinationManagerService(destinationManagerService);
        andesRESTService.setMessageManagerService(messageManagerService);
        
        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpGet getRequest = new HttpGet(Constants.BASE_URL +
                                         "/amqp-0-91/destination-type/queue/name/mq1/messages/789789?content=false");
        getRequest.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        
        HttpResponse response = httpClient.execute(getRequest);
        Assert.assertEquals(response.getStatusLine().getStatusCode(), Response.Status.OK.getStatusCode(), 
                                                                                                    "200 not received");
        
        verify(messageManagerService, atLeastOnce()).getMessage(Mockito.anyString(), Mockito.anyString(), 
                                                                        Mockito.anyString(), eq(789789L), eq(false));
        
        httpClient.getConnectionManager().shutdown();
    }
    
    /**
     * Test that a 200 is received when requested for a message with content.
     *
     * @throws MessageManagerException
     * @throws AndesException
     * @throws IOException
     * @throws DestinationManagerException
     */
    @Test(groups = {"wso2.mb", "rest"})
    public void getMessageWithContentTestCase() throws MessageManagerException, AndesException, IOException,
            DestinationManagerException {
        DestinationManagerService destinationManagerService = mock(DestinationManagerService.class);
        when(destinationManagerService.getDestination(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(new Destination());
        
        MessageManagerService messageManagerService = mock(MessageManagerService.class);
        Message messageMock = new Message();
        messageMock.setAndesMsgMetadataId(789789L);
        when(messageManagerService.getMessage(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), 
                                                    Mockito.anyLong(), Mockito.anyBoolean())).thenReturn(messageMock);
        
        andesRESTService.setDestinationManagerService(destinationManagerService);
        andesRESTService.setMessageManagerService(messageManagerService);
        
        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpGet getRequest = new HttpGet(Constants.BASE_URL +
                                         "/amqp-0-91/destination-type/queue/name/mq1/messages/789789?content=true");
        getRequest.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        
        HttpResponse response = httpClient.execute(getRequest);
        Assert.assertEquals(response.getStatusLine().getStatusCode(), Response.Status.OK.getStatusCode(), 
                                                                                                    "200 not received");
        
        verify(messageManagerService, atLeastOnce()).getMessage(Mockito.anyString(), Mockito.anyString(), 
                                                                            Mockito.anyString(), eq(789789L), eq(true));
        
        httpClient.getConnectionManager().shutdown();
    }
    
    /**
     * Tests that a 404 is received when requested for a missing message.
     *
     * @throws MessageManagerException
     * @throws AndesException
     * @throws IOException
     * @throws DestinationManagerException
     * @throws JSONException
     */
    @Test(groups = {"wso2.mb", "rest"})
    public void getNonExistingMessageTestCase() throws MessageManagerException, AndesException, IOException,
            DestinationManagerException, JSONException {
        DestinationManagerService destinationManagerService = mock(DestinationManagerService.class);
        when(destinationManagerService.getDestination(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(new Destination());
        
        MessageManagerService messageManagerService = mock(MessageManagerService.class);
        when(messageManagerService.getMessage(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), 
                                                            Mockito.anyLong(), Mockito.anyBoolean())).thenReturn(null);
        
        andesRESTService.setDestinationManagerService(destinationManagerService);
        andesRESTService.setMessageManagerService(messageManagerService);
        
        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpGet getRequest = new HttpGet(Constants.BASE_URL +
                                                        "/amqp-0-91/destination-type/queue/name/mq1/messages/789789");
        getRequest.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        
        HttpResponse response = httpClient.execute(getRequest);
        
        validateExceptionHandling(response.getEntity());
        Assert.assertEquals(response.getStatusLine().getStatusCode(), Response.Status.NOT_FOUND.getStatusCode(), 
                                                                                                    "404 not received");
        
        verify(messageManagerService, atLeastOnce()).getMessage(Mockito.anyString(), Mockito.anyString(), 
                                                                        Mockito.anyString(), eq(789789L), eq(false));
        
        httpClient.getConnectionManager().shutdown();
    }
    
    /**
     * Tests that a 404 is received when an invalid destination is passed when getting a messages.
     *
     * @throws AndesException
     * @throws IOException
     * @throws JSONException
     * @throws MessageManagerException
     * @throws DestinationManagerException
     */
    @Test(groups = {"wso2.mb", "rest"})
    public void getMessageWithInvalidDestinationTestCase() throws AndesException, IOException, JSONException,
            MessageManagerException, DestinationManagerException {
        DestinationManagerService destinationManagerService = mock(DestinationManagerService.class);
        when(destinationManagerService.getDestination(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(null);
        
        andesRESTService.setDestinationManagerService(destinationManagerService);
        
        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpGet getRequest = new HttpGet(Constants.BASE_URL +
                                                    "/amqp-0-91/destination-type/queue/name/MyQueue/messages/789789");
        getRequest.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        
        HttpResponse response = httpClient.execute(getRequest);
        
        validateExceptionHandling(response.getEntity());
        Assert.assertEquals(response.getStatusLine().getStatusCode(), Response.Status.NOT_FOUND.getStatusCode(), 
                                                                                                    "404 not received");
        
        verify(destinationManagerService, atLeastOnce()).getDestination(Mockito.anyString(), Mockito.anyString(),
                                                                                                Mockito.anyString());
        
        httpClient.getConnectionManager().shutdown();
    }
    
    /**
     * Tests that a 500 is received when an {@link DestinationManagerException} occurs while getting a message.
     *
     * @throws MessageManagerException
     * @throws AndesException
     * @throws IOException
     * @throws DestinationManagerException
     * @throws JSONException
     */
    @Test(groups = {"wso2.mb", "rest"})
    public void getMessageThrowDestinationManagerErrorTestCase() throws MessageManagerException, AndesException,
            IOException, DestinationManagerException, JSONException {
        DestinationManagerService destinationManagerService = mock(DestinationManagerService.class);
        when(destinationManagerService.getDestination(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenThrow(new DestinationManagerException("Destination Manager Error"));
        
        andesRESTService.setDestinationManagerService(destinationManagerService);
        
        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpGet getRequest = new HttpGet(Constants.BASE_URL +
                                                        "/amqp-0-91/destination-type/queue/name/mq1/messages/789789");
        getRequest.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        
        HttpResponse response = httpClient.execute(getRequest);
        
        validateExceptionHandling(response.getEntity());
        Assert.assertEquals(response.getStatusLine().getStatusCode(), 
                                            Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "500 not received");
        
        verify(destinationManagerService, atLeastOnce()).getDestination(Mockito.anyString(), Mockito.anyString(),
                                                                                                Mockito.anyString());
        
        httpClient.getConnectionManager().shutdown();
    }
    
    /**
     * Tests that a 500 is received when an {@link MessageManagerException} occurs while getting a message.
     *
     * @throws MessageManagerException
     * @throws AndesException
     * @throws IOException
     * @throws DestinationManagerException
     * @throws JSONException
     */
    @Test(groups = {"wso2.mb", "rest"})
    public void getMessageThrowMessageManagerErrorTestCase() throws MessageManagerException, AndesException,
            IOException, DestinationManagerException, JSONException {
        DestinationManagerService destinationManagerService = mock(DestinationManagerService.class);
        when(destinationManagerService.getDestination(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(new Destination());
        
        MessageManagerService messageManagerService = mock(MessageManagerService.class);
        when(messageManagerService.getMessage(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), 
                Mockito.anyLong(), Mockito.anyBoolean()))
                .thenThrow(new MessageManagerException("Message Manager Error"));
        
        andesRESTService.setDestinationManagerService(destinationManagerService);
        andesRESTService.setMessageManagerService(messageManagerService);
        
        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpGet getRequest = new HttpGet(Constants.BASE_URL +
                                                        "/amqp-0-91/destination-type/queue/name/mq1/messages/789789");
        getRequest.addHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        
        HttpResponse response = httpClient.execute(getRequest);
        
        validateExceptionHandling(response.getEntity());
        Assert.assertEquals(response.getStatusLine().getStatusCode(), 
                                            Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "500 not received");
        
        verify(messageManagerService, atLeastOnce()).getMessage(Mockito.anyString(), Mockito.anyString(), 
                                                        Mockito.anyString(), Mockito.anyLong(), Mockito.anyBoolean());
        
        httpClient.getConnectionManager().shutdown();
    }
    
    /**
     * Tests that a 404 is received when an invalid destination is passed for purging messages.
     *
     * @throws DestinationManagerException
     * @throws IOException
     * @throws JSONException
     */
    @Test(groups = {"wso2.mb", "rest"})
    public void purgeMessagesWithInvalidDestinationTestCase() throws DestinationManagerException, IOException,
            JSONException {
        DestinationManagerService destinationManagerService = mock(DestinationManagerService.class);
        when(destinationManagerService.getDestination(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(null);
        
        andesRESTService.setDestinationManagerService(destinationManagerService);
        
        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpDelete deleteRequest = new HttpDelete(Constants.BASE_URL +
                                                  "/amqp-0-91/destination-type/queue/name/MyQueue/messages");
        
        HttpResponse response = httpClient.execute(deleteRequest);
        
        validateExceptionHandling(response.getEntity());
        Assert.assertEquals(response.getStatusLine().getStatusCode(), 
                                                        Response.Status.NOT_FOUND.getStatusCode(), "404 not received");
        
        verify(destinationManagerService, atLeastOnce()).getDestination(Mockito.anyString(), Mockito.anyString(),
                                                                                                Mockito.anyString());
        
        httpClient.getConnectionManager().shutdown();
    }
    
    /**
     * Tests that a 500 is received when an {@link DestinationManagerException} occurs while purging messages.
     *
     * @throws DestinationManagerException
     * @throws IOException
     * @throws JSONException
     */
    @Test(groups = {"wso2.mb", "rest"})
    public void purgeMessagesThrowDestinationManagerErrorTestCase() throws DestinationManagerException, IOException,
            JSONException {
        DestinationManagerService destinationManagerService = mock(DestinationManagerService.class);
        when(destinationManagerService.getDestination(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenThrow(new DestinationManagerException("Destination Manager Error"));
        
        andesRESTService.setDestinationManagerService(destinationManagerService);
        
        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpDelete deleteRequest = new HttpDelete(Constants.BASE_URL +
                                                                "/amqp-0-91/destination-type/queue/name/mq1/messages");
        
        HttpResponse response = httpClient.execute(deleteRequest);
        
        validateExceptionHandling(response.getEntity());
        Assert.assertEquals(response.getStatusLine().getStatusCode(), 
                                            Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), "500 not received");
        
        verify(destinationManagerService, atLeastOnce()).getDestination(Mockito.anyString(), Mockito.anyString(),
                                                                                                Mockito.anyString());
        
        httpClient.getConnectionManager().shutdown();
    }
    
    /**
     * Tests whether 204 error code is received when a destination is purged.
     *
     * @throws DestinationManagerException
     * @throws MessageManagerException
     * @throws IOException
     */
    @Test(groups = {"wso2.mb", "rest"})
    public void purgeMessagesTestCase() throws DestinationManagerException, MessageManagerException, IOException {
        DestinationManagerService destinationManagerService = mock(DestinationManagerService.class);
        when(destinationManagerService.getDestination(Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(new Destination());
        
        MessageManagerService messageManagerService = mock(MessageManagerService.class);
        
        andesRESTService.setDestinationManagerService(destinationManagerService);
        andesRESTService.setMessageManagerService(messageManagerService);
        
        DefaultHttpClient httpClient = new DefaultHttpClient();
        HttpDelete deleteRequest = new HttpDelete(Constants.BASE_URL +
                                                  "/amqp-0-91/destination-type/queue/name/mq1/messages");
        
        HttpResponse response = httpClient.execute(deleteRequest);
        Assert.assertEquals(response.getStatusLine().getStatusCode(), 
                                                    Response.Status.NO_CONTENT.getStatusCode(), "204 not received");
        
        verify(messageManagerService, atLeastOnce()).deleteMessages(Mockito.anyString(), Mockito.anyString(), 
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
        Assert.assertTrue(null != jsonObject.get("title"), "Title for the error is missing.");
        Assert.assertTrue(null != jsonObject.get("code"), "Error code is missing.");
        Assert.assertTrue(null != jsonObject.get("message"), "A message is required for the error.");
    }
}
