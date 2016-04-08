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
package org.wso2.carbon.andes.authentication.andes;

import org.apache.axis2.context.ServiceContext;
import org.apache.axis2.transport.http.HTTPConstants;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.log4j.Logger;
import org.dna.mqtt.moquette.server.IAuthenticator;
import org.dna.mqtt.moquette.server.AuthenticationInfo;
import org.wso2.carbon.andes.authentication.andes.oauth.OAuthTokenValidaterStubFactory;
import org.wso2.carbon.identity.oauth2.stub.OAuth2TokenValidationServiceStub;
import org.wso2.carbon.identity.oauth2.stub.dto.OAuth2TokenValidationRequestDTO;
import org.wso2.carbon.identity.oauth2.stub.dto.OAuth2TokenValidationRequestDTO_OAuth2AccessToken;
import org.wso2.carbon.identity.oauth2.stub.dto.OAuth2TokenValidationResponseDTO;
import org.wso2.carbon.andes.authentication.andes.oauth.config.OAuthConfigurationManager;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.List;
/**
 * Authenticates token with OAuthValidationService. Intended usage is
 * via providing fully qualified class name
 * in broker.xml
 */
public class OAuth2BasedMQTTAuthenticator implements IAuthenticator {
	private static final Logger log = Logger.getLogger(OAuth2BasedMQTTAuthenticator.class);
	private static final String TOKEN_TYPE = "bearer";
	private static final String SCOPE_IDENTIFIER = "scope";
	private static final String TOKEN_EXPIRY_TIME_IDENTIFIER = "expiry_time";
	private static String cookie;
	private GenericObjectPool stubs;

	/**
	 * Initialize the OAUTH2ValidationStubFactory  to communicate with the OAuth2TokenValidationService
	 */
	public OAuth2BasedMQTTAuthenticator(){
		stubs = new GenericObjectPool(new OAuthTokenValidaterStubFactory());
	}


	/**
	 * {@inheritDoc} Authenticates the user against carbon user store.
	 */
	@Override
	public AuthenticationInfo checkValid(String token, String unusedParameter) {
		return validateToken(token);
	}


	/**
	 * This method gets a string accessToken and validates it
	 *
	 * @param token which need to be validated.
	 * @return AuthenticationInfo with the validated results.
	 */
	private AuthenticationInfo validateToken(String token) {
		OAuth2TokenValidationServiceStub tokenValidationServiceStub = null;
		try {
			Object stub = this.stubs.borrowObject();
			if (stub != null) {
				tokenValidationServiceStub = (OAuth2TokenValidationServiceStub) stub;
				if (cookie != null) {
					tokenValidationServiceStub._getServiceClient().getOptions().setProperty(
							HTTPConstants.COOKIE_STRING, cookie);
				}
				return getAuthenticationInfo(token, tokenValidationServiceStub);
			} else {
				log.warn("Stub initialization failed.");
			}
		} catch (RemoteException e) {
			log.error("Error on connecting with the validation endpoint.", e);
		} catch (Exception e) {
			log.error("Error occurred in borrowing an validation stub from the pool.", e);

		} finally {
			try {
				if (tokenValidationServiceStub != null) {
					this.stubs.returnObject(tokenValidationServiceStub);
				}
			} catch (Exception e) {
				log.warn("Error occurred while returning the object back to the oauth token validation service " +
								 "stub pool.", e);
			}
		}
		AuthenticationInfo authenticationInfo = new AuthenticationInfo();
		authenticationInfo.setAuthenticated(false);
		return authenticationInfo;
	}

	/**
	 * This creates an AuthenticationInfo object that is used for authorization. This method will validate the token and
	 * sets the required parameters to the object.
	 *
	 * @param token                      that needs to be validated.
	 * @param tokenValidationServiceStub stub that is used to call the external service.
	 * @return AuthenticationInfo This contains the information related to authenticated client.
	 * @throws RemoteException that triggers when failing to call the external service..
	 */
	private AuthenticationInfo getAuthenticationInfo(String token,
													 OAuth2TokenValidationServiceStub tokenValidationServiceStub)
			throws RemoteException {
		AuthenticationInfo authenticationInfo = new AuthenticationInfo();
		OAuth2TokenValidationRequestDTO validationRequest = new OAuth2TokenValidationRequestDTO();
		OAuth2TokenValidationRequestDTO_OAuth2AccessToken accessToken =
				new OAuth2TokenValidationRequestDTO_OAuth2AccessToken();
		accessToken.setTokenType(TOKEN_TYPE);
		accessToken.setIdentifier(token);
		validationRequest.setAccessToken(accessToken);
		boolean authenticated;
		OAuth2TokenValidationResponseDTO tokenValidationResponse;
		tokenValidationResponse = tokenValidationServiceStub.validate(validationRequest);
		if (tokenValidationResponse == null) {
			authenticationInfo.setAuthenticated(false);
			return authenticationInfo;
		}
		authenticated = tokenValidationResponse.getValid();
		if (authenticated) {
			String authorizedUser = tokenValidationResponse.getAuthorizedUser();
			String username = MultitenantUtils.getTenantAwareUsername(authorizedUser);
			String tenantDomain = MultitenantUtils.getTenantDomain(authorizedUser);
			authenticationInfo.setUsername(username);
			authenticationInfo.setTenantDomain(tenantDomain);
			authenticationInfo.setProperty(TOKEN_EXPIRY_TIME_IDENTIFIER, tokenValidationResponse.getExpiryTime());
			String validateResponseScope[] = tokenValidationResponse.getScope();
			if (validateResponseScope != null && validateResponseScope.length > 0) {
				List<String> responseScopes = Arrays.asList(validateResponseScope);
				authenticationInfo.setProperty(SCOPE_IDENTIFIER, responseScopes);
			}
		} else {
			if (log.isDebugEnabled()) {
				log.debug("Token validation failed for token: " + token);
			}
		}
		ServiceContext serviceContext = tokenValidationServiceStub._getServiceClient()
				.getLastOperationContext().getServiceContext();
		cookie = (String) serviceContext.getProperty(HTTPConstants.COOKIE_STRING);
		authenticationInfo.setAuthenticated(authenticated);
		return authenticationInfo;
	}

}
