<%@ page import="org.wso2.carbon.andes.stub.AndesAdminServiceStub" %>
<%@ page import="org.wso2.carbon.andes.ui.UIUtils" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.andes.stub.AndesAdminServiceBrokerManagerAdminException" %>
<%
    AndesAdminServiceStub stub = UIUtils.getAndesAdminServiceStub(config, session, request);

    String subscriptionID = request.getParameter("subscriptionID");
    String subscriptionDestination = request.getParameter("destination");
    String protocolType = request.getParameter("protocolType");
    String destinationType = request.getParameter("destinationType");
    String message = "";
    try {
        stub.closeSubscription(subscriptionID, subscriptionDestination, protocolType, destinationType);
    } catch (AndesAdminServiceBrokerManagerAdminException e) {
        CarbonUIMessage uiMsg = new CarbonUIMessage(CarbonUIMessage.ERROR, e.getFaultMessage().getBrokerManagerAdminException().getErrorMessage(), e);
        session.setAttribute(CarbonUIMessage.ID, uiMsg);
        message = "Error: " + e.getFaultMessage().getBrokerManagerAdminException().getErrorMessage();
    }
%><%=message%>
