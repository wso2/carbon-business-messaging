/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.andes.listeners;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.ServiceRegistration;
import org.wso2.andes.kernel.AndesException;
import org.wso2.andes.kernel.AndesKernelBoot;
import org.wso2.carbon.core.ServerShutdownHandler;

/**
 * Any specific things that need to be done before the server is shutdown should be handled by this handler. Please
 * note that this is not intended for doing cleanup on bundle deactivation. That should be done within each
 * respective bundle. Any pre-shutdown work that needs to be done, should be done here.
 */
public class AndesServerShutDownListener implements ServerShutdownHandler {

    private static final Log log = LogFactory.getLog(AndesServerShutDownListener.class);

    private ServiceRegistration qpidService = null;

    /**
     * Need to keep reference of QpidService so that we can unregister it.
     * @param qpidService
     */
    public AndesServerShutDownListener(ServiceRegistration qpidService) {
        this.qpidService = qpidService;
    }

    /**
     * {@inheritDoc}
     * Before the shut down process begins, we need to close any and all dependent services threads that could be
     * affected by unavailability of dependencies activated within the server.
     */
    @Override
    public void invoke() {
        if (log.isDebugEnabled()) {
            log.debug("Shutting down all dependent services and threads before stopping all osgi components to allow " +
                    "graceful shutdown.");
        }

        // Unregister QpidService
        if (null != qpidService) {
            qpidService.unregister();
        }

        // Shut down the Andes broker
        try {
            AndesKernelBoot.shutDownAndesKernel();
        } catch (AndesException e) {
            log.error("Error while shutting down Andes kernel. ", e);
        }
    }
}
