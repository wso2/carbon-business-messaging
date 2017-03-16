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
package org.wso2.carbon.andes.authentication.andes.oauth.config;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.andes.configuration.AndesConfigurationManager;
import org.wso2.andes.configuration.enums.AndesConfiguration;
import org.wso2.andes.configuration.modules.JKSStore;
import org.wso2.andes.kernel.AndesException;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 * This class acts as a access point for config parameters used within the oauth based mqtt authenticator.
 * this configuration is read from broker.xml
 * configuration:
 * <p/>
 * <authenticator class="org.wso2.carbon.andes.authentication.andes.OAuth2BasedMQTTAuthenticator">
 * <property name="hostURL">https://localhost:9443/services/OAuth2TokenValidationService</property>
 * <property name="username">admin</property>
 * <property name="password">admin</property>
 * <property name="scopes">device_scope</property>
 * <property name="maxConnectionsPerHost">10</property>
 * <property name="maxTotalConnections">150</property>
 * </authenticator>
 */
public class OAuthConfigurationManager {
	private static final Log log = LogFactory.getLog(OAuthConfigurationManager.class);
	private URL hostUrl;
	private String username;
	private String password;
	private JKSStore jksKeyStore;
	private JKSStore jksTrustStore;
	private int maximumTotalHttpConnection;
	private int maximumHttpConnectionPerHost;
	private static final String USERNAME_KEY = "username";
	private static final String PASSWORD_KEY = "password";
	private static final String HOST_URL_KEY = "hostURL";
	private static final String MAX_CONNECTION_KEY = "maxConnectionsPerHost";
	private static final String TOTAL_CONNECTION_KEY = "maxTotalConnections";
	private static final int DEFAULT_CONNECTION_PER_HOST = 2;
	private static final int DEFAULT_TOTAL_CONNECTION = 100;


	private static final OAuthConfigurationManager
			oAuthConfigurationManager = new OAuthConfigurationManager();

	public static OAuthConfigurationManager getInstance() {
		return oAuthConfigurationManager;
	}

	private OAuthConfigurationManager() {
	}

	/**
	 * Initialize the configuration properties that required for OAuth based authentication.
	 */
	public synchronized void initConfig() throws AndesException {
		jksKeyStore = AndesConfigurationManager.readValue(
				AndesConfiguration.TRANSPORTS_MQTT_SSL_CONNECTION_KEYSTORE);
		jksTrustStore = AndesConfigurationManager.readValue(
				AndesConfiguration.TRANSPORTS_MQTT_SSL_CONNECTION_TRUSTSTORE);

		List<String> mqttTranportProperties = AndesConfigurationManager.readValueList
				(AndesConfiguration.LIST_TRANSPORT_MQTT_AUTHENTICATION_PROPERTIES);

		for (String property : mqttTranportProperties) {
			String propertyValue = AndesConfigurationManager.readValueOfChildByKey(
					AndesConfiguration.TRANSPORT_MQTT_AUTHENTICATION_PROPERTIES, property);
			switch (property) {
				case HOST_URL_KEY:
					setHostUrl(propertyValue);
					break;
				case USERNAME_KEY:
					setUsername(propertyValue);
					break;
				case PASSWORD_KEY:
					setPassword(propertyValue);
					break;
				case MAX_CONNECTION_KEY:
					setMaximumHttpConnectionPerHost(propertyValue);
					break;
				case TOTAL_CONNECTION_KEY:
					setMaximumTotalHttpConnection(propertyValue);
					break;
			}
		}
	}

	/**
	 * @return Oauth authentication service endpoint
	 */
	public URL getHostUrl() {
		return hostUrl;
	}

	/**
	 * @return username to connect with Oauth authentication service
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * @return password to connect with Oauth authentication service
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * @return Keystore used for ssl connection in CommonHTTP transport in axis
	 */
	public JKSStore getJksKeyStore() {
		return jksKeyStore;
	}

	/**
	 * @return maximumTotalHttpConnection used in a http connection manager
	 */
	public int getMaximumTotalHttpConnection() {
		return maximumTotalHttpConnection;
	}

	/**
	 * @return maximumHttpConnectionPerHost used in a http connection manager
	 */
	public int getMaximumHttpConnectionPerHost() {
		return maximumHttpConnectionPerHost;
	}

	private void setHostUrl(String hostUrlString) throws AndesException {
		try {
			hostUrl = new URL(hostUrlString);
		} catch (MalformedURLException | NullPointerException e) {
			String errorMsg = "invalid token endpoint URL " + hostUrlString;
			log.error(errorMsg);
			throw new AndesException(errorMsg);
		}
	}

	private void setUsername(String username) throws AndesException {
		if (username != null && !username.isEmpty()) {
			this.username = username;
		} else {
			String errorMsg = "invalid username " + username;
			log.error(errorMsg);
			throw new AndesException(errorMsg);
		}
	}

	private void setPassword(String password) throws AndesException {
		if (password != null && !password.isEmpty()) {
			this.password = password;
		} else {
			String errorMsg = "invalid password " + password;
			log.error(errorMsg);
			throw new AndesException(errorMsg);
		}
	}

	private void setMaximumTotalHttpConnection(String maximumTotalConnectionString) {
		try {
			maximumTotalHttpConnection = Integer.parseInt(maximumTotalConnectionString);
		} catch (NumberFormatException | NullPointerException e) {
			//set to default Value
			if (log.isDebugEnabled()) {
				log.debug("maximum total connection is " + maximumTotalConnectionString +
								  " therefore set to the default value");
			}
			maximumTotalHttpConnection = DEFAULT_TOTAL_CONNECTION;
		}
	}

	private void setMaximumHttpConnectionPerHost(String maximumConnectionPerHostString) {
		try {
			maximumHttpConnectionPerHost = Integer.parseInt(maximumConnectionPerHostString);
		} catch (NumberFormatException e) {
			//set to default Value
			if (log.isDebugEnabled()) {
				log.debug("maximum connection per host is " + maximumConnectionPerHostString +
								  " therefore set to the default value");
			}
			maximumHttpConnectionPerHost = DEFAULT_CONNECTION_PER_HOST;
		}
	}

}
