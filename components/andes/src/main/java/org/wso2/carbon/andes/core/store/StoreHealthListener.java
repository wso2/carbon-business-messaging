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

/**
 * Defines contractual obligations for any party interested in knowing
 * when a {@link HealthAwareStore} becomes operational or offline.
 */
public interface StoreHealthListener {

    /**
     * Invoked when specified store becomes non-operational
     *
     * @param store the store which went offline.
     * @param ex    exception occurred.
     */
    public void storeNonOperational(HealthAwareStore store, Exception ex);

    /**
     * Invoked when specified store becomes operational
     *
     * @param store Reference to the operational store
     */
    public void storeOperational(HealthAwareStore store);

}
