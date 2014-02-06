<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.wso2.carbon.andes.cluster.mgt.ui.ClusterManagerClient" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="carbon" uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.andes.cluster.mgt.ui.Constants" %>
<%@ page import="org.wso2.carbon.andes.mgt.stub.types.carbon.Queue" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="java.util.List" %>
<%@ page import="org.wso2.carbon.andes.mgt.stub.types.carbon.NodeDetail" %>

<fmt:bundle basename="org.wso2.carbon.andes.cluster.mgt.ui.i18n.Resources">
<carbon:jsi18n
        resourceBundle="org.wso2.carbon.andes.cluster.mgt.ui.i18n.Resources"
        request="<%=request%>"/>

<script type="text/javascript" src="../admin/js/breadcrumbs.js"></script>
<script type="text/javascript" src="../admin/js/cookies.js"></script>
<script type="text/javascript" src="../admin/js/main.js"></script>
<link rel="stylesheet" href="../qpid/css/dsxmleditor.css"/>

<%
    String serverURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
    ConfigurationContext configContext =
            (ConfigurationContext) config.getServletContext().getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
    String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);

    ClusterManagerClient client;
    Queue[] queueList;
    String requestedHostName = request.getParameter("hostName");
    String IPAddressOfHost = request.getParameter("IPAddress");
    boolean isClusteringEnabled = Boolean.parseBoolean(request.getParameter("isClusteringEnabled"));
    String concatenatedParams = "hostName="+requestedHostName+"&IPAddress="+IPAddressOfHost+"&isClusteringEnabled="+isClusteringEnabled;

    long totalQueueCount;
    int queueCountPerPage = 20;
    int pageNumber = 0;
    String pageNumberAsStr = request.getParameter("pageNumber");
    if (pageNumberAsStr != null) {
        pageNumber = Integer.parseInt(pageNumberAsStr);
    }
    int numberOfPages;


    try {
        client = new ClusterManagerClient(configContext, serverURL, cookie);
        totalQueueCount = client.updateQueueCountForNode(requestedHostName);
        numberOfPages = (int) Math.ceil(((float) totalQueueCount) / queueCountPerPage);
        queueList = client.getQueuesOfNode(requestedHostName, pageNumber * queueCountPerPage, queueCountPerPage);

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

    function updateWorkerLocationForQueue(queueName, index, successMessage) {
        var originalQueueNode = getUrlVars()["hostName"];
        var selectedNode = $('#combo' + index + ' option:selected').val();
        if (originalQueueNode == selectedNode) {
            CARBON.showErrorDialog("You cannot move queue worker for " + queueName + " from current node to itself");
            return;
        }
        var isClusteringEnabled = getUrlVars()["isClusteringEnabled"];
        if (!isClusteringEnabled) {
            CARBON.showErrorDialog("This operation is not allowed when not in clustered mode");
            return;
        }

        $.ajax({
            url:'updateWorkers.jsp?queueName=' + queueName + '&selectedNode=' + selectedNode,
            async:false,
            dataType:"html",
            success:function (data) {
                html = data;
                var isSuccess = $(data).find('#workerReassignState').text();
                if (isSuccess == successMessage) {
                    var result = CARBON.showInfoDialog("Worker thread of " + queueName + " queue successfully moved to " + selectedNode,
                            function () {
                                location.href = 'nodesList.jsp';
                            });
                }
                else {
                    CARBON.showErrorDialog("Failed moving " + queueName + " to " + selectedNode);
                }
            },
            failure:function (data) {
                if (data.responseText !== undefined) {
                    CARBON.showErrorDialog("Error " + data.status + "\n Following is the message from the server.\n" + data.responseText);
                }
            }
        });
    }

    function getUrlVars() {
        var vars = [], hash;
        var hashes = window.location.href.slice(window.location.href.indexOf('?') + 1).split('&');
        for (var i = 0; i < hashes.length; i++) {
            hash = hashes[i].split('=');
            vars.push(hash[0]);
            vars[hash[0]] = hash[1];
        }
        return vars;
    }

</script>

<carbon:breadcrumb
        label="queues.list"
        resourceBundle="org.wso2.carbon.andes.cluster.mgt.ui.i18n.Resources"
        topPage="false"
        request="<%=request%>"/>

<div id="middle">
    <h2><fmt:message key="queues.list"/></h2>

    <h2><fmt:message key="node.id"/>: <%=requestedHostName%>
    </h2>

    <div id="workArea">

        <%
            if (queueList == null) {
        %>
        No queues are created.
        <%
        } else {

        %>
        <input type="hidden" name="pageNumber" value="<%=pageNumber%>"/>
        <carbon:paginator pageNumber="<%=pageNumber%>" numberOfPages="<%=numberOfPages%>"
                          page="queue_List.jsp" pageNumberParameterName="pageNumber"
                          resourceBundle="org.wso2.carbon.andes.cluster.mgt.ui.i18n.Resources"
                          prevKey="prev" nextKey="next"
                          parameters="<%=concatenatedParams%>"/>
        <table class="styledLeft" style="width:100%">
            <thead>
            <tr>
                <th><fmt:message key="queue.name"/></th>
                <th><fmt:message key="queue.messageCount"/></th>
                <th><fmt:message key="queue.worker"/></th>
            </tr>
            </thead>
            <tbody>
            <%
                if (queueList != null) {
                    int index = 0;
                    for (Queue queue : queueList) {
                        String queueName = queue.getQueueName();
                        long queueSize = queue.getQueueDepth();
                        String queueSizeWithPostFix;
                        if (queueSize > 1000000000) {
                            queueSizeWithPostFix = Long.toString(queueSize / 1000000000) + " GB";
                        } else if (queueSize > 1000000) {
                            queueSizeWithPostFix = Long.toString(queueSize / 1000000) + " MB";
                        } else if (queueSize > 1000) {
                            queueSizeWithPostFix = Long.toString(queueSize / 1000) + " KB";
                        } else {
                            queueSizeWithPostFix = Long.toString(queueSize) + " bytes";
                        }

            %>
            <tr>
                <td>
                    <%=queueName%>
                </td>
                <td><%=queue.getMessageCount()%>
                </td>
                <td>
                    <select id="combo<%=index%>">
                        <%
                            NodeDetail[] nodeDetailArray = client.getAllNodeDetail(0, 1000);
                            for (int count = 0; count < nodeDetailArray.length; count++) {
                                String nodeName = nodeDetailArray[count].getHostName();
                                if (nodeName.equals(requestedHostName)) {
                        %>
                        <option value="<%=nodeName%>" selected><%=nodeName%>
                        </option>
                        <%
                        } else {
                        %>
                        <option value="<%=nodeName%>"><%=nodeName%>
                        </option>
                        <%
                                }


                            }
                        %>
                    </select>
                </td>
            </tr>
            <%
                    }

                    index++;
                }
            %>
            </tbody>
        </table>
        <%
            }
        %>
    </div>
</div>
</fmt:bundle>