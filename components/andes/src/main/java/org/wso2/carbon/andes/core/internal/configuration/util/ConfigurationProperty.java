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
 * Interface used to abstract common attributes and behaviour of similar enums.
 * The use case is that we have different types of configurations (mqtt,qpid,
 * virtual-host related) But all config properties have the same attributes.
 * the key, default value, and data type. By implementing this interface hierarchy,
 * we can avoid code duplication of repeating these properties and their access logic.
 * <p/>
 * Inspired by : http://sett.ociweb.com/sett/settNov2012.html
 */
public interface ConfigurationProperty {

    /**
     * returns meta data about the configuration.
     *
     * @return a meta data
     */
    MetaProperties get();

    /**
     * Returns the deprecated configuration definition after introducing this.
     *
     * @return an instance of {@link ConfigurationProperty}
     */
    ConfigurationProperty getDeprecated();

    /**
     * Indicates whether this configuration deprecates another configuration.
     */
    boolean hasDeprecatedProperty();
}
