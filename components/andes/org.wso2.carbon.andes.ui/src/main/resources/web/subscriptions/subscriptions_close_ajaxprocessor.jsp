<%@ page import="org.wso2.carbon.andes.stub.AndesAdminServiceStub" %>
<%@ page import="org.wso2.carbon.andes.ui.UIUtils" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.andes.stub.AndesAdminServiceBrokerManagerAdminException" %>
<%
    AndesAdminServiceStub stub = UIUtils.getAndesAdminServiceStub(config, session, request);

    boolean isDurable = Boolean.valueOf(request.getParameter("isDurable"));
    String subscriptionID = request.getParameter("subscriptionID");
    String subscribedQueueOrTopicName = request.getParameter("subscribedQueueOrTopicName");
    String protocolType = request.getParameter("protocolType");
    String destinationType = request.getParameter("destinationType");
    String subscriberQueueName = request.getParameter("subscriberQueueName");
    String message = "";
    try {
        stub.closeSubscription(isDurable, subscriptionID, subscribedQueueOrTopicName, protocolType, destinationType, subscriberQueueName);
    } catch (AndesAdminServiceBrokerManagerAdminException e) {
        CarbonUIMessage uiMsg = new CarbonUIMessage(CarbonUIMessage.ERROR, e.getFaultMessage().getBrokerManagerAdminException().getErrorMessage(), e);
        session.setAttribute(CarbonUIMessage.ID, uiMsg);
        message = "Error: " + e.getFaultMessage().getBrokerManagerAdminException().getErrorMessage();
    }
%><%=message%>
