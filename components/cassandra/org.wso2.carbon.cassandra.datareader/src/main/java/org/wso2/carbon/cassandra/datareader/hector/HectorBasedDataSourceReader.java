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

import me.prettyprint.cassandra.service.CassandraHostConfigurator;
import me.prettyprint.hector.api.Cluster;
import me.prettyprint.hector.api.factory.HFactory;
import org.wso2.carbon.ndatasource.common.DataSourceException;
import org.wso2.carbon.ndatasource.common.spi.DataSourceReader;

import java.util.HashMap;
import java.util.Map;

/**
 * Create hector data source and initialize cassandra cluster
 */
public class HectorBasedDataSourceReader implements DataSourceReader {

    /**
     * Get data source type - HECTOR
     *
     * @return data source type
     */
    @Override
    public String getType() {
        return DataReaderConstants.DATASOURCE_TYPE;
    }

    /**
     * Create data source by given xml configuration
     *
     * @param xmlConfig                    data source xml configuration
     * @param isDataSourceFactoryReference isDataSourceFactoryReference
     * @return Cluster object
     * @throws DataSourceException
     */
    @Override
    public Object createDataSource(String xmlConfig, boolean isDataSourceFactoryReference) throws DataSourceException {
        try {
            HectorConfiguration config = DataReaderUtil.loadConfig(xmlConfig);
            return this.initCluster(config);
        } catch (Exception ex) {
            throw new DataSourceException(ex);
        }
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

    /**
     * Initialize cluster object by HectorConfiguration
     *
     * @param config HectorConfiguration
     * @return Cluster
     */
    private Cluster initCluster(HectorConfiguration config) {
        Map<String, String> credentials = new HashMap<String, String>();
        credentials.put(DataReaderConstants.USERNAME, config.getUsername());
        credentials.put(DataReaderConstants.PASSWORD, config.getPassword());

        CassandraHostConfigurator configurator =
                DataReaderUtil.createCassandraHostConfigurator(config);
        return HFactory.createCluster(config.getClusterName(), configurator, credentials);
    }

}
