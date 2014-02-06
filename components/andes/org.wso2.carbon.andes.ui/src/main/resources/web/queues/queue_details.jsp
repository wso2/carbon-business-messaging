<%@ page import="org.apache.axis2.AxisFault" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="carbon" uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" %>
<%@ page import="org.wso2.carbon.andes.stub.AndesAdminServiceStub" %>
<%@ page import="org.wso2.carbon.andes.stub.admin.types.Queue" %>
<%@ page import="org.wso2.carbon.andes.ui.UIUtils" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="javax.jms.JMSException" %>
<%@ page import="javax.naming.NamingException" %>
<%@ page import="org.wso2.carbon.andes.ui.client.QueueReceiverClient" %>
<%@ page import="javax.jms.Message" %>
<script type="text/javascript" src="js/treecontrol.js"></script>
<fmt:bundle basename="org.wso2.carbon.andes.ui.i18n.Resources">
    <carbon:jsi18n
            resourceBundle="org.wso2.carbon.andes.ui.i18n.Resources"
            request="<%=request%>"/>

    <script type="text/javascript" src="../admin/js/breadcrumbs.js"></script>
    <script type="text/javascript" src="../admin/js/cookies.js"></script>
    <script type="text/javascript" src="../admin/js/main.js"></script>
    <link rel="stylesheet" href="../qpid/css/dsxmleditor.css"/>

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
        } catch (Exception e) {
            CarbonUIMessage.sendCarbonUIMessage(e.getMessage(), CarbonUIMessage.ERROR, request, e);
            e.printStackTrace();
    %>

    <script type="text/javascript">
        location.href = "../admin/error.jsp";
        alert("error");
    </script>
    <%
            return;
        }
    %>

    <%-- This block is for handling queue purge operation--%>
    <%
        if(request.getParameter("purge") != null && request.getParameter("purge").equalsIgnoreCase("true")){
            String queuename = request.getParameter("nameOfQueue");
            String accesskey = stub.getAccessKey();
            String username = stub.getCurrentUser();
            try {
                QueueReceiverClient receiverClient = new QueueReceiverClient();
                receiverClient.registerReceiver(queuename, username, accesskey);
                Message message = null;
                while ((message = receiverClient.getQueueConsumer().receive(30000)) != null) {
                }
                receiverClient.closeReceiver();

   %>

    <script type="text/javascript">CARBON.showInfoDialog('Queue <%=queuename %> successfully purged.', function
            () {
        location.href = 'queue_details.jsp';
    });</script>

    <%
            } catch (NamingException e) {
    %>
            <script type="text/javascript">CARBON.showInfoDialog('<%=e.getMessage()%>' , function
                    () {
                location.href = 'queue_details.jsp';
            });</script>
    <%
                e.printStackTrace();
            } catch (JMSException e) {
    %>
            <script type="text/javascript">CARBON.showInfoDialog('<%=e.getMessage()%>' , function
                    () {
                location.href = 'queue_details.jsp';
            });</script>
    <%
                e.printStackTrace();
            }

        }
    %>

    <%-- This block is for handling show message count operation--%>
    <%
        if(request.getParameter("show") != null && request.getParameter("show").equalsIgnoreCase("true")){
            String queueName = request.getParameter("nameOfQueue");

    %>
    <script type="text/javascript">
      CARBON.showConfirmationDialog('Do you wish to view message count in queue   ' + '<%=queueName%>' + '?',
              function(){
                  CARBON.showInfoDialog('Message Count: ' + '<%=stub.getMessageCountForQueue(queueName)%>',function
                          () {
                      location.href = 'queue_details.jsp';
                  });

              });

    </script>

    <%
        }
    %>


    <script type="text/javascript">

        function updateWorkerLocationForQueue(queueName, index, successMessage) {
            var selectedNode = $('#combo' + index + ' option:selected').val();
            $.ajax({
                url:'updateWorkers.jsp?queueName=' + queueName + '&selectedNode=' + selectedNode,
                async:false,
                dataType:"html",
                success:function (data) {
                    html = data;
                    var isSuccess = $(data).find('#workerReassignState').text();
                    if (isSuccess == successMessage) {
                        var result = CARBON.showInfoDialog("Worker thread of" + queueName + "queue successfully moved to" + selectedNode,
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

        /* This function will display a loading dialog window till the selected queue is
         *  successfully purged.*/
        function waitTillPurgeDone(){
            CARBON.showLoadingDialog('Purging is currently in Progress. Please Wait...', null);
        }
    </script>

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
    <script type="text/javascript">CARBON.showInfoDialog('Queue <%=queueName %> successfully deleted.', function
            () {
        location.href = 'queue_details.jsp';
    });</script>
    <%

    } catch (AxisFault fault) {
    %>
    <script type="text/javascript">
//        location.href = 'queue_details.jsp#';
        CARBON.showErrorDialog('<%=fault.getMessage()%>',function
                () {
            location.href = 'queue_details.jsp#';
        });
    </script>
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
                %>
                <tr>
                    <td>
                        <%=queue.getQueueName()%>
                    </td>

                <%
                    if(UIUtils.isDefaultMsgCountViewOptionConfigured(stub)){
                %>
                    <td><%=queue.getMessageCount()%></td>
                <%
                    } else {
                %>
                    <td><a href="queue_details.jsp?show=true&nameOfQueue=<%=nameOfQueue%>">Show</a></td>
                <%
                    }
                %>

                    <td><a href="queue_messages_list.jsp?nameOfQueue=<%=nameOfQueue%>">Browse</a></td>
                    <td><img src="images/move.gif" alt=""/>&nbsp;<a href="queue_message_sender.jsp?nameOfQueue=<%=nameOfQueue%>">Publish Messages</a></td>
                    <td><img src="images/minus.gif" alt=""/>&nbsp;<a href="queue_details.jsp?purge=true&nameOfQueue=<%=nameOfQueue%>" onclick="waitTillPurgeDone()">Purge Messages</a></td>
                    <td>
                        <a style="background-image: url(../admin/images/delete.gif);"
                           class="icon-link"
                           onclick="doDelete('<%=queue.getQueueName()%>')">Delete</a>
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
            %>
            <div>
                <form id="deleteForm" name="input" action="" method="get"><input type="HIDDEN"
                                                                                 name="queueName"
                                                                                 value=""/></form>
            </div>
        </div>
    </div>
</fmt:bundle>
