/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.andes.core;

import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * Represents a queue (both queue and topics) within MB core
  */
public class AndesQueue {
    public String queueName;
    public String queueOwner;
    public boolean isExclusive;
    public boolean isDurable;
    public int subscriptionCount;

    private ProtocolType protocolType;
    private DestinationType destinationType;

    /**
     * Added to infer the state of the queue during concurrent message delivery.
     * Initial value before the first purge within this server session should be 0.
     */
    private Long lastPurgedTimestamp;

    /**
     * create an instance of andes queue
     *
     * @param queueName       name of the queue
     * @param queueOwner      owner of the queue (virtual host)
     * @param isExclusive     is queue exclusive
     * @param isDurable       is queue durable
     * @param protocolType    The protocol which the queue belongs to
     * @param destinationType The destination type which the queue belongs to
     */
    public AndesQueue(String queueName, String queueOwner, boolean isExclusive, boolean isDurable,
                      ProtocolType protocolType, DestinationType destinationType) {
        this.queueName = queueName;
        this.queueOwner = queueOwner;
        this.isExclusive = isExclusive;
        this.isDurable = isDurable;
        this.subscriptionCount = 1;
        this.lastPurgedTimestamp = 0L;
        this.protocolType = protocolType;
        this.destinationType = destinationType;
    }

    public Long getLastPurgedTimestamp() {
        return lastPurgedTimestamp;
    }

    public void setLastPurgedTimestamp(Long lastPurgedTimestamp) {
        this.lastPurgedTimestamp = lastPurgedTimestamp;
    }

    /**
     * create an instance of andes queue
     *
     * @param queueAsStr queue information as encoded string
     */
    public AndesQueue(String queueAsStr) throws AndesException {
        String[] propertyToken = queueAsStr.split(",");
        for (String pt : propertyToken) {
            String[] tokens = pt.split("=");
            if (tokens[0].equals("queueName")) {
                this.queueName = tokens[1];
            } else if ("queueOwner".equals(tokens[0])) {
                this.queueOwner = tokens[1].equals("null") ? null : tokens[1];
            } else if ("isExclusive".equals(tokens[0])) {
                this.isExclusive = Boolean.parseBoolean(tokens[1]);
            } else if ("isDurable".equals(tokens[0])) {
                this.isDurable = Boolean.parseBoolean(tokens[1]);
            } else if ("lastPurgedTimestamp".equals(tokens[0])) {
                this.lastPurgedTimestamp = Long.parseLong(tokens[1]);
            } else if ("protocolType".equals(tokens[0])) {
                this.protocolType = new ProtocolType(tokens[1]);
            } else if ("destinationType".equals(tokens[0])) {
                this.destinationType = DestinationType.valueOf(tokens[1]);
            }
        }
    }

    public String toString() {
        return "[" + queueName + "] " +
                "OW=" + queueOwner +
                "/X=" + isExclusive +
                "/D" + isDurable +
                "/LPT" + lastPurgedTimestamp;
    }

    public String encodeAsString() {
        return "queueName=" + queueName +
                ",queueOwner=" + queueOwner +
                ",isExclusive=" + isExclusive +
                ",isDurable=" + isDurable +
                ",lastPurgedTimestamp=" + lastPurgedTimestamp +
                ",protocolType=" + protocolType +
                ",destinationType=" + destinationType.name();
    }

    public boolean equals(Object o) {
        if (o instanceof AndesQueue) {
            AndesQueue c = (AndesQueue) o;
            if (this.queueName.equals(c.queueName)) {
                return true;
            }
        }
        return false;
    }

    public int hashCode() {
        return new HashCodeBuilder(17, 31)
                .append(queueName)
                .append(protocolType)
                .append(destinationType)
                .toHashCode();
    }

    public ProtocolType getProtocolType() {
        return protocolType;
    }

    public void setProtocolType(ProtocolType protocolType) {
        this.protocolType = protocolType;
    }

    public DestinationType getDestinationType() {
        return destinationType;
    }

    public void setDestinationType(DestinationType destinationType) {
        this.destinationType = destinationType;
    }
}
