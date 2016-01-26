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

public class OAuth2BasedMQTTAuthenticator implements IAuthenticator {
	private static final Logger log = Logger.getLogger(OAuth2BasedMQTTAuthenticator.class);
	private static final String TOKEN_TYPE = "bearer";
	private static final String SCOPE_IDENTIFIER = "scope";
	private static String cookie;
	private GenericObjectPool stubs;

	/**
	 * initialize the  OAUTH2ValidationStubFactory  to communicate with the OAuth2TokenValidationService
	 */
	public OAuth2BasedMQTTAuthenticator(){
		stubs = new GenericObjectPool(new OAuthTokenValidaterStubFactory());
	}

	/**
	 * This method gets a string accessToken and validates it
	 *
	 * @param token which need to be validated.
	 * @return AuthenticationInfo with the validated results.
	 */
	@Override
	public AuthenticationInfo checkValid(String token, String unusedParameter) {
		return validateToken(token);
	}

	private AuthenticationInfo validateToken(String token) {
		AuthenticationInfo authenticationInfo = new AuthenticationInfo();
		OAuth2TokenValidationServiceStub tokenValidationServiceStub = null;
		try {
			tokenValidationServiceStub = (OAuth2TokenValidationServiceStub) this.stubs.borrowObject();
			if (cookie != null) {
				tokenValidationServiceStub._getServiceClient().getOptions().setProperty(
						HTTPConstants.COOKIE_STRING, cookie);
			}

			OAuth2TokenValidationRequestDTO validationRequest = new OAuth2TokenValidationRequestDTO();

			OAuth2TokenValidationRequestDTO_OAuth2AccessToken accessToken =
					new OAuth2TokenValidationRequestDTO_OAuth2AccessToken();
			accessToken.setTokenType(TOKEN_TYPE);
			accessToken.setIdentifier(token);
			validationRequest.setAccessToken(accessToken);

			boolean authenticated;
			OAuth2TokenValidationResponseDTO tokenValidationResponse;

			try {
				tokenValidationResponse = tokenValidationServiceStub.validate(validationRequest);
				if(tokenValidationResponse == null){
					authenticationInfo.setAuthenticated(false);
					return authenticationInfo;
				}
				authenticated = tokenValidationResponse.getValid();
				if(authenticated) {
					String authorizedUser = tokenValidationResponse.getAuthorizedUser();
					String username = MultitenantUtils.getTenantAwareUsername(authorizedUser);
					String tenantDomain = MultitenantUtils.getTenantDomain(authorizedUser);
					authenticationInfo.setUsername(username);
					authenticationInfo.setTenantDomain(tenantDomain);
				} else {
					if(log.isDebugEnabled()) {
						log.debug("token validation failed for token:" + token);
					}
				}
			} catch (RemoteException e) {
				log.error("Error on connecting with the validation endpoint", e);
				authenticationInfo.setAuthenticated(false);
				return authenticationInfo;
			}

			ServiceContext serviceContext =
					tokenValidationServiceStub._getServiceClient().getLastOperationContext()
							.getServiceContext();
			cookie = (String) serviceContext.getProperty(HTTPConstants.COOKIE_STRING);


			List<String> requiredScopes =  OAuthConfigurationManager.getInstance().getScopes();
			if (authenticated && requiredScopes != null) {
				String validateResponseScope[] = tokenValidationResponse.getScope();
				List<String> responseScopes = Arrays.asList(validateResponseScope);
				authenticationInfo.setProperty(SCOPE_IDENTIFIER, responseScopes);
				authenticated = isAuthroized(requiredScopes, responseScopes);
			}
			authenticationInfo.setAuthenticated(authenticated);
		} catch (Exception e) {
			log.error("Error occurred in borrowing an validation stub from the pool", e);
			authenticationInfo.setAuthenticated(false);
		} finally {
			try {
				if (tokenValidationServiceStub != null) {
					this.stubs.returnObject(tokenValidationServiceStub);
				}
			} catch (Exception e) {
				log.warn("Error occurred while returning the object back to the oauth token validation service " +
								 "stub pool", e);
			}
		}
		return authenticationInfo;
	}

	/**
	 *
	 * @param requiredScopes configured in broker.xml
	 * @param responseScopes response from token validation endpoint
	 * @return If scopes are configured in broker.xml, then it will compare the
	scopes with the token validation response and if the required scopes are in response scope
	then then connection will be authorized
	 */
	private boolean isAuthroized(List<String> requiredScopes, List<String> responseScopes){
		for (String requiredScope : requiredScopes) {
			if (!responseScopes.contains(requiredScope)) {
				return false;
			}
		}
		return true;
	}
}
