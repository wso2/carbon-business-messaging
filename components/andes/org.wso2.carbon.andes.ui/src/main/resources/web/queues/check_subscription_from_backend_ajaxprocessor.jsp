<%@ page import="org.wso2.carbon.andes.stub.AndesAdminServiceStub" %>
<%@ page import="org.wso2.carbon.andes.ui.UIUtils" %>
<%@ page import="org.wso2.carbon.andes.stub.admin.types.QueueRolePermission" %>
<%@ page import="org.wso2.carbon.andes.stub.AndesAdminServiceBrokerManagerAdminException" %>
<%
    AndesAdminServiceStub stub = UIUtils.getAndesAdminServiceStub(config, session, request);
    String message = "";
    String queueName;

    String queueName = request.getParameter("queueName");
    try {
        if(stub.checkForSubscriptions(queueName)){
            message = "true";
        }
        else{
            message="false";
        }
    } catch (AndesAdminServiceBrokerManagerAdminException e) {
        message = UIUtils.getHtmlString(e.getFaultMessage().getBrokerManagerAdminException().getErrorMessage());
    }

%><%=message%>