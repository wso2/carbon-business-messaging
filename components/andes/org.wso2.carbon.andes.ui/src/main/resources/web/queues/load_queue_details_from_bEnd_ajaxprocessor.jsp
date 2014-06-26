<%@ page import="org.wso2.carbon.andes.stub.AndesAdminServiceStub" %>
<%@ page import="org.wso2.carbon.andes.ui.UIUtils" %>

<%
    AndesAdminServiceStub stub = UIUtils.getAndesAdminServiceStub(config, session, request);
    String nameOfQueue = request.getParameter("queueName");
    session.removeAttribute("queue");
    session.removeAttribute("queueRolePermission");
    session.setAttribute("queue", nameOfQueue);
    session.setAttribute("queueRolePermission", stub.getQueueRolePermission(nameOfQueue));
%>
