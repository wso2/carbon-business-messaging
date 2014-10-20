CREATE TABLE IF NOT EXISTS messages (
                message_id BIGINT,
                offset INTEGER,
                content VARBINARY(65500),
                PRIMARY KEY (message_id,offset)
);

CREATE TABLE IF NOT EXISTS queues (
                queue_id INTEGER AUTO_INCREMENT,
                name VARCHAR(512) NOT NULL,
                PRIMARY KEY (queue_id, name)
);

CREATE TABLE IF NOT EXISTS reference_counts (
                message_id BIGINT,
                reference_count INTEGER,
                PRIMARY KEY (message_id)
);

CREATE TABLE IF NOT EXISTS metadata (
                message_id BIGINT,
                queue_id INTEGER,
                data VARBINARY(65500),
                PRIMARY KEY (message_id, queue_id),
                FOREIGN KEY (queue_id)
                REFERENCES queues (queue_id)
);

CREATE TABLE IF NOT EXISTS expiration_data (
                message_id BIGINT UNIQUE,
                expiration_time BIGINT,
                destination VARCHAR(512) NOT NULL,
                FOREIGN KEY (message_id)
                REFERENCES metadata (message_id)
);

 
CREATE TABLE IF NOT EXISTS durable_subscriptions (
                        sub_id VARCHAR(512) NOT NULL, 
                        destination_identifier VARCHAR(512) NOT NULL,
                        data VARCHAR(2048) NOT NULL
);

CREATE TABLE IF NOT EXISTS node_info (
                        node_id VARCHAR(512) NOT NULL,
                        data VARCHAR(2048) NOT NULL,
                        PRIMARY KEY(node_id)
);

CREATE TABLE IF NOT EXISTS exchanges (
                        name VARCHAR(512) NOT NULL,
                        data VARCHAR(2048) NOT NULL,
                        PRIMARY KEY(name)
);

CREATE TABLE IF NOT EXISTS queue_info (
                        name VARCHAR(512) NOT NULL,
                        data VARCHAR(2048) NOT NULL,
                        PRIMARY KEY(name)
);

CREATE TABLE IF NOT EXISTS bindings (
                        exchange_name VARCHAR(512) NOT NULL,
                        queue_name VARCHAR(512) NOT NULL,
                        binding_info VARCHAR(2048) NOT NULL,
                        FOREIGN KEY (exchange_name) REFERENCES exchanges (name),
                        FOREIGN KEY (queue_name) REFERENCES queue_info (name)
);

CREATE TABLE IF NOT EXISTS queue_counter (
                        name VARCHAR(512) NOT NULL,
                        count BIGINT, 
                        PRIMARY KEY (name) 
);

