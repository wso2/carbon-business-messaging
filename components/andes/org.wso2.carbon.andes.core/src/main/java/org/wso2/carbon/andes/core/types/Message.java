package org.wso2.carbon.andes.core.types;

public class Message {

    private String msgProperties;
    private String contentType;
    private String[] messageContent;
    private String JMSMessageId;
    private String JMSCorrelationId;
    private String JMSType;
    private Boolean JMSReDelivered;
    private Integer JMSDeliveredMode;
    private Integer JMSPriority;
    private Long JMSTimeStamp;
    private Long JMSExpiration;
    private String dlcMsgDestination;

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

    public Integer getJMSDeliveredMode() {
        return JMSDeliveredMode;
    }

    public void setJMSDeliveredMode(Integer JMSDeliveredMode) {
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
}
