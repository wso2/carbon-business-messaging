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
import org.wso2.carbon.andes.core.AndesBinding;
import org.wso2.carbon.andes.core.AndesException;
import org.wso2.carbon.andes.core.AndesExchange;
import org.wso2.carbon.andes.core.AndesQueue;
import org.wso2.carbon.andes.core.AndesSubscription;
import org.wso2.carbon.andes.core.ProtocolType;
import org.wso2.carbon.andes.core.internal.AndesContext;
import org.wso2.carbon.andes.core.internal.configuration.util.ConfigurationProperties;
import org.wso2.carbon.andes.core.internal.metrics.MetricsConstants;
import org.wso2.carbon.andes.core.internal.slot.Slot;
import org.wso2.carbon.andes.core.internal.slot.SlotState;
import org.wso2.carbon.andes.core.store.AndesContextStore;
import org.wso2.carbon.andes.core.store.AndesDataIntegrityViolationException;
import org.wso2.carbon.andes.core.store.DurableStoreConnection;
import org.wso2.carbon.andes.core.subscription.BasicSubscription;
import org.wso2.carbon.metrics.core.Level;
import org.wso2.carbon.metrics.core.Timer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.sql.DataSource;

/**
 * ANSI SQL based Andes Context Store implementation. This is used to persist information of
 * current durable subscription, exchanges, queues and bindings
 */

public class RDBMSAndesContextStoreImpl implements AndesContextStore {

    private static final Logger logger = Logger.getLogger(RDBMSAndesContextStoreImpl.class);

    /**
     * Connection pooled sql data source object. Used to create connections in method scope
     */
    private DataSource datasource;


    /**
     * Contains utils methods related to connection health tests
     */
    private RDBMSStoreUtils rdbmsStoreUtils;

    /**
     * Set of registered protocols are kep here.
     */
    private Set<ProtocolType> protocols = new HashSet<>();


