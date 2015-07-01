<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1" %>
<%@page import="org.wso2.carbon.ui.CarbonUIMessage"%>
<%@ page import="org.wso2.carbon.andes.stub.AndesAdminServiceStub" %>
<%@ page import="org.wso2.carbon.andes.ui.UIUtils" %>

<%
    AndesAdminServiceStub stub = UIUtils.getAndesAdminServiceStub(config, session, request);
    String idList = request.getParameter("msgList");
    String nameOfQueue = request.getParameter("nameOfQueue");

    String[] idArray = idList.split(",");
    long[] andesMessageIDs = new long[idArray.length];
    for (int i = 0; i < idArray.length; i++) {
        andesMessageIDs[i] = Long.parseLong(idArray[i]);
    }
    try {
        stub.deleteMessagesFromDeadLetterQueue(andesMessageIDs, nameOfQueue);

    } catch (Exception e) {
        CarbonUIMessage uiMsg = new CarbonUIMessage(CarbonUIMessage.ERROR, e.getMessage(), e);
        session.setAttribute(CarbonUIMessage.ID, uiMsg);
    }
%>
