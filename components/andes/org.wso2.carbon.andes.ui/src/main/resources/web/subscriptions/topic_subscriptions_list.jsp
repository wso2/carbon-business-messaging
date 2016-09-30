<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="carbon" uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" %>
<%@ page import="org.wso2.carbon.andes.stub.AndesAdminServiceStub" %>
<%@ page import="org.wso2.carbon.andes.ui.UIUtils" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>
<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.wso2.carbon.andes.stub.admin.types.Subscription" %>
<%@ page import="org.wso2.carbon.andes.cluster.mgt.ui.ClusterManagerClient" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="java.util.Arrays" %>
<%@ page import="org.wso2.carbon.andes.event.stub.service.AndesEventAdminServiceStub" %>
<%@ page import="org.wso2.carbon.andes.event.stub.service.AndesEventAdminServiceEventAdminException" %>
<%@ page import="org.wso2.carbon.andes.mgt.stub.AndesManagerServiceStub" %>
<%@ page import="org.wso2.andes.kernel.DestinationType" %>
<%@ page import="org.wso2.andes.kernel.ProtocolType" %>
<%@ page import="org.wso2.andes.configuration.AndesConfigurationManager" %>
<%@ page import="org.wso2.andes.configuration.enums.AndesConfiguration" %>
<%@ taglib uri="http://www.owasp.org/index.php/Category:OWASP_CSRFGuard_Project/Owasp.CsrfGuard.tld" prefix="csrf" %>

