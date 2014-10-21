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
    try{
        queueList = stub.getAllQueues();
        for(int count=0 ; count < queueList.length ; count ++) {
             if(!queueList[count].getQueueName().equals(Constants.DEAD_LETTER_QUEUE_NAME)) {
                 queueListString = queueListString + queueList[count].getQueueName() + "#";
             }
        }
        //remove the last #
        queueListString = queueListString.substring(0,queueListString.length()-1);

    } catch (AndesAdminServiceBrokerManagerAdminException e) {
        CarbonUIMessage uiMsg = new CarbonUIMessage(CarbonUIMessage.ERROR, e.getFaultMessage().getBrokerManagerAdminException().getErrorMessage(), e);
        session.setAttribute(CarbonUIMessage.ID, uiMsg);
    }
%>
<%=queueListString%>
