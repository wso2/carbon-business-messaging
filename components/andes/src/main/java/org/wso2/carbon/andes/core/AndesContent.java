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

import java.nio.ByteBuffer;

/**
 * Andes content is used by local subscriptions to retrieve message content.
 */
public interface AndesContent {

    /**
     * This method gives access to content of the message.
     *
     * @param offset            Starting byte position
     * @param destinationBuffer Destination Byte buffer
     * @return Number of bytes written to the buffer
     * @throws AndesException
     */
    int putContent(int offset, ByteBuffer destinationBuffer) throws AndesException;

    /**
     * Return the content length of the message
     *
     * @return content length
     */
    int getContentLength();
}
