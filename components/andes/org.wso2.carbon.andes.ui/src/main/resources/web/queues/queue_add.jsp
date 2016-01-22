<%@ page import="org.wso2.carbon.andes.stub.AndesAdminServiceBrokerManagerAdminException" %>
<%@ page import="org.wso2.carbon.andes.stub.AndesAdminServiceStub" %>
<%@ page import="org.wso2.carbon.andes.stub.admin.types.QueueRolePermission" %>
<%@ page import="org.wso2.carbon.andes.ui.Constants" %>
<%@ page import="org.wso2.carbon.andes.ui.UIUtils" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.regex.Pattern" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" prefix="carbon" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<!--Local js includes-->
<script type="text/javascript" src="js/treecontrol.js"></script>

<link href="styles/tree-styles.css" media="all" rel="stylesheet"/>
<jsp:include page="../resources/resources-i18n-ajaxprocessor.jsp"/>

<fmt:bundle basename="org.wso2.carbon.andes.ui.i18n.Resources">
<carbon:breadcrumb
        label="queue.add"
        resourceBundle="org.wso2.carbon.andes.ui.i18n.Resources"
        topPage="true"
        request="<%=request%>"/>

<script type="text/javascript">

    jQuery(document).ready(function () {
        jQuery('#queue').keyup(function () {
            changeAllLinks();
        });

        jQuery('#search').keyup(function () {
            changeAllLinks();
        });

        jQuery('.checkboxChanged').click(function () {
            var $element = jQuery(this);
            var role = $element.attr('role');
            // prop is used because when unchecked, attr gives undefined
            var checked = $element.prop('checked');
            var action = $element.attr('permission');

            jQuery.ajax({
                url: "update_queue_role_permissions_to_session_ajaxprocessor.jsp",
                data: {role: role, checked: checked, action: action},
                success: function (data) {
                    //do nothing
                }
            });
        });
    });

    function changeAllLinks() {
        jQuery('#permissionTable').find('tr td a').each(
                function () {
                    var href = jQuery(this).attr('href');
                    var queueName;
                    var searchTerm;

                    var parameters = href.match(/queueName=(.*?)\&searchTerm=(.*?)$/);
                    if (parameters) {
                        queueName = parameters[1];
                        searchTerm = parameters[2];
                    }
                    href = href.replace("&queueName=" + queueName, "&queueName=" +
                            jQuery('#queue').val());
                    href = href.replace("&searchTerm=" + searchTerm, "&searchTerm=" +
                            jQuery('#search').val());
                    jQuery(this).attr('href', href);
                }
        );
    }

    function searchRole() {
        var searchTerm = jQuery('#search').val();
        var queueName = jQuery('#queue').val();
        var splitted = window.location.href.split("queue_add.jsp?");
        window.location.assign(splitted[0] +
                "queue_add.jsp?region=region1&item=queue_add&queueName=" + queueName +
                "&searchTerm=" + searchTerm);
    }

</script>

