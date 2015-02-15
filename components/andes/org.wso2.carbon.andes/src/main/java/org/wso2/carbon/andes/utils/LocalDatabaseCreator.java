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
 *
 */
public class LocalDatabaseCreator  extends DatabaseCreator {

    private DataSource dataSource;
    private static final Log log = LogFactory.getLog(LocalDatabaseCreator.class);

    public LocalDatabaseCreator(DataSource dataSource) {
        super(dataSource);
        this.dataSource = dataSource;
    }

    /**
     * Creates database if the script exists, otherwise returns with exception.
     *
     * @throws Exception
     */
    public void createRegistryDatabase() throws Exception {

        String databaseType = DatabaseCreator.getDatabaseType(this.dataSource.getConnection());

        log.info("this.dataSource.getConnection() : " + this.dataSource.getConnection());

        String scripPath = getDbScriptLocation(databaseType);
        File scripFile = new File(scripPath);

        if(scripFile.canRead()){
            super.createRegistryDatabase();
        } else {
            log.error("Unexpected error occurred while reading db script " + scripFile );
            throw new ConfigurationException("Unexpected error occurred while reading db script"  + scripFile);
        }
    }

    /**
     * This method returns relevant mb store sql file path based on given database type.
     *
     * @param databaseType
     * @return databaseSqlScriptPath which contain absolute file path to matching sql file.
     */
    protected String getDbScriptLocation(String databaseType) {
        String scriptName = databaseType + "-mb.sql";
        String carbonHome = System.getProperty("carbon.home");

        String databaseSqlScriptPath = carbonHome +
                "/dbscripts/mb-store/" + scriptName;

        if (log.isDebugEnabled()) {
            log.debug("Loading database script from : " + scriptName);
            log.debug("Load database path           : " + databaseSqlScriptPath);
        }

        return databaseSqlScriptPath;

    }

}
