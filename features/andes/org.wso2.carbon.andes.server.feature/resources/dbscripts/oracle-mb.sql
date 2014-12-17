CREATE TABLE messages (
	message_id NUMBER(19),
	offset INT,
	content BLOB,
	CONSTRAINT pk_messages PRIMARY KEY (message_id,offset))
/
CREATE TABLE queues (
	queue_id INT,
	name VARCHAR2(512) UNIQUE,
        CONSTRAINT pk_queues PRIMARY KEY (queue_id))
/
CREATE SEQUENCE queues_sequence START WITH 1 INCREMENT BY 1 NOCACHE
/
CREATE OR REPLACE TRIGGER queues_trigger 
	BEFORE INSERT ON queues
	REFERENCING NEW AS NEW
	FOR EACH ROW
	BEGIN
		SELECT queues_sequence.nextval INTO :NEW.queue_id FROM dual;
	END;
/	         
CREATE TABLE reference_counts (
	message_id NUMBER(19),
        reference_count INT,
        CONSTRAINT pk_reference_counts PRIMARY KEY (message_id)
)
/
CREATE TABLE metadata (
	message_id NUMBER(19),
	queue_id INT,
        data RAW(2000),
        CONSTRAINT pk_metadata PRIMARY KEY (message_id),
        CONSTRAINT fk_metadata_queues FOREIGN KEY (queue_id) REFERENCES queues (queue_id)
)
/
CREATE TABLE expiration_data (
	message_id NUMBER(19) UNIQUE,
	expiration_time NUMBER(19),
        destination VARCHAR2(512) NOT NULL,
        CONSTRAINT fk_expiration_data FOREIGN KEY (message_id) REFERENCES metadata (message_id)
)
/
CREATE TABLE durable_subscriptions (
	sub_id VARCHAR2(512) NOT NULL, 
	destination_identifier VARCHAR2(512) NOT NULL,
        data VARCHAR2(2048) NOT NULL
)
/
CREATE TABLE node_info (
	node_id VARCHAR2(512) NOT NULL,
	data VARCHAR2(2048) NOT NULL,
        CONSTRAINT pk_node_info PRIMARY KEY (node_id)
)
/
CREATE TABLE exchanges (
	name VARCHAR2(512) NOT NULL,
	data VARCHAR2(2048) NOT NULL,
        CONSTRAINT pk_exchanges PRIMARY KEY (name)
)
/
CREATE TABLE queue_info (
	name VARCHAR2(512) NOT NULL,
	data VARCHAR2(2048) NOT NULL,
        CONSTRAINT pk_queue_info PRIMARY KEY (name)
)
/
CREATE TABLE bindings (
	exchange_name VARCHAR2(512) NOT NULL,
	queue_name VARCHAR2(512) NOT NULL,
        binding_info VARCHAR2(2048) NOT NULL,
        CONSTRAINT fk_bindings_exchanges FOREIGN KEY (exchange_name) REFERENCES exchanges (name),
        CONSTRAINT fk_bindings_queue_info FOREIGN KEY (queue_name) REFERENCES queue_info (name)
)
/
CREATE TABLE queue_counter (
	name VARCHAR2(512) NOT NULL,
        count NUMBER(19), 
        CONSTRAINT pk_queue_counter PRIMARY KEY (name) 
)
/
