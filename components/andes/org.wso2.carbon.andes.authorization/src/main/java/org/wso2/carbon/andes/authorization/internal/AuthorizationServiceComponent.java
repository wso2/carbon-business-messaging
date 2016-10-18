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

package org.wso2.carbon.andes.authorization.internal;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.wso2.andes.configuration.AndesConfigurationManager;
import org.wso2.andes.configuration.qpid.plugins.ConfigurationPluginFactory;
import org.wso2.andes.server.security.SecurityPluginFactory;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.andes.authorization.config.AuthorizationConfigurationManager;
import org.wso2.carbon.andes.authorization.service.andes.AndesAuthorizationPlugin;
import org.wso2.carbon.andes.authorization.service.andes.AndesAuthorizationPluginConfiguration;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.base.api.ServerConfigurationService;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.user.core.service.RealmService;

/**
 * @scr.component  name="org.wso2.carbon.andes.authorization.internal.AuthorizationServiceComponent"
 *                              immediate="true"
 * @scr.reference    name="registry.service"
 *                              interface="org.wso2.carbon.registry.core.service.RegistryService"
 *                              cardinality="1..1"
 *                              policy="dynamic"
 *                              bind="setRegistryService"
 *                              unbind="unsetRegistryService"
 * @scr.reference    name="realm.service"
 *                              interface="org.wso2.carbon.user.core.service.RealmService"
 *                              cardinality="1..1"
 *                              policy="dynamic"
 *                              bind="setRealmService"
 *                              unbind="unsetRealmService"
 * @scr.reference name="server.configuration" interface="org.wso2.carbon.base.api.ServerConfigurationService"
 *                  cardinality="1..1"
 *                  policy="dynamic"
 *                  bind="setServerConfiguration"
 *                  unbind="unsetServerConfiguration"
 */
public class AuthorizationServiceComponent {

    private static final Log log = LogFactory.getLog(AuthorizationServiceComponent.class);
    private ServiceRegistration securityPluginFactory = null;
    private ServiceRegistration configurationPluginFactory = null;
    private static final String CARBON_CONFIG_PORT_OFFSET = "Ports.Offset";
    private static final int CARBON_DEFAULT_PORT_OFFSET = 0;

    protected void activate(ComponentContext ctx) {
        try {
            //TODO :reinitializing Andes Configuration manager, since we cannot guarantee the startup order, have to provide proper configuration service
            AndesConfigurationManager.initialize(getPortOffset());
            // Register security plugin factory
            securityPluginFactory = ctx.getBundleContext().registerService(
                    SecurityPluginFactory.class.getName(), AndesAuthorizationPlugin.FACTORY, null);

            // Register security configuration plugin factory
            configurationPluginFactory = ctx.getBundleContext().registerService(
                    ConfigurationPluginFactory.class.getName(),
                    AndesAuthorizationPluginConfiguration.FACTORY, null);
        } catch (Throwable e) {
            log.error("Failed to activate org.wso2.carbon.andes.authorization.internal." +
                      "AuthorizationServiceComponent : " + e);
        }
    }

    protected void deactivate(ComponentContext ctx) {
        // Unregister OSGi services that were registered at the time of activation
        if (null != securityPluginFactory) {
            securityPluginFactory.unregister();
        }

        if (null != configurationPluginFactory) {
            configurationPluginFactory.unregister();
        }
    }

    protected void setRegistryService(RegistryService registryService) {
        AuthorizationServiceDataHolder.getInstance().setRegistryService(registryService);
    }

    protected void unsetRegistryService(RegistryService registryService) {
        AuthorizationServiceDataHolder.getInstance().setRegistryService(null);
    }

    protected void setRealmService(RealmService realmService) {
        AuthorizationServiceDataHolder.getInstance().setRealmService(realmService);
    }

    protected void unsetRealmService(RealmService realmService) {
        AuthorizationServiceDataHolder.getInstance().setRealmService(null);
    }

    //wait till serverConfigurationService is started to pick the carbon offset
    protected void setServerConfiguration(ServerConfigurationService serverConfiguration) {

    }

    protected void unsetServerConfiguration(ServerConfigurationService serverConfiguration) {
    }

    private int getPortOffset() {
        ServerConfiguration carbonConfig = ServerConfiguration.getInstance();
        String portOffset = System.getProperty("portOffset",
                                               carbonConfig.getFirstProperty(CARBON_CONFIG_PORT_OFFSET));
        try {
            if ((portOffset != null)) {
                return Integer.parseInt(portOffset.trim());
            } else {
                return CARBON_DEFAULT_PORT_OFFSET;
            }
        } catch (NumberFormatException e) {
            return CARBON_DEFAULT_PORT_OFFSET;
        }
    }
}
