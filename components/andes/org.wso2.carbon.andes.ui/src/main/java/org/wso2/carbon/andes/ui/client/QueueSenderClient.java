/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.carbon.andes.ui.client;


import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.andes.ui.UIUtils;
import org.wso2.carbon.ui.CarbonUIMessage;

import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;
import java.util.Properties;

public class QueueSenderClient {

    public static final String QPID_ICF = "org.wso2.andes.jndi.PropertiesFileInitialContextFactory";
    private static final String CF_NAME_PREFIX = "connectionfactory.";
    private static final String QUEUE_NAME_PREFIX = "queue.";
    private static final String CF_NAME = "qpidConnectionfactory";

    InitialContext ctx;
    QueueConnection queueConnection;
    private QueueSession queueSession;
    private QueueSender queueSender;

    public QueueSenderClient(String nameOfQueue, String username, String accessKey) throws NamingException, JMSException {
        queueSender = registerQueueSender(nameOfQueue, username, accessKey);

    }

    private QueueSender registerQueueSender(String nameOfQueue, String username, String accessKey) throws NamingException, JMSException {
        Properties properties = new Properties();
        properties.put(Context.INITIAL_CONTEXT_FACTORY, QPID_ICF);
        properties.put(CF_NAME_PREFIX + CF_NAME, UIUtils.getTCPConnectionURL(username, accessKey));
        properties.put(QUEUE_NAME_PREFIX + nameOfQueue, nameOfQueue);
        properties.put(CarbonConstants.REQUEST_BASE_CONTEXT, "true");
        ctx = new InitialContext(properties);

        // Lookup connection factory
        QueueConnectionFactory connFactory = (QueueConnectionFactory) ctx.lookup(CF_NAME);
        queueConnection = connFactory.createQueueConnection();
        queueConnection.start();
        queueSession = queueConnection.createQueueSession(false, QueueSession.AUTO_ACKNOWLEDGE);
        Queue queue = (Queue) ctx.lookup(nameOfQueue);

        return queueSession.createSender(queue);

    }

    public boolean sendMessage(HttpServletRequest request) throws JMSException, NamingException {

        if (queueSender != null) {
            // as a way of preventing sending a message on page load, before the 'submit' button clicked,  num. of messages field is
            // used as the validation field here. That is valid message will be sent only if the num. of message count is 1 or above
            if (request.getParameter("num_of_msgs") != null && !request.getParameter("num_of_msgs").equals("")) {

                int delivery_mode = 2;
                int priority = 4;
                long time_to_live = 0;

                // set delivery mode 1- non persistent, 2- persistent
                if (request.getParameter("delivery_mode") == null) {
                    delivery_mode = 1;
                }
                // set time to live
                if (!request.getParameter("expire").equals("")) {
                    String expire_time = request.getParameter("expire");
                    time_to_live = Long.parseLong(expire_time);
                }

/*              // set priority value 0-9 range
                if (!request.getParameter("priority").equals("")) {
                    String priority_val = request.getParameter("priority");
                    priority = Integer.parseInt(priority_val);
                }*/


                // get number of messages to send and send them
                int msg_count = Integer.parseInt(request.getParameter("num_of_msgs"));
                for (int i = 0; i < msg_count; i++) {
                    TextMessage message = createMessageAndSetParameters(request);
                    queueSender.send(message, delivery_mode, priority, time_to_live);

                }
               // CarbonUIMessage.sendCarbonUIMessage("Successful sent message to: " +request.getParameter("nameOfQueue"), CarbonUIMessage.INFO, request);
            }
            queueSender.close();
            queueSession.close();
            queueConnection.stop();
            queueConnection.close();

            return true;
        } else {
            return false;
        }

    }

    /**
     * create a new message, reads all the parameter values from request, set them into message, for the fields not defined, defaults values are taken
     *
     * @param request - http request object
     * @return newly created JMS text message
     * @throws JMSException
     * @throws NamingException
     */
    private TextMessage createMessageAndSetParameters(HttpServletRequest request) throws JMSException, NamingException {

        TextMessage message = queueSession.createTextMessage();


        // set correlation id
        if (!request.getParameter("cor_id").equals("")) {
            String cor_id = request.getParameter("cor_id");
            message.setJMSCorrelationID(cor_id);
        }

        // set jms type
        if (!request.getParameter("jms_type").equals("")) {
            String jms_type = request.getParameter("jms_type");
            message.setJMSType(jms_type);
        }

        // set message text
        if (!request.getParameter("msg_text").equalsIgnoreCase("")) {
            String message_txt = request.getParameter("msg_text");
            message.setText(message_txt);
        } else {
            message.setText("Type message here..");
        }

        return message;
    }


}
