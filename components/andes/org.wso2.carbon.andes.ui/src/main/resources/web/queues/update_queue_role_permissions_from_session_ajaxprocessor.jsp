<%@ page import="org.wso2.carbon.andes.stub.AndesAdminServiceBrokerManagerAdminException" %>
<%@ page import="org.wso2.carbon.andes.stub.AndesAdminServiceStub" %>
<%@ page import="org.wso2.carbon.andes.stub.admin.types.QueueRolePermission" %>
<%@ page import="org.wso2.carbon.andes.ui.UIUtils" %>
<%@ page import="java.net.URI" %>
<%@ page import="java.util.ArrayList" %>
<%
    AndesAdminServiceStub stub = UIUtils.getAndesAdminServiceStub(config, session, request);
    String message = "";

    String queue = (String) session.getAttribute("queue");

    ArrayList<QueueRolePermission> queueRolePermissionArrayList = (ArrayList<QueueRolePermission>) session.getAttribute("queueRolePermissions");
    QueueRolePermission[] queueRolePermissions = new QueueRolePermission[queueRolePermissionArrayList.size()];
    queueRolePermissions = queueRolePermissionArrayList.toArray(queueRolePermissions);

    try {
        stub.updatePermission(queue, queueRolePermissions);
        message = "";
    } catch (AndesAdminServiceBrokerManagerAdminException e) {
        message = e.getFaultMessage().getBrokerManagerAdminException().getErrorMessage();
    }

    session.removeAttribute("queueRolePermissions");
%><%=message%>
