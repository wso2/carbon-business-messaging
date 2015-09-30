<%@ page import="org.apache.axis2.client.Options" %>
<%@ page import="org.apache.axis2.client.ServiceClient" %>
<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.andes.event.stub.service.AndesEventAdminServiceStub" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.andes.event.stub.service.AndesEventAdminServiceEventAdminException" %>
<%

    ConfigurationContext configContext = (ConfigurationContext) config.getServletContext()
            .getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
    //Server URL which is defined in the server.xml
    String serverURL = CarbonUIUtil.getServerURL(config.getServletContext(),
            session) + "AndesEventAdminService.AndesEventAdminServiceHttpsSoap12Endpoint";
    AndesEventAdminServiceStub stub = new AndesEventAdminServiceStub(configContext, serverURL);

    String cookie = (String) session.getAttribute(org.wso2.carbon.utils.ServerConstants.ADMIN_SERVICE_COOKIE);

    ServiceClient client = stub._getServiceClient();
    Options option = client.getOptions();
    option.setManageSession(true);
    option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, cookie);

    String topic = request.getParameter("topic");
    String textMsg = request.getParameter("xmlMessage");
    session.setAttribute("errorTopic", topic);
    session.setAttribute("xmlMessage", textMsg);
    String messageToBePrinted = null;
    try {

        if (textMsg != null && !textMsg.isEmpty()) {
            stub.publishToTopic(textMsg, topic);
        } else {
            messageToBePrinted = "Error: Failed to get document element from message " + textMsg;
        }
    } catch (AndesEventAdminServiceEventAdminException e) {
        messageToBePrinted = e.getFaultMessage().getEventAdminException().getErrorMessage();
    }

    if (messageToBePrinted == null) {
        messageToBePrinted = "Successfully published the message to the topic :" + topic;
    }
%>
<%=messageToBePrinted%>



