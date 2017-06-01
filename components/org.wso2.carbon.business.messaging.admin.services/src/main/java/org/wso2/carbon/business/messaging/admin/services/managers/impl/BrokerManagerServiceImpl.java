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

package org.wso2.carbon.business.messaging.admin.services.managers.impl;

import org.wso2.andes.kernel.Andes;
import org.wso2.andes.kernel.ProtocolType;
import org.wso2.carbon.business.messaging.admin.services.exceptions.InternalServerException;
import org.wso2.carbon.business.messaging.admin.services.internal.MbRestServiceDataHolder;
import org.wso2.carbon.business.messaging.admin.services.managers.BrokerManagerService;
import org.wso2.carbon.business.messaging.admin.services.types.BrokerInformation;
import org.wso2.carbon.business.messaging.admin.services.types.ClusterInformation;
import org.wso2.carbon.business.messaging.admin.services.types.Protocols;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Class to handle all the broker related information queries using APIs of Andes messaging core.
 */
public class BrokerManagerServiceImpl implements BrokerManagerService {
    /**
     * Registered andes core instance through OSGi.
     */
    private Andes andesCore;

    public BrokerManagerServiceImpl() {
        andesCore = MbRestServiceDataHolder.getInstance().getAndesCore();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Protocols getSupportedProtocols() throws InternalServerException {
        Set<ProtocolType> protocolTypeSet = andesCore.getSupportedProtocols();
        Protocols protocols = new Protocols();
        protocols.setProtocol(
                protocolTypeSet.stream().map(protocolType -> protocolType.toString()).collect(Collectors.toList()));
        return protocols;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ClusterInformation getClusterInformation() throws InternalServerException {
        ClusterInformation clusterInformation = new ClusterInformation();
        clusterInformation.setIsClusteringEnabled(andesCore.isClusteringEnabled());
        return clusterInformation;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BrokerInformation getBrokerInformation() throws InternalServerException {
        BrokerInformation brokerInformation = new BrokerInformation();
        brokerInformation.setProperties(andesCore.getBrokerDetails());
        return brokerInformation;
    }
}
