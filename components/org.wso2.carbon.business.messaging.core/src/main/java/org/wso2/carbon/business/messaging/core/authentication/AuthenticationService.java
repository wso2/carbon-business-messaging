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
package org.wso2.carbon.business.messaging.core.authentication;

import org.wso2.carbon.business.messaging.core.exceptions.AuthenticationException;

/**
 * Interface to expose operations related to user authentication.
 */
public interface AuthenticationService {
    /**
     * @param userName username of user
     * @param password password of user
     * @return if the user credentials are valid or not
     * @throws AuthenticationException can be thrown.
     */
    boolean isValidUser(String userName, char[] password) throws AuthenticationException;
}
