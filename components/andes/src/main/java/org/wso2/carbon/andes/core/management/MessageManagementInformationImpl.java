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

package org.wso2.carbon.andes.core.management;

import org.wso2.carbon.andes.core.Andes;
import org.wso2.carbon.andes.core.AndesException;
import org.wso2.carbon.andes.core.AndesMessage;
import org.wso2.carbon.andes.core.DestinationType;
import org.wso2.carbon.andes.core.ProtocolType;
import org.wso2.carbon.andes.core.management.mbeans.MessageManagementInformationMXBean;
import org.wso2.carbon.andes.core.resource.manager.AndesResourceManager;
import org.wso2.carbon.andes.core.util.CompositeDataHelper;

import java.util.ArrayList;
import java.util.List;
import javax.management.MBeanException;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.OpenDataException;

/**
 * MBeans for managing resources related to messages.
 */
public class MessageManagementInformationImpl implements MessageManagementInformationMXBean {

    /**
     * Andes resource manager instance.
     */
    AndesResourceManager andesResourceManager;

    /**
     * Helper class for converting a message for {@link CompositeData}.
     */
    CompositeDataHelper.MessagesCompositeDataHelper messagesCompositeDataHelper;

    /**
     * Initializes the composite data helper.
     */
    public MessageManagementInformationImpl() {
        messagesCompositeDataHelper = new CompositeDataHelper().new MessagesCompositeDataHelper();
        andesResourceManager = Andes.getInstance().getAndesResourceManager();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompositeData[] browseDestinationWithMessageID(String protocolTypeAsString, String
            destinationTypeAsString, String destinationName, boolean getContentFlag, long nextMessageID, int limit)
            throws MBeanException {
        List<CompositeData> compositeDataList = new ArrayList<>();

        try {
            ProtocolType protocolType = new ProtocolType(protocolTypeAsString);
            DestinationType destinationType = DestinationType.valueOf(destinationTypeAsString.toUpperCase());

            List<AndesMessage> andesMessages = andesResourceManager.browseDestinationWithMessageID(protocolType,
                                                                                                   destinationType,
                                                                                                   destinationName,
                                                                                                   getContentFlag,
                                                                                                   nextMessageID,
                                                                                                   limit);

            for (AndesMessage andesMessage : andesMessages) {
                compositeDataList.add(messagesCompositeDataHelper.getMessageAsCompositeData(protocolType,
                                                                                            andesMessage,
                                                                                            getContentFlag));
            }

        } catch (AndesException | OpenDataException e) {
            throw new MBeanException(e, "Error occurred in browsing destination.");
        }
        return compositeDataList.toArray(new CompositeData[compositeDataList.size()]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompositeData[] browseDestinationWithOffset(String protocolTypeAsString, String destinationTypeAsString,
                                                       String destinationName, boolean getContentFlag, int offset,
                                                       int limit) throws MBeanException {
        List<CompositeData> compositeDataList = new ArrayList<>();

        try {
            ProtocolType protocolType = new ProtocolType(protocolTypeAsString);
            DestinationType destinationType = DestinationType.valueOf(destinationTypeAsString.toUpperCase());

            List<AndesMessage> andesMessages = andesResourceManager.browseDestinationWithOffset(protocolType,
                                                                                                destinationType,
                                                                                                destinationName,
                                                                                                getContentFlag, offset,
                                                                                                limit);

            for (AndesMessage andesMessage : andesMessages) {
                compositeDataList.add(messagesCompositeDataHelper.getMessageAsCompositeData(protocolType,
                                                                                            andesMessage,
                                                                                            getContentFlag));
            }

        } catch (AndesException | OpenDataException e) {
            throw new MBeanException(e, "Error occurred in browsing destination.");
        }
        return compositeDataList.toArray(new CompositeData[compositeDataList.size()]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompositeData getMessage(String protocolTypeAsString, String destinationTypeAsString, String
            destinationName, long andesMessageID, boolean getContentFlag) throws MBeanException {
        CompositeData message;
        try {
            ProtocolType protocolType = new ProtocolType(protocolTypeAsString);
            DestinationType destinationType = DestinationType.valueOf(destinationTypeAsString.toUpperCase());

            AndesMessage andesMessages = andesResourceManager.getMessage(protocolType, destinationType,
                                                                         destinationName, andesMessageID,
                                                                         getContentFlag);

            message = messagesCompositeDataHelper.getMessageAsCompositeData(protocolType, andesMessages,
                                                                            getContentFlag);

        } catch (AndesException | OpenDataException e) {
            throw new MBeanException(e, "Error occurred in browse queue.");
        }
        return message;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteMessages(String protocolTypeAsString, String destinationTypeAsString, String destinationName)
            throws MBeanException {

        try {
            ProtocolType protocolType = new ProtocolType(protocolTypeAsString);
            DestinationType destinationType = DestinationType.valueOf(destinationTypeAsString.toUpperCase());

            andesResourceManager.deleteMessages(protocolType, destinationType, destinationName);
        } catch (AndesException e) {
            throw new MBeanException(e, "Error occurred in browse queue.");
        }
    }
}
