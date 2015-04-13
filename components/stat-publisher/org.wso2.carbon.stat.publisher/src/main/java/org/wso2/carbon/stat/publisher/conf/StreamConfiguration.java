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
 * Read stream configuration values from mbStatConfiguration.xml file.
 */
public class StreamConfiguration {
    private String messageStreamVersion;
    private String acknowledgeStreamVersion;
    private String systemStatisticStreamVersion;
    private String mbStatisticStreamVersion;

    /**
     * Set value of message version
     * @param messageStreamVersion - version of stream used to publish message stats
     */

    public void setMessageStreamVersion(String messageStreamVersion) {
        this.messageStreamVersion = messageStreamVersion;
    }

    /**
     * Set value of acknowledge packet version
     * @param acknowledgeStreamVersion - version of stream used to publish ack stats
     */
    public void setAcknowledgeStreamVersion(String acknowledgeStreamVersion) {
        this.acknowledgeStreamVersion = acknowledgeStreamVersion;
    }

    /**
     * Set value of system statistic version
     * @param systemStatisticStreamVersion - version of stream used to publish system stats
     */
    public void setSystemStatisticStreamVersion(String systemStatisticStreamVersion) {
        this.systemStatisticStreamVersion = systemStatisticStreamVersion;
    }

    /**
     * Set value of message broker statistic version
     * @param mbStatisticStreamVersion - version of stream used to publish MB stats
     */
    public void setMbStatisticStreamVersion(String mbStatisticStreamVersion) {
        this.mbStatisticStreamVersion = mbStatisticStreamVersion;
    }

    /**
     * Get message version
     * @return messageStreamVersion Stream version for message stat publisher stream
     */
    public String getMessageStreamVersion() {
        return messageStreamVersion;
    }

    /**
     * Get acknowledge packet version
     * @return acknowledgeStreamVersion Stream version for acknowledge message stat publisher stream
     */
    public String getAcknowledgeStreamVersion() {
        return acknowledgeStreamVersion;
    }

    /**
     * Get system statistic version
     * @return systemStatisticStreamVersion Stream version for system stat publisher stream
     */
    public String getSystemStatisticStreamVersion() {
        return systemStatisticStreamVersion;
    }

    /**
     * Get message broker statistic version
     * @return mbStatisticStreamVersion Stream version for MB stat publisher stream
     */
    public String getMbStatisticStreamVersion() {
        return mbStatisticStreamVersion;
    }


}
