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

package org.wso2.carbon.andes.core.internal.cluster.error.detection;

/**
 * Implementation of a {@link NetworkPartitionDetector} which does nothing.
 * Used when network partition detection is not enabled in broker.xml
 */
public class DisabledNetworkPartitionDetector implements NetworkPartitionDetector {

    /**
     * {@inheritDoc}
     */
    @Override
    public void start() {
        // Do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void memberAdded(Object member) {
        // Do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void memberRemoved(Object member) {
        // Do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void networkPatitionMerged() {
        // Do nothing
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addNetworkPartitionListener(NetworkPartitionListener listner) {
        // Do nothing

    }

}
