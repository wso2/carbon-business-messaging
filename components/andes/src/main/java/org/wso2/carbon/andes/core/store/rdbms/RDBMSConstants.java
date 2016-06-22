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

import org.wso2.carbon.andes.core.internal.slot.SlotState;

/**
 * JDBC storage related prepared statements, table names, column names and tasks are grouped
 * in this class.
 */
public class RDBMSConstants {

    // Configuration properties
    protected static final String PROP_JNDI_LOOKUP_NAME = "dataSource";

    /**
     * Configuration name used to specify sql state code classes (i.e first two digits) for database
     * connectivity errors.
     * <p>
     * configuration is configured in broker.xml,
     * <ul>
     * <li>persistence/messageStore/</li>
     * <li>persistence/contextStore/</li>
     * </ul>
     * </p>
     */
    protected static final String STORE_UNAVAILABLE_SQL_STATE_CLASSES = "storeUnavailableSQLStateClasses";

    /**
     * Configuration name used to specify SQL state code classes (i.e first two
     * digits)
     * corresponding to various database server generated errors similar to
     * {@link java.sql.DataTruncation}, {@link java.sql.SQLDataException} (but driver didn't
     * differentiated and just choose set the sql state only)
     * <p>
     * configuration is configured in broker.xml,
     * <ul>
     * <li>persistence/messageStore/</li>
     * <li>persistence/contextStore/</li>
     * </ul>
     * </p>
     */
    protected static final String DATA_INTEGRITY_VIOLATION_SQL_STATE_CLASSES = "integrityViolationSQLStateClasses";

    /**
     * Configuration name used to specify SQL state code classes (i.e first two
     * digits)
     * corresponding to integrity violation errors.
     * <p>
     * configuration is configured in broker.xml,
     * <ul>
     * <li>persistence/messageStore/</li>
     * <li>persistence/contextStore/</li>
     * </ul>
     * </p>
     */
    protected static final String DATA_ERROR_SQL_STATE_CLASSES = "dataErrorSQLStateClasses";


    protected static final String TRANSACTION_ROLLBACK_ERROR_SQL_STATE_CLASSES = "transactionRollbackSQLStateClasses";

    // Message Store tables
    protected static final String CONTENT_TABLE = "MB_CONTENT";
    protected static final String METADATA_TABLE = "MB_METADATA";
    protected static final String QUEUES_TABLE = "MB_QUEUE_MAPPING";
    protected static final String EXPIRATION_TABLE = "MB_EXPIRATION_DATA";
    protected static final String MSG_STORE_STATUS_TABLE = "MB_MSG_STORE_STATUS";
    protected static final String RETAINED_METADATA_TABLE = "MB_RETAINED_METADATA";
    protected static final String RETAINED_CONTENT_TABLE = "MB_RETAINED_CONTENT";


    // Message Store table columns
    protected static final String MESSAGE_ID = "MESSAGE_ID";
    protected static final String QUEUE_ID = "QUEUE_ID";
    protected static final String DLC_QUEUE_ID = "DLC_QUEUE_ID";
    protected static final String QUEUE_NAME = "QUEUE_NAME";
    protected static final String METADATA = "MESSAGE_METADATA";
    protected static final String MSG_OFFSET = "CONTENT_OFFSET";
    protected static final String MESSAGE_CONTENT = "MESSAGE_CONTENT";
    protected static final String EXPIRATION_TIME = "EXPIRATION_TIME";
    protected static final String DESTINATION_QUEUE = "MESSAGE_DESTINATION";
    protected static final String TOPIC_NAME = "TOPIC_NAME";
    protected static final String TOPIC_ID = "TOPIC_ID";

    // Andes Context Store tables
    protected static final String DURABLE_SUB_TABLE = "MB_DURABLE_SUBSCRIPTION";
    protected static final String NODE_INFO_TABLE = "MB_NODE";
    protected static final String EXCHANGES_TABLE = "MB_EXCHANGE";
    protected static final String BINDINGS_TABLE = "MB_BINDING";
    protected static final String QUEUE_INFO_TABLE = "MB_QUEUE";
    protected static final String QUEUE_COUNTER_TABLE = "MB_QUEUE_COUNTER";
    // Slot related tables
    protected static final String SLOT_TABLE = "MB_SLOT";
    protected static final String SLOT_MESSAGE_ID_TABLE = "MB_SLOT_MESSAGE_ID";
    protected static final String QUEUE_TO_LAST_ASSIGNED_ID = "MB_QUEUE_TO_LAST_ASSIGNED_ID";

    /**
     * This dataset maps to the nodeID - localSafeZone association within the broker.
     */
    protected static final String NODE_TO_LAST_PUBLISHED_ID = "MB_NODE_TO_LAST_PUBLISHED_ID";

    // Andes Context Store table columns
    protected static final String DURABLE_SUB_ID = "SUBSCRIPTION_ID";
    protected static final String DESTINATION_IDENTIFIER = "DESTINATION_IDENTIFIER";
    protected static final String DURABLE_SUB_DATA = "SUBSCRIPTION_DATA";
    protected static final String NODE_ID = "NODE_ID";
    protected static final String NODE_INFO = "NODE_DATA";
    protected static final String EXCHANGE_NAME = "EXCHANGE_NAME";
    protected static final String EXCHANGE_DATA = "EXCHANGE_DATA";
    protected static final String BINDING_INFO = "BINDING_DETAILS";
    protected static final String BINDING_QUEUE_NAME = "QUEUE_NAME";
    protected static final String BINDING_EXCHANGE_NAME = "EXCHANGE_NAME";
    protected static final String QUEUE_DATA = "QUEUE_DATA";
    protected static final String MESSAGE_COUNT = "MESSAGE_COUNT";
    protected static final String TIME_STAMP = "TIME_STAMP";

