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
 * Defines contractual obligations for any network partition detection scheme.
 */
public interface NetworkPartitionDetector {

    /**
     * Meant to be invoked when detection scheme/algorithm should start working.
     * This is typically during the server start up.
     */
    void start();

    /**
     * Invoked when a member/node joins the cluster.
     *
     * @param member information about newly added node.
     */
    void memberAdded(Object member);

    /**
     * Invoked when a member/node leaves the cluster.
     *
     * @param member information about disconnected node.
     */
    void memberRemoved(Object member);

    /**
     * Invoked when clustering mechanism ( / library, i.e. Hazelcast) detects
     * that network partition(s) have been resolved.
     */
    void networkPatitionMerged();

    /**
     * Registers a {@link NetworkPartitionListener} with the scheme.
     *
     * @param listner
     */
    void addNetworkPartitionListener(NetworkPartitionListener listner);

}
