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
import org.wso2.carbon.andes.core.ProtocolType;
import org.wso2.carbon.andes.core.management.mbeans.BrokerManagementInformationMXBean;

import java.util.List;
import java.util.Set;

/**
 * The the JMS MBean that expose cluster management information exposes the Cluster Management related information using
 * MBeans.
 */
public class BrokerManagementInformationImpl implements BrokerManagementInformationMXBean {

    /**
     * Andes instance for getting broker related information.
     */
    private Andes andesInstance;

    /**
     * Initializes broker managing bean.
     */
    public BrokerManagementInformationImpl() {
        andesInstance = Andes.getInstance();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<ProtocolType> getSupportedProtocols() {
        return andesInstance.getSupportedProtocols();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isClusteringEnabled() {
        return andesInstance.isClusteringEnabled();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getMyNodeID() {
        return andesInstance.getLocalNodeID();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCoordinatorNodeAddress() {
        return andesInstance.getCoordinatorNodeAddress();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getAllClusterNodeAddresses() {
        return andesInstance.getAllClusterNodeAddresses();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getStoreHealth() {
        return andesInstance.getStoreHealth();
    }
}
