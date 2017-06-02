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
 
-- WSO2 Message Broker v 3.1.0 to v 3.2.0 migration script for mssql --

UPDATE MB_EXCHANGE SET EXCHANGE_DATA = REPLACE(EXCHANGE_DATA, 'exchangeName=', 'messageRouterName=');

DELETE FROM MB_DURABLE_SUBSCRIPTION;

DROP TABLE MB_EXPIRATION_DATA;

IF NOT EXISTS (SELECT * FROM sys.objects WHERE OBJECT_ID = OBJECT_ID(N'[DB0].[MB_EXPIRATION_DATA]') AND TYPE IN (N'U'))
CREATE TABLE MB_EXPIRATION_DATA (
                MESSAGE_ID BIGINT UNIQUE,
                EXPIRATION_TIME BIGINT,
                DLC_QUEUE_ID INTEGER NOT NULL,
                MESSAGE_DESTINATION VARCHAR(512) NOT NULL,
                FOREIGN KEY (MESSAGE_ID) REFERENCES MB_METADATA (MESSAGE_ID)
                ON DELETE CASCADE
);

IF NOT EXISTS (SELECT * FROM sys.objects WHERE OBJECT_ID = OBJECT_ID(N'[DB0].[MB_DTX_XID]') AND TYPE IN (N'U'))
CREATE TABLE MB_DTX_XID (
                INTERNAL_XID BIGINT UNIQUE NOT NULL,
                NODE_ID VARCHAR(512) NOT NULL,
                FORMAT_CODE BIGINT NOT NULL,
                GLOBAL_ID VARBINARY(260), -- AMQP-10 vbin8 type
                BRANCH_ID VARBINARY(260), -- AMQP-10 vbin8 type
                PRIMARY KEY (INTERNAL_XID, NODE_ID)
);

-- create table dtx prepared dequeue record
IF NOT EXISTS (SELECT * FROM sys.objects WHERE OBJECT_ID = OBJECT_ID(N'[DB0].[MB_DTX_DEQUEUE_RECORD]') AND TYPE IN (N'U'))
CREATE TABLE MB_DTX_DEQUEUE_RECORD (
                INTERNAL_XID BIGINT NOT NULL,
                MESSAGE_ID BIGINT NOT NULL,
                QUEUE_NAME VARCHAR(512) NOT NULL,
                PRIMARY KEY (MESSAGE_ID),
                FOREIGN KEY (INTERNAL_XID) REFERENCES MB_DTX_XID (INTERNAL_XID)
                ON DELETE CASCADE
);

-- create table dtx prepared enqueue record
IF NOT EXISTS (SELECT * FROM sys.objects WHERE OBJECT_ID = OBJECT_ID(N'[DB0].[MB_DTX_ENQUEUE_RECORD]') AND TYPE IN (N'U'))
CREATE TABLE MB_DTX_ENQUEUE_RECORD (
                INTERNAL_XID BIGINT NOT NULL,
                MESSAGE_ID BIGINT NOT NULL,
                MESSAGE_METADATA VARBINARY(MAX) NOT NULL,
                PRIMARY KEY (MESSAGE_ID),
                FOREIGN KEY (INTERNAL_XID) REFERENCES MB_DTX_XID (INTERNAL_XID)
                ON DELETE CASCADE
);

-- create table dtx prepared content
IF NOT EXISTS (SELECT * FROM sys.objects WHERE OBJECT_ID = OBJECT_ID(N'[DB0].[MB_DTX_ENQUEUE_CONTENT]') AND TYPE IN (N'U'))
CREATE TABLE MB_DTX_ENQUEUE_CONTENT (
                MESSAGE_ID BIGINT NOT NULL,
                INTERNAL_XID BIGINT NOT NULL,
                CONTENT_OFFSET INTEGER NOT NULL,
                MESSAGE_CONTENT VARBINARY(MAX) NOT NULL,
                PRIMARY KEY (MESSAGE_ID,CONTENT_OFFSET),
                FOREIGN KEY (MESSAGE_ID) REFERENCES MB_DTX_ENQUEUE_RECORD (MESSAGE_ID)
                ON DELETE CASCADE
);

