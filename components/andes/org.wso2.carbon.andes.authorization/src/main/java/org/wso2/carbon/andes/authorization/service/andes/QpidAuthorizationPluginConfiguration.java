/*
 *  Copyright (c) 2008, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.wso2.carbon.andes.authorization.service.andes;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.wso2.andes.server.configuration.plugins.ConfigurationPlugin;
import org.wso2.andes.server.configuration.plugins.ConfigurationPluginFactory;
import java.util.Arrays;
import java.util.List;

/**
 * This is the configuration class for QpidAuthorizationPlugin that is based on Qpid plugin configuration model.
 * This is not actually used as QpidAuthorizationPlugin loads configuration off Carbon Registry.
 */
public class QpidAuthorizationPluginConfiguration extends ConfigurationPlugin {

    /**
        * Factory method for QpidAuthorizationPluginConfiguration
        */
    public static final ConfigurationPluginFactory FACTORY = new ConfigurationPluginFactory()
    {
        public ConfigurationPlugin newInstance(String path, Configuration config)
                throws ConfigurationException
        {
            ConfigurationPlugin instance = new QpidAuthorizationPluginConfiguration();
            return instance;
        }

        public List<String> getParentPaths()
        {
            return Arrays.asList("");
        }
    };

    public String[] getElementsProcessed() {
        return new String[]{""};
    }
}
