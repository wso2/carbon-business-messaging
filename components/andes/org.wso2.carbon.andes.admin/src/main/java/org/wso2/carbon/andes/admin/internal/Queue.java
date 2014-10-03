package org.wso2.carbon.andes.admin.internal;


import java.util.Calendar;

public class Queue {

    private String queueName;

    private long queueDepth;

    private long messageCount;

    private Calendar createdTime;

    private Calendar updatedTime;

    private String createdFrom;

    public Queue() {
    }

    public Queue(String queueName) {
        this.queueName = queueName;
    }
     
    public String getQueueName() {
        return queueName;
    }

    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    public long getQueueDepth() {
        return queueDepth;
    }

    public void setQueueDepth(long queueDepth) {
        this.queueDepth = queueDepth;
    }

    public long getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(long messageCount) {
        this.messageCount = messageCount;
    }

    public Calendar getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(Calendar createdTime) {
        this.createdTime = createdTime;
    }

    public Calendar getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(Calendar updatedTime) {
        this.updatedTime = updatedTime;
    }

    public String getCreatedFrom() {
        return createdFrom;
    }

    public void setCreatedFrom(String createdFrom) {
        this.createdFrom = createdFrom;
    }
}
