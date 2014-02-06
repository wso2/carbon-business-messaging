<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.wso2.carbon.andes.cluster.mgt.ui.ClusterManagerClient" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="carbon" uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>

<%@ page import="org.wso2.carbon.andes.mgt.stub.types.carbon.NodeDetail" %>
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
    String cassandraConnection = "";
    String zookeeperConnection = "";
    String nodeID = "";
    boolean isClusteringEnabled = false;

    try {
        client = new ClusterManagerClient(configContext, serverURL, cookie);
        cassandraConnection = client.getCassandraConnection();
        zookeeperConnection = client.getZookeeperConnection();
        nodeID = client.getMyNodeID();
        isClusteringEnabled = client.isClusteringEnabled();
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

<script type="text/javascript">

    function updateThroughput(hostName, index) {
        jQuery.ajax({
            url:'nodeUpdateQueries.jsp?hostName=' + hostName,
            success:function (data) {
                jQuery('#throughPutCell' + index).html($('#nodeThroughput', data));
            }
        });
    }

    function updateNumOfTopicsAndQueues(hostName, index) {
        jQuery.ajax({
            url:'nodeUpdateQueries.jsp?hostName=' + hostName,
            success:function (data) {
                jQuery('#queueLinkCell' + index).html($('#nodeQueues', data));
                jQuery('#topicLinkCell' + index).html($('#nodeTopics', data));
            }
        });
    }

    function updateMemoryUsage(hostName, index) {
        jQuery.ajax({
            url:'nodeUpdateQueries.jsp?hostName=' + hostName,
            success:function (data) {
                jQuery('#memoryUsageCell' + index).html($('#memoryUsage', data));
            }
        });
    }


</script>

<carbon:breadcrumb
        label="nodes.list"
        resourceBundle="org.wso2.carbon.andes.cluster.mgt.ui.i18n.Resources"
        topPage="false"
        request="<%=request%>"/>

<div id="middle">
    <h2>Cluster Management- WSO2 Message Broker</h2>

    <div id="workArea">
        <table width="100%">
            <tr>
                <td width="75%">
                    <table style="width:80%" class="styledLeft">
                        <thead>
                        <tr>
                            <th width=60%><I><fmt:message key='cassandra.connection'/></I></th>
                            <th><I><%=cassandraConnection%>
                            </I></th>
                        </tr>
                        </thead>
                    </table>
                    <br>
                    <br>
                    <table style="width:80%" class="styledLeft">
                        <thead>
                        <tr>
                            <th width=60%><I><fmt:message key='zookeeper.connection'/></I></th>
                            <th><I><%=zookeeperConnection%>
                            </I></th>
                        </tr>
                        </thead>
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
                        <%
                            if (isClusteringEnabled) {
                        %> <p style="margin-left:15px">Running in Cluster Mode...</p>
                        <%
                        } else {
                        %> <p style="margin-left:15px">Running in Standalone Mode...</p>
                        <%}%>
                    </div>
                </td>
            </tr>
        </table>

        <br>
        <br>
        <br>

        <%
            if (isClusteringEnabled) {
                NodeDetail[] nodeDetailArray;
                int totalNodeCount = client.getNumOfNodes();
                int nodeCountPerPage = 20;
                int pageNumber = 0;
                String pageNumberAsStr = request.getParameter("pageNumber");
                if (pageNumberAsStr != null) {
                    pageNumber = Integer.parseInt(pageNumberAsStr);
                }
                int numberOfPages = (int) Math.ceil(((float) totalNodeCount) / nodeCountPerPage);
                try {
                    nodeDetailArray = client.getAllNodeDetail(pageNumber * nodeCountPerPage, nodeCountPerPage);
                    if (nodeDetailArray == null || nodeDetailArray.length == 0) {
        %>
        <fmt:message key='no.nodes.available'/>
        <%
        } else {
        %>
        <input type="hidden" name="pageNumber" value="<%=pageNumber%>"/>
        <carbon:paginator pageNumber="<%=pageNumber%>" numberOfPages="<%=numberOfPages%>"
                          page="nodesList.jsp" pageNumberParameterName="pageNumber"
                          resourceBundle="org.wso2.carbon.andes.cluster.mgt.ui.i18n.Resources"
                          prevKey="prev" nextKey="next"
                          parameters="<%=""%>"/>
        <table class="styledLeft">
            <thead>
            <tr>
                <th align="middle" width=50%><fmt:message key='node.id'/></th>
                <th align="middle" width=50%><fmt:message key='node.queue.workers'/></th>
            </tr>
            </thead>
            <tbody>

            <%
                int index = 0;
                for (NodeDetail aNodeDetail : nodeDetailArray) {
                    if (aNodeDetail != null) {
                        String nodeId = aNodeDetail.getNodeId();
                        String hostName = aNodeDetail.getHostName();
                        String ipAddress = aNodeDetail.getIpAddress();
                        String zookeeperID = aNodeDetail.getZookeeperID();
                        index++;

            %>
            <tr>
                <td>
                    <%=zookeeperID%>
                </td>
                <td align="right">
                    <a href="queue_List.jsp?hostName=<%=hostName%>&IPAddress=<%=ipAddress%>&isClusteringEnabled=<%=client.isClusteringEnabled()%>"><abbr
                            id="queueLinkCell<%=index%>"><%=aNodeDetail.getNumOfQueues() %>
                    </abbr>
                    </a>
                </td>
            </tr>
            <%
                    }
                }
            %>
            </tbody>
        </table>
        <%
            }
        } catch (Exception e) {
        %>
        <script type="text/javascript">CARBON.showErrorDialog('Failed with BE.<%=e%>');</script>
        <%
                    return;
                }
            }%>

    </div>
</div>

</fmt:bundle>