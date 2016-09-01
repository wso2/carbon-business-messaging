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

import com.google.common.base.Splitter;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.wso2.carbon.andes.core.AndesException;
import org.wso2.carbon.andes.core.internal.configuration.util.ConfigurationProperties;
import org.wso2.carbon.andes.core.store.AndesBatchUpdateException;
import org.wso2.carbon.andes.core.store.AndesDataException;
import org.wso2.carbon.andes.core.store.AndesDataIntegrityViolationException;
import org.wso2.carbon.andes.core.store.AndesStoreUnavailableException;
import org.wso2.carbon.andes.core.store.AndesTransactionRollbackException;

import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.DataTruncation;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLClientInfoException;
import java.sql.SQLDataException;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.SQLInvalidAuthorizationSpecException;
import java.sql.SQLNonTransientConnectionException;
import java.sql.SQLTimeoutException;
import java.sql.SQLTransactionRollbackException;
import java.sql.SQLTransientConnectionException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Contains utilility methods required for both {@link RDBMSMessageStoreImpl}
 * and {@link RDBMSAndesContextStoreImpl}
 */
public class RDBMSStoreUtils {

    private static final Logger log = Logger.getLogger(RDBMSStoreUtils.class);

    /**
     * Keep track of SQL state code classes (i.e first two digits)
     * corresponding to database connectivity errors
     */
    private Set<String> storeUnavailableSQLStatesClassCodes;


    /**
     * Keep track of SQL state code classes (i.e first two digits)
     * corresponding to integrity violation errors
     */

    private Set<String> dataIntegrityViolationSQLStateClassCodes;


    /**
     * Keep track of SQL state code classes (i.e first two digits)
     * corresponding to various database server generated errors similar to
     * {@link DataTruncation}, {@link SQLDataException} (but driver didn't
     * differentiated and just choose set the sql state only)
     */
    private Set<String> dataErrorSQLStateClassCodes;


    private Set<String> transactionRollbackSQLStateClassCodes;


    public RDBMSStoreUtils(ConfigurationProperties connectionProperties) {

        // extract the configurations set in broker.xml
        storeUnavailableSQLStatesClassCodes =
                extractAndFill(connectionProperties,
                               RDBMSConstants.STORE_UNAVAILABLE_SQL_STATE_CLASSES
                );

        dataIntegrityViolationSQLStateClassCodes =
                extractAndFill(connectionProperties,
                               RDBMSConstants.DATA_INTEGRITY_VIOLATION_SQL_STATE_CLASSES
                );

        dataErrorSQLStateClassCodes = extractAndFill(connectionProperties, RDBMSConstants.DATA_ERROR_SQL_STATE_CLASSES
        );


        transactionRollbackSQLStateClassCodes =
                extractAndFill(connectionProperties,
                               RDBMSConstants.TRANSACTION_ROLLBACK_ERROR_SQL_STATE_CLASSES
                );

    }


    public AndesException convertSQLException(String message, SQLException sqlException) {

        // try using SQLException subclasses (works for mysql)
        AndesException convertedException = convertBySQLException(message, sqlException);


        if (null == convertedException) {
            // trying using sql state flags ( works for jtds, mssql etc)
            convertedException = convertBySQLState(message, sqlException);
        }

        if (null == convertedException) {
            // if unable to determine exact error then throw a generic exception.
            convertedException = new AndesException(message, sqlException.getSQLState(), sqlException);
        }

        return convertedException;

    }

    private AndesException convertBySQLException(String message, SQLException sqlException) {
        AndesException convertedException = null;

        if (SQLNonTransientConnectionException.class.isInstance(sqlException) ||
                SQLTransientConnectionException.class.isInstance(sqlException) ||
                SQLInvalidAuthorizationSpecException.class.isInstance(sqlException) ||
                SQLClientInfoException.class.isInstance(sqlException) ||
                SQLTimeoutException.class.isInstance(sqlException)) {

            String sqlState = extractSqlState(sqlException);
            convertedException = new AndesStoreUnavailableException(message, sqlState, sqlException);

        } else if (SQLIntegrityConstraintViolationException.class.isInstance(sqlException)) {
            String sqlState = extractSqlState(sqlException);
            convertedException = new AndesDataIntegrityViolationException(message, sqlState, sqlException);
        } else if (SQLTransactionRollbackException.class.isInstance(sqlException)) {
            String sqlState = extractSqlState(sqlException);
            convertedException = new AndesTransactionRollbackException(message, sqlState, sqlException);
        } else if (DataTruncation.class.isInstance(sqlException) ||
                SQLDataException.class.isInstance(sqlException)) {
            String sqlState = extractSqlState(sqlException);
            convertedException = new AndesDataException(message, sqlState, sqlException);
        }

        return convertedException;
    }