    /**
     * {@inheritDoc}
     */
    @Override
    public DurableStoreConnection init(ConfigurationProperties connectionProperties) throws AndesException {

        RDBMSConnection rdbmsConnection = new RDBMSConnection();
        rdbmsConnection.initialize(connectionProperties);

        rdbmsStoreUtils = new RDBMSStoreUtils(connectionProperties);

        datasource = rdbmsConnection.getDataSource();
        logger.info("Andes Context Store initialised");
        return rdbmsConnection;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<BasicSubscription> getAllStoredDurableSubscriptions() throws AndesException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        Set<BasicSubscription> subscriptions = new HashSet<>();
        Timer.Context contextRead = AndesContext.getInstance().getMetricService().timer(MetricsConstants.DB_READ,
                                                                                        Level.INFO).start();

        try {

            connection = getConnection();
            preparedStatement = connection.prepareStatement(RDBMSConstants
                                                                    .PS_SELECT_ALL_DURABLE_SUBSCRIPTIONS);
            resultSet = preparedStatement.executeQuery();

            // create Subscriber Map
            while (resultSet.next()) {
                BasicSubscription subscription
                        = new BasicSubscription(resultSet.getString(RDBMSConstants.DURABLE_SUB_DATA));
                subscriptions.add(subscription);
            }
            return subscriptions;

        } catch (SQLException e) {
            throw rdbmsStoreUtils.convertSQLException("Error occurred while " + RDBMSConstants
                    .TASK_RETRIEVING_ALL_DURABLE_SUBSCRIPTIONS, e);
        } finally {
            contextRead.stop();
            close(resultSet, RDBMSConstants.TASK_RETRIEVING_ALL_DURABLE_SUBSCRIPTIONS);
            close(preparedStatement, RDBMSConstants.TASK_RETRIEVING_ALL_DURABLE_SUBSCRIPTIONS);
            close(connection, RDBMSConstants.TASK_RETRIEVING_ALL_DURABLE_SUBSCRIPTIONS);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, String> getAllDurableSubscriptionsByID() throws AndesException {

        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        Map<String, String> subscriberMap = new HashMap<>();
        Timer.Context contextRead =
                AndesContext.getInstance().getMetricService().timer(MetricsConstants.DB_READ, Level.INFO).start();

        try {
            connection = getConnection();
            preparedStatement = connection.prepareStatement(RDBMSConstants
                                                                    .PS_SELECT_ALL_DURABLE_SUBSCRIPTIONS_WITH_SUB_ID);
            resultSet = preparedStatement.executeQuery();

            // create the subscriber Map
            while (resultSet.next()) {
                String subId = resultSet.getString(RDBMSConstants.DURABLE_SUB_ID);
                String subscriber = resultSet.getString(RDBMSConstants.DURABLE_SUB_DATA);
                subscriberMap.put(subId, subscriber);
            }
            return subscriberMap;

        } catch (SQLException e) {
            throw rdbmsStoreUtils.convertSQLException("Error occurred while "
                                                              + RDBMSConstants
                                                              .TASK_RETRIEVING_ALL_DURABLE_SUBSCRIPTIONS,
                                                      e);
        } finally {
            contextRead.stop();
            close(resultSet, RDBMSConstants.TASK_RETRIEVING_ALL_DURABLE_SUBSCRIPTIONS);
            close(preparedStatement, RDBMSConstants.TASK_RETRIEVING_ALL_DURABLE_SUBSCRIPTIONS);
            close(connection, RDBMSConstants.TASK_RETRIEVING_ALL_DURABLE_SUBSCRIPTIONS);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSubscriptionExist(String subscriptionId) throws AndesException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            connection = getConnection();
            preparedStatement = connection.prepareStatement(RDBMSConstants.PS_IS_SUBSCRIPTION_EXIST);
            preparedStatement.setString(1, subscriptionId);
            resultSet = preparedStatement.executeQuery();

            return resultSet.next();
        } catch (SQLException e) {
            throw rdbmsStoreUtils.convertSQLException("Error occurred while "
                                                              + RDBMSConstants.TASK_CHECK_SUBSCRIPTION_EXISTENCE, e);
        } finally {
            close(resultSet, RDBMSConstants.TASK_CHECK_SUBSCRIPTION_EXISTENCE);
            close(preparedStatement, RDBMSConstants.TASK_CHECK_SUBSCRIPTION_EXISTENCE);
            close(connection, RDBMSConstants.TASK_CHECK_SUBSCRIPTION_EXISTENCE);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void storeDurableSubscription(AndesSubscription subscription) throws AndesException {

        Connection connection = null;
        PreparedStatement preparedStatement = null;
        Timer.Context contextWrite = AndesContext.getInstance().getMetricService().timer(MetricsConstants.DB_WRITE,
                                                                                         Level.INFO).start();

        String destinationIdentifier = getDestinationIdentifier(subscription);
        String subscriptionID = this.generateSubscriptionID(subscription);

        try {
            connection = getConnection();

            preparedStatement = connection.prepareStatement(
                    RDBMSConstants.PS_INSERT_DURABLE_SUBSCRIPTION);
            preparedStatement.setString(1, destinationIdentifier);
            preparedStatement.setString(2, subscriptionID);
            preparedStatement.setString(3, subscription.encodeAsStr());
            preparedStatement.executeUpdate();

            connection.commit();

        } catch (SQLException e) {
            rollback(connection, RDBMSConstants.TASK_STORING_DURABLE_SUBSCRIPTION);
            throw rdbmsStoreUtils.convertSQLException("Error occurred while storing durable subscription. sub id: "
                                                              + subscriptionID + " destination identifier: " +
                                                              destinationIdentifier,
                                                      e);
        } finally {
            contextWrite.stop();
            close(preparedStatement, RDBMSConstants.TASK_STORING_DURABLE_SUBSCRIPTION);
            close(connection, RDBMSConstants.TASK_STORING_DURABLE_SUBSCRIPTION);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateDurableSubscription(AndesSubscription subscription) throws AndesException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        Timer.Context contextWrite = AndesContext.getInstance().getMetricService().timer(MetricsConstants.DB_WRITE,
                                                                                         Level.INFO).start();

        String destinationIdentifier = getDestinationIdentifier(subscription);
        String subscriptionID = this.generateSubscriptionID(subscription);

        try {

            connection = getConnection();

            preparedStatement = connection.prepareStatement(
                    RDBMSConstants.PS_UPDATE_DURABLE_SUBSCRIPTION);
            preparedStatement.setString(1, subscription.encodeAsStr());
            preparedStatement.setString(2, destinationIdentifier);
            preparedStatement.setString(3, subscriptionID);
            preparedStatement.executeUpdate();

            connection.commit();

        } catch (SQLException e) {
            rollback(connection, RDBMSConstants.TASK_UPDATING_DURABLE_SUBSCRIPTION);
            throw rdbmsStoreUtils.convertSQLException("Error occurred while updating durable subscription. sub id: "
                                                              + subscriptionID + " destination identifier: " +
                                                              destinationIdentifier,
                                                      e);
        } finally {
            contextWrite.stop();
            close(preparedStatement, RDBMSConstants.TASK_UPDATING_DURABLE_SUBSCRIPTION);
            close(connection, RDBMSConstants.TASK_UPDATING_DURABLE_SUBSCRIPTION);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateDurableSubscriptions(Map<String, String> subscriptions) throws AndesException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        Timer.Context contextWrite = AndesContext.getInstance().getMetricService().timer(MetricsConstants.DB_WRITE,
                                                                                         Level.INFO).start();

        try {
            connection = getConnection();
            preparedStatement = connection.prepareStatement(RDBMSConstants.PS_UPDATE_DURABLE_SUBSCRIPTION_BY_ID);
            for (Map.Entry<String, String> entry : subscriptions.entrySet()) {
                preparedStatement.setString(1, entry.getValue());
                preparedStatement.setString(2, entry.getKey());
                preparedStatement.addBatch();
            }
            preparedStatement.executeBatch();
            connection.commit();

        } catch (SQLException e) {
            rollback(connection, RDBMSConstants.TASK_UPDATING_DURABLE_SUBSCRIPTIONS);
            throw rdbmsStoreUtils.convertSQLException("Error occurred while updating durable subscriptions.", e);
        } finally {
            contextWrite.stop();
            close(preparedStatement, RDBMSConstants.TASK_UPDATING_DURABLE_SUBSCRIPTIONS);
            close(connection, RDBMSConstants.TASK_UPDATING_DURABLE_SUBSCRIPTIONS);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeDurableSubscription(AndesSubscription subscription)
            throws AndesException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        String destinationIdentifier = getDestinationIdentifier(subscription);
        String subscriptionID = this.generateSubscriptionID(subscription);

        String task = RDBMSConstants.TASK_REMOVING_DURABLE_SUBSCRIPTION + "destination: " +
                destinationIdentifier + " sub id: " + subscriptionID;
        Timer.Context contextWrite = AndesContext.getInstance().getMetricService().timer(MetricsConstants.DB_WRITE,
                                                                                         Level.INFO).start();

        try {

            connection = getConnection();
            preparedStatement = connection.prepareStatement(RDBMSConstants
                                                                    .PS_DELETE_DURABLE_SUBSCRIPTION);
            preparedStatement.setString(1, destinationIdentifier);
            preparedStatement.setString(2, subscriptionID);
            preparedStatement.executeUpdate();

            connection.commit();

        } catch (SQLException e) {
            rollback(connection, task);
            throw rdbmsStoreUtils.convertSQLException("error occurred while " + task, e);
        } finally {
//            contextWrite.stop();
            close(preparedStatement, task);
            close(connection, task);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void storeNodeDetails(String nodeID, String data) throws AndesException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        // task in progress that's logged on an exception
        String task = RDBMSConstants.TASK_STORING_NODE_INFORMATION + "node id: " + nodeID;

        Timer.Context contextWrite = AndesContext.getInstance().getMetricService().timer(MetricsConstants.DB_WRITE,
                                                                                         Level.INFO).start();

        try {
            // done as a transaction
            connection = getConnection();

            preparedStatement = connection.prepareStatement(RDBMSConstants.PS_INSERT_NODE_INFO);
            preparedStatement.setString(1, nodeID);
            preparedStatement.setString(2, data);
            preparedStatement.executeUpdate();

            connection.commit();

        } catch (SQLException e) {
            rollback(connection, task);
            throw rdbmsStoreUtils.convertSQLException("Error occurred while " + task, e);
        } finally {
            contextWrite.stop();
            close(preparedStatement, task);
            close(connection, task);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, String> getAllStoredNodeData() throws AndesException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        Map<String, String> nodeInfoMap = new HashMap<>();
        Timer.Context contextRead = AndesContext.getInstance().getMetricService().timer(MetricsConstants.DB_READ,
                                                                                        Level.INFO).start();

        try {

            connection = getConnection();
            preparedStatement = connection.prepareStatement(RDBMSConstants.PS_SELECT_ALL_NODE_INFO);
            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                nodeInfoMap.put(
                        resultSet.getString(RDBMSConstants.NODE_ID),
                        resultSet.getString(RDBMSConstants.NODE_INFO)
                );
            }

            return nodeInfoMap;
        } catch (SQLException e) {
            throw rdbmsStoreUtils.convertSQLException(
                    "Error occurred while " + RDBMSConstants.TASK_RETRIEVING_ALL_NODE_DETAILS, e);
        } finally {
            contextRead.stop();
            close(resultSet, RDBMSConstants.TASK_RETRIEVING_ALL_NODE_DETAILS);
            close(preparedStatement, RDBMSConstants.TASK_RETRIEVING_ALL_NODE_DETAILS);
            close(connection, RDBMSConstants.TASK_RETRIEVING_ALL_NODE_DETAILS);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeNodeData(String nodeID) throws AndesException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        String task = RDBMSConstants.TASK_REMOVING_NODE_INFORMATION + " node id: " + nodeID;
        Timer.Context contextWrite = AndesContext.getInstance().getMetricService().timer(MetricsConstants.DB_WRITE,
                                                                                         Level.INFO).start();

        try {

            connection = getConnection();

            preparedStatement = connection.prepareStatement(RDBMSConstants.PS_DELETE_NODE_INFO);
            preparedStatement.setString(1, nodeID);
            preparedStatement.executeUpdate();

            connection.commit();

        } catch (SQLException e) {
            rollback(connection, task);
            throw rdbmsStoreUtils.convertSQLException("Error occurred while " + task, e);
        } finally {
            contextWrite.stop();
            close(preparedStatement, task);
            close(connection, task);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addMessageCounterForQueue(String destinationQueueName) throws AndesException {

        Connection connection = null;
        PreparedStatement preparedStatement = null;
        Timer.Context contextWrite = AndesContext.getInstance().getMetricService().timer(MetricsConstants.DB_WRITE,
                                                                                         Level.INFO).start();
        try {
            connection = getConnection();

            if (!isCounter4QueueExist(connection, destinationQueueName)) {
                // if queue counter does not exist

                preparedStatement = connection.prepareStatement(RDBMSConstants
                                                                        .PS_INSERT_QUEUE_COUNTER);
                preparedStatement.setString(1, destinationQueueName);
                preparedStatement.setLong(2, 0); // initial count is set to zero for parameter two
                preparedStatement.executeUpdate();
                connection.commit();
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("counter for queue: " + destinationQueueName + " already exists.");
                }
            }

        } catch (SQLException e) {
            rollback(connection, RDBMSConstants.TASK_ADDING_QUEUE_COUNTER);
            throw rdbmsStoreUtils.convertSQLException("Error occurred while " + RDBMSConstants
                    .TASK_ADDING_QUEUE_COUNTER, e);
        } finally {
            contextWrite.stop();
            close(preparedStatement, RDBMSConstants.TASK_ADDING_QUEUE_COUNTER);
            close(connection, RDBMSConstants.TASK_ADDING_QUEUE_COUNTER);
        }
    }

    /**
     * Check whether the queue counter already exists. Provided connection is not closed
     *
     * @param connection SQL Connection
     * @param queueName  queue name
     * @return returns true if the queue counter exists
     * @throws AndesException
     */
    private boolean isCounter4QueueExist(Connection connection,
                                         String queueName) throws AndesException {

        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            // check if queue already exist
            preparedStatement = connection.prepareStatement(RDBMSConstants.PS_SELECT_QUEUE_COUNT);
            preparedStatement.setString(1, queueName);
            resultSet = preparedStatement.executeQuery();

            return resultSet.next();
        } catch (SQLException e) {
            throw rdbmsStoreUtils.convertSQLException("Error occurred while " + RDBMSConstants
                    .TASK_ADDING_QUEUE_COUNTER, e);
        } finally {
            close(resultSet, RDBMSConstants.TASK_CHECK_QUEUE_COUNTER_EXIST);
            close(preparedStatement, RDBMSConstants.TASK_CHECK_QUEUE_COUNTER_EXIST);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getMessageCountForQueue(String destinationQueueName) throws AndesException {

        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        Timer.Context contextRead = AndesContext.getInstance().getMetricService().timer(MetricsConstants.DB_READ,
                                                                                        Level.INFO).start();

        try {
            connection = getConnection();
            preparedStatement = connection.prepareStatement(RDBMSConstants.PS_SELECT_QUEUE_COUNT);
            preparedStatement.setString(1, destinationQueueName);

            resultSet = preparedStatement.executeQuery();

            long count = 0;
            if (resultSet.next()) {
                count = resultSet.getLong(RDBMSConstants.MESSAGE_COUNT);
            }
            return count;
        } catch (SQLException e) {
            throw rdbmsStoreUtils.convertSQLException("Error occurred while " + RDBMSConstants
                    .TASK_RETRIEVING_QUEUE_COUNT, e);
        } finally {
            contextRead.stop();
            close(resultSet, RDBMSConstants.TASK_RETRIEVING_QUEUE_COUNT);
            close(preparedStatement, RDBMSConstants.TASK_RETRIEVING_QUEUE_COUNT);
            close(connection, RDBMSConstants.TASK_RETRIEVING_QUEUE_COUNT);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void resetMessageCounterForQueue(String storageQueueName) throws AndesException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        Timer.Context contextWrite = AndesContext.getInstance().getMetricService().timer(MetricsConstants.DB_WRITE,
                                                                                         Level.INFO).start();

        try {
            connection = getConnection();

            // RESET the queue counter to 0
            preparedStatement = connection
                    .prepareStatement(RDBMSConstants.PS_RESET_QUEUE_COUNT);
            preparedStatement.setString(1, storageQueueName);

            preparedStatement.execute();
            connection.commit();

        } catch (SQLException e) {
            rollback(connection, RDBMSConstants.TASK_RESETTING_MESSAGE_COUNTER + storageQueueName);
            throw rdbmsStoreUtils.convertSQLException("error occurred while resetting message count for queue :" +
                                                              storageQueueName, e);
        } finally {
            contextWrite.stop();
            String task = RDBMSConstants.TASK_RESETTING_MESSAGE_COUNTER + storageQueueName;
            close(preparedStatement, task);
            close(connection, task);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeMessageCounterForQueue(String destinationQueueName) throws AndesException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        Timer.Context contextWrite = AndesContext.getInstance().getMetricService().timer(MetricsConstants.DB_WRITE,
                                                                                         Level.INFO).start();

        try {
            connection = getConnection();

            preparedStatement = connection.prepareStatement(RDBMSConstants.PS_DELETE_QUEUE_COUNTER);
            preparedStatement.setString(1, destinationQueueName);
            preparedStatement.executeUpdate();

            connection.commit();

        } catch (SQLException e) {
            rollback(connection, RDBMSConstants.TASK_DELETING_QUEUE_COUNTER);
            throw rdbmsStoreUtils.convertSQLException("Error occurred while " + RDBMSConstants
                    .TASK_DELETING_QUEUE_COUNTER + " queue: " + destinationQueueName, e);
        } finally {
            contextWrite.stop();
            close(preparedStatement, RDBMSConstants.TASK_DELETING_QUEUE_COUNTER);
            close(connection, RDBMSConstants.TASK_DELETING_QUEUE_COUNTER);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void incrementMessageCountForQueue(String destinationQueueName, long incrementBy)
            throws AndesException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        Timer.Context contextWrite = AndesContext.getInstance().getMetricService().timer(MetricsConstants.DB_WRITE,
                                                                                         Level.INFO).start();

        try {
            connection = getConnection();

            preparedStatement = connection.prepareStatement(RDBMSConstants.PS_INCREMENT_QUEUE_COUNT);
            preparedStatement.setLong(1, incrementBy);
            preparedStatement.setString(2, destinationQueueName);
            preparedStatement.executeUpdate();

            connection.commit();
        } catch (SQLException e) {
            rollback(connection, RDBMSConstants.TASK_INCREMENTING_QUEUE_COUNT);
            throw rdbmsStoreUtils.convertSQLException("Error occurred while " + RDBMSConstants
                    .TASK_INCREMENTING_QUEUE_COUNT + " queue name: " + destinationQueueName, e);
        } finally {
            contextWrite.stop();
            close(preparedStatement, RDBMSConstants.TASK_INCREMENTING_QUEUE_COUNT);
            close(connection, RDBMSConstants.TASK_INCREMENTING_QUEUE_COUNT);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void decrementMessageCountForQueue(String destinationQueueName, long decrementBy)
            throws AndesException {

        Connection connection = null;
        PreparedStatement preparedStatement = null;
        Timer.Context contextWrite = AndesContext.getInstance().getMetricService().timer(MetricsConstants.DB_WRITE,
                                                                                         Level.INFO).start();

        try {
            connection = getConnection();

            preparedStatement = connection.prepareStatement(RDBMSConstants.PS_DECREMENT_QUEUE_COUNT);
            preparedStatement.setLong(1, decrementBy);
            preparedStatement.setString(2, destinationQueueName);
            preparedStatement.executeUpdate();

            connection.commit();
        } catch (SQLException e) {
            rollback(connection, RDBMSConstants.TASK_DECREMENTING_QUEUE_COUNT);
            throw rdbmsStoreUtils.convertSQLException("Error occurred while " + RDBMSConstants
                    .TASK_DECREMENTING_QUEUE_COUNT + " queue name: " + destinationQueueName, e);
        } finally {
            contextWrite.stop();
            close(preparedStatement, RDBMSConstants.TASK_DECREMENTING_QUEUE_COUNT);
            close(connection, RDBMSConstants.TASK_DECREMENTING_QUEUE_COUNT);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void storeExchangeInformation(String exchangeName, String exchangeInfo)
            throws AndesException {

        Connection connection = null;
        PreparedStatement preparedStatement = null;
        Timer.Context contextWrite = AndesContext.getInstance().getMetricService().timer(MetricsConstants.DB_WRITE,
                                                                                         Level.INFO).start();
        try {

            connection = getConnection();
            // If exchange doesn't exist in DB create exchange
            // NOTE: Qpid tries to create default exchanges at startup. If this
            // is not a vanilla setup DB already have the created exchanges. hence need to check
            // for existence before insertion.
            // This check is done here rather than inside Qpid code that will be updated in
            // future.

            if (!isExchangeExist(connection, exchangeName)) {

                preparedStatement = connection
                        .prepareStatement(RDBMSConstants.PS_STORE_EXCHANGE_INFO);
                preparedStatement.setString(1, exchangeName);
                preparedStatement.setString(2, exchangeInfo);
                preparedStatement.executeUpdate();

                connection.commit();
            }
        } catch (SQLException e) {
            rollback(connection, RDBMSConstants.TASK_STORING_EXCHANGE_INFORMATION);
            throw rdbmsStoreUtils.convertSQLException("Error occurred while " + RDBMSConstants
                    .TASK_STORING_EXCHANGE_INFORMATION + " exchange: " + exchangeName, e);
        } finally {
            contextWrite.stop();
            close(preparedStatement, RDBMSConstants.TASK_STORING_EXCHANGE_INFORMATION);
            close(connection, RDBMSConstants.TASK_STORING_EXCHANGE_INFORMATION);
        }
    }

    /**
     * Helper method to check the existence of an exchange in database
     *
     * @param connection   SQL Connection
     * @param exchangeName exchange name to be checked
     * @return return true if exist and wise versa
     * @throws AndesException
     */
    private boolean isExchangeExist(Connection connection, String exchangeName)
            throws AndesException {
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;

        try {
            preparedStatement = connection.prepareStatement(RDBMSConstants.PS_SELECT_EXCHANGE);

            preparedStatement.setString(1, exchangeName);
            resultSet = preparedStatement.executeQuery();
            return resultSet.next(); // if present true
        } catch (SQLException e) {
            throw rdbmsStoreUtils.convertSQLException("Error occurred retrieving exchange information for" +
                                                              " exchange: " +
                                                              exchangeName, e);
        } finally {
            close(resultSet, RDBMSConstants.TASK_IS_EXCHANGE_EXIST);
            close(preparedStatement, RDBMSConstants.TASK_IS_EXCHANGE_EXIST);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<AndesExchange> getAllExchangesStored() throws AndesException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        Timer.Context contextRead = AndesContext.getInstance().getMetricService().timer(MetricsConstants.DB_READ,
                                                                                        Level.INFO).start();
        try {
            List<AndesExchange> exchangeList = new ArrayList<>();

            connection = getConnection();
            preparedStatement = connection
                    .prepareStatement(RDBMSConstants.PS_SELECT_ALL_EXCHANGE_INFO);
            resultSet = preparedStatement.executeQuery();

            // traverse the result set and add it to exchange list and return the list
            while (resultSet.next()) {
                AndesExchange andesExchange = new AndesExchange(
                        resultSet.getString(RDBMSConstants.EXCHANGE_DATA)
                );
                exchangeList.add(andesExchange);
            }
            return exchangeList;
        } catch (SQLException e) {
            throw rdbmsStoreUtils.convertSQLException("Error occurred while " + RDBMSConstants
                    .TASK_RETRIEVING_ALL_EXCHANGE_INFO, e);
        } finally {
            contextRead.stop();
            close(resultSet, RDBMSConstants.TASK_RETRIEVING_ALL_EXCHANGE_INFO);
            close(preparedStatement, RDBMSConstants.TASK_RETRIEVING_ALL_EXCHANGE_INFO);
            close(connection, RDBMSConstants.TASK_RETRIEVING_ALL_EXCHANGE_INFO);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteExchangeInformation(String exchangeName) throws AndesException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        Timer.Context contextWrite = AndesContext.getInstance().getMetricService().timer(MetricsConstants.DB_WRITE,
                                                                                         Level.INFO).start();
        try {

            connection = getConnection();

            preparedStatement = connection.prepareStatement(RDBMSConstants.PS_DELETE_EXCHANGE);
            preparedStatement.setString(1, exchangeName);
            preparedStatement.executeUpdate();

            connection.commit();

        } catch (SQLException e) {
            String errMsg = RDBMSConstants.TASK_DELETING_EXCHANGE + " exchange: " + exchangeName;
            rollback(connection, errMsg);
            throw rdbmsStoreUtils.convertSQLException("Error occurred while " + errMsg, e);
        } finally {
            contextWrite.stop();
            close(preparedStatement, RDBMSConstants.TASK_DELETING_EXCHANGE);
            close(connection, RDBMSConstants.TASK_DELETING_EXCHANGE);
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void storeQueueInformation(String queueName, String queueInfo) throws AndesException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        Timer.Context contextWrite = AndesContext.getInstance().getMetricService().timer(MetricsConstants.DB_WRITE,
                                                                                         Level.INFO).start();

        try {
            connection = getConnection();

            preparedStatement = connection.prepareStatement(RDBMSConstants.PS_INSERT_QUEUE_INFO);
            preparedStatement.setString(1, queueName);
            preparedStatement.setString(2, queueInfo);
            preparedStatement.executeUpdate();

            connection.commit();
        } catch (SQLException e) {
            AndesException andesException =
                    rdbmsStoreUtils.convertSQLException("Error occurred while storing queue", e);
            String errMsg = RDBMSConstants.TASK_STORING_QUEUE_INFO + " queue name:" + queueName;
            rollback(connection, errMsg);
            if (andesException instanceof AndesDataIntegrityViolationException) {
                // This exception occurred because some other node has created the queue in parallel.
                // Therefore no need to create the queue. It's already created.
                // Nothing need to be done if this exception occur.
                logger.warn("Queue already created. Skipping queue insert [" + queueName + "] to database ");
            } else {
                logger.error("Error occurred while storing queue [" + queueName + "] to database ");
                throw new AndesException(andesException);
            }
        } finally {
            contextWrite.stop();
            close(preparedStatement, RDBMSConstants.TASK_STORING_QUEUE_INFO);
            close(connection, RDBMSConstants.TASK_STORING_QUEUE_INFO);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<AndesQueue> getAllQueuesStored() throws AndesException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        Timer.Context contextRead =
                AndesContext.getInstance().getMetricService().timer(MetricsConstants.DB_READ, Level.INFO).start();
        try {
            connection = getConnection();
            preparedStatement = connection.prepareStatement(RDBMSConstants.PS_SELECT_ALL_QUEUE_INFO);
            resultSet = preparedStatement.executeQuery();

            List<AndesQueue> queueList = new ArrayList<>();
            // iterate through the result set and add to queue list
            while (resultSet.next()) {
                AndesQueue andesQueue = new AndesQueue(
                        resultSet.getString(RDBMSConstants.QUEUE_DATA)
                );
                queueList.add(andesQueue);
            }

            return queueList;
        } catch (SQLException e) {
            throw rdbmsStoreUtils.convertSQLException(
                    "Error occurred while " + RDBMSConstants.TASK_RETRIEVING_ALL_QUEUE_INFO, e);
        } finally {
            contextRead.stop();
            close(resultSet, RDBMSConstants.TASK_RETRIEVING_ALL_QUEUE_INFO);
            close(preparedStatement, RDBMSConstants.TASK_RETRIEVING_ALL_QUEUE_INFO);
            close(connection, RDBMSConstants.TASK_RETRIEVING_ALL_QUEUE_INFO);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteQueueInformation(String queueName) throws AndesException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        Timer.Context contextWrite = AndesContext.getInstance().getMetricService().timer(MetricsConstants.DB_WRITE,
                                                                                         Level.INFO).start();

        try {

            connection = getConnection();

            preparedStatement = connection.prepareStatement(RDBMSConstants.PS_DELETE_QUEUE_INFO);
            preparedStatement.setString(1, queueName);
            preparedStatement.executeUpdate();

            connection.commit();

        } catch (SQLException e) {
            String errMsg = RDBMSConstants.TASK_DELETING_QUEUE_INFO + "queue name: " + queueName;
            rollback(connection, errMsg);
            throw rdbmsStoreUtils.convertSQLException("Error occurred while " + errMsg, e);
        } finally {
            contextWrite.stop();
            close(preparedStatement, RDBMSConstants.TASK_DELETING_QUEUE_INFO);
            close(connection, RDBMSConstants.TASK_DELETING_QUEUE_INFO);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void storeBindingInformation(String exchange, String boundQueueName, String bindingInfo)
            throws AndesException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        Timer.Context contextWrite = AndesContext.getInstance().getMetricService().timer(MetricsConstants.DB_WRITE,
                                                                                         Level.INFO).start();

        try {

            connection = getConnection();

            preparedStatement = connection.prepareStatement(RDBMSConstants.PS_INSERT_BINDING);
            preparedStatement.setString(1, exchange);
            preparedStatement.setString(2, boundQueueName);
            preparedStatement.setString(3, bindingInfo);
            preparedStatement.executeUpdate();

            connection.commit();

        } catch (SQLException e) {
            String errMsg = RDBMSConstants.TASK_STORING_BINDING + " exchange: " + exchange +
                    " queue: " + boundQueueName + " routing key: " + bindingInfo;
            rollback(connection, errMsg);
            throw rdbmsStoreUtils.convertSQLException("Error occurred while " + errMsg, e);
        } finally {
            contextWrite.stop();
            close(preparedStatement, RDBMSConstants.TASK_STORING_BINDING);
            close(connection, RDBMSConstants.TASK_STORING_BINDING);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<AndesBinding> getBindingsStoredForExchange(String exchangeName)
            throws AndesException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        Timer.Context contextRead = AndesContext.getInstance().getMetricService().timer(MetricsConstants.DB_READ,
                                                                                        Level.INFO).start();

        try {
            List<AndesBinding> bindingList = new ArrayList<>();
            connection = getConnection();

            preparedStatement = connection.prepareStatement(RDBMSConstants
                                                                    .PS_SELECT_BINDINGS_FOR_EXCHANGE);
            preparedStatement.setString(1, exchangeName);
            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                AndesBinding andesBinding = new AndesBinding(
                        resultSet.getString(RDBMSConstants.BINDING_INFO)
                );
                bindingList.add(andesBinding);
            }

            return bindingList;
        } catch (SQLException e) {
            throw rdbmsStoreUtils.convertSQLException(
                    "Error occurred while " + RDBMSConstants.TASK_RETRIEVING_BINDING_INFO, e);
        } finally {
            contextRead.stop();
            close(resultSet, RDBMSConstants.TASK_RETRIEVING_BINDING_INFO);
            close(preparedStatement, RDBMSConstants.TASK_RETRIEVING_BINDING_INFO);
            close(connection, RDBMSConstants.TASK_RETRIEVING_BINDING_INFO);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteBindingInformation(String exchangeName, String boundQueueName)
            throws AndesException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        Timer.Context contextWrite = AndesContext.getInstance().getMetricService().timer(MetricsConstants.DB_WRITE,
                                                                                         Level.INFO).start();

        try {
            connection = getConnection();

            preparedStatement = connection.prepareStatement(RDBMSConstants.PS_DELETE_BINDING);
            preparedStatement.setString(1, exchangeName);
            preparedStatement.setString(2, boundQueueName);
            preparedStatement.executeUpdate();

            connection.commit();
        } catch (SQLException e) {
            String errMsg =
                    RDBMSConstants.TASK_DELETING_BINDING + " exchange: " + exchangeName + " bound queue: " +
                            boundQueueName;
            rollback(connection, errMsg);
            throw rdbmsStoreUtils.convertSQLException("Error occurred while " + errMsg, e);
        } finally {
            contextWrite.stop();
            close(preparedStatement, RDBMSConstants.TASK_DELETING_BINDING);
            close(connection, RDBMSConstants.TASK_DELETING_BINDING);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        // nothing to do here.
    }

    /**
     * Creates a connection using a thread pooled data source object and returns the connection
     *
     * @return Connection
     * @throws SQLException
     */
    protected Connection getConnection() throws SQLException {
        return datasource.getConnection();
    }

    /**
     * Closes the provided connection. on failure log the error;
     *
     * @param connection Connection
     * @param task       task that was done before closing
     */
    protected void close(Connection connection, String task) {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            logger.error("Failed to close connection after " + task, e);
        }
    }

    /**
     * On database update failure tries to rollback
     *
     * @param connection database connection
     * @param task       explanation of the task done when the rollback was triggered
     */
    protected void rollback(Connection connection, String task) {
        if (connection != null) {
            try {
                connection.rollback();
            } catch (SQLException e) {
                logger.warn("Rollback failed on " + task, e);
            }
        }
    }

    /**
     * close the prepared statement resource
     *
     * @param preparedStatement PreparedStatement
     * @param task              task that was done by the closed prepared statement.
     */
    protected void close(PreparedStatement preparedStatement, String task) {
        if (preparedStatement != null) {
            try {
                preparedStatement.close();
            } catch (SQLException e) {
                logger.error("Closing prepared statement failed after " + task, e);
            }
        }
    }

    /**
     * closes the result set resources
     *
     * @param resultSet ResultSet
     * @param task      task that was done by the closed result set.
     */
    protected void close(ResultSet resultSet, String task) {
        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (SQLException e) {
                logger.error("Closing result set failed after " + task, e);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createSlot(long startMessageId, long endMessageId, String storageQueueName,
                           String assignedNodeId) throws AndesException {

        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {

            connection = getConnection();

            preparedStatement =
                    connection.prepareStatement(RDBMSConstants.PS_INSERT_SLOT);
            preparedStatement.setLong(1, startMessageId);
            preparedStatement.setLong(2, endMessageId);
            preparedStatement.setString(3, storageQueueName);
            preparedStatement.setString(4, assignedNodeId);

            preparedStatement.executeUpdate();
            connection.commit();
        } catch (SQLException e) {
            String errMsg =
                    RDBMSConstants.TASK_CREATE_SLOT + " startMessageId: " + startMessageId + " endMessageId: " +
                            endMessageId + " storageQueueName:" + storageQueueName + " assignedNodeId:" +
                            assignedNodeId;
            rollback(connection, RDBMSConstants.TASK_CREATE_SLOT);
            throw rdbmsStoreUtils.convertSQLException("Error occurred while " + errMsg, e);
        } finally {
            close(preparedStatement, RDBMSConstants.TASK_CREATE_SLOT);
            close(connection, RDBMSConstants.TASK_CREATE_SLOT);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean deleteSlot(long startMessageId, long endMessageId) throws AndesException {
        Connection connection = null;
        PreparedStatement deleteNonOverlappingSlotPS = null;
        PreparedStatement getSlotPS = null;

        boolean slotDeleted;

        try {

            connection = getConnection();

            deleteNonOverlappingSlotPS = connection.prepareStatement(RDBMSConstants.PS_DELETE_NON_OVERLAPPING_SLOT);
            deleteNonOverlappingSlotPS.setLong(1, startMessageId);
            deleteNonOverlappingSlotPS.setLong(2, endMessageId);

            int rowsAffected = deleteNonOverlappingSlotPS.executeUpdate();
            connection.commit();

            if (rowsAffected == 0) {
                // Check if the Slot exists in Store
                getSlotPS = connection.prepareStatement(RDBMSConstants.PS_GET_SLOT);
                getSlotPS.setLong(1, startMessageId);
                getSlotPS.setLong(2, endMessageId);

                ResultSet resultSet = getSlotPS.executeQuery();

                // slotDeleted set to true if there is no overlapping slot in the DB
                slotDeleted = !resultSet.next();
                resultSet.close();
            } else {
                slotDeleted = true;
            }

            if (logger.isDebugEnabled()) {
                if (slotDeleted) {
                    logger.debug("Slot deleted, startMessageId " + startMessageId + " endMessageId" + endMessageId);
                } else {
                    logger.debug(
                            "Cannot delete slot, startMessageId " + startMessageId + " endMessageId" + endMessageId);
                }
            }

            return slotDeleted;
        } catch (SQLException e) {
            String errMsg =
                    RDBMSConstants.TASK_DELETE_SLOT + " startMessageId: " + startMessageId + " endMessageId: " +
                            endMessageId;
            rollback(connection, RDBMSConstants.TASK_DELETE_SLOT);
            throw rdbmsStoreUtils.convertSQLException("Error occurred while " + errMsg, e);
        } finally {
            close(deleteNonOverlappingSlotPS, RDBMSConstants.TASK_DELETE_SLOT);
            close(getSlotPS, RDBMSConstants.TASK_DELETE_SLOT);
            close(connection, RDBMSConstants.TASK_DELETE_SLOT);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteSlotsByQueueName(String queueName) throws AndesException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {

            connection = getConnection();

            preparedStatement =
                    connection.prepareStatement(RDBMSConstants.PS_DELETE_SLOTS_BY_QUEUE_NAME);
            preparedStatement.setString(1, queueName);

            preparedStatement.executeUpdate();
            connection.commit();
        } catch (SQLException e) {
            String errMsg =
                    RDBMSConstants.TASK_DELETE_SLOT_BY_QUEUE_NAME + " queueName: " + queueName;
            rollback(connection, RDBMSConstants.TASK_DELETE_SLOT_BY_QUEUE_NAME);
            throw rdbmsStoreUtils.convertSQLException("Error occurred while " + errMsg, e);
        } finally {
            close(preparedStatement, RDBMSConstants.TASK_DELETE_SLOT_BY_QUEUE_NAME);
            close(connection, RDBMSConstants.TASK_DELETE_SLOT_BY_QUEUE_NAME);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteMessageIdsByQueueName(String queueName) throws AndesException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {

            connection = getConnection();

            preparedStatement =
                    connection.prepareStatement(RDBMSConstants.PS_DELETE_MESSAGE_IDS_BY_QUEUE_NAME);
            preparedStatement.setString(1, queueName);

            preparedStatement.executeUpdate();
            connection.commit();
        } catch (SQLException e) {
            String errMsg =
                    RDBMSConstants.TASK_DELETE_MESSAGE_ID_BY_QUEUE_NAME + " queueName: " + queueName;
            rollback(connection, RDBMSConstants.TASK_DELETE_MESSAGE_ID_BY_QUEUE_NAME);
            throw rdbmsStoreUtils.convertSQLException("Error occurred while " + errMsg, e);
        } finally {
            close(preparedStatement, RDBMSConstants.TASK_DELETE_MESSAGE_ID_BY_QUEUE_NAME);
            close(connection, RDBMSConstants.TASK_DELETE_MESSAGE_ID_BY_QUEUE_NAME);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createSlotAssignment(String nodeId, String queueName, long startMsgId,
                                     long endMsgId)
            throws AndesException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {

            connection = getConnection();

            preparedStatement =
                    connection.prepareStatement(RDBMSConstants.PS_INSERT_SLOT_ASSIGNMENT);
            preparedStatement.setString(1, nodeId);
            preparedStatement.setString(2, queueName);
            preparedStatement.setLong(3, startMsgId);
            preparedStatement.setLong(4, endMsgId);

            preparedStatement.executeUpdate();
            connection.commit();
        } catch (SQLException e) {
            String errMsg =
                    RDBMSConstants.TASK_CREATE_SLOT_ASSIGNMENT + " nodeId: " + nodeId + " queueName: " +
                            queueName + "startMsgId: " + startMsgId + "endMsgId: " + endMsgId;
            rollback(connection, RDBMSConstants.TASK_CREATE_SLOT_ASSIGNMENT);
            throw rdbmsStoreUtils.convertSQLException("Error occurred while " + errMsg, e);
        } finally {
            close(preparedStatement, RDBMSConstants.TASK_CREATE_SLOT_ASSIGNMENT);
            close(connection, RDBMSConstants.TASK_CREATE_SLOT_ASSIGNMENT);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteSlotAssignment(long startMessageId, long endMessageId)
            throws AndesException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {

            connection = getConnection();

            preparedStatement =
                    connection.prepareStatement(RDBMSConstants.PS_DELETE_SLOT_ASSIGNMENT);
            preparedStatement.setLong(1, startMessageId);
            preparedStatement.setLong(2, endMessageId);

            preparedStatement.executeUpdate();
            connection.commit();
        } catch (SQLException e) {
            String errMsg =
                    RDBMSConstants.TASK_DELETE_SLOT_ASSIGNMENT + " startMessageId: " + startMessageId + " " +
                            "endMessageId: " +
                            endMessageId;
            rollback(connection, RDBMSConstants.TASK_DELETE_SLOT_ASSIGNMENT);
            throw rdbmsStoreUtils.convertSQLException("Error occurred while " + errMsg, e);
        } finally {
            close(preparedStatement, RDBMSConstants.TASK_DELETE_SLOT_ASSIGNMENT);
            close(connection, RDBMSConstants.TASK_DELETE_SLOT_ASSIGNMENT);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteSlotAssignmentByQueueName(String nodeId, String queueName)
            throws AndesException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {

            connection = getConnection();

            preparedStatement = connection
                    .prepareStatement(RDBMSConstants.PS_DELETE_SLOT_ASSIGNMENT_BY_QUEUE_NAME);
            preparedStatement.setString(1, nodeId);
            preparedStatement.setString(2, queueName);

            preparedStatement.executeUpdate();

            connection.commit();
        } catch (SQLException e) {
            String errMsg =
                    RDBMSConstants.PS_DELETE_SLOT_ASSIGNMENT_BY_QUEUE_NAME + " nodeId: " + nodeId + " queueName: " +
                            queueName;
            rollback(connection, RDBMSConstants.PS_DELETE_SLOT_ASSIGNMENT_BY_QUEUE_NAME);
            throw rdbmsStoreUtils.convertSQLException("Error occurred while " + errMsg, e);
        } finally {
            close(preparedStatement, RDBMSConstants.PS_DELETE_SLOT_ASSIGNMENT_BY_QUEUE_NAME);
            close(connection, RDBMSConstants.PS_DELETE_SLOT_ASSIGNMENT_BY_QUEUE_NAME);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Slot selectUnAssignedSlot(String queueName) throws AndesException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        Slot unAssignedSlot = null;

        try {
            connection = getConnection();

            preparedStatement =
                    connection.prepareStatement(RDBMSConstants.PS_SELECT_UNASSIGNED_SLOT);

            preparedStatement.setString(1, queueName);
            resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                unAssignedSlot = new Slot(SlotState.RETURNED);
                unAssignedSlot.setStartMessageId(resultSet.getLong(RDBMSConstants.START_MESSAGE_ID));
                unAssignedSlot.setEndMessageId(resultSet.getLong(RDBMSConstants.END_MESSAGE_ID));
                unAssignedSlot.setStorageQueueName(resultSet.getString(RDBMSConstants.STORAGE_QUEUE_NAME));
            }

            return unAssignedSlot;
        } catch (SQLException e) {
            String errMsg =
                    RDBMSConstants.TASK_SELECT_UNASSIGNED_SLOTS + " queueName: " + queueName;
            throw rdbmsStoreUtils.convertSQLException("Error occurred while " + errMsg, e);
        } finally {
            close(resultSet, RDBMSConstants.TASK_SELECT_UNASSIGNED_SLOTS);
            close(preparedStatement, RDBMSConstants.TASK_SELECT_UNASSIGNED_SLOTS);
            close(connection, RDBMSConstants.TASK_SELECT_UNASSIGNED_SLOTS);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getQueueToLastAssignedId(String queueName) throws AndesException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        long messageId = 0L;

        try {
            connection = getConnection();

            preparedStatement =
                    connection.prepareStatement(RDBMSConstants.PS_SELECT_QUEUE_TO_LAST_ASSIGNED_ID);
            preparedStatement.setString(1, queueName);
            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                messageId = resultSet.getLong(RDBMSConstants.MESSAGE_ID);
            }
            return messageId;
        } catch (SQLException e) {
            String errMsg =
                    RDBMSConstants.TASK_GET_QUEUE_TO_LAST_ASSIGNED_ID + " queueName: " + queueName;
            throw rdbmsStoreUtils.convertSQLException("Error occurred while " + errMsg, e);
        } finally {
            close(resultSet, RDBMSConstants.TASK_GET_QUEUE_TO_LAST_ASSIGNED_ID);
            close(preparedStatement, RDBMSConstants.TASK_GET_QUEUE_TO_LAST_ASSIGNED_ID);
            close(connection, RDBMSConstants.TASK_GET_QUEUE_TO_LAST_ASSIGNED_ID);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setQueueToLastAssignedId(String queueName, long messageId) throws AndesException {
        Connection connection = null;
        PreparedStatement selectQueueToLastAssignIDPS = null;
        PreparedStatement updateQueueToLastAssignedIDPS = null;
        PreparedStatement insertQueueToLastAssignedIDPS = null;
        ResultSet resultSet;

        try {

            connection = getConnection();
            selectQueueToLastAssignIDPS =
                    connection.prepareStatement(RDBMSConstants.PS_SELECT_QUEUE_TO_LAST_ASSIGNED_ID);
            selectQueueToLastAssignIDPS.setString(1, queueName);
            resultSet = selectQueueToLastAssignIDPS.executeQuery();

            if (resultSet.next()) {
                updateQueueToLastAssignedIDPS =
                        connection.prepareStatement(RDBMSConstants.PS_UPDATE_QUEUE_TO_LAST_ASSIGNED_ID);
                updateQueueToLastAssignedIDPS.setLong(1, messageId);
                updateQueueToLastAssignedIDPS.setString(2, queueName);
                updateQueueToLastAssignedIDPS.executeUpdate();
            } else {
                insertQueueToLastAssignedIDPS =
                        connection.prepareStatement(RDBMSConstants.PS_INSERT_QUEUE_TO_LAST_ASSIGNED_ID);

                insertQueueToLastAssignedIDPS.setString(1, queueName);
                insertQueueToLastAssignedIDPS.setLong(2, messageId);
                insertQueueToLastAssignedIDPS.executeUpdate();
            }

            connection.commit();

        } catch (SQLException e) {
            String errMsg =
                    RDBMSConstants.TASK_SET_QUEUE_TO_LAST_ASSIGNED_ID + " queueName: " + queueName + " messageId: " +
                            messageId;
            rollback(connection, RDBMSConstants.TASK_SET_QUEUE_TO_LAST_ASSIGNED_ID);
            throw rdbmsStoreUtils.convertSQLException("Error occurred while " + errMsg, e);
        } finally {
            close(selectQueueToLastAssignIDPS, RDBMSConstants.TASK_SET_QUEUE_TO_LAST_ASSIGNED_ID);
            close(updateQueueToLastAssignedIDPS, RDBMSConstants.TASK_SET_QUEUE_TO_LAST_ASSIGNED_ID);
            close(insertQueueToLastAssignedIDPS, RDBMSConstants.TASK_SET_QUEUE_TO_LAST_ASSIGNED_ID);
            close(connection, RDBMSConstants.TASK_SET_QUEUE_TO_LAST_ASSIGNED_ID);
        }
    }

    /**
     * {@inheritDoc}
     */
    public long getLocalSafeZoneOfNode(String nodeId) throws AndesException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        long messageId = 0L;

        try {
            connection = getConnection();

            preparedStatement =
                    connection.prepareStatement(RDBMSConstants.PS_SELECT_NODE_TO_LAST_PUBLISHED_ID);
            preparedStatement.setString(1, nodeId);
            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                messageId = resultSet.getLong(RDBMSConstants.MESSAGE_ID);
            }
            return messageId;
        } catch (SQLException e) {
            String errMsg =
                    RDBMSConstants.TASK_GET_NODE_TO_LAST_PUBLISHED_ID + " nodeId: " + nodeId;
            throw rdbmsStoreUtils.convertSQLException("Error occurred while " + errMsg, e);
        } finally {
            close(resultSet, RDBMSConstants.TASK_GET_NODE_TO_LAST_PUBLISHED_ID);
            close(preparedStatement, RDBMSConstants.TASK_GET_NODE_TO_LAST_PUBLISHED_ID);
            close(connection, RDBMSConstants.TASK_GET_NODE_TO_LAST_PUBLISHED_ID);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setLocalSafeZoneOfNode(String nodeId, long messageId) throws AndesException {
        Connection connection = null;
        PreparedStatement selectNodeToLastPublishedIdPS = null;
        PreparedStatement updateNodeToLastPublishedIdPS = null;
        PreparedStatement insertNodeToLastPublishedIdPS = null;
        ResultSet resultSet;

        try {

            connection = getConnection();

            selectNodeToLastPublishedIdPS =
                    connection.prepareStatement(RDBMSConstants.PS_SELECT_NODE_TO_LAST_PUBLISHED_ID);
            selectNodeToLastPublishedIdPS.setString(1, nodeId);
            resultSet = selectNodeToLastPublishedIdPS.executeQuery();

            if (resultSet.next()) {
                updateNodeToLastPublishedIdPS =
                        connection.prepareStatement(RDBMSConstants.PS_UPDATE_NODE_TO_LAST_PUBLISHED_ID);
                updateNodeToLastPublishedIdPS.setLong(1, messageId);
                updateNodeToLastPublishedIdPS.setString(2, nodeId);
                updateNodeToLastPublishedIdPS.executeUpdate();
            } else {
                insertNodeToLastPublishedIdPS =
                        connection.prepareStatement(RDBMSConstants.PS_INSERT_NODE_TO_LAST_PUBLISHED_ID);

                insertNodeToLastPublishedIdPS.setString(1, nodeId);
                insertNodeToLastPublishedIdPS.setLong(2, messageId);
                insertNodeToLastPublishedIdPS.executeUpdate();
            }

            connection.commit();
        } catch (SQLException e) {
            String errMsg =
                    RDBMSConstants.TASK_SET_NODE_TO_LAST_PUBLISHED_ID + " nodeId: " + nodeId + " messageId: " +
                            messageId;
            rollback(connection, RDBMSConstants.TASK_SET_NODE_TO_LAST_PUBLISHED_ID);
            throw rdbmsStoreUtils.convertSQLException("Error occurred while " + errMsg, e);
        } finally {
            close(selectNodeToLastPublishedIdPS, RDBMSConstants.TASK_SET_NODE_TO_LAST_PUBLISHED_ID);
            close(updateNodeToLastPublishedIdPS, RDBMSConstants.TASK_SET_NODE_TO_LAST_PUBLISHED_ID);
            close(insertNodeToLastPublishedIdPS, RDBMSConstants.TASK_SET_NODE_TO_LAST_PUBLISHED_ID);
            close(connection, RDBMSConstants.TASK_SET_NODE_TO_LAST_PUBLISHED_ID);
        }
    }

    @Override
    public void removePublisherNodeId(String nodeId) throws AndesException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {

            connection = getConnection();

            preparedStatement = connection.prepareStatement(RDBMSConstants.PS_DELETE_PUBLISHER_ID);

            preparedStatement.setString(1, nodeId);

            preparedStatement.executeUpdate();
            connection.commit();
        } catch (SQLException e) {
            String errMsg =
                    RDBMSConstants.TASK_DELETE_PUBLISHER_ID + " node ID: " + nodeId;
            rollback(connection, RDBMSConstants.TASK_DELETE_MESSAGE_ID);
            throw rdbmsStoreUtils.convertSQLException("Error occurred while " + errMsg, e);
        } finally {
            close(preparedStatement, RDBMSConstants.TASK_DELETE_MESSAGE_ID);
            close(connection, RDBMSConstants.TASK_DELETE_MESSAGE_ID);
        }
    }

    /**
     * {@inheritDoc}
     */
    public TreeSet<String> getMessagePublishedNodes() throws AndesException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        TreeSet<String> nodeList = new TreeSet<>();

        try {
            connection = getConnection();

            preparedStatement =
                    connection.prepareStatement(RDBMSConstants.PS_SELECT_MESSAGE_PUBLISHED_NODES);
            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                nodeList.add(resultSet.getString(RDBMSConstants.NODE_ID));
            }
            return nodeList;
        } catch (SQLException e) {
            String errMsg =
                    RDBMSConstants.TASK_GET_MESSAGE_PUBLISHED_NODES;
            throw rdbmsStoreUtils.convertSQLException("Error occurred while " + errMsg, e);
        } finally {
            close(resultSet, RDBMSConstants.TASK_GET_MESSAGE_PUBLISHED_NODES);
            close(preparedStatement, RDBMSConstants.TASK_GET_MESSAGE_PUBLISHED_NODES);
            close(connection, RDBMSConstants.TASK_GET_MESSAGE_PUBLISHED_NODES);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setSlotState(long startMessageId, long endMessageId, SlotState slotState)
            throws AndesException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {

            connection = getConnection();

            preparedStatement = connection.prepareStatement(RDBMSConstants.PS_SET_SLOT_STATE);
            preparedStatement.setInt(1, slotState.getCode());
            preparedStatement.setLong(2, startMessageId);
            preparedStatement.setLong(3, endMessageId);

            preparedStatement.executeUpdate();
            connection.commit();
        } catch (SQLException e) {
            String errMsg =
                    RDBMSConstants.TASK_SET_SLOT_STATE + " startMessageId: " + startMessageId + " endMessageId: " +
                            endMessageId + " slotState:" + slotState;
            rollback(connection, RDBMSConstants.TASK_SET_SLOT_STATE);
            throw rdbmsStoreUtils.convertSQLException("Error occurred while " + errMsg, e);
        } finally {
            close(preparedStatement, RDBMSConstants.TASK_SET_SLOT_STATE);
            close(connection, RDBMSConstants.TASK_SET_SLOT_STATE);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Slot getOverlappedSlot(String nodeId, String queueName) throws AndesException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        Slot overlappedSlot = null;

        try {
            connection = getConnection();

            preparedStatement =
                    connection.prepareStatement(RDBMSConstants.PS_SELECT_OVERLAPPED_SLOT);
            preparedStatement.setString(1, queueName);
            preparedStatement.setString(2, nodeId);
            resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                overlappedSlot = new Slot(SlotState.OVERLAPPED);
                overlappedSlot.setStartMessageId(resultSet.getLong(RDBMSConstants.START_MESSAGE_ID));
                overlappedSlot.setEndMessageId(resultSet.getLong(RDBMSConstants.END_MESSAGE_ID));
                overlappedSlot.setStorageQueueName(
                        resultSet.getString(RDBMSConstants.STORAGE_QUEUE_NAME));
                overlappedSlot.setAnOverlappingSlot(true);
            }
            return overlappedSlot;
        } catch (SQLException e) {
            String errMsg = RDBMSConstants.TASK_GET_OVERLAPPED_SLOT + " queueName: " + queueName;
            logger.error("Error occurred while " + errMsg, e);
            throw rdbmsStoreUtils.convertSQLException("Error occurred while " + errMsg, e);
        } finally {
            close(resultSet, RDBMSConstants.TASK_GET_OVERLAPPED_SLOT);
            close(preparedStatement, RDBMSConstants.TASK_GET_OVERLAPPED_SLOT);
            close(connection, RDBMSConstants.TASK_GET_OVERLAPPED_SLOT);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void addMessageId(String queueName, long messageId) throws AndesException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {

            connection = getConnection();

            preparedStatement =
                    connection.prepareStatement(RDBMSConstants.PS_INSERT_SLOT_MESSAGE_ID);

            preparedStatement.setString(1, queueName);
            preparedStatement.setLong(2, messageId);

            preparedStatement.executeUpdate();
            connection.commit();
        } catch (SQLException e) {
            String errMsg =
                    RDBMSConstants.TASK_ADD_MESSAGE_ID + " queueName: " + queueName + " messageId: " + messageId;
            rollback(connection, RDBMSConstants.TASK_ADD_MESSAGE_ID);
            throw rdbmsStoreUtils.convertSQLException("Error occurred while " + errMsg, e);
        } finally {
            close(preparedStatement, RDBMSConstants.TASK_ADD_MESSAGE_ID);
            close(connection, RDBMSConstants.TASK_ADD_MESSAGE_ID);
        }
    }

    /**
     * {@inheritDoc}
     */
    public TreeSet<Long> getMessageIds(String queueName) throws AndesException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        TreeSet<Long> messageIdSet = new TreeSet<>();

        try {
            connection = getConnection();

            preparedStatement = connection.prepareStatement(RDBMSConstants.PS_GET_MESSAGE_IDS);
            preparedStatement.setString(1, queueName);
            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                messageIdSet.add(resultSet.getLong(RDBMSConstants.MESSAGE_ID));
            }
            return messageIdSet;
        } catch (SQLException e) {
            String errMsg =
                    RDBMSConstants.TASK_GET_MESSAGE_IDS + " queueName: " + queueName;
            throw rdbmsStoreUtils.convertSQLException("Error occurred while " + errMsg, e);
        } finally {
            close(resultSet, RDBMSConstants.TASK_GET_MESSAGE_IDS);
            close(preparedStatement, RDBMSConstants.TASK_GET_MESSAGE_IDS);
            close(connection, RDBMSConstants.TASK_GET_MESSAGE_IDS);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void deleteMessageId(long messageId) throws AndesException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;

        try {

            connection = getConnection();

            preparedStatement = connection.prepareStatement(RDBMSConstants.PS_DELETE_MESSAGE_ID);

            preparedStatement.setLong(1, messageId);

            preparedStatement.executeUpdate();
            connection.commit();
        } catch (SQLException e) {
            String errMsg =
                    RDBMSConstants.TASK_DELETE_MESSAGE_ID + " messageId: " + messageId;
            rollback(connection, RDBMSConstants.TASK_DELETE_MESSAGE_ID);
            throw rdbmsStoreUtils.convertSQLException("Error occurred while " + errMsg, e);
        } finally {
            close(preparedStatement, RDBMSConstants.TASK_DELETE_MESSAGE_ID);
            close(connection, RDBMSConstants.TASK_DELETE_MESSAGE_ID);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TreeSet<Slot> getAssignedSlotsByNodeId(String nodeId) throws AndesException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        TreeSet<Slot> assignedSlotSet = new TreeSet<>();

        try {
            connection = getConnection();

            preparedStatement =
                    connection.prepareStatement(RDBMSConstants.PS_GET_ASSIGNED_SLOTS_BY_NODE_ID);
            preparedStatement.setString(1, nodeId);
            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                Slot assignedSlot = new Slot(SlotState.ASSIGNED);
                assignedSlot.setStartMessageId(resultSet.getLong(RDBMSConstants.START_MESSAGE_ID));
                assignedSlot.setEndMessageId(resultSet.getLong(RDBMSConstants.END_MESSAGE_ID));
                assignedSlot.setStorageQueueName(
                        resultSet.getString(RDBMSConstants.STORAGE_QUEUE_NAME));
                assignedSlotSet.add(assignedSlot);
            }
            return assignedSlotSet;
        } catch (SQLException e) {
            String errMsg =
                    RDBMSConstants.TASK_GET_ASSIGNED_SLOTS_BY_NODE_ID + " nodeId: " + nodeId;
            throw rdbmsStoreUtils.convertSQLException("Error occurred while " + errMsg, e);
        } finally {
            close(resultSet, RDBMSConstants.TASK_GET_ASSIGNED_SLOTS_BY_NODE_ID);
            close(preparedStatement, RDBMSConstants.TASK_GET_ASSIGNED_SLOTS_BY_NODE_ID);
            close(connection, RDBMSConstants.TASK_GET_ASSIGNED_SLOTS_BY_NODE_ID);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public TreeSet<Slot> getAllSlotsByQueueName(String queueName) throws AndesException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        TreeSet<Slot> slotSet = new TreeSet<>();

        try {
            connection = getConnection();

            preparedStatement =
                    connection.prepareStatement(RDBMSConstants.PS_SELECT_ALL_SLOTS_BY_QUEUE_NAME);
            preparedStatement.setString(1, queueName);
            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                Slot slot = new Slot(SlotState.getById(resultSet.getInt(RDBMSConstants.SLOT_STATE)));
                slot.setStartMessageId(resultSet.getLong(RDBMSConstants.START_MESSAGE_ID));
                slot.setEndMessageId(resultSet.getLong(RDBMSConstants.END_MESSAGE_ID));
                slot.setStorageQueueName(resultSet.getString(RDBMSConstants.STORAGE_QUEUE_NAME));
                slot.setSlotInActive();
                slotSet.add(slot);
            }
            return slotSet;
        } catch (SQLException e) {
            String errMsg =
                    RDBMSConstants.TASK_GET_ALL_SLOTS_BY_QUEUE_NAME + " queueName: " + queueName;
            throw rdbmsStoreUtils.convertSQLException("Error occurred while " + errMsg, e);
        } finally {
            close(resultSet, RDBMSConstants.TASK_GET_ALL_SLOTS_BY_QUEUE_NAME);
            close(preparedStatement, RDBMSConstants.TASK_GET_ALL_SLOTS_BY_QUEUE_NAME);
            close(connection, RDBMSConstants.TASK_GET_ALL_SLOTS_BY_QUEUE_NAME);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> getAllQueues() throws AndesException {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        Set<String> queueList = new TreeSet<>();

        try {
            connection = getConnection();

            preparedStatement =
                    connection.prepareStatement(RDBMSConstants.PS_GET_ALL_QUEUES);
            resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                queueList.add(resultSet.getString(RDBMSConstants.STORAGE_QUEUE_NAME));
            }
            return queueList;

        } catch (SQLException e) {
            String errMsg =
                    RDBMSConstants.TASK_GET_ALL_QUEUES;
            throw rdbmsStoreUtils.convertSQLException("Error occurred while " + errMsg, e);
        } finally {
            close(resultSet, RDBMSConstants.TASK_GET_ALL_QUEUES);
            close(preparedStatement, RDBMSConstants.TASK_GET_ALL_QUEUES);
            close(connection, RDBMSConstants.TASK_GET_ALL_QUEUES);
        }
    }

    /**
     * Clear and reset slot storage
     *
     * @throws AndesException
     */
    @Override
    public void clearSlotStorage() throws AndesException {
        Connection connection = null;
        PreparedStatement clearSlotTablePS = null;
        PreparedStatement clearSlotMessageIdTablePS = null;
        PreparedStatement clearNodeToLastPublisherIdPS = null;
        PreparedStatement clearQueueToLastAssignedIdPS = null;

        try {
            connection = getConnection();
            clearSlotTablePS = connection.prepareStatement(RDBMSConstants.PS_CLEAR_SLOT_TABLE);
            clearSlotTablePS.executeUpdate();
            clearSlotMessageIdTablePS = connection.prepareStatement(RDBMSConstants.PS_CLEAR_SLOT_MESSAGE_ID_TABLE);
            clearSlotMessageIdTablePS.executeUpdate();
            clearNodeToLastPublisherIdPS = connection.prepareStatement(
                    RDBMSConstants.PS_CLEAR_NODE_TO_LAST_PUBLISHED_ID);
            clearNodeToLastPublisherIdPS.executeUpdate();
            clearQueueToLastAssignedIdPS = connection.prepareStatement(
                    RDBMSConstants.PS_CLEAR_QUEUE_TO_LAST_ASSIGNED_ID);
            clearQueueToLastAssignedIdPS.executeUpdate();
            connection.commit();
        } catch (SQLException e) {
            String errMsg = RDBMSConstants.TASK_CLEAR_SLOT_TABLES;
            rollback(connection, RDBMSConstants.TASK_CLEAR_SLOT_TABLES);
            throw rdbmsStoreUtils.convertSQLException("Error occurred while " + errMsg, e);
        } finally {
            close(clearSlotTablePS, RDBMSConstants.TASK_CLEAR_SLOT_TABLES);
            close(clearSlotMessageIdTablePS, RDBMSConstants.TASK_CLEAR_SLOT_TABLES);
            close(clearNodeToLastPublisherIdPS, RDBMSConstants.TASK_CLEAR_SLOT_TABLES);
            close(clearQueueToLastAssignedIdPS, RDBMSConstants.TASK_CLEAR_SLOT_TABLES);
            close(connection, RDBMSConstants.TASK_CLEAR_SLOT_TABLES);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isOperational(String testString, long testTime) {
        try {
            // Here order is important
            return rdbmsStoreUtils.testInsert(getConnection(), testString, testTime)
                    && rdbmsStoreUtils.testRead(getConnection(), testString, testTime)
                    && rdbmsStoreUtils.testDelete(getConnection(), testString, testTime);
        } catch (SQLException e) {
            return false;
        }

    }

    private String getDestinationIdentifier(AndesSubscription subscription) {
        return subscription.getDestinationType() + "." + subscription.getSubscribedDestination();
    }

    /**
     * Generates a unique ID for a subscription based on node ID, destination and subscriber's ID
     *
     * @param subscription The subscription
     * @return A subscription ID
     */
    private String generateSubscriptionID(AndesSubscription subscription) {
        return subscription.getSubscribedNode() + "_" + subscription.getSubscribedDestination() + "_" + subscription
                .getSubscriptionID();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addProtocolType(ProtocolType protocolType) {
        protocols.add(protocolType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<ProtocolType> getProtocols() {
        return protocols;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeProtocolType(ProtocolType protocolType) {
        protocols.remove(protocolType);
    }
}
