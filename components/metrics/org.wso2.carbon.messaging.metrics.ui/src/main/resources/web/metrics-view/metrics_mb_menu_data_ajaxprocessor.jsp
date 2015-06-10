
<%
    /*
     * Copyright 2015 WSO2 Inc. (http://wso2.org)
     * 
     * Licensed under the Apache License, Version 2.0 (the "License");
     * you may not use this file except in compliance with the License.
     * You may obtain a copy of the License at
     * 
     *     http://www.apache.org/licenses/LICENSE-2.0
     * 
     * Unless required by applicable law or agreed to in writing, software
     * distributed under the License is distributed on an "AS IS" BASIS,
     * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     * See the License for the specific language governing permissions and
     * limitations under the License.
     */
%>
<%@page import="java.io.OutputStreamWriter"%>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"%>
<%@ taglib uri="http://wso2.org/projects/carbon/taglibs/carbontags.jar" prefix="carbon"%>
<%@ page import="java.util.ArrayList"%>
<%@ page import="org.wso2.carbon.metrics.data.common.MetricList"%>
<%@ page import="org.wso2.carbon.metrics.data.common.Metric"%>
<%@ page import="org.wso2.carbon.metrics.data.common.MetricType"%>
<%@ page import="org.wso2.carbon.metrics.data.common.MetricAttribute"%>
<%@ page import="org.wso2.carbon.metrics.data.common.MetricDataFormat"%>
<%@ page import="org.wso2.carbon.metrics.view.ui.MetricsViewClient"%>
<%@ page import="org.wso2.carbon.metrics.view.ui.MetricDataWrapper"%>
<%@ page import="com.google.gson.Gson"%>
<%@ page import="org.apache.axis2.context.ConfigurationContext"%>
<%@ page import="org.wso2.carbon.CarbonConstants"%>
<%@ page import="org.wso2.carbon.utils.ServerConstants"%>
<%@ page import="org.wso2.carbon.ui.CarbonUIUtil"%>
<%@ page import="org.wso2.carbon.utils.CarbonUtils"%>
<%@ page import="org.wso2.carbon.ui.CarbonUIMessage"%>

<%
    String source = request.getParameter("source");
    String from = request.getParameter("from");
    String type = request.getParameter("type");

    String backendServerURL = CarbonUIUtil.getServerURL(config.getServletContext(), session);
    ConfigurationContext configContext = (ConfigurationContext) config.getServletContext().getAttribute(
            CarbonConstants.CONFIGURATION_CONTEXT);
    String cookie = (String) session.getAttribute(ServerConstants.ADMIN_SERVICE_COOKIE);
    MetricsViewClient metricsViewClient;
    try {
        metricsViewClient = new MetricsViewClient(cookie, backendServerURL, configContext);
        Gson gson = new Gson();
        MetricDataWrapper metricData = null;
        ArrayList<Metric> metrics = new ArrayList<Metric>();
        if ("Disruptor".equals(type)) {
            metrics.add(new Metric(MetricType.GAUGE, "org.wso2.mb.inbound.disruptor.message.count", "Total Messages in Inbound Disruptor",
                    MetricAttribute.VALUE, null));
            metrics.add(new Metric(MetricType.GAUGE, "org.wso2.mb.inbound.disruptor.ack.count", "Total Acks in Inbound Disruptor",
                    MetricAttribute.VALUE, null));
            metrics.add(new Metric(MetricType.GAUGE, "org.wso2.mb.outbound.disruptor.message.count", "Total Messages in Outbound Disruptor",
                    MetricAttribute.VALUE, null));
        } else if ("PubSub".equals(type)) {
            metrics.add(new Metric(MetricType.GAUGE, "org.wso2.mb.queue.subscribers.count", "Total Queue Subscribers",
                    MetricAttribute.VALUE, null));
            metrics.add(new Metric(MetricType.GAUGE, "org.wso2.mb.topic.subscribers.count", "Total Topic Subscribers",
                    MetricAttribute.VALUE, null));
            metrics.add(new Metric(MetricType.GAUGE, "org.wso2.mb.channels.active.count", "Total Channels",
                    MetricAttribute.VALUE, null));
        }

        MetricList metricList = new MetricList();
        metricList.setMetric(metrics.toArray(new Metric[metrics.size()]));
        metricData = metricsViewClient.findLastMetrics(metricList, source, from);
        if (metricData != null) {
            response.getWriter().write(gson.toJson(metricData));
        }
    } catch (Exception e) {
        return;
    }

    response.setContentType("application/json");
%>
