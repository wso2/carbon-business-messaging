<%@page import="java.sql.Array" %>
<%@page import="org.wso2.carbon.andes.stub.admin.types.Queue" %>
<%@page import="org.apache.axis2.AxisFault" %>
<%@ page import="org.wso2.carbon.andes.ui.UIUtils" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="carbon" uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.andes.stub.AndesAdminServiceStub" %>
<%@ page import="org.wso2.carbon.andes.stub.AndesAdminServiceBrokerManagerAdminException" %>
<%@ page import="org.wso2.carbon.andes.stub.admin.types.Message" %>
<%@ page import="org.wso2.andes.configuration.enums.AndesConfiguration" %>
<%@ page import="org.wso2.andes.configuration.AndesConfigurationManager" %>
<%@ page import="org.wso2.andes.store.cassandra.ServerStartupRecoveryUtils" %>
<%@ page import="javax.xml.bind.SchemaOutputResolver" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="org.apache.commons.lang.StringUtils" %>
<%@ page import="org.wso2.andes.kernel.AndesConstants" %>
<%@ page import="org.wso2.andes.server.queue.DLCQueueUtils" %>
<script type="text/javascript" src="js/treecontrol.js"></script>
<fmt:bundle basename="org.wso2.carbon.andes.ui.i18n.Resources">
    <jsp:include page="resources-i18n-ajaxprocessor.jsp"/>
    <carbon:jsi18n resourceBundle="org.wso2.carbon.andes.ui.i18n.Resources" request="<%=request%>"/>

    <script type="text/javascript" src="../admin/js/breadcrumbs.js"></script>
    <script type="text/javascript" src="../admin/js/cookies.js"></script>
    <script type="text/javascript" src="../admin/js/main.js"></script>
    <link rel="stylesheet" href="styles/dsxmleditor.css"/>
    <script type="text/javascript">

        function toggleCheck(source) {

            var allcheckBoxes = document.getElementsByName("checkbox");
            for (var i = 0; i < allcheckBoxes.length; i++) {
                if (allcheckBoxes[i].type == 'checkbox') {
                    allcheckBoxes[i].checked = source.checked;
                }
            }
        }

        function checkSelectAll(source) {
            var selectAllCheckBox = document.getElementsByName("selectAllCheckBox");
            if (selectAllCheckBox[0].checked) {
                selectAllCheckBox[0].checked = source.checked;
            }

            var allcheckBoxesInPage = $("input:checkbox");

            var totalCheckboxCount = allcheckBoxesInPage.size() - 1; ////removing the select all check box from the count.

            var checkedBoxes = $("input[@type=checkbox]:checked"); //the checked box count
            var checkedBoxesCount = checkedBoxes.size();

            if (totalCheckboxCount == checkedBoxesCount) {
                selectAllCheckBox[0].checked = true;
            }
        }

        function filterByQueue() {
            var filterQueueText = $("#queueName").val();
            if (filterQueueText == "") {
                $("#filterQueueName").val("DeadLetterChannel");
            } else {
                $("#filterQueueName").val(filterQueueText);
            }
            document.getElementById('dummyForm').submit();
        }

        $(document).ready(function () {
            removeFirstAndLastPaginations();

//            $("#queueName").autocomplete({
//                source: getAllQueues()
//            });
        })
    </script>

    <%
        AndesAdminServiceStub stub = UIUtils.getAndesAdminServiceStub(config, session, request);
        String nameOfQueue = request.getParameter("nameOfQueue");
        String concatenatedParameters = "nameOfQueue=" + nameOfQueue;
        String pageNumberAsStr = request.getParameter("pageNumber");
        int msgCountPerPage = AndesConfigurationManager.readValue(
                AndesConfiguration.MANAGEMENT_CONSOLE_MESSAGE_BATCH_SIZE_FOR_BROWSER_SUBSCRIPTIONS);

        Map<Integer, Long> pageNumberToMessageIdMap = null;
        if (request.getSession().getAttribute("pageNumberToMessageIdMap") != null) {
            pageNumberToMessageIdMap = (Map<Integer, Long>) request.getSession().getAttribute("pageNumberToMessageIdMap");
        } else {
            pageNumberToMessageIdMap = new HashMap<Integer, Long>();
        }

        int pageNumber = 0;
        int numberOfPages = 1;
        long totalMsgsInQueue;
        long startMessageIdOfPage;
        long nextMessageIdToRead = 0L;

        Message[] filteredMsgArray = null;
        if (null == nameOfQueue) {
            nameOfQueue = AndesConstants.DEAD_LETTER_QUEUE_SUFFIX;
        }
        if (null != pageNumberAsStr) {
            pageNumber = Integer.parseInt(pageNumberAsStr);
        }
        try {
            // The total number of messages depends on whether the filter was used or not
            if (DLCQueueUtils.isDeadLetterQueue(nameOfQueue)) {
                totalMsgsInQueue = stub.getTotalMessagesInQueue(nameOfQueue);
            } else {
                totalMsgsInQueue = stub.getNumberMessagesInDLCForQueue(nameOfQueue);
            }
            numberOfPages = (int) Math.ceil(((float) totalMsgsInQueue) / msgCountPerPage);
            if (totalMsgsInQueue == 0L) {
                nextMessageIdToRead = ServerStartupRecoveryUtils.getMessageIdToCompleteRecovery();
            } else if (pageNumberToMessageIdMap.size() > 0) {
                if (pageNumberToMessageIdMap.get(pageNumber) != null) {
                    nextMessageIdToRead = pageNumberToMessageIdMap.get(pageNumber);
                }
            }
            // The source of the messages depends on whether the filter was used or not
            if (DLCQueueUtils.isDeadLetterQueue(nameOfQueue)) {
                filteredMsgArray = stub.browseQueue(nameOfQueue, nextMessageIdToRead, msgCountPerPage);
            } else {
                filteredMsgArray = stub.getMessageInDLCForQueue(nameOfQueue, nextMessageIdToRead, msgCountPerPage);
            }
            if (filteredMsgArray != null && filteredMsgArray.length > 0) {
                startMessageIdOfPage = filteredMsgArray[0].getAndesMsgMetadataId();
                pageNumberToMessageIdMap.put(pageNumber, startMessageIdOfPage);
                nextMessageIdToRead = filteredMsgArray[filteredMsgArray.length - 1].getAndesMsgMetadataId() + 1;
                pageNumberToMessageIdMap.put((pageNumber + 1), nextMessageIdToRead);
                request.getSession().setAttribute("pageNumberToMessageIdMap", pageNumberToMessageIdMap);
            }
        } catch (AndesAdminServiceBrokerManagerAdminException e) {
            CarbonUIMessage.sendCarbonUIMessage(e.getFaultMessage().getBrokerManagerAdminException().getErrorMessage(),
                                                CarbonUIMessage.ERROR, request, e);
        }

        // When searched for a queue, the queue name should persist in the text box.
        String previouslySearchedQueueName = StringUtils.EMPTY;
        if (!DLCQueueUtils.isDeadLetterQueue(nameOfQueue)) {
            previouslySearchedQueueName = nameOfQueue;
        }
    %>
    <carbon:breadcrumb
            label="queue.content"
            resourceBundle="org.wso2.carbon.andes.ui.i18n.Resources"
            topPage="false"
            request="<%=request%>"/>

    <div id="middle">
        <h2><fmt:message key="dlc.queue.content"/> <%=nameOfQueue%>
        </h2>

        <div id="workArea">
            <table id="queueAddTable" class="styledLeft" style="width:100%">
                <thead>
                <tr>
                    <th colspan="2">Enter Queue Name to Filter</th>
                </tr>
                </thead>
                <tbody>
                <tr>
                    <td class="formRaw leftCol-big">Queue Name:</td>
                    <td><input type="text" id="queueName" value="<%= previouslySearchedQueueName %>">
                        <input id="searchButton" class="button" type="button"
                               onclick="return filterByQueue();" value="Filter">

                        <form id="dummyForm" action="dlc_messages_list.jsp" method="post">
                            <input type="hidden" name="nameOfQueue" id="filterQueueName" value=""/>
                        </form>
                    </td>
                </tr>
                </tbody>
            </table>
            <p>&nbsp;</p>

            <div id="iconArea">
                <table align="right">
                    <thead>
                    <tr align="right">
                        <th align="right">
                            <a style="background-image: url(../admin/images/delete.gif);"
                               class="icon-link"
                               onclick="doDeleteDLC('<%=nameOfQueue%>')">Delete</a>
                        </th>
                        <th align="right">
                            <a style="background-image: url(../admin/images/move.gif);"
                               class="icon-link"
                               onclick="deRestoreMessages('<%=nameOfQueue%>')">Restore</a>
                        </th>
                        <th align="right">
                            <a style="background-image: url(images/move.gif);"
                               class="icon-link"
                               onclick="doReRouteMessages('<%=nameOfQueue%>')">ReRoute</a>
                        </th>
                    </tr>
                    </thead>
                </table>
            </div>
            <input type="hidden" name="pageNumber" value="<%=pageNumber%>"/>
            <carbon:paginator pageNumber="<%=pageNumber%>" numberOfPages="<%=numberOfPages%>"
                              page="dlc_messages_list.jsp" pageNumberParameterName="pageNumber"
                              resourceBundle="org.wso2.carbon.andes.ui.i18n.Resources"
                              prevKey="prev" nextKey="next" parameters="<%=concatenatedParameters%>"
                              showPageNumbers="false"/>

            <table class="styledLeft" style="width:100%">
                <thead>
                <tr>
                    <th><input type="checkbox" name="selectAllCheckBox"
                               onClick="toggleCheck(this)"/></th>
                    <th><fmt:message key="message.contenttype"/></th>
                    <th><fmt:message key="message.messageId"/></th>
                    <th><fmt:message key="message.correlationId"/></th>
                    <th><fmt:message key="message.type"/></th>
                    <th><fmt:message key="message.redelivered"/></th>
                    <th><fmt:message key="message.deliverymode"/></th>
                    <th><fmt:message key="message.priority"/></th>
                    <th><fmt:message key="message.timestamp"/></th>
                    <th><fmt:message key="message.destination"/></th>
                    <th><fmt:message key="message.properties"/></th>
                    <th><fmt:message key="message.summary"/></th>
                </tr>
                </thead>
                <tbody>
                <%
                    if (filteredMsgArray != null) {
                        for (Message queueMessage : filteredMsgArray) {
                            if (queueMessage != null) {
                                String msgProperties = queueMessage.getMsgProperties();
                                String contentType = queueMessage.getContentType();
                                String[] messageContent = queueMessage.getMessageContent();
                                String dlcMsgDestination = queueMessage.getDlcMsgDestination();

                %>
                <tr>
                    <td><input type="checkbox" name="checkbox" onClick="checkSelectAll(this)"
                               value="<%= queueMessage.getAndesMsgMetadataId() %>"/></td>
                    <td><img src="images/<%= contentType.toLowerCase()%>.png"
                             alt=""/>&nbsp;&nbsp;<%= contentType%>
                    </td>
                    <td><%= queueMessage.getJMSMessageId()%>
                    </td>
                    <td><%= queueMessage.getJMSCorrelationId()%>
                    </td>
                    <td><%= queueMessage.getJMSType()%>
                    </td>
                    <td><%= queueMessage.getJMSReDelivered()%>
                    </td>
                    <td><%= queueMessage.getJMSDeliveredMode()%>
                    </td>
                    <td><%= queueMessage.getJMSPriority()%>
                    </td>
                    <td><%= queueMessage.getJMSTimeStamp()%>
                    </td>
                    <td><%= dlcMsgDestination %>
                    </td>
                    <td><%= msgProperties%>
                    </td>
                    <td><%= messageContent[0]%>
                        <% if (messageContent[1].length() > messageContent[0].length()) { %>
                        <a href="message_content.jsp?message=<%=messageContent[1]%>">&nbsp;&nbsp;&nbsp;more...</a>
                        <% } %>
                    </td>
                </tr>

                <%
                            }
                        }
                    }
                %>
                </tbody>
            </table>

        </div>
    </div>
    <div>
        <form id="deleteForm" name="input" action="dlc_messages_list.jsp" method="get"><input
                type="HIDDEN"
                name="deleteMsg"
                value=""/>
            <input type="HIDDEN"
                   name="nameOfQueue"
                   value=""/>
            <input type="HIDDEN"
                   name="msgList"
                   value=""/></form>
        <form id="restoreForm" name="input" action="dlc_messages_list.jsp" method="get"><input
                type="HIDDEN"
                name="restoreMsgs"
                value=""/>
            <input type="HIDDEN"
                   name="nameOfQueue"
                   value=""/>
            <input type="HIDDEN"
                   name="msgList"
                   value=""/></form>
    </div>
</fmt:bundle>
