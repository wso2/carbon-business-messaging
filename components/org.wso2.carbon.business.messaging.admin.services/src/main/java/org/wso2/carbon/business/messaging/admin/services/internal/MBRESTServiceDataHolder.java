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

package org.wso2.carbon.business.messaging.admin.services.internal;

import org.wso2.andes.kernel.Andes;

import java.util.logging.Logger;

/**
 * MBRESTServiceDataHolder to hold object instances related to MB admin service
 *
 * @since 4.0.0-SNAPSHOT
 */
public class MBRESTServiceDataHolder {
    private static MBRESTServiceDataHolder instance = new MBRESTServiceDataHolder();
    Logger logger = Logger.getLogger(MBRESTServiceDataHolder.class.getName());
    private Andes andesCore;

    private MBRESTServiceDataHolder() {

    }

    /**
     * This returns the MBRESTServiceDataHolder instance.
     *
     * @return The MBRESTServiceDataHolder instance of this singleton class
     */
    public static MBRESTServiceDataHolder getInstance() {
        return instance;
    }

    /**
     * Returns the business-messaging-core which gets set through a service component.
     *
     * @return business-messaging-core Service
     */
    public Andes getAndesCore() {
        return andesCore;
    }

    /**
     * This method is for setting the business-messaging-core service. This method is used by
     * ServiceComponent.
     *
     * @param andesCore The reference being passed through ServiceComponent
     */
    public void setAndesCore(Andes andesCore) {
        this.andesCore = andesCore;
    }
}
