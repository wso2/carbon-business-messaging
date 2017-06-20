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

package org.wso2.broker.test;

import org.h2.tools.RunScript;
import org.testng.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;
import org.wso2.broker.test.constants.H2Constants;
import org.wso2.broker.test.jdbc.ConnectionProperties;
import org.wso2.broker.test.providers.QueueContextDataProvider;
import org.wso2.broker.test.store.DBUnitMessageStoreImpl;
import org.wso2.broker.test.store.MessageStoreTestQueries;

import java.net.URL;
import java.sql.SQLException;

/**
 * Test the database functionality
 */
public class RDBMSMessageStoreImplTest {

    /**
     * Instance of the layer which communicated with the database
     */
    private DBUnitMessageStoreImpl messageStore;

    /**
     * Instance used to send test queries
     */
    private MessageStoreTestQueries queryEngine;


    /**
     * <p>
     * Before the start of the suite we create the dbUnit database connection
     * </p>
     * <p>
     * <b>Note : </b>Once the connection is established it's expected for the preceding test cases to clear and
     * refresh the table values accordingly
     * </p>
     *
     * @throws ClassNotFoundException if the jdbc class path is not found
     */
    @BeforeSuite
    public void init() throws Exception {
        ConnectionProperties jdbcProperties = new ConnectionProperties();
        jdbcProperties.setConnectionUrl(H2Constants.CONNECTION_URL);
        jdbcProperties.setDriverClass(H2Constants.DATABASE_DRIVER);
        jdbcProperties.setUserName(H2Constants.USERNAME);
        jdbcProperties.setPassword(H2Constants.PASSWORD);

        initializeDatabase();
        messageStore.initializeMessageStore(jdbcProperties);
    }

    /**
     * Clears the in-memory data-source once the test is complete
     */
    @AfterSuite
    public void clear() {
        messageStore.close();
    }


    public RDBMSMessageStoreImplTest() {
        this.messageStore = new DBUnitMessageStoreImpl();
        this.queryEngine = new MessageStoreTestQueries(messageStore);
    }

    /**
     * Creates a test database
     *
     * @throws SQLException if database initialization fails
     */
    private void initializeDatabase() throws SQLException {

        URL resource = RDBMSMessageStoreImplTest.class.getClassLoader().getResource(H2Constants.SCHEMA_URL);

        if (null != resource) {
            String schemaFileLocation = resource.getPath();
            RunScript.execute(H2Constants.CONNECTION_URL, H2Constants.USERNAME, H2Constants.PASSWORD,
                    schemaFileLocation,
                    H2Constants.CHARSET, false);
        } else {
            String reason = "Database schema could not be found in the specified file path " + H2Constants.SCHEMA_URL;
            throw new SQLException(reason);
        }
    }

    /**
     * Tests for addition of queue
     *
     * @param queueName name of the queue
     * @param validity  true if the queue name is valid
     * @throws Exception if an error occurred when adding the queue
     * @see QueueContextDataProvider#getQueueNames()
     * @see org.wso2.andes.store.rdbms.RDBMSMessageStoreImpl#addQueue(String)
     */
    @Test(dataProvider = "QueueNames", dataProviderClass = QueueContextDataProvider.class)
    public void testAddQueue(String queueName, Boolean validity) throws Exception {
        //Adds the queue to the message store
        messageStore.addQueue(queueName);
        //Whether the queue which created exists
        boolean queueExists = queryEngine.queueExists(queueName);
        //Check for the existence/non-existence of the created queue
        Assert.assertEquals(queueExists, validity.booleanValue());
    }


}
