
--create table messages
IF NOT EXISTS (SELECT * FROM sys.objects WHERE OBJECT_ID = OBJECT_ID(N'[DB0].[messages]') AND TYPE IN (N'U'))
CREATE TABLE messages (
                message_id BIGINT,
                offset INTEGER,
                content VARBINARY(MAX),
                PRIMARY KEY (message_id,offset)
);

--create table queues
IF NOT EXISTS (SELECT * FROM sys.objects WHERE OBJECT_ID = OBJECT_ID(N'[DB0].[queues]') AND TYPE IN (N'U'))
CREATE TABLE queues (
                queue_id INTEGER IDENTITY(1,1),
                name VARCHAR(512) NOT NULL,
                PRIMARY KEY(queue_id, name),
                UNIQUE (queue_id),
                CONSTRAINT const UNIQUE (name)              
);

--create table reference_counts
IF NOT EXISTS (SELECT * FROM sys.objects WHERE OBJECT_ID = OBJECT_ID(N'[DB0].[reference_counts]') AND TYPE IN (N'U'))
CREATE TABLE reference_counts (
                message_id BIGINT,
                reference_count INTEGER,
                PRIMARY KEY (message_id)              
);

--create table metadata
IF NOT EXISTS (SELECT * FROM sys.objects WHERE OBJECT_ID = OBJECT_ID(N'[DB0].[metadata]') AND TYPE IN (N'U'))
CREATE TABLE metadata (
                message_id BIGINT,
                queue_id INTEGER,
                data VARBINARY(MAX),
                PRIMARY KEY (message_id, queue_id),
                UNIQUE (message_id),
                FOREIGN KEY (queue_id)
                REFERENCES queues (queue_id)             
);

--create table expiration_data
IF NOT EXISTS (SELECT * FROM sys.objects WHERE OBJECT_ID = OBJECT_ID(N'[DB0].[expiration_data]') AND TYPE IN (N'U'))
CREATE TABLE expiration_data (
                message_id BIGINT UNIQUE,
                expiration_time BIGINT,
                destination VARCHAR(512) NOT NULL,
                FOREIGN KEY (message_id)
                REFERENCES metadata (message_id)             
);

--create table durable_subscriptions
IF NOT EXISTS (SELECT * FROM sys.objects WHERE OBJECT_ID = OBJECT_ID(N'[DB0].[durable_subscriptions]') AND TYPE IN (N'U'))
CREATE TABLE durable_subscriptions (
                        sub_id VARCHAR(512) NOT NULL, 
                        destination_identifier VARCHAR(512) NOT NULL,
                        data VARCHAR(2048) NOT NULL           
);

--create table node_info
IF NOT EXISTS (SELECT * FROM sys.objects WHERE OBJECT_ID = OBJECT_ID(N'[DB0].[node_info]') AND TYPE IN (N'U'))
CREATE TABLE node_info (
                        node_id VARCHAR(512) NOT NULL,
                        data VARCHAR(2048) NOT NULL,
                        PRIMARY KEY(node_id)           
);

--create table exchanges
IF NOT EXISTS (SELECT * FROM sys.objects WHERE OBJECT_ID = OBJECT_ID(N'[DB0].[exchanges]') AND TYPE IN (N'U'))
CREATE TABLE exchanges (
                        name VARCHAR(512) NOT NULL,
                        data VARCHAR(2048) NOT NULL,
                        PRIMARY KEY(name)           
);

--create table queue_info
IF NOT EXISTS (SELECT * FROM sys.objects WHERE OBJECT_ID = OBJECT_ID(N'[DB0].[queue_info]') AND TYPE IN (N'U'))
CREATE TABLE queue_info (
                        name VARCHAR(512) NOT NULL,
                        data VARCHAR(2048) NOT NULL,
                        PRIMARY KEY(name)           
);

--create table bindings
IF NOT EXISTS (SELECT * FROM sys.objects WHERE OBJECT_ID = OBJECT_ID(N'[DB0].[bindings]') AND TYPE IN (N'U'))
CREATE TABLE bindings (
                        exchange_name VARCHAR(512) NOT NULL,
                        queue_name VARCHAR(512) NOT NULL,
                        binding_info VARCHAR(2048) NOT NULL,
                        FOREIGN KEY (exchange_name) REFERENCES exchanges (name),
                        FOREIGN KEY (queue_name) REFERENCES queue_info (name)       
);

--create table queue_counter
IF NOT EXISTS (SELECT * FROM sys.objects WHERE OBJECT_ID = OBJECT_ID(N'[DB0].[queue_counter]') AND TYPE IN (N'U'))
CREATE TABLE queue_counter (
                        name VARCHAR(512) NOT NULL,
                        count BIGINT, 
                        PRIMARY KEY (name)      
);

