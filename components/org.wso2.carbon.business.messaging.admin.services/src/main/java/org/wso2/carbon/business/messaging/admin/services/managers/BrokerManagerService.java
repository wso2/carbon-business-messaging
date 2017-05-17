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

package org.wso2.carbon.business.messaging.admin.services.managers;

import org.wso2.carbon.business.messaging.admin.services.exceptions.BrokerManagerException;
import org.wso2.carbon.business.messaging.admin.services.types.BrokerInformation;
import org.wso2.carbon.business.messaging.admin.services.types.ClusterInformation;
import org.wso2.carbon.business.messaging.admin.services.types.Hello;
import org.wso2.carbon.business.messaging.admin.services.types.Protocols;
import org.wso2.carbon.business.messaging.admin.services.types.StoreInformation;

/**
 * This interface provides the base for managing all broker information related services.
 */
public interface BrokerManagerService {

    /**
     * Gets the supported protocol types by the broker.
     *
     * @return A Protocol instance.
     */
    Protocols getSupportedProtocols() throws BrokerManagerException;

    /**
     * Gets information regarding clustering of the broker.
     *
     * @return Clustering related information.
     * @throws BrokerManagerException
     */
    ClusterInformation getClusterInformation() throws BrokerManagerException;

    /**
     * Gets information regarding message store of the broker.
     *
     * @return Message store related information.
     * @throws BrokerManagerException
     */
    StoreInformation getStoreInformation() throws BrokerManagerException;

    /**
     * Gets information regarding broker configuration of the broker.
     *
     * @return Broker configuration related information.
     * @throws BrokerManagerException
     */
    BrokerInformation getBrokerInformation() throws BrokerManagerException;

    /**
     * Say hello to the broker
     * This is a temp implementation to test the messaging core functionality
     *
     * @return Broker configuration related information.
     * @throws BrokerManagerException
     */
    Hello sayHello() throws BrokerManagerException;
}
