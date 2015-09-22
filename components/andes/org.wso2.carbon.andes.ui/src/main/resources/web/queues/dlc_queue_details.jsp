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
        //clear data
        request.getSession().removeAttribute("pageNumberToMessageIdMap");

        AndesAdminServiceStub stub = UIUtils.getAndesAdminServiceStub(config, session, request);
        Queue dlcQueue = null;
        try {
        	dlcQueue = stub.getDLCQueue();

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

    <carbon:breadcrumb
            label="queues.list"
            resourceBundle="org.wso2.carbon.andes.ui.i18n.Resources"
            topPage="false"
            request="<%=request%>"/>
       
    <div id="middle">
        <h2><fmt:message key="dlc"/></h2>

        <div id="workArea">

            <%
                if (dlcQueue == null) {
            %>
            No queues are created.
            <%
            } else {

            %>
            <table class="styledLeft" style="width:100%">
                <thead>
                <tr>
                    <th><fmt:message key="queue.name"/></th>
                    <th><fmt:message key="queue.messageCount"/></th>
                    <th><fmt:message key="queue.view"/></th>
                </tr>
                </thead>
                <tbody>
                <%
                    if (dlcQueue != null) {
                        String nameOfQueue = dlcQueue.getQueueName();
                %>
                <tr>
                    <td>
                        <%=nameOfQueue%>
                    </td>
                    <td><%=dlcQueue.getMessageCount()%>
                    </td>
                    <td>
                    <a href="javascript:void(null);" onclick="document.getElementById('dummyForm').submit();">Browse</a>
 					<form id="dummyForm" action="dlc_messages_list.jsp" method="post">
						<input type="hidden" name="nameOfQueue" value="<%=nameOfQueue%>" />
					</form>
                 </tr>
                <%
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
