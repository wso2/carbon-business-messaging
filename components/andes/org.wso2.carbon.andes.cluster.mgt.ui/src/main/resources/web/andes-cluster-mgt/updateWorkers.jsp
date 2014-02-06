<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.wso2.carbon.andes.cluster.mgt.ui.ClusterManagerClient" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="carbon" uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" %>
<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.andes.cluster.mgt.ui.Constants" %>


<%
        String serverURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
        ConfigurationContext configContext =
                (ConfigurationContext) config.getServletContext().getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
        String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);

        ClusterManagerClient client;
        String successResponse = "not defined";
        String queueToUpdate = request.getParameter("queueName");
        String newNodeToAssign = request.getParameter("selectedNode");
        boolean isSuccess;

        try {
            client = new ClusterManagerClient(configContext, serverURL, cookie);
            isSuccess = client.updateWorkerForQueue(queueToUpdate, newNodeToAssign);
            if (isSuccess == true) {
            successResponse =  Constants.SUCCESS;
            }
            else
            {
               successResponse = Constants.FAILURE;
            }
        } catch (Exception e) {
            successResponse = Constants.FAILURE;
            CarbonUIMessage.sendCarbonUIMessage(e.getMessage(), CarbonUIMessage.ERROR, request, e);
    %>

    %>
    <script type="text/javascript">
        location.href = "../admin/error.jsp";
        alert("error");
    </script>
    <%
            return;
        }

    %>

<div id ="workerReassignState"><%=successResponse%></div>