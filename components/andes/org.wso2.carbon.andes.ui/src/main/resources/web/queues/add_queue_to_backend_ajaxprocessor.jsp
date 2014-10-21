<%@ page import="org.wso2.carbon.andes.stub.AndesAdminServiceStub" %>
<%@ page import="org.wso2.carbon.andes.ui.UIUtils" %>
<%@ page import="org.wso2.carbon.andes.stub.admin.types.QueueRolePermission" %>
<%@ page import="org.wso2.carbon.andes.stub.AndesAdminServiceBrokerManagerAdminException" %>
<%
    AndesAdminServiceStub stub = UIUtils.getAndesAdminServiceStub(config, session, request);
    String message = "";

    String queue = request.getParameter("queue");
    try {
        stub.createQueue(queue);
        message = "Queue added successfully";
        session.removeAttribute("queue");
    } catch (AndesAdminServiceBrokerManagerAdminException e) {
        message = UIUtils.getHtmlString(e.getFaultMessage().getBrokerManagerAdminException().getErrorMessage());
    }

%><%=message%><%
    QueueRolePermission[] queueRolePermission = stub.getQueueRolePermission(queue);
    session.setAttribute("queue", queue);
    session.setAttribute("queueRolePermission", queueRolePermission);
%>
