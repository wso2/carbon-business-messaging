<%@ page import="org.apache.axis2.AxisFault" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="carbon" uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" %>
<%@ page import="org.wso2.carbon.andes.stub.AndesAdminServiceStub" %>
<%@ page import="org.wso2.carbon.andes.stub.admin.types.Queue" %>
<%@ page import="org.wso2.carbon.andes.ui.UIUtils" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="javax.jms.JMSException" %>
<%@ page import="javax.naming.NamingException" %>
<%@ page import="org.wso2.carbon.andes.stub.AndesAdminServiceBrokerManagerAdminException" %>
<%@ page import="org.wso2.andes.server.queue.DLCQueueUtils" %>
<script type="text/javascript" src="js/treecontrol.js"></script>
<fmt:bundle basename="org.wso2.carbon.andes.ui.i18n.Resources">
    <carbon:jsi18n
            resourceBundle="org.wso2.carbon.andes.ui.i18n.Resources"
            request="<%=request%>"/>

    <script type="text/javascript" src="../admin/js/breadcrumbs.js"></script>
    <script type="text/javascript" src="../admin/js/cookies.js"></script>
    <script type="text/javascript" src="../admin/js/main.js"></script>
    <link rel="stylesheet" href="styles/dsxmleditor.css"/>

    <%
        AndesAdminServiceStub stub = UIUtils.getAndesAdminServiceStub(config, session, request);
        Queue[] filteredQueueList = null;
        Queue[] queueList;
        int queueCountPerPage = 20;
        int pageNumber = 0;
        int numberOfPages = 1;
        String concatenatedParams = "region=region1&item=queue_browse";
        try {
            queueList = stub.getAllQueues();
            long totalQueueCount;
            String pageNumberAsStr = request.getParameter("pageNumber");
            if (pageNumberAsStr != null) {
                pageNumber = Integer.parseInt(pageNumberAsStr);
            }

            if (queueList != null) {
                totalQueueCount = queueList.length;
                numberOfPages = (int) Math.ceil(((float) totalQueueCount) / queueCountPerPage);
                filteredQueueList = UIUtils.getFilteredQueueList(queueList, pageNumber * queueCountPerPage, queueCountPerPage);
            }
        } catch (AndesAdminServiceBrokerManagerAdminException e) {
            CarbonUIMessage.sendCarbonUIMessage(e.getFaultMessage().getBrokerManagerAdminException().getErrorMessage(), CarbonUIMessage.ERROR, request, e);
    %>

    <script type="text/javascript">
        location.href = "../admin/error.jsp";
        alert("error");
    </script>
    <%
            return;
        }
    %>


    <%
        if(request.getParameter("purge") != null && request.getParameter("purge").equalsIgnoreCase("true")){
            String queuename = request.getParameter("nameOfQueue");
            try {
                stub.purgeMessagesOfQueue(queuename);
    %>

    <script type="text/javascript">CARBON.showInfoDialog('Queue <%=queuename %> successfully purged.', function
            () {
        location.href = 'queue_details.jsp';
    });</script>

    <%
            } catch (AndesAdminServiceBrokerManagerAdminException e) {
    %>
            <script type="text/javascript">CARBON.showErrorDialog('<%=e.getFaultMessage().getBrokerManagerAdminException().getErrorMessage()%>' , function
                    () {
                location.href = 'queue_details.jsp';
            });</script>
    <%
            }
        }
    %>

    <carbon:breadcrumb
            label="queues.list"
            resourceBundle="org.wso2.carbon.andes.ui.i18n.Resources"
            topPage="false"
            request="<%=request%>"/>

    <%

        String queueName = request.getParameter("queueName");
        if (queueName != null) {
            try {
                stub.deleteQueue(queueName);
    %>
    <script type="text/javascript">

        CARBON.showInfoDialog('Queue <%=queueName %> successfully deleted.', function() {
            location.href = 'queue_details.jsp';
        });
    </script>
    <%
    } catch (AndesAdminServiceBrokerManagerAdminException e) {
    %>
            <script type="text/javascript">
            CARBON.showErrorDialog('<%=e.getFaultMessage().getBrokerManagerAdminException().getErrorMessage()%>' , function
                    () {
                location.href = 'queue_details.jsp#';
            });</script>
    <%
            }
        }
    %>

    <div id="middle">
        <h2><fmt:message key="queues.list"/></h2>

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
                              page="queue_details.jsp" pageNumberParameterName="pageNumber"
                              resourceBundle="org.wso2.carbon.andes.ui.i18n.Resources"
                              prevKey="prev" nextKey="next"
                              parameters="<%=concatenatedParams%>"/>
            <table class="styledLeft" style="width:100%">
                <thead>
                <tr>
                    <th><fmt:message key="queue.name"/></th>
                    <th><fmt:message key="queue.messageCount"/></th>
                    <th><fmt:message key="queue.view"/></th>
                    <th colspan="2"><fmt:message key="operations"/></th>
                    <th style="width:40px;"><fmt:message key="queue.operations"/></th>
                </tr>
                </thead>
                <tbody>
                <%
                    if (filteredQueueList != null) {
                        for (Queue queue : filteredQueueList) {
                            String nameOfQueue = queue.getQueueName();
                            if(!DLCQueueUtils.isDeadLetterQueue(nameOfQueue)){
                %>
                <tr>
                    <td>
                        <a href="javascript:void(0);" onclick="showManageQueueWindow('<%=queue.getQueueName()%>')"
                                    title="<fmt:message key='queues.queueDetailsToolTip'/>"><%=queue.getQueueName()%></a>
                    </td>
                    <td><%=queue.getMessageCount()%>
                    </td>
                    <td><a href="queue_messages_list.jsp?nameOfQueue=<%=nameOfQueue%>">Browse</a></td>
                    <td><img src="images/move.gif" alt=""/>&nbsp;<a href="queue_message_sender.jsp?nameOfQueue=<%=nameOfQueue%>">Publish Messages</a></td>
                    <td><img src="images/minus.gif" alt=""/>&nbsp;<a href="queue_details.jsp?purge=true&nameOfQueue=<%=nameOfQueue%>">Purge Messages</a></td>
                    <td>
                        <a style="background-image: url(../admin/images/delete.gif);"
                           class="icon-link"
                           onclick="doDelete('<%=queue.getQueueName()%>')">Delete</a>
                    </td>
                </tr>
                <%
                        	}
                        }
                    }
                %>
                </tbody>
            </table>
            <%
                }
            %>
            <div>
                <form id="deleteForm" name="input" action="" method="get"><input type="HIDDEN"
                                                                                 name="queueName"
                                                                                 value=""/></form>
            </div>
        </div>
    </div>
</fmt:bundle>
