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

package org.wso2.carbon.andes.core.internal.configuration;

import org.wso2.carbon.andes.core.internal.configuration.util.ConfigurationProperties;

/**
 * This contains all persistent store related configurations used by andes. Even though master-datasources.xml
 * handles database-level configurations, These properties control how andes accesses the data store.
 */
public class StoreConfiguration {

    /**
     * Qualified name of the implementation of MessageStore interface
     */
    private String messageStoreClassName;

    /**
     * Qualified name of the implementation of AndesContextStore interface
     */
    private String andesContextStoreClassName;

    /**
     * For each MessageStore implementation there is a set of properties specific for the
     * database connection. These properties, key value pairs, are stored in this.
     */
    private ConfigurationProperties messageStoreProperties;

    /**
     * For each AndesContextStore implementation there is a set of properties specific for the
     * database connection. These properties, key value pairs, are stored in this.
     */
    private ConfigurationProperties andesContextStoreProperties;

    /**
     * These properties are the current configurations related to how andes accesses the store.
     * Kept for reference.
     */
    public static final String DATA_SOURCE = "dataSource";

    public StoreConfiguration() {
        messageStoreProperties = new ConfigurationProperties();
        andesContextStoreProperties = new ConfigurationProperties();
    }


    /**
     * Qualified name of the implementation of MessageStore interface
     */
    public String getMessageStoreClassName() {
        return messageStoreClassName;
    }

    /**
     * Set MessageStore implementation class name
     *
     * @param messageStoreClassName qualified name of the class
     */
    public void setMessageStoreClassName(String messageStoreClassName) {
        this.messageStoreClassName = messageStoreClassName;
    }

    /**
     * Qualified name of the implementation of AndesContextStore interface
     */
    public String getAndesContextStoreClassName() {
        return andesContextStoreClassName;
    }

    /**
     * Set AndesContextStore implementation class name
     *
     * @param andesContextStoreClassName qualified name of the class
     */
    public void setAndesContextStoreClassName(String andesContextStoreClassName) {
        this.andesContextStoreClassName = andesContextStoreClassName;
    }

    /**
     * Get properties for MessageStore connection
     *
     * @return ConfigurationProperties
     */
    public ConfigurationProperties getMessageStoreProperties() {
        return messageStoreProperties;
    }

    /**
     * Add MessageStore property with value
     *
     * @param propertyName property name
     * @param value        property value map
     */
    public void addMessageStoreProperty(String propertyName, String value) {
        messageStoreProperties.addProperty(propertyName, value);
    }

    /**
     * Get properties for AndesContextStore connection
     *
     * @return ConfigurationProperties
     */
    public ConfigurationProperties getContextStoreProperties() {
        return andesContextStoreProperties;
    }

    /**
     * Add AndesContextStore property with value
     *
     * @param propertyName property name
     * @param value        property value
     */
    public void addContextStoreProperty(String propertyName, String value) {
        andesContextStoreProperties.addProperty(propertyName, value);
    }
}
