<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="carbon" uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" %>
<%@ page import="org.wso2.carbon.andes.stub.AndesAdminServiceStub" %>
<%@ page import="org.wso2.carbon.andes.ui.UIUtils" %>
<%@ page import="org.wso2.carbon.andes.stub.AndesAdminServiceBrokerManagerAdminException" %>
<fmt:bundle basename="org.wso2.carbon.andes.ui.i18n.Resources">

    <carbon:jsi18n
            resourceBundle="org.wso2.carbon.andes.ui.i18n.Resources"
            request="<%=request%>"/>

    <!--Local js includes-->
    <script type="text/javascript" src="js/treecontrol.js"></script>
    <script type="text/javascript" src="../admin/js/breadcrumbs.js"></script>
    <script type="text/javascript" src="../admin/js/cookies.js"></script>
    <script type="text/javascript" src="../admin/js/main.js"></script>
    <link rel="stylesheet" href="styles/dsxmleditor.css"/>

    <%
        AndesAdminServiceStub stub = UIUtils.getAndesAdminServiceStub(config, session, request);
        String nameOfQueue = request.getParameter("nameOfQueue");
    %>

    <script>
        function sendMessage() {
            var theform = document.getElementById('send_message_form');
            theform.submit();
        }
    </script>

    <carbon:breadcrumb
            label="send.message"
            resourceBundle="org.wso2.carbon.andes.ui.i18n.Resources"
            topPage="false"
            request="<%=request%>"/>

    <%
        String numberOfMessagesToSend = request.getParameter("num_of_msgs");
        int msg_count = 0;
        long time_to_live = 0;
        if(numberOfMessagesToSend != null) {
            boolean inputValidated = true;
            if(request.getParameter("num_of_msgs").equals("")) {
                inputValidated = false;
                %>
                <script type="text/javascript">CARBON.showErrorDialog('Number of messages cannot be empty', function
                        () {
                    location.href = 'queue_message_sender.jsp?nameOfQueue=<%=nameOfQueue%>';
                });</script>
                <%
            }
            if (inputValidated && !request.getParameter("num_of_msgs").equals("")) {
                try {
                    msg_count = Integer.parseInt(request.getParameter("num_of_msgs"));
                    if(msg_count <= 0) {
                        inputValidated = false;
                        %>
                            <script type="text/javascript">CARBON.showErrorDialog('Please enter a valid number of messages to send', function
                                    () {
                                location.href = 'queue_message_sender.jsp?nameOfQueue=<%=nameOfQueue%>';
                            });</script>
                        <%
                    }
                } catch (NumberFormatException e) {
                        inputValidated = false;
                        %>
                            <script type="text/javascript">CARBON.showErrorDialog('Number of messages input is not a number', function
                                    () {
                                location.href = 'queue_message_sender.jsp?nameOfQueue=<%=nameOfQueue%>';
                            });</script>
                        <%
                }
            }

            if(inputValidated) {
                try {
                    // set correlation id
                    String cor_id = null;
                    if (!request.getParameter("cor_id").equals("")) {
                        cor_id = request.getParameter("cor_id");
                    }
                    // set jms type
                    String jms_type = null;
                    if (!request.getParameter("jms_type").equals("")) {
                        jms_type = request.getParameter("jms_type");
                    }
                    // set message text
                    String message_txt;
                    if (!request.getParameter("msg_text").equalsIgnoreCase("")) {
                        message_txt = request.getParameter("msg_text");
                    } else {
                        message_txt = "Type message here..";
                    }
                    int delivery_mode = 2;
                    int priority = 4;

                    boolean success = stub.sendMessage(nameOfQueue, jms_type, cor_id, msg_count, message_txt, delivery_mode, priority, time_to_live);
                    if(success) {
                    %>
                        <script type="text/javascript">CARBON.showInfoDialog('Successfully sent <%=numberOfMessagesToSend%> messages to Queue <%=nameOfQueue%>' , function
                                () {
                            location.href = 'queue_details.jsp';
                        });</script>
                    <%
                }
                } catch (AndesAdminServiceBrokerManagerAdminException e) {
                    %>
                    <script type="text/javascript">CARBON.showErrorDialog('<%=e.getFaultMessage().getBrokerManagerAdminException().getErrorMessage()%>' , function
                            () {
                        location.href = 'queue_details.jsp';
                    });</script>
                    <%
                }
            }
        }
    %>

    <div id="middle">
        <h2><fmt:message key="send.message"/></h2>
        <div id="workArea">

            <form id="send_message_form" name="send_message_form" action="queue_message_sender.jsp?nameOfQueue=<%=nameOfQueue%>" method="post">

                <table class="styledLeft" style="width:100%">
                <thead>
                <tr>
                    <th colspan="4"><fmt:message key="message.header.fields"/></th>
                </tr>
                </thead>
                <tbody>
                <tr>
                    <td>Correlation ID: </td><td><input type="text" id="cor_id" name="cor_id"></td>
                    <td>JMS Type: </td><td><input type="text" id="jms_type" name="jms_type"></td>
                </tr>
<%--                <tr>
                    <td>Persistent? </td><td><input type="checkbox"  id="delivery_mode" name="delivery_mode" value="true"></td>
                    <td>Priority:(0-9) </td><td><input type="text" id="priority" name="priority"></td>
                </tr>--%>
                <tr>
                    <td>Number of Messages:<span class="required">*</span></td><td><input type="text" id="num_of_msgs" name="num_of_msgs"></td>
                    <td></td><td></td>
                </tr>
                </tbody>
                </table>

                <br/>

                <table class="styledLeft" style="width:100%">
                <thead>
                <tr>
                    <th><fmt:message key="message.body"/></th>
                </tr>
                </thead>
                <tbody>
                <tr>
                    <td><textarea style="resize:none" name="msg_text" rows="25" cols="80" id="msg_text">Type message here..</textarea></td>
                </tr>
                <tr><td><input type="submit" value="<fmt:message key="send.message"/>" onclick="sendMessage()"></td>
                </tr>
                </tbody>
                </table>
            </form>
        </div>

    </div>

</fmt:bundle>


