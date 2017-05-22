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

import org.wso2.carbon.business.messaging.admin.services.beans.BrokerManagementBeans;
import org.wso2.carbon.business.messaging.admin.services.exceptions.BrokerManagerException;
import org.wso2.carbon.business.messaging.admin.services.managers.BrokerManagerService;
import org.wso2.carbon.business.messaging.admin.services.types.BrokerInformation;
import org.wso2.carbon.business.messaging.admin.services.types.ClusterInformation;
import org.wso2.carbon.business.messaging.admin.services.types.Hello;
import org.wso2.carbon.business.messaging.admin.services.types.Protocols;
import org.wso2.carbon.business.messaging.admin.services.types.StoreInformation;

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
    public Protocols getSupportedProtocols() throws BrokerManagerException {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ClusterInformation getClusterInformation() throws BrokerManagerException {
        return null;
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

    @Override
    public Hello sayHello() throws BrokerManagerException {
        return null;
    }
}