    //Slot table columns
    protected static final String SLOT_ID = "SLOT_ID";
    protected static final String START_MESSAGE_ID = "START_MESSAGE_ID";
    protected static final String END_MESSAGE_ID = "END_MESSAGE_ID";
    protected static final String STORAGE_QUEUE_NAME = "STORAGE_QUEUE_NAME";
    protected static final String SLOT_STATE = "SLOT_STATE";
    protected static final String ASSIGNED_NODE_ID = "ASSIGNED_NODE_ID";
    protected static final String ASSIGNED_QUEUE_NAME = "ASSIGNED_QUEUE_NAME";


    // prepared statements for Message Store
    protected static final String PS_INSERT_MESSAGE_PART =
            "INSERT INTO " + CONTENT_TABLE + "("
                    + MESSAGE_ID + ","
                    + MSG_OFFSET + ","
                    + MESSAGE_CONTENT + ") VALUES (?, ?, ?)";

    protected static final String PS_RETRIEVE_MESSAGE_PART =
            "SELECT " + MESSAGE_CONTENT
                    + " FROM " + CONTENT_TABLE
                    + " WHERE " + MESSAGE_ID + "=?"
                    + " AND " + MSG_OFFSET + "=?";

    /**
     * We need to select rows that have the DLC_QUEUE_ID = -1 indicating that the message is not moved
     * into the dead letter channel
     */
    protected static final String PS_INSERT_METADATA =
            "INSERT INTO " + METADATA_TABLE + " ("
                    + MESSAGE_ID + ","
                    + QUEUE_ID + ","
                    + DLC_QUEUE_ID + ","
                    + METADATA + ")"
                    + " VALUES ( ?,?,-1,? )";

    protected static final String PS_INSERT_EXPIRY_DATA =
            "INSERT INTO " + EXPIRATION_TABLE + " ("
                    + MESSAGE_ID + ","
                    + EXPIRATION_TIME + ","
                    + DESTINATION_QUEUE + ")"
                    + " VALUES ( ?,?,? )";

    protected static final String PS_INSERT_QUEUE =
            "INSERT INTO " + QUEUES_TABLE + " ("
                    + RDBMSConstants.QUEUE_NAME + ")"
                    + "  VALUES (?)";

    protected static final String PS_DELETE_QUEUE =
            "DELETE FROM " + QUEUES_TABLE
                    + " WHERE " + QUEUE_NAME + "=?";

    protected static final String PS_ALIAS_FOR_COUNT = "count";

    protected static final String PS_SELECT_QUEUE_MESSAGE_COUNT =
            "SELECT COUNT(" + QUEUE_ID + ") AS " + PS_ALIAS_FOR_COUNT
                    + " FROM " + METADATA_TABLE
                    + " WHERE " + QUEUE_ID + "=?"
                    + " AND " + DLC_QUEUE_ID + "=-1";

    /**
     * Prepared statement to retrieve message count within a message id range for a queue.
     */
    protected static final String PS_SELECT_RANGED_QUEUE_MESSAGE_COUNT =
            "SELECT COUNT(" + MESSAGE_ID + ") AS " + PS_ALIAS_FOR_COUNT
                    + " FROM " + METADATA_TABLE
                    + " WHERE " + QUEUE_ID + "=?"
                    + " AND " + MESSAGE_ID + " BETWEEN ? AND ?"
                    + " AND " + DLC_QUEUE_ID + "=-1";

    protected static final String ALIAS_FOR_QUEUES = "QUEUE_COUNT";

    /**
     * Prepared statement to select all the queue names with the number of messages remaining in the database
     * by joining the tables MB_QUEUE_MAPPING and MB_METADATA.
     */
    protected static final String PS_SELECT_ALL_QUEUE_MESSAGE_COUNT =
            "SELECT " + QUEUE_NAME + ", " + PS_ALIAS_FOR_COUNT
                    + " FROM " + QUEUES_TABLE + " LEFT OUTER JOIN "
                    + "(SELECT " + QUEUE_ID + ", COUNT(" + QUEUE_ID + ") AS " + PS_ALIAS_FOR_COUNT
                    + " FROM " + METADATA_TABLE
                    + " WHERE " + DLC_QUEUE_ID + "=-1"
                    + " GROUP BY " + QUEUE_ID + " ) " + ALIAS_FOR_QUEUES
                    + " ON " + QUEUES_TABLE + "." + QUEUE_ID + "=" + ALIAS_FOR_QUEUES + "." + QUEUE_ID;

    protected static final String PS_SELECT_QUEUE_MESSAGE_COUNT_FROM_DLC =
            "SELECT COUNT(" + MESSAGE_ID + ")"
                    + " AS " + PS_ALIAS_FOR_COUNT
                    + " FROM " + METADATA_TABLE
                    + " WHERE " + QUEUE_ID + "=?"
                    + " AND " + DLC_QUEUE_ID + "=?";

    protected static final String PS_SELECT_MESSAGE_COUNT_IN_DLC =
            "SELECT COUNT(" + MESSAGE_ID + ")"
                    + " AS " + PS_ALIAS_FOR_COUNT
                    + " FROM " + METADATA_TABLE
                    + " WHERE " + DLC_QUEUE_ID + "=?";

    protected static final String PS_SELECT_METADATA =
            "SELECT " + METADATA
                    + " FROM " + METADATA_TABLE
                    + " WHERE " + MESSAGE_ID + "=?";

    protected static final String PS_SELECT_METADATA_RANGE_FROM_QUEUE =
            "SELECT " + MESSAGE_ID + "," + METADATA
                    + " FROM " + METADATA_TABLE
                    + " WHERE " + QUEUE_ID + "=?"
                    + " AND " + DLC_QUEUE_ID + "=-1"
                    + " AND " + MESSAGE_ID + " BETWEEN ? AND ?"
                    + " ORDER BY " + MESSAGE_ID;

    protected static final String PS_SELECT_METADATA_RANGE_FROM_QUEUE_IN_DLC =
            "SELECT " + MESSAGE_ID + "," + METADATA
                    + " FROM " + METADATA_TABLE
                    + " WHERE " + QUEUE_ID + "=?"
                    + " AND " + DLC_QUEUE_ID + "=?"
                    + " AND " + MESSAGE_ID + " BETWEEN ? AND ?"
                    + " ORDER BY " + MESSAGE_ID;

