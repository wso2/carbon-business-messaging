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

package org.wso2.broker.test.providers;

import org.testng.annotations.DataProvider;

/**
 * Holds the list of data which will be used to unit test RDBMStore
 */
public class QueueContextDataProvider {

    /**
     * <p>
     * Returns the list of values which should be used for testing insertion of queues
     * </p>
     * <p>
     * The list of values which will be returned will hold the name and the validity of the name i.e whether the
     * value is acceptable or not. If the value is acceptable this will be true else it will be false.
     * <p>
     * Hence the test case should validate whether the given value is accepted or not by the function against it's
     * validity
     * </p>
     *
     * @return the list of queue names and the validity of the names
     */
    @DataProvider(name = "QueueNames")
    public static Object[][] getQueueNames() {
        return new Object[][]{{new String("testQueue"), new Boolean(true)}};
    }
}
