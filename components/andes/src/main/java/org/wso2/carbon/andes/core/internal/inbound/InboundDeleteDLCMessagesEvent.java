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

package org.wso2.carbon.andes.core.internal.inbound;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.andes.core.AndesException;
import org.wso2.carbon.andes.core.AndesMessageMetadata;
import org.wso2.carbon.andes.core.MessagingEngine;

import java.util.List;

/**
 * Class to hold information about deleting messages from dead letter channel event
 */
public class InboundDeleteDLCMessagesEvent implements AndesInboundStateEvent {
    private static Log log = LogFactory.getLog(InboundDeleteMessagesEvent.class);

    /**
     * Supported state events
     */
    private enum EventType {

        /**
         * Delete messages event related event type
         */
        DELETE_DLC_MESSAGES_EVENT,
    }

    /**
     * Type of this event
     */
    private EventType eventType;

    /**
     * List of messages to remove
     */
    private List<AndesMessageMetadata> andesMessageMetadataList;

    /**
     * Reference to MessagingEngine for message deletion
     */
    private MessagingEngine messagingEngine;

    /**
     * Delete messages in dead letter channel.
     *
     * @param messagesToRemove List<AndesMessageMetadata>
     */
    public InboundDeleteDLCMessagesEvent(List<AndesMessageMetadata> messagesToRemove) {
        this.andesMessageMetadataList = messagesToRemove;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateState() throws AndesException {
        switch (eventType) {
            case DELETE_DLC_MESSAGES_EVENT:
                messagingEngine.deleteDLCMessages(andesMessageMetadataList);
                break;
            default:
                log.error("Event type not set properly " + eventType);
                break;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String eventInfo() {
        return eventType.toString();
    }

    /**
     * Prepare to update Andes state with a delete messages event
     *
     * @param messagingEngine MessagingEngine to be used for this event
     */
    public void prepareForDelete(MessagingEngine messagingEngine) {
        eventType = EventType.DELETE_DLC_MESSAGES_EVENT;
        this.messagingEngine = messagingEngine;
    }
}
