<%@ page import="org.apache.axis2.client.Options" %>
<%@ page import="org.apache.axis2.client.ServiceClient" %>
<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.andes.event.stub.admin.Subscription" %>
<%@ page import="org.wso2.carbon.andes.event.stub.service.AndesEventAdminServiceEventAdminException" %>
<%@ page import="org.wso2.carbon.andes.event.stub.service.AndesEventAdminServiceStub" %>
<%@ page import="org.wso2.carbon.andes.event.ui.UIUtils" %>
<%@ page import="org.wso2.carbon.registry.resource.stub.ResourceAdminServiceExceptionException" %>
<%@ page import="org.wso2.carbon.registry.resource.stub.ResourceAdminServiceStub" %>
<%@ page import="org.wso2.carbon.registry.resource.stub.beans.xsd.ResourceTreeEntryBean" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%
    ConfigurationContext configContext = (ConfigurationContext) config.getServletContext()
            .getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
//Server URL which is defined in the server.xml
    String eventAdminServerURL = CarbonUIUtil.getServerURL(config.getServletContext(),
            session) + "AndesEventAdminService.AndesEventAdminServiceHttpsSoap12Endpoint";
    String resourceAdminServerURL = CarbonUIUtil.getServerURL(config.getServletContext(),
            session) + "ResourceAdminService";
    AndesEventAdminServiceStub eventAdminStub = new AndesEventAdminServiceStub(configContext, eventAdminServerURL);
    ResourceAdminServiceStub resourceAdminStub = new ResourceAdminServiceStub(configContext, resourceAdminServerURL);

    String cookie = (String) session.getAttribute(org.wso2.carbon.utils.ServerConstants.ADMIN_SERVICE_COOKIE);

    ServiceClient eventAdminClient = eventAdminStub._getServiceClient();
    Options eventAdminOption = eventAdminClient.getOptions();
    eventAdminOption.setManageSession(true);
    eventAdminOption.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, cookie);

    ServiceClient resourceAdminClient = resourceAdminStub._getServiceClient();
    Options resourceAdminOptions = resourceAdminClient.getOptions();
    resourceAdminOptions.setManageSession(true);
    resourceAdminOptions.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, cookie);

    String message = "";
    boolean isSubscriptionExist = false;
    String topic = request.getParameter("topic");
    session.removeAttribute("topicWsSubscriptions");

    try {

        //get all child resources of given topic
        ResourceTreeEntryBean resources = resourceAdminStub.getResourceTreeEntry(UIUtils.getTopicID(topic));

        //check subscriptions exist for given topic
        Subscription[] jmsSubscriptionsForTopic = eventAdminStub.getJMSSubscriptionsForTopic(topic);
        if ((jmsSubscriptionsForTopic != null && jmsSubscriptionsForTopic.length > 0)) {
            message = "Error: Subscriptions exists for this topic, Please remove all subscriptions before deleting the topic";
            isSubscriptionExist = true;
        //check subscriptions exist for sub topic of given topic
        } else if (resources != null) {
            String[] subTopics = resources.getChildren();
            if (subTopics != null) {
                for (String path : subTopics) {
                    String subTopic = path.replace("/_system/governance/event/topics/", "");
                    Subscription[] jmsSubscriptionsForSubTopic = eventAdminStub.getJMSSubscriptionsForTopic(subTopic);
                    if (jmsSubscriptionsForSubTopic != null && jmsSubscriptionsForSubTopic.length > 0) {
                        isSubscriptionExist = true;
                        message = "Error: Subscriptions exists for sub topic of this topic, Please remove all subscriptions before deleting the topic";
                        break;
                    }
                }
            }
        }

        //if subscriptions not exists then proceed with deleting
        if (!isSubscriptionExist) {
            eventAdminStub.removeTopic(topic);
            message = "Topic removed successfully";
        }

    } catch (AndesEventAdminServiceEventAdminException e) {
        message = "Error: " + e.getFaultMessage().getEventAdminException().getErrorMessage();
    } catch (ResourceAdminServiceExceptionException e) {
        message = "Error: " + e.getFaultMessage().getResourceAdminServiceException().getMessage();
%> <%=message%>
<%
    }
%><%=message%>
