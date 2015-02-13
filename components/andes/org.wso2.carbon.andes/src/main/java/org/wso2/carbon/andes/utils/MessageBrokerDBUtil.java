/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.andes.utils;

import org.apache.commons.dbcp.BasicDataSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.andes.service.exception.ConfigurationException;
import org.wso2.carbon.utils.CarbonUtils;

import javax.sql.DataSource;
import java.io.File;
import java.util.HashMap;

/**
 * Connect to an external RDBMS store automatically through -Dsetup feature
 * of wso2server startup script
 */
public final class MessageBrokerDBUtil {


    private boolean mbStoreInitialized;
    private static volatile DataSource dataSource = null;
    private static final Log log = LogFactory.getLog(MessageBrokerDBUtil.class);

    private static final String MASTER_DATASOURCE_FILE_NAME = "master-datasources.xml";
    private static final String DB_DRIVER    = "driverClassName";
    private static final String DB_URL       = "url";
    private static final String DB_PASSWORD  = "password";
    private static final String DB_USERNAME  = "username";
    private static final String DB_CHECK_SQL = "SELECT * FROM MB_QUEUE_COUNTER";

    /**
     * Initializes the RDBMS message store database with configurations given in master-datasources.xml
     *
     * @throws ConfigurationException if an error occurs while loading DB configuration
     */
    public void initialize() throws ConfigurationException {

        String dSetupValue = System.getProperty("setup");

        if (dSetupValue != null) {
            String filePath = CarbonUtils.getCarbonHome() + File.separator + "repository" +
                    File.separator + "conf" + File.separator + "datasources" +
                    File.separator + MASTER_DATASOURCE_FILE_NAME;

            log.info("Initializing message broker RDBMS store...");

            DataSourceConfiguration configuration = new DataSourceConfiguration();
            if (!mbStoreInitialized) {
                configuration.loadDbConfiguration(filePath);
                mbStoreInitialized = true;
            }

            setMBStoreRdbmsConfiguration(configuration);
            setupMBStoreRdbmsDatabase();
        }
    }

    private void setupMBStoreRdbmsDatabase() throws ConfigurationException {

        LocalDatabaseCreator databaseCreator = new LocalDatabaseCreator(dataSource);

        try {

            if (!databaseCreator.isDatabaseStructureCreated(DB_CHECK_SQL)) {
                databaseCreator.createRegistryDatabase();
            } else {
                log.info("Message Broker database store already exists. Not creating a new database.");
            }

        } catch (Exception e) {
            log.error("Unexpected error occurred while parsing configuration: " + e.getMessage());
            throw new ConfigurationException("Unexpected error occurred while parsing configuration: ", e);
        }

        if(databaseCreator.isDatabaseStructureCreated(DB_CHECK_SQL)) {
            log.info("Successfully sourced relevant sql files to database.");
        } else {
            log.error("Unable to read sourced database tables. Database not successfully initialized.");
        }


    }


    private void setMBStoreRdbmsConfiguration(DataSourceConfiguration configuration) throws ConfigurationException {

        HashMap dbConfigurationMap = configuration.getConfigurationMap();

        String driver   = dbConfigurationMap.get(DB_DRIVER).toString();
        String dbUrl    = dbConfigurationMap.get(DB_URL).toString();
        String username = dbConfigurationMap.get(DB_USERNAME).toString();
        String password = dbConfigurationMap.get(DB_PASSWORD).toString();

        if (log.isDebugEnabled()) {
            log.debug("Initializing data source configurations for MB RDBMS store");
            log.debug("Driver Configurations :");
            log.debug(DB_DRIVER + " : " + driver);
            log.debug(DB_URL + " : " + dbUrl);
            log.debug(DB_USERNAME + " : " + username);
            log.debug(DB_PASSWORD + " : " + password);
        }

        if(dbUrl == null) {
            log.warn("Required database url unspecified. So Message Broker RDBMS Store " +
                    "will not work as expected.");
        }
        if(driver == null) {
            log.warn("Required database driver unspecified. So Message Broker RDBMS Store " +
                    "will not work as expected.");
        }
        if(username == null) {
            log.warn("Required database username unspecified. So Message Broker RDBMS Store " +
                    "will not work as expected.");
        }
        if(password == null) {
            log.warn("Required database password unspecified. So Message Broker RDBMS Store " +
                    "will not work as expected.");
        }

        BasicDataSource basicDataSource = new BasicDataSource();
        basicDataSource.setDriverClassName(driver);
        basicDataSource.setUrl(dbUrl);
        basicDataSource.setUsername(username);
        basicDataSource.setPassword(password);

        dataSource = basicDataSource;

    }


}
