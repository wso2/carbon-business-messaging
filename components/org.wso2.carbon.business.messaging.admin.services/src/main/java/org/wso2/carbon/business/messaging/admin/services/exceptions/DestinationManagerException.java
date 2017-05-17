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
 * Exception class for destination manager MBeans.
 */
public class DestinationManagerException extends Exception {
    private static final long serialVersionUID = 2254471598608383687L;
    private String faultCode;
    private String faultString;

    public DestinationManagerException() {
        super();
    }

    public DestinationManagerException(String message, Throwable cause) {
        super(message, cause);
    }

    public DestinationManagerException(String message) {
        super(message);
    }

    public DestinationManagerException(String faultString, String faultCode) {
        this.faultCode = faultCode;
        this.faultString = faultString;
    }

    public DestinationManagerException(Throwable cause) {
        super(cause);
    }

    public String getFaultCode() {
        return faultCode;
    }

    public String getFaultString() {
        return faultString;
    }
}
