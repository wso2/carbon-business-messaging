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

import org.apache.log4j.Logger;
import org.wso2.broker.test.constants.RDBMSTestConstants;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Holds the queries which will be used to validate the existing functionality of RDBMS store
 * @see org.wso2.andes.store.rdbms.RDBMSMessageStoreImpl
 */
public class MessageStoreTestQueries {

    private static final Logger log = Logger.getLogger(MessageStoreTestQueries.class);

    /**
     * Holds the message store instance overridden by DBUnit
     */
    private DBUnitMessageStoreImpl messageStore;


    public MessageStoreTestQueries(DBUnitMessageStoreImpl messageStore) {
        this.messageStore = messageStore;
    }


    /**
     * <p>
     * Validates whether a queue exists.
     * </p>
     *
     * @param destinationQueue name of the queue
     * @return true if the queue exists
     * @throws SQLException if an error occurs when querying from the database
     * @see org.wso2.broker.test.RDBMSMessageStoreImplTest#testAddQueue(String, Boolean)
     */
    public boolean queueExists(String destinationQueue) throws SQLException {

        boolean hasQueue = false;

        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet;

        try {
            connection = messageStore.getConnection();
            preparedStatement = connection.prepareStatement(RDBMSTestConstants.PS_SELECT_QUEUE_COUNT);
            preparedStatement.setString(1, destinationQueue);
            resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                int numberOfRows = resultSet.getInt(1);
                int minimumRowCount = 0;
                //If the query returns rows
                if (numberOfRows > minimumRowCount) {
                    hasQueue = true;
                }

                if (log.isDebugEnabled()) {
                    log.debug("The query returned " + numberOfRows + " rows");
                }
            }

        } catch (SQLException e) {
            String message = "Error occurred while retrieving destination queue id " +
                    "for destination queue " + destinationQueue;
            throw new SQLException(message, e);
        } finally {
            if (connection != null) {
                connection.close();
            }

            if (preparedStatement != null) {
                preparedStatement.close();
            }
        }

        return hasQueue;
    }
}
