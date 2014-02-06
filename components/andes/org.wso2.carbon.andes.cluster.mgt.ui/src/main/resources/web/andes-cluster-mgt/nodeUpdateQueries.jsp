<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.wso2.carbon.andes.cluster.mgt.ui.ClusterManagerClient" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="carbon" uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>


    <%
        String serverURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
        ConfigurationContext configContext =
                (ConfigurationContext) config.getServletContext().getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
        String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);

        ClusterManagerClient client;
        String requestedHostName = request.getParameter("hostName");
        long nodeThroughput;
        long numOfQueues;
        long numOfTopics;
        long nodeMemoryUsage;

        try {
            client = new ClusterManagerClient(configContext, serverURL, cookie);
            nodeThroughput = client.updateThroughputForNode(requestedHostName);
            numOfQueues = client.updateQueueCountForNode(requestedHostName);
            numOfTopics = client.updateTopicCount();
            nodeMemoryUsage = client.updateMemoryUsage(requestedHostName);
        } catch (Exception e) {
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

<p id = "nodeThroughput"><%=nodeThroughput%></p>
<p id="nodeQueues"><%=numOfQueues%></p>
<p id = "nodeTopics"><%=numOfTopics%></p>
<p id="memoryUsage"><%=nodeMemoryUsage%></p>
