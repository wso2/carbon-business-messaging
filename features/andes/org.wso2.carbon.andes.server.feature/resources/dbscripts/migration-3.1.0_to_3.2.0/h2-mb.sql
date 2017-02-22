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
 
-- WSO2 Message Broker v 3.1.0 to v 3.2.0 migration script for h2 --

UPDATE MB_EXCHANGE SET EXCHANGE_DATA = REPLACE(EXCHANGE_DATA, 'exchangeName=', 'messageRouterName=');

DELETE FROM MB_DURABLE_SUBSCRIPTION;

DROP TABLE MB_EXPIRATION_DATA;

CREATE TABLE IF NOT EXISTS MB_EXPIRATION_DATA (
                MESSAGE_ID BIGINT UNIQUE,
                EXPIRATION_TIME BIGINT,
                DLC_QUEUE_ID INT NOT NULL,
                MESSAGE_DESTINATION VARCHAR NOT NULL,
                FOREIGN KEY (MESSAGE_ID) REFERENCES MB_METADATA (MESSAGE_ID)
                ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS MB_DTX_XID (
                INTERNAL_XID BIGINT NOT NULL,
                NODE_ID VARCHAR(512) NOT NULL,
                FORMAT_CODE BIGINT NOT NULL,
                GLOBAL_ID BINARY NOT NULL,
                BRANCH_ID BINARY NOT NULL,
                PRIMARY KEY (INTERNAL_XID, NODE_ID)
);

CREATE TABLE IF NOT EXISTS MB_DTX_ENQUEUE_RECORD (
                INTERNAL_XID BIGINT NOT NULL,
                MESSAGE_ID BIGINT NOT NULL,
                MESSAGE_METADATA BINARY NOT NULL,
                PRIMARY KEY (MESSAGE_ID),
                FOREIGN KEY (INTERNAL_XID) REFERENCES MB_DTX_XID (INTERNAL_XID)
                ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS MB_DTX_DEQUEUE_RECORD (
                INTERNAL_XID BIGINT NOT NULL,
                MESSAGE_ID BIGINT NOT NULL,
                QUEUE_NAME VARCHAR NOT NULL,
                MESSAGE_METADATA BINARY NOT NULL,
                PRIMARY KEY (MESSAGE_ID),
                FOREIGN KEY (INTERNAL_XID) REFERENCES MB_DTX_XID (INTERNAL_XID)
                ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS MB_DTX_ENQUEUE_CONTENT (
                MESSAGE_ID BIGINT NOT NULL,
                INTERNAL_XID BIGINT NOT NULL,
                CONTENT_OFFSET INT NOT NULL,
                MESSAGE_CONTENT BINARY NOT NULL,
                PRIMARY KEY (MESSAGE_ID, CONTENT_OFFSET),
                FOREIGN KEY (MESSAGE_ID) REFERENCES MB_DTX_ENQUEUE_RECORD (MESSAGE_ID)
                ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS MB_DTX_DEQUEUE_CONTENT (
                INTERNAL_XID BIGINT NOT NULL,
                MESSAGE_ID BIGINT,
                CONTENT_OFFSET INT,
                MESSAGE_CONTENT BINARY NOT NULL,
                PRIMARY KEY (MESSAGE_ID, CONTENT_OFFSET),
                FOREIGN KEY (MESSAGE_ID) REFERENCES MB_DTX_DEQUEUE_RECORD (MESSAGE_ID)
                ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS MB_CLUSTER_EVENT (
                EVENT_ID BIGINT NOT NULL AUTO_INCREMENT,
                ORIGINATED_NODE_ID VARCHAR(512) NOT NULL,
                DESTINED_NODE_ID VARCHAR(512) NOT NULL,
                EVENT_ARTIFACT VARCHAR(25) NOT NULL,
                EVENT_TYPE VARCHAR(25) NOT NULL,
                EVENT_DETAILS VARCHAR(1024) NOT NULL,
                EVENT_DESCRIPTION VARCHAR(1024),
                PRIMARY KEY (EVENT_ID)
);

CREATE TABLE IF NOT EXISTS MB_COORDINATOR_HEARTBEAT (
                ANCHOR INT NOT NULL,
                NODE_ID VARCHAR(512) NOT NULL,
                LAST_HEARTBEAT BIGINT NOT NULL,
                THRIFT_HOST VARCHAR(512) NOT NULL,
                THRIFT_PORT INT NOT NULL,
                PRIMARY KEY (ANCHOR)
);

CREATE TABLE IF NOT EXISTS MB_NODE_HEARTBEAT (
                NODE_ID VARCHAR(512) NOT NULL,
                LAST_HEARTBEAT BIGINT NOT NULL,
                IS_NEW_NODE TINYINT NOT NULL,
                CLUSTER_AGENT_HOST VARCHAR(512) NOT NULL,
                CLUSTER_AGENT_PORT INT NOT NULL,
                PRIMARY KEY (NODE_ID)
);

CREATE TABLE IF NOT EXISTS MB_MEMBERSHIP (
                EVENT_ID BIGINT NOT NULL AUTO_INCREMENT,
                NODE_ID VARCHAR(512) NOT NULL,
                CHANGE_TYPE tinyint(4) NOT NULL,
                CHANGED_MEMBER_ID VARCHAR(512) NOT NULL,
                PRIMARY KEY (EVENT_ID)
);