/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.broker.test.constants;

/**
 * Will be used to hold prepared statements related to tests
 */
public class RDBMSTestConstants {

    /**
     * Holds the mapping between the queue name and the id
     */
    private static final String QUEUES_TABLE = "MB_QUEUE_MAPPING";

    /**
     * The name of the queue
     */
    protected static final String QUEUE_NAME = "QUEUE_NAME";

    /**
     * Produces the number of queues present this query is used on the following tests
     *
     * @see org.wso2.broker.test.store.MessageStoreTestQueries#queueExists(String)
     */
    public static final String PS_SELECT_QUEUE_COUNT =
            "SELECT count(*) FROM " + QUEUES_TABLE + " WHERE " + QUEUE_NAME + " = ?";

}