    protected static final String PS_SELECT_METADATA_FROM_QUEUE =
            "SELECT " + MESSAGE_ID + "," + METADATA
                    + " FROM " + METADATA_TABLE
                    + " WHERE " + MESSAGE_ID + ">?"
                    + " AND " + QUEUE_ID + "=?"
                    + " AND " + DLC_QUEUE_ID + "=-1"
                    + " ORDER BY " + MESSAGE_ID;

    protected static final String PS_SELECT_MESSAGE_IDS_FROM_QUEUE =
            "SELECT " + MESSAGE_ID
                    + " FROM " + METADATA_TABLE
                    + " WHERE " + MESSAGE_ID + ">?"
                    + " AND " + QUEUE_ID + "=?"
                    + " AND " + DLC_QUEUE_ID + "=-1"
                    + " ORDER BY " + MESSAGE_ID;

    protected static final String PS_SELECT_METADATA_IN_DLC_FOR_QUEUE =
            "SELECT " + MESSAGE_ID + "," + METADATA
                    + " FROM " + METADATA_TABLE
                    + " WHERE " + MESSAGE_ID + ">?"
                    + " AND " + QUEUE_ID + "=?"
                    + " AND " + DLC_QUEUE_ID + "=?"
                    + " ORDER BY " + MESSAGE_ID;

    protected static final String PS_SELECT_METADATA_IN_DLC =
            "SELECT " + MESSAGE_ID + "," + METADATA
                    + " FROM " + METADATA_TABLE
                    + " WHERE " + MESSAGE_ID + ">?"
                    + " AND " + DLC_QUEUE_ID + "=?"
                    + " ORDER BY " + MESSAGE_ID;

    protected static final String PS_SELECT_MESSAGE_IDS_FROM_METADATA_FOR_QUEUE =
            "SELECT " + MESSAGE_ID
                    + " FROM " + METADATA_TABLE
                    + " WHERE " + QUEUE_ID + "=?"
                    + " ORDER BY " + MESSAGE_ID;

    protected static final String PS_DELETE_EXPIRY_DATA =
            "DELETE  FROM " + EXPIRATION_TABLE
                    + " WHERE " + MESSAGE_ID + "=?";

    protected static final String PS_DELETE_METADATA_FROM_QUEUE =
            "DELETE  FROM " + METADATA_TABLE
                    + " WHERE " + QUEUE_ID + "=?"
                    + " AND " + MESSAGE_ID + "=?";

    protected static final String PS_DELETE_METADATA_IN_DLC =
            "DELETE  FROM " + METADATA_TABLE
                    + " WHERE " + MESSAGE_ID + "=?"
                    + " AND " + DLC_QUEUE_ID + "!=-1";

    protected static final String PS_DELETE_METADATA =
            "DELETE  FROM " + METADATA_TABLE
                    + " WHERE " + MESSAGE_ID + "=?"
                    + " AND " + DLC_QUEUE_ID + "=-1";

    protected static final String PS_CLEAR_QUEUE_FROM_METADATA =
            "DELETE  FROM " + METADATA_TABLE
                    + " WHERE " + QUEUE_ID + "=?";

    protected static final String PS_CLEAR_DLC_QUEUE =
            "DELETE  FROM " + METADATA_TABLE
                    + " WHERE " + DLC_QUEUE_ID + "=?";

    protected static final String PS_SELECT_EXPIRED_MESSAGES =
            "SELECT " + MESSAGE_ID + "," + DESTINATION_QUEUE
                    + " FROM " + EXPIRATION_TABLE
                    + " WHERE " + EXPIRATION_TIME + "<" + System.currentTimeMillis();

    protected static final String PS_SELECT_QUEUE_ID =
            "SELECT " + QUEUE_ID
                    + " FROM " + QUEUES_TABLE
                    + " WHERE " + QUEUE_NAME + "=?";

    // prepared statements for Andes Context Store
    protected static final String PS_INSERT_DURABLE_SUBSCRIPTION =
            "INSERT INTO " + DURABLE_SUB_TABLE + " ("
                    + DESTINATION_IDENTIFIER + ", "
                    + DURABLE_SUB_ID + ", "
                    + DURABLE_SUB_DATA + ")"
                    + "  VALUES (?,?,?)";

    protected static final String PS_UPDATE_DURABLE_SUBSCRIPTION =
            "UPDATE " + DURABLE_SUB_TABLE
                    + " SET " + DURABLE_SUB_DATA + "=?"
                    + " WHERE " + DESTINATION_IDENTIFIER + "=?"
                    + " AND " + DURABLE_SUB_ID + "=?";

    protected static final String PS_UPDATE_DURABLE_SUBSCRIPTION_BY_ID =
            "UPDATE " + DURABLE_SUB_TABLE
                    + " SET " + DURABLE_SUB_DATA + "=?"
                    + " WHERE " + DURABLE_SUB_ID + "=?";

    protected static final String PS_SELECT_ALL_DURABLE_SUBSCRIPTIONS =
            "SELECT " + DURABLE_SUB_DATA
                    + " FROM " + DURABLE_SUB_TABLE;

    protected static final String PS_SELECT_ALL_DURABLE_SUBSCRIPTIONS_WITH_SUB_ID =
            "SELECT " + DURABLE_SUB_ID + "," + DURABLE_SUB_DATA
                    + " FROM " + DURABLE_SUB_TABLE;

    protected static final String PS_IS_SUBSCRIPTION_EXIST =
            "SELECT 1 FROM " + DURABLE_SUB_TABLE
                    + " WHERE " + DURABLE_SUB_ID + "=?";

    protected static final String PS_DELETE_DURABLE_SUBSCRIPTION =
            "DELETE FROM " + DURABLE_SUB_TABLE
                    + " WHERE " + DESTINATION_IDENTIFIER + "=?"
                    + " AND " + DURABLE_SUB_ID + "=?";

    protected static final String PS_INSERT_NODE_INFO =
            "INSERT INTO " + NODE_INFO_TABLE + " ( "
                    + NODE_ID + ","
                    + NODE_INFO + ")"
                    + " VALUES (?,?)";

