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

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.wso2.carbon.andes.core.internal.configuration.AndesConfigurationManager;
import org.wso2.carbon.andes.core.internal.configuration.enums.AndesConfiguration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * This class keeps track of all the {@link StoreHealthListener}s and notifies when
 * store becomes (non-)operational.
 */
public class FailureObservingStoreManager {


    /**
     * All the {@link StoreHealthListener} implementations registered to receive
     * callbacks.
     */
    private static Collection<StoreHealthListener> healthListeners =
            Collections.synchronizedCollection(new ArrayList<StoreHealthListener>());

    /**
     * A named thread factory build executor.
     */
    private static final ThreadFactory namedThreadFactory =
            new ThreadFactoryBuilder().setNameFormat("FailureObservingStores-HealthCheckPool").build();

    /**
     * Executor used for health checking
     */
    private static final ScheduledExecutorService executor = Executors.newScheduledThreadPool(2, namedThreadFactory);

    /**
     * A Private constructor
     */
    private FailureObservingStoreManager() {
    }


    /**
     * Registers specified {@link StoreHealthListener} to receive events.
     *
     * @param healthListener
     */
    public static void registerStoreHealthListener(StoreHealthListener healthListener) {
        healthListeners.add(healthListener);
    }

    /**
     * Schedules a health check task for a non-operational for a {@link HealthAwareStore}
     * Note: visibility is at package level which is intentional.
     *
     * @param store which became in-operational
     * @return a future referring to the periodic health check task.
     */
    static ScheduledFuture<?> scheduleHealthCheckTask(HealthAwareStore store) {

        int taskDelay = AndesConfigurationManager.readValue(
                AndesConfiguration.PERSISTENCE_STORE_HEALTH_CHECK_INTERVAL);
        //initial delay and period interval is set to same for the sake of simplicity
        StoreHealthCheckTask healthCheckTask = new StoreHealthCheckTask(store, healthListeners);
        return executor.scheduleWithFixedDelay(healthCheckTask, taskDelay,
                                               taskDelay, TimeUnit.SECONDS);
    }

    /**
     * A utility method to broadcast that a store became non-operational.
     * TODO: Name Change
     *
     * @param e                error occurred
     * @param healthAwareStore in-operational store
     */
    static synchronized void notifyStoreNonOperational(AndesStoreUnavailableException e,
                                                       HealthAwareStore healthAwareStore) {
        // this is the first failure
        for (StoreHealthListener listener : healthListeners) {
            listener.storeNonOperational(healthAwareStore, e);
        }
    }

    /**
     * A utility method to broadcast that a store became operational.
     *
     * @param healthAwareStore the store which became operational.
     */
    static synchronized void notifyStoreOperational(HealthAwareStore healthAwareStore) {
        // this is the first failure
        for (StoreHealthListener listener : healthListeners) {
            listener.storeOperational(healthAwareStore);
        }
    }

    /**
     * Stop the scheduled tasks which are checking for message stores availability.
     */
    /*package*/
    static void close() {
        executor.shutdown();
    }
}
