<%@ page import="org.wso2.carbon.andes.stub.AndesAdminServiceStub" %>
<%@ page import="org.wso2.carbon.andes.ui.UIUtils" %>
<%@ page import="org.wso2.carbon.andes.stub.admin.types.QueueRolePermission" %>
<%@ page import="org.wso2.carbon.andes.stub.AndesAdminServiceBrokerManagerAdminException" %>
<%
    AndesAdminServiceStub stub = UIUtils.getAndesAdminServiceStub(config, session, request);
    String message = "";
    boolean isExclusiveConsumer;

    String queue = request.getParameter("queue");
    if (null!=request.getParameter("exclusiveConsumer")) {
        isExclusiveConsumer = true;
    } else {
       isExclusiveConsumer = false;
    }
    try {
        stub.createQueue(queue, isExclusiveConsumer);
        message = "Queue added successfully";
        session.removeAttribute("queue");
    } catch (AndesAdminServiceBrokerManagerAdminException e) {
        message = UIUtils.getHtmlString(e.getFaultMessage().getBrokerManagerAdminException().getErrorMessage());
    }

%><%=message%><%
    session.setAttribute("queue", queue);
    session.setAttribute("isExclusiveConsumer"+queue, isExclusiveConsumer);
%>