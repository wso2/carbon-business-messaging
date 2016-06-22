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

/**
 * This class contains the immutable meta attributes of a config property. These are
 * immutable so that enums can contain them.
 */
public final class ImmutableMetaProperties implements MetaProperties {

    private final String keyInFile;
    private final String defaultValue;
    private final Class<?> dataType;
    private final String name;

    /**
     * constructor
     *
     * @param keyInFile    xpath expression to the property in the config file
     * @param defaultValue default value of property in case its not specified or found in config files.
     * @param dataType     expected data type of the property
     */
    public ImmutableMetaProperties(String name, String keyInFile, String defaultValue, Class<?> dataType) {
        this.name = name;
        this.keyInFile = keyInFile;
        this.defaultValue = defaultValue;
        this.dataType = dataType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getKeyInFile() {
        return keyInFile;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<?> getDataType() {
        return dataType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "Property : " + keyInFile + " data-type : " + dataType.getName() + " default value" +
                " : " + defaultValue;
    }


}
