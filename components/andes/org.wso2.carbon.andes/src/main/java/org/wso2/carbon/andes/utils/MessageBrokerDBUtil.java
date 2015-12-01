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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.andes.service.exception.ConfigurationException;
import org.wso2.carbon.utils.CarbonUtils;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.File;

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

    /**
     * log variable for logging.
     */
    private static final Log log = LogFactory.getLog(MessageBrokerDBUtil.class);

    /**
     * sql query to be execute to verify database tables.
     */
    private static final String DB_CHECK_SQL = "SELECT * FROM MB_QUEUE_COUNTER";

    /**
     * keep data source configurations.
     */
    private static volatile DataSource messageStoreDataSource = null;

    /**
     * keep context store data source configurations.
     */
    private static volatile DataSource contextStoreDataSource = null;

    /**
     * keep state if message store configurations set.
     */
    private boolean isMessageStoreDataSourceSet;

    /**
     * keep state if context store configurations set.
     */
    private boolean isContextStoreDataSourceSet;


    /**
     * Creating schema for RDBMS message store using jndi exposed data sources
     * by Carbon (these data sources are formed reading /repository/conf/masterdatasource.xml
     * file)
     *
     * @throws org.wso2.carbon.andes.service.exception.ConfigurationException
     * throws exception if error occurs while creating DB schema
     */
    public void initialize() throws ConfigurationException {

        String dSetupValue = System.getProperty("setup");

        if (dSetupValue != null) {

            //load necessary configurations
            final String brokerConfigFilePath  = CarbonUtils.getCarbonHome()
                    + File.separator + "repository"
                    + File.separator + "conf"
                    + File.separator + "broker.xml";

            MBDatabaseConfig mbDatabaseConfig = new MBDatabaseConfig(brokerConfigFilePath);

            setMessageStoreDataSource(mbDatabaseConfig);

            if(isMessageStoreDataSourceSet) {
                setupMBStoreRdbmsDatabase(messageStoreDataSource);
            }
            // message store and context store schemas can source to same database. In that case
            // context store data source won't set.

            setContextStoreDataSource(mbDatabaseConfig);

            if(isContextStoreDataSourceSet) {
                setupMBStoreRdbmsDatabase(contextStoreDataSource);
            }
        }
    }

    /**
     * Lookup JNDI data source for message store and set
     * @param dbConfig configuration holding DB configurations
     */
    private void setMessageStoreDataSource(MBDatabaseConfig dbConfig) {
        try {

            Context initContext = new InitialContext();
            messageStoreDataSource = (DataSource) initContext.lookup(dbConfig.getMessageStoreJndiName());
            isMessageStoreDataSourceSet = true;

        } catch (NamingException e) {

            log.error("Cannot lookup data source named " + dbConfig.getMessageStoreJndiName()
                    + ". Cannot setup message store", e);
        }
    }

    /**
     * Lookup JNDI data source for context store and set
     * @param dbConfig configuration holding DB configurations
     */
    private void setContextStoreDataSource(MBDatabaseConfig dbConfig) {
        try {
            //if message store and context store both exposed by same jndi name
            if(dbConfig.isContextStoreAvaliable()) {
                contextStoreDataSource = InitialContext.doLookup(dbConfig.getContextStoreJndiName());
                isContextStoreDataSourceSet = true;
            } else {
                isContextStoreDataSourceSet = false;
            }

        } catch (NamingException e) {

            log.info("Cannot lookup data source named " + dbConfig.getContextStoreJndiName()
                    + ". It is optional to use this");

        }
    }

    /**
     * Based on database configurations create database tables, if tables dose not
     * exist in given database.
     *
     * @throws org.wso2.carbon.andes.service.exception.ConfigurationException
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
}
