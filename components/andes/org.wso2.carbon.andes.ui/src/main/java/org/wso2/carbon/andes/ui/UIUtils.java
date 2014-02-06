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
package org.wso2.carbon.andes.ui;

import org.apache.axis2.AxisFault;
import org.apache.axis2.client.Options;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.andes.stub.AndesAdminServiceStub;
import org.wso2.carbon.andes.stub.admin.types.Queue;
import org.wso2.carbon.andes.ui.client.QueueReceiverClient;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.ui.CarbonUIUtil;
import org.wso2.carbon.utils.ServerConstants;

import javax.jms.JMSException;
import javax.naming.NamingException;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.rmi.RemoteException;
import java.util.ArrayList;

public class UIUtils {
    private static Log log = LogFactory.getLog(UIUtils.class);

    private static Boolean IS_MSG_COUNT_VIEW_OPTION_SET = null;

    public static String getHtmlString(String message) {
        return message.replaceAll("<", "&lt;").replaceAll(">", "&gt;");

    }

    public static AndesAdminServiceStub getAndesAdminServiceStub(ServletConfig config,
                                                                           HttpSession session,
                                                                           HttpServletRequest request)
            throws AxisFault {
        String backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
        backendServerURL = backendServerURL + "AndesAdminService";
        ConfigurationContext configContext =
                (ConfigurationContext) config.getServletContext().getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
        AndesAdminServiceStub stub = new AndesAdminServiceStub(configContext,backendServerURL);
        String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
        if (cookie != null) {
            Options option = stub._getServiceClient().getOptions();
            option.setManageSession(true);
            option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, cookie);
        }

        return stub;
    }

    /**
     * filter the full queue list to suit the range
     * @param fullList
     * @param startingIndex
     * @param maxQueueCount
     * @return  Queue[]
     */
    public static Queue[] getFilteredQueueList(Queue[] fullList ,int startingIndex, int maxQueueCount) {
        Queue[] queueDetailsArray;
        int resultSetSize = maxQueueCount;

        ArrayList<Queue> resultList = new ArrayList<Queue>();
        for(Queue aQueue : fullList) {
            resultList.add(aQueue);
        }

        if ((resultList.size() - startingIndex) < maxQueueCount) {
            resultSetSize = (resultList.size() - startingIndex);
        }
        queueDetailsArray = new Queue[resultSetSize];
        int index = 0;
        int queueDetailsIndex = 0;
        for (Queue queueDetail : resultList) {
            if (startingIndex == index || startingIndex < index) {
                queueDetailsArray[queueDetailsIndex] = new Queue();

                queueDetailsArray[queueDetailsIndex].setQueueName(queueDetail.getQueueName());
                queueDetailsArray[queueDetailsIndex].setMessageCount(queueDetail.getMessageCount());
                queueDetailsIndex++;
                if (queueDetailsIndex == maxQueueCount) {
                    break;
                }
            }
            index++;
        }

        return queueDetailsArray;
    }

    /**
     * filter the whole message list to fit for the page range
     *
     * @param msgArrayList  - total message list
     * @param startingIndex -  index of the first message of given page
     * @param maxMsgCount   - max messages count per a page
     * @return filtered message object array for the given page
     */
    public static Object[] getFilteredMsgsList(ArrayList msgArrayList, int startingIndex, int maxMsgCount) {
        Object[] messageArray;
        int resultSetSize = maxMsgCount;

        ArrayList resultList = new ArrayList();
        for (Object aMsg : msgArrayList) {
            resultList.add(aMsg);
        }

        if ((resultList.size() - startingIndex) < maxMsgCount) {
            resultSetSize = (resultList.size() - startingIndex);
        }

        messageArray = new Object[resultSetSize];
        int index = 0;
        int msgDetailsIndex = 0;
        for (Object msgDetailOb : resultList) {
            if (startingIndex == index || startingIndex < index) {
                messageArray[msgDetailsIndex] = msgDetailOb;
                msgDetailsIndex++;
                if (msgDetailsIndex == maxMsgCount) {
                    break;
                }
            }
            index++;
        }
        return messageArray;
    }

    /**
     * Reads the post offset value defined in carbon.xml file
     * @return offset value
     */
    public static int getPortOffset(){
        String CARBON_CONFIG_PORT_OFFSET = "Ports.Offset";
        int CARBON_DEFAULT_PORT_OFFSET = 0;
        ServerConfiguration carbonConfig = ServerConfiguration.getInstance();
        String portOffset = carbonConfig.getFirstProperty(CARBON_CONFIG_PORT_OFFSET);

        try {
            return ((portOffset != null) ? Integer.parseInt(portOffset.trim()) : CARBON_DEFAULT_PORT_OFFSET);
        } catch (NumberFormatException e) {
            return CARBON_DEFAULT_PORT_OFFSET;
        }
    }

    /**
     * Gets the TCP connection url to reach the broker by using the currently logged in user and the accesskey for
     * the user, generated by andes Authentication Service
     * @param userName - currently logged in user
     * @param accessKey - the key (uuid) generated by authentication service
     * @return
     */
    public static String getTCPConnectionURL(String userName, String accessKey) {
        // amqp://{username}:{accesskey}@carbon/carbon?brokerlist='tcp://{hostname}:{port}'
        String CARBON_CLIENT_ID = "carbon";
        String CARBON_VIRTUAL_HOST_NAME = "carbon";
        String CARBON_DEFAULT_HOSTNAME = "localhost";
        String CARBON_DEFAULT_PORT = "5672";
        int portOffset = getPortOffset();
        int carbonPort = Integer.valueOf(CARBON_DEFAULT_PORT)+portOffset;
        String CARBON_PORT = String.valueOf(carbonPort);

        // as it is nt possible to obtain the password of for the given user, we use service generated access key
        // to authenticate the user
        return new StringBuffer()
                .append("amqp://").append(userName).append(":").append(accessKey)
                .append("@").append(CARBON_CLIENT_ID)
                .append("/").append(CARBON_VIRTUAL_HOST_NAME)
                .append("?brokerlist='tcp://").append(CARBON_DEFAULT_HOSTNAME).append(":").append(CARBON_PORT).append("'")
                .toString();
    }

    /**
     * Gets the message count of a given queue at that moment
     *
     * @param queueList - list of all queues available
     * @param queuename - the given queue
     * @return - message count of the given queue
     */

    public static int getCurrentMessageCountInQueue(Queue[] queueList, String queuename) {
        int messageCount = 0;
        if (queueList != null) {
            for (Queue queue : queueList) {
                if (queue.getQueueName().equals(queuename)) {
                    messageCount = queue.getMessageCount();
                }
            }
        }

        return messageCount;
    }

    public static boolean isDefaultMsgCountViewOptionConfigured(AndesAdminServiceStub stub) throws RemoteException {
       try{
           if(IS_MSG_COUNT_VIEW_OPTION_SET == null){
               IS_MSG_COUNT_VIEW_OPTION_SET = stub.getValueOfMsgCountViewOption();
           }

       }catch (Exception e){
           log.error("Error in reading MSG_COUNT_VIEW_OPTION from server: " , e);
       }
       return IS_MSG_COUNT_VIEW_OPTION_SET;
    }


}
