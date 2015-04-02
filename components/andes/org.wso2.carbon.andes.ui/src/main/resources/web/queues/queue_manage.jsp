<%@ page import="org.wso2.carbon.andes.stub.AndesAdminServiceStub" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" prefix="carbon" %>
<%@ page import="org.wso2.carbon.andes.stub.admin.types.QueueRolePermission" %>
<%@ page import="org.wso2.carbon.andes.ui.UIUtils" %>
<%@ page import="org.wso2.carbon.andes.stub.AndesAdminServiceBrokerManagerAdminException" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<script type="text/javascript" src="../ajax/js/prototype.js"></script>
<script type="text/javascript" src="../resources/js/resource_util.js"></script>
<!--Yahoo includes for dom event handling-->
<script src="../yui/build/yahoo-dom-event/yahoo-dom-event.js" type="text/javascript"></script>

<!--Yahoo includes for animations-->
<script src="../yui/build/animation/animation-min.js" type="text/javascript"></script>

<!--Yahoo includes for menus-->
<link rel="stylesheet" type="text/css" href="../yui/build/menu/assets/skins/sam/menu.css"/>
<script type="text/javascript" src="../yui/build/container/container_core-min.js"></script>
<script type="text/javascript" src="../yui/build/menu/menu-min.js"></script>

<!--EditArea javascript syntax hylighter -->
<script language="javascript" type="text/javascript" src="../editarea/edit_area_full.js"></script>

<!--Local js includes-->
<script type="text/javascript" src="js/treecontrol.js"></script>

<link href="styles/tree-styles.css" media="all" rel="stylesheet"/>
<link href="styles/dsxmleditor.css" media="all" rel="stylesheet"/>

<fmt:bundle basename="org.wso2.carbon.andes.ui.i18n.Resources">
    <carbon:breadcrumb
            label="queue.edit"
            resourceBundle="org.wso2.carbon.andes.ui.i18n.Resources"
            topPage="true"
            request="<%=request%>"/>

    <script type="text/javascript" src="../admin/js/breadcrumbs.js"></script>
    <script type="text/javascript" src="../admin/js/cookies.js"></script>
    <script type="text/javascript" src="../admin/js/main.js"></script>

    <%
        String queue;
        boolean exclusiveConsumer;
        String  exclusiveCheck;
        if (session.getAttribute("queue") == null) {
    %>
    <script type="text/javascript">
        location.href = 'queue_details.jsp';</script>
    <%
            return;
        } else {
              queue = (String) session.getAttribute("queue");
              exclusiveConsumer = (Boolean)session.getAttribute("isExclusiveConsumer"+queue);

              if( exclusiveConsumer){
                  exclusiveCheck = "checked";
                  }
              else{
                  exclusiveCheck = " ";
                  }
        }
%>
    <jsp:include page="../resources/resources-i18n-ajaxprocessor.jsp"/>

    <div id="middle">

        <div id="workArea">
            <h2><fmt:message key="edit.queue"/></h2>
            <table class="styledLeft" style="width:100%">
                <thead>
                <tr>
                    <th colspan="2">Queue Details</th>
                </tr>
                </thead>
                <tbody>
                <tr>
                    <td class="formRaw"><fmt:message key="queue"/></td>
                    <td><input type="text" id="queue" readonly="true" value="<%=queue%>"></td>
                </tr>
                <tr>
                  <td class="formRaw leftCol-big">Exclusive Consumer</td>
                  <td><input type="checkbox" id="isExclusiveConsumer" value="checked" onclick="checkBoxStatus(<%=queue%>)" <%=exclusiveCheck%> ></td>
                </tr>

                <tr>
                    <td class="formRow" colspan="2">
                        <h4>Permissions</h4>
                        <table class="styledLeft" style="width:100%" id="permissionsTable">
                            <thead>
                            <tr>
                                <th><fmt:message key="role"/></th>
                                <th><fmt:message key="consume"/></th>
                                <th><fmt:message key="publish"/></th>
                            </tr>
                            </thead>
                            <tbody>
                            <%
                                QueueRolePermission[] queueRolePermissions;
                                AndesAdminServiceStub stub = UIUtils.getAndesAdminServiceStub(config, session, request);

                                try {
                                    queueRolePermissions = stub.getQueueRolePermission(queue);
                                } catch (AndesAdminServiceBrokerManagerAdminException e) {
                            %>
                            <script type="text/javascript">
                                CARBON.showErrorDialog('<%= e.getFaultMessage().getBrokerManagerAdminException().getErrorMessage()%>');

                            </script>
                            <%
                                    return;
                                }

                                if (queueRolePermissions != null) {

                                    for (QueueRolePermission queueRolePermission : queueRolePermissions) {
                            %>
                            <tr>
                                <td><%=queueRolePermission.getRoleName()%>
                                </td>
                                <td><input type="checkbox"
                                           id="<%=queueRolePermission.getRoleName()%>^consume"
                                           value="consume" <% if (queueRolePermission.getAllowedToConsume()) { %>
                                           checked <% } %></td>
                                <td><input type="checkbox"
                                           id="<%=queueRolePermission.getRoleName()%>^publish"
                                           value="publish"  <% if (queueRolePermission.getAllowedToPublish()) { %>
                                           checked <% } %></td>
                            </tr>
                            <%
                                    }
                                }
                            %>

                            </tbody>
                        </table>
                    </td>
                </tr>
                <tr>
                    <td colspan="2" class="buttonRow"><input type="button" class="button"
                               value="<fmt:message key="update.queue"/>"
                               onclick="updatePermissions()"/>
                    </td>
                </tr>
                </tbody>
            </table>
        </div>
    </div>
</fmt:bundle>
