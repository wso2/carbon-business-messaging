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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.andes.core;

/**
 * This exception represents subscription channel is already closed while
 * delivering message to a protocol (AMQP/MQTT). The block which catches this exception should
 * handle the failure state (i.e re-queuing/discarding/DLC)
 */
public class SubscriptionAlreadyClosedException extends ProtocolDeliveryFailureException {

    /***
     * Constructor
     *
     * @param message   descriptive message
     * @param errorCode one of the above defined constants that classifies the error.
     * @param cause     reference to the exception for reference.
     */
    public SubscriptionAlreadyClosedException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, cause);
    }

    /***
     * Constructor
     *
     * @param message descriptive message
     * @param cause   reference to the exception for reference.
     */
    public SubscriptionAlreadyClosedException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor
     *
     * @param message descriptive message
     */
    public SubscriptionAlreadyClosedException(String message) {
        super(message);
    }
}
