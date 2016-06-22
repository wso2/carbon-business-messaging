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

package org.wso2.carbon.andes.core.store.rdbms;

import org.apache.log4j.Logger;
import org.wso2.carbon.andes.core.AndesException;
import org.wso2.carbon.andes.core.internal.configuration.util.ConfigurationProperties;
import org.wso2.carbon.andes.core.store.DurableStoreConnection;

import java.sql.Connection;
import java.sql.SQLException;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

/**
 * JDBC connection class. Connection is made using the jndi lookup name provided and connection
 * pooled data source is used to create new connections
 */
public class RDBMSConnection extends DurableStoreConnection {

    private static final Logger logger = Logger.getLogger(RDBMSConnection.class);
    private DataSource datasource;

    @Override
    public void initialize(ConfigurationProperties connectionProperties) throws AndesException {

        super.initialize(connectionProperties);

        Connection connection = null;
        String jndiLookupName = "";
        String dataSourceUserName = "";
        try {
            // try to get the lookup name. If error empty string will be returned
            jndiLookupName = connectionProperties.getProperty(RDBMSConstants.PROP_JNDI_LOOKUP_NAME);
            datasource = InitialContext.doLookup(jndiLookupName);

            // TODO: C5-migration
//            if (datasource instanceof org.apache.tomcat.jdbc.pool.DataSource) {
//                org.apache.tomcat.jdbc.pool.DataSource tcDataSource =
//                        (org.apache.tomcat.jdbc.pool.DataSource) datasource;
//                if (StringUtils.isNotBlank(tcDataSource.getUsername())) {
//                    dataSourceUserName = tcDataSource.getUsername();
//                }
//            }

            connection = datasource.getConnection();
            logger.info("JDBC connection established with jndi config " + jndiLookupName);
        } catch (SQLException e) {
            throw new AndesException("Connecting to database failed with jndi lookup : " +
                                             jndiLookupName  /* + ". data source username : " +
                                     dataSourceUserName + ". SQL Error message : " +
                                     e.getMessage()*/, e);
        } catch (NamingException e) {
            throw new AndesException("Couldn't look up jndi entry for " +
                                             "\"" + jndiLookupName + "\"", e);
        } finally {
            String task = "Initialising database";
            close(connection, task);
        }
    }

    /**
     * connection pooled data source object is returned. Connections to database can be created
     * using the data source.
     *
     * @return DataSource
     */
    public DataSource getDataSource() {
        return datasource;
    }

    @Override
    public void close() {
        // nothing to do.
    }

    @Override
    public Object getConnection() {
        return this;
    }

    /**
     * Closes the provided connection. on failure log the error;
     *
     * @param connection Connection
     * @param task       task that was done before closing
     */
    private void close(Connection connection, String task) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                logger.error("Failed to close connection after " + task, e);
            }
        }
    }
}
