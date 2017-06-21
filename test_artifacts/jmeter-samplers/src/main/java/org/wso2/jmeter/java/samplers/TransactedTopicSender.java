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

package org.wso2.jmeter.java.samplers;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.threads.JMeterVariables;
import org.apache.jorphan.logging.LoggingManager;
import org.apache.log.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;
import javax.jms.JMSException;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicSession;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * This is Topic Sender Jmeter Sampler class for local transactions.
 */
public class TransactedTopicSender extends AbstractJavaSamplerClient {
    private static final Logger log = LoggingManager.getLoggerForClass();

    private String userName;
    private String password;
    private String topicName;
    private String message;
    private int msgCount;
    private String cfName;
    private Properties properties;

    private TopicConnection topicConnection;
    private TopicSession topicSession;

    // set up default arguments for the JMeter GUI
    @Override
    public Arguments getDefaultParameters() {
        Arguments defaultParameters = new Arguments();
        defaultParameters.addArgument("QPID_ICF", "org.wso2.andes.jndi.PropertiesFileInitialContextFactory");
        defaultParameters.addArgument("CF_NAME_PREFIX", "connectionfactory.");
        defaultParameters.addArgument("TOPIC_NAME_PREFIX", "topic.");
        defaultParameters.addArgument("CF_NAME", "qpidConnectionfactory");
        defaultParameters.addArgument("userName", "admin");
        defaultParameters.addArgument("password", "admin");
        defaultParameters.addArgument("topicName", "testTopic");
        defaultParameters.addArgument("CARBON_CLIENT_ID", "carbon");
        defaultParameters.addArgument("CARBON_VIRTUAL_HOST_NAME", "carbon");
        defaultParameters.addArgument("CARBON_DEFAULT_HOSTNAME", "localhost");
        defaultParameters.addArgument("CARBON_DEFAULT_PORT", "5672");
        defaultParameters.addArgument("File path", " ");
        defaultParameters.addArgument("Message count per commit", "1");

        return defaultParameters;
    }

    @Override
    public void setupTest(JavaSamplerContext context) {
        userName = context.getParameter("userName");
        password = context.getParameter("password");
        topicName = context.getParameter("topicName");
        String filepath = context.getParameter("File path");
        try {
            message = new String(Files.readAllBytes(Paths.get(filepath)), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        msgCount = context.getIntParameter("Message count per commit");
        cfName = context.getParameter("CF_NAME");
        properties = new Properties();
        properties.put(Context.INITIAL_CONTEXT_FACTORY, context.getParameter("QPID_ICF"));
        properties
                .put(context.getParameter("CF_NAME_PREFIX") + cfName, getTCPConnectionURL(userName, password, context));
        properties.put(context.getParameter("TOPIC_NAME_PREFIX") + topicName, topicName);
    }

    @Override
    public SampleResult runTest(JavaSamplerContext context) {
        SampleResult result = new SampleResult();
        result.sampleStart();
        result.setSampleLabel("Transacted Topic Sender");
        try {
            JMeterVariables vars = JMeterContextService.getContext().getVariables();
            InitialContext ctx = new InitialContext(properties);

            if (null == vars.getObject("topicSession")) {
                // Lookup connection factory
                TopicConnectionFactory connFactory = (TopicConnectionFactory) ctx.lookup(cfName);
                topicConnection = connFactory.createTopicConnection();
                topicConnection.start();
                topicSession = topicConnection.createTopicSession(true, TopicSession.SESSION_TRANSACTED);
                // Store topicSession
                vars.putObject("topicSession", topicSession);
                JMeterContextService.getContext().setVariables(vars);
            } else {
                topicSession = (TopicSession) vars.getObject("TopicSession");
            }
            // Send message
            Topic topic = (Topic) ctx.lookup(topicName);
            // create the message to send
            TextMessage textMessage = topicSession.createTextMessage(message);
            javax.jms.TopicPublisher topicPublisher = topicSession.createPublisher(topic);
            for (int i = 0; i < msgCount; i++) {
                topicPublisher.publish(textMessage);
            }

            topicSession.commit();

            topicPublisher.close();

            result.sampleEnd();
            result.setSuccessful(true);
            result.setResponseMessage("Successfully published messages");
            result.setResponseCodeOK();

        } catch (JMSException | NamingException e) {
            result.sampleEnd();
            result.setSuccessful(false);
            // get stack trace as a String to return as document data
            java.io.StringWriter stringWriter = new java.io.StringWriter();
            e.printStackTrace(new java.io.PrintWriter(stringWriter));
            result.setResponseData(stringWriter.toString(), null);
            result.setResponseMessage("Unable to publish messages to topic." + "-" + "Exception: " + e.toString());
            result.setDataType(SampleResult.TEXT);
            result.setResponseCode("FAILED");
        }
        return result;
    }

    private String getTCPConnectionURL(String username, String password, JavaSamplerContext context) {
        // amqp://{username}:{password}@carbon/carbon?brokerlist='tcp://{hostname}:{port}'
        return new StringBuffer().append("amqp://").append(username).append(":").append(password).append("@")
                .append(context.getParameter("CARBON_CLIENT_ID)")).append("/")
                .append(context.getParameter("CARBON_VIRTUAL_HOST_NAME")).append("?brokerlist='tcp://")
                .append(context.getParameter("CARBON_DEFAULT_HOSTNAME")).append(":")
                .append(context.getParameter("CARBON_DEFAULT_PORT")).append("'").toString();
    }

    @Override
    public void teardownTest(JavaSamplerContext context) {
        try {
            if (null != topicSession) {
                topicSession.close();
            }
            if (null != topicConnection) {
                topicConnection.stop();
                topicConnection.close();
            }
        } catch (JMSException e) {
            log.error(e.getMessage(), e);
        }
    }
}
