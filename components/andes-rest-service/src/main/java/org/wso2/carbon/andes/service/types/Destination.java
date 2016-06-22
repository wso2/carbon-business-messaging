/*
 * Copyright (c) 2016, WSO2 Inc. (http://wso2.com) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.andes.service.types;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import org.wso2.carbon.andes.core.AndesException;
import org.wso2.carbon.andes.core.DestinationType;
import org.wso2.carbon.andes.core.ProtocolType;

import java.util.Date;

/**
 * This class represent a destination information object.
 */
@ApiModel(value = "Destination", description = "A destination representation which message can be pub/sub.")
public class Destination {
    @ApiModelProperty(value = "ID of the destination.")
    private long id = 0;
    @ApiModelProperty(value = "Name of the destination.", required = true)
    private String destinationName = null;
    @ApiModelProperty(value = "The created date of the destination.")
    private Date createdDate = null;
    @ApiModelProperty(value = "The type of the destination.")
    private DestinationType destinationType = null;
    @ApiModelProperty(value = "The type of the protocol.")
    private ProtocolType protocol = null;
    @ApiModelProperty(value = "The message count for the destination.")
    private long messageCount = 0;
    @ApiModelProperty(value = "Whether the destination is durable.")
    private boolean isDurable = false;
    @ApiModelProperty(value = "The owner's username.")
    private String owner = null;
    @ApiModelProperty(value = "The subscription count for the destination.")
    private int subscriptionCount;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getDestinationName() {
        return destinationName;
    }

    public void setDestinationName(String destinationName) {
        this.destinationName = destinationName;
    }

    public Date getCreatedDate() {
        return (Date) createdDate.clone();
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = (Date) createdDate.clone();
    }

    public DestinationType getDestinationType() {
        return destinationType;
    }

    public void setDestinationType(DestinationType destinationType) {
        this.destinationType = destinationType;
    }

    public ProtocolType getProtocol() {
        return protocol;
    }

    public void setProtocol(ProtocolType protocol) {
        this.protocol = protocol;
    }

    public void setProtocol(String protocolAsString) throws AndesException {
        this.protocol = new ProtocolType(protocolAsString);
    }

    public long getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(long messageCount) {
        this.messageCount = messageCount;
    }

    public boolean isDurable() {
        return isDurable;
    }

    public void setDurable(boolean durable) {
        isDurable = durable;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public int getSubscriptionCount() {
        return subscriptionCount;
    }

    public void setSubscriptionCount(int subscriptionCount) {
        this.subscriptionCount = subscriptionCount;
    }
}
