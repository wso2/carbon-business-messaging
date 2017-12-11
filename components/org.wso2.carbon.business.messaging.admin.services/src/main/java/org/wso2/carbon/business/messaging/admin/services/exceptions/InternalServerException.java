/*
 * Copyright (c) 2017, WSO2 Inc. (http://wso2.com) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.business.messaging.admin.services.exceptions;

/**
 * Exception class for the rest service when an server error occurs.
 */
public class InternalServerException extends Exception {
    private static final long serialVersionUID = 3803992383244407058L;
    private String faultCode;
    private String faultString;

    /**
     * Creates an internal service exceptions with an exceptions. Exception would be server related error.
     *
     * @param cause The exceptions occurred in the server.
     */
    public InternalServerException(Throwable cause) {
        super(cause);
    }

    /**
     * Creates an exception with given string and cause.
     *
     * @param message The exception message content.
     * @param cause The exception cause.
     */
    public InternalServerException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates an exception with given string.
     *
     * @param message The exception message content.
     */
    public InternalServerException(String message) {
        super(message);
    }

    /**
     * Creates an exception with given string and code.
     *
     * @param faultString The fault message content.
     * @param faultCode The fault code.
     */
    public InternalServerException(String faultString, String faultCode) {
        this.faultCode = faultCode;
        this.faultString = faultString;
    }
    public String getFaultCode() {
        return faultCode;
    }

    public String getFaultString() {
        return faultString;
    }
}
