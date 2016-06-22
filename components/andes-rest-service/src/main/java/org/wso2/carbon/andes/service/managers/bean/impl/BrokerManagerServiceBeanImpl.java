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

package org.wso2.carbon.andes.service.managers.bean.impl;

import org.wso2.carbon.andes.service.beans.BrokerManagementBeans;
import org.wso2.carbon.andes.service.exceptions.BrokerManagerException;
import org.wso2.carbon.andes.service.managers.BrokerManagerService;
import org.wso2.carbon.andes.service.types.BrokerInformation;
import org.wso2.carbon.andes.service.types.ClusterInformation;
import org.wso2.carbon.andes.service.types.NodeInformation;
import org.wso2.carbon.andes.service.types.StoreInformation;

import java.util.ArrayList;
import java.util.List;

/**
 * This implementation provides the base for managing all messages related services through JMX.
 */
public class BrokerManagerServiceBeanImpl implements BrokerManagerService {
    
    private BrokerManagementBeans brokerManagementBeans;
    
    public BrokerManagerServiceBeanImpl() {
        brokerManagementBeans = new BrokerManagementBeans();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getSupportedProtocols() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ClusterInformation getClusterInformation() throws BrokerManagerException {
        ClusterInformation clusterInformation = new ClusterInformation();
        clusterInformation.setClusteringEnabled(brokerManagementBeans.isClusteringEnabled());
        clusterInformation.setNodeID(brokerManagementBeans.getMyNodeID());
        clusterInformation.setCoordinatorAddress(brokerManagementBeans.getCoordinatorNodeAddress());
        List<String> allClusterNodeAddresses = brokerManagementBeans.getAllClusterNodeAddresses();
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
        storeInformation.setHealthy(brokerManagementBeans.getStoreHealth());
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
