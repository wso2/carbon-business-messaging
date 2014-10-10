/*
 * Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 */
package org.wso2.carbon.andes.ui.client;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.andes.ui.UIUtils;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.xml.stream.XMLStreamException;
import java.io.FileNotFoundException;
import java.util.Properties;

public class QueueReceiverClient {
    private static Log log = LogFactory.getLog(QueueReceiverClient.class);
    public static final String QPID_ICF = "org.wso2.andes.jndi.PropertiesFileInitialContextFactory";
    private static final String CF_NAME_PREFIX = "connectionfactory.";
    private static final String CF_NAME = "qpidConnectionfactory";
    private QueueSession queueSession;
    private QueueConnection queueConnection;
    private MessageConsumer queueConsumer;

    public Queue registerReceiver(String nameOfQueue, String username, String accesskey) throws NamingException,
            JMSException, FileNotFoundException, XMLStreamException {
        Properties properties = new Properties();
        properties.put(Context.INITIAL_CONTEXT_FACTORY, QPID_ICF);
        properties.put(CF_NAME_PREFIX + CF_NAME, UIUtils.getTCPConnectionURL(username, accesskey));
        properties.put("queue." + nameOfQueue, nameOfQueue);
        properties.put(CarbonConstants.REQUEST_BASE_CONTEXT, "true");
        InitialContext ctx = new InitialContext(properties);
        // Lookup connection factory
        QueueConnectionFactory connFactory = (QueueConnectionFactory) ctx.lookup(CF_NAME);
        queueConnection = connFactory.createQueueConnection();
        queueSession = queueConnection.createQueueSession(false, QueueSession.AUTO_ACKNOWLEDGE);
        //Receive message
        Queue queue = (Queue) ctx.lookup(nameOfQueue);
        queueConsumer = queueSession.createConsumer(queue);
        queueConnection.start();
        return queue;

    }

    public int purgeQueue(Queue queue) throws JMSException {
        Message message;
        int messageCount = 0;
        while ((message = queueConsumer.receive(10000)) != null) {
            messageCount++;
        }
        log.info("Executed purge queue operation for the queue: " + queue.getQueueName() + " and removed " +
                messageCount + " messages");
        return messageCount;
    }

    public boolean closeReceiver() throws JMSException {
        queueConnection.close();
        queueSession.close();
        queueConsumer.close();
        return true;
    }
}
