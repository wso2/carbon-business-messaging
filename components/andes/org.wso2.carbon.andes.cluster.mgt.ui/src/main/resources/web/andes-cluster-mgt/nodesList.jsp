<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.wso2.carbon.andes.cluster.mgt.ui.ClusterManagerClient" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="carbon" uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>

<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.andes.mgt.stub.AndesManagerServiceClusterMgtAdminException" %>


<fmt:bundle basename="org.wso2.carbon.andes.cluster.mgt.ui.i18n.Resources">
<carbon:jsi18n
        resourceBundle="org.wso2.carbon.andes.cluster.mgt.ui.i18n.Resources"
        request="<%=request%>"/>

<script type="text/javascript" src="../admin/js/breadcrumbs.js"></script>
<script type="text/javascript" src="../admin/js/cookies.js"></script>
<script type="text/javascript" src="../admin/js/main.js"></script>
<link rel="stylesheet" href="styles/dsxmleditor.css"/>
<link rel="stylesheet" href="styles/tree-styles.css"/>

<%
    String serverURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
    ConfigurationContext configContext =
            (ConfigurationContext) config.getServletContext().getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
    String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);

    ClusterManagerClient client;
    boolean isClusteringEnabled = false;
    String[] allClusterNodeAddresses;
    String coordinatorAddress = "";
    String nodeID = "";

    try {
        client = new ClusterManagerClient(configContext, serverURL, cookie);
        isClusteringEnabled = client.isClusteringEnabled();
        allClusterNodeAddresses = client.getAllClusterNodeAddresses();
        coordinatorAddress = client.getCoordinatorNodeAddress();
        nodeID = client.getMyNodeID();

    } catch (Exception e) {
        CarbonUIMessage.sendCarbonUIMessage(e.getMessage(), CarbonUIMessage.ERROR, request, e);

%>
<script type="text/javascript">
    location.href = "../admin/error.jsp";
    alert("error");
</script>
<%
        return;
    }

%>

<carbon:breadcrumb
        label="nodes.list"
        resourceBundle="org.wso2.carbon.andes.cluster.mgt.ui.i18n.Resources"
        topPage="false"
        request="<%=request%>"/>

<div id="middle">
    <h2>Cluster Management- WSO2 Message Broker</h2>
    <div id="workArea">
    <h2>Node Details</h2>
    <%
    try{
        if (isClusteringEnabled) {
    %>
        <div class="padding-left-ten"><h3>Node ID : <%=nodeID%></h3></div>
        <div class="padding-left-ten"><h3>Running in Cluster Mode...</h3></div>
        <table width="100%">
            <tr>
                <td width="100%">
                    <table style="width:95%" class="styledLeft">
                        <thead>
                        <tr>
                            <th><fmt:message key='node.ip'/></th>
                            <th><fmt:message key='node.port'/></th>
                            <th><fmt:message key='node.isCoordinator'/></th>
                        </tr>
                        </thead>
                        <% for(int i = 0; i < allClusterNodeAddresses.length; i++){ %>
                         <tr>
                            <td><%=
                                    allClusterNodeAddresses[i].split(":")[0]
                                %>
                            </td>
                            <td>
                                <%=
                                    allClusterNodeAddresses[i].split(":")[1]
                                %>
                            </td>
                            <td>
                                <% if(allClusterNodeAddresses[i].equals(coordinatorAddress)){%>
                                    Yes
                                <% }else{ %>
                                    No
                                <% } %>
                            </td>
                        </tr>
                        <% } %>
                    </table>
                </td>
            </tr>
        </table>
    <%  }else{ %>
        <div class="padding-left-ten"><h3>Node ID : <%=nodeID%></h3></div>
        <div class="padding-left-ten"><h3>Running in Standalone Mode...</h3></div>
    <%  }
    } catch (Exception e) { %>
        <script type="text/javascript">CARBON.showErrorDialog('Failed with BE.<%=e%>');</script>
    <%  return;
    } %>

        <%
            boolean storeHealth = false;

            try {
                storeHealth = client.getStoreHealth();
            } catch (Exception e) {
                CarbonUIMessage.sendCarbonUIMessage(e.getMessage(), CarbonUIMessage.ERROR, request, e);
            }
        %>

        <div class="margin-top-twenty-five"><h2>Message Store Details</h2></div>
        <% if(storeHealth) { %>
            <div class="padding-left-ten"><h3>Message Store Health : <span class="green-text">Healthy</span></h3></div>
        <% } else { %>
            <div class="padding-left-ten"><h3>Message Store Health : <span class="red-text">Unhealthy</span></h3></div>
        <% } %>

    </div>
</div>

</fmt:bundle>