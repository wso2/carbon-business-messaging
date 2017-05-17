/*
 * Copyright (c) 2017, WSO2 Inc. (http://wso2.com) All Rights Reserved.
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

package org.wso2.carbon.business.messaging.admin.services.types;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.Map;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * * This class represent a message information object.
 */
@ApiModel(value = "Message",
          description = "The structure that represents a single message.")
@XmlRootElement
public class Message {
    @ApiModelProperty(value = "The unique message ID used in the broker.")
    private Long andesMsgMetadataId;
    @ApiModelProperty(value = "The destination to which the message belongs to.")
    private String destination;
    @ApiModelProperty(value = "Properties of the message. These properties should align with the protocol type of "
            + "the message.")
    private Map<String, String> messageProperties;
    @ApiModelProperty(value = "The content of the message.")
    private String messageContent;

    public Long getAndesMsgMetadataId() {
        return andesMsgMetadataId;
    }

    public void setAndesMsgMetadataId(Long andesMsgMetadataId) {
        this.andesMsgMetadataId = andesMsgMetadataId;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public Map<String, String> getMessageProperties() {
        return messageProperties;
    }

    public void setMessageProperties(Map<String, String> messageProperties) {
        this.messageProperties = messageProperties;
    }

    public String getMessageContent() {
        return messageContent;
    }

    public void setMessageContent(String messageContent) {
        this.messageContent = messageContent;
    }
}