    protected static final String PS_SELECT_ALL_NODE_INFO =
            "SELECT " + NODE_ID + "," + NODE_INFO
                    + " FROM " + NODE_INFO_TABLE;

    protected static final String PS_DELETE_NODE_INFO =
            "DELETE FROM " + NODE_INFO_TABLE
                    + " WHERE " + NODE_ID + "=?";

    protected static final String PS_STORE_EXCHANGE_INFO =
            "INSERT INTO " + EXCHANGES_TABLE + " ("
                    + EXCHANGE_NAME + ","
                    + EXCHANGE_DATA + ")"
                    + " VALUES (?,?)";

    protected static final String PS_SELECT_ALL_EXCHANGE_INFO =
            "SELECT " + EXCHANGE_DATA
                    + " FROM " + EXCHANGES_TABLE;

    protected static final String PS_SELECT_EXCHANGE =
            "SELECT " + EXCHANGE_DATA
                    + " FROM " + EXCHANGES_TABLE
                    + " WHERE " + EXCHANGE_NAME + "=?";

    protected static final String PS_DELETE_EXCHANGE =
            "DELETE FROM " + EXCHANGES_TABLE
                    + " WHERE " + EXCHANGE_NAME + "=?";

    protected static final String PS_INSERT_QUEUE_INFO =
            "INSERT INTO " + QUEUE_INFO_TABLE + " ("
                    + QUEUE_NAME + ","
                    + QUEUE_DATA + ")"
                    + " VALUES (?,?)";

    protected static final String PS_SELECT_ALL_QUEUE_INFO =
            "SELECT " + QUEUE_DATA
                    + " FROM " + QUEUE_INFO_TABLE;

    protected static final String PS_DELETE_QUEUE_INFO =
            "DELETE FROM " + QUEUE_INFO_TABLE
                    + " WHERE " + QUEUE_NAME + "=?";

    protected static final String PS_INSERT_BINDING =
            "INSERT INTO " + BINDINGS_TABLE + " ( "
                    + BINDING_EXCHANGE_NAME + ","
                    + BINDING_QUEUE_NAME + ","
                    + BINDING_INFO + " )"
                    + " VALUES (?,?,?)";

    protected static final String PS_SELECT_BINDINGS_FOR_EXCHANGE =
            "SELECT " + BINDING_INFO
                    + " FROM " + BINDINGS_TABLE
                    + " WHERE " + BINDING_EXCHANGE_NAME + "=?";

    protected static final String PS_DELETE_BINDING =
            "DELETE FROM " + BINDINGS_TABLE
                    + " WHERE " + BINDING_EXCHANGE_NAME + "=?"
                    + " AND " + BINDING_QUEUE_NAME + "=?";

    protected static final String PS_UPDATE_METADATA_QUEUE =
            "UPDATE " + METADATA_TABLE
                    + " SET " + QUEUE_ID + " = ?"
                    + " WHERE " + MESSAGE_ID + " = ?"
                    + " AND " + QUEUE_ID + " = ?";

    protected static final String PS_UPDATE_METADATA =
            "UPDATE " + METADATA_TABLE
                    + " SET " + QUEUE_ID + " = ?," + METADATA + " = ?"
                    + " WHERE " + MESSAGE_ID + " = ?"
                    + " AND " + QUEUE_ID + " = ?";
    /**
     * Prepared Statement to insert a new queue counter.
     */
    protected static final String PS_INSERT_QUEUE_COUNTER =
            "INSERT INTO " + QUEUE_COUNTER_TABLE + " ("
                    + QUEUE_NAME + ","
                    + MESSAGE_COUNT + " )"
                    + " VALUES ( ?,? )";

    /**
     * Prepared Statement to select count for a queue prepared statement
     */
    protected static final String PS_SELECT_QUEUE_COUNT =
            "SELECT " + MESSAGE_COUNT
                    + " FROM " + QUEUE_COUNTER_TABLE
                    + " WHERE " + QUEUE_NAME + "=?";

    /**
     * Prepared Statement to delete queue counter with a given queue name
     */
    protected static final String PS_DELETE_QUEUE_COUNTER =
            "DELETE FROM " + QUEUE_COUNTER_TABLE
                    + " WHERE " + QUEUE_NAME + "=?";

    /**
     * Increments the queue count by a given value in a atomic db update
     */
    protected static final String PS_INCREMENT_QUEUE_COUNT =
            "UPDATE " + QUEUE_COUNTER_TABLE
                    + " SET " + MESSAGE_COUNT + "="
                    + MESSAGE_COUNT + "+?"
                    + " WHERE " + QUEUE_NAME + "=?";

    /**
     * Decrement the queue count by a given value in a atomic db update
     */
    protected static final String PS_DECREMENT_QUEUE_COUNT =
            "UPDATE " + QUEUE_COUNTER_TABLE
                    + " SET " + MESSAGE_COUNT + "=" + MESSAGE_COUNT + "-?"
                    + " WHERE " + QUEUE_NAME + "=?";

    protected static final String PS_RESET_QUEUE_COUNT =
            "UPDATE " + QUEUE_COUNTER_TABLE
                    + " SET " + MESSAGE_COUNT + "= 0"
                    + " WHERE " + QUEUE_NAME + "=?";

    /**
     * Prepared Statement to test inserts are working for message store
     */
    protected static final String PS_TEST_MSG_STORE_INSERT =
            "INSERT INTO " + MSG_STORE_STATUS_TABLE + " ("
                    + NODE_ID + ","
                    + TIME_STAMP + " )"
                    + " VALUES ( ?,? )";

    /**
     * Prepared statements to clear slot storage tables
     */

    protected static final String PS_CLEAR_SLOT_TABLE =
            "DELETE FROM " + SLOT_TABLE;

    protected static final String PS_CLEAR_SLOT_MESSAGE_ID_TABLE =
            "DELETE FROM " + SLOT_MESSAGE_ID_TABLE;

