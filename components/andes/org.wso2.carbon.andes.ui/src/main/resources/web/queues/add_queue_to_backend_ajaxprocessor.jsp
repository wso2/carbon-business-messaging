<%@ page import="org.wso2.carbon.andes.stub.AndesAdminServiceStub" %>
<%@ page import="org.wso2.carbon.andes.ui.UIUtils" %>
<%
    AndesAdminServiceStub stub = UIUtils.getAndesAdminServiceStub(config, session, request);
    String message = "";

    String queue = request.getParameter("queue");
    try {
        stub.createQueue(queue);
        message = "Added queue successfully";
        session.removeAttribute("queue");
    } catch (Exception e) {
        message = "Error:" + UIUtils.getHtmlString(e.getMessage());
    }

%><%=message%><%
    session.setAttribute("queue", queue);
%>