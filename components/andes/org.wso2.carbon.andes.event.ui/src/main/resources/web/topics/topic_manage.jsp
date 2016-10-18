<%@ page import="org.apache.axis2.client.Options" %>
<%@ page import="org.apache.axis2.client.ServiceClient" %>
<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.andes.event.stub.service.AndesEventAdminServiceStub" %>
<%@ page import="org.wso2.carbon.andes.event.stub.admin.Subscription" %>
<%@ page import="org.wso2.carbon.andes.event.stub.core.TopicRolePermission" %>
<%@ page import="org.wso2.carbon.andes.event.ui.UIUtils" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.apache.axis2.databinding.utils.ConverterUtil" %>
<%@ page import="org.wso2.carbon.andes.event.stub.service.AndesEventAdminServiceEventAdminException" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jstl/fmt" %>
<%@ taglib prefix="carbon" uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" %>
<script type="text/javascript" src="js/treecontrol.js"></script>
<!--Yahoo includes for dom event handling-->
<script src="../yui/build/yahoo-dom-event/yahoo-dom-event.js" type="text/javascript"></script>
<script type="text/javascript" src="../yui/build/yahoo/yahoo-min.js"></script>
<script type="text/javascript" src="../yui/build/event/event-min.js"></script>
<script type="text/javascript" src="../yui/build/connection/connection-min.js"></script>

<script src="../yui/build/utilities/utilities.js" type="text/javascript"></script>
<!--Yahoo includes for animations-->
<script src="../yui/build/animation/animation-min.js" type="text/javascript"></script>

<!--Yahoo includes for menus-->
<link rel="stylesheet" type="text/css" href="../yui/build/menu/assets/skins/sam/menu.css"/>

<script type="text/javascript" src="../yui/build/container/container_core-min.js"></script>
<script type="text/javascript" src="../yui/build/menu/menu-min.js"></script>

<link href="css/tree-styles.css" media="all" rel="stylesheet"/>
<link href="css/dsxmleditor.css" media="all" rel="stylesheet"/>

<script type="text/javascript">
    YAHOO.util.Event.onAvailable("JScript", function() {
        editAreaLoader.init({
                                id : "JScript"        // textarea id
                                ,syntax: "js"            // syntax to be uses for highgliting
                                ,start_highlight: true        // to display with highlight mode on start-up
                                ,allow_resize: "both"
                                ,min_height:250
                            });
    });
    function handleFocus(obj, txt) {
        if (obj.value == txt) {
            obj.value = '';
            YAHOO.util.Dom.removeClass(obj, 'defaultText');

        }
    }
    function handleBlur(obj, txt) {
        if (obj.value == '') {
            obj.value = txt;
            YAHOO.util.Dom.addClass(obj, 'defaultText');
        }
    }
    YAHOO.util.Event.onDOMReady(
            function() {
                document.getElementById("hhid").value = "HH";
                document.getElementById("mmid").value = "mm";
                document.getElementById("ssid").value = "ss";
            }
            )


</script>
<link type="text/css" href="../topics/css/topics.css" rel="stylesheet"/>

<%-- YUI Calendar includes--%>
<link rel="stylesheet" type="text/css" href="../yui/build/fonts/fonts-min.css"/>
<link rel="stylesheet" type="text/css" href="../yui/build/calendar/assets/skins/sam/calendar.css"/>
<script type="text/javascript" src="../yui/build/calendar/calendar-min.js"></script>

<style type="text/css">

    #cal1Container {
        display: none;
        position: absolute;
        font-size: 12px;
        z-index: 1
    }

    .defaultText {
        color: #666666;
        font-style: italic;
    }
</style>


<%
    ConfigurationContext configContext = (ConfigurationContext) config.getServletContext()
            .getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
    //Server URL which is defined in the server.xml
    String serverURL = CarbonUIUtil.getServerURL(config.getServletContext(),
                                                 session) + "AndesEventAdminService.AndesEventAdminServiceHttpsSoap12Endpoint";
    AndesEventAdminServiceStub stub = new AndesEventAdminServiceStub(configContext, serverURL);

    String cookie = (String) session.getAttribute(org.wso2.carbon.utils.ServerConstants.ADMIN_SERVICE_COOKIE);

    ServiceClient client = stub._getServiceClient();
    Options option = client.getOptions();
    option.setManageSession(true);
    option.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, cookie);

    String subscriptionId = request.getParameter("subId");
    if (subscriptionId != null) {

    }
    String topic;
    if (session.getAttribute("topic") == null) {
%>
<script type="text/javascript">
    location.href = 'topics.jsp';</script>
<%
        return;
    } else {
        topic = (String) session.getAttribute("topic");
    }


    TopicRolePermission[] topicRolePermissions = new TopicRolePermission[0];
    try {
        topicRolePermissions = stub.getTopicRolePermissions(topic);
    } catch (AndesEventAdminServiceEventAdminException e) {
%>
<script type="text/javascript">
    CARBON.showErrorDialog('<%= e.getFaultMessage().getEventAdminException().getErrorMessage()%>');
</script>
<%
    }
    int pageNumberInt = 0;
    String pageNumberAsStr = request.getParameter("pageNumber");
    if (pageNumberAsStr != null) {
        pageNumberInt = Integer.parseInt(pageNumberAsStr);
    }

    String parameters = "serviceTypeFilter=" + "&serviceGroupSearchString=";

