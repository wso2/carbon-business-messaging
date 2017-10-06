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
package org.wso2.carbon.business.messaging.core.internal;

import org.wso2.carbon.business.messaging.core.authentication.AuthenticationService;

/**
 * This class implements AuthenticationService interface to provide user authentication.
 */
public class AuthenticationServiceImpl implements AuthenticationService {
    /**
     * @param userName username of user
     * @param password password of user
     * @return if the user credentials are valid or not.
     */
    @Override
    //TODO: Integrate with user authentication implementation when available
    public boolean isValidUser(String userName, char[] password) {

        if (userName.equals("admin") && new String(password).equals("admin")) {
            return true;
        }
        return false;
    }
}
