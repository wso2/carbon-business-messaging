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
 * Exception class for broker manager MBeans.
 */
public class BrokerManagerException extends Exception {
    private static final long serialVersionUID = -9060869634592621585L;
    private String faultCode;
    private String faultString;

    public BrokerManagerException() {
        super();
    }

    public BrokerManagerException(String message, Throwable cause) {
        super(message, cause);
    }

    public BrokerManagerException(String message) {
        super(message);
    }

    public BrokerManagerException(String faultString, String faultCode) {
        this.faultCode = faultCode;
        this.faultString = faultString;
    }

    public BrokerManagerException(Throwable cause) {
        super(cause);
    }

    public String getFaultCode() {
        return faultCode;
    }

    public String getFaultString() {
        return faultString;
    }
}
