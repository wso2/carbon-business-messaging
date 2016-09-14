<%@ page import="org.wso2.carbon.andes.event.stub.service.AndesEventAdminServiceStub" %>
<%@ page import="org.wso2.carbon.andes.event.stub.service.AndesEventAdminServiceEventAdminException" %>
<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.andes.event.stub.core.TopicNode" %>
<%@ page import="java.util.Stack" %>
<%@page import="org.wso2.carbon.ui.CarbonUIMessage"%>
<%@ page import="java.util.Stack" %>



<%

    ConfigurationContext configContext = (ConfigurationContext) config.getServletContext()
            .getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);

    //Server URL which is defined in the server.xml
    String serverURL = CarbonUIUtil.getServerURL(config.getServletContext(),
            session) + "AndesEventAdminService.AndesEventAdminServiceHttpsSoap12Endpoint";
    AndesEventAdminServiceStub stub = new AndesEventAdminServiceStub(configContext, serverURL);
    TopicNode topicNode;
    String html="";
    try{
        int startIndex = Integer.parseInt(request.getParameter("startIndex"));
        int topicCount = Integer.parseInt(request.getParameter("count"));
        String topicPath = request.getParameter("topicPath");
        topicNode = stub.getPaginatedTopicTree(topicPath , startIndex, topicCount);

        TopicNode[] childNodes = topicNode.getChildren();
        if(childNodes != null && childNodes.length != 0){
            Stack stack = new Stack();
            for(TopicNode childNode: childNodes){
                stack.add(childNode);
            }
            int i = 0;
            while(!stack.isEmpty()){
               Object obj = stack.pop();
               TopicNode childNode = (TopicNode) obj;
               i++;
               html = html.concat("<ul><li>");
               if(childNode.getLeafNode()){
                    html = html.concat("<a class=\"minus\" onclick=\"treeColapse(this)\">&nbsp;</a>");
               } else {
                    html = html.concat("<li id=\"nodeList");
                    html = html.concat(childNode.getTopicName());
                    html = html.concat("\"><a class=\"plus\" nodeId=\"");
                    html = html.concat(childNode.getTopicName());
                    html = html.concat("\" listId=\"nodeList");
                    html = html.concat(childNode.getTopicName());
                    html = html.concat("\">&nbsp;</a>");
               }
               html = html.concat("<a class=\"treeNode\" onclick=\"hideTheRestAndShowMe(this)\" href=\"javascript:void(0)\" title=\"" +childNode.getTopicName()+ "\">"+childNode.getNodeName() + "</a>");

               // Add Sub Topic
               try {
                   if(stub.checkCurrentUserHasAddTopicPermission()){
                        html = html.concat("<a class=\"addSubtopicStyle\" onclick=\"showAddTopicWindow('");
                        html = html.concat(childNode.getTopicName());
                        html = html.concat( "')\">Add Subtopic</a>");
                   } else {
                        html = html.concat("<a class=\"addSubtopicStyle disabled-ahref\">Add Subtopic</a>");
                   }
               } catch (AndesEventAdminServiceEventAdminException e) {
                   html = html.concat("<a class=\"addSubtopicStyle disabled-ahref\">Add Subtopic</a>");

               }

               // View Details
               try {
                   if(stub.checkCurrentUserHasAddTopicPermission() || stub.checkCurrentUserHasDetailsTopicPermission()){
                        html = html.concat("<a class=\"topicDetailsStyle\" onclick=\"showManageTopicWindow('");
                        html = html.concat(childNode.getTopicName());
                        html = html.concat("')\">Details</a>");
                   }else {
                        html = html.concat("<a class=\"topicDetailsStyle disabled-ahref\">Details</a>");
                   }
               } catch (AndesEventAdminServiceEventAdminException e) {
                   html = html.concat("<a class=\"topicDetailsStyle disabled-ahref\">Details</a>");
               }

               //Delete Topic

                try {
                    if(stub.checkCurrentUserHasDeleteTopicPermission()){
                        if (!childNode.getTopicName().equals("/")) {
                            html = html.concat("<a class=\"topicDeleteStyle\" onclick = \"deleteTopic('");
                            html = html.concat(childNode.getTopicName());
                            html = html.concat("')\" > Delete </a >");
                        }

                    } else {
                        if (!childNode.getTopicName().equals("/")) {
                            html.concat("<a class=\"topicDeleteStyle disabled-ahref\" > Delete </a >");
                        }

                    }
                } catch (AndesEventAdminServiceEventAdminException e) {
                    if (!childNode.getTopicName().equals("/")) {
                        html = html.concat("<a class=\"topicDeleteStyle disabled-ahref\" > Delete </a >");
                    }
                }

               html = html.concat("</li></ul>");
            }
        } else{
           html = "No more topics to show";
        }

    } catch (Exception e) {
        html = "Error";
        CarbonUIMessage uiMsg = new CarbonUIMessage(CarbonUIMessage.ERROR, e.getMessage(), e);
        session.setAttribute(CarbonUIMessage.ID, uiMsg);
    }
%>
<%=html%>