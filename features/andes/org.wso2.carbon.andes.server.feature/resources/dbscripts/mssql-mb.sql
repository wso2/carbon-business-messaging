/*
 * Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 
-- WSO2 Message Broker MS SQL Database schema --

-- Start of Message Store Tables --

--create table messages
IF NOT EXISTS (SELECT * FROM sys.objects WHERE OBJECT_ID = OBJECT_ID(N'[DB0].[MB_CONTENT]') AND TYPE IN (N'U'))
CREATE TABLE MB_CONTENT (
                MESSAGE_ID BIGINT,
                CONTENT_OFFSET INTEGER,
                MESSAGE_CONTENT VARBINARY(MAX),
                PRIMARY KEY (MESSAGE_ID,CONTENT_OFFSET)
);

--create table queues
IF NOT EXISTS (SELECT * FROM sys.objects WHERE OBJECT_ID = OBJECT_ID(N'[DB0].[MB_QUEUE_MAPPING]') AND TYPE IN (N'U'))
CREATE TABLE MB_QUEUE_MAPPING (
                QUEUE_ID INTEGER IDENTITY(1,1),
                QUEUE_NAME VARCHAR(512) NOT NULL,
                PRIMARY KEY(QUEUE_ID),
                UNIQUE (QUEUE_ID),
                CONSTRAINT const UNIQUE (QUEUE_NAME)              
);

--create table metadata
IF NOT EXISTS (SELECT * FROM sys.objects WHERE OBJECT_ID = OBJECT_ID(N'[DB0].[MB_METADATA]') AND TYPE IN (N'U'))
CREATE TABLE MB_METADATA (
                MESSAGE_ID BIGINT,
                QUEUE_ID INTEGER,
                MESSAGE_METADATA VARBINARY(MAX),
                PRIMARY KEY (MESSAGE_ID, QUEUE_ID),
                UNIQUE (MESSAGE_ID),
                FOREIGN KEY (QUEUE_ID) REFERENCES MB_QUEUE_MAPPING (QUEUE_ID)             
);

--create table expiration_data
IF NOT EXISTS (SELECT * FROM sys.objects WHERE OBJECT_ID = OBJECT_ID(N'[DB0].[MB_EXPIRATION_DATA]') AND TYPE IN (N'U'))
CREATE TABLE MB_EXPIRATION_DATA (
                MESSAGE_ID BIGINT UNIQUE,
                EXPIRATION_TIME BIGINT,
                MESSAGE_DESTINATION VARCHAR(512) NOT NULL,
                FOREIGN KEY (MESSAGE_ID) REFERENCES MB_METADATA (MESSAGE_ID)             
);

-- create table retained metadata
IF NOT EXISTS (SELECT * FROM sys.objects WHERE OBJECT_ID = OBJECT_ID(N'[DB0].[MB_RETAINED_METADATA]') AND TYPE IN (N'U'))
CREATE TABLE MB_RETAINED_METADATA (
                TOPIC_ID INTEGER,
                TOPIC_NAME VARCHAR(512) NOT NULL,
                MESSAGE_ID BIGINT,
                MESSAGE_METADATA VARBINARY(MAX),
                PRIMARY KEY (TOPIC_ID)
);


-- End of Message Store Tables --

-- Start of Andes Context Store Tables --

--create table durable_subscriptions
IF NOT EXISTS (SELECT * FROM sys.objects WHERE OBJECT_ID = OBJECT_ID(N'[DB0].[MB_DURABLE_SUBSCRIPTION]') AND TYPE IN (N'U'))
CREATE TABLE MB_DURABLE_SUBSCRIPTION (
                        SUBSCRIPTION_ID VARCHAR(512) NOT NULL, 
                        DESTINATION_IDENTIFIER VARCHAR(512) NOT NULL,
                        SUBSCRIPTION_DATA VARCHAR(2048) NOT NULL           
);

--create table node_info
IF NOT EXISTS (SELECT * FROM sys.objects WHERE OBJECT_ID = OBJECT_ID(N'[DB0].[MB_NODE]') AND TYPE IN (N'U'))
CREATE TABLE MB_NODE (
                        NODE_ID VARCHAR(512) NOT NULL,
                        NODE_DATA VARCHAR(2048) NOT NULL,
                        PRIMARY KEY(NODE_ID)           
);

--create table exchanges
IF NOT EXISTS (SELECT * FROM sys.objects WHERE OBJECT_ID = OBJECT_ID(N'[DB0].[MB_EXCHANGE]') AND TYPE IN (N'U'))
CREATE TABLE MB_EXCHANGE (
                        EXCHANGE_NAME VARCHAR(512) NOT NULL,
                        EXCHANGE_DATA VARCHAR(2048) NOT NULL,
                        PRIMARY KEY(EXCHANGE_NAME)           
);

--create table queue_info
IF NOT EXISTS (SELECT * FROM sys.objects WHERE OBJECT_ID = OBJECT_ID(N'[DB0].[MB_QUEUE]') AND TYPE IN (N'U'))
CREATE TABLE MB_QUEUE (
                        QUEUE_NAME VARCHAR(512) NOT NULL,
                        QUEUE_DATA VARCHAR(2048) NOT NULL,
                        PRIMARY KEY(QUEUE_NAME)           
);

--create table bindings
IF NOT EXISTS (SELECT * FROM sys.objects WHERE OBJECT_ID = OBJECT_ID(N'[DB0].[MB_BINDING]') AND TYPE IN (N'U'))
CREATE TABLE MB_BINDING (
                        EXCHANGE_NAME VARCHAR(512) NOT NULL,
                        QUEUE_NAME VARCHAR(512) NOT NULL,
                        BINDING_DETAILS VARCHAR(2048) NOT NULL,
                        FOREIGN KEY (EXCHANGE_NAME) REFERENCES MB_EXCHANGE (EXCHANGE_NAME),
                        FOREIGN KEY (QUEUE_NAME) REFERENCES MB_QUEUE (QUEUE_NAME)       
);

--create table queue_counter
IF NOT EXISTS (SELECT * FROM sys.objects WHERE OBJECT_ID = OBJECT_ID(N'[DB0].[MB_QUEUE_COUNTER]') AND TYPE IN (N'U'))
CREATE TABLE MB_QUEUE_COUNTER (
                        QUEUE_NAME VARCHAR(512) NOT NULL,
                        MESSAGE_COUNT BIGINT, 
                        PRIMARY KEY (QUEUE_NAME)      
);

-- create table retained_content
IF NOT EXISTS (SELECT * FROM sys.objects WHERE OBJECT_ID = OBJECT_ID(N'[DB0].[MB_RETAINED_CONTENT]') AND TYPE IN (N'U'))
CREATE TABLE MB_RETAINED_CONTENT (
                MESSAGE_ID BIGINT,
                CONTENT_OFFSET INTEGER,
                MESSAGE_CONTENT VARBINARY(MAX) NOT NULL,
                PRIMARY KEY (MESSAGE_ID,CONTENT_OFFSET)
);

IF NOT EXISTS (SELECT * FROM sys.objects WHERE OBJECT_ID = OBJECT_ID(N'[DB0].[MB_MSG_STORE_STATUS]') AND TYPE IN (N'U'))
CREATE TABLE MB_MSG_STORE_STATUS (
                        NODE_ID VARCHAR(512) NOT NULL,
                        TIME_STAMP BIGINT, 
                        PRIMARY KEY(NODE_ID, TIME_STAMP)   
);

-- End of Andes Context Store Tables --
