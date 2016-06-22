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
 * This should be used to expose exceptional scenarios specific to wso2 mb logic. Can also be used to unify and
 * expose different
 * exceptions coming from different components into relevant error messages.
 */
public class AndesException extends Exception {

    /**
     * Default Serialization UID
     */
    private static final long serialVersionUID = 1L;

    /***
     * The most frequent exception happening here is due to message content being miscollected or not being there.
     * we cannot throw this exception and interrupt other delivery tasks. The message content can be unavailable only
     * if :
     * 1. a message is sent and acknowledged
     * 2. all messages of a queue is suddenly purged.
     */
    public static final String MESSAGE_CONTENT_OBSOLETE = "MESSAGE_CONTENT_OBSOLETE";

    /**
     * error code for our custom exception type to identify specific scenarios and handle them properly.
     * TODO - add error codes to other frequent exceptional scenarios
     */
    private String errorCode = "";

    public AndesException() {
    }

    public AndesException(String message) {
        super(message);
    }

    /***
     * Constructor
     *
     * @param message   descriptive message
     * @param errorCode one of the above defined constants that classifies the error.
     */
    public AndesException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    /***
     * Constructor
     *
     * @param message descriptive message
     * @param cause   reference to the exception for reference.
     */
    public AndesException(String message, Throwable cause) {
        super(message, cause);
    }

    /***
     * Constructor
     *
     * @param message   descriptive message
     * @param errorCode one of the above defined constants that classifies the error.
     * @param cause     reference to the exception for reference.
     */
    public AndesException(String message, String errorCode, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /***
     * Constructor
     *
     * @param cause reference to the exception for reference.
     */
    public AndesException(Throwable cause) {
        super(cause);
    }

    /***
     * One of the above defined constants that classifies the error. e.g.- MESSAGE_CONTENT_OBSOLETE
     *
     * @return
     */
    public String getErrorCode() {
        return errorCode;
    }

}
