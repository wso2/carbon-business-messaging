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

/**
 * This class holds the constants used across andes project
 */
public class AndesConstants {

    public static final String MESSAGE_EXPIRATION_PROPERTY = "Message Expiration";

    /**
     * The default Dead Letter Channel queue name suffix
     */
    public static final String DEAD_LETTER_QUEUE_SUFFIX = "DeadLetterChannel";

    /**
     * The separator to separate a domain name and the queue name in a tenant mode.
     */
    public static final String TENANT_SEPARATOR = "/";

    /**
     * The protocol name for the DLC.
     */
    public static final String DLC_PROTOCOL_NAME = "DLC";

    /**
     * The protocol version for the DLC.
     */
    public static final String DLC_PROTOCOL_VERSION = "default";

    /**
     * The destination type for the DLC.
     */
    public static final DestinationType DLC_DESTINATION_TYPE = DestinationType.QUEUE;
}
