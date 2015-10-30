<%@ page import="org.wso2.carbon.andes.stub.AndesAdminServiceStub" %>
<%@ page import="org.wso2.carbon.andes.ui.UIUtils" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.andes.stub.AndesAdminServiceBrokerManagerAdminException" %>
<%
    AndesAdminServiceStub stub = UIUtils.getAndesAdminServiceStub(config, session, request);

    String queue = request.getParameter("nameOfQueue");
    String topicName = request.getParameter("nameOfTopic");
    String message = "";
    try {
        stub.deleteQueue(queue);
        if (null != topicName ) {
            stub.deleteTopicFromRegistry(topicName, queue);
        }
    } catch (AndesAdminServiceBrokerManagerAdminException e) {
        CarbonUIMessage uiMsg = new CarbonUIMessage(CarbonUIMessage.ERROR, e.getFaultMessage().getBrokerManagerAdminException().getErrorMessage(), e);
        session.setAttribute(CarbonUIMessage.ID, uiMsg);
        message = "Error: " + e.getFaultMessage().getBrokerManagerAdminException().getErrorMessage();
    }
%><%=message%>
