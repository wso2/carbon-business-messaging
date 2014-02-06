<%@ page import="org.wso2.carbon.andes.ui.UIUtils" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="carbon" uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" %>
<%@ page import="org.wso2.carbon.andes.ui.client.QueueBrowserClient" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="javax.jms.JMSException" %>
<%@ page import="javax.jms.Message" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.Collections" %>
<%@ page import="java.util.Enumeration" %>
<%@ page import="org.wso2.carbon.andes.stub.AndesAdminServiceStub" %>
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
        String nameOfQueue = request.getParameter("nameOfQueue");
        QueueBrowserClient queueBrowserClient = new QueueBrowserClient(nameOfQueue, stub.getCurrentUser(),stub.getAccessKey());
        Enumeration queueEnu = queueBrowserClient.browseQueue();
        ArrayList msgArrayList = Collections.list(queueEnu);
        Object[] filteredMsgArray = null;
        int msgCountPerPage = 100;
        int pageNumber = 0;
        int numberOfPages = 1;
        String concatenatedParameters = "nameOfQueue=" + nameOfQueue;
        long totalMsgsInQueue;
        String pageNumberAsStr = request.getParameter("pageNumber");
        if (pageNumberAsStr != null) {
            pageNumber = Integer.parseInt(pageNumberAsStr);
        }

        if (msgArrayList != null) {
            totalMsgsInQueue = msgArrayList.size();
            numberOfPages = (int) Math.ceil(((float) totalMsgsInQueue) / msgCountPerPage);
            filteredMsgArray = UIUtils.getFilteredMsgsList(msgArrayList, pageNumber * msgCountPerPage, msgCountPerPage);
        }

    %>
    <carbon:breadcrumb
            label="queue.content"
            resourceBundle="org.wso2.carbon.andes.ui.i18n.Resources"
            topPage="false"
            request="<%=request%>"/>

    <div id="middle">
        <h2><fmt:message key="queue.content"/>  <%=nameOfQueue%></h2>

        <div id="workArea">
            <input type="hidden" name="pageNumber" value="<%=pageNumber%>"/>
            <carbon:paginator pageNumber="<%=pageNumber%>" numberOfPages="<%=numberOfPages%>"
                              page="queue_messages_list.jsp" pageNumberParameterName="pageNumber"
                              resourceBundle="org.wso2.carbon.andes.ui.i18n.Resources"
                              prevKey="prev" nextKey="next" parameters="<%=concatenatedParameters%>"/>

            <table class="styledLeft" style="width:100%">
                <thead>
                <tr>
                    <th><fmt:message key="message.contenttype"/></th>
                    <th><fmt:message key="message.messageId"/></th>
                    <th><fmt:message key="message.correlationId"/></th>
                    <th><fmt:message key="message.type"/></th>
                    <th><fmt:message key="message.redelivered"/></th>
                    <th><fmt:message key="message.deliverymode"/></th>
                    <th><fmt:message key="message.priority"/></th>
                    <th><fmt:message key="message.timestamp"/></th>
                    <th><fmt:message key="message.expiration"/></th>
                    <th><fmt:message key="message.properties"/></th>
                    <th><fmt:message key="message.summary"/></th>
                </tr>
                </thead>
                <tbody>
                <%
                    try {
                        for (Object message : filteredMsgArray) {
                            Message queueMessage = (Message) message;
                            if (queueMessage != null) {
                                String msgProperties = queueBrowserClient.getMsgProperties(queueMessage);
                                String contentType = queueBrowserClient.getMsgContentType(queueMessage);
                                String[] messageContent = queueBrowserClient.getMessageContentAsString(queueMessage);
                %>
                <tr>
                    <td><img src="images/<%= contentType.toLowerCase()%>.png"
                             alt=""/>&nbsp;&nbsp;<%= contentType%>
                    </td>
                    <td><%= queueMessage.getJMSMessageID()%>
                    </td>
                    <td><%= queueMessage.getJMSCorrelationID()%>
                    </td>
                    <td><%= queueMessage.getJMSType()%>
                    </td>
                    <td><%= queueMessage.getJMSRedelivered()%>
                    </td>
                    <td><%= queueMessage.getJMSDeliveryMode()%>
                    </td>
                    <td><%= queueMessage.getJMSPriority()%>
                    </td>
                    <td><%= queueMessage.getJMSTimestamp()%>
                    </td>
                    <td><%= queueMessage.getJMSExpiration()%>
                    </td>
                    <td><%= msgProperties%>
                    </td>
                    <td><%= messageContent[0]%><a href="message_content.jsp?message=<%=messageContent[1]%>">&nbsp;&nbsp;&nbsp;more...</a>
                    </td>
                </tr>

                <% }
                }

                } catch (JMSException e) {
                    CarbonUIMessage.sendCarbonUIMessage(e.getMessage(), CarbonUIMessage.ERROR, request, e);
                    e.printStackTrace();
                }
                %>
                </tbody>
            </table>

            <%
                try {
                    queueBrowserClient.closeBrowser();
                } catch (JMSException e) {
                    CarbonUIMessage.sendCarbonUIMessage(e.getMessage(), CarbonUIMessage.ERROR, request, e);
                    e.printStackTrace();
                }
            %>
        </div>
    </div>
</fmt:bundle>
