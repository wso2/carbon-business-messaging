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
 * Defines contractual obligations for an entity interesting in knowing about
 * network partitions during runtime.
 */
public interface NetworkPartitionListener {

    /**
     * Invoked when a current cluster size becomes less than expected minimum
     *
     * @param currentNodeCount current size of the cluster
     */
    public void minimumNodeCountNotFulfilled(int currentNodeCount);

    /**
     * Invoked when a current cluster size become equal to expected minimum
     *
     * @param currentNodeCount current size of the cluster
     */
    public void minimumNodeCountFulfilled(int currentNodeCount);

}
