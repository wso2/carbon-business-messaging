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

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.cassandra.datareader.cql.util.CassandraDatasourceUtils;
import org.wso2.carbon.ndatasource.common.DataSourceException;
import org.wso2.carbon.ndatasource.common.spi.DataSourceReader;
import org.wso2.carbon.utils.CarbonUtils;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Cluster.Builder;
import com.datastax.driver.core.Host;
import com.datastax.driver.core.PoolingOptions;
import com.datastax.driver.core.ProtocolOptions;
import com.datastax.driver.core.SocketOptions;
import com.datastax.driver.core.exceptions.NoHostAvailableException;

/**
 * This class represents the Cassandra based data source reader implementation.
 */
public class CassandraDataSourceReader implements DataSourceReader {

    private static final Log log = LogFactory.getLog(CassandraDataSourceReader.class);

    /**
     * Get data source type - CASSANDRA
     *
     * @return data source type
     */
    @Override
    public String getType() {
        return CassandraDataSourceConstants.CASSANDRA_DATASOURCE_TYPE;
    }

    /**
     * CQL configuration load from given xml data source configuration
     *
     * @param xmlConfiguration xml configuration of data source
     * @return CassandraConfiguration
     * @throws DataSourceException
     */
    public static CassandraConfiguration loadConfig(String xmlConfiguration)
            throws DataSourceException {
        try {
            xmlConfiguration = CarbonUtils
                    .replaceSystemVariablesInXml(xmlConfiguration);
            JAXBContext ctx = JAXBContext
                    .newInstance(CassandraConfiguration.class);
            return (CassandraConfiguration) ctx.createUnmarshaller().unmarshal(
                    new ByteArrayInputStream(xmlConfiguration.getBytes()));
        } catch (Exception e) {
            throw new DataSourceException("Error in loading Cassandra configuration: " + e.getMessage(), e);
        }
    }

    /**
     * Create data source by given xml configuration
     *
     * @param xmlConfiguration             data source xml configuration
     * @param isDataSourceFactoryReference isDataSourceFactoryReference
     * @return Cluster
     * @throws DataSourceException
     */
    @Override
    public Object createDataSource(String xmlConfiguration,
                                   boolean isDataSourceFactoryReference) throws DataSourceException {
        try {
            CassandraConfiguration config = loadConfig(xmlConfiguration);
            Cluster cluster = createCluster(config);
            if (CassandraDataSourceConstants.SESSION_MODE.equalsIgnoreCase(config.getMode()) && (config.getKeysapce() != null && config.getKeysapce().trim().length() > 0)) {
                return cluster.connect(config.getKeysapce());
            }
            return cluster;
        } catch (Exception ex) {
            throw new DataSourceException(ex);
        }

    }

    /**
     * Initialize cluster object by given CassandraConfiguration
     *
     * @param config CassandraConfiguration
     * @return Cluster
     * @throws DataSourceException
     */
    public static Cluster createCluster(CassandraConfiguration config)
            throws DataSourceException {

        boolean debugEnabled = false;
        if (log.isDebugEnabled()) {
            debugEnabled = true;
        }

        String userName = config.getUsername();
        String password = config.getPassword();
        List<String> connections = config.getHosts();
        String clusterName = config.getClusterName();
        int port = config.getPort();
        boolean jmxDisabled = config.getJmxDisabled();

        if (userName == null || password == null) {
            throw new DataSourceException("Can't create cluster with empty userName or Password");
        }

        if (clusterName == null) {
            throw new DataSourceException("Can't create cluster with empty cluster name");
        }

        if (connections == null || connections.isEmpty()) {
            throw new DataSourceException("Can't create cluster with empty connection string");
        }

        int maxConnections = config.getMaxConnections();
        int concurrency = config.getConcurrency();
        boolean async = config.getAsync();
        String compression = config.getCompression();

        StringBuilder configProps = new StringBuilder();
        configProps
                .append("  concurrency:          " + concurrency)
                .append("\n  mode:                 "
                        + (async ? "asynchronous" : "blocking"))
                .append("\n  per-host connections: " + maxConnections)
                .append("\n  compression:          " + compression);

        if (debugEnabled) {
            log.debug(configProps.toString());
        }

        Cluster cluster = null;

        try {
            PoolingOptions poolOptions = CassandraDatasourceUtils.createPoolingOptions(config);
            SocketOptions socketOptions = CassandraDatasourceUtils.createSocketOptions(config);

            // Create cluster

            Builder builder = Cluster.builder();

            for (String con : connections) {
                builder.addContactPoints(con);
            }
            builder.withPoolingOptions(poolOptions)
                    .withSocketOptions(socketOptions)
                    .withPort(port).withCredentials(userName, password);

            CassandraDatasourceUtils.createRetryPolicy(config, builder);
            CassandraDatasourceUtils.createReconnectPolicy(config, builder);
            CassandraDatasourceUtils.createLoadBalancingPolicy(config, builder);
            if (jmxDisabled) {
                builder.withoutMetrics();
                builder.withoutJMXReporting();
            }

            if (ProtocolOptions.Compression.SNAPPY.name().equalsIgnoreCase(compression)) {
                builder.withCompression(ProtocolOptions.Compression.SNAPPY);
            } else {
                builder.withCompression(ProtocolOptions.Compression.NONE);
            }

            cluster = builder.build();

            // validate cluster information after connect
            StringBuilder metaDataAfterConnect = new StringBuilder();
            Set<Host> allHosts = cluster.getMetadata().getAllHosts();
            for (Host h : allHosts) {
                metaDataAfterConnect.append("[");
                metaDataAfterConnect.append(h.getDatacenter());
                metaDataAfterConnect.append("-");
                metaDataAfterConnect.append(h.getRack());
                metaDataAfterConnect.append("-");
                metaDataAfterConnect.append(h.getAddress());
                metaDataAfterConnect.append("]\n");
            }

            if (debugEnabled) {
                log.debug("Cassandra Cluster: " + metaDataAfterConnect.toString());
            }


        } catch (NoHostAvailableException ex) {
            throw new DataSourceException(" No Host available to access ", ex);
        } catch (Exception ex) {
            throw new DataSourceException(" Can not create cluster ", ex);
        }

        return cluster;
    }

    /**
     * Test data source connection
     *
     * @param xmlConfiguration data source string
     * @return status
     * @throws DataSourceException
     */
    @Override
    public boolean testDataSourceConnection(String xmlConfiguration) throws DataSourceException {
        return false;
    }

}
