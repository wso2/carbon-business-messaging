<%@page import="java.sql.Array"%>
<%@page import="org.wso2.carbon.andes.stub.admin.types.Queue"%>
<%@page import="org.apache.axis2.AxisFault"%>
<%@ page import="org.wso2.carbon.andes.ui.UIUtils" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="carbon" uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.andes.stub.AndesAdminServiceStub" %>
<%@ page import="org.wso2.carbon.andes.stub.AndesAdminServiceBrokerManagerAdminException" %>
<%@ page import="org.wso2.carbon.andes.stub.admin.types.Message" %>
<script type="text/javascript" src="js/treecontrol.js"></script>
<fmt:bundle basename="org.wso2.carbon.andes.ui.i18n.Resources">
<jsp:include page="resources-i18n-ajaxprocessor.jsp"/>
    <carbon:jsi18n
            resourceBundle="org.wso2.carbon.andes.ui.i18n.Resources"
            request="<%=request%>"/>

    <script type="text/javascript" src="../admin/js/breadcrumbs.js"></script>
    <script type="text/javascript" src="../admin/js/cookies.js"></script>
    <script type="text/javascript" src="../admin/js/main.js"></script>
    <link rel="stylesheet" href="styles/dsxmleditor.css"/>
    <script type="text/javascript">
    
    function toggleCheck(source) 
    {
       
        var allcheckBoxes = document.getElementsByName("checkbox");
        for (var i=0; i < allcheckBoxes.length; i++) {
            if (allcheckBoxes[i].type == 'checkbox') 
            {
            	allcheckBoxes[i].checked = source.checked;
            }
        }
    }
    
    function checkSelectAll(source)
    {
    	var selectAllCheckBox = document.getElementsByName("selectAllCheckBox");
    	if(selectAllCheckBox[0].checked){
    		selectAllCheckBox[0].checked = source.checked;
    	}
    	
    	var allcheckBoxesInPage = $("input:checkbox"); 
    	 
    	var totalCheckboxCount = allcheckBoxesInPage.size() -1; ////removing the select all check box from the count.  
    	   
    	var checkedBoxes = $("input[@type=checkbox]:checked"); //the checked box count  
    	var checkedBoxesCount = checkedBoxes.size(); 
    	
    	if(totalCheckboxCount == checkedBoxesCount){
    		selectAllCheckBox[0].checked = true;arg0
    	}
    }
    
    function Ondelete(){
    
    }
  
    </script> 

    <%
        AndesAdminServiceStub stub = UIUtils.getAndesAdminServiceStub(config, session, request);
        String nameOfQueue = request.getParameter("nameOfQueue");
        String concatenatedParameters = "nameOfQueue=" + nameOfQueue;
        String pageNumberAsStr = request.getParameter("pageNumber");
        int msgCountPerPage = 100;
        int pageNumber = 0;
        int numberOfPages = 1;
        long totalMsgsInQueue;
        Message[] filteredMsgArray = null;
        if(nameOfQueue == null){
            nameOfQueue = "DeadLetterChannel";
        }
        if (pageNumberAsStr != null) {
            pageNumber = Integer.parseInt(pageNumberAsStr);
        }
        try {
            totalMsgsInQueue = stub.getTotalMessagesInQueue(nameOfQueue);
            numberOfPages = (int) Math.ceil(((float) totalMsgsInQueue) / msgCountPerPage);
            filteredMsgArray = stub.browseQueue(nameOfQueue, pageNumber * msgCountPerPage, msgCountPerPage);
        } catch (AndesAdminServiceBrokerManagerAdminException e) {
            CarbonUIMessage.sendCarbonUIMessage(e.getFaultMessage().getBrokerManagerAdminException().getErrorMessage(), CarbonUIMessage.ERROR, request, e);
        }
      %>
    <carbon:breadcrumb
            label="queue.content"
            resourceBundle="org.wso2.carbon.andes.ui.i18n.Resources"
            topPage="false"
            request="<%=request%>"/>

    <div id="middle">
        <h2><fmt:message key="dlc.queue.content"/>  <%=nameOfQueue%></h2>
        <div id="iconArea">
                 <table align="right"> 
           <thead>    
           <tr align="right">            
                <th align="right">
                 <a style="background-image: url(../admin/images/delete.gif);"
                           class="icon-link"
                           onclick="doDeleteDLC('<%=nameOfQueue%>')">Delete</a>
                 </th>
                 <th align="right">
                 <a style="background-image: url(../admin/images/move.gif);"
                           class="icon-link"
                           onclick="deRestoreMessages('<%=nameOfQueue%>')">Restore</a>
                 </th>
               <th align="right">
                   <a style="background-image: url(images/move.gif);"
                      class="icon-link"
                      onclick="doReRouteMessages('<%=nameOfQueue%>')">ReRoute</a>
               </th>
           </tr>
                </thead>
           </table>
        </div>

        <div id="workArea">
            <input type="hidden" name="pageNumber" value="<%=pageNumber%>"/>
            <carbon:paginator pageNumber="<%=pageNumber%>" numberOfPages="<%=numberOfPages%>"
                              page="dlc_messages_list.jsp" pageNumberParameterName="pageNumber"
                              resourceBundle="org.wso2.carbon.andes.ui.i18n.Resources"
                              prevKey="prev" nextKey="next" parameters="<%=concatenatedParameters%>"/>
                     
            <table class="styledLeft" style="width:100%">
                <thead>
                 <tr>
                    <th><input type="checkbox" name="selectAllCheckBox" onClick="toggleCheck(this)" /></th>
                    <th><fmt:message key="message.contenttype"/></th>
                    <th><fmt:message key="message.messageId"/></th>
                    <th><fmt:message key="message.correlationId"/></th>
                    <th><fmt:message key="message.type"/></th>
                    <th><fmt:message key="message.redelivered"/></th>
                    <th><fmt:message key="message.deliverymode"/></th>
                    <th><fmt:message key="message.priority"/></th>
                    <th><fmt:message key="message.timestamp"/></th>
                    <th><fmt:message key="message.destination"/></th>
                    <th><fmt:message key="message.properties"/></th>
                    <th><fmt:message key="message.summary"/></th>
                </tr>
                </thead>
                <tbody>
                <%
                        if(filteredMsgArray != null) {
                            for (Message queueMessage : filteredMsgArray) {
                            if (queueMessage != null) {
                                String msgProperties = queueMessage.getMsgProperties();
                                String contentType = queueMessage.getContentType();
                                String[] messageContent = queueMessage.getMessageContent();
                                String dlcMsgDestination = queueMessage.getDlcMsgDestination();

                %>
                <tr>
                    <td><input type="checkbox" name="checkbox" onClick="checkSelectAll(this)" value="<%= queueMessage.getJMSMessageId() %>"/></td>
                    <td><img src="images/<%= contentType.toLowerCase()%>.png"
                             alt=""/>&nbsp;&nbsp;<%= contentType%>
                    </td>
                    <td><%= queueMessage.getJMSMessageId()%>
                    </td>
                    <td><%= queueMessage.getJMSCorrelationId()%>
                    </td>
                    <td><%= queueMessage.getJMSType()%>
                    </td>
                    <td><%= queueMessage.getJMSReDelivered()%>
                    </td>
                    <td><%= queueMessage.getJMSDeliveredMode()%>
                    </td>
                    <td><%= queueMessage.getJMSPriority()%>
                    </td>
                    <td><%= queueMessage.getJMSTimeStamp()%>
                    </td>
                    <td><%= dlcMsgDestination %>
                    </td>
                    <td><%= msgProperties%>
                    </td>
                    <td><%= messageContent[0]%><a href="message_content.jsp?message=<%=messageContent[1]%>">&nbsp;&nbsp;&nbsp;more...</a>
                    </td>
                </tr>

                <%
                            }
                        }
                    }
                %>
                </tbody>
            </table>

        </div>
    </div>
      <div>
                <form id="deleteForm" name="input" action="dlc_messages_list.jsp" method="get"><input type="HIDDEN"
                                                                                 name="deleteMsg"
                                                                                 value=""/>
                                                                                 <input type="HIDDEN"
                                                                                 name="nameOfQueue"
                                                                                 value=""/>
                                                                                 <input type="HIDDEN"
                                                                                 name="msgList"
                                                                                 value=""/></form>
            <form id="restoreForm" name="input" action="dlc_messages_list.jsp" method="get"><input type="HIDDEN"
                                                                                 name="restoreMsgs"
                                                                                 value=""/>
                                                                                 <input type="HIDDEN"
                                                                                 name="nameOfQueue"
                                                                                 value=""/>
                                                                                 <input type="HIDDEN"
                                                                                 name="msgList"
                                                                                 value=""/></form>                                                                      
            </div>
</fmt:bundle>
