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
<link rel="stylesheet" href="../styles/dsxmleditor.css"/>
<link rel="stylesheet" href="../styles/tree-styles.css"/>

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
        e.printStackTrace();
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

<carbon:breadcrumb
        label="nodes.list"
        resourceBundle="org.wso2.carbon.andes.cluster.mgt.ui.i18n.Resources"
        topPage="false"
        request="<%=request%>"/>

<div id="middle">
    <h2>Cluster Management- WSO2 Message Broker</h2>
    <div id="workArea">

    <%
    try{
        if (isClusteringEnabled) {
    %>
        <table width="100%">
            <tr>
                <td width="75%">
                    <table style="width:95%" class="styledLeft">
                        <thead>
                        <tr>
                            <th><I><fmt:message key='node.ip'/></I></th>
                            <th><I><fmt:message key='node.port'/></I></th>
                            <th><I><fmt:message key='node.isCoordinator'/></I></th>
                        </tr>
                        </thead>
                        <% for(int i = 0; i < allClusterNodeAddresses.length; i++){ %>
                         <tr>
                            <td><%=allClusterNodeAddresses[i].split(":")[0]%></td>
                            <td><%=allClusterNodeAddresses[i].split(":")[1]%></td>
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
                <td width="25%">
                    <div style="background-color:rgba(10,46,38,0.15);width:100%;height:100px;border:1px solid #000">
                        <br/>
                        <br/>

                        <p style="font-size:30px;margin-left:15px">Node:<%=nodeID%>
                        </p>
                        <br/>
                        <br/>
                        <p style="margin-left:15px">Running in Cluster Mode...</p>
                    </div>
                </td>
            </tr>
        </table>
    <%  }else{ %>
        <div style="background-color:rgba(10,46,38,0.15);width:100%;height:100px;border:1px solid #000">
            <br/>
            <br/>
            <p style="font-size:30px;margin-left:15px">Node:<%=nodeID%>
            </p>
            <br/>
            <br/><p style="margin-left:15px">Running in Standalone Mode...</p>
        </div>
    <%  }
    } catch (Exception e) { %>
        <script type="text/javascript">CARBON.showErrorDialog('Failed with BE.<%=e%>');</script>
    <%  return;
    } %>
    </div>
</div>

</fmt:bundle>