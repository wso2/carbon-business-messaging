<!--
~ Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
~
~ WSO2 Inc. licenses this file to you under the Apache License,
~ Version 2.0 (the "License"); you may not use this file except
~ in compliance with the License.
~ You may obtain a copy of the License at
~
~ http://www.apache.org/licenses/LICENSE-2.0
~
~ Unless required by applicable law or agreed to in writing,
~ software distributed under the License is distributed on an
~ "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
~ KIND, either express or implied. See the License for the
~ specific language governing permissions and limitations
~ under the License.
-->

<%@ page import="org.apache.axis2.context.ConfigurationContext" %>
<%@ page import="org.wso2.carbon.CarbonConstants" %>
<%@ page import="org.wso2.carbon.andes.ui.Constants" %>
<%@ page import="org.wso2.carbon.stat.publisher.conf.xsd.StatPublisherConfiguration" %>
<%@ page import="org.wso2.carbon.stat.publisher.ui.StatPublisherClient" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage" %>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil" %>
<%@ page import="org.wso2.carbon.utils.ServerConstants" %>


<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" prefix="carbon" %>
<script type="text/javascript" src="js/toggle.js"></script>


<fmt:bundle basename="org.wso2.carbon.stat.publisher.ui.i18n.Resources">

<%
    String serverURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
    ConfigurationContext configContext =
            (ConfigurationContext) config.getServletContext().getAttribute(CarbonConstants.CONFIGURATION_CONTEXT);
    String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);

    StatPublisherClient client;
    StatPublisherConfiguration statPublisherConfiguration;


    try {
        client = new StatPublisherClient(configContext, serverURL, cookie);
        statPublisherConfiguration = client.getStatConfiguration();

    } catch (Exception e) {
        CarbonUIMessage.sendCarbonUIMessage(e.getMessage(), CarbonUIMessage.ERROR, request, e);
%>
<jsp:include page="../admin/error.jsp"/>
<%
        return;
    }
%>
<%
    String setConfig = request.getParameter("setConfig");

    String get_username_value = request.getParameter("user_name");

    String get_password_value = request.getParameter("password");

    String get_URL_value = request.getParameter("url_address");


    String message_stat_check_value = request.getParameter("message_stat_enable_check");

    String system_stat_check_value = request.getParameter("system_stat_enable_check");

    String MB_stat_check_value = request.getParameter("mb_stat_enable_check");

    String get_node_URL = request.getLocalAddr() + ":" + request.getLocalPort();


    if (setConfig != null) {    // form submitted request to set eventing config
        statPublisherConfiguration = new StatPublisherConfiguration();


        if (message_stat_check_value != null) {
            statPublisherConfiguration.setMessageStatEnable(true);
        } else {
            statPublisherConfiguration.setMessageStatEnable(false);
        }
        if (system_stat_check_value != null) {
            statPublisherConfiguration.setSystemStatEnable(true);
        } else {
            statPublisherConfiguration.setSystemStatEnable(false);
        }
        if (MB_stat_check_value != null) {
            statPublisherConfiguration.setMbStatEnable(true);
        } else {
            statPublisherConfiguration.setMbStatEnable(false);
        }
        if (get_URL_value != null) {
            statPublisherConfiguration.setURL(get_URL_value);
        }
        if (get_username_value != null) {
            statPublisherConfiguration.setUsername(get_username_value);
        }
        if (get_password_value != null) {
            statPublisherConfiguration.setPassword(get_password_value);
        }
        if (get_node_URL != null) {
            statPublisherConfiguration.setNodeURL(get_node_URL);
        }

        try {
            client.setStatConfiguration(statPublisherConfiguration);

%>
<script type="text/javascript">
    /*jQuery(document).init(function () {*/
    function handleOK() {

    }

    CARBON.showInfoDialog("Statistics Configuration Successfully Updated!", handleOK);
    /*});*/
</script>
<%
} catch (Exception e) {
    if (!e.getCause().getMessage().toLowerCase().contains("you are not authorized")) {
        response.setStatus(500);
        CarbonUIMessage uiMsg = new CarbonUIMessage(CarbonUIMessage.ERROR, e.getMessage(), e);
        session.setAttribute(CarbonUIMessage.ID, uiMsg);
%>
<jsp:include page="../admin/error.jsp"/>
<%
        }
    }
} else {
    try {
        statPublisherConfiguration = client.getStatConfiguration();
    } catch (Exception e) {
        if (!e.getCause().getMessage().toLowerCase().contains("you are not authorized")) {
            response.setStatus(500);
            CarbonUIMessage uiMsg = new CarbonUIMessage(CarbonUIMessage.ERROR, e.getMessage(), e);
            session.setAttribute(CarbonUIMessage.ID, uiMsg);
%>
<jsp:include page="../admin/error.jsp"/>
<%
            }
        }
    }


    if (statPublisherConfiguration != null) {

        get_username_value = statPublisherConfiguration.getUsername();

        get_password_value = statPublisherConfiguration.getPassword();

        get_URL_value = statPublisherConfiguration.getURL();


        if (statPublisherConfiguration.getMbStatEnable()) {
            MB_stat_check_value = "checked";

        }

        if (statPublisherConfiguration.getMessageStatEnable()) {
            message_stat_check_value = "checked";

        }
        if (statPublisherConfiguration.getSystemStatEnable()) {
            system_stat_check_value = "checked";

        }


    }


