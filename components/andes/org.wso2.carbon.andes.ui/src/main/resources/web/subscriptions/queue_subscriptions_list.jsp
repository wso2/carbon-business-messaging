<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="carbon" uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" %>
<%@ page import="org.wso2.carbon.andes.stub.AndesAdminServiceStub" %>
<%@ page import="org.wso2.carbon.andes.ui.UIUtils" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.andes.stub.admin.types.Subscription" %>
<%@ page import="org.wso2.carbon.andes.mgt.stub.AndesManagerServiceStub" %>

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
            var subscriptionId = aTag.attr('data-id');
            var queueName = subscriptionId.split("@")[1];

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
            aTag.css('font-weight', 'bolder');

            CARBON.showConfirmationDialog("Are you sure you want to close this subscription?", function(){
                $.ajax({
                    url:'subscriptions_close_ajaxprocessor.jsp?subscriptionID=' + subscriptionID + "&destination="
                    + subscriptionDestination,
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
        AndesAdminServiceStub stub = UIUtils.getAndesAdminServiceStub(config, session, request);
        AndesManagerServiceStub managerServiceStub = UIUtils.getAndesManagerServiceStub(config, session);
        Subscription[] filteredSubscriptionList = null;
        Subscription[] subscriptionList;
        int subscriptionCountPerPage = 20;
        int pageNumber = 0;
        int numberOfPages = 1;
        String myNodeID;
        String concatenatedParams = "region=region1&item=Queue_subscriptions";
        try {
            myNodeID = managerServiceStub.getMyNodeID();
            subscriptionList = stub.getAllDurableQueueSubscriptions();
            long totalQueueSubscriptionCount;
            String pageNumberAsStr = request.getParameter("pageNumber");
            if (pageNumberAsStr != null) {
                pageNumber = Integer.parseInt(pageNumberAsStr);
            }

            if (subscriptionList != null) {
                totalQueueSubscriptionCount = subscriptionList.length;
                numberOfPages = (int) Math.ceil(((float) totalQueueSubscriptionCount) / subscriptionCountPerPage);
                filteredSubscriptionList = UIUtils.getFilteredSubscriptionList(subscriptionList, pageNumber * subscriptionCountPerPage, subscriptionCountPerPage);
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
            <%
                if (subscriptionList == null) {
            %>
            No subscriptions are created.
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
                    <td><%=sub.getSubscriberNodeAddress()%>
                    </td>

                    <td id="msg-<%=sub.getSubscriptionIdentifier()%>"><%=sub.getNumberOfMessagesRemainingForSubscriber()%>
                    </td>
                    <td>
                        <a style="background-image: url(images/refresh.gif);"
                           class="icon-link"
                           data-id="<%=sub.getSubscriptionIdentifier()%>"
                           onclick="refreshMessageCount(this)">Refresh
                        </a>
                    </td>
                    <%--Subscription close--%>
                    <% try {
                        //close is only allowed for subscriptions on this node
                        if(stub.checkCurrentUserHasSubscriptionClosePermission() &&
                                sub.getSubscriberNodeAddress().equals(myNodeID)){ %>
                    <td>
                        <a style="background-image: url(images/unsubscribe.png);"
                           class="icon-link"
                           subscription-id="<%=sub.getSubscriptionIdentifier()%>"
                           subscription-destination="<%=sub.getSubscriberQueueName()%>"
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