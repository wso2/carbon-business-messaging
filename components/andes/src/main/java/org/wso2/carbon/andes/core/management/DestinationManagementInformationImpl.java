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
import org.wso2.carbon.andes.core.AndesQueue;
import org.wso2.carbon.andes.core.DestinationType;
import org.wso2.carbon.andes.core.ProtocolType;
import org.wso2.carbon.andes.core.management.mbeans.DestinationManagementInformationMXBean;
import org.wso2.carbon.andes.core.resource.manager.AndesResourceManager;
import org.wso2.carbon.andes.core.util.CompositeDataHelper;

import java.util.ArrayList;
import java.util.List;
import javax.management.JMException;
import javax.management.MBeanException;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.OpenDataException;

/**
 * MBeans for managing resources related to destinations such as queues and topics.
 */
public class DestinationManagementInformationImpl implements DestinationManagementInformationMXBean {

    /**
     * Andes resource manager instance.
     */
    private AndesResourceManager andesResourceManager;

    /**
     * Helper class for converting destinations for {@link CompositeData}.
     */
    private CompositeDataHelper.DestinationCompositeDataHelper destinationCompositeDataHelper;

    /**
     * Initializes the composite data helper and virtual host.
     *
     * @throws MBeanException
     */
    public DestinationManagementInformationImpl() throws MBeanException {
        andesResourceManager = Andes.getInstance().getAndesResourceManager();
        destinationCompositeDataHelper = new CompositeDataHelper().new DestinationCompositeDataHelper();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompositeData[] getDestinations(String protocolTypeAsString, String destinationTypeAsString, String
            keyword, int offset, int limit) throws MBeanException {
        List<CompositeData> destinationCompositeList = new ArrayList<>();
        try {
            ProtocolType protocolType = new ProtocolType(protocolTypeAsString);
            DestinationType destinationType = DestinationType.valueOf(destinationTypeAsString.toUpperCase());

            List<AndesQueue> destinations = andesResourceManager.getDestinations(protocolType, destinationType,
                                                                                 keyword, offset, limit);

            for (AndesQueue destination : destinations) {

                long messageCountOfQueue = Andes.getInstance().getMessageCountOfQueue(destination.queueName);
                CompositeDataSupport support = destinationCompositeDataHelper.getDestinationAsCompositeData
                        (destination, messageCountOfQueue);
                destinationCompositeList.add(support);
            }

        } catch (AndesException | OpenDataException e) {
            throw new MBeanException(e, "Error occurred when getting destinations.");
        }
        return destinationCompositeList.toArray(new CompositeData[destinationCompositeList.size()]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteDestinations(String protocolTypeAsString, String destinationTypeAsString) throws MBeanException {
        try {
            ProtocolType protocolType = new ProtocolType(protocolTypeAsString);
            DestinationType destinationType = DestinationType.valueOf(destinationTypeAsString.toUpperCase());
            andesResourceManager.deleteDestinations(protocolType, destinationType);
        } catch (AndesException e) {
            throw new MBeanException(e, "Error occurred in deleting destinations.");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompositeData getDestination(String protocolTypeAsString, String destinationTypeAsString, String
            destinationName) throws MBeanException {
        try {
            ProtocolType protocolType = new ProtocolType(protocolTypeAsString);
            DestinationType destinationType = DestinationType.valueOf(destinationTypeAsString.toUpperCase());
            AndesQueue destination = andesResourceManager.getDestination(protocolType, destinationType,
                                                                         destinationName);
            if (null != destination) {
                long messageCountOfQueue = Andes.getInstance().getMessageCountOfQueue(destination.queueName);
                return destinationCompositeDataHelper.getDestinationAsCompositeData(destination, messageCountOfQueue);
            } else {
                return null;
            }
        } catch (AndesException | JMException e) {
            throw new MBeanException(e, "Error occurred in getting destination.");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CompositeData createDestination(String protocolTypeAsString, String destinationTypeAsString, String
            destinationName, String currentUsername) throws MBeanException {
        CompositeData newDestination;
        try {
            ProtocolType protocolType = new ProtocolType(protocolTypeAsString);
            DestinationType destinationType = DestinationType.valueOf(destinationTypeAsString.toUpperCase());
            AndesQueue destination = andesResourceManager.createDestination(protocolType, destinationType,
                                                                            destinationName, currentUsername);

            long messageCountOfQueue = Andes.getInstance().getMessageCountOfQueue(destination.queueName);
            newDestination = destinationCompositeDataHelper.getDestinationAsCompositeData(destination,
                                                                                          messageCountOfQueue);
        } catch (AndesException | JMException e) {
            throw new MBeanException(e, "Error occurred in creating destination.");
        }
        return newDestination;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteDestination(String protocolTypeAsString, String destinationTypeAsString, String
            destinationName) throws MBeanException {
        try {
            ProtocolType protocolType = new ProtocolType(protocolTypeAsString);
            DestinationType destinationType = DestinationType.valueOf(destinationTypeAsString.toUpperCase());
            Andes.getInstance().getAndesResourceManager().deleteDestination(protocolType, destinationType,
                                                                            destinationName);
        } catch (AndesException e) {
            throw new MBeanException(e, "Error occurred in deleting destination.");
        }
    }
}
