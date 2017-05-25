/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.wso2.carbon.business.messaging.identity.impl.internal;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.business.messaging.identity.AuthorizationService;
import org.wso2.carbon.business.messaging.identity.connector.AuthorizationStoreConnectorFactory;
import org.wso2.carbon.business.messaging.identity.connector.config.AuthorizationStoreConnectorConfig;
import org.wso2.carbon.business.messaging.identity.exception.AuthorizationStoreException;
import org.wso2.carbon.business.messaging.identity.exception.CarbonIdentityMgtConfigException;
import org.wso2.carbon.business.messaging.identity.impl.AuthorizationServiceImpl;
import org.wso2.carbon.business.messaging.identity.impl.internal.config.ConnectorConfigReader;
import org.wso2.carbon.kernel.startupresolver.RequiredCapabilityListener;

import java.util.Map;

/**
 * IdentityMgtComponent for Broker related identity implementation
 */
@Component(
        name = "org.wso2.carbon.business.messaging.identity.impl.internal.IdentityMgtComponent",
        immediate = true

)
public class IdentityMgtComponent implements RequiredCapabilityListener {

    public static final String CONNECTOR_TYPE = "connector-type";
    private static final Logger log = LoggerFactory.getLogger(IdentityMgtComponent.class);

    private ServiceRegistration<AuthorizationService> authorizationServiceRegistration;

    private BundleContext bundleContext;

    /**
     * @param bundleContext
     */
    @Activate
    public void registerCarbonIdentityMgtProvider(BundleContext bundleContext) {

        this.bundleContext = bundleContext;
    }

    /**
     * @param bundleContext
     */
    @Deactivate
    public void unregisterCarbonIdentityMgtProvider(BundleContext bundleContext) {
        if (bundleContext != null && authorizationServiceRegistration != null) {
            bundleContext.ungetService(authorizationServiceRegistration.getReference());
        }
    }

    @Reference(
            name = "AuthorizationStoreConnectorFactory",
            service = AuthorizationStoreConnectorFactory.class,
            cardinality = ReferenceCardinality.AT_LEAST_ONE,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unregisterAuthorizationStoreConnectorFactory"
    )
    /**
     * Register authorizationStoreConnectorFactory
     */
    protected void registerAuthorizationStoreConnectorFactory(
            AuthorizationStoreConnectorFactory authorizationStoreConnectorFactory,
            Map<String, String> properties) {

        String connectorId = properties.get("connector-type");
        IdentityMgtDataHolder.getInstance()
                .registerAuthorizationStoreConnectorFactory(connectorId,
                                                            authorizationStoreConnectorFactory);
    }

    /**
     * Unregister authorizationStoreConnectorFactory
     *
     * @param authorizationStoreConnectorFactory
     */
    protected void unregisterAuthorizationStoreConnectorFactory(
            AuthorizationStoreConnectorFactory authorizationStoreConnectorFactory) {
    }

    //TODO: add constructCredentialService()
    @Override
    public void onAllRequiredCapabilitiesAvailable() {

        constructAuthorizationService();
    }

    /**
     * Construct authorization service to initialise authorization store
     */
    private void constructAuthorizationService() {

        IdentityMgtDataHolder identityMgtDataHolder = IdentityMgtDataHolder.getInstance();

        try {

            // Load authorization store connector configs
            Map<String, AuthorizationStoreConnectorConfig> storeConnectorConfigs =
                    ConnectorConfigReader.getAuthStoreConnectorConfigs();
            if (storeConnectorConfigs != null) {
                AuthorizationService authorizationService = new AuthorizationServiceImpl(storeConnectorConfigs);
                identityMgtDataHolder.registerAuthorizationService(authorizationService);
                authorizationServiceRegistration = bundleContext.registerService(AuthorizationService.class,
                                                                                 authorizationService, null);
                log.info("Authorization service registered successfully.");
            } else {
                log.error("No authorization connector configurations were found");
            }
        } catch (CarbonIdentityMgtConfigException | AuthorizationStoreException e) {
            log.error("Error loading store configurations", e);
        }
    }


}
