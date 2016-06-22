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

package org.wso2.carbon.andes.core.store;

import org.wso2.carbon.andes.core.AndesException;

/**
 * Indicates an error situation occurred when a attempted transaction was rejected/rollbacked by database
 */
public class AndesTransactionRollbackException extends AndesException {

    /**
     * The serial version id.
     */
    private static final long serialVersionUID = 1L;

    /***
     * Constructor
     *
     * @param message   descriptive message
     * @param errorCode one of the above defined constants that classifies the error.
     * @param cause     reference to the exception for reference.
     */
    public AndesTransactionRollbackException(String message, String errorCode, Throwable cause) {
        super(message, errorCode, cause);
    }


    /***
     * Constructor
     *
     * @param message descriptive message
     * @param cause   reference to the exception for reference.
     */
    public AndesTransactionRollbackException(String message, Throwable cause) {
        super(message, cause);
    }

}