<script>
    function refreshMessageCount(obj, durable){
        var aTag = jQuery(obj);
        var queueName = aTag.attr('data-id');
        aTag.css('font-weight', 'bolder');

        var destinationType;

        if (durable == 'true') {
            destinationType = "<%= DestinationType.DURABLE_TOPIC.name()%>";
        } else {
            destinationType = "<%= DestinationType.TOPIC.name()%>";
        }

        jQuery.ajax({
            url:"retrive_message_count_for_subscriber_ajaxprocessor.jsp?queueName=" + queueName,
            data:{},
            type:"POST",
            beforeSend: function(xhr) {
                        xhr.setRequestHeader("<csrf:tokenname/>","<csrf:tokenvalue/>");
                    },
            success:function(data){
                data = data.trim();
                //$('#msg-'+queueName).html(data);
                $(document.getElementById('msg-'+queueName)).html(data);
                aTag.css('font-weight', 'normal');
                // jQuery('.normalTopicMsgCount',aTag.parent().parent()).html(data);
            },
            failure: function(o) {
                if (o.responseText !== undefined) {
                    alert("Error " + o.status + "\n Following is the message from the server.\n" + o.responseText);
                }
            }
        });
    }

    function unSubscribe(obj) {
        var aTag = jQuery(obj);
        var queueName = aTag.attr('data-id');
        var topicName = aTag.attr('data-id-topic');
        aTag.css('font-weight', 'bolder');

        CARBON.showConfirmationDialog("Are you sure you want to unsubscribe?", function(){
             $.ajax({
                url:'../queues/queue_delete_ajaxprocessor.jsp?nameOfQueue=' + queueName+"&nameOfTopic=" + topicName,
                async:true,
                type:"POST",
                beforeSend: function(xhr) {
                            xhr.setRequestHeader("<csrf:tokenname/>","<csrf:tokenvalue/>");
                        },
                success: function(o) {
                    if (o.indexOf("Error") > -1) {
                        CARBON.showErrorDialog("" + o, function() {
                            location.href = "../subscriptions/topic_subscriptions_list.jsp"
                        });
                    } else {
                        CARBON.showInfoDialog("Successfully unsubscribed.", function() {
                            location.href = "../subscriptions/topic_subscriptions_list.jsp"
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
                beforeSend: function(xhr) {
                            xhr.setRequestHeader("<csrf:tokenname/>","<csrf:tokenvalue/>");
                      },
                success: function(o) {
                    if (o.indexOf("Error") > -1) {
                        CARBON.showErrorDialog("" + o, function() {
                            location.href = "../subscriptions/topic_subscriptions_list.jsp"
                        });
                    } else {
                        CARBON.showInfoDialog("Successfully closed subscription " + subscriptionID, function() {
                            location.href = "../subscriptions/topic_subscriptions_list.jsp"
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

<fmt:bundle basename="org.wso2.carbon.andes.ui.i18n.Resources">
<carbon:jsi18n
        resourceBundle="org.wso2.carbon.andes.ui.i18n.Resources"
        request="<%=request%>"/>

<script type="text/javascript" src="../admin/js/breadcrumbs.js"></script>
<script type="text/javascript" src="../admin/js/cookies.js"></script>
<script type="text/javascript" src="../admin/js/main.js"></script>
<link rel="stylesheet" href="styles/dsxmleditor.css"/>

<%

    String filteredName = request.getParameter("topicNamePattern");
    String identifierPattern = request.getParameter("identifier");
    String filteredNameByExactMatch = request.getParameter("isTopicExactlyMatch");
    String identifierPatternByExactMatch = request.getParameter("isIdentifierExactlyMatch");
    boolean isFilteredNameByExactMatch = false;
    boolean isIdentifierPatternByExactMatch = false;

    if (null != filteredNameByExactMatch) {
        isFilteredNameByExactMatch = true;
    }

    if (null != identifierPatternByExactMatch) {
        isIdentifierPatternByExactMatch = true;
    }

    if (filteredName == null || filteredName.trim().length() == 0) {
        filteredName = "";
    }
    if (identifierPattern == null || identifierPattern.trim().length() == 0) {
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
        if (isClusteringEnabled) {
           List clusterNodesDropdownList = new ArrayList(Arrays.asList(allClusterNodeAddressesInDropdown));
           clusterNodesDropdownList.add("All");
           allClusterNodeAddressesInDropdown = (String[]) clusterNodesDropdownList.toArray(new String[0]);
        }
        nodeId = client.getMyNodeID();
    } catch (Exception e) {
    %>
    <script type="text/javascript">
        CARBON.showErrorDialog('Error in getting the cluster node addresses <%=e.getMessage()%>');
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
    AndesAdminServiceStub andesAdminStub = UIUtils.getAndesAdminServiceStub(config, session, request);
    AndesEventAdminServiceStub andesEventAdminStub = UIUtils.getAndesEventAdminServiceStub(config, session, request);
    AndesManagerServiceStub managerServiceStub = UIUtils.getAndesManagerServiceStub(config, session);
    Subscription[] filteredNormalTopicSubscriptionList = null;
    Subscription[] filteredActiveDurableTopicSubscriptionList = null;
    Subscription[] filteredInActiveDurableTopicSubscriptionList = null;
    Subscription[] activeDurableTopicSubscriptionList = null;
    Subscription[] filteredSubscriptionListForSearch;
    Boolean allowSharedSubscribers;
    int subscriptionCountPerPage = 20;
    int normalTopicPageNumber = 0;
    int activeDurableTopicPageNumber = 0;
    int inactiveDurableTopicPageNumber = 0;
    int numberOfNormalTopicSubscriptionPages = 1;
    int numberOfActiveDurableSubscriptionPages = 1;
    int numberOfInactiveDurableSubscriptionPages = 1;

    String concatenatedParams = "region=region1&item=Topic_subscriptions&topicNamePattern="+ filteredName
        + "&identifier=" + identifierPattern + "&ownNodeId=" + ownNodeId;

    if(isFilteredNameByExactMatch){
        concatenatedParams += "&isTopicExactlyMatch="+ filteredNameByExactMatch;
    }

    if(isIdentifierPatternByExactMatch){
        concatenatedParams += "&isIdentifierExactlyMatch=" + identifierPatternByExactMatch;
    }

    String myNodeID;
    try {
        myNodeID = managerServiceStub.getMyNodeID();
        long totalNormalTopicSubscriptionCount;
        long totalActiveDurableTopicSubscriptionCount;
        long totalInactiveDurableTopicSubscriptionCount;
        String normalTopicPageNumberAsStr = request.getParameter("normalTopicPageNumber");
        String activeDurableTopicPageNumberAsStr = request.getParameter("activeDurableTopicPageNumber");
        String inactiveDurableTopicPageNumberAsStr = request.getParameter("inactiveDurableTopicPageNumber");
        allowSharedSubscribers = AndesConfigurationManager.readValue(AndesConfiguration.ALLOW_SHARED_SHARED_SUBSCRIBERS);
        if (normalTopicPageNumberAsStr != null) {
            normalTopicPageNumber = Integer.parseInt(normalTopicPageNumberAsStr);
        }
        if (activeDurableTopicPageNumberAsStr != null) {
            activeDurableTopicPageNumber = Integer.parseInt(activeDurableTopicPageNumberAsStr);
        }
        if (inactiveDurableTopicPageNumberAsStr != null) {
            inactiveDurableTopicPageNumber = Integer.parseInt(inactiveDurableTopicPageNumberAsStr);
        }

        filteredNormalTopicSubscriptionList = andesAdminStub.getFilteredSubscriptions(false, true,
            ProtocolType.AMQP.name(), DestinationType.TOPIC.name(), filteredName, isFilteredNameByExactMatch,
            identifierPattern, isIdentifierPatternByExactMatch, ownNodeId, normalTopicPageNumber,
            subscriptionCountPerPage);

        if (filteredNormalTopicSubscriptionList != null) {

            totalNormalTopicSubscriptionCount = andesAdminStub.getTotalSubscriptionCountForSearchResult(false, true,
                ProtocolType.AMQP.name(), DestinationType.TOPIC.name(),filteredName, isFilteredNameByExactMatch,
                identifierPattern, isIdentifierPatternByExactMatch, ownNodeId);

            numberOfNormalTopicSubscriptionPages =
            (int) Math.ceil(((float) totalNormalTopicSubscriptionCount) / subscriptionCountPerPage);
        }


        filteredActiveDurableTopicSubscriptionList = andesAdminStub.getFilteredSubscriptions(true, true,
            ProtocolType.AMQP.name(), DestinationType.DURABLE_TOPIC.name(), filteredName, isFilteredNameByExactMatch,
             identifierPattern, isIdentifierPatternByExactMatch, ownNodeId, activeDurableTopicPageNumber,
             subscriptionCountPerPage);

        if (filteredActiveDurableTopicSubscriptionList != null) {
            totalActiveDurableTopicSubscriptionCount = andesAdminStub.getTotalSubscriptionCountForSearchResult(true, true,
                ProtocolType.AMQP.name(), DestinationType.DURABLE_TOPIC.name(),filteredName,
                isFilteredNameByExactMatch, identifierPattern, isIdentifierPatternByExactMatch, ownNodeId);
            numberOfActiveDurableSubscriptionPages =
             (int) Math.ceil(((float) totalActiveDurableTopicSubscriptionCount) / subscriptionCountPerPage);
        }

        filteredInActiveDurableTopicSubscriptionList =  andesAdminStub.getFilteredSubscriptions(true, false,
            ProtocolType.AMQP.name(), DestinationType.DURABLE_TOPIC.name(), filteredName, isFilteredNameByExactMatch,
             identifierPattern, isIdentifierPatternByExactMatch, ownNodeId, inactiveDurableTopicPageNumber,
             subscriptionCountPerPage);


        if (filteredInActiveDurableTopicSubscriptionList != null) {
            totalInactiveDurableTopicSubscriptionCount = andesAdminStub.getTotalSubscriptionCountForSearchResult(true, false,
                ProtocolType.AMQP.name(), DestinationType.DURABLE_TOPIC.name(),filteredName,
                isFilteredNameByExactMatch, identifierPattern, isIdentifierPatternByExactMatch, ownNodeId);
            numberOfInactiveDurableSubscriptionPages =
            (int) Math.ceil(((float) totalInactiveDurableTopicSubscriptionCount) / subscriptionCountPerPage);
        }

    } catch (Exception e) {
%>
    <script type="text/javascript">
        CARBON.showErrorDialog('Error in getting the subscriptions <%=e.getMessage()%>');
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
<h2><fmt:message key="subscription.topic.list"/></h2>

<div id="workArea">

    <form name="filterForm" method="post" action="topic_subscriptions_list.jsp">

        <table class="styledLeft noBorders">
            <thead>
                <tr>
                    <th colspan="2">Search</th>
                        </tr>
            </thead>
            <tbody>


                <tr>
                    <td class="leftCol-big" style="padding-right: 0 !important;">Topic name pattern
                    </td>
                    <td>
                        <input type="text" name="topicNamePattern" value="<%=filteredName%>"/>
                        <%
                           if(isFilteredNameByExactMatch){
                        %>
                             <input type="checkbox" name="isTopicExactlyMatch" checked/>Match entire word only
                        <%
                           }else {
                        %>
                             <input type="checkbox" name="isTopicExactlyMatch" />Match entire word only
                        <%
                           }
                        %>
                    </td>
                </tr>
                <tr>
                    <td class="leftCol-big" style="padding-right: 0 !important;">Select connected node ID </td>
                    <td>
                        <select id="ownNodeId" name="ownNodeId">
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
                                 <%     }
                                    }%>
                                <%  }else{ %>
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
                    <td class="leftCol-big" style="padding-right: 0 !important;">ID / Subscription Identifier pattern
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

<!--normal topic subscription list-->
<h3><fmt:message key="subscription.topic.normal.list"/></h3>
<%
    if (filteredNormalTopicSubscriptionList == null) {
%>
No subscriptions to show.
<br/>
<br/>
<br/>
<%
} else {

%>
<input type="hidden" name="pageNumber" value="<%=normalTopicPageNumber%>"/>
<carbon:paginator pageNumber="<%=normalTopicPageNumber%>" numberOfPages="<%=numberOfNormalTopicSubscriptionPages%>"
                  page="topic_subscriptions_list.jsp" pageNumberParameterName="normalTopicPageNumber"
                  resourceBundle="org.wso2.carbon.andes.ui.i18n.Resources"
                  prevKey="prev" nextKey="next"
                  parameters="<%=concatenatedParams%>"
                  action="POST"/>
<table class="styledLeft" style="width:100%;margin-bottom: 20px">
    <thead>
    <tr>
        <th><fmt:message key="subscription.identifier"/></th>
        <th><fmt:message key="subscription.topicName"/></th>
        <th><fmt:message key="subscription.nodeAddress"/></th>
        <th><fmt:message key="subscription.originAddress"/></th>
        <th><fmt:message key="subscription.operations"/></th>
    </tr>
    </thead>
    <tbody>
    <%
        if (filteredNormalTopicSubscriptionList != null) {
            for (Subscription sub : filteredNormalTopicSubscriptionList) {
    %>
    <tr>
        <td><%=sub.getSubscriptionIdentifier()%>
        </td>
        <td><%=sub.getSubscribedQueueOrTopicName()%>
        </td>
        <td><%=sub.getConnectedNodeAddress()%>
        </td>
        <td><%=sub.getOriginHostAddress()%>
        </td>
            <%--Subscription close--%>
        <% try {
            //close is only allowed for subscriptions on this node
            if(andesAdminStub.checkCurrentUserHasTopicSubscriptionClosePermission() &&
                    sub.getConnectedNodeAddress().equals(myNodeID)){ %>
        <td>
            <a style="background-image: url(images/unsubscribe.png);"
               class="icon-link"
               subscription-id="<%=sub.getSubscriptionIdentifier()%>"
               subscription-destination="<%=sub.getSubscribedQueueOrTopicName()%>"
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
        } %></tbody>
</table>
<%
    }
%>

<!--durable active topic subscription list-->
<h3><fmt:message key="subscription.topic.active.durable.list"/></h3>
<%
    if (filteredActiveDurableTopicSubscriptionList == null) {
%>
No subscriptions to show.
<br/>
<br/>
<br/>
<%
} else {

%>
<input type="hidden" name="pageNumber" value="<%=activeDurableTopicPageNumber%>"/>
<carbon:paginator pageNumber="<%=activeDurableTopicPageNumber%>" numberOfPages="<%=numberOfActiveDurableSubscriptionPages%>"
                  page="topic_subscriptions_list.jsp" pageNumberParameterName="activeDurableTopicPageNumber"
                  resourceBundle="org.wso2.carbon.andes.ui.i18n.Resources"
                  prevKey="prev" nextKey="next"
                  parameters="<%=concatenatedParams%>"
                  action="POST"/>
<table class="styledLeft" style="width:100%;margin-bottom: 20px">
    <thead>
    <tr>
        <th><fmt:message key="subscription.identifier"/></th>
        <th><fmt:message key="subscription.durable.identifier"/></th>
        <th><fmt:message key="subscription.topicName"/></th>
        <th><fmt:message key="subscription.nodeAddress"/></th>
        <th><fmt:message key="subscription.originAddress"/></th>
        <th colspan="3"><fmt:message key="subscription.numOfMessages"/></th>
        <th><fmt:message key="subscription.operations"/></th>
    </tr>
    </thead>
    <tbody>
    <%
        if (filteredActiveDurableTopicSubscriptionList != null) {
            for (Subscription sub : filteredActiveDurableTopicSubscriptionList) {
    %>
    <tr>
        <%
            String identifierForActiveSub = sub.getSubscriptionIdentifier();
            if(allowSharedSubscribers){
                identifierForActiveSub =  sub.getSubscriberQueueName() + "_" + sub.getSubscriptionIdentifier();
            }
        %>
        <td><%=identifierForActiveSub%>
        </td>
        <td><%=sub.getSubscriberQueueName()%>
        </td>
        <td><%=sub.getSubscribedQueueOrTopicName()%>
        </td>
        <td><%=sub.getConnectedNodeAddress()%>
        </td>
        <td><%=sub.getOriginHostAddress()%>
        </td>

        <td id="msg-<%=sub.getSubscriberQueueName()%>"><%=sub.getNumberOfMessagesRemainingForSubscriber()%>
        </td>
         <td>
            <a style="background-image: url(images/refresh.gif);"
               class="icon-link"
               data-id="<%=sub.getSubscriberQueueName()%>"
               subscription-destination="<%=sub.getSubscribedQueueOrTopicName()%>"
               onclick="refreshMessageCount(this, 'true')">Refresh
            </a>
        </td>
        <td><a href="../queues/queue_messages_list.jsp?nameOfQueue=<%=sub.getSubscriberQueueName()%>">Browse</a></td>

            <%--Subscription close--%>
        <% try {
            //close is only allowed for subscriptions on this node
            if(andesAdminStub.checkCurrentUserHasTopicSubscriptionClosePermission() &&
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
        } %></tbody>
</table>
<%
    }
%>

<!--durable inActive topic subscription list-->

<h3><fmt:message key="subscription.topic.inactive.durable.list"/></h3>

<%
    if (filteredInActiveDurableTopicSubscriptionList == null) {
%>
No subscriptions to show.
<br/>
<br/>
<br/>
<%
} else {

%>
<input type="hidden" name="pageNumber" value="<%=inactiveDurableTopicPageNumber%>"/>
<carbon:paginator pageNumber="<%=inactiveDurableTopicPageNumber%>" numberOfPages="<%=numberOfInactiveDurableSubscriptionPages%>"
                  page="topic_subscriptions_list.jsp" pageNumberParameterName="inactiveDurableTopicPageNumber"
                  resourceBundle="org.wso2.carbon.andes.ui.i18n.Resources"
                  prevKey="prev" nextKey="next"
                  parameters="<%=concatenatedParams%>"
                  action="POST"/>
<table class="styledLeft" style="width:100%;margin-bottom: 20px">
    <thead>
    <tr>
        <th><fmt:message key="subscription.durable.identifier"/></th>
        <th><fmt:message key="subscription.topicName"/></th>
        <th colspan="3"><fmt:message key="subscription.numOfMessages"/></th>
        <th><fmt:message key="subscription.operations"/></th>
    </tr>
    </thead>
    <tbody>
    <%
        if (filteredInActiveDurableTopicSubscriptionList != null) {
            for (Subscription sub : filteredInActiveDurableTopicSubscriptionList) {
    %>
    <tr>
        <%
            String identifierForInactiveSub = sub.getSubscriptionIdentifier();
            if(allowSharedSubscribers){
                identifierForInactiveSub =  sub.getSubscriberQueueName() + "_" + sub.getSubscriptionIdentifier();
            }
        %>
        <td><%=identifierForInactiveSub%>
        </td>
        <td><%=sub.getSubscribedQueueOrTopicName()%>
        </td>

        <td id="msg-<%=sub.getSubscriptionIdentifier()%>"><%=sub.getNumberOfMessagesRemainingForSubscriber()%>
        </td>

        <%--Refresh--%>
        <td>
            <a style="background-image: url(images/refresh.gif);"
               class="icon-link"
               data-id="<%=sub.getSubscriptionIdentifier()%>"
               onclick="refreshMessageCount(this, 'true')">Refresh
            </a>
        </td>

        <%--Browse--%>
        <% try {
            if(andesEventAdminStub.checkCurrentUserHasDetailsTopicPermission()){ %>
        <td><a href="../queues/queue_messages_list.jsp?nameOfQueue=<%=sub.getSubscriberQueueName()%>">Browse</a></td>
        <% } else { %>
        <td><a class="disabled-ahref" href="#">Browse</a></td>
        <% }
        } catch (AndesEventAdminServiceEventAdminException e) { %>
        <td><a class="disabled-ahref" href="#">Browse</a></td>
        <% } %>

        <%--Unsubscribe--%>
        <% try {
            if(andesEventAdminStub.checkCurrentUserHasDeleteTopicPermission()){ %>
        <td>
            <a style="background-image: url(images/unsubscribe.png);"
               class="icon-link"
               data-id="<%=sub.getSubscriberQueueName()%>"
               data-id-topic="<%=sub.getSubscribedQueueOrTopicName()%>"
               onclick="unSubscribe(this)">Unsubscribe
            </a>
        </td>
        <% } else { %>
        <td>
            <a style="background-image: url(images/unsubscribe_grey.png);"
               class="icon-link disabled-ahref">Unsubscribe
            </a>
        </td>
        <% }
        } catch (AndesEventAdminServiceEventAdminException e) { %>
        <td>
            <a style="background-image: url(images/unsubscribe_grey.png);"
               class="icon-link disabled-ahref">Unsubscribe
            </a>
        </td>
        <% } %>

    </tr>
    <%
            }
        } %></tbody>
</table>
<%
    }
%>
</div>
</div>
</fmt:bundle>
