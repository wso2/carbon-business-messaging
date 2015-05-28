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
package org.wso2.carbon.cassandra.datareader.cql.util;

import java.util.concurrent.TimeUnit;

import org.wso2.carbon.cassandra.datareader.cql.CassandraConfiguration;
import org.wso2.carbon.cassandra.datareader.cql.CassandraConfiguration.LoadBalancingPolicyOptions;
import org.wso2.carbon.cassandra.datareader.cql.CassandraConfiguration.ReconnectPolicyOptions;
import org.wso2.carbon.cassandra.datareader.cql.CassandraDataSourceConstants;

import com.datastax.driver.core.Cluster.Builder;
import com.datastax.driver.core.HostDistance;
import com.datastax.driver.core.PoolingOptions;
import com.datastax.driver.core.SocketOptions;
import com.datastax.driver.core.policies.ConstantReconnectionPolicy;
import com.datastax.driver.core.policies.DCAwareRoundRobinPolicy;
import com.datastax.driver.core.policies.DefaultRetryPolicy;
import com.datastax.driver.core.policies.DowngradingConsistencyRetryPolicy;
import com.datastax.driver.core.policies.ExponentialReconnectionPolicy;
import com.datastax.driver.core.policies.FallthroughRetryPolicy;
import com.datastax.driver.core.policies.LatencyAwarePolicy;
import com.datastax.driver.core.policies.RoundRobinPolicy;

public class CassandraDatasourceUtils {

    public static boolean isPropertyEmpty(String value) {
        return value == null || value.trim().length() == 0;
    }

    public static boolean isPropertyEmpty(Boolean value) {
        return value == null;
    }

    public static boolean isPropertyEmpty(Number value) {
        return value == null || value.intValue() == 0;
    }

    public static PoolingOptions createPoolingOptions(CassandraConfiguration config) {

        CassandraConfiguration.PoolingOptions poolingConfig = config.getPoolOptions();
        if (poolingConfig == null) {
            return null;
        }
        PoolingOptions pools = new PoolingOptions();

        pools = isPropertyEmpty(poolingConfig.getCoreConnectionsForLocal()) ? pools : pools.setCoreConnectionsPerHost(
                HostDistance.LOCAL, poolingConfig.getCoreConnectionsForLocal());
        pools = isPropertyEmpty(poolingConfig.getCoreConnectionsForRemote()) ? pools : pools.setCoreConnectionsPerHost(
                HostDistance.REMOTE, poolingConfig.getCoreConnectionsForRemote());

        pools = isPropertyEmpty(poolingConfig.getMaxConnectionsForLocal()) ? pools : pools.setMaxConnectionsPerHost(
                HostDistance.LOCAL, poolingConfig.getMaxConnectionsForLocal());
        pools = isPropertyEmpty(poolingConfig.getMaxConnectionsForRemote()) ? pools : pools.setMaxConnectionsPerHost(
                HostDistance.REMOTE, poolingConfig.getMaxConnectionsForRemote());

        pools = isPropertyEmpty(poolingConfig.getMinSimultaneousRequestsForLocal()) ? pools : pools
                .setMinSimultaneousRequestsPerConnectionThreshold(HostDistance.LOCAL,
                        poolingConfig.getMinSimultaneousRequestsForLocal());
        pools = isPropertyEmpty(poolingConfig.getMinSimultaneousRequestsForRemote()) ? pools : pools
                .setMinSimultaneousRequestsPerConnectionThreshold(HostDistance.REMOTE,
                        poolingConfig.getMinSimultaneousRequestsForRemote());

        pools = isPropertyEmpty(poolingConfig.getMaxSimultaneousRequestsForLocal()) ? pools : pools
                .setMaxSimultaneousRequestsPerConnectionThreshold(HostDistance.LOCAL,
                        poolingConfig.getMaxSimultaneousRequestsForLocal());
        pools = isPropertyEmpty(poolingConfig.getMaxSimultaneousRequestsForRemote()) ? pools : pools
                .setMaxSimultaneousRequestsPerConnectionThreshold(HostDistance.REMOTE,
                        poolingConfig.getMaxSimultaneousRequestsForRemote());

        return pools;
    }

    public static SocketOptions createSocketOptions(CassandraConfiguration config) {
        CassandraConfiguration.SocketOptions socketConfig = config.getSocketOptions();
        if (socketConfig == null) {
            return null;
        }
        SocketOptions socket = new SocketOptions();

        socket = isPropertyEmpty(socketConfig.getConnectTimeoutMillis()) ? socket : socket
                .setConnectTimeoutMillis(socketConfig.getConnectTimeoutMillis());

        socket = isPropertyEmpty(socketConfig.getKeepAlive()) ? socket : socket.setKeepAlive(socketConfig
                .getKeepAlive());

        socket = isPropertyEmpty(socketConfig.getReadTimeoutMillis()) ? socket : socket
                .setReadTimeoutMillis(socketConfig.getReadTimeoutMillis());

        socket = isPropertyEmpty(socketConfig.getReceiveBufferSize()) ? socket : socket
                .setReceiveBufferSize(socketConfig.getReceiveBufferSize());

        socket = isPropertyEmpty(socketConfig.getReuseAddress()) ? socket : socket.setReuseAddress(socketConfig
                .getReuseAddress());

        socket = isPropertyEmpty(socketConfig.getSendBufferSize()) ? socket : socket.setSendBufferSize(socketConfig
                .getSendBufferSize());

        socket = isPropertyEmpty(socketConfig.getSoLinger()) ? socket : socket.setSoLinger(socketConfig.getSoLinger());

        socket = isPropertyEmpty(socketConfig.getTcpNoDelay()) ? socket : socket.setTcpNoDelay(socketConfig
                .getTcpNoDelay());

        return socket;
    }

