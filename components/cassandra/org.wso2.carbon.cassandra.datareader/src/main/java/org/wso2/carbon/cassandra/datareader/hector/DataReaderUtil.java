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
package org.wso2.carbon.cassandra.datareader.hector;

import me.prettyprint.cassandra.connection.DynamicLoadBalancingPolicy;
import me.prettyprint.cassandra.connection.LeastActiveBalancingPolicy;
import me.prettyprint.cassandra.connection.LoadBalancingPolicy;
import me.prettyprint.cassandra.connection.RoundRobinBalancingPolicy;
import me.prettyprint.cassandra.connection.factory.HKerberosSaslThriftClientFactoryImpl;
import me.prettyprint.cassandra.service.CassandraHostConfigurator;
import org.wso2.carbon.ndatasource.common.DataSourceException;
import org.wso2.carbon.utils.CarbonUtils;

import javax.xml.bind.JAXBContext;
import java.io.ByteArrayInputStream;

/**
 * Hector data reader util
 */
public class DataReaderUtil {

    /**
     * Create cassandra host configurator
     *
     * @param config HectorConfiguration
     * @return CassandraHostConfigurator
     */
    public static CassandraHostConfigurator createCassandraHostConfigurator(
            HectorConfiguration config) {
        String hosts = config.getHosts();
        CassandraHostConfigurator configurator = new CassandraHostConfigurator(hosts);
        configurator.setAutoDiscoverHosts(config.getAutoDiscoverHosts());
        if (config.getAutoDiscoverDelayInSeconds() != -1) {
            configurator.setAutoDiscoveryDelayInSeconds(config.getAutoDiscoverDelayInSeconds());
        }
        if (config.getCassandraThriftSocketTimeout() != -1) {
            configurator.setCassandraThriftSocketTimeout(config.getCassandraThriftSocketTimeout());
        }
        if (config.getHostTimeoutCounter() != -1) {
            configurator.setHostTimeoutCounter(config.getHostTimeoutCounter());
        }
        if (config.getHostTimeoutSuspensionDurationInSeconds() != -1) {
            configurator.setHostTimeoutSuspensionDurationInSeconds(
                    config.getHostTimeoutSuspensionDurationInSeconds());
        }
        if (config.getHostTimeoutUnsuspendCheckDelay() != -1) {
            configurator.setHostTimeoutUnsuspendCheckDelay(
                    config.getHostTimeoutUnsuspendCheckDelay());
        }
        if (config.getHostTimeoutWindow() != -1) {
            configurator.setHostTimeoutWindow(config.getHostTimeoutWindow());
        }
        if (config.getMaxActive() != -1) {
            configurator.setMaxActive(config.getMaxActive());
        }
        if (config.getMaxConnectTimeMillis() != -1) {
            configurator.setMaxConnectTimeMillis(config.getMaxConnectTimeMillis());
        }
        if (config.getMaxWaitTimeWhenExhausted() != -1) {
            configurator.setMaxWaitTimeWhenExhausted(config.getMaxWaitTimeWhenExhausted());
        }
        configurator.setRetryDownedHosts(config.getRetryDownedHosts());
        configurator.setRetryDownedHostsDelayInSeconds(config.getRetryDownedHostsDelayInSeconds());
        configurator.setRunAutoDiscoveryAtStartup(config.getRunAutoDiscoveryAtStartup());
        configurator.setUseSocketKeepalive(config.getUseSocketKeepalive());
        configurator.setUseHostTimeoutTracker(config.getUseHostTimeoutTracker());
        if (config.getLoadBalancingPolicy() != null) {
            configurator.setLoadBalancingPolicy(
                    getLoadBalancingPolicy(config.getLoadBalancingPolicy()));
        }
        if (config.isEnableSecurity()) {
            configurator.setClientFactoryClass(HKerberosSaslThriftClientFactoryImpl.class.getName());
        }
        configurator.setLifo(config.getLifo());
        return configurator;
    }

    /**
     * Hector configuration load from given xml data source configuration
     *
     * @param xmlConfiguration xml configuration of data source
     * @return HectorConfiguration
     * @throws DataSourceException
     */
    public static HectorConfiguration loadConfig(String xmlConfiguration)
            throws DataSourceException {
        try {
            xmlConfiguration = CarbonUtils
                    .replaceSystemVariablesInXml(xmlConfiguration);
            JAXBContext ctx = JAXBContext
                    .newInstance(HectorConfiguration.class);
            return (HectorConfiguration) ctx.createUnmarshaller().unmarshal(
                    new ByteArrayInputStream(xmlConfiguration.getBytes()));
        } catch (Exception e) {
            throw new DataSourceException("Error in loading Cassandra configuration: " +
                    e.getMessage(), e);
        }
    }

    /**
     * Load balancing policy of hector configuration
     *
     * @param policy HectorConfiguration.LoadBalancingPolicy
     * @return LoadBalancingPolicy
     */
    public static LoadBalancingPolicy getLoadBalancingPolicy(
            HectorConfiguration.LoadBalancingPolicy policy) {
        switch (policy) {
            case DYNAMIC:
                return new DynamicLoadBalancingPolicy();
            case ROUND_ROBIN:
                return new RoundRobinBalancingPolicy();
            case LEAST_ACTIVE:
                return new LeastActiveBalancingPolicy();
            default:
                return new RoundRobinBalancingPolicy();
        }
    }

}
