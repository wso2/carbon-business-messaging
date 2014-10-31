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
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.stat.publisher.internal.publisher.StatPublisherManager;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.ConfigurationContextService;
/**
 * Keep service values
 */
public class StatPublisherValueHolder {
    private static RegistryService registryService;
    private static RealmService realmService;
    private static StatPublisherManager statPublisherManager;
    private static ConfigurationContextService configurationContextService;
    private StatPublisherValueHolder() {
    }
    /**
     * Set the configuration context of component
     * @param configurationContextService - configuration context of component
     */
    public static void setConfigurationContextService(
            ConfigurationContextService configurationContextService) {
        StatPublisherValueHolder.configurationContextService = configurationContextService;
    }
    /**
     * Get configurationContext
     * @return configurationContext
     */
    public static ConfigurationContextService getConfigurationContextService() {
        return configurationContextService;
    }
    /**
     * Get registry service
     * @return registryService
     */
    public static RegistryService getRegistryService() {
        return registryService;
    }
    /**
     * Initialize registry service
     * @param registryServiceParam - registry service
     */
    public static void setRegistryService(RegistryService registryServiceParam) {
        registryService = registryServiceParam;
    }
    /**
     * Get realmService
     * @return realmService
     */
    public static RealmService getRealmService() {
        return realmService;
    }
    /**
     * Initialize realm service
     * @param realmServiceParam - Realm Service
     */
    public static void setRealmService(RealmService realmServiceParam) {
        StatPublisherValueHolder.realmService = realmServiceParam;
    }
    /**
     * Get StatPublisherManager instance
     * @return instance of statPublisherManager
     */
    public static StatPublisherManager getStatPublisherManager() {
        return statPublisherManager;
    }
    /**
     * Set StatPublisherManager Service
     * @param statPublisherManager - StatPublisherManager instance
     */
    public static void setStatPublisherManager(StatPublisherManager statPublisherManager) {
        StatPublisherValueHolder.statPublisherManager = statPublisherManager;
    }
}