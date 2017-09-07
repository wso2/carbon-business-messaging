/*
 * Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 */

package org.wso2.carbon.andes.admin.mqtt.internal;

public class Message {

    private String msgProperties;
    private String contentType;
    private String[] messageContent;
    private String JMSMessageId;
    private String JMSCorrelationId;
    private String JMSType;
    private Boolean JMSReDelivered;
    private String JMSDeliveredMode;
    private Integer JMSPriority;
    private Long JMSTimeStamp;
    private Long JMSExpiration;
    private String dlcMsgDestination;
    private Long andesMsgMetadataId;

    public String getMsgProperties() {
        return msgProperties;
    }

    public void setMsgProperties(String msgProperties) {
        this.msgProperties = msgProperties;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String[] getMessageContent() {
        return messageContent;
    }

    public void setMessageContent(String[] messageContent) {
        this.messageContent = messageContent;
    }

    public String getJMSMessageId() {
        return JMSMessageId;
    }

    public void setJMSMessageId(String JMSMessageId) {
        this.JMSMessageId = JMSMessageId;
    }

    public String getJMSCorrelationId() {
        return JMSCorrelationId;
    }

    public void setJMSCorrelationId(String JMSCorrelationId) {
        this.JMSCorrelationId = JMSCorrelationId;
    }

    public String getJMSType() {
        return JMSType;
    }

    public void setJMSType(String JMSType) {
        this.JMSType = JMSType;
    }

    public Boolean getJMSReDelivered() {
        return JMSReDelivered;
    }

    public void setJMSReDelivered(Boolean JMSReDelivered) {
        this.JMSReDelivered = JMSReDelivered;
    }

    public String getJMSDeliveredMode() {
        return JMSDeliveredMode;
    }

    public void setJMSDeliveredMode(String JMSDeliveredMode) {
        this.JMSDeliveredMode = JMSDeliveredMode;
    }

    public Integer getJMSPriority() {
        return JMSPriority;
    }

    public void setJMSPriority(Integer JMSPriority) {
        this.JMSPriority = JMSPriority;
    }

    public Long getJMSTimeStamp() {
        return JMSTimeStamp;
    }

    public void setJMSTimeStamp(Long JMSTimeStamp) {
        this.JMSTimeStamp = JMSTimeStamp;
    }

    public Long getJMSExpiration() {
        return JMSExpiration;
    }

    public void setJMSExpiration(Long JMSExpiration) {
        this.JMSExpiration = JMSExpiration;
    }

    public String getDlcMsgDestination() {
        return dlcMsgDestination;
    }

    public void setDlcMsgDestination(String dlcMsgDestination) {
        this.dlcMsgDestination = dlcMsgDestination;
    }

    public Long getAndesMsgMetadataId() {
        return andesMsgMetadataId;
    }

    public void setAndesMsgMetadataId(Long andesMsgMetadataId) {
        this.andesMsgMetadataId = andesMsgMetadataId;
    }
}
