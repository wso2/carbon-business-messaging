<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="carbon" uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" %>
<%@ page import="org.wso2.carbon.andes.stub.AndesAdminServiceStub" %>
<%@ page import="org.wso2.carbon.andes.ui.UIUtils" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.andes.stub.admin.types.Subscription" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.ArrayList" %>
<%@ page import="org.wso2.carbon.andes.event.stub.service.AndesEventAdminServiceStub" %>
<%@ page import="org.wso2.carbon.andes.event.stub.service.AndesEventAdminServiceEventAdminException" %>
<%@ page import="org.wso2.carbon.andes.mgt.stub.AndesManagerServiceStub" %>
<%@ page import="org.wso2.andes.kernel.DestinationType" %>
<%@ page import="org.wso2.andes.kernel.ProtocolType" %>
<%@ page import="org.wso2.andes.configuration.AndesConfigurationManager" %>
<%@ page import="org.wso2.andes.configuration.enums.AndesConfiguration" %>

<script>
    function refreshMessageCount(obj, durable){
        var aTag = jQuery(obj);
        var subscriptionID = aTag.attr('data-id');
        aTag.css('font-weight', 'bolder');

        var destinationType;

        if (durable == 'true') {
            destinationType = <%=DestinationType.TOPIC.name()%>;
        } else {
            destinationType = <%=DestinationType.DURABLE_TOPIC.name()%>;
        }

        jQuery.ajax({
            url:"retrive_message_count_for_subscriber_ajaxprocessor.jsp?subscriptionID=" + subscriptionID
            + "&durable=" + durable + "&protocolType=" + <%=ProtocolType.AMQP.name()%>
            + "&destinationType=" + destinationType,
            data:{},
            type:"POST",
            success:function(data){
                data = data.trim();
                //$('#msg-'+subscriptionID).html(data);
                $(document.getElementById('msg-'+subscriptionID)).html(data);
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
                type:"GET",
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
    AndesAdminServiceStub andesAdminStub = UIUtils.getAndesAdminServiceStub(config, session, request);
    AndesEventAdminServiceStub andesEventAdminStub = UIUtils.getAndesEventAdminServiceStub(config, session, request);
    AndesManagerServiceStub managerServiceStub = UIUtils.getAndesManagerServiceStub(config, session);
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
    int numberOfNormalTopicSubscriptionPages = 1;
    int numberOfActiveDurableSubscriptionPages = 1;
    int numberOfInactiveDurableSubscriptionPages = 1;
    String concatenatedParams = "region=region1&item=Topic_subscriptions";
    String myNodeID;
    try {
        myNodeID = managerServiceStub.getMyNodeID();
        normalTopicSubscriptionList = andesAdminStub.getSubscriptions("false", "*", ProtocolType.AMQP.name(),
                                        DestinationType.TOPIC.name());
        durableTopicSubscriptionList = andesAdminStub.getSubscriptions("true", "*", ProtocolType.AMQP.name(),
                                        DestinationType.DURABLE_TOPIC.name());

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

            Boolean allowSharedSubscribers = AndesConfigurationManager.readValue(
                AndesConfiguration.ALLOW_SHARED_SHARED_SUBSCRIBERS);


            if (allowSharedSubscribers) {
                List<Subscription> inActiveSubsToDisplay = new ArrayList<Subscription>();
                List<String> alreadySelectedQueueNames = new ArrayList<String>();
                for (Subscription inActiveSub : inActiveSubs) {
                    if (alreadySelectedQueueNames.contains(inActiveSub.getSubscriberQueueName())) {
                        continue;
                    } else {
                        inActiveSubsToDisplay.add(inActiveSub);
                        alreadySelectedQueueNames.add(inActiveSub.getSubscriberQueueName());
                    }
                }

                inActiveSubs = inActiveSubsToDisplay;
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
        String inactiveDurableTopicPageNumberAsStr = request.getParameter("inactiveDurableTopicPageNumber");

        if (normalTopicPageNumberAsStr != null) {
            normalTopicPageNumber = Integer.parseInt(normalTopicPageNumberAsStr);
        }
        if (activeDurableTopicPageNumberAsStr != null) {
            activeDurableTopicPageNumber = Integer.parseInt(activeDurableTopicPageNumberAsStr);
        }
        if (inactiveDurableTopicPageNumberAsStr != null) {
            inactiveDurableTopicPageNumber = Integer.parseInt(inactiveDurableTopicPageNumberAsStr);
        }

        if (normalTopicSubscriptionList != null) {
            totalNormalTopicSubscriptionCount = normalTopicSubscriptionList.length;
            numberOfNormalTopicSubscriptionPages = (int) Math.ceil(((float) totalNormalTopicSubscriptionCount) / subscriptionCountPerPage);
            filteredNormalTopicSubscriptionList = UIUtils.getFilteredSubscriptionList(normalTopicSubscriptionList, normalTopicPageNumber * subscriptionCountPerPage, subscriptionCountPerPage);
        }

        if (activeDurableTopicSubscriptionList != null) {
            totalActiveDurableTopicSubscriptionCount = activeDurableTopicSubscriptionList.length;
            numberOfActiveDurableSubscriptionPages = (int) Math.ceil(((float) totalActiveDurableTopicSubscriptionCount) / subscriptionCountPerPage);
            filteredActiveDurableTopicSubscriptionList = UIUtils.getFilteredSubscriptionList(activeDurableTopicSubscriptionList, activeDurableTopicPageNumber * subscriptionCountPerPage, subscriptionCountPerPage);
        }

        if (inActiveDurableTopicSubscriptionList != null) {
            totalInactiveDurableTopicSubscriptionCount = inActiveDurableTopicSubscriptionList.length;
            numberOfInactiveDurableSubscriptionPages = (int) Math.ceil(((float) totalInactiveDurableTopicSubscriptionCount) / subscriptionCountPerPage);
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
<carbon:paginator pageNumber="<%=normalTopicPageNumber%>" numberOfPages="<%=numberOfNormalTopicSubscriptionPages%>"
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
        <td><%=sub.getActive()%>
        </td>
        <td><%=sub.getSubscriberNodeAddress()%>
        </td>
            <%--Subscription close--%>
        <% try {
            //close is only allowed for subscriptions on this node
            if(andesAdminStub.checkCurrentUserHasTopicSubscriptionClosePermission() &&
                    sub.getSubscriberNodeAddress().equals(myNodeID)){ %>
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
    if (activeDurableTopicSubscriptionList == null) {
%>
No subscriptions are created.
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
        if (filteredActiveDurableTopicSubscriptionList != null) {
            for (Subscription sub : filteredActiveDurableTopicSubscriptionList) {
    %>
    <tr>
        <td><%=sub.getSubscriptionIdentifier()%>
        </td>
        <td><%=sub.getSubscribedQueueOrTopicName()%>
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
               subscription-destination="<%=sub.getSubscribedQueueOrTopicName()%>"
               onclick="refreshMessageCount(this, 'true')">Refresh
            </a>
        </td>
            <%--Subscription close--%>
        <% try {
            //close is only allowed for subscriptions on this node
            if(andesAdminStub.checkCurrentUserHasTopicSubscriptionClosePermission() &&
                    sub.getSubscriberNodeAddress().equals(myNodeID)){ %>
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
    if (inActiveDurableTopicSubscriptionList == null) {
%>
No subscriptions are created.
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
        <td><%=sub.getSubscribedQueueOrTopicName()%>
        </td>
        <td><%=sub.getActive()%>
        </td>
        <td><%=sub.getSubscriberNodeAddress()%>
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
