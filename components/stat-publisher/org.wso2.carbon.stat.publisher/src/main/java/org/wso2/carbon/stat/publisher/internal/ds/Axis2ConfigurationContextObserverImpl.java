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

package org.wso2.carbon.stat.publisher.internal.ds;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.log4j.Logger;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.stat.publisher.exception.StatPublisherConfigurationException;
import org.wso2.carbon.stat.publisher.exception.StatPublisherRuntimeException;
import org.wso2.carbon.stat.publisher.internal.publisher.StatPublisherManager;
import org.wso2.carbon.utils.Axis2ConfigurationContextObserver;

/**
 * This class use get notification about creating and terminating  carbon context for particular tenant
 * This class implemented Axis2ConfigurationContextObserver from carbon utils
 */
public class Axis2ConfigurationContextObserverImpl implements Axis2ConfigurationContextObserver {

    private static final Logger logger = Logger.getLogger(Axis2ConfigurationContextObserverImpl.class);

    /**
     * This will triggered before tenant create carbon context.
     */

    @Override
    public void creatingConfigurationContext(int i) {
    }

    /**
     * This will triggered after tenant create carbon context.
     *
     * @param configurationContext -configurationContext for specific tenant
     */
    @Override
    public void createdConfigurationContext(ConfigurationContext configurationContext) {
        PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
        int tenantID = carbonContext.getTenantId();
        try {
            StatPublisherManager statPublisherManager = new StatPublisherManager();
            statPublisherManager.createStatPublisherObserver(tenantID);
        } catch (StatPublisherConfigurationException e) {
            logger.error("Exception in initializing StatPublisherManager ", e);
            throw new StatPublisherRuntimeException(e);
        }
    }

    /**
     * This will triggered before tenant terminate it's carbon context (tenant clean up)
     */
    @Override
    public void terminatingConfigurationContext(ConfigurationContext configurationContext) {
    }

    /**
     * This will triggered after tenant terminate it's carbon context (tenant clean up)
     * Using this Observer instance for specific tenant will remove from observer list in StatPublisherManager
     *
     * @param configurationContext-configurationContext for specific tenant
     */
    @Override
    public void terminatedConfigurationContext(ConfigurationContext configurationContext) {
        PrivilegedCarbonContext carbonContext = PrivilegedCarbonContext.getThreadLocalCarbonContext();
        int tenantID = carbonContext.getTenantId();
        try {
            StatPublisherManager statPublisherManager = new StatPublisherManager();
            statPublisherManager.removeStatPublisherObserver(tenantID);

        } catch (StatPublisherConfigurationException e) {
            logger.error("Exception in removing StatPublisherManager ", e);
            throw new StatPublisherRuntimeException(e);
        }
    }
}