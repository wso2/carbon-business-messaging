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

package org.wso2.carbon.business.messaging.core.service.ws;

import org.wso2.carbon.business.messaging.core.internal.QpidServiceDataHolder;
import org.wso2.carbon.business.messaging.core.service.QpidServiceImpl;

/**
 * this class is used to access the Qpid service details remotely through a web service.
 */
public class QpidAdminService {

    public String getAccessKey() {
        return QpidServiceDataHolder.getInstance().getAccessKey();
    }

    /**
     * Get client id (machine name) that is used to connect to the broker
     *
     * @return Client ID
     */
    public String getClientID() {
        QpidServiceImpl qpidServiceImpl =
                new QpidServiceImpl(QpidServiceDataHolder.getInstance().getAccessKey());
        return qpidServiceImpl.getClientID();
    }

    /**
     * Get default virtual host name of the broker
     *
     * @return Default virtual host name
     */
    public String getVirtualHostName() {
        QpidServiceImpl qpidServiceImpl =
                new QpidServiceImpl(QpidServiceDataHolder.getInstance().getAccessKey());
        return qpidServiceImpl.getVirtualHostName();
    }

    /**
     * Get hostname of the machine that broker (i.e.Carbon) runs on
     *
     * @return Carbon hostname
     */
    public String getHostname() {
        QpidServiceImpl qpidServiceImpl =
                new QpidServiceImpl(QpidServiceDataHolder.getInstance().getAccessKey());
        return qpidServiceImpl.getHostname();
    }

    /**
     * Get TCP port of the broker
     *
     * @return Broker TCP port
     */
    public String getPort() {
        QpidServiceImpl qpidServiceImpl =
                new QpidServiceImpl(QpidServiceDataHolder.getInstance().getAccessKey());
        return qpidServiceImpl.getAMQPPort().toString();
    }


    /**
     * Returns the SSL port that a client can use to communicate with the broker over SSL
     *
     * @return the SSL port
     */
    public String getSSLPort() {
        QpidServiceImpl qpidServiceImpl =
                new QpidServiceImpl(QpidServiceDataHolder.getInstance().getAccessKey());
        return qpidServiceImpl.getAMQPSSLPort().toString();
    }
}
