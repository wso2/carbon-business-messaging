<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" prefix="carbon" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ page import="org.apache.axis2.client.Options" %>
<%@ page import="org.apache.axis2.client.ServiceClient" %>
<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.andes.event.stub.service.AndesEventAdminServiceStub" %>
<%@ page import="org.wso2.carbon.andes.event.stub.core.TopicNode" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="java.util.Stack" %>
<%@ page import="org.wso2.carbon.andes.event.stub.service.AndesEventAdminServiceEventAdminException" %>
<script type="text/javascript" src="../ajax/js/prototype.js"></script>
<script type="text/javascript" src="../resources/js/resource_util.js"></script>
<!--Yahoo includes for dom event handling-->
<script src="../yui/build/yahoo-dom-event/yahoo-dom-event.js" type="text/javascript"></script>

<!--Yahoo includes for animations-->
<script src="../yui/build/animation/animation-min.js" type="text/javascript"></script>

<script src="../yui/build/yahoo/yahoo-min.js" type="text/javascript"></script>
<script src="../yui/build/utilities/utilities.js" type="text/javascript"></script>

<!--Yahoo includes for menus-->
<link rel="stylesheet" type="text/css" href="../yui/build/menu/assets/skins/sam/menu.css"/>
<script type="text/javascript" src="../yui/build/container/container_core-min.js"></script>
<script type="text/javascript" src="../yui/build/menu/menu-min.js"></script>

<!--Local js includes-->
<script type="text/javascript" src="js/treecontrol.js"></script>

<link href="css/tree-styles.css" media="all" rel="stylesheet"/>
<link href="css/dsxmleditor.css" media="all" rel="stylesheet"/>
<link rel="stylesheet" type="text/css" href="css/topics.css"/>

<fmt:bundle basename="org.wso2.carbon.event.ui.i18n.Resources">
    <script type="text/javascript" src="../admin/js/breadcrumbs.js"></script>

    <%
        String message = request.getParameter("message");
        if (message != null) {
    %><h3><%=message%>
</h3><%
    }
%>
    <jsp:include page="../resources/resources-i18n-ajaxprocessor.jsp"/>
    <script type="text/javascript" src="../admin/js/breadcrumbs.js"></script>
    <script type="text/javascript" src="../admin/js/cookies.js"></script>
    <script type="text/javascript" src="../admin/js/main.js"></script>

    <carbon:breadcrumb
            label="add"
            resourceBundle="org.wso2.carbon.event.ui.i18n.Resources"
            topPage="false"
            request="<%=request%>"/>
    <div id="middle">
        <div id="workArea">
            <h2>Topic List</h2>

            <table style="width:100%;margin-bottom:20px;" class="yui-skin-sam">
                <tr>
                    <td class="tree-top"><h3>Topics</h3></td>
                </tr>
                <tr>
                    <td valign="top" style="width:200px;" class="leftBox">
                        <div class="treeControl" id="topicTree">
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


                                TopicNode topicNode = null;
                                try {
                                    topicNode = stub.getAllTopics();
                                } catch (Exception e) {
                                %>
                            <script type="text/javascript">
                                CARBON.showErrorDialog('<%= e.getMessage()%>');
                            </script>
                                <%
                                }


                                Stack stack = new Stack();
                                stack.add(topicNode);

                                while (!stack.isEmpty()) {
                                    Object obj = stack.pop();
                                    if (obj instanceof String) {
                            %><%=obj%><%
                        } else {
                        %>
                            <ul><%
                                stack.add("</ul>\n");
                                TopicNode node = (TopicNode) obj;
                                TopicNode[] children = node.getChildren();
                                if (children != null && children.length > 0) {
                                    for (TopicNode child : children) {
                                        if (child != null) {
                                            stack.push(child);
                                        }
                                    }
                                }
                            %>
                                <li><a class="minus" onclick="treeColapse(this)">&nbsp;</a>

                                    <a class="treeNode" onclick="hideTheRestAndShowMe(this)"
                                       href="#"
                                       title="<%=node.getTopicName()%>"><%=node.getNodeName()%>
                                    </a>

                                        <%--Add topic--%>
                                    <% try {
                                        if(stub.checkCurrentUserHasAddTopicPermission()){ %>
                                    <a class="addSubtopicStyle"
                                       onclick="showAddTopicWindow('<%=node.getTopicName()%>')">Add
                                        Subtopic</a>
                                    <% } else { %>
                                    <a class="addSubtopicStyle disabled-ahref">Add
                                        Subtopic</a>

                                    <% }
                                    } catch (AndesEventAdminServiceEventAdminException e) { %>
                                    <a class="addSubtopicStyle disabled-ahref">Add
                                        Subtopic</a>
                                    <% } %>


                                        <%--View details--%>
                                    <% try {
                                        if(stub.checkCurrentUserHasAddTopicPermission() || stub.checkCurrentUserHasDetailsTopicPermission()){ %>
                                    <a class="topicDetailsStyle"
                                       onclick="showManageTopicWindow('<%=node.getTopicName()%>')">Details</a>
                                    <% } else { %>
                                    <a class="topicDetailsStyle disabled-ahref">Details</a>
                                    <% }
                                    } catch (AndesEventAdminServiceEventAdminException e) { %>
                                    <a class="topicDetailsStyle disabled-ahref">Details</a>
                                    <% } %>


                                        <%--Delete topic--%>
                                    <% try {
                                        if(stub.checkCurrentUserHasDeleteTopicPermission()){ %>

                                    <%if (!node.getTopicName().equals("/")) {%>
                                    <a class="topicDeleteStyle"
                                       onclick="deleteTopic('<%=node.getTopicName()%>')">Delete</a> <%
                                        }
                                    %>

                                    <% } else { %>

                                    <%if (!node.getTopicName().equals("/")) {%>
                                    <a class="topicDeleteStyle disabled-ahref">Delete</a> <%
                                        }
                                    %>

                                    <% }
                                    } catch (AndesEventAdminServiceEventAdminException e) { %>

                                    <%if (!node.getTopicName().equals("/")) {%>
                                    <a class="topicDeleteStyle disabled-ahref">Delete</a> <%
                                        }
                                    %>

                                    <% } %>




                                </p>
                                <%

                                        }
                                    }
                                %>
                            </ul>
                            <span id="domChecker"></span>
                        </div>
                    </td>
                        <%--<td valign="top" class="topicData" id="topicData">

                        </td>--%>
                </tr>
            </table>
        </div>
    </div>

    <script type="text/javascript">
        function hideTheRestAndShowMe(me) {
            jQuery(".addSubtopicStyle").hide();
            jQuery(".topicDetailsStyle").hide();
            jQuery(".topicSubscribeStyle").hide();
            jQuery(".topicDeleteStyle").hide();
            jQuery(me).next().show();
            jQuery(me).next().next().show();
            jQuery(me).next().next().next().show();
            jQuery(me).next().next().next().next().show();
        }
        jQuery(document).ready(function () {
            jQuery(".addSubtopicStyle").hide();
            jQuery(".topicDetailsStyle").hide();
            jQuery(".topicSubscribeStyle").hide();
            jQuery(".topicDeleteStyle").hide();
        });
    </script>

    <%--    <script type="text/javascript">
        addRightClicks(); //adding right clicks to the tree
        alternateTableRows('expiredsubscriptions', 'tableEvenRow', 'tableOddRow');
        alternateTableRows('validsubscriptions', 'tableEvenRow', 'tableOddRow');
    </script>--%>

</fmt:bundle>