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

package org.wso2.carbon.andes.core;

import org.apache.commons.lang.StringUtils;

/**
 * The transport protocol type which connects with Andes core.
 */
public final class ProtocolType {

    /**
     * The name of the protocol.
     */
    private String protocolName;

    /**
     * The version of the protocol.
     */
    private String version;

    /**
     * We keep the hashCode pre calculated so that operations will be faster.
     */
    private int hashCode;

    private static String separator = "-";

    /**
     * Constructor with only protocol name.
     * Character '-' is not allowed within a protocol name as it is used as the separator between protocol and version.
     *
     * @param protocolName The name of the protocol
     * @param version      The version of the protocol
     * @throws AndesException
     */
    public ProtocolType(String protocolName, String version) throws AndesException {
        setProtocolName(protocolName);
        setVersion(version);
        generateHashCode();
    }

    /**
     * Create a ProtocolType by passing a string.
     *
     * @param protocolType The protocol type as a string.
     * @throws AndesException
     */
    public ProtocolType(String protocolType) throws AndesException {
        if (StringUtils.isNotEmpty(protocolType)) {
            String[] properties = protocolType.split(separator, 2);

            if (properties.length == 2) {
                setProtocolName(properties[0]);
                setVersion(properties[1]);
                generateHashCode();
            } else {
                throw new AndesException("An invalid string is given for creating a ProtocolType : " + protocolType);
            }
        } else {
            throw new AndesException("An empty string is given for creating a ProtocolType");
        }
    }

    public String getProtocolName() {
        return protocolName;
    }

    /**
     * Validate and set protocol name.
     * Character '- is not allowed within a protocol name as it is used as the separator within this class.
     *
     * @param protocolName The protocol name to set
     * @throws AndesException
     */
    private void setProtocolName(String protocolName) throws AndesException {
        if (StringUtils.isNotEmpty(protocolName)) {
            if (protocolName.contains(separator)) {
                throw new AndesException("Character '" + separator + "' is not allowed within a protocol name");
            }
            this.protocolName = protocolName;
        } else {
            throw new AndesException("Protocol name cannot be empty");
        }
    }

    public String getVersion() {
        return version;
    }

    private void setVersion(String version) throws AndesException {
        if (StringUtils.isNotEmpty(version)) {
            this.version = version;
        } else {
            throw new AndesException("Protocol version cannot be empty");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        /* We're using only the pre-calculated hashcode value here since we expect to have ony a limited number
          * of protocol types in the system and each hash code is calculated using the string values
          * protocolName and version.
         */
        return this.hashCode == o.hashCode();
    }

    /**
     * Since there are limited set of ProtocolType objects available, we can keep the hash code pre calculated
     * and the the hashcode number set will only grow when a new protocol is plugged in to Andes.
     */
    private void generateHashCode() {
        int result = protocolName.toLowerCase().hashCode();
        result = 31 * result + version.toLowerCase().hashCode();
        this.hashCode = result;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        return protocolName + separator + version;
    }
}
