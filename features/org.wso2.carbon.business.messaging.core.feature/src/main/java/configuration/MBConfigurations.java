/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package configuration;


import org.wso2.carbon.config.annotation.Configuration;
import org.wso2.carbon.config.annotation.Element;

@Configuration(namespace = "wso2.carbon.mb", description = "MB Configuration Parameters")
public class MBConfigurations {

    @Element(description = "mb node id")
    private String nodeId = "default";


    @Element(description = "amqp transport enabled")
    private boolean isAmqpEnabled = true;

    @Element(description = "amqp bind address")
    private String amqpBindAddress = "0.0.0.0";

    @Element(description = "amqp default connection enabled")
    private boolean amqpDefaultConnectionEnabled = true;

    @Element(description = "amqp default connection port")
    private String amqpPort = "5672";

    @Element(description =  "amqp ssl connection enabled")
    private  boolean amqpSSLConnectionEnabled = true;

    @Element(description = "amqp ssl connection port")
    private String amqpSSLPort = "8672";

    @Element(description = "flow control global low limit")
    private String flowControlGlobalLowLimit = "800";

    @Element(description = "flow control global high limit")
    private String flowControlGlobalHighLimit = "8000";

    @Element(description = "flow control global low limit")
    private String flowControlBufferBasedLowLimit = "100";

    @Element(description = "flow control global high limit")
    private String flowControlBufferBasedHighLimit = "1000";


    public String getNodeId() {
        return nodeId;
    }

    public boolean isAmqpEnabled() {
        return isAmqpEnabled;
    }

    public String getFlowControlBufferBasedHighLimit() {
        return flowControlBufferBasedHighLimit;
    }

    public String getFlowControlBufferBasedLowLimit() {
        return flowControlBufferBasedLowLimit;
    }

    public String getFlowControlGlobalHighLimit() {
        return flowControlGlobalHighLimit;
    }

    public String getFlowControlGlobalLowLimit() {
        return flowControlGlobalLowLimit;
    }

    public String getAmqpSSLPort() {
        return amqpSSLPort;
    }

    public boolean isAmqpSSLConnectionEnabled() {
        return amqpSSLConnectionEnabled;
    }

    public String getAmqpPort() {
        return amqpPort;
    }

    public boolean isAmqpDefaultConnectionEnabled() {
        return amqpDefaultConnectionEnabled;
    }

    public String getAmqpBindAddress() {
        return amqpBindAddress;
    }

}