<%@ page import="org.wso2.carbon.andes.ui.Constants" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" prefix="carbon" %>
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

<link href="css/tree-styles.css" media="all" rel="stylesheet"/>
<link href="css/dsxmleditor.css" media="all" rel="stylesheet"/>

<fmt:bundle basename="org.wso2.carbon.andes.ui.i18n.Resources">
    <carbon:breadcrumb
            label="queue.add"
            resourceBundle="org.wso2.carbon.andes.ui.i18n.Resources"
            topPage="true"
            request="<%=request%>"/>

    <script type="text/javascript" src="../carbon/admin/js/breadcrumbs.js"></script>
    <script type="text/javascript" src="../carbon/admin/js/cookies.js"></script>
    <script type="text/javascript" src="../carbon/admin/js/main.js"></script>

    <%
        String message = request.getParameter("message");
        if (message != null) {
    %><h3><%=message%>
</h3><%
    }
%>
    <jsp:include page="../resources/resources-i18n-ajaxprocessor.jsp"/>

    <div id="middle">
        <h2><fmt:message key="add.queue"/></h2>

        <div id="workArea">
            <table class="styledLeft" style="width:100%">
                <thead>
                <tr>
                    <th colspan="2">Enter Queue Name</th>
                </tr>
                </thead>
                <tbody>
                <tr>
                    <td class="formRaw"><fmt:message key="queue"/><span
                            class="required">*</span></td>
                    <td><input type="text" id="queue"></td>
                </tr>
                <tr>
                    <td colspan="2"><input type="button" class="button"
                               value="<fmt:message key="add.queue"/>"
                               onclick="addQueue('<%=Constants.MB_QUEUE_CREATED_FROM_AMQP%>')"/>
                    </td>
                </tr>
                </tbody>
            </table>
        </div>
    </div>
</fmt:bundle>