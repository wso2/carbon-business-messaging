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
 * This class defines the content of an Andes message.
 */
public class AndesMessagePart {
    long messageID;
    int offSet = 0;
    private byte[] data;
    private int dataLength;

    public int getOffset() {
        return offSet;
    }

    public void setOffSet(int offSet) {
        this.offSet = offSet;
    }

    public long getMessageID() {
        return messageID;
    }

    public void setMessageID(long messageID) {
        this.messageID = messageID;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public int getDataLength() {
        return dataLength;
    }

    public void setDataLength(int dataLength) {
        this.dataLength = dataLength;
    }

    /**
     * Create a clone, with new message ID
     *
     * @param messageId message id
     * @return returns AndesMessagePart
     */
    public AndesMessagePart shallowCopy(long messageId) {
        AndesMessagePart clone = new AndesMessagePart();
        clone.messageID = messageId;
        clone.offSet = offSet;
        clone.data = data;
        clone.dataLength = dataLength;
        return clone;
    }
}
