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
import org.wso2.carbon.andes.core.DeliverableAndesMetadata;
import org.wso2.carbon.andes.core.MessagingEngine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Class to hold information about deleting messages event
 */
public class InboundDeleteMessagesEvent implements AndesInboundStateEvent {

    private static Log log = LogFactory.getLog(InboundDeleteMessagesEvent.class);

    /**
     * Supported state events
     */
    private enum EventType {

        /**
         * Delete messages event related event type
         */
        DELETE_MESSAGES_EVENT,

    }

    /**
     * Type of this event
     */
    private EventType eventType;

    /**
     * List of deliverable messages to remove
     */
    private List<DeliverableAndesMetadata> deliverableAndesMetadataList;

    /**
     * List of messages to remove
     */
    private Collection<AndesMessageMetadata> andesMessageMetadataList;

    /**
     * Whether to move deleted messages to DLC or not.
     * True if need to move to DLC and vice versa
     */
    private boolean moveToDLC;

    /**
     * Reference to MessagingEngine for message deletion
     */
    private MessagingEngine messagingEngine;

    /**
     * Delete messages in queues with option move to DLC
     *
     * @param messagesToRemove List<AndesRemovableMetadata>
     * @param moveToDLC        whether move deleted messages to DLC
     */
    public InboundDeleteMessagesEvent(List<DeliverableAndesMetadata> messagesToRemove, boolean moveToDLC) {
        this.deliverableAndesMetadataList = messagesToRemove;
        this.andesMessageMetadataList = new ArrayList<>(0);
        this.moveToDLC = moveToDLC;
    }

    /**
     * Delete messages in queues with option move to DLC
     *
     * @param messagesToRemove List<AndesRemovableMetadata>
     * @param moveToDLC        whether move deleted messages to DLC
     */
    public InboundDeleteMessagesEvent(Collection<AndesMessageMetadata> messagesToRemove, boolean moveToDLC) {
        this.deliverableAndesMetadataList = new ArrayList<>(0);
        this.andesMessageMetadataList = messagesToRemove;
        this.moveToDLC = moveToDLC;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateState() throws AndesException {
        switch (eventType) {
            case DELETE_MESSAGES_EVENT:
                if (!deliverableAndesMetadataList.isEmpty()) {
                    if (!moveToDLC) {
                        messagingEngine.deleteMessages(deliverableAndesMetadataList);
                    } else {
                        messagingEngine.moveMessageToDeadLetterChannel(deliverableAndesMetadataList);
                    }
                } else {
                    if (!moveToDLC) {
                        messagingEngine.deleteMessages(andesMessageMetadataList);
                    } else {
                        messagingEngine.moveMessageToDeadLetterChannel(andesMessageMetadataList);
                    }
                }
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
        eventType = EventType.DELETE_MESSAGES_EVENT;
        this.messagingEngine = messagingEngine;
    }
}