IF NOT EXISTS (SELECT * FROM sys.objects WHERE OBJECT_ID = OBJECT_ID(N'[DB0].[MB_DTX_DEQUEUE_CONTENT]') AND TYPE IN (N'U'))
CREATE TABLE MB_DTX_DEQUEUE_CONTENT (
                MESSAGE_ID BIGINT NOT NULL,
                INTERNAL_XID BIGINT NOT NULL,
                CONTENT_OFFSET INTEGER NOT NULL,
                MESSAGE_CONTENT VARBINARY(MAX) NOT NULL,
                PRIMARY KEY (MESSAGE_ID,CONTENT_OFFSET),
                FOREIGN KEY (MESSAGE_ID) REFERENCES MB_DTX_DEQUEUE_RECORD (MESSAGE_ID)
                ON DELETE CASCADE
);

IF NOT EXISTS (SELECT * FROM sys.objects WHERE OBJECT_ID = OBJECT_ID(N'[DB0].[MB_CLUSTER_EVENT]') AND TYPE IN (N'U'))
CREATE TABLE MB_CLUSTER_EVENT (
                        EVENT_ID bigint IDENTITY(1,1) NOT NULL,
                        ORIGINATED_NODE_ID varchar(512) NOT NULL,
                        DESTINED_NODE_ID varchar(512) NOT NULL,
                        EVENT_ARTIFACT VARCHAR(25) NOT NULL,
                        EVENT_TYPE varchar(25) NOT NULL,
                        EVENT_DETAILS varchar(1024) NOT NULL,
                        EVENT_DESCRIPTION VARCHAR(1024),
                        PRIMARY KEY (EVENT_ID)
);

--create cluster coordinator heartbeat
IF NOT EXISTS (SELECT * FROM sys.objects WHERE OBJECT_ID = OBJECT_ID(N'[DB0].[MB_COORDINATOR_HEARTBEAT]') AND TYPE IN (N'U'))
CREATE TABLE MB_COORDINATOR_HEARTBEAT (
                        ANCHOR INT NOT NULL,
                        NODE_ID VARCHAR(512) NOT NULL,
                        LAST_HEARTBEAT BIGINT NOT NULL,
                        THRIFT_HOST VARCHAR(512) NOT NULL,
                        THRIFT_PORT INT NOT NULL,
                        PRIMARY KEY (ANCHOR)
);

--create cluster node heartbeat
IF NOT EXISTS (SELECT * FROM sys.objects WHERE OBJECT_ID = OBJECT_ID(N'[DB0].[MB_NODE_HEARTBEAT]') AND TYPE IN (N'U'))
CREATE TABLE MB_NODE_HEARTBEAT (
                        NODE_ID VARCHAR(512) NOT NULL,
                        LAST_HEARTBEAT BIGINT NOT NULL,
                        IS_NEW_NODE TINYINT NOT NULL,
                        CLUSTER_AGENT_HOST VARCHAR(512) NOT NULL,
                        CLUSTER_AGENT_PORT INT NOT NULL,
                        PRIMARY KEY (NODE_ID)
);

--create membership
IF NOT EXISTS (SELECT * FROM sys.objects WHERE OBJECT_ID = OBJECT_ID(N'[DB0].[MB_MEMBERSHIP]') AND TYPE IN (N'U'))
CREATE TABLE MB_MEMBERSHIP (
                        EVENT_ID BIGINT IDENTITY(1,1) NOT NULL,
                        NODE_ID VARCHAR(512) NOT NULL,
                        CHANGE_TYPE TINYINT NOT NULL,
                        CHANGED_MEMBER_ID VARCHAR(512) NOT NULL,
                        PRIMARY KEY (EVENT_ID)
);