    protected static final String PS_CLEAR_QUEUE_TO_LAST_ASSIGNED_ID =
            "DELETE FROM " + QUEUE_TO_LAST_ASSIGNED_ID;

    protected static final String PS_CLEAR_NODE_TO_LAST_PUBLISHED_ID =
            "DELETE FROM " + NODE_TO_LAST_PUBLISHED_ID;

    /**
     * Prepared statement to create a new slot in database
     */
    protected static final String PS_INSERT_SLOT =
            "INSERT INTO " + SLOT_TABLE + " ("
                    + START_MESSAGE_ID + ","
                    + END_MESSAGE_ID + ","
                    + STORAGE_QUEUE_NAME + ","
                    + SLOT_STATE + ","
                    + ASSIGNED_NODE_ID + ")"
                    + " VALUES (?,?,?," + SlotState.ASSIGNED.getCode() + ",?)";

    /**
     * Prepared statement to delete a slot from database
     */
    protected static final String PS_DELETE_NON_OVERLAPPING_SLOT =
            "DELETE FROM " + SLOT_TABLE
                    + " WHERE " + START_MESSAGE_ID + "=?"
                    + " AND " + END_MESSAGE_ID + "=?"
                    + " AND " + SLOT_STATE + "!=" + SlotState.OVERLAPPED.getCode();

    /**
     * Prepared statement to delete a slot by queue name
     */
    protected static final String PS_DELETE_SLOTS_BY_QUEUE_NAME =
            "DELETE FROM " + SLOT_TABLE
                    + " WHERE " + STORAGE_QUEUE_NAME + "=?";

    /**
     * Prepared statement to assign a slot to node
     */
    protected static final String PS_INSERT_SLOT_ASSIGNMENT =
            "UPDATE " + SLOT_TABLE
                    + " SET " + ASSIGNED_NODE_ID + "=?, "
                    + ASSIGNED_QUEUE_NAME + "=?,"
                    + SLOT_STATE + "=" + SlotState.ASSIGNED.getCode()
                    + " WHERE " + START_MESSAGE_ID + "=?"
                    + " AND " + END_MESSAGE_ID + "=?";

    /**
     * Prepared statement to un-assign a slot from node
     */
    protected static final String PS_DELETE_SLOT_ASSIGNMENT =
            "UPDATE " + SLOT_TABLE
                    + " SET " + ASSIGNED_NODE_ID + "=NULL, "
                    + ASSIGNED_QUEUE_NAME + "=NULL, "
                    + SLOT_STATE + "=" + SlotState.RETURNED.getCode()
                    + " WHERE " + START_MESSAGE_ID + "=?"
                    + " AND " + END_MESSAGE_ID + "=?";

    /**
     * Prepared statement to un-assign slots assigned to a given queue
     */
    protected static final String PS_DELETE_SLOT_ASSIGNMENT_BY_QUEUE_NAME =
            "UPDATE " + SLOT_TABLE
                    + " SET " + ASSIGNED_NODE_ID + " = NULL, "
                    + ASSIGNED_QUEUE_NAME + " = NULL, "
                    + SLOT_STATE + "=" + SlotState.RETURNED.getCode()
                    + " WHERE " + ASSIGNED_NODE_ID + "= ?"
                    + " AND " + ASSIGNED_QUEUE_NAME + "= ?";

    /**
     * Prepared statement to get slots assigned to a give node
     */
    protected static final String PS_GET_ASSIGNED_SLOTS_BY_NODE_ID =
            "SELECT " + START_MESSAGE_ID + "," + END_MESSAGE_ID + "," + STORAGE_QUEUE_NAME
                    + " FROM " + SLOT_TABLE
                    + " WHERE " + ASSIGNED_NODE_ID + "=?"
                    + " AND " + SLOT_STATE + "=" + SlotState.ASSIGNED.getCode()
                    + " ORDER BY " + SLOT_ID;

    /**
     * Prepared statement to get a slot
     */
    protected static final String PS_GET_SLOT =
            "SELECT " + SLOT_STATE + "," + STORAGE_QUEUE_NAME
                    + " FROM " + SLOT_TABLE
                    + " WHERE " + START_MESSAGE_ID + "=?"
                    + " AND " + END_MESSAGE_ID + "=?";

    /**
     * Prepared statements for setting slot states
     */
    protected static final String PS_SET_SLOT_STATE =
            "UPDATE " + SLOT_TABLE
                    + " SET " + SLOT_STATE + " = ?"
                    + " WHERE " + START_MESSAGE_ID + " = ?"
                    + " AND " + END_MESSAGE_ID + " = ?";

    /**
     * Prepared statement for selecting unassigned slot
     */

    protected static final String PS_SELECT_ALL_SLOTS_BY_QUEUE_NAME =
            "SELECT " + START_MESSAGE_ID + "," + END_MESSAGE_ID + "," + STORAGE_QUEUE_NAME + "," + SLOT_STATE
                    + " FROM " + SLOT_TABLE
                    + " WHERE " + STORAGE_QUEUE_NAME + " =?"
                    + " ORDER BY " + SLOT_ID;

    protected static final String PS_SELECT_UNASSIGNED_SLOT =
            "SELECT " + START_MESSAGE_ID + "," + END_MESSAGE_ID + "," + STORAGE_QUEUE_NAME
                    + " FROM " + SLOT_TABLE
                    + " WHERE " + STORAGE_QUEUE_NAME + " =?"
                    + " AND " + SLOT_STATE + " = " + SlotState.RETURNED.getCode()
                    + " ORDER BY " + SLOT_ID;

    /**
     * Prepared statement for selecting oldest overlapped slot
     */
    protected static final String PS_SELECT_OVERLAPPED_SLOT =
            "SELECT " + START_MESSAGE_ID + "," + END_MESSAGE_ID + "," + STORAGE_QUEUE_NAME
                    + " FROM " + SLOT_TABLE
                    + " WHERE " + STORAGE_QUEUE_NAME + "=?"
                    + " AND " + ASSIGNED_NODE_ID + "=?"
                    + " AND " + SLOT_STATE + "=" + SlotState.OVERLAPPED.getCode()
                    + " ORDER BY " + SLOT_ID;

