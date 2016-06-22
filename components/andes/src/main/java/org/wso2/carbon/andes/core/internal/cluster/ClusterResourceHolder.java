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
package org.wso2.carbon.andes.core.internal.cluster;

import org.wso2.carbon.andes.core.AndesRecoveryTask;
import org.wso2.carbon.andes.core.AndesSubscriptionManager;
import org.wso2.carbon.andes.core.MessageExpirationWorker;
import org.wso2.carbon.andes.core.internal.transport.ConfigSynchronizer;

/**
 * Class <code>ClusterResourceHolder</code> holds the Cluster implementation specific
 * Objects.This act as a Singleton object holder for that objects with in the broker.
 */
public class ClusterResourceHolder {

    private static ClusterResourceHolder resourceHolder;

    /**
     * Holds SubscriptionManager
     */
    private AndesSubscriptionManager subscriptionManager;


    /**
     * Holds Config Synchroniser
     */
    private ConfigSynchronizer configSynchronizer;


    /**
     * Holds a reference to andes recovery task
     */
    private AndesRecoveryTask andesRecoveryTask;

    /**
     * holds cluster manager
     */
    private ClusterManager clusterManager;
    private MessageExpirationWorker messageExpirationWorker;

    private ClusterResourceHolder() {

    }


    public static ClusterResourceHolder getInstance() {
        if (resourceHolder == null) {

            synchronized (ClusterResourceHolder.class) {
                if (resourceHolder == null) {
                    resourceHolder = new ClusterResourceHolder();
                }
            }
        }

        return resourceHolder;
    }

    public AndesSubscriptionManager getSubscriptionManager() {
        return subscriptionManager;
    }

    public void setSubscriptionManager(AndesSubscriptionManager subscriptionManager) {
        this.subscriptionManager = subscriptionManager;
    }

    public void setClusterManager(ClusterManager clusterManager) {
        this.clusterManager = clusterManager;
    }

    public ClusterManager getClusterManager() {
        return this.clusterManager;
    }

    public ConfigSynchronizer getConfigSynchronizer() {
        return configSynchronizer;
    }

    public void setConfigSynchronizer(ConfigSynchronizer configSynchronizer) {
        this.configSynchronizer = configSynchronizer;
    }

    public AndesRecoveryTask getAndesRecoveryTask() {
        return andesRecoveryTask;
    }

    public void setAndesRecoveryTask(AndesRecoveryTask andesRecoveryTask) {
        this.andesRecoveryTask = andesRecoveryTask;
    }

    public MessageExpirationWorker getMessageExpirationWorker() {
        return messageExpirationWorker;
    }

    public void setMessageExpirationWorker(MessageExpirationWorker messageExpirationWorker) {
        this.messageExpirationWorker = messageExpirationWorker;
    }
}
