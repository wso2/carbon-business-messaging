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

import org.apache.commons.lang.StringEscapeUtils;
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
import java.util.Enumeration;
import java.util.Properties;

/**
 * The client class which is used to create a QueueConnection in order to browse the content of a selected queue.
 */
public class QueueBrowserClient {

    private static final Log log = LogFactory.getLog(QueueBrowserClient.class);

    public static final String QPID_ICF = "org.wso2.andes.jndi.PropertiesFileInitialContextFactory";
    private static final String CF_NAME_PREFIX = "connectionfactory.";
    private static final String QUEUE_NAME_PREFIX = "queue.";
    private static final String CF_NAME = "qpidConnectionfactory";

    private String nameOfQueue;
    private Properties properties;
    private QueueConnection queueConnection;
    private QueueSession queueSession;
    private QueueBrowser queueBrowser;


    public QueueBrowserClient(String nameOfQueue, String userName, String accessKey) throws FileNotFoundException,
            XMLStreamException {
        this.nameOfQueue = nameOfQueue;
        this.properties = new Properties();
        properties.put(Context.INITIAL_CONTEXT_FACTORY, QPID_ICF);
        properties.put(CF_NAME_PREFIX + CF_NAME, UIUtils.getTCPConnectionURL(userName, accessKey));
        properties.put(QUEUE_NAME_PREFIX + nameOfQueue, nameOfQueue);
        properties.put(CarbonConstants.REQUEST_BASE_CONTEXT, "true");

    }

    public Enumeration browseQueue() {

        Enumeration queueContentsEnu = null;
        try {
            InitialContext ctx = new InitialContext(properties);
            QueueConnectionFactory connFactory = (QueueConnectionFactory) ctx.lookup(CF_NAME);
            queueConnection = connFactory.createQueueConnection();
            Queue queue = (Queue) ctx.lookup(nameOfQueue);
            queueSession = queueConnection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
            queueBrowser = queueSession.createBrowser(queue);
            queueConnection.start();

            queueContentsEnu = queueBrowser.getEnumeration();

        } catch (NamingException e) {
            log.error("Error browsing queue.", e);
        } catch (JMSException e) {
            log.error("Error browsing queue.", e);
        }
        return queueContentsEnu;
    }

    public void closeBrowser() throws JMSException {
        queueConnection.close();
        queueSession.close();
        queueBrowser.close();
    }

    public String getMsgProperties(Message queueMessage) throws JMSException {

        Enumeration propertiesEnu = queueMessage.getPropertyNames();
        StringBuilder sb = new StringBuilder("");
        if (propertiesEnu != null) {
            while (propertiesEnu.hasMoreElements()) {
                String propName = (String) propertiesEnu.nextElement();
                sb.append(propName + " = " + queueMessage.getStringProperty(propName));
                sb.append(", ");
            }
        }

        return sb.toString();
    }

    /**
     * Determines the type of the JMS message
     *
     * @param queueMessage - input message
     * @return type of the message as a string
     */
    public String getMsgContentType(Message queueMessage) {

        String contentType = "";
        if (queueMessage instanceof TextMessage) {
            contentType = "Text";
        } else if (queueMessage instanceof ObjectMessage) {
            contentType = "Object";
        } else if (queueMessage instanceof MapMessage) {
            contentType = "Map";
        } else if (queueMessage instanceof StreamMessage) {
            contentType = "Stream";
        } else if (queueMessage instanceof BytesMessage) {
            contentType = "Byte";
        }

        return contentType;
    }

