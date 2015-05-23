/**
 *  Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.wso2.carbon.cassandra.datareader.cql;

/**
 * RDBMS data source constants.
 */
public class CassandraDataSourceConstants {

    public static final String CASSANDRA_DATASOURCE_TYPE = "CASSANDRA";
    public static final long delayMs = 200l;
    public static final long baseDelayMs = 200l;
    public static final long maxDelayMs = 200l;
    public static final String CLUSTER_MODE = "Cluster";
    public static final String SESSION_MODE = "Session";

    /**
     * Cassandra CQL RetryPolicy
     */
    public enum RetryPolicy {
        DefaultRetryPolicy, DowngradingConsistencyRetryPolicy, FallthroughRetryPolicy, LoggingRetryPolicy;
    }

    /**
     * Cassandra CQL LoadBalancingPolicy
     */
    public enum LoadBalancingPolicy {
        DCAwareRoundRobinPolicy, LatencyAwarePolicy, RoundRobinPolicy;
    }

    /**
     * Cassandra CQL ReconnectionPolicy
     */
    public enum ReconnectionPolicy {
        ConstantReconnectionPolicy, ExponentialReconnectionPolicy;
    }

}
