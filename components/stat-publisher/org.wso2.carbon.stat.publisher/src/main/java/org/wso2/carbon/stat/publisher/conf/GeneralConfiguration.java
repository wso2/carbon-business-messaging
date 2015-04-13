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

/**
 *  Read general configuration values from mbStatConfiguration.xml file.
 */
public class GeneralConfiguration {
    //timeInterval for timerTask to publish statistics
    private int timeInterval;

    //number of queue slots to store message/ack details
    private int asyncMessagePublisherBufferTime;

    /**
     * Set value of timeInterval node
     * @param timeInterval - set for timerTask
     */
    public void setTimeInterval(int timeInterval) {
        this.timeInterval = timeInterval;
    }

    /**
     * Get value of timeInterval node
     * @return timeInterval
     */
    public int getTimeInterval() {
        return timeInterval;
    }

    /**
     * Set value of asyncMessagePublisherBufferTime node
     * @param asyncMessagePublisherBufferTime - size of queue which store messages
     */
    public void setAsyncMessagePublisherBufferTime(int asyncMessagePublisherBufferTime) {
        this.asyncMessagePublisherBufferTime = asyncMessagePublisherBufferTime;
    }

    /**
     * Get value of asyncMessagePublisherBufferTime node
     * @return asyncMessagePublisherBufferTime
     */
    public int getAsyncMessagePublisherBufferTime() {
        return asyncMessagePublisherBufferTime;
    }

}
