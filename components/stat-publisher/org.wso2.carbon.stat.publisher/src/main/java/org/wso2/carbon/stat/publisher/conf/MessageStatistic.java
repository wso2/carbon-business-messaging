/*
*  Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
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

package org.wso2.carbon.stat.publisher.conf;

import org.wso2.andes.kernel.AndesAckData;
import org.wso2.andes.kernel.AndesMessageMetadata;

/**
 *  Store message and ack statistics
 */
public class MessageStatistic {
    //This boolean value use to determine it's a message or Ack message
    private boolean message;
    private AndesMessageMetadata andesMessageMetadata;
    private AndesAckData andesAckData;
    private int noOfSubscribers;
    private String domain;

    /**
     * Get ack statistics
     * @return ack stat
     */
    public AndesAckData getAndesAckData() {
        return andesAckData;
    }

    /**
     * Set ack statistics
     * @param andesAckData - sck statistics sent from andes
     */
    public void setAndesAckData(AndesAckData andesAckData) {
        this.andesAckData = andesAckData;
    }

    /**
     * This will check whether ack or message
     * If it is message stat then return true ,If it is Ack message stat then return false
     * @return message/ack
     */
    public boolean isMessage() {
        return message;
    }

    /**
     * This will set whether ack or message
     * If it is message stat set as true ,If it is Ack message stat set as false
     * @param message - message or ack
     */
    public void setMessage(boolean message) {
        this.message = message;
    }

    /**
     * Get message meta data
     * @return message meta data
     */
    public AndesMessageMetadata getAndesMessageMetadata() {
        return andesMessageMetadata;
    }

    /**
     * Set message meta data
     * @param andesMessageMetadata - message meta data
     */
    public void setAndesMessageMetadata(AndesMessageMetadata andesMessageMetadata) {
        this.andesMessageMetadata = andesMessageMetadata;
    }

    /**
     * Get number of Subscribers for message
     * @return number of Subscribers
     */
    public int getNoOfSubscribers() {
        return noOfSubscribers;
    }

    /**
     * Set number of Subscribers for message
     * @param noOfSubscribers - subscriber count for particular message
     */
    public void setNoOfSubscribers(int noOfSubscribers) {
        this.noOfSubscribers = noOfSubscribers;
    }

    /**
     * Get tenant domain
     * @return tenant domain
     */
    public String getDomain() {
        return domain;
    }

    /**
     * Set tenant domain
     * @param domain - used to identify particular tenant
     */
    public void setDomain(String domain) {
        this.domain = domain;
    }
}
