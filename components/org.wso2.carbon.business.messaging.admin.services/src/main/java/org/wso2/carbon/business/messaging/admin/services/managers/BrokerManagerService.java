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

import org.wso2.carbon.business.messaging.admin.services.exceptions.InternalServerException;
import org.wso2.carbon.business.messaging.admin.services.types.BrokerInformation;
import org.wso2.carbon.business.messaging.admin.services.types.ClusterInformation;
import org.wso2.carbon.business.messaging.admin.services.types.Protocols;

/**
 * This interface provides the base for managing all broker information related services.
 */
public interface BrokerManagerService {

    /**
     * Gets the supported protocol types by the broker.
     *
     * @return A Protocol instance.
     * @throws InternalServerException Error in retrieving broker information
     */
    Protocols getSupportedProtocols() throws InternalServerException;

    /**
     * Gets information regarding clustering of the broker.
     *
     * @return Clustering related information.
     * @throws InternalServerException Error in retrieving broker information
     */
    ClusterInformation getClusterInformation() throws InternalServerException;

    /**
     * Gets information regarding broker configuration of the broker.
     *
     * @return Broker configuration related information.
     * @throws InternalServerException Error in retrieving broker information
     */
    BrokerInformation getBrokerInformation() throws InternalServerException;
}
