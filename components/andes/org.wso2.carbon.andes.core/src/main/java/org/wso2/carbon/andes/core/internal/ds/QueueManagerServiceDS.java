/*
 * Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 */
package org.wso2.carbon.andes.core.internal.ds;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.andes.core.QueueManagerService;
import org.wso2.carbon.andes.core.internal.builder.QueueManagerServiceBuilder;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.user.core.service.RealmService;
import org.wso2.carbon.utils.ConfigurationContextService;

/**
 * @scr.component name="QueueManagerService.component" immediate="true"
 * @scr.reference name="registry.service"
 * interface="org.wso2.carbon.registry.core.service.RegistryService" cardinality="1..1"
 * policy="dynamic" bind="setRegistryService" unbind="unsetRegistryService"
 * @scr.reference name="realm.service" interface="org.wso2.carbon.user.core.service.RealmService"
 * cardinality="1..1" policy="dynamic" bind="setRealmService"  unbind="unsetRealmService"
 * @scr.reference name="configurationcontext.service"
 * interface="org.wso2.carbon.utils.ConfigurationContextService" cardinality="1..1"
 * policy="dynamic" bind="setConfigurationContextService" unbind="unsetConfigurationContextService"
 */

public class QueueManagerServiceDS {

    public static Log log = LogFactory.getLog(QueueManagerServiceDS.class);

    protected void activate(ComponentContext context) {
        try {
            QueueManagerService brokerService = QueueManagerServiceBuilder.createQueueManagerService();
            context.getBundleContext().registerService(QueueManagerService.class.getName(),
                    brokerService, null);
            log.info("Successfully created the queue manager service");
        } catch (RuntimeException e) {
            log.error("Can not create queue manager service ", e);
        }
    }

    protected void setRealmService(RealmService realmService) {
        log.info("Setting Realm Service");
        QueueManagerServiceValueHolder.getInstance().registerRealmService(realmService);
    }

    protected void unsetRealmService(RealmService realmService) {
        QueueManagerServiceValueHolder.getInstance().registerRealmService(null);
    }

    protected void setRegistryService(RegistryService registryService) {
        QueueManagerServiceValueHolder.getInstance().registerRegistryService(registryService);
    }

    protected void unsetRegistryService(RegistryService registryService) {

    }

    protected void setConfigurationContextService(
            ConfigurationContextService configurationContextService) {
        QueueManagerServiceValueHolder.getInstance().registerConfigurationContextService(configurationContextService);
    }

    protected void unsetConfigurationContextService(
            ConfigurationContextService configurationContextService) {

    }

}
