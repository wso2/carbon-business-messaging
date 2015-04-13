/*
* Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
* WSO2 Inc. licenses this file to you under the Apache License,
* Version 2.0 (the "License"); you may not use this file except
* in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied. See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.carbon.stat.publisher.internal.ds;

import org.apache.log4j.Logger;
import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.wso2.andes.kernel.MessagingEngine;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.stat.publisher.exception.StatPublisherConfigurationException;
import org.wso2.carbon.stat.publisher.internal.publisher.StatPublisherManager;
import org.wso2.carbon.stat.publisher.internal.publisher.StatPublisherMessageListenerImpl;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.Axis2ConfigurationContextObserver;
import org.wso2.carbon.utils.ConfigurationContextService;

/**
 * @scr.component name="org.wso2.carbon.stat.publisher" immediate="true"
 * @scr.reference name="configurationContext.service"
 * interface="org.wso2.carbon.utils.ConfigurationContextService" cardinality="1..1"
 * policy="dynamic" bind="setConfigurationContextService" unbind="unsetConfigurationContextService"
 * @scr.reference name="org.wso2.carbon.registry.service"
 * interface="org.wso2.carbon.registry.core.service.RegistryService" cardinality="1..1"
 * policy="dynamic" bind="setRegistryService" unbind="unsetRegistryService"
 * @scr.reference name="realm.service" interface="org.wso2.carbon.user.core.service.RealmService"
 * cardinality="1..1" policy="dynamic" bind="setRealmService" unbind="unsetRealmService"
 */
public class StatPublisherDS {

    private static final Logger logger = Logger.getLogger(StatPublisherDS.class);

    /**
     * Activate method in stat publisher bundle
     *
     * @param context - Component context
     * @throws StatPublisherConfigurationException
     */
    protected void activate(ComponentContext context)
            throws StatPublisherConfigurationException, UserStoreException {
        //initialize Stat Publisher manager instance
        StatPublisherManager statPublisherManager = new StatPublisherManager();
        // Store Stat Publisher manager instance in StatPublisherValueHolder
        StatPublisherValueHolder.setStatPublisherManager(statPublisherManager);
        //create StatPublisherObserver for super tenant
        StatPublisherValueHolder.getStatPublisherManager().
                createStatPublisherObserver(CarbonContext.getThreadLocalCarbonContext().getTenantId());

           //set interface in andes StatPublisher to MessageStatPublisher for get messages and Ack messages
           MessagingEngine.getInstance().setStatPublisherMessageListener(StatPublisherMessageListenerImpl.getInstance());

              //get Axis2ConfigurationContext services to get notification about changing configuration contexts
        BundleContext bundleContext = context.getBundleContext();
        //register in Axis2ConfigurationContextObserver for get notification  out carbon context changes
        bundleContext.registerService(Axis2ConfigurationContextObserver.class.getName(),
                new Axis2ConfigurationContextObserverImpl(), null);
        logger.info("Statistics Publisher Bundle Successfully Activated");
    }

    /**
     * Deactivate method in stat publisher component
     *
     * @param context - Component context
     */
    protected void deactivate(ComponentContext context) {

    }

    /**
     * Set ConfigurationContextService
     *
     * @param configurationContextService - ConfigurationContextService
     */
    protected void setConfigurationContextService(
            ConfigurationContextService configurationContextService) {
        StatPublisherValueHolder.setConfigurationContextService(configurationContextService);
    }

    /**
     * Remove ConfigurationContextService
     *
     * @param configurationContextService -ConfigurationContextService
     */
    protected void unsetConfigurationContextService(
            ConfigurationContextService configurationContextService) {
        StatPublisherValueHolder.setConfigurationContextService(null);
    }

    /**
     * Set RegistryService
     *
     * @param registryService - RegistryService
     */
    protected void setRegistryService(RegistryService registryService) {
        StatPublisherValueHolder.setRegistryService(registryService);
    }

    /**
     * Remove RegistryService
     *
     * @param registryService -RegistryService
     */
    protected void unsetRegistryService(RegistryService registryService) {
        StatPublisherValueHolder.setRegistryService(null);
    }

    /**
     * Set RealmService
     *
     * @param realmService - RealmService
     */
    protected void setRealmService(RealmService realmService) {
        StatPublisherValueHolder.setRealmService(realmService);
    }

    /**
     * Remove RealmService
     *
     * @param realmService - RealmService
     */
    protected void unsetRealmService(RealmService realmService) {
        StatPublisherValueHolder.setRealmService(null);
    }
}