/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.business.messaging.core.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.wso2.carbon.business.messaging.core.authentication.AuthenticationService;

import java.util.logging.Logger;

/**
 * BrokerCore Bundle Activator to register default implementation of Authentication service.This bundleActivator will be
 * executed before any OSGI component.
 */
public class BrokerCoreBundleActivator implements BundleActivator {
    private ServiceRegistration serviceRegistration;
    Logger logger = Logger.getLogger(BrokerCoreBundleActivator.class.getName());

    /**
     * Register the default authentication impl provided by broker component before BrokerServiceComponent is activated.
     *
     * @param bundleContext
     * @throws Exception
     */
    @Override
    public void start(BundleContext bundleContext) throws Exception {
        logger.info("BrokerCoreBundleActivator is started.");
        serviceRegistration = bundleContext
                .registerService(AuthenticationService.class.getName(), new AuthenticationServiceImpl(), null);
    }

    @Override
    public void stop(BundleContext bundleContext) throws Exception {
        logger.info("BrokerCoreBundleActivator is deactivated.");
        serviceRegistration.unregister();
    }
}
