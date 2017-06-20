/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.broker.test.store;

import org.dbunit.IDatabaseTester;
import org.dbunit.JdbcDatabaseTester;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.operation.DatabaseOperation;
import org.wso2.andes.kernel.AndesException;
import org.wso2.andes.store.rdbms.RDBMSMessageStoreImpl;
import org.wso2.broker.test.jdbc.ConnectionProperties;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Creates the store by wrapping the JDBC connection with DBUnit
 */
public class DBUnitMessageStoreImpl extends RDBMSMessageStoreImpl {

    /**
     * Instance of the test connection
     */
    private IDatabaseTester dbUnitConnection;

    public DBUnitMessageStoreImpl() {
        super();
    }

    /**
     * Initializes message store with the
     *
     * @throws AndesException         if an error occurs when creating the cache in RDBMS store
     * @throws ClassNotFoundException if unitDB instance could not be found
     */
    public void initializeMessageStore(ConnectionProperties properties) throws AndesException, ClassNotFoundException {
        dbUnitConnection = new JdbcDatabaseTester(properties.getDriverClass(),
                properties.getConnectionUrl(),
                properties.getUserName(),
                properties.getPassword());

        //This is a private method call, hence we'll be using declarative methods
        this.initializeQueueMappingCache();
    }

    /**
     * Clean the existing data in the connection and re populate with new data
     *
     * @param data the list of data
     * @throws Exception dbunit throws a generic exception, propagated if an error occurs
     */
    public void cleanAndPopulateData(IDataSet data) throws Exception {
        dbUnitConnection.setSetUpOperation(DatabaseOperation.CLEAN_INSERT);
        dbUnitConnection.setDataSet(data);
        dbUnitConnection.onSetup();

    }

    /**
     * <p>
     * Initializes the cache by calling the parent's cache initialization method
     * </p>
     * <p>
     * <b>Note : </b> Reflection was used here to bypass the visibility private of the method
     * </p>
     *
     * @throws AndesException will be thrown upon an error while initializing the cache
     * @see RDBMSMessageStoreImpl#initializeQueueMappingCache()
     */
    private void initializeQueueMappingCache() throws AndesException {
        try {
            String initializeQueueMappingCacheMethodName = "initializeQueueMappingCache";
            Method cacheInitializer = RDBMSMessageStoreImpl.class.getDeclaredMethod
                    (initializeQueueMappingCacheMethodName);
            //At this point we bypass the accessibility private
            cacheInitializer.setAccessible(true);
            cacheInitializer.invoke(this, (Object[]) null);
        } catch (NoSuchMethodException e) {
            String message = "Error occurred while initializing the method initializeQueueMappingCache";
            throw new AndesException(message, e);
        } catch (IllegalAccessException e) {
            String message = "Error occurred while accessing the method initializeQueueMappingCache";
            throw new AndesException(message, e);
        } catch (InvocationTargetException e) {
            String message = "Error occurred while invoking the method initializeQueueMappingCache";
            throw new AndesException(message, e);
        }
    }

    /**
     * <p>
     * Returns a connection created using DBUnit
     * </p>
     * <p>
     * This methodd is overridden in order to replace the database connection with a mock connection
     * </p>
     */
    @Override
    protected Connection getConnection() throws SQLException {
        Connection connection;
        try {
            IDatabaseConnection iDatabaseConnection = dbUnitConnection.getConnection();
            connection = iDatabaseConnection.getConnection();
        } catch (Exception e) {
            String message = "Error occurred while creating mock connection ";
            throw new SQLException(message, e);
        }
        return connection;
    }
}
