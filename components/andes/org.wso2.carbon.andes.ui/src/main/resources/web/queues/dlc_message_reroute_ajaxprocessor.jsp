<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1" %>
<%@page import="org.wso2.carbon.ui.CarbonUIMessage"%>
<%@ page import="org.wso2.carbon.andes.stub.AndesAdminServiceStub" %>
<%@ page import="org.wso2.carbon.andes.ui.UIUtils" %>
<%@ page import="org.wso2.carbon.andes.stub.AndesAdminServiceBrokerManagerAdminException" %>

<%
    AndesAdminServiceStub stub = UIUtils.getAndesAdminServiceStub(config, session, request);
    String idList = request.getParameter("msgList");
    String newQueueName = request.getParameter("newQueueName");
    String nameOfQueue = request.getParameter("nameOfQueue");

    long unavailableMessageCount = 0;

    String[] idArray = idList.split(",");
    if(!idList.isEmpty()) {
        long[] andesMessageIDs = new long[idArray.length];
        for (int i = 0; i < idArray.length; i++) {
            andesMessageIDs[i] = Long.parseLong(idArray[i]);
        }

        try {
            unavailableMessageCount = stub.rerouteSelectedMessagesFromDeadLetterChannel(andesMessageIDs,
            newQueueName, nameOfQueue);

        } catch (AndesAdminServiceBrokerManagerAdminException e) {
            CarbonUIMessage uiMsg = new CarbonUIMessage(CarbonUIMessage.ERROR, e.getFaultMessage().
                                    getBrokerManagerAdminException().getErrorMessage(), e);
            session.setAttribute(CarbonUIMessage.ID, uiMsg);
        }
    } else {
        CarbonUIMessage uiMsg = new CarbonUIMessage(CarbonUIMessage.ERROR, "There is no message in DLC belonging to "
        + nameOfQueue);
        session.setAttribute(CarbonUIMessage.ID, uiMsg);
    }
%>
<%=unavailableMessageCount%>