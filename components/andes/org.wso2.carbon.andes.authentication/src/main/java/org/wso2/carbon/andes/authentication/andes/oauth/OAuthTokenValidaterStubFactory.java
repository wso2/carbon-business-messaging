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
package org.wso2.carbon.andes.authentication.andes.oauth;

import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.axis2.transport.http.HttpTransportProperties;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpConnectionManager;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.contrib.ssl.EasySSLProtocolSocketFactory;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.httpclient.protocol.ProtocolSocketFactory;
import org.apache.commons.pool.BasePoolableObjectFactory;
import org.apache.commons.ssl.KeyMaterial;
import org.apache.log4j.Logger;
import org.wso2.andes.configuration.modules.JKSStore;
import org.wso2.carbon.andes.authentication.andes.oauth.config.OAuthConfigurationManager;
import org.wso2.carbon.andes.authentication.andes.oauth.exception.OAuthTokenValidationException;
import org.wso2.carbon.identity.oauth2.stub.OAuth2TokenValidationServiceStub;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.security.GeneralSecurityException;

/**
 * This follows object pool pattern to manage the stub for oauth validation service.
 */
public class OAuthTokenValidaterStubFactory extends BasePoolableObjectFactory {
	private static final Logger log = Logger.getLogger(OAuthTokenValidaterStubFactory.class);
	OAuthConfigurationManager config;
	private HttpClient httpClient;

	public OAuthTokenValidaterStubFactory() {
		this.config = OAuthConfigurationManager.getInstance();
		this.httpClient = createHttpClient();
	}

	/**
	 * This creates a object to the pool.
	 * @return an OAuthValidationStub object
	 * @throws Exception
	 */
	@Override
	public Object makeObject() throws Exception {
		return this.generateStub();
	}

	/**
	 * This is used to clean up the Oauth validation stub and releases to the object pool.
	 * @param o
	 * @throws Exception
	 */
	@Override
	public void passivateObject(Object o) throws Exception {
		if (o instanceof OAuth2TokenValidationServiceStub) {
			OAuth2TokenValidationServiceStub stub = (OAuth2TokenValidationServiceStub) o;
			stub._getServiceClient().cleanupTransport();
		}
	}

	private OAuth2TokenValidationServiceStub generateStub() throws OAuthTokenValidationException {
		OAuth2TokenValidationServiceStub stub;
		URL hostURL = config.getHostUrl();
		try {
			if(hostURL != null) {
				stub = new OAuth2TokenValidationServiceStub(hostURL.toString());
				if (stub != null) {
					ServiceClient client = stub._getServiceClient();
					client.getServiceContext().getConfigurationContext().setProperty(
							HTTPConstants.CACHED_HTTP_CLIENT, httpClient);

					HttpTransportProperties.Authenticator auth =
							new HttpTransportProperties.Authenticator();
					auth.setPreemptiveAuthentication(true);
					String username = config.getUsername();
					String password = config.getPassword();
					auth.setPassword(username);
					auth.setUsername(password);

					Options options = client.getOptions();
					options.setProperty(HTTPConstants.AUTHENTICATE, auth);
					options.setProperty(HTTPConstants.REUSE_HTTP_CLIENT, Constants.VALUE_TRUE);
					client.setOptions(options);
					if (hostURL.getProtocol().equals("https")) {
						// set up ssl factory since axis2 https transport is used.
						EasySSLProtocolSocketFactory sslProtocolSocketFactory =
								createProtocolSocketFactory();
						Protocol authhttps = new Protocol(hostURL.getProtocol(),
														  (ProtocolSocketFactory) sslProtocolSocketFactory,
														  hostURL.getPort());
						Protocol.registerProtocol(hostURL.getProtocol(), authhttps);
						options.setProperty(HTTPConstants.CUSTOM_PROTOCOL_HANDLER, authhttps);
					}
				} else {
					String errorMsg = "OAuth Validation instanization failed.";
					throw new OAuthTokenValidationException(errorMsg);
				}
			} else {
				String errorMsg = "host url is invalid";
				throw new OAuthTokenValidationException(errorMsg);
			}
		} catch (AxisFault axisFault) {
			throw new OAuthTokenValidationException(
					"Error occurred while creating the OAuth2TokenValidationServiceStub.", axisFault);
		}

		return stub;
	}

	/**
	 * @return an EasySSLProtocolSocketFactory for SSL communication. This is required because of using
	 * CommonHTTPTransport(axis2 transport) in axis2
	 */
	private EasySSLProtocolSocketFactory createProtocolSocketFactory() throws OAuthTokenValidationException{
		try {
			EasySSLProtocolSocketFactory easySSLPSFactory = new EasySSLProtocolSocketFactory();
			JKSStore jksKeyStore = OAuthConfigurationManager.getInstance().getJksKeyStore();
			String keyStoreLocation = jksKeyStore.getStoreLocation();
			char[] password = jksKeyStore.getPassword().toCharArray();
			File keyStoreFile = new File(keyStoreLocation);
			if (keyStoreFile.exists()) {
				KeyMaterial km = new KeyMaterial(keyStoreLocation, password);
				easySSLPSFactory.setKeyMaterial(km);
				return easySSLPSFactory;
			} else {
				String errorMsg = "Unable to load Keystore from the following location: " + keyStoreLocation;
				throw new OAuthTokenValidationException(errorMsg);
			}
		} catch (IOException e) {
			String errorMsg = "Failed to initiate EasySSLProtocolSocketFactory.";
			throw new OAuthTokenValidationException(errorMsg);
		} catch (GeneralSecurityException e) {
			String errorMsg = "Failed to set the key material in easy ssl factory.";
			throw new OAuthTokenValidationException(errorMsg);
		}

	}

	/**
	 * @return an instance of HttpClient that is configured with MultiThreadedHttpConnectionManager
	 */
	private HttpClient createHttpClient() {
		HttpConnectionManagerParams params = new HttpConnectionManagerParams();
		params.setDefaultMaxConnectionsPerHost(config.getMaximumHttpConnectionPerHost());
		params.setMaxTotalConnections(config.getMaximumTotalHttpConnection());
		HttpConnectionManager connectionManager = new MultiThreadedHttpConnectionManager();
		connectionManager.setParams(params);
		return new HttpClient(connectionManager);
	}
}
