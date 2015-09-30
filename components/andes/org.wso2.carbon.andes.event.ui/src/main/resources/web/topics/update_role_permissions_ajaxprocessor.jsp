<%@ page import="org.apache.axis2.client.Options" %>
<%@ page import="org.apache.axis2.client.ServiceClient" %>
<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.andes.event.stub.service.AndesEventAdminServiceStub" %>
<%@ page import="org.wso2.carbon.andes.event.stub.core.TopicRolePermission" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="java.util.ArrayList" %>
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
    String message = "";

    String topic = (String) session.getAttribute("topic");
    String permissions = request.getParameter("permissions");
    String[] permissionParams = new String[0];
    if (permissions != null && !"".equals(permissions)) {
         permissionParams = permissions.split(",");
    }

    ArrayList<TopicRolePermission> topicRolePermissionArrayList = new ArrayList<TopicRolePermission>();
    for (int i = 0; i < permissionParams.length; i++) {
        String role = permissionParams[i];
        i++;
        String allowedSub = permissionParams[i];
        i++;
        String allowedPub = permissionParams[i];
        TopicRolePermission topicRolePermission = new TopicRolePermission();
        topicRolePermission.setRoleName(role);
        topicRolePermission.setAllowedToSubscribe(Boolean.parseBoolean(allowedSub));
        topicRolePermission.setAllowedToPublish(Boolean.parseBoolean(allowedPub));
        topicRolePermissionArrayList.add(topicRolePermission);
    }
    session.removeAttribute("topicRolePermission");

    TopicRolePermission[] topicRolePermissions = new TopicRolePermission[topicRolePermissionArrayList.size()];
    try {
        stub.updatePermission(topic, topicRolePermissionArrayList.toArray(topicRolePermissions));
        session.setAttribute("topicRolePermission", stub.getTopicRolePermissions(topic));
        // message content is decided when showing the pop-up as this is used when adding a new topic and also when updating permission roles for a topic.
        message = "Permissions updated successfully";
    } catch (AndesEventAdminServiceEventAdminException e) {
        message = "Error: " + e.getFaultMessage().getEventAdminException().getErrorMessage();
    }
%><%=message%>
