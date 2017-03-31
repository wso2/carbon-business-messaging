<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage"%>
<%@ page import="org.wso2.carbon.andes.stub.AndesAdminServiceStub" %>
<%@ page import="org.wso2.carbon.andes.ui.UIUtils" %>
<%@ page import="org.wso2.carbon.andes.stub.admin.types.Message" %>
<%@ page import="org.wso2.andes.server.queue.DLCQueueUtils" %>
<%@ page import="org.wso2.carbon.andes.stub.AndesAdminServiceBrokerManagerAdminException" %>

<%
    AndesAdminServiceStub stub = UIUtils.getAndesAdminServiceStub(config, session, request);
    String queueName = request.getParameter("nameOfQueue");
    String messageIDList = "";
    try {
        // Getting the number of messages for a destination in DLC
        int numberOfMessagesInDLCForQueue = (int) stub.getNumberOfMessagesInDLCForQueue(queueName);

        // Getting the message IDs into a string with delimiter ","
        Message[] messagesInDLCForQueue = stub.getMessagesInDLCForQueue(queueName, 0, numberOfMessagesInDLCForQueue);
        for (int i = 0; i < numberOfMessagesInDLCForQueue; i++) {
            messageIDList += messagesInDLCForQueue[i].getAndesMsgMetadataId() + ",";
        }

        // We can remove the "," tag only if the messageIDList is not empty
        if (messageIDList.length() > 0) {
            //remove the last ","
            messageIDList = messageIDList.substring(0, messageIDList.length() - 1);
        }

    } catch (AndesAdminServiceBrokerManagerAdminException e) {
        CarbonUIMessage uiMsg = new CarbonUIMessage(CarbonUIMessage.ERROR, e.getFaultMessage().getBrokerManagerAdminException().getErrorMessage(), e);
        session.setAttribute(CarbonUIMessage.ID, uiMsg);
    }
%>
<%= messageIDList %>