    private AndesException convertBySQLState(String message, SQLException sqlException) {

        AndesException convertedException = null;
        String sqlState = extractSqlState(sqlException);

        String sqlStateClassCode = determineSqlStateClassCode(sqlState);

        if (storeUnavailableSQLStatesClassCodes.contains(sqlStateClassCode)) {
            convertedException = new AndesStoreUnavailableException(message, sqlState, sqlException);
        } else if (dataIntegrityViolationSQLStateClassCodes.contains(sqlStateClassCode)) {
            convertedException = new AndesDataIntegrityViolationException(message, sqlState, sqlException);
        } else if (transactionRollbackSQLStateClassCodes.contains(sqlStateClassCode)) {
            convertedException = new AndesTransactionRollbackException(message, sqlState, sqlException);
        } else if (dataErrorSQLStateClassCodes.contains(sqlStateClassCode)) {
            convertedException = new AndesDataException(message, sqlState, sqlException);
        }

        return convertedException;
    }


    /**
     * Inserts a test record
     *
     * @param testString a string value
     * @param testTime   a time value
     * @return true if the test was successful.
     */
    public boolean testInsert(Connection connection, String testString, long testTime) throws SQLException {

        boolean canInsert = false;
        PreparedStatement preparedStatement = null;

        try {
            connection.setAutoCommit(false);

            preparedStatement = connection.prepareStatement(RDBMSConstants.PS_TEST_MSG_STORE_INSERT);

            preparedStatement.setString(1, testString);
            preparedStatement.setLong(2, testTime);

            preparedStatement.executeUpdate();
            connection.commit();
            canInsert = true;
        } catch (SQLException e) {
            rollback(connection,
                     RDBMSConstants.TASK_TEST_MESSAGE_STORE_OPERATIONAL_INSERT +
                             String.format("test data : [%s, %d]", testString, testTime));

        } finally {
            String msg =
                    RDBMSConstants.TASK_TEST_MESSAGE_STORE_OPERATIONAL_INSERT +
                            String.format("test data : [%s, %d]", testString, testTime);
            close(preparedStatement, msg);
            close(connection, msg);
        }

        return canInsert;
    }

    /**
     * Reads a test record
     *
     * @param testString a string value
     * @param testTime   a time value
     * @return true if the test was successful.
     */
    public boolean testRead(Connection connection,
                            String testString,
                            long testTime) throws SQLException {

        boolean canRead = false;

        PreparedStatement selectPreparedStatement = null;

        ResultSet results = null;

        try {
            selectPreparedStatement = connection.prepareStatement(
                    RDBMSConstants.PS_TEST_MSG_STORE_SELECT);
            selectPreparedStatement.setString(1, testString);
            selectPreparedStatement.setLong(2, testTime);

            results = selectPreparedStatement.executeQuery();

            if (results.next()) {
                canRead = true;
            }

        } catch (SQLException e) {
            log.error(RDBMSConstants.TASK_TEST_MESSAGE_STORE_OPERATIONAL_READ +
                              String.format("test data : [%s, %d]", testString, testTime), e);

        } finally {
            String msg =
                    RDBMSConstants.TASK_TEST_MESSAGE_STORE_OPERATIONAL_READ +
                            String.format("test data : [%s, %d]", testString, testTime);

            close(results, msg);
            close(selectPreparedStatement, msg);
            close(connection, msg);
        }

        return canRead;
    }

    /**
     * Delete a test record
     *
     * @param testString a string value
     * @param testTime   a time value
     * @return true if the test was successful.
     */
    public boolean testDelete(Connection connection, String testString, long testTime) throws SQLException {

        boolean canDelete = false;
        PreparedStatement preparedStatement = null;

        ResultSet results = null;

        try {
            connection.setAutoCommit(false);

            preparedStatement = connection.prepareStatement(RDBMSConstants.PS_TEST_MSG_STORE_DELETE);
            preparedStatement.setString(1, testString);
            preparedStatement.setLong(2, testTime);
            preparedStatement.executeUpdate();
            connection.commit();
            canDelete = true;
        } catch (SQLException e) {
            rollback(connection,
                     RDBMSConstants.TASK_TEST_MESSAGE_STORE_OPERATIONAL_DELETE +
                             String.format("test data : [%s, %d]", testString, testTime));
        } finally {

            String msg =
                    RDBMSConstants.TASK_TEST_MESSAGE_STORE_OPERATIONAL_DELETE +
                            String.format("test data : [%s, %d]", testString, testTime);
            close(results, msg);
            close(preparedStatement, msg);
            close(connection, msg);
        }

        return canDelete;
    }

