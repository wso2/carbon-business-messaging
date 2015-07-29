<%@ page import="org.apache.axis2.AxisFault" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="carbon" uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" %>
<%@ page import="org.wso2.carbon.andes.stub.AndesAdminServiceStub" %>
<%@ page import="org.wso2.carbon.andes.ui.UIUtils" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.andes.stub.admin.types.Subscription" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>

<script type="text/javascript" src="js/treecontrol.js"></script>

<script>
    function refreshMessageCount(obj, durable){
        var aTag = jQuery(obj);
        var subscriptionID = aTag.attr('data-id');

        aTag.css('font-weight', 'bolder');

        jQuery.ajax({
            url:"retrive_message_count_for_subscriber_ajaxprocessor.jsp?subscriptionID="+subscriptionID+"&durable="+durable,
            data:{},
            type:"POST",
            success:function(data){
                data = data.trim();
                //$('#msg-'+subscriptionID).html(data);
                $(document.getElementById('msg-'+subscriptionID)).html(data);
                aTag.css('font-weight', 'normal');
                // jQuery('.normalTopicMsgCount',aTag.parent().parent()).html(data);
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
                dataType:"html",
                success: function() {
                    CARBON.showInfoDialog("Successfully unsubscribed.", function(){
                        location.href = "../subscriptions/topic_subscriptions_list.jsp";
                    });
                    aTag.css('font-weight', 'normal');

                },

                failure: function(transport) {
                    CARBON.showErrorDialog(trim(transport.responseText),function(){
                        location.href = "../subscriptions/topic_subscriptions_list.jsp";
                        return;
                    });
                    aTag.css('font-weight', 'normal');
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
    AndesAdminServiceStub stub = UIUtils.getAndesAdminServiceStub(config, session, request);
    Subscription[] filteredNormalTopicSubscriptionList = null;
    Subscription[] filteredActiveDurableTopicSubscriptionList = null;
    Subscription[] filteredInActiveDurableTopicSubscriptionList = null;
    Subscription[] normalTopicSubscriptionList;
    Subscription[] durableTopicSubscriptionList;
    Subscription[] activeDurableTopicSubscriptionList = null;
    Subscription[] inActiveDurableTopicSubscriptionList = null;
    int subscriptionCountPerPage = 20;
    int normalTopicPageNumber = 0;
    int activeDurableTopicPageNumber = 0;
    int inactiveDurableTopicPageNumber = 0;
    int numberOfPages = 1;
    String concatenatedParams = "region=region1&item=Topic_subscriptions";
    try {
        normalTopicSubscriptionList = stub.getAllLocalTempTopicSubscriptions();
        durableTopicSubscriptionList = stub.getAllDurableTopicSubscriptions();

        if (durableTopicSubscriptionList != null && durableTopicSubscriptionList.length != 0) {
            List<Subscription> activeSubs = new ArrayList<Subscription>();
            List<Subscription> inActiveSubs = new ArrayList<Subscription>();
            for (Subscription sub : durableTopicSubscriptionList) {
                if (sub.getActive()) {
                    activeSubs.add(sub);
                } else {
                    inActiveSubs.add(sub);
                }
            }
            activeDurableTopicSubscriptionList = new Subscription[activeSubs.size()];
            activeSubs.toArray(activeDurableTopicSubscriptionList);
            inActiveDurableTopicSubscriptionList = new Subscription[inActiveSubs.size()];
            inActiveSubs.toArray(inActiveDurableTopicSubscriptionList);
        }

        long totalNormalTopicSubscriptionCount;
        long totalActiveDurableTopicSubscriptionCount;
        long totalInactiveDurableTopicSubscriptionCount;
        String normalTopicPageNumberAsStr = request.getParameter("normalTopicPageNumber");
        String activeDurableTopicPageNumberAsStr = request.getParameter("activeDurableTopicPageNumber");
        String inActiveDurableTopicPageNumberAsStr = request.getParameter("inActiveDurableTopicPageNumber");

        if (normalTopicPageNumberAsStr != null) {
            normalTopicPageNumber = Integer.parseInt(normalTopicPageNumberAsStr);
        }
        if (activeDurableTopicPageNumberAsStr != null) {
            activeDurableTopicPageNumber = Integer.parseInt(activeDurableTopicPageNumberAsStr);
        }
        if (inActiveDurableTopicPageNumberAsStr != null) {
            inactiveDurableTopicPageNumber = Integer.parseInt(inActiveDurableTopicPageNumberAsStr);
        }

        if (normalTopicSubscriptionList != null) {
            totalNormalTopicSubscriptionCount = normalTopicSubscriptionList.length;
            numberOfPages = (int) Math.ceil(((float) totalNormalTopicSubscriptionCount) / subscriptionCountPerPage);
            filteredNormalTopicSubscriptionList = UIUtils.getFilteredSubscriptionList(normalTopicSubscriptionList, normalTopicPageNumber * subscriptionCountPerPage, subscriptionCountPerPage);
        }

        if (activeDurableTopicSubscriptionList != null) {
            totalActiveDurableTopicSubscriptionCount = activeDurableTopicSubscriptionList.length;
            numberOfPages = (int) Math.ceil(((float) totalActiveDurableTopicSubscriptionCount) / subscriptionCountPerPage);
            filteredActiveDurableTopicSubscriptionList = UIUtils.getFilteredSubscriptionList(activeDurableTopicSubscriptionList, activeDurableTopicPageNumber * subscriptionCountPerPage, subscriptionCountPerPage);
        }

        if (inActiveDurableTopicSubscriptionList != null) {
            totalInactiveDurableTopicSubscriptionCount = inActiveDurableTopicSubscriptionList.length;
            numberOfPages = (int) Math.ceil(((float) totalInactiveDurableTopicSubscriptionCount) / subscriptionCountPerPage);
            filteredInActiveDurableTopicSubscriptionList = UIUtils.getFilteredSubscriptionList(inActiveDurableTopicSubscriptionList, inactiveDurableTopicPageNumber * subscriptionCountPerPage, subscriptionCountPerPage);
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

<carbon:breadcrumb
        label="queues.list"
        resourceBundle="org.wso2.carbon.andes.ui.i18n.Resources"
        topPage="false"
        request="<%=request%>"/>


<div id="middle">
<h2><fmt:message key="subscription.topic.list"/></h2>

<div id="workArea">

<!--normal topic subscription list-->
<h3><fmt:message key="subscription.topic.normal.list"/></h3>
<%
    if (normalTopicSubscriptionList == null) {
%>
No subscriptions are created.
<br/>
<br/>
<br/>
<%
} else {

%>
<input type="hidden" name="pageNumber" value="<%=normalTopicPageNumber%>"/>
<carbon:paginator pageNumber="<%=normalTopicPageNumber%>" numberOfPages="<%=numberOfPages%>"
                  page="topic_subscriptions_list.jsp" pageNumberParameterName="normalTopicPageNumber"
                  resourceBundle="org.wso2.carbon.andes.ui.i18n.Resources"
                  prevKey="prev" nextKey="next"
                  parameters="<%=concatenatedParams%>"/>
<table class="styledLeft" style="width:100%;margin-bottom: 20px">
    <thead>
    <tr>
        <th><fmt:message key="subscription.identifier"/></th>
        <th><fmt:message key="subscription.topicName"/></th>
        <th><fmt:message key="subscription.active"/></th>
        <th><fmt:message key="subscription.nodeAddress"/></th>
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
        <td><%=sub.getDestination()%>
        </td>
        <td><%=sub.getActive()%>
        </td>
        <td><%=sub.getSubscriberNodeAddress()%>
        </td>
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
    if (activeDurableTopicSubscriptionList == null) {
%>
No subscriptions are created.
<br/>
<br/>
<br/>
<%
} else {

%>
<input type="hidden" name="pageNumber" value="<%=normalTopicPageNumber%>"/>
<carbon:paginator pageNumber="<%=normalTopicPageNumber%>" numberOfPages="<%=numberOfPages%>"
                  page="topic_subscriptions_list.jsp" pageNumberParameterName="activeDurableTopicPageNumber"
                  resourceBundle="org.wso2.carbon.andes.ui.i18n.Resources"
                  prevKey="prev" nextKey="next"
                  parameters="<%=concatenatedParams%>"/>
<table class="styledLeft" style="width:100%;margin-bottom: 20px">
    <thead>
    <tr>
        <th><fmt:message key="subscription.identifier"/></th>
        <th><fmt:message key="subscription.topicName"/></th>
        <th><fmt:message key="subscription.active"/></th>
        <th><fmt:message key="subscription.nodeAddress"/></th>
        <th colspan="3"><fmt:message key="subscription.numOfMessages"/></th>
    </tr>
    </thead>
    <tbody>
    <%
        if (filteredActiveDurableTopicSubscriptionList != null) {
            for (Subscription sub : filteredActiveDurableTopicSubscriptionList) {
    %>
    <tr>
        <td><%=sub.getSubscriptionIdentifier()%>
        </td>
        <td><%=sub.getDestination()%>
        </td>
        <td><%=sub.getActive()%>
        </td>
        <td><%=sub.getSubscriberNodeAddress()%>
        </td>

        <td id="msg-<%=sub.getSubscriptionIdentifier()%>"><%=sub.getNumberOfMessagesRemainingForSubscriber()%>
        </td>
        <td><a href="../queues/queue_messages_list.jsp?nameOfQueue=<%=sub.getSubscriberQueueName()%>">Browse</a></td>
        <td>
            <a style="background-image: url(images/refresh.gif);"
               class="icon-link"
               data-id="<%=sub.getSubscriptionIdentifier()%>"
               onclick="refreshMessageCount(this, 'true')">Refresh
            </a>
        </td>
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
    if (inActiveDurableTopicSubscriptionList == null) {
%>
No subscriptions are created.
<br/>
<br/>
<br/>
<%
} else {

%>
<input type="hidden" name="pageNumber" value="<%=normalTopicPageNumber%>"/>
<carbon:paginator pageNumber="<%=normalTopicPageNumber%>" numberOfPages="<%=numberOfPages%>"
                  page="topic_subscriptions_list.jsp" pageNumberParameterName="inActiveDurableTopicPageNumber"
                  resourceBundle="org.wso2.carbon.andes.ui.i18n.Resources"
                  prevKey="prev" nextKey="next"
                  parameters="<%=concatenatedParams%>"/>
<table class="styledLeft" style="width:100%;margin-bottom: 20px">
    <thead>
    <tr>
        <th><fmt:message key="subscription.identifier"/></th>
        <th><fmt:message key="subscription.topicName"/></th>
        <th><fmt:message key="subscription.active"/></th>
        <th><fmt:message key="subscription.nodeAddress"/></th>
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
        <td><%=sub.getSubscriptionIdentifier()%>
        </td>
        <td><%=sub.getDestination()%>
        </td>
        <td><%=sub.getActive()%>
        </td>
        <td><%=sub.getSubscriberNodeAddress()%>
        </td>

        <td id="msg-<%=sub.getSubscriptionIdentifier()%>"><%=sub.getNumberOfMessagesRemainingForSubscriber()%>
        </td>
        <td>
            <a style="background-image: url(images/refresh.gif);"
               class="icon-link"
               data-id="<%=sub.getSubscriptionIdentifier()%>"
               onclick="refreshMessageCount(this, 'true')">Refresh
            </a>
        </td>
        <td><a href="../queues/queue_messages_list.jsp?nameOfQueue=<%=sub.getSubscriberQueueName()%>">Browse</a></td>
        <td>
            <a style="background-image: url(images/unsubscribe.png);"
               class="icon-link"
               data-id="<%=sub.getSubscriberQueueName()%>"
               data-id-topic="<%=sub.getDestination()%>"
               onclick="unSubscribe(this)">Unsubscribe
            </a>
        </td>
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
