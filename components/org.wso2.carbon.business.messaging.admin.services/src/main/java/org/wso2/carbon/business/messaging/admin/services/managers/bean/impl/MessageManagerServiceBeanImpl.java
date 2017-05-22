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

package org.wso2.carbon.business.messaging.admin.services.managers.bean.impl;

import org.wso2.carbon.business.messaging.admin.services.beans.MessageManagementBeans;
import org.wso2.carbon.business.messaging.admin.services.exceptions.MessageManagerException;
import org.wso2.carbon.business.messaging.admin.services.managers.MessageManagerService;
import org.wso2.carbon.business.messaging.admin.services.types.Message;

import java.util.List;

/**
 * This interface provides the base for managing all messages related services through JMX.
 */
public class MessageManagerServiceBeanImpl implements MessageManagerService {

    private MessageManagementBeans messageManagementBeans;
    
    public MessageManagerServiceBeanImpl() {
        messageManagementBeans = new MessageManagementBeans();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public List<Message> getMessagesOfDestinationByMessageID(String protocol, String destinationType, String
            destinationName, boolean content, long nextMessageID, int limit) throws MessageManagerException {
        return messageManagementBeans.getMessagesOfDestinationByMessageID(protocol, destinationType,
                                                                        destinationName, content, nextMessageID, limit);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Message> getMessagesOfDestinationByOffset(String protocol, String destinationType, String
            destinationName, boolean content, int offset, int limit) throws MessageManagerException {
        return messageManagementBeans.getMessagesOfDestinationByOffset(protocol, destinationType,
                                                                            destinationType, content, offset, limit);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Message getMessage(String protocol, String destinationType, String destinationName, long andesMessageID,
                              boolean content) throws MessageManagerException {
        return messageManagementBeans.getMessage(protocol, destinationType, destinationName, andesMessageID, content);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteMessages(String protocol, String destinationType, String destinationName)
                                                                                        throws MessageManagerException {
        messageManagementBeans.deleteMessages(protocol, destinationType, destinationName);
    }
}
