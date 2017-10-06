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

import java.util.Properties;
import javax.jms.JMSException;
import javax.jms.MessageConsumer;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSession;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * This is Queue Receiver Jmeter Sampler class for local transactions.
 */
public class TransactedQueueReceiver extends AbstractJavaSamplerClient {
    private static final Logger log = LoggingManager.getLoggerForClass();

    private String userName;
    private String password;
    private String queueName;
    private String cfName;
    private Properties properties;

    private QueueConnection queueConnection;
    private QueueSession queueSession;
    private MessageConsumer consumer;


    // set up default arguments for the JMeter GUI
    @Override
    public Arguments getDefaultParameters() {
        Arguments defaultParameters = new Arguments();
        defaultParameters.addArgument("QPID_ICF", "org.wso2.andes.jndi.PropertiesFileInitialContextFactory");
        defaultParameters.addArgument("CF_NAME_PREFIX", "connectionfactory.");
        defaultParameters.addArgument("QUEUE_NAME_PREFIX", "queue.");
        defaultParameters.addArgument("CF_NAME", "qpidConnectionfactory");
        defaultParameters.addArgument("USER_NAME", "admin");
        defaultParameters.addArgument("PASSWORD", "admin");
        defaultParameters.addArgument("QUEUE_NAME", "testQueue");
        defaultParameters.addArgument("CARBON_CLIENT_ID", "carbon");
        defaultParameters.addArgument("CARBON_VIRTUAL_HOST_NAME", "carbon");
        defaultParameters.addArgument("CARBON_DEFAULT_HOSTNAME", "localhost");
        defaultParameters.addArgument("CARBON_DEFAULT_PORT", "5672");
        defaultParameters.addArgument("MESSAGE_COUNT_PER_COMMIT", "1");

        return defaultParameters;
    }

    @Override
    public void setupTest(JavaSamplerContext context) {
        userName = context.getParameter("USER_NAME");
        password = context.getParameter("PASSWORD");
        queueName = context.getParameter("QUEUE_NAME");
        cfName = context.getParameter("CF_NAME");
        properties = new Properties();
        properties.put(Context.INITIAL_CONTEXT_FACTORY, context.getParameter("QPID_ICF"));
        properties.put(context.getParameter("CF_NAME_PREFIX") +
                cfName, getTCPConnectionURL(userName, password, context));
        properties.put(context.getParameter("QUEUE_NAME_PREFIX") + queueName, queueName);
    }

    @Override
    public SampleResult runTest(JavaSamplerContext context) {
        SampleResult result = new SampleResult();
        result.sampleStart();
        result.setSampleLabel("Transacted Queue Receiver");
        try {
            JMeterVariables vars = JMeterContextService.getContext().getVariables();
            InitialContext ctx = new InitialContext(properties);

            if (null == vars.getObject("queueSession")) {
                // Lookup connection factory
                QueueConnectionFactory connFactory = (QueueConnectionFactory) ctx.lookup(cfName);
                queueConnection = connFactory.createQueueConnection();
                queueConnection.start();
                queueSession = queueConnection.createQueueSession(true, QueueSession.SESSION_TRANSACTED);
                // Store queueSession
                vars.putObject("queueSession", queueSession);
                JMeterContextService.getContext().setVariables(vars);
            } else {
                queueSession = (QueueSession) vars.getObject("queueSession");
            }

            //Receive message
            Queue queue =  (Queue) ctx.lookup(queueName);
            consumer = queueSession.createConsumer(queue);

            queueSession.commit();

            consumer.close();

            result.sampleEnd();
            result.setSuccessful(true);
            result.setResponseMessage("Successfully consumed messages");
            result.setResponseCodeOK();

        } catch (JMSException | NamingException ex) {
            result.sampleEnd();
            result.setSuccessful(false);
            // get stack trace as a String to return as document data
            java.io.StringWriter stringWriter = new java.io.StringWriter();
            ex.printStackTrace(new java.io.PrintWriter(stringWriter));
            result.setResponseData(stringWriter.toString(), null);
            result.setResponseMessage("Unable to consume messages from queue." + "-" + "Exception: " + ex.toString());
            result.setDataType(org.apache.jmeter.samplers.SampleResult.TEXT);
            result.setResponseCode("FAILED");
        }
        return result;
    }

    private String getTCPConnectionURL(String username, String password, JavaSamplerContext context) {
        // amqp://{username}:{password}@carbon/carbon?brokerlist='tcp://{hostname}:{port}'
        return new StringBuffer()
                .append("amqp://").append(username).append(":").append(password)
                .append("@").append(context.getParameter("CARBON_CLIENT_ID)"))
                .append("/").append(context.getParameter("CARBON_VIRTUAL_HOST_NAME"))
                .append("?brokerlist='tcp://").append(context.getParameter("CARBON_DEFAULT_HOSTNAME"))
                .append(":").append(context.getParameter("CARBON_DEFAULT_PORT")).append("'")
                .toString();
    }

    @Override
    public void teardownTest(JavaSamplerContext context) {
        try {
            if (null != queueSession) {
                queueSession.close();
            }
            if (null != queueConnection) {
                queueConnection.stop();
            }
            if (null != queueConnection) {
                queueConnection.close();
            }
        } catch (JMSException e) {
            log.error(e.getMessage(), e);
        }
    }
}
