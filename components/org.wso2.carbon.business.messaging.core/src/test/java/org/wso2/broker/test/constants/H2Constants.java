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

package org.wso2.broker.test.constants;

/**
 * Holds the database connection details related to h2 for dbUnit
 */
public class H2Constants {
    /**
     * Classpath of the JDBC driver
     */
    public static final String DATABASE_DRIVER = "org.h2.Driver";

    /**
     * Connection uri of the in-memory database
     */
    public static final String CONNECTION_URL = "jdbc:h2:mem:mbstore;DB_CLOSE_DELAY=-1";

    /**
     * user login name
     */
    public static final String USERNAME = "sa";

    /**
     * user login password
     */
    public static final String PASSWORD = "";

    /**
     * Resource location of the h2 database schema file
     */
    public static final String SCHEMA_URL = "schemas/h2-schema.sql";

    /**
     * Database encoding type
     */
    public static final String CHARSET = "UTF8";


}
