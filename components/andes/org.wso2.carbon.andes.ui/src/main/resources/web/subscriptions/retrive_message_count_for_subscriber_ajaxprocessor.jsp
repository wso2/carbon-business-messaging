<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1" %>
<%@page import="org.wso2.carbon.ui.CarbonUIMessage"%>
<%@ page import="org.wso2.carbon.andes.stub.AndesAdminServiceStub" %>
<%@ page import="org.wso2.carbon.andes.ui.UIUtils" %>
<%@ page import="org.wso2.andes.kernel.DestinationType" %>
<%@ page import="org.wso2.andes.kernel.ProtocolType" %>

<%
    AndesAdminServiceStub stub = UIUtils.getAndesAdminServiceStub(config, session, request);
    String numberOfMessages = "-1";
    try{
        String queueName = request.getParameter("queueName");
        long messageCount = stub.getPendingMessageCount(queueName);
        numberOfMessages = Long.toString(messageCount);

    } catch (Exception e) {
        numberOfMessages = "Error";
        CarbonUIMessage uiMsg = new CarbonUIMessage(CarbonUIMessage.ERROR, e.getMessage(), e);
        session.setAttribute(CarbonUIMessage.ID, uiMsg);
    }
%>
<%=numberOfMessages%>
