<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1" %>
<%@ page import="java.util.Set" %>
<%@ page import="java.util.HashSet" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage"%>
<%@ page import="org.wso2.carbon.andes.stub.AndesAdminServiceStub" %>
<%@ page import="org.wso2.carbon.andes.ui.UIUtils" %>
<%@ page import="org.wso2.carbon.andes.stub.admin.types.Message" %>
<%@ page import="org.wso2.andes.server.queue.DLCQueueUtils" %>
<%@ page import="org.wso2.carbon.andes.stub.AndesAdminServiceBrokerManagerAdminException" %>

<%
    AndesAdminServiceStub stub = UIUtils.getAndesAdminServiceStub(config, session, request);
    String destinationList = "";
    try {
        String dlcQueueName = request.getParameter("nameOfQueue");
        String[] durableQueues = stub.getNamesOfAllDurableQueues();
        // Creating a string with all destinations with delimiter as #
        for (String queueName : durableQueues) {
            if (!DLCQueueUtils.isDeadLetterQueue(queueName)) {
                destinationList += queueName + "#";
            }
        }
        // We can remove the # tag only if the destinationList is not empty
        if (destinationList.length() > 0) {
            //remove the last #
            destinationList = destinationList.substring(0, destinationList.length() - 1);
        }
    } catch (AndesAdminServiceBrokerManagerAdminException e) {
        CarbonUIMessage uiMsg = new CarbonUIMessage(CarbonUIMessage.ERROR, e.getFaultMessage().getBrokerManagerAdminException().getErrorMessage(), e);
        session.setAttribute(CarbonUIMessage.ID, uiMsg);
    }
%>
<%= destinationList %>
