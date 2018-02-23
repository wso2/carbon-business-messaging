<%@ page import="org.wso2.carbon.andes.ui.UIUtils" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="carbon" uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.andes.stub.AndesAdminServiceStub" %>
<%@ page import="org.wso2.carbon.andes.stub.AndesAdminServiceBrokerManagerAdminException" %>
<%@ page import="org.wso2.carbon.andes.stub.admin.types.Message" %>
<%@ page import="org.wso2.andes.configuration.enums.AndesConfiguration" %>
<%@ page import="org.wso2.andes.configuration.AndesConfigurationManager" %>
<%@ page import="javax.xml.bind.SchemaOutputResolver" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.util.HashMap" %>
<%@ page import="org.apache.commons.lang.StringUtils" %>
<%@ page import="org.apache.commons.lang.StringEscapeUtils" %>
<%@ page import="org.owasp.encoder.Encode" %>
<%@ taglib uri="http://www.owasp.org/index.php/Category:OWASP_CSRFGuard_Project/Owasp.CsrfGuard.tld" prefix="csrf" %>

<script type="text/javascript" src="js/treecontrol.js"></script>
<fmt:bundle basename="org.wso2.carbon.andes.ui.i18n.Resources">

    <carbon:jsi18n
            resourceBundle="org.wso2.carbon.andes.ui.i18n.Resources"
            request="<%=request%>"/>

    <script type="text/javascript" src="../admin/js/breadcrumbs.js"></script>
    <script type="text/javascript" src="../admin/js/cookies.js"></script>
    <script type="text/javascript" src="../admin/js/main.js"></script>
    <link rel="stylesheet" href="styles/dsxmleditor.css"/>
    <script type="text/javascript">
        $(document).ready(function () {
            removeFirstAndLastPaginations();
        })
    </script>
    <%
        AndesAdminServiceStub stub = UIUtils.getAndesAdminServiceStub(config, session, request);
        String nameOfQueue = request.getParameter("nameOfQueue");
        String concatenatedParameters = "nameOfQueue=" + nameOfQueue;
        String pageNumberAsStr = request.getParameter("pageNumber");
        String headerPrefix;
        // ':' is a forbidden letter for a queue name
        // It is used for create a durable subscription using the tenant name and the topic
        // Therefore, the presence of ':' in a queue name is and indication that it is a durable subscription
        if (nameOfQueue.contains(":")){
            headerPrefix = "durable.subscription.content";
        }
        else{
            headerPrefix = "queue.content";
        }

        int msgCountPerPage = AndesConfigurationManager.readValue(
                AndesConfiguration.MANAGEMENT_CONSOLE_MESSAGE_BROWSE_PAGE_SIZE);
        Map<Integer, Long> pageNumberToMessageIdMap = null;
        if(request.getSession().getAttribute("pageNumberToMessageIdMap") != null) {
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
        if (pageNumberAsStr != null) {
            pageNumber = Integer.parseInt(pageNumberAsStr);
        }
        try {
            totalMsgsInQueue = stub.getTotalMessagesInQueue(nameOfQueue);
            numberOfPages = (int) Math.ceil(((float) totalMsgsInQueue) / msgCountPerPage);
            
            if(pageNumberToMessageIdMap.size() > 0) {
                if(pageNumberToMessageIdMap.get(pageNumber) != null) {
                    nextMessageIdToRead = pageNumberToMessageIdMap.get(pageNumber);
                }
            }
            filteredMsgArray = stub.browseQueue(nameOfQueue, nextMessageIdToRead, msgCountPerPage);
            if(filteredMsgArray != null && filteredMsgArray.length > 0) {
                startMessageIdOfPage = filteredMsgArray[0].getAndesMsgMetadataId();
                pageNumberToMessageIdMap.put(pageNumber, startMessageIdOfPage);
                nextMessageIdToRead = filteredMsgArray[filteredMsgArray.length - 1].getAndesMsgMetadataId() + 1;
                pageNumberToMessageIdMap.put((pageNumber + 1), nextMessageIdToRead);
                request.getSession().setAttribute("pageNumberToMessageIdMap", pageNumberToMessageIdMap);
            }
        } catch (AndesAdminServiceBrokerManagerAdminException e) {
    %>
            <script type="text/javascript">CARBON.showErrorDialog('<%=e.getFaultMessage().getBrokerManagerAdminException().getErrorMessage()%>' , function
                    () {
                location.href = 'queue_details.jsp';
            });</script>
            <%
        }
    %>

    <carbon:breadcrumb
            label="<%=headerPrefix%>"
            resourceBundle="org.wso2.carbon.andes.ui.i18n.Resources"
            topPage="false"
            request="<%=request%>"/>

    <div id="middle">
        <h2><fmt:message key="<%=headerPrefix%>"/>  <%=nameOfQueue%></h2>

        <div id="workArea">
            <input type="hidden" name="pageNumber" value="<%=pageNumber%>"/>
            <carbon:paginator pageNumber="<%=pageNumber%>" numberOfPages="<%=numberOfPages%>"
                              page="queue_messages_list.jsp" pageNumberParameterName="pageNumber"
                              resourceBundle="org.wso2.carbon.andes.ui.i18n.Resources"
                              prevKey="prev" nextKey="next" parameters="<%=concatenatedParameters%>"
                              showPageNumbers="false"/>

            <table class="styledLeft" style="width:100%">
                <thead>
                <tr>
                    <th><fmt:message key="message.contenttype"/></th>
                    <th><fmt:message key="message.jmsMessageId"/></th>
                    <th><fmt:message key="message.internalMessageId"/></th>
                    <th><fmt:message key="message.redelivered"/></th>
                    <th><fmt:message key="message.timestamp"/></th>
                    <th><fmt:message key="message.properties"/></th>
                    <th><fmt:message key="message.summary"/></th>
                </tr>
                </thead>
                <tbody>
                <%
                    if(filteredMsgArray != null) {
                        int count =1;
                        for (Message queueMessage : filteredMsgArray) {
                            if (queueMessage != null) {
                            String msgProperties = queueMessage.getMsgProperties();
                            String contentType = queueMessage.getContentType();
                            String[] messageContent = queueMessage.getMessageContent();
                            long contentDisplayID = queueMessage.getJMSTimeStamp()+count;
                            count++;
                %>
                <tr>
                    <td><% if(StringUtils.isNotBlank(contentType)) { %>
                        <img src="images/<%= contentType.toLowerCase()%>.png"
                             alt=""/>&nbsp;&nbsp;<%= contentType%>
                        <% } else { %>
                            null
                        <% } %>
                    </td>
                    <td><%= queueMessage.getJMSMessageId()%>
                    </td>
                    <td><%= queueMessage.getAndesMsgMetadataId()%>
                    </td>
                    <td><%= queueMessage.getJMSReDelivered()%>
                    </td>
                    <td><%= queueMessage.getJMSTimeStamp()%>
                    </td>
                    <td><%= Encode.forHtml(msgProperties)%>
                    </td>
                    <td>
                        <%=StringEscapeUtils.escapeHtml(messageContent[0])%>
                        <!-- This is converted to a POST to avoid message length eating up the URI request length. -->
                        <form name="msgViewForm<%=contentDisplayID%>" method="POST" action="message_content.jsp">
                            <input type="hidden" name="<csrf:tokenname/>" value="<csrf:tokenvalue/>" />
                            <input type="hidden" name="message" value="<%=StringEscapeUtils.escapeHtml(messageContent[1])%>" />
                            <a href="javascript:document.msgViewForm<%=contentDisplayID%>.submit()">&nbsp;&nbsp;&nbsp;more..</a>
                        </form>
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
</fmt:bundle>
