<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="carbon" uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" %>
<%@ page import="org.wso2.carbon.andes.stub.AndesAdminServiceStub" %>
<%@ page import="org.wso2.carbon.andes.ui.UIUtils" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.wso2.carbon.andes.cluster.mgt.ui.ClusterManagerClient" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.Arrays" %>
<%@ page import="org.wso2.carbon.andes.stub.admin.types.Subscription" %>
<%@ page import="org.wso2.carbon.andes.mgt.stub.AndesManagerServiceStub" %>
<%@ page import="org.wso2.andes.kernel.DestinationType" %>
<%@ page import="org.wso2.andes.kernel.ProtocolType" %>

<fmt:bundle basename="org.wso2.carbon.andes.ui.i18n.Resources">
    <carbon:breadcrumb
            label="queue.add"
            resourceBundle="org.wso2.carbon.andes.ui.i18n.Resources"
            topPage="true"
            request="<%=request%>"/>

    <script type="text/javascript" src="../admin/js/breadcrumbs.js"></script>
    <script type="text/javascript" src="../admin/js/cookies.js"></script>
    <script type="text/javascript" src="../admin/js/main.js"></script>

    <script>
        function refreshMessageCount(obj){
            var aTag = jQuery(obj);
            var queueName = aTag.attr('data-id');

            aTag.css('font-weight', 'bolder');

            jQuery.ajax({
                url:"retrieve_message_count_ajaxprocessor.jsp?queueName="+queueName+"&msgPattern=queue",
                data:{},
                type:"POST",
                success:function(data){
                    data = data.trim();
                    //$('#msg-'+subscriptionId).html(data);
                    $(aTag).parent().prev().html(data)
                    aTag.css('font-weight', 'normal');
                    // jQuery('.normalTopicMsgCount',aTag.parent().parent()).html(data);
                }
            });
        }

        function closeSubscription(obj) {
            var aTag = jQuery(obj);
            var subscriptionID = aTag.attr('subscription-id');
            var subscriptionDestination = aTag.attr('subscription-destination');
            var protocolType = aTag.attr('protocolType');
            var destinationType = aTag.attr('destinationType');
            aTag.css('font-weight', 'bolder');

            CARBON.showConfirmationDialog("Are you sure you want to close this subscription?", function(){
                $.ajax({
                    url:'subscriptions_close_ajaxprocessor.jsp?subscriptionID=' + subscriptionID + "&destination="
                    + subscriptionDestination + "&protocolType=" + protocolType + "&destinationType="
                    + destinationType,
                    async:true,
                    type:"POST",
                    success: function(o) {
                        if (o.indexOf("Error") > -1) {
                            CARBON.showErrorDialog("" + o, function() {
                                location.href = "../subscriptions/queue_subscriptions_list.jsp"
                            });
                        } else {
                            CARBON.showInfoDialog("Successfully closed subscription " + subscriptionID, function() {
                                location.href = "../subscriptions/queue_subscriptions_list.jsp"
                            });
                        }
                    },
                    failure: function(o) {
                        if (o.responseText !== undefined) {
                            alert("Error " + o.status + "\n Following is the message from the server.\n" + o.responseText);
                        }
                    }
                });
            });

        }
    </script>

    <%
        String filteredName = request.getParameter("queueNamePattern");
        String identifierPattern = request.getParameter("identifier");
        String filteredNameByExactMatch = request.getParameter("isQueueExactlyMatch");
        String identifierPatternByExactMatch = request.getParameter("isIdentifierExactlyMatch");
        boolean isFilteredNameByExactMatch = false;
        boolean isIdentifierPatternByExactMatch = false;

        if(null != filteredNameByExactMatch){
            isFilteredNameByExactMatch = true;
        }

        if(null != identifierPatternByExactMatch){
            isIdentifierPatternByExactMatch = true;
        }

        if(filteredName == null || filteredName.trim().length() == 0){
            filteredName = "";
        }
        if(identifierPattern == null || identifierPattern.trim().length() == 0){
            identifierPattern = "";
        }
        ClusterManagerClient client;
        String[] allClusterNodeAddressesInDropdown;
        boolean isClusteringEnabled = false;
        String serverURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
        ConfigurationContext configContext = (ConfigurationContext) config.getServletContext().getAttribute
        (CarbonConstants.CONFIGURATION_CONTEXT);
        String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
        String nodeId = "";
            try {
                client = new ClusterManagerClient(configContext, serverURL, cookie);
                isClusteringEnabled = client.isClusteringEnabled();
                allClusterNodeAddressesInDropdown = client.getAllClusterNodeAddresses();
                if(isClusteringEnabled){
                    List clusterNodesDropdownList = new ArrayList(Arrays.asList(allClusterNodeAddressesInDropdown));
                    clusterNodesDropdownList.add("All");
                    allClusterNodeAddressesInDropdown = (String[]) clusterNodesDropdownList.toArray(new String[0]);
                }
                nodeId = client.getMyNodeID();
            } catch (Exception e) {
                CarbonUIMessage.sendCarbonUIMessage(e.getMessage(), CarbonUIMessage.ERROR, request, e);
            %>
            <script type="text/javascript">
                location.href = "../admin/error.jsp";
                alert("error");
            </script>
            <%
                return;
            }
        String ownNodeId = request.getParameter("ownNodeId");
        if(ownNodeId == null || ownNodeId.trim().length() == 0){
            if (isClusteringEnabled) {
                ownNodeId = "All";
            } else {
               ownNodeId = nodeId ;
            }
        }
        AndesAdminServiceStub stub = UIUtils.getAndesAdminServiceStub(config, session, request);
        AndesManagerServiceStub managerServiceStub = UIUtils.getAndesManagerServiceStub(config, session);
        Subscription[] filteredSubscriptionList = null;
        Subscription[] subscriptionList;
        Subscription[] filteredSubscriptionListForSearch;
        int subscriptionCountPerPage = 20;
        int pageNumber = 0;
        int numberOfPages = 1;
        String myNodeID;
        String concatenatedParams = "region=region1&item=Queue_subscriptions&queueNamePattern="+ filteredName
        + "&identifier=" + identifierPattern + "&ownNodeId=" + ownNodeId;

        if(isFilteredNameByExactMatch){
            concatenatedParams += "&isQueueExactlyMatch="+ filteredNameByExactMatch;
        }

        if(isIdentifierPatternByExactMatch){
            concatenatedParams += "&isIdentifierExactlyMatch=" + identifierPatternByExactMatch;
        }

        try {
            myNodeID = managerServiceStub.getMyNodeID();

            int totalQueueSubscriptionCount;
            String pageNumberAsStr = request.getParameter("pageNumber");
            if (pageNumberAsStr != null) {
                pageNumber = Integer.parseInt(pageNumberAsStr);
            }
            filteredSubscriptionList = stub.getFilteredSubscriptions(true, true, ProtocolType.AMQP.name(),
            DestinationType.QUEUE.name(), filteredName, isFilteredNameByExactMatch, identifierPattern,
            isIdentifierPatternByExactMatch, ownNodeId, pageNumber, subscriptionCountPerPage);
            if (filteredSubscriptionList != null){
                totalQueueSubscriptionCount = stub.getTotalSubscriptionCountForSearchResult(true, true, ProtocolType
                .AMQP.name(),DestinationType.QUEUE.name(), filteredName, isFilteredNameByExactMatch,
                identifierPattern, isIdentifierPatternByExactMatch, ownNodeId);
                numberOfPages = (int) Math.ceil(((float) totalQueueSubscriptionCount) / subscriptionCountPerPage);
            }
        } catch (Exception e) {
            CarbonUIMessage.sendCarbonUIMessage(e.getMessage(), CarbonUIMessage.ERROR, request, e);
            e.printStackTrace();
    %>

    <script type="text/javascript">
        location.href = "../admin/error.jsp";
        alert("error");
    </script>
    <%
            return;
        }
    %>

    <div id="middle">
        <h2><fmt:message key="subscription.queue.durable.list"/></h2>

        <div id="workArea">

            <form name="filterForm" method="post" action="queue_subscriptions_list.jsp">
                <table class="styledLeft noBorders">
                    <thead>
                        <tr>
                            <th colspan="2">Search</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr>
                            <td class="leftCol-big" style="padding-right: 0 !important;">Queue name pattern
                            </td>
                            <td>
                                <input type="text" name="queueNamePattern" value="<%=filteredName%>"/>
                                <%
                                    if(isFilteredNameByExactMatch){
                                %>
                                     <input type="checkbox" name="isQueueExactlyMatch" checked/>Match entire word only
                                <%
                                    }else {
                                %>
                                     <input type="checkbox" name="isQueueExactlyMatch" />Match entire word only
                                <%
                                    }
                                %>
                            </td>
                        </tr>
                        <tr>
                            <td class="leftCol-big" style="padding-right: 0 !important;">Select connected node ID </td>
                             <td><select id="ownNodeId" name="ownNodeId">
                                <%
                                    try {
                                        if (isClusteringEnabled) {
                                %>
                                    <option selected="selected" value="<%=ownNodeId%>"><%=ownNodeId%></option>
                                    <% for(int i = 0; i < allClusterNodeAddressesInDropdown.length; i++){
                                            if(!ownNodeId.equals(allClusterNodeAddressesInDropdown[i].split(",")[0])){
                                     %>
                                        <option value="<%=allClusterNodeAddressesInDropdown[i].split(",")[0]%>">
                                        <%=allClusterNodeAddressesInDropdown[i].split(",")[0]%></option>
                                    <%      }
                                        } %>
                                    <%}else{ %>
                                     <option selected="selected" value="<%=nodeId%>"><%=nodeId%></option>
                                <%  }
                            } catch (Exception e) {%>
                              <script type="text/javascript">CARBON.showErrorDialog('Failed with BE.<%=e%>');</script>
                                <%  return;
                                } %>
                            </select>
                            </td>
                        </tr>
                        <tr>
                            <td class="leftCol-big" style="padding-right: 0 !important;">Identifier pattern
                            </td>
                            <td>
                                <input type="text" name="identifier" value="<%=identifierPattern%>"/>
                                 <%
                                    if(isIdentifierPatternByExactMatch){
                                %>
                                     <input type="checkbox" name="isIdentifierExactlyMatch" checked/>Match entire word only
                                <%
                                    }else {
                                %>
                                     <input type="checkbox" name="isIdentifierExactlyMatch" />Match entire word only
                                <%
                                    }
                                %>
                            </td>
                        </tr>
                        <tr>
                            <td>
                                <input class="button" type="submit" value="search"/>
                            </td>
                            <td>
                            </td>
                        </tr>

                    </tbody>
                </table>
            </form>
            <p>&nbsp;</p>

            <%
                if (filteredSubscriptionList == null) {
            %>
            No subscriptions to show.
            <%
            } else {

            %>
            <input type="hidden" name="pageNumber" value="<%=pageNumber%>"/>
            <carbon:paginator pageNumber="<%=pageNumber%>" numberOfPages="<%=numberOfPages%>"
                              page="queue_subscriptions_list.jsp" pageNumberParameterName="pageNumber"
                              resourceBundle="org.wso2.carbon.andes.ui.i18n.Resources"
                              prevKey="prev" nextKey="next"
                              parameters="<%=concatenatedParams%>"/>
            <table class="styledLeft" style="width:100%">
                <thead>
                <tr>
                    <th><fmt:message key="subscription.identifier"/></th>
                    <th><fmt:message key="subscription.exchange"/></th>
                    <th><fmt:message key="subscription.queueName"/></th>
                    <th><fmt:message key="subscription.queueOrTopic"/></th>
                    <th><fmt:message key="subscription.durable"/></th>
                    <th><fmt:message key="subscription.active"/></th>
                    <th><fmt:message key="subscription.nodeAddress"/></th>
                    <th><fmt:message key="subscription.originAddress"/></th>
                    <th colspan="2"><fmt:message key="subscription.numOfMessages"/></th>
                    <th><fmt:message key="subscription.operations"/></th>
                </tr>
                </thead>
                <tbody>
                <%
                    if (filteredSubscriptionList != null) {
                        for (Subscription sub : filteredSubscriptionList) {
                %>
                <tr>
                    <td><%=sub.getSubscriptionIdentifier()%>
                    </td>
                    <td><%=sub.getSubscriberQueueBoundExchange()%>
                    </td>
                    <td><%=sub.getSubscriberQueueName()%>
                    </td>
                    <td><%=sub.getSubscribedQueueOrTopicName()%>
                    </td>
                    <td><%=sub.getDurable()%>
                    </td>
                    <td><%=sub.getActive()%>
                    </td>
                    <td><%=sub.getConnectedNodeAddress()%>
                    </td>
                    <td><%=sub.getOriginHostAddress()%>
                    </td>

                    <td id="msg-<%=sub.getSubscriptionIdentifier()%>"><%=sub.getNumberOfMessagesRemainingForSubscriber()%>
                    </td>
                    <td>
                        <a style="background-image: url(images/refresh.gif);"
                           class="icon-link"
                           data-id="<%=sub.getSubscriberQueueName()%>"
                           onclick="refreshMessageCount(this)">Refresh
                        </a>
                    </td>
                    <%--Subscription close--%>
                    <% try {
                        //close is only allowed for subscriptions on this node
                        if(stub.checkCurrentUserHasQueueSubscriptionClosePermission() &&
                                sub.getConnectedNodeAddress().equals(myNodeID)){ %>
                    <td>
                        <a style="background-image: url(images/unsubscribe.png);"
                           class="icon-link"
                           subscription-id="<%=sub.getSubscriptionIdentifier()%>"
                           subscription-destination="<%=sub.getSubscriberQueueName()%>"
                           protocolType="<%=sub.getProtocolType()%>"
                           destinationType="<%=sub.getDestinationType()%>"
                           onclick="closeSubscription(this)">Close
                        </a>
                    </td>
                    <% } else { %>
                    <td>
                        <a style="background-image: url(images/unsubscribe_grey.png);"
                           class="icon-link disabled-ahref">Close
                        </a>
                    </td>
                    <% }
                    } catch (Exception e) { %>
                    <td>
                        <a style="background-image: url(images/unsubscribe_grey.png);"
                           class="icon-link disabled-ahref">Close
                        </a>
                    </td>
                    <% } %>
                </tr>
                <%
                        }
                    }
                %></tbody>
            </table>
            <%
                }
            %>
        </div>
    </div>
</fmt:bundle>