    /**
     * Prepared statement for getting last assigned id for queue
     */
    protected static final String PS_SELECT_QUEUE_TO_LAST_ASSIGNED_ID =
            "SELECT " + MESSAGE_ID
                    + " FROM " + QUEUE_TO_LAST_ASSIGNED_ID
                    + " WHERE " + QUEUE_NAME + "=?";

    /**
     * Prepared statement to insert last assigned id of queue
     */
    protected static final String PS_INSERT_QUEUE_TO_LAST_ASSIGNED_ID =
            "INSERT INTO " + QUEUE_TO_LAST_ASSIGNED_ID + "("
                    + QUEUE_NAME + ","
                    + MESSAGE_ID + ")"
                    + " VALUES (?,?)";

    /**
     * Prepared statement to update last assigned id of queue
     */
    protected static final String PS_UPDATE_QUEUE_TO_LAST_ASSIGNED_ID =
            "UPDATE " + QUEUE_TO_LAST_ASSIGNED_ID
                    + " SET " + MESSAGE_ID + "=?"
                    + " WHERE " + QUEUE_NAME + "=?";

    /**
     * Prepared statement to get last published id of node
     */
    protected static final String PS_SELECT_NODE_TO_LAST_PUBLISHED_ID =
            "SELECT " + MESSAGE_ID
                    + " FROM " + NODE_TO_LAST_PUBLISHED_ID
                    + " WHERE " + NODE_ID + "=?";

    /**
     * Prepared statement to insert last published id of a node
     */
    protected static final String PS_INSERT_NODE_TO_LAST_PUBLISHED_ID =
            "INSERT INTO " + NODE_TO_LAST_PUBLISHED_ID + "("
                    + NODE_ID + ","
                    + MESSAGE_ID + ")"
                    + " VALUES (?,?)";

    protected static final String PS_DELETE_PUBLISHER_ID =
            "DELETE FROM " + NODE_TO_LAST_PUBLISHED_ID
                    + " WHERE " + NODE_ID + "=?";

    /**
     * Prepared statement to update last published id of a node
     */
    protected static final String PS_UPDATE_NODE_TO_LAST_PUBLISHED_ID =
            "UPDATE " + NODE_TO_LAST_PUBLISHED_ID
                    + " SET " + MESSAGE_ID + "=?"
                    + " WHERE " + NODE_ID + "=?";

    /**
     * Prepared statement to get list of message published nodes
     */
    protected static final String PS_SELECT_MESSAGE_PUBLISHED_NODES =
            "SELECT " + NODE_ID
                    + " FROM " + NODE_TO_LAST_PUBLISHED_ID;

    /**
     * Prepared statement to insert slot message ids
     */
    protected static final String PS_INSERT_SLOT_MESSAGE_ID =
            "INSERT INTO " + SLOT_MESSAGE_ID_TABLE
                    + "(" + QUEUE_NAME + ","
                    + MESSAGE_ID + ")"
                    + " VALUES (?,?)";

    /**
     * Prepared statement to get slot message ids
     */
    protected static final String PS_GET_MESSAGE_IDS =
            "SELECT " + MESSAGE_ID
                    + " FROM " + SLOT_MESSAGE_ID_TABLE
                    + " WHERE " + QUEUE_NAME + "=?"
                    + " ORDER BY " + MESSAGE_ID;

    /**
     * Prepared statement to delete slot message ids
     */
    protected static final String PS_DELETE_MESSAGE_ID =
            "DELETE FROM " + SLOT_MESSAGE_ID_TABLE
                    + " WHERE " + MESSAGE_ID + "=?";

    /**
     * Prepared statement to delete message ids by queue name
     */
    protected static final String PS_DELETE_MESSAGE_IDS_BY_QUEUE_NAME =
            "DELETE FROM " + SLOT_MESSAGE_ID_TABLE
                    + " WHERE " + QUEUE_NAME + "=?";

    /**
     * Prepared statement to get all queues
     */
    protected static final String PS_GET_ALL_QUEUES =
            "SELECT DISTINCT " + STORAGE_QUEUE_NAME
                    + " FROM " + SLOT_TABLE;

    /**
     * Prepared Statement to test deletes are working for message store
     */
    protected static final String PS_TEST_MSG_STORE_SELECT =
            "SELECT " + NODE_ID + ", " + TIME_STAMP
                    + " FROM " + MSG_STORE_STATUS_TABLE
                    + " WHERE " + NODE_ID + "=?"
                    + " AND " + TIME_STAMP + "=?";

    /**
     * Prepared Statement to test deletes are working for message store
     */
    protected static final String PS_TEST_MSG_STORE_DELETE =
            "DELETE FROM " + MSG_STORE_STATUS_TABLE
                    + " WHERE " + NODE_ID + " = ?"
                    + " AND " + TIME_STAMP + " = ?";


    /**
     * Prepared statement to update retained metadata
     */
    protected static final String PS_UPDATE_RETAINED_METADATA =
            "UPDATE " + RETAINED_METADATA_TABLE
                    + " SET " + MESSAGE_ID + " = ?, " + METADATA + " = ?"
                    + " WHERE " + TOPIC_ID + " = ?";

    /**
     * Prepared statement to delete messages from retained content
     */
    protected static final String PS_DELETE_RETAIN_MESSAGE_PARTS =
            "DELETE FROM " + RETAINED_CONTENT_TABLE
                    + " WHERE " + MESSAGE_ID + "=?";

    /**
     * Prepared statement to delete messages from retained metadata
     */
    protected static final String PS_DELETE_RETAIN_MESSAGE_METADATA =
            "DELETE FROM " + RETAINED_METADATA_TABLE
                    + " WHERE " + MESSAGE_ID + "=?";

    /**
     * Prepared statement to insert messages to retained content
     */
    protected static final String PS_INSERT_RETAIN_MESSAGE_PART =
            "INSERT INTO " + RETAINED_CONTENT_TABLE + "("
                    + MESSAGE_ID + ","
                    + MSG_OFFSET + ","
                    + MESSAGE_CONTENT + ")"
                    + " VALUES (?, ?, ?)";

