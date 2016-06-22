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

package org.wso2.carbon.andes.core.internal.configuration.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Class to store property value pairs related to configurations. This is primarily used in StoreConfiguration
 */
public class ConfigurationProperties {

    /**
     * Internal map to store property values
     */
    private Map<String, String> propertyValueMap;

    public ConfigurationProperties() {
        propertyValueMap = new HashMap<String, String>();
    }

    /**
     * Adds a property
     *
     * @param property property name
     * @param value    value of property
     */
    public void addProperty(String property, String value) {
        propertyValueMap.put(property, value);
    }

    /**
     * Returns a value for the given property
     *
     * @param propertyName property name
     * @return value for the property, empty string for invalid property
     */
    public String getProperty(String propertyName) {

        return getProperty(propertyName, "");

    }

    /**
     * Returns a value for the given property
     *
     * @param propertyName property name
     * @param defaultValue value to return if the property was not found.
     * @return value for the property, default value if the property is not
     * found
     */
    public String getProperty(String propertyName, String defaultValue) {
        String value = propertyValueMap.get(propertyName);
        if (null == value) {
            value = defaultValue;
        }
        return value;
    }

    /**
     * Returns a value for the given property
     *
     * @param propertyName property name
     * @param defaultValue value to return if the property was not found.
     * @return value for the property, default value if the property is not
     * found
     */
    public int getProperty(String propertyName, int defaultValue) {
        String valueFromConfig = propertyValueMap.get(propertyName);
        int value = 0;

        if (null == valueFromConfig) {
            value = defaultValue;
        } else {
            value = Integer.parseInt(valueFromConfig);
        }
        return value;
    }

    /**
     * Returns a value for the given property
     *
     * @param propertyName property name
     * @param defaultValue value to return if the property was not found.
     * @return value for the property, default value if the property is not
     * found
     */
    public boolean getProperty(String propertyName, boolean defaultValue) {
        String valueFromConfig = propertyValueMap.get(propertyName);
        boolean value = false;

        if (null == valueFromConfig) {
            value = defaultValue;
        } else {
            value = Boolean.parseBoolean(valueFromConfig);
        }
        return value;
    }

}