<%
    Pattern pattern = Pattern.compile("region=region1&item=queues_add$");
    if (pattern.matcher(request.getAttribute("javax.servlet.forward.query_string").toString()).matches()) {
        session.removeAttribute("queueRolePermissions");
    }

    // Get queue name and search term from the request. If they are not found in the request,
    // use the default ones.
    String message = request.getParameter("message");
    String queueNameFromRequest = request.getParameter("queueName");
    String queueName = queueNameFromRequest == null ? "" : queueNameFromRequest;
    String searchTermFromRequest = request.getParameter("searchTerm");
    String searchTerm = searchTermFromRequest == null ? "*" : searchTermFromRequest;
    String concatenatedParams = "region=region1&item=queue_add&queueName=" + queueName +
            "&searchTerm=" + searchTerm;
    if (message != null) {
%><h3><%=message%>
</h3><%
    }

    // Get the permissions given to user roles which are stored in the session
    ArrayList<QueueRolePermission> queueRolePermissions = (ArrayList<QueueRolePermission>)
            session.getAttribute("queueRolePermissions");

    if (!(queueRolePermissions != null && queueRolePermissions.size() > 0)) {

        // If the permissions are not found in the Session, store them to the session
        queueRolePermissions = new ArrayList<QueueRolePermission>();
        session.setAttribute("queueRolePermissions", queueRolePermissions);

        try {
            AndesAdminServiceStub stub = UIUtils.getAndesAdminServiceStub(config, session, request);
            String[] userRoles = stub.getUserRoles();

            QueueRolePermission queueRolePermission;
            for (String role : userRoles) {
                queueRolePermission = new QueueRolePermission();
                queueRolePermission.setRoleName(role);
                queueRolePermission.setAllowedToConsume(false);
                queueRolePermission.setAllowedToPublish(false);
                queueRolePermissions.add(queueRolePermission);
            }

        } catch (AndesAdminServiceBrokerManagerAdminException e) {
%>
<script type="text/javascript">
    CARBON.showErrorDialog('<%=
    e.getFaultMessage().getBrokerManagerAdminException().getErrorMessage()%>');
</script>
<%
            return;
        }
    }

    //Select the roles according to the submitted search term
    ArrayList<QueueRolePermission> selectedQueueRolePermissions = new ArrayList<QueueRolePermission>();

    if (searchTerm.equals("*") || searchTerm.equals("")) {
        selectedQueueRolePermissions = queueRolePermissions;
    } else {
        for (QueueRolePermission permission : queueRolePermissions) {
            if (permission.getRoleName().toLowerCase().contains(searchTerm.toLowerCase())) {
                selectedQueueRolePermissions.add(permission);
            }
        }
    }

    //Obtain values needed to handle pagination when displaying role permissions.
    int rolesCountPerPage = 20;
    int pageNumber = 0;
    int numberOfPages = 1;
    long totalRoleCount;
    ArrayList<QueueRolePermission> filteredRoleList = new ArrayList<QueueRolePermission>();

    String pageNumberAsStr = request.getParameter("pageNumber");
    if (pageNumberAsStr != null) {
        pageNumber = Integer.parseInt(pageNumberAsStr);
    }

    if (selectedQueueRolePermissions.size() > 0) {
        totalRoleCount = selectedQueueRolePermissions.size();
        numberOfPages = (int) Math.ceil(((float) totalRoleCount) / rolesCountPerPage);
        filteredRoleList = UIUtils.getFilteredRoleList(selectedQueueRolePermissions,
                pageNumber * rolesCountPerPage, rolesCountPerPage);
    }

%>
<div id="middle">
    <div id="workArea">
        <h2><fmt:message key="add.queue"/></h2>
        <table id="queueAddTable" class="styledLeft" style="width:100%">
            <thead>
            <tr>
                <th colspan="2">Enter Queue Name</th>
            </tr>
            </thead>
            <tbody>
            <tr>
                <td class="formRaw leftCol-big"><fmt:message key="queue"/><span
                        class="required">*</span></td>
                <td><input type="text" id="queue" value="<%=queueName%>" maxlength="238" /></td>
            </tr>
            </tbody>
        </table>

        <p>&nbsp;</p>

        <table id="permissionTable" class="styledLeft" style="width:100%">
            <thead>
            <tr>
                <th colspan="2"><fmt:message key="permissions"/></th>
            </tr>
            </thead>
            <tbody>
            <tr>
                <td class="leftCol-big"><fmt:message key="search.label"/></td>
                <td>
                    <input type="text" id="search" value="<%=searchTerm%>"/>
                    <input id="searchButton" class="button" type="button" onclick="searchRole()"
                           value="Search"/>
                </td>
            </tr>
            <tr>
                <td class="formRow" colspan="2">
                    <input type="hidden" name="pageNumber" value="<%=pageNumber%>"/>

                    <div class="paginatorWrapper">
                        <carbon:paginator pageNumber="<%=pageNumber%>"
                                          numberOfPages="<%=numberOfPages%>"
                                          page="queue_add.jsp" pageNumberParameterName="pageNumber"
                                          resourceBundle="org.wso2.carbon.andes.ui.i18n.Resources"
                                          prevKey="prev" nextKey="next"
                                          parameters="<%=concatenatedParams%>"/>
                    </div>
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
                            if (filteredRoleList.size() <= 0) {
                        %>
                        <script type="text/javascript">
                            CARBON.showInfoDialog('No matching roles found');
                        </script>
                        <%
                            }
                            for (QueueRolePermission rolePermission : filteredRoleList) {
                        %>
                        <tr>
                            <td><%=rolePermission.getRoleName()%>
                            </td>
                            <td><input type="checkbox"
                                       class="checkboxChanged"
                                       role="<%=rolePermission.getRoleName()%>"
                                       permission="consume"
                                       id="<%=rolePermission.getRoleName()%>^consume"
                                       value="consume" <% if (rolePermission.getAllowedToConsume()) { %>
                                       checked <% } %></td>
                            <td><input type="checkbox"
                                       class="checkboxChanged"
                                       role="<%=rolePermission.getRoleName()%>"
                                       permission="publish"
                                       id="<%=rolePermission.getRoleName()%>^publish"
                                       value="publish"  <% if (rolePermission.getAllowedToPublish()) { %>
                                       checked <% } %></td>
                        </tr>
                        <%
                            }
                        %>

                        </tbody>
                    </table>
                    <div class="paginatorWrapper">
                        <carbon:paginator pageNumber="<%=pageNumber%>"
                                          numberOfPages="<%=numberOfPages%>"
                                          page="queue_add.jsp" pageNumberParameterName="pageNumber"
                                          resourceBundle="org.wso2.carbon.andes.ui.i18n.Resources"
                                          prevKey="prev" nextKey="next"
                                          parameters="<%=concatenatedParams%>"/>
                    </div>
                </td>
            </tr>
            <tr>
                <td colspan="2" class="buttonRow"><input type="button" id="addQueueButton"
                                                         class="button"
                                                         onclick="addQueue('<%=Constants.MB_QUEUE_CREATED_FROM_AMQP%>')"
                                                         value="<fmt:message key="add.queue"/>"/>
                </td>
            </tr>
            </tbody>
        </table>
    </div>
</div>
</fmt:bundle>