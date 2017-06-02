/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.business.messaging.core.constants;

/**
 * Defines the system properties and the contants which will be used for bootstrapping
 */
public class BrokerConstants {
    /**
     * MB store data service access definition key
     */
    public static final String MB_DS_NAME = "WSO2_MB_STORE_DB";

    /**
     * System property which will indicate to populate database schema
     */
    public static final String SYS_PROP_DATABASE_SETUP = "setup";

    /**
     * The MBean interface represented through qpid, which is used to test whether the broker is started
     */
    public static final String VIRTUAL_HOST_MBEAN = "org.wso2.andes:type=VirtualHost.VirtualHostManager,*";

    /**
     * The location for the qpid configuration file directory.
     */
    public static final String QPID_CONF = "qpid.conf";
}
