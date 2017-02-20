<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1" %>
<%@page import="org.wso2.carbon.ui.CarbonUIMessage"%>
<%@ page import="org.wso2.carbon.andes.stub.AndesAdminServiceStub" %>
<%@ page import="org.wso2.carbon.andes.ui.UIUtils" %>
<%@ page import="org.wso2.carbon.andes.stub.admin.types.Queue" %>
<%@ page import="org.wso2.carbon.andes.ui.Constants" %>
<%@ page import="org.wso2.carbon.andes.stub.AndesAdminServiceBrokerManagerAdminException" %>

<%
    AndesAdminServiceStub stub = UIUtils.getAndesAdminServiceStub(config, session, request);
    Queue[] queueList = null;
    String queueListString = "";
    try {
        String queueName = request.getParameter("nameOfQueue");
        queueList = stub.getAllQueues();
        // When there are only durable topic subscribers, queueList getting null because admin service does not return
        // queue name starts with carbon:
        if (queueList != null) {
            //the queueName is not set, it is assumed that the requirement is to get all queues
            if ("".equals(queueName) || null == queueName) {
                for (Queue queue : queueList) {
                    if (!(Constants.DEAD_LETTER_QUEUE_NAME.equals(queue.getQueueName()))) {
                        queueListString = queueListString + queue.getQueueName() + "#";
                    }
                }
            } else {
                //If the queue name contains ":" then it's a durable topic. Therefore, we should only show durable topics
                //else, we only show queues
                if (queueName.contains(":")) {
                    for (Queue queue : queueList) {
                        String currentQueueName = queue.getQueueName();
                        if (currentQueueName.contains(":")) {
                            queueListString = queueListString + currentQueueName + "#";
                        }

                    }
                } else {
                    for (Queue queue : queueList) {
                        String currentQueueName = queue.getQueueName();
                        if (!(Constants.DEAD_LETTER_QUEUE_NAME.equals(currentQueueName))) {
                            if (!currentQueueName.contains(":")) {
                                queueListString = queueListString + currentQueueName + "#";
                            }
                        }
                    }
                }
            }
        }

        //We can remove the # tag only if the queueList is not empty
        if (queueListString.length() > 0) {
            //remove the last #
            queueListString = queueListString.substring(0, queueListString.length() - 1);
        }

    } catch (AndesAdminServiceBrokerManagerAdminException e) {
        CarbonUIMessage uiMsg = new CarbonUIMessage(CarbonUIMessage.ERROR, e.getFaultMessage().getBrokerManagerAdminException().getErrorMessage(), e);
        session.setAttribute(CarbonUIMessage.ID, uiMsg);
    }
%>
<%=queueListString%>
