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
package org.wso2.carbon.andes.authorization.config;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.andes.configuration.AndesConfigurationManager;
import org.wso2.andes.configuration.enums.AndesConfiguration;
import org.wso2.andes.kernel.AndesException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class acts as a access point to retrieve config parameters used within the authorization.
 * this configuration is read from broker.xml
 */
public class AuthorizationConfigurationManager {
	private static final Log log = LogFactory.getLog(AuthorizationConfigurationManager.class);
	private static Map<String, String> propertyMap = new HashMap<>();

	private static final AuthorizationConfigurationManager
			authorizationConfigurationManager = new AuthorizationConfigurationManager();

	public static AuthorizationConfigurationManager getInstance() {
		return authorizationConfigurationManager;
	}

	private AuthorizationConfigurationManager() {
	}

	/**
	 * Initialize the configuration properties that required for OAuth based authentication.
	 *
	 * @throws AndesException Thrown when an error occurs.
	 */
	public synchronized void initConfig() throws AndesException {
		List<String> mqttTranportProperties = AndesConfigurationManager.readValueList
				(AndesConfiguration.LIST_TRANSPORT_MQTT_AUTHORIZATION_PROPERTIES);
		for (String property : mqttTranportProperties) {
			String propertyValue = AndesConfigurationManager.readValueOfChildByKey(
					AndesConfiguration.TRANSPORT_MQTT_AUTHORIZATION_PROPERTIES, property);
			propertyMap.put(property, propertyValue);
		}
	}

	public String getProperty(String propertyKey) {
		return propertyMap.get(propertyKey);
	}

}
