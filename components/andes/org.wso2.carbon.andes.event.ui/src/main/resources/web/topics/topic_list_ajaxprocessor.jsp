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
    StringBuilder builder = new StringBuilder();
    builder.append(html);
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
               builder.append("<ul><li>");
               if(childNode.getLeafNode()){
                    builder.append("<a class=\"minus\" onclick=\"treeColapse(this)\">&nbsp;</a>");
               } else {
                    builder.append("<li id=\"nodeList");
                    builder.append(childNode.getTopicName());
                    builder.append("\"><a class=\"plus\" nodeId=\"");
                    builder.append(childNode.getTopicName());
                    builder.append("\" listId=\"nodeList");
                    builder.append(childNode.getTopicName());
                    builder.append("\">&nbsp;</a>");
               }
               builder.append("<a class=\"treeNode\" onclick=\"hideTheRestAndShowMe(this)\" href=\"javascript:void(0)\" title=\"" +childNode.getTopicName()+ "\">"+childNode.getNodeName() + "</a>");

               // Add Sub Topic
               try {
                   if(stub.checkCurrentUserHasAddTopicPermission()){
                        builder.append("<a class=\"addSubtopicStyle\" onclick=\"showAddTopicWindow('");
                        builder.append(childNode.getTopicName());
                        builder.append( "')\">Add Subtopic</a>");
                   } else {
                        builder.append("<a class=\"addSubtopicStyle disabled-ahref\">Add Subtopic</a>");
                   }
               } catch (AndesEventAdminServiceEventAdminException e) {
                   builder.append("<a class=\"addSubtopicStyle disabled-ahref\">Add Subtopic</a>");

               }

               // View Details
               try {
                   if(stub.checkCurrentUserHasAddTopicPermission() || stub.checkCurrentUserHasDetailsTopicPermission()){
                        builder.append("<a class=\"topicDetailsStyle\" onclick=\"showManageTopicWindow('");
                        builder.append(childNode.getTopicName());
                        builder.append("')\">Details</a>");
                   }else {
                        builder.append("<a class=\"topicDetailsStyle disabled-ahref\">Details</a>");
                   }
               } catch (AndesEventAdminServiceEventAdminException e) {
                   builder.append("<a class=\"topicDetailsStyle disabled-ahref\">Details</a>");
               }

               //Delete Topic

                try {
                    if(stub.checkCurrentUserHasDeleteTopicPermission()){
                        if (!childNode.getTopicName().equals("/")) {
                            builder.append("<a class=\"topicDeleteStyle\" onclick = \"deleteTopic('");
                            builder.append(childNode.getTopicName());
                            builder.append("')\" > Delete </a >");
                        }

                    } else {
                        if (!childNode.getTopicName().equals("/")) {
                            builder.append("<a class=\"topicDeleteStyle disabled-ahref\" > Delete </a >");
                        }

                    }
                } catch (AndesEventAdminServiceEventAdminException e) {
                    if (!childNode.getTopicName().equals("/")) {
                        builder.append("<a class=\"topicDeleteStyle disabled-ahref\" > Delete </a >");
                    }
                }

               builder.append("</li></ul>");
            }
        } else{
           builder.append("No more topics to show");
        }

    } catch (Exception e) {
        builder.append("Error");
        CarbonUIMessage uiMsg = new CarbonUIMessage(CarbonUIMessage.ERROR, e.getMessage(), e);
        session.setAttribute(CarbonUIMessage.ID, uiMsg);
    }
%>
<%=builder.toString()%>