    public static void createRetryPolicy(CassandraConfiguration config, Builder builder) {

        String policyName = config.getRetryPolicy();

        if (CassandraDataSourceConstants.RetryPolicy.DowngradingConsistencyRetryPolicy.name().equalsIgnoreCase(
                policyName)) {
            builder.withRetryPolicy(DowngradingConsistencyRetryPolicy.INSTANCE);
        } else if (CassandraDataSourceConstants.RetryPolicy.FallthroughRetryPolicy.name().equalsIgnoreCase(policyName)) {
            builder.withRetryPolicy(FallthroughRetryPolicy.INSTANCE);
        } else {
            builder.withRetryPolicy(DefaultRetryPolicy.INSTANCE);
        }

    }

    public static void createReconnectPolicy(CassandraConfiguration config, Builder builder) {

        ReconnectPolicyOptions policy = config.getReconnectPolicy();

        if (policy == null) {
            builder.withRetryPolicy(DefaultRetryPolicy.INSTANCE);
            return;
        }

        Long constantDelayMs = policy.getDelayMs();
        Long baseDelayMs = policy.getBaseDelayMs();
        Long maxDelayMs = policy.getMaxDelayMs();

        String policyName = policy.getPolicyName();

        if (CassandraDataSourceConstants.ReconnectionPolicy.ConstantReconnectionPolicy.name().equalsIgnoreCase(
                policyName)) {
            ConstantReconnectionPolicy reconnect = new ConstantReconnectionPolicy(
                    isPropertyEmpty(constantDelayMs) ? CassandraDataSourceConstants.delayMs : constantDelayMs);
            builder.withReconnectionPolicy(reconnect);
        } else if (CassandraDataSourceConstants.ReconnectionPolicy.ExponentialReconnectionPolicy.name()
                .equalsIgnoreCase(policyName)) {
            ExponentialReconnectionPolicy reconnect = new ExponentialReconnectionPolicy(
                    isPropertyEmpty(baseDelayMs) ? CassandraDataSourceConstants.baseDelayMs : baseDelayMs,
                    isPropertyEmpty(maxDelayMs) ? CassandraDataSourceConstants.maxDelayMs : maxDelayMs);
            builder.withReconnectionPolicy(reconnect);
        }
    }

    public static void createLoadBalancingPolicy(CassandraConfiguration config, Builder builder) {

        LoadBalancingPolicyOptions policy = config.getLoadBalancePolicy();

        if (policy == null) {
            builder.withRetryPolicy(DefaultRetryPolicy.INSTANCE);
            return;
        }

        String policyName = policy.getPolicyName();
        String localDc = policy.getLocalDc();
        Integer usedHostsPerRemoteDc = policy.getUsedHostsPerRemoteDc();

        Boolean latencyAware = policy.getLatencyAware();

        Long scale = policy.getScale();
        Long retryPeriod = policy.getRetryPeriod();
        Long minMeasure = policy.getMinMeasure();
        Double exclusionThreshold = policy.getExclusionThreshold();

        if (CassandraDataSourceConstants.LoadBalancingPolicy.DCAwareRoundRobinPolicy.name()
                .equalsIgnoreCase(policyName)) {

            DCAwareRoundRobinPolicy type = (!isPropertyEmpty(localDc) && !isPropertyEmpty(usedHostsPerRemoteDc) ? new DCAwareRoundRobinPolicy(
                    localDc, usedHostsPerRemoteDc) : new DCAwareRoundRobinPolicy(localDc));

            if (latencyAware) {
                LatencyAwarePolicy.Builder latencyBuilder = LatencyAwarePolicy.builder(type);
                latencyBuilder = isPropertyEmpty(scale) ? latencyBuilder : latencyBuilder.withScale(scale, TimeUnit.MILLISECONDS);
                latencyBuilder = isPropertyEmpty(retryPeriod) ? latencyBuilder : latencyBuilder.withRetryPeriod(retryPeriod, TimeUnit.MILLISECONDS);
                latencyBuilder = isPropertyEmpty(minMeasure) ? latencyBuilder : latencyBuilder.withMininumMeasurements(minMeasure.intValue());
                latencyBuilder = isPropertyEmpty(exclusionThreshold) ? latencyBuilder : latencyBuilder.withExclusionThreshold(exclusionThreshold);
                builder.withLoadBalancingPolicy(latencyBuilder.build());
            } else {
                builder.withLoadBalancingPolicy(type);
            }

        } else {

            RoundRobinPolicy type = new RoundRobinPolicy();

            if (latencyAware) {
                LatencyAwarePolicy.Builder latencyBuilder = LatencyAwarePolicy.builder(type);
                latencyBuilder = isPropertyEmpty(scale) ? latencyBuilder : latencyBuilder.withScale(scale, TimeUnit.MILLISECONDS);
                latencyBuilder = isPropertyEmpty(retryPeriod) ? latencyBuilder : latencyBuilder.withRetryPeriod(retryPeriod, TimeUnit.MILLISECONDS);
                latencyBuilder = isPropertyEmpty(minMeasure) ? latencyBuilder : latencyBuilder.withMininumMeasurements(minMeasure.intValue());
                latencyBuilder = isPropertyEmpty(exclusionThreshold) ? latencyBuilder : latencyBuilder.withExclusionThreshold(exclusionThreshold);
                builder.withLoadBalancingPolicy(latencyBuilder.build());
            } else {
                builder.withLoadBalancingPolicy(type);
            }


        }
    }
}
