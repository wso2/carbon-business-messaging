/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
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

package org.wso2.carbon.andes.core.security;


import java.io.UnsupportedEncodingException;

/**
 * Utility class for AMQP security functions.
 */
public class AMQPSecurityUtils {

    /**
     * The charset which the credentials is encoded in QPID.
     */
    public static final String RESPONSE_CHARSET = "UTF8";

    /**
     * The separator which is used to separate username and password in encoded response.
     */
    private static final byte SEPARATOR = 0;

    /**
     * Encode a given username and password of QPID to a byte array.
     *
     * @param authenticationID The username/authenticate ID to use
     * @param password         The password
     * @return Encoded credentials byte array
     */
    public static byte[] encodeCredentials(String authenticationID, String password) throws Exception {
        try {
            byte[] passwordBytes = password.getBytes(RESPONSE_CHARSET);
            byte[] authIDBytes = authenticationID.getBytes(RESPONSE_CHARSET);
            byte[] encodedCredentials = new byte[passwordBytes.length + authIDBytes.length + 2];
            int startPosition = 0;

            // Mark the start by adding the separator
            encodedCredentials[startPosition] = SEPARATOR;

            // Copy authenticationID bytes to encoded byte array
            System.arraycopy(authIDBytes, 0, encodedCredentials, startPosition, authIDBytes.length);

            // Set next start position to copy to in the encoded byte array
            startPosition = authIDBytes.length;

            // Add a separator
            encodedCredentials[startPosition] = SEPARATOR;

            // Append password bytes to encoded byte array
            System.arraycopy(passwordBytes, 0, encodedCredentials, startPosition, passwordBytes.length);
            return encodedCredentials;
        } catch (UnsupportedEncodingException e) {
//            throw new AMQProtocolException(null, "Cannot get UTF-8 encoding of credentials.", e);
            throw new Exception("Cannot get UTF-8 encoding of credentials.", e);
            //TODO:Should handle it here
        }
    }
}
