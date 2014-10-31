/*
*  Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/

package org.wso2.carbon.stat.publisher;

import org.apache.log4j.Logger;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.stat.publisher.conf.StatPublisherConfiguration;
import org.wso2.carbon.stat.publisher.exception.StatPublisherConfigurationException;
import org.wso2.carbon.stat.publisher.internal.ds.StatPublisherValueHolder;
import org.wso2.carbon.stat.publisher.internal.publisher.StatPublisherManager;
import org.wso2.carbon.stat.publisher.internal.util.RegistryPersistenceManager;


public class StatPublisherService {

    private static Logger logger = Logger.getLogger(StatPublisherService.class);

    /**
     * StatPublisherConfiguration details get method
     *
     * @return StatPublisherConfiguration
     * @throws StatPublisherConfigurationException
     */
    public StatPublisherConfiguration getStatConfiguration() throws StatPublisherConfigurationException {
        //get tenant ID
        int tenantID = CarbonContext.getThreadLocalCarbonContext().getTenantId();

        return RegistryPersistenceManager.loadConfigurationData(tenantID);
    }

    /**
     * StatConfiguration details set method
     *
     * @param statPublisherConfiguration UI configuration of statPublisher configurations
     * @throws StatPublisherConfigurationException
     */
    public void setStatConfiguration(StatPublisherConfiguration statPublisherConfiguration) throws StatPublisherConfigurationException {
        //get tenant ID
        int tenantID = CarbonContext.getThreadLocalCarbonContext().getTenantId();

        StatPublisherManager statPublisherManager = StatPublisherValueHolder.getStatPublisherManager();

        try {
            RegistryPersistenceManager.storeConfigurationData(statPublisherConfiguration, tenantID);
        } catch (StatPublisherConfigurationException e) {
            logger.error("Error occurs while trying to store configurations values to registry", e);
        }
        //update stat publisher observer
        statPublisherManager.updateStatPublisherObserver(tenantID);
    }


}