    /**
     * Gets the message content as a string, after verifying its type
     *
     * @param queueMessage - JMS Message
     * @return a string array of message content; a summary and the whole message
     * @throws JMSException
     */
    public String[] getMessageContentAsString(Message queueMessage) throws JMSException {

        String messageContent[] = new String[2];
        String summaryMsg = "";
        String wholeMsg = "";

        StringBuilder sb = new StringBuilder();
        if (queueMessage instanceof TextMessage) {
            wholeMsg = StringEscapeUtils.escapeHtml(((TextMessage) queueMessage).getText()).trim();
            if (wholeMsg.length() >= 15) {
                summaryMsg = wholeMsg.substring(0, 15);
            } else {
                summaryMsg = wholeMsg;
            }
            if (wholeMsg.length() > UIUtils.MESSAGE_DISPLAY_LENGTH_MAX) {
                wholeMsg = wholeMsg.substring(0, UIUtils.MESSAGE_DISPLAY_LENGTH_MAX - 3) + UIUtils
                        .DISPLAY_CONTINUATION + UIUtils.DISPLAY_LENGTH_EXCEEDED;
            }


        } else if (queueMessage instanceof ObjectMessage) {
            wholeMsg = "This Operation is Not Supported!";
            summaryMsg = "Not Supported";

        } else if (queueMessage instanceof MapMessage) {
            MapMessage mapMessage = ((MapMessage) queueMessage);
            Enumeration mapEnu = mapMessage.getMapNames();
            while (mapEnu.hasMoreElements()) {
                String mapName = (String) mapEnu.nextElement();
                String mapVal = mapMessage.getObject(mapName).toString();
                wholeMsg = StringEscapeUtils.escapeHtml(sb.append(mapName + ": " + mapVal + ", ").toString()).trim();

            }
            if (wholeMsg.length() >= 15) {
                summaryMsg = wholeMsg.substring(0, 15);
            } else {
                summaryMsg = wholeMsg;
            }

        } else if (queueMessage instanceof StreamMessage) {
            ((StreamMessage) queueMessage).reset();
            wholeMsg = getContentFromStreamMessage((StreamMessage) queueMessage, sb).trim();
            if (wholeMsg.length() >= 15) {
                summaryMsg = wholeMsg.substring(0, 15);
            } else {
                summaryMsg = wholeMsg;
            }

        } else if (queueMessage instanceof BytesMessage) {
            ((BytesMessage) queueMessage).reset();
            long msglength = ((BytesMessage) queueMessage).getBodyLength();
            byte[] byteMsgArr = new byte[(int) msglength];

            int index = ((BytesMessage) queueMessage).readBytes(byteMsgArr);
            for (int i = 0; i < index; i++) {
                wholeMsg = sb.append(byteMsgArr[i] + " ").toString().trim();
            }

            if (wholeMsg.length() >= 15) {
                summaryMsg = wholeMsg.substring(0, 15);
            } else {
                summaryMsg = wholeMsg;
            }
        }
        messageContent[0] = summaryMsg;
        messageContent[1] = wholeMsg;
        return messageContent;
    }


    /**
     * A stream message can have java primitives plus objects, as its content. This message it used to retrieve the
     *
     * @param queueMessage - input message
     * @param sb           - a string builder to build the whole message content
     * @return - complete message content inside the stream message
     * @throws JMSException
     */
    private String getContentFromStreamMessage(StreamMessage queueMessage, StringBuilder sb) throws JMSException {

        // todo : Fix this to get content in a better way
        while (true) {

            try {
                Object obj = queueMessage.readObject();
                if (obj == null) {
                    break;
                } else {
                    if (obj instanceof Double) {
                        sb.append(obj + ", ");

                    } else if (obj instanceof Integer) {
                        sb.append(obj + ", ");

                    } else if (obj instanceof String) {
                        sb.append(obj + ", ");

                    } else if (obj instanceof Character) {
                        sb.append(obj + ", ");

                    } else if (obj instanceof Long) {
                        sb.append(obj + ", ");

                    } else if (obj instanceof Short) {
                        sb.append(obj + ", ");

                    } else if (obj instanceof Byte) {
                        sb.append(obj + ", ");

                    } else if (obj instanceof Boolean) {
                        sb.append(obj + ", ");
                    } else if (obj instanceof Float) {
                        sb.append(obj + ", ");
                    } else {
                        sb.append(obj.toString() + ", ");
                    }
                }
            } catch (MessageEOFException ex) {
                return sb.toString();
            }

        }

        return StringEscapeUtils.escapeHtml(sb.toString());
    }


}
