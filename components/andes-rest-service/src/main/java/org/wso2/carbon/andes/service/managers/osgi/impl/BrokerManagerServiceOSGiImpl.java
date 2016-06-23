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

import org.wso2.carbon.andes.core.Andes;
import org.wso2.carbon.andes.service.exceptions.BrokerManagerException;
import org.wso2.carbon.andes.service.internal.AndesRESTComponentDataHolder;
import org.wso2.carbon.andes.service.managers.BrokerManagerService;
import org.wso2.carbon.andes.service.types.BrokerInformation;
import org.wso2.carbon.andes.service.types.ClusterInformation;
import org.wso2.carbon.andes.service.types.NodeInformation;
import org.wso2.carbon.andes.service.types.StoreInformation;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation for getting broker information through OSGi.
 */
public class BrokerManagerServiceOSGiImpl implements BrokerManagerService {
    /**
     * Registered andes instance through OSGi.
     */
    private Andes andesInstance;
    public BrokerManagerServiceOSGiImpl() {
        andesInstance = AndesRESTComponentDataHolder.getInstance().getAndesInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getSupportedProtocols() throws BrokerManagerException {
        return andesInstance.getSupportedProtocols()
                .stream()
                .map(p -> p.toString().toLowerCase())
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ClusterInformation getClusterInformation() throws BrokerManagerException {
        ClusterInformation clusterInformation = new ClusterInformation();
        clusterInformation.setClusteringEnabled(andesInstance.isClusteringEnabled());
        clusterInformation.setNodeID(andesInstance.getLocalNodeID());
        clusterInformation.setCoordinatorAddress(andesInstance.getCoordinatorNodeAddress());
        List<String> allClusterNodeAddresses = andesInstance.getAllClusterNodeAddresses();
        List<NodeInformation> nodeInformationList = new ArrayList<>();
        for (String allClusterNodeAddress : allClusterNodeAddresses) {
            String[] nodeDetails = allClusterNodeAddress.split(",");
            NodeInformation nodeInformation = new NodeInformation();
            nodeInformation.setNodeID(nodeDetails[0]);
            nodeInformation.setHostname(nodeDetails[1]);
            nodeInformation.setPort(Integer.parseInt(nodeDetails[2]));
            if (clusterInformation.getCoordinatorAddress().equals(nodeDetails[1] + "," + nodeDetails[2])) {
                nodeInformation.setCoordinator(true);
            } else {
                nodeInformation.setCoordinator(false);
            }
            nodeInformationList.add(nodeInformation);
        }
        clusterInformation.setNodeAddresses(nodeInformationList);
        return clusterInformation;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StoreInformation getStoreInformation() throws BrokerManagerException {
        StoreInformation storeInformation = new StoreInformation();
        storeInformation.setHealthy(andesInstance.getStoreHealth());
        return storeInformation;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BrokerInformation getBrokerInformation() throws BrokerManagerException {
        return null;
    }
}