%>
<script type="text/javascript" src="../admin/js/breadcrumbs.js"></script>
<script type="text/javascript" src="../admin/js/cookies.js"></script>
<script type="text/javascript" src="../admin/js/main.js"></script>
<fmt:bundle basename="org.wso2.carbon.andes.event.ui.i18n.Resources">
<carbon:breadcrumb
        label="topic.details"
        resourceBundle="org.wso2.carbon.andes.event.ui.i18n.Resources"
        topPage="true"
        request="<%=request%>"/>
<div id="middle">
<div style="clear:both">&nbsp;</div>
<h2><fmt:message key="topic.Details"/></h2>

<div id="workArea">
<div id="test">
<table class="styledLeft" style="width:100%">
    <tr>
        <td><fmt:message key="topic.name"/></td>
        <td><input class="longInput" id="existingTopic" type="text" readonly="true"
                   value="<%=topic%>"></td>
    </tr>
</table>

<div style="clear:both">&nbsp;</div>


<div style="clear:both">&nbsp;</div>
<h3><fmt:message key="permission.Details"/></h3>
<table class="styledLeft" style="width:100%">
    <tbody>
    <tr>
        <td class="formRow" colspan="2">
            <table class="normal" style="width:100%" id="permissionsTable">
                <thead>
                <tr>
                    <th><fmt:message key="role"/></th>
                    <th><fmt:message key="subscribe"/></th>
                    <th><fmt:message key="publish"/></th>
                </tr>
                </thead>
                <tbody>
                <%
                    if (topicRolePermissions != null) {
                        for (TopicRolePermission topicRolePermission : topicRolePermissions) {
                %>
                <tr>
                    <td><%=topicRolePermission.getRoleName()%>
                    </td>
                    <td><input type="checkbox" id="<%=topicRolePermission.getRoleName()%>^subscribe"
                               value="subscribe" <% if (topicRolePermission.getAllowedToSubscribe()) { %>
                               checked <% } %>/>
                    </td>
                    <td>
                        <input type="checkbox" id="<%=topicRolePermission.getRoleName()%>^publish"
                               value="publish"  <% if (topicRolePermission.getAllowedToPublish()) { %>
                               checked <% } %>/>
                    </td>
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
        <% try {
            if(stub.checkCurrentUserHasAddTopicPermission()){ %>
        <td>
            <input type="button" onclick="updatePermissions()" value="Update Permissions">
        </td>
        <% } else { %>
        <td>
            <input type="button" disabled onclick="updatePermissions()" value="Update Permissions">
        </td>
        <% }
        } catch (AndesEventAdminServiceEventAdminException e) { %>
        <td>
            <input type="button" disabled onclick="updatePermissions()" value="Update Permissions">
        </td>
        <% } %>

    </tr>
    </tbody>
</table>
<div style="clear:both">&nbsp;</div>

<%if (!topic.equals("/")) { %>
    <h3><fmt:message key="publish"/></h3>
    <table class="styledLeft">
        <tr>
            <td class="formRaw">
                <table class="normal">
                    <tr>
                        <td><fmt:message key="topic"/>
                        </td>
                        <td>
                            <input class="longInput" type="text" readonly="true" name="topic"
                                   id="topic" value="<%=topic%>"/>
                        </td>

                    </tr>
                    <tr>
                        <td><fmt:message key="text.message"/></td>
                        <td><textarea cols="50" rows="10" name="textMessage" id="textMessage"></textarea>
                        </td>
                    </tr>
                </table>
            </td>
        </tr>
        <tr>

            <% try {
                if(stub.checkCurrentUserHasPublishTopicPermission(topic)){ %>
            <td>
                <input type="button" onclick="invokeService()" value="<fmt:message key="publish"/>">
            </td>
            <% } else { %>
            <td>
                <input type="button" disabled onclick="invokeService()" value="<fmt:message key="publish"/>">
            </td>
            <% }
            } catch (AndesEventAdminServiceEventAdminException e) { %>
            <td>
                <input type="button" disabled onclick="invokeService()" value="<fmt:message key="publish"/>">
            </td>
            <% } %>

        </tr>
    </table>
<% } %>
</div>
</div>
</div>
</fmt:bundle>
