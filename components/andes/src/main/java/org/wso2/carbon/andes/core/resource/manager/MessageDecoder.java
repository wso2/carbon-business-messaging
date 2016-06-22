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

package org.wso2.carbon.andes.core.resource.manager;

import org.wso2.carbon.andes.core.AndesException;
import org.wso2.carbon.andes.core.AndesMessage;
import org.wso2.carbon.andes.core.AndesMessageMetadata;

import java.util.Map;

/**
 * Interface for decoding {@link AndesMessage} for end users.
 */
public interface MessageDecoder {
    /**
     * Gets the message properties.
     *
     * @param andesMessageMetadata The andes message metadata object.
     * @return A map of key value pair with the properties.
     * @throws AndesException
     */
    Map<String, String> getMessageProperties(AndesMessageMetadata andesMessageMetadata) throws AndesException;

    /**
     * Gets the message content as a string.
     *
     * @param andesMessage The andes message object.
     * @return Message content as a string.
     * @throws AndesException
     */
    String getMessageContent(AndesMessage andesMessage) throws AndesException;

}
