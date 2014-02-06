<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.wso2.carbon.andes.cluster.mgt.ui.ClusterManagerClient" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="carbon" uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.andes.cluster.mgt.ui.Constants" %>
<%@ page import= "org.wso2.carbon.andes.mgt.stub.types.carbon.Topic" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>

<fmt:bundle basename="org.wso2.carbon.andes.cluster.mgt.ui.i18n.Resources">
    <carbon:jsi18n
            resourceBundle="org.wso2.carbon.andes.cluster.mgt.ui.i18n.Resources"
            request="<%=request%>"/>

    <script type="text/javascript" src="../admin/js/breadcrumbs.js"></script>
    <script type="text/javascript" src="../admin/js/cookies.js"></script>
    <script type="text/javascript" src="../admin/js/main.js"></script>
    <link rel="stylesheet" href="../qpid/css/dsxmleditor.css" />
    <script type="text/javascript" >

        function updateNumOfSubscribers(topicName, index)
        {
            jQuery.ajax({
                url:'topicUpdateQueries.jsp?topicName=' + topicName,
                success:function(data) {
                    jQuery('#subscriberCountCell' + index).html($('#subscriberCount', data));
                }
            });
        }

    </script>

    <%
        String serverURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
        ConfigurationContext configContext =
                (ConfigurationContext) config.getServletContext().getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
        String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);

        ClusterManagerClient client;
        Topic[] topicList;
        String requestedHostName = request.getParameter("hostName");
        String IPAddressOfHost = request.getParameter("IPAddress");

        long totalQueueCount;
        int topicCountPerPage = 20;
        int pageNumber = 0;
        String pageNumberAsStr = request.getParameter("pageNumber");
        if (pageNumberAsStr != null) {
            pageNumber = Integer.parseInt(pageNumberAsStr);
        }
        int numberOfPages;


        try {
            client = new ClusterManagerClient(configContext, serverURL, cookie);
            totalQueueCount = client.updateQueueCountForNode(requestedHostName);
            numberOfPages =  (int) Math.ceil(((float) totalQueueCount) / topicCountPerPage);
            topicList  = client.getAllTopics(pageNumber * topicCountPerPage, topicCountPerPage);
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

    <carbon:breadcrumb
    label="topics.list"
    resourceBundle="org.wso2.carbon.andes.cluster.mgt.ui.i18n.Resources"
    topPage="false"
    request="<%=request%>"/>

    <div id="middle">
        <h2><fmt:message key="topics.list"/></h2>
        <h2>Node Name: <%=requestedHostName%> IP Address: <%=IPAddressOfHost%></h2>
        <div id="workArea">

        <%
                if (topicList == null) {
            %>
            <fmt:message key='no.topics.created'/>
            <%
            } else {

            %>
            <input type="hidden" name="pageNumber" value="<%=pageNumber%>"/>
            <carbon:paginator pageNumber="<%=pageNumber%>" numberOfPages="<%=numberOfPages%>"
                              page="topic_List.jsp" pageNumberParameterName="pageNumber"
                              resourceBundle="org.wso2.carbon.andes.cluster.mgt.ui.i18n.Resources"
                              prevKey="prev" nextKey="next"
                              parameters="<%="test"%>"/>
            <table class="styledLeft" style="width:100%">
                <thead>
                <tr>
                    <th><fmt:message key="topic.name"/></th>
                    <th colspan="2"><fmt:message key="topic.subscribers"/></th>
                </tr>
                </thead>
                <tbody>
                <%
                    if (topicList != null) {
                        int index = 0;
                        for (Topic topic : topicList) {
                              String topicName = topic.getName();
                %>
                <tr>
                    <td>
                    <%=topicName%>
                    </td>
                    <td id abbr="subscriberCountCell<%=index%>"><%=topic.getNumberOfSubscribers()%>
                    </td>
                    <td>
                        <a style="background-image: url(images/refresh.gif);"
                           class="icon-link" onclick="updateNumOfSubscribers('<%=topicName%>','<%=index%>')"></a>
                    </td>
                </tr>
                <%
                            index++;
                        }
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