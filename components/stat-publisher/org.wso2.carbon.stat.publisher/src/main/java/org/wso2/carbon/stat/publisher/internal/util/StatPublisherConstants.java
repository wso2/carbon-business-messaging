/*
*  Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
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

package org.wso2.carbon.stat.publisher.internal.util;

import org.wso2.carbon.core.RegistryResources;

import java.io.File;

public final class StatPublisherConstants {
    public static final String RESOURCE = "StatisticConfiguration";
    public static final String NODE_URL = "nodeURL";
    public static final String USER_NAME = "username";
    public static final String PASSWORD = "password";
    public static final String URL = "url";
    public static final String MB_STAT_ENABLE = "mbStatEnable";
    public static final String MESSAGE_STAT_ENABLE = "messageStatEnable";
    public static final String SYSTEM_STAT_ENABLE = "systemStatEnable";
    public static final String MEDIATION_STATISTICS_REG_PATH =
            RegistryResources.COMPONENTS + "org.wso2.carbon.stat.publisher"+File.separator+"messageBrokerStats"+File.separator;
    public static final String JMX_XML = "jmx.xml";
    public static final String CARBON_XML = "carbon.xml";
    public static final String STAT_CONF_XML = "mbStatConfiguration.xml";
    public static final String CONF_DIRECTORY_PATH = "repository"+ File.separator+"conf"+File.separator;
    public static final String JMX_DIRECTORY_PATH = "repository"+ File.separator+"conf"+File.separator+"etc"+File.separator;

}
