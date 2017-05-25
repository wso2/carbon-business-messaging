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
package org.wso2.carbon.business.messaging.identity.impl.internal.config;

import org.wso2.carbon.business.messaging.identity.connector.config.AuthorizationStoreConnectorConfig;
import org.wso2.carbon.business.messaging.identity.connector.config.StoreConnectorConfig;
import org.wso2.carbon.business.messaging.identity.exception.CarbonIdentityMgtConfigException;
import org.wso2.carbon.business.messaging.identity.impl.util.BrokerSecurityConstants;
import org.wso2.carbon.business.messaging.identity.impl.util.FileUtil;
import org.wso2.carbon.kernel.utils.StringUtils;
import org.wso2.carbon.kernel.utils.Utils;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Read Connector config files for authorization/credential store connectors
 */
public class ConnectorConfigReader {

    private ConnectorConfigReader() {

    }

    /**
     * Read auth store connector config files.
     *
     * @return Connector name to connector config map.
     * @throws CarbonIdentityMgtConfigException Carbon Identity Mgt Config Exception.
     */
    public static Map<String, AuthorizationStoreConnectorConfig> getAuthStoreConnectorConfigs()
            throws
            CarbonIdentityMgtConfigException {

        List<AuthorizationStoreConnectorConfig> storeConnectorConfigs = new ArrayList<>();


        List<ConnectorConfigEntry> externalAuthStoreConnectors = buildExternalAuthStoreConnectorConfig();
        if (!externalAuthStoreConnectors.isEmpty()) {


            storeConnectorConfigs.addAll(getStoreConnectorConfig(externalAuthStoreConnectors,
                                                                 AuthorizationStoreConnectorConfig.class));
        }

        return storeConnectorConfigs.stream().collect(Collectors.toMap(StoreConnectorConfig::getConnectorId,
                                                                       storeConnectorConfig -> storeConnectorConfig));
    }


    /**
     * Read the AuthorizationStoreConnector config entries from external identity-connector.yaml files.
     *
     * @return List of external connector config entries.
     * @throws CarbonIdentityMgtConfigException Carbon Identity Mgt Config Exception.
     */
    private static List<ConnectorConfigEntry> buildExternalAuthStoreConnectorConfig() throws
                                                                                      CarbonIdentityMgtConfigException {

        Path path = Paths.get(Utils.getCarbonConfigHome() + File.separator + BrokerSecurityConstants.IDENTITY);

        return FileUtil.readConfigFiles(path, ConnectorConfigEntry.class, BrokerSecurityConstants.AUTH_CONNECTOR_FILE);
    }


    private static <T extends StoreConnectorConfig> List<AuthorizationStoreConnectorConfig> getStoreConnectorConfig
            (List<ConnectorConfigEntry> connectorConfigEntries, Class<T> classType) {

        return connectorConfigEntries.stream()
                .filter(Objects::nonNull)
                .filter(connectorConfigEntry -> !StringUtils.isNullOrEmpty(connectorConfigEntry.getConnectorId())
                                                && !StringUtils.isNullOrEmpty(connectorConfigEntry.getConnectorType()))
                .map(connectorConfigEntry -> {
                    if (classType.equals(AuthorizationStoreConnectorConfig.class)) {
                        return new AuthorizationStoreConnectorConfig(connectorConfigEntry.getConnectorId(),
                                                                     connectorConfigEntry.getConnectorType(),
                                                                     connectorConfigEntry.isReadOnly(),
                                                                     connectorConfigEntry.getProperties());
                    } else {
                        //TODO return credentialStoreConnectorConfig
                        return null;
                    }

                })
                .collect(Collectors.toList());
    }


}
