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
import java.util.ArrayList;
import java.util.HashMap;

/**
 * <h1>Initialize MB RDBMS store database</h1>
 * Connect to an external RDBMS store automatically when Dsetup feature enabled
 * with wso2server startup script.
 * This utility class contain methods for following functions.
 * 1. Find rdbms configurations to rdbms data store from external source.
 * 2. Source given sql scripts according to data source configurations.
 * 3. verify database tables are created.
 */
public final class MessageBrokerDBUtil {

    public static final int MESSAGE_STORE_DATA_SOURCE = 0;
    public static final int CONTEXT_STORE_DATA_SOURCE = 1;

    /**
     * log variable for logging.
     */
    private static final Log log =
            LogFactory.getLog(MessageBrokerDBUtil.class);

    /**
     * file name which holds data source configuration.
     */
    private static final String MASTER_DATA_SOURCE_FILE_NAME =
            "master-datasources.xml";

    private static final String DB_DRIVER = "driverClassName";
    private static final String DB_URL = "url";
    private static final String DB_PASSWORD = "password";
    private static final String DB_USERNAME = "username";

    /**
     * sql query to be execute to verify database tables.
     */
    private static final String DB_CHECK_SQL =
            "SELECT * FROM MB_QUEUE_COUNTER";

    /**
     * keep data source configurations.
     */
    private static volatile DataSource messageStoreDataSource = null;

    /**
     * keep context store data source configurations.
     */
    private static volatile DataSource contextStoreDataSource = null;

    /**
     * keep state if database configurations loaded.
     */
    private boolean isMBStoreDatabaseConfigurationLoaded;

    /**
     * keep state if message store configurations set.
     */
    private boolean isMessageStoreDataSourceSet;

    /**
     * keep state if context store configurations set.
     */
    private boolean isContextStoreDatasourceSet;


    /**
     * Creating schema for RDBMS message store with configurations given in
     * master-datasources.xml
     *
     * @throws ConfigurationException if an error occurs while loading DB configuration
     */
    public void initialize() throws ConfigurationException {

        String dSetupValue = System.getProperty("setup");

        if (dSetupValue != null) {
            String filePath = CarbonUtils.getCarbonHome() + File.separator + "repository" +
                              File.separator + "conf" + File.separator + "datasources" +
                              File.separator + MASTER_DATA_SOURCE_FILE_NAME;

            log.info("Creating schema for RDBMS message store.");

            DataSourceConfiguration configuration = new DataSourceConfiguration();
            if (!isMBStoreDatabaseConfigurationLoaded) {
                configuration.loadDbConfiguration(filePath);
                isMBStoreDatabaseConfigurationLoaded = true;
            }

            setMBStoreRdbmsConfiguration(configuration);
            if(isMessageStoreDataSourceSet) {
                setupMBStoreRdbmsDatabase(messageStoreDataSource);
            }
            // message store and context store schemas can source to same database. In that case
            // context store data source won't set.
            if(isContextStoreDatasourceSet) {
                setupMBStoreRdbmsDatabase(contextStoreDataSource);
            }
        }
    }

    /**
     * Based on database configurations create database tables, if tables dose not
     * exist in given database.
     *
     * @throws ConfigurationException
     * @param dataSource holds configuration data source
     */
    private void setupMBStoreRdbmsDatabase(DataSource dataSource) throws ConfigurationException {

        LocalDatabaseCreator databaseCreator = new LocalDatabaseCreator(dataSource);


        try {

            if (!databaseCreator.isDatabaseStructureCreated(DB_CHECK_SQL)) {
                databaseCreator.createRegistryDatabase();
            } else {
                log.info("Message Broker database store already exists." +
                         " Not creating a new database.");
            }

        } catch (ConfigurationException e) {
            log.error("Unexpected error occurred while creating database: ", e);
            throw new ConfigurationException("Unexpected error occurred while " +
                                             " creating database. ", e);
        }

        verifyRdbmsDatabase(databaseCreator);

    }

    /**
     * This method verifies if tables exist in database by executing
     * DB_CHECK_SQL query.
     *
     * @param databaseCreator local database creator instance
     * @throws RuntimeException
     */
    private void verifyRdbmsDatabase(LocalDatabaseCreator databaseCreator) throws RuntimeException {

        if (databaseCreator.isDatabaseStructureCreated(DB_CHECK_SQL)) {
            log.info("Successfully sourced relevant sql files to database.");
        } else {
            log.error("Unable to read sourced database tables. Database not " +
                      " successfully created.");
            throw new RuntimeException("Unable to read sourced database tables. Database not " +
                                       " successfully created.");
        }

    }


    /**
     * Set database configuration parameters to BasicDataSource object.
     *
     * @param configuration data source configurations
     * @throws ConfigurationException
     */
    private void setMBStoreRdbmsConfiguration(DataSourceConfiguration configuration)
            throws ConfigurationException {

        String driver;
        String dbUrl;
        String username;
        String password;

        ArrayList<HashMap<Object, String>> dbConfigurationList = configuration.getConfigurationMap();

        int dbConfigurationNumber = MESSAGE_STORE_DATA_SOURCE;

        for (Object dbConfiguration : dbConfigurationList) {

            HashMap<String, String> dbConfigurationMap = (HashMap<String, String>) dbConfiguration;

            try {
                driver = dbConfigurationMap.get(DB_DRIVER);
                dbUrl = dbConfigurationMap.get(DB_URL);
                username = dbConfigurationMap.get(DB_USERNAME);
                password = dbConfigurationMap.get(DB_PASSWORD);
            } catch (Exception e) {
                log.error("Unexpected error occurred while reading data source configuration map. "+
                          " Database configurations not set properly. ", e);
                throw new ConfigurationException("Unexpected error occurred while reading data" +
                                                 " source configuration map.Database configurations"+
                                                 " not set properly. ", e);
            }

            if (log.isDebugEnabled()) {
                log.debug("Initializing data source configurations for MB RDBMS store");
                log.debug("Data Source Configurations :");
                log.debug(DB_DRIVER + " : " + driver);
                log.debug(DB_URL + " : " + dbUrl);

            }

            if (null == dbUrl) {
                log.warn("Required database url unspecified. So Message Broker RDBMS Store " +
                         "will not work as expected.");
            }
            if (null == driver) {
                log.warn("Required database driver unspecified. So Message Broker RDBMS Store " +
                         "will not work as expected.");
            }
            if (null == username) {
                log.warn("Required database username unspecified. So Message Broker RDBMS Store " +
                         "will not work as expected.");
            }
            if (null == password) {
                log.warn("Required database password unspecified. So Message Broker RDBMS Store " +
                         "will not work as expected.");
            }

            BasicDataSource basicDataSource = new BasicDataSource();
            basicDataSource.setDriverClassName(driver);
            basicDataSource.setUrl(dbUrl);
            basicDataSource.setUsername(username);
            basicDataSource.setPassword(password);

            if(dbConfigurationNumber == MESSAGE_STORE_DATA_SOURCE) {
                messageStoreDataSource = basicDataSource;
                isMessageStoreDataSourceSet = true;
            }

            if(dbConfigurationNumber == CONTEXT_STORE_DATA_SOURCE) {
                contextStoreDataSource = basicDataSource;
                isContextStoreDatasourceSet = true;
            }

            dbConfigurationNumber++;

        }

    }


}
