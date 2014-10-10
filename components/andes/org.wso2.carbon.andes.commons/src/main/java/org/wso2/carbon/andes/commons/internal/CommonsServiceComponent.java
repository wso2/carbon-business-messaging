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

package org.wso2.carbon.andes.commons.internal;

import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.user.core.service.RealmService;

/**
 * @scr.component name="org.wso2.carbon.andes.commons.internal.CommonsServiceComponent"
 *                             immediate="true"
 * @scr.reference name="registry.service"
 *                           interface="org.wso2.carbon.registry.core.service.RegistryService"
 *                           cardinality="1..1"
 *                           policy="dynamic"
 *                           bind="setRegistryService"
 *                           unbind="unsetRegistryService"
 * @scr.reference name="realm.service"
 *                           interface="org.wso2.carbon.user.core.service.RealmService"
 *                           cardinality="1..1"
 *                           policy="dynamic"
 *                           bind="setRealmService"
 *                           unbind="unsetRealmService"
 */
public class CommonsServiceComponent {

    protected void activate(ComponentContext ctx) {
    }

    protected void deactivate(ComponentContext ctx) {
    }

    protected void setRegistryService(RegistryService registryService) {
        CommonsDataHolder.getInstance().setRegistryService(registryService);
    }

    protected void unsetRegistryService(RegistryService registryService) {
        CommonsDataHolder.getInstance().setRegistryService(null);
    }

    protected void setRealmService(RealmService realmService) {
        CommonsDataHolder.getInstance().setRealmService(realmService);
    }

    protected void unsetRealmService(RealmService realmService) {
        CommonsDataHolder.getInstance().setRealmService(null);
    }
}
