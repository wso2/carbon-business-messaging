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

package org.wso2.carbon.andes.service.managers.osgi.impl;

import org.wso2.carbon.andes.core.AndesException;
import org.wso2.carbon.andes.core.AndesMessage;
import org.wso2.carbon.andes.core.DestinationType;
import org.wso2.carbon.andes.core.ProtocolType;
import org.wso2.carbon.andes.core.resource.manager.AndesResourceManager;
import org.wso2.carbon.andes.core.resource.manager.MessageDecoder;
import org.wso2.carbon.andes.service.exceptions.MessageManagerException;
import org.wso2.carbon.andes.service.internal.AndesRESTComponentDataHolder;
import org.wso2.carbon.andes.service.managers.MessageManagerService;
import org.wso2.carbon.andes.service.types.Message;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation for handling message related resource through OSGi.
 */
public class MessageManagerServiceOSGiImpl implements MessageManagerService {
    private AndesResourceManager andesResourceManager;
    public MessageManagerServiceOSGiImpl() {
        andesResourceManager = AndesRESTComponentDataHolder.getInstance().getAndesInstance().getAndesResourceManager();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Message> getMessagesOfDestinationByMessageID(String protocol, String destinationType, String
            destinationName, boolean content, long nextMessageID, int limit) throws MessageManagerException {
        List<Message> messages = new ArrayList<>();
        try {
            ProtocolType protocolType = new ProtocolType(protocol);
            DestinationType destinationTypeEnum = DestinationType.valueOf(destinationType);
            List<AndesMessage> andesMessages = andesResourceManager.browseDestinationWithMessageID(protocolType,
                    destinationTypeEnum, destinationName, content, nextMessageID, limit);
            for (AndesMessage andesMessage : andesMessages) {
                messages.add(getMessageFromAndesMessage(andesMessage,
                                                                andesResourceManager.getMessageDecoder(protocolType)));
            }

        } catch (AndesException e) {
            throw new MessageManagerException("Error occurred while browsing messages.", e);
        }

        return messages;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Message> getMessagesOfDestinationByOffset(String protocol, String destinationType, String
            destinationName, boolean content, int offset, int limit) throws MessageManagerException {
        List<Message> messages = new ArrayList<>();
        try {
            ProtocolType protocolType = new ProtocolType(protocol);
            DestinationType destinationTypeEnum = DestinationType.valueOf(destinationType);
            List<AndesMessage> andesMessages = andesResourceManager.browseDestinationWithOffset(protocolType,
                    destinationTypeEnum, destinationName, content, offset, limit);
            for (AndesMessage andesMessage : andesMessages) {
                messages.add(getMessageFromAndesMessage(andesMessage,
                                                                andesResourceManager.getMessageDecoder(protocolType)));
            }

        } catch (AndesException e) {
            throw new MessageManagerException("Error occurred while browsing messages.", e);
        }

        return messages;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Message getMessage(String protocol, String destinationType, String destinationName, long andesMessageID,
                              boolean content) throws MessageManagerException {
        Message message;
        try {
            ProtocolType protocolType = new ProtocolType(protocol);
            DestinationType destinationTypeEnum = DestinationType.valueOf(destinationType);
            AndesMessage andesMessage = andesResourceManager.getMessage(protocolType,
                    destinationTypeEnum, destinationName, andesMessageID, content);
            message = getMessageFromAndesMessage(andesMessage, andesResourceManager.getMessageDecoder(protocolType));
        } catch (AndesException e) {
            throw new MessageManagerException("Message could not be found with given andes message metadata ID : "
                                              + andesMessageID + " .", e);
        }

        return message;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteMessages(String protocol, String destinationType, String destinationName)
                                                                                        throws MessageManagerException {
        try {
            ProtocolType protocolType = new ProtocolType(protocol);
            DestinationType destinationTypeEnum = DestinationType.valueOf(destinationType);
            andesResourceManager.deleteMessages(protocolType, destinationTypeEnum, destinationName);
        } catch (AndesException e) {
            throw new MessageManagerException("Unable to delete/purge messages.", e);
        }
    }

    /**
     * Converts a {@link AndesMessage} to {@link Message}.
     *
     * @param andesMessage   The andes message object.
     * @param messageDecoder The message decoder.
     * @return Converted message.
     * @throws AndesException
     */
    private Message getMessageFromAndesMessage(AndesMessage andesMessage, MessageDecoder messageDecoder)
            throws AndesException {
        Message message = new Message();
        message.setAndesMsgMetadataId(andesMessage.getMetadata().getMessageId());
        message.setDestination(andesMessage.getMetadata().getDestination());
        message.setMessageProperties(messageDecoder.getMessageProperties(andesMessage.getMetadata()));
        message.setMessageContent(messageDecoder.getMessageContent(andesMessage));
        return message;
    }
}
