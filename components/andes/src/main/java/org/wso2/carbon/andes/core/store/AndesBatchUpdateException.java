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

import java.util.Collections;
import java.util.List;

/**
 * Most of the time {@link MessageStore} implementations insert/delete/update
 * data as batches. if an error occurs doing a batch operation this exception should
 * be thrown.
 *
 * @see FailureObservingMessageStore
 * @see FailureObservingAndesContextStore
 */
public class AndesBatchUpdateException extends AndesException {

    /**
     * The serial version id.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Contains failed data objects during the batch operation.
     */
    private List<?> failedBatches;

    /**
     * Contains succeeded data objects during the batch operation.
     */
    private List<?> successfullInserts;

    /***
     * Constructor
     *
     * @param message           descriptive message
     * @param errorCode         one of the above defined constants that classifies the error.
     * @param cause             reference to the exception for reference.
     * @param failedBatches     data objects that failed
     * @param successfulBatches data objects that succeeded.
     */
    public <T> AndesBatchUpdateException(String message, String errorCode,
                                         Throwable cause, List<T> failedBatches,
                                         List<T> successfulBatches) {
        super(message, errorCode, cause);
        this.failedBatches = failedBatches;
        this.successfullInserts = successfulBatches;
    }

    /***
     * Constructor
     *
     * @param message   descriptive message
     * @param errorCode one of the above defined constants that classifies the error.
     * @param cause     reference to the exception for reference.
     */
    public <T> AndesBatchUpdateException(String message, String errorCode,
                                         Throwable cause) {
        this(message, errorCode, cause, Collections.emptyList(), Collections.emptyList());
    }


    /**
     * Returns the list of failed data objects.
     *
     * @return a list
     */
    public List<?> getFailedBatches() {
        return failedBatches;
    }

    /***
     * Returns the list of successfully data objects
     *
     * @return a list
     */
    public List<?> getSuccessfullBatches() {
        return successfullInserts;
    }
}
