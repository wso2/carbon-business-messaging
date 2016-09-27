/*
* Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.andes.discovery;


import org.osgi.framework.BundleException;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.andes.server.cluster.IpAddressRetriever;


/**
 * @scr.component name="org.wso2.carbon.andes.discovery.DiscoveryActivator"
 * immediate="true"
 */
public class DiscoveryActivator {

    protected final Logger logger = LoggerFactory.getLogger(DiscoveryActivator.class);

    protected void activate(ComponentContext ctx) {

        LocalCluster localCluster = new LocalCluster();
        ctx.getBundleContext().registerService(IpAddressRetriever.class.getName(),localCluster,null);

    }

    /**
     * Unregister OSGi services that were registered at the time of activation
     *
     * @param ctx component context
     * @throws BundleException
     */
    protected void deactivate(ComponentContext ctx) throws BundleException {
        ctx.getBundleContext().getBundle().stop();

    }

}