%>

<div id="middle">
    <h2><fmt:message key="mb.stat.publisher"/></h2>

    <div id="workArea">

        <form id="details_form" action="/carbon/stat-publisher/stat_publisher.jsp" method="POST"
              onsubmit="return DoValidation();">
            <input type="hidden" name="setConfig" value="on"/>
            <table width="100%" class="styledLeft" style="margin-left: 0;">
                <col width="40%">
                <thead>
                <tr>
                    <th colspan="4"><fmt:message key="connection.configuration"/></th>
                </tr>
                </thead>
                                   <tbody>
                    <tr>
                        <td>
                            <fmt:message key="username"/>
                        </td>
                        <td><label for="user_name"></label><input type="text" id="user_name" name="user_name" value="<%=get_username_value%>"/></td>

                    </tr>
                    <tr>
                        <td>
                            <fmt:message key="password"/>
                        </td>
                        <td><label for="password"></label><input type="password" id="password" name="password" value="<%=get_password_value%>"/></td>

                    </tr>

                    <tr>
                        <td>
                            <fmt:message key="url"/>
                        </td>
                        <td><label for="url_address"></label><input type="text" id="url_address" name="url_address" value="<%=get_URL_value%>"/>
                            <input type="button" value="<fmt:message key="test.server"/>" onclick="testServer()"/>
                            &nbsp;&nbsp;&nbsp;&nbsp;<i> eg:- tcp://localhost:7611,tcp://...</i>
                        </td>

                    </tr>
                    </tbody>
                    <thead>
                    <tr>
                        <th colspan="4"><fmt:message key="statistic.configuration"/></th>
                    </tr>
                    </thead>

                    <tbody>
                    <tr>
                        <td><label for="message_stat_enable_check"></label><input type="checkbox" id="message_stat_enable_check" name="message_stat_enable_check"
                                <%=message_stat_check_value%>
                                />&nbsp;&nbsp;&nbsp;&nbsp;
                            <fmt:message key="publish.message.statistics"/>
                        </td>
                        <td></td>
                    </tr>
                    <tr>
                        <td><label for="system_stat_enable_check"></label><input type="checkbox" id="system_stat_enable_check" name="system_stat_enable_check"
                                <%=system_stat_check_value%>
                                />&nbsp;&nbsp;&nbsp;&nbsp;
                            <fmt:message key="publish.system.statistics"/>
                        </td>
                        <td></td>
                    </tr>

                    <tr>
                        <td><label for="mb_stat_enable_check"></label><input type="checkbox" id="mb_stat_enable_check" name="mb_stat_enable_check"
                                <%=MB_stat_check_value%>
                                />&nbsp;&nbsp;&nbsp;&nbsp;
                            <fmt:message key="publish.mb.statistics"/>
                        </td>
                        <td></td>
                    </tr>

                <tr>
                    <td><input type="submit" value="<fmt:message key="button.update"/>"/>

                    </td>
                    <td></td>
                </tr>
                </tbody>

            </table>

        </form>
    </div>
</div>
<script type="text/javascript" src="js/toggle.js"></script>
</fmt:bundle>