    /**
     * Prepared statement to select all retained topics from retained metadata
     */
    protected static final String PS_SELECT_ALL_RETAINED_TOPICS =
            "SELECT " + TOPIC_NAME
                    + " FROM " + RETAINED_METADATA_TABLE;

    /**
     * Prepared statement to select retained message metadata for a given topic id
     */
    protected static final String PS_SELECT_RETAINED_METADATA =
            "SELECT " + MESSAGE_ID + ", " + METADATA
                    + " FROM " + RETAINED_METADATA_TABLE
                    + " WHERE " + TOPIC_ID + "=?";

    /**
     * Prepared statement to select retained message content for given message id
     */
    protected static final String PS_RETRIEVE_RETAIN_MESSAGE_PART =
            "SELECT " + MSG_OFFSET + ", " + MESSAGE_CONTENT
                    + " FROM " + RETAINED_CONTENT_TABLE
                    + " WHERE " + MESSAGE_ID + "=?";

    /**
     * Prepared statement to select retained metadata for given topic name
     */
    protected static final String PS_SELECT_RETAINED_MESSAGE_ID =
            "SELECT " + TOPIC_ID + ", " + MESSAGE_ID
                    + " FROM " + RETAINED_METADATA_TABLE
                    + " WHERE " + TOPIC_NAME + "=?";

    /**
     * Prepared statement to insert retained metadata
     */
    protected static final String PS_INSERT_RETAINED_METADATA =
            "INSERT INTO " + RETAINED_METADATA_TABLE + " ("
                    + TOPIC_ID + ","
                    + TOPIC_NAME + ","
                    + MESSAGE_ID + ","
                    + METADATA + ")"
                    + " VALUES ( ?,?,?,? )";

    /**
     * Prepared statement to move messages to DLC
     */
    protected static final String PS_MOVE_METADATA_TO_DLC =
            "UPDATE " + METADATA_TABLE
                    + " SET " + DLC_QUEUE_ID + "=?"
                    + " WHERE " + MESSAGE_ID + "=?";

    // Message Store related jdbc tasks executed
    protected static final String TASK_STORING_MESSAGE_PARTS = "storing message parts.";
    protected static final String TASK_DELETING_MESSAGE_PARTS = "deleting message parts.";
    protected static final String TASK_RETRIEVING_MESSAGE_PARTS = "retrieving message parts.";
    protected static final String TASK_RETRIEVING_CONTENT_FOR_MESSAGES = "retrieving content for multiple messages";
    protected static final String TASK_ADDING_METADATA_LIST = "adding metadata list.";
    protected static final String TASK_ADDING_METADATA = "adding metadata.";
    protected static final String TASK_ADDING_MESSAGE = "adding message.";
    protected static final String TASK_ADDING_MESSAGES = "adding messages";
    protected static final String TASK_DELETING_MESSAGES = "deleting messages";
    protected static final String TASK_MOVING_METADATA_TO_DLC = "moving message metadata to dlc.";

    protected static final String TASK_ADDING_METADATA_TO_QUEUE = "adding metadata to destination. ";
    protected static final String TASK_ADDING_METADATA_LIST_TO_QUEUE = "adding metadata list to destination. ";
    protected static final String TASK_RETRIEVING_ALL_QUEUE_MSG_COUNT = "retrieving message counts for all queues. ";
    protected static final String TASK_RETRIEVING_RANGED_QUEUE_MSG_COUNT = "retrieving ranged message count for queue" +
            ". ";
    protected static final String TASK_RETRIEVING_QUEUE_MSG_COUNT = "retrieving message count for queue. ";
    protected static final String TASK_RETRIEVING_QUEUE_MSG_COUNT_IN_DLC = "retrieving message count in DLC for"
            + " queue. ";
    protected static final String TASK_RETRIEVING_METADATA = "retrieving metadata for message id. ";
    protected static final String TASK_RETRIEVING_METADATA_RANGE_FROM_QUEUE = "retrieving metadata within a range "
            + "from queue. ";
    protected static final String TASK_RETRIEVING_METADATA_RANGE_IN_DLC_FROM_QUEUE = "retrieving metadata in dlc "
            + "within a range from queue. ";
    protected static final String TASK_RETRIEVING_METADATA_RANGE_IN_DLC = "retrieving metadata in dlc within a range. ";
    protected static final String TASK_RETRIEVING_NEXT_N_METADATA_FROM_QUEUE = "retrieving metadata list from queue. ";
    protected static final String TASK_RETRIEVING_NEXT_N_IDS_FROM_QUEUE = "retrieving message id list from queue. ";
    protected static final String TASK_RETRIEVING_NEXT_N_METADATA_IN_DLC_FOR_QUEUE = "retrieving metadata list in DLC "
            + "for queue. ";
    protected static final String TASK_RETRIEVING_NEXT_N_METADATA_FROM_DLC = "retrieving metadata list from DLC ";
    protected static final String TASK_RETRIEVING_NEXT_N_MESSAGE_IDS_OF_QUEUE = "retrieving message ID list from "
            + "queue. ";
    protected static final String TASK_DELETING_FROM_EXPIRY_TABLE = "deleting from expiry table.";
    protected static final String TASK_DELETING_METADATA_FROM_QUEUE = "deleting metadata from queue. ";
    protected static final String TASK_DELETING_MESSAGE_FROM_DLC = "deleting message from dlc. ";
    protected static final String TASK_CLEARING_DLC_QUEUE = "clearing dlc queue. ";
    protected static final String TASK_RESETTING_MESSAGE_COUNTER = "Resetting message counter for queue";
    protected static final String TASK_RETRIEVING_EXPIRED_MESSAGES = "retrieving expired messages.";
    protected static final String TASK_RETRIEVING_QUEUE_ID = "retrieving queue id for queue. ";
    protected static final String TASK_CREATING_QUEUE = "creating queue. ";

    // Message Store related retained message jdbc tasks executed
    protected static final String TASK_STORING_RETAINED_MESSAGE = "storing retained messages.";
    protected static final String TASK_RETRIEVING_RETAINED_MESSAGE_PARTS = "retrieving retained message parts.";
    protected static final String TASK_RETRIEVING_RETAINED_TOPICS = "retrieving all retained topics";
    protected static final String TASK_RETRIEVING_RETAINED_TOPIC_ID = "retrieving retained  message id and topic id "
            + "for given destination.";

    // Andes Context Store related jdbc tasks executed
    protected static final String TASK_STORING_DURABLE_SUBSCRIPTION = "storing durable subscription";
    protected static final String TASK_UPDATING_DURABLE_SUBSCRIPTION = "updating durable subscription";
    protected static final String TASK_UPDATING_DURABLE_SUBSCRIPTIONS = "updating durable subscriptions";
    protected static final String TASK_RETRIEVING_ALL_DURABLE_SUBSCRIPTIONS = "retrieving all durable subscriptions. ";

    protected static final String TASK_CHECK_SUBSCRIPTION_EXISTENCE = "checking subscription existence";

    protected static final String TASK_REMOVING_DURABLE_SUBSCRIPTION = "removing durable subscription. ";
    protected static final String TASK_STORING_NODE_INFORMATION = "storing node information";
    protected static final String TASK_RETRIEVING_ALL_NODE_DETAILS = "retrieving all node information. ";
    protected static final String TASK_REMOVING_NODE_INFORMATION = "removing node information";
    protected static final String TASK_STORING_EXCHANGE_INFORMATION = "storing exchange information";
    protected static final String TASK_RETRIEVING_ALL_EXCHANGE_INFO = "retrieving all exchange information. ";
    protected static final String TASK_IS_EXCHANGE_EXIST = "checking whether an exchange exist. ";
    protected static final String TASK_DELETING_EXCHANGE = "deleting an exchange ";
    protected static final String TASK_STORING_QUEUE_INFO = "storing queue information ";
    protected static final String TASK_RETRIEVING_ALL_QUEUE_INFO = "retrieving all queue information. ";
    protected static final String TASK_DELETING_QUEUE_INFO = "deleting queue information. ";
    protected static final String TASK_DELETE_QUEUE_MAPPING = "deleting queue mapping";
    protected static final String TASK_STORING_BINDING = "storing binding information. ";
    protected static final String TASK_RETRIEVING_BINDING_INFO = "retrieving binding information.";
    protected static final String TASK_DELETING_BINDING = "deleting binding information. ";
    protected static final String TASK_UPDATING_META_DATA_QUEUE = "updating message meta data queue.";
    protected static final String TASK_UPDATING_META_DATA = "updating message meta data.";
    protected static final String TASK_ADDING_QUEUE_COUNTER = "adding counter for queue";
    protected static final String TASK_CHECK_QUEUE_COUNTER_EXIST = "checking queue counter exist";
    protected static final String TASK_RETRIEVING_QUEUE_COUNT = "retrieving queue count";
    protected static final String TASK_DELETING_QUEUE_COUNTER = "deleting queue counter";
    protected static final String TASK_INCREMENTING_QUEUE_COUNT = "incrementing queue count";
    protected static final String TASK_DECREMENTING_QUEUE_COUNT = "decrementing queue count";

    protected static final String TASK_CREATE_SLOT = "creating slot";
    protected static final String TASK_DELETE_SLOT = "deleting slot";
    protected static final String TASK_DELETE_SLOT_BY_QUEUE_NAME = "deleting slot by queue name";
    protected static final String TASK_DELETE_MESSAGE_ID_BY_QUEUE_NAME = "deleting message id by queue name";
    protected static final String TASK_CREATE_SLOT_ASSIGNMENT = "creating slot assignment";
    protected static final String TASK_DELETE_SLOT_ASSIGNMENT = "deleting slot assignment";
    protected static final String TASK_SELECT_UNASSIGNED_SLOTS = "selecting unassigned slots";
    protected static final String TASK_GET_QUEUE_TO_LAST_ASSIGNED_ID = "getting queue to last assigned id";
    protected static final String TASK_SET_QUEUE_TO_LAST_ASSIGNED_ID = "setting queue to last assigned id";
    protected static final String TASK_GET_NODE_TO_LAST_PUBLISHED_ID = "getting node to last published id";
    protected static final String TASK_SET_NODE_TO_LAST_PUBLISHED_ID = "setting node to last published id";
    protected static final String TASK_DELETE_PUBLISHER_ID = "deleting publisher id";
    protected static final String TASK_GET_MESSAGE_PUBLISHED_NODES = "getting message published nodes";
    protected static final String TASK_SET_SLOT_STATE = "setting slot state";
    protected static final String TASK_ADD_MESSAGE_ID = "adding message id";
    protected static final String TASK_DELETE_MESSAGE_ID = "deleting message ids";
    protected static final String TASK_GET_MESSAGE_IDS = "getting message ids";
    protected static final String TASK_GET_ASSIGNED_SLOTS_BY_NODE_ID = "getting assigned slots by node id";
    protected static final String TASK_GET_ALL_SLOTS_BY_QUEUE_NAME = "getting all slots by queue name";
    protected static final String TASK_GET_OVERLAPPED_SLOT = "getting overlapped slot";
    protected static final String TASK_GET_ALL_QUEUES = "getting all queues";
    protected static final String TASK_CLEAR_SLOT_TABLES = "clearing slot tables";

    /**
     * Messages related to checking message store is operational.
     */
    protected static final String TASK_TEST_MESSAGE_STORE_OPERATIONAL_READ = "testing data can be read from message"
            + " store.";
    protected static final String TASK_TEST_MESSAGE_STORE_OPERATIONAL_INSERT = "testing data can be inserted to message"
            + " store.";
    protected static final String TASK_TEST_MESSAGE_STORE_OPERATIONAL_DELETE = "testing data can be deleted from"
            + " message store.";

    /**
     * Only public static constants are in this class. No need to instantiate.
     */
    private RDBMSConstants() {
    }
}
