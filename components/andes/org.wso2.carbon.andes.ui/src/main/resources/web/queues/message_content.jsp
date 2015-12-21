<%@ page import="org.apache.commons.lang.StringEscapeUtils" %>
<%@ page import="org.apache.commons.lang.StringUtils" %>

<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib prefix="carbon" uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" %>
<script type="text/javascript" src="js/treecontrol.js"></script>
<fmt:bundle basename="org.wso2.carbon.andes.ui.i18n.Resources">

    <carbon:jsi18n
            resourceBundle="org.wso2.carbon.andes.ui.i18n.Resources"
            request="<%=request%>"/>

    <script type="text/javascript" src="../admin/js/breadcrumbs.js"></script>
    <script type="text/javascript" src="../admin/js/cookies.js"></script>
    <script type="text/javascript" src="../admin/js/main.js"></script>
    <link rel="stylesheet" href="styles/dsxmleditor.css"/>

    <%
    
        final int TEXT_AREA_COLUMN_WIDTH  = 200;
        String wholeMessage = request.getParameter("message");
        // This will not handle the all the possible cases
        // But provide a acceptable level of wrapping.
        int textAreaRows = Math.max(StringUtils.countMatches(wholeMessage, "\n"), 
                                        wholeMessage.length()/TEXT_AREA_COLUMN_WIDTH) + 2;
       
        
    %>

     <carbon:breadcrumb
            label="message.content"
            resourceBundle="org.wso2.carbon.andes.ui.i18n.Resources"
            topPage="false"
            request="<%=request%>"/>

    <div id="middle">
        <h2><fmt:message key="message.content"/></h2>
        <div id="workArea">
            <%-- There should be no spaces between the "<textarea>" tags as it counts them as white spaces and displays in the page--%>
            <textarea rows="<%=textAreaRows %>" cols="<%=TEXT_AREA_COLUMN_WIDTH %>" readonly="true" style="border:none;"><%=wholeMessage%></textarea>
        </div>
    </div>

</fmt:bundle>