    /**
     * close the prepared statement resource
     *
     * @param preparedStatement PreparedStatement
     * @param task              task that was done by the closed prepared statement.
     */
    private void close(PreparedStatement preparedStatement, String task) {
        if (preparedStatement != null) {
            try {
                preparedStatement.close();
            } catch (SQLException e) {
                log.error("Closing prepared statement failed after " + task, e);
            }
        }
    }

    /**
     * closes the result set resources
     *
     * @param resultSet ResultSet
     * @param task      task that was done by the closed result set.
     */
    private void close(ResultSet resultSet, String task) {
        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (SQLException e) {
                log.error("Closing result set failed after " + task, e);
            }
        }
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
                connection.setAutoCommit(true);
                connection.close();
            } catch (SQLException e) {
                log.error("Failed to close connection after " + task, e);
            }
        }
    }

    /**
     * On database update failure tries to rollback
     *
     * @param connection database connection
     * @param task       explanation of the task done when the rollback was triggered
     */
    private void rollback(Connection connection, String task) {
        if (connection != null) {
            try {
                connection.rollback();
            } catch (SQLException e) {
                log.warn("Rollback failed on " + task, e);
            }
        }
    }

    /**
     * Throws an {@link AndesBatchUpdateException} with specified
     * information. and rollback the tried batch operation.
     *
     * @param dataList   data objects will was used for batch operation.
     * @param connection SQL connection used.
     * @param bue        the original SQL batch update exception.
     * @param task       the string indicating the task tried out (in failed batch
     *                   update operation)
     * @throws AndesBatchUpdateException the error.
     */
    public <T> void raiseBatchUpdateException(List<T> dataList, Connection connection,
                                              BatchUpdateException bue,
                                              String task) throws AndesBatchUpdateException {

        int[] updateCountsOfFailedBatch = bue.getUpdateCounts();
        List<T> failed = new ArrayList<T>();
        List<T> succeded = new ArrayList<T>();

        for (int i = 0; i < updateCountsOfFailedBatch.length; i++) {
            T msgPart = dataList.get(i);
            if (Statement.EXECUTE_FAILED == updateCountsOfFailedBatch[i]) {
                failed.add(msgPart);
            } else {
                succeded.add(msgPart);
            }
        }

        rollback(connection, task); // try to rollback the batch operation.

        AndesBatchUpdateException insertEx =
                new AndesBatchUpdateException(task + " failed", bue.getSQLState(),
                                              bue, failed, succeded);
        throw insertEx;
    }


    /**
     * A Utility method which will extract a configuration, tokenize and
     * return a {@link Set} with values.
     *
     * @param connectionProperties All configurations
     * @param configParam          specific configuration name (to extract value)
     * @return parsed configurations
     */
    private Set<String> extractAndFill(ConfigurationProperties connectionProperties,
                                       String configParam) {
        Set<String> configValues = Collections.emptySet();

        String value = connectionProperties.getProperty(configParam);

        if (StringUtils.isNotBlank(value)) {
            Iterable<String> splitValues = Splitter.on(',').trimResults()
                    .omitEmptyStrings()
                    .split(value);

            configValues = new HashSet<String>();
            for (String state : splitValues) {
                configValues.add(state);
            }
        }

        return configValues;
    }

    /**
     * Extracts SQL State from a given sql exception
     *
     * @param sqlException the error
     * @return sql state
     */
    private String extractSqlState(SQLException sqlException) {
        String sqlState = sqlException.getSQLState();
        SQLException nextEx = sqlException.getNextException();
        while (sqlState == null && nextEx != null) {
            sqlState = nextEx.getSQLState();
            nextEx = nextEx.getNextException();
        }
        return sqlState;
    }

    /**
     * Extracts the Sql state class ( the first two digits of the sql state)
     *
     * @param sqlState the sql state given in a sql exception
     * @return the sql state class
     */
    private String determineSqlStateClassCode(String sqlState) {
        if (sqlState == null || sqlState.length() < 2) {
            return sqlState;
        }
        return sqlState.substring(0, 2);
    }

}
