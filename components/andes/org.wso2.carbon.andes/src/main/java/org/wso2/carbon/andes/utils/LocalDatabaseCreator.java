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
import org.wso2.carbon.utils.dbcreator.DatabaseCreator;

import javax.sql.DataSource;
import java.io.File;

/**
 * <h1>Create MB store database tables based on configurations set</h1>
 * This class contain methods to create database tables
 * for mb store based on given DataSource Configurations.
 */
public class LocalDatabaseCreator extends DatabaseCreator {

    private static final Log log = LogFactory.getLog(LocalDatabaseCreator.class);
    private DataSource dataSource;

    public LocalDatabaseCreator(DataSource dataSource) {
        super(dataSource);
        this.dataSource = dataSource;
    }

    /**
     * Creates database if the script exists, otherwise returns with exception.
     *
     * @throws org.wso2.carbon.andes.service.exception.ConfigurationException
     */
    public void createRegistryDatabase() throws ConfigurationException {

        String databaseType;
        File scripFile;

        try {

            databaseType = DatabaseCreator.getDatabaseType(this.dataSource.getConnection());
            String scripPath = getDbScriptLocation(databaseType);
            scripFile = new File(scripPath);

        } catch (Exception e) {
            log.error("Unexpected error occurred while connecting to the database.", e);
            throw new ConfigurationException("Unexpected error occurred while connecting to" +
                                             " the database.", e);
        }


        if (scripFile.canRead()) {

            try {
                super.createRegistryDatabase();
            } catch (Exception e) { // Carbon throws Exception.
                log.error("Unexpected error occurred while creating the database tables.", e);
                throw new ConfigurationException("Unexpected error occurred while creating the" +
                                                 " database tables.", e);
            }

        } else {
            log.error("Unexpected error occurred while reading db script : " + scripFile);
            throw new ConfigurationException("Unexpected error occurred while reading db script");
        }
    }

    /**
     * This method returns relevant mb store sql file path based on given database type.
     *
     * @param databaseType type of the database as a string.
     * @return databaseSqlScriptPath which contain absolute file path to matching sql file.
     */
    protected String getDbScriptLocation(String databaseType) {
        String scriptName = databaseType + "-mb.sql";
        String carbonHome = System.getProperty("carbon.home");

        String databaseSqlScriptPath = carbonHome + File.separator + "dbscripts" + File.separator +
                                       "mb-store" + File.separator + scriptName;

        if (log.isDebugEnabled()) {
            log.debug("Load database path : " + databaseSqlScriptPath);

        }

        return databaseSqlScriptPath;

    }

}
