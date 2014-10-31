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
 * Statistic configuration values manage by user interface & relevant configurations
 */
public class StatPublisherConfiguration {

    //credential details

    // username of publishing destination
    private String username = "";
    //password of publishing destination
    private String password = "";
    //publishing destination urls
    private String url = "";
    //node url of current MB node (this will use t uniquely identify MB node)
    private String nodeURL;

    //enable Stat publisher features (message,system and message broker)
    private boolean messageStatEnable = false;
    private boolean systemStatEnable = false;
    private boolean mbStatEnable = false;

    /**
     * Get Username
     *
     * @return username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Set Username
     *
     * @param username - username of CEP/BAM
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Get password
     *
     * @return password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Set password
     *
     * @param password - password of CEP/BAM
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Get url
     *
     * @return url
     */
    public String getURL() {
        return url;
    }

    /**
     * Set url
     *
     * @param url - destination to publish statistics
     */
    public void setURL(String url) {
        this.url = url;
    }

    /**
     * Get value of messageStatEnable
     *
     * @return messageStatEnable
     */
    public boolean isMessageStatEnable() {
        return messageStatEnable;
    }

    /**
     * Set whether message stat publishing enable or not
     *
     * @param messageStatEnable - whether enable message stat publishing or not
     */
    public void setMessageStatEnable(boolean messageStatEnable) {
        this.messageStatEnable = messageStatEnable;
    }

    /**
     * Get value of systemStatEnable
     *
     * @return systemStatEnable
     */
    public boolean isSystemStatEnable() {
        return systemStatEnable;
    }

    /**
     * Set whether system stat enable or not
     *
     * @param systemStatEnable -  whether enable system stat publishing or not
     */
    public void setSystemStatEnable(boolean systemStatEnable) {
        this.systemStatEnable = systemStatEnable;
    }

    /**
     * Get value of mbStatEnable
     *
     * @return mbStatEnable
     */
    public boolean isMbStatEnable() {
        return mbStatEnable;
    }

    /**
     * Set whether message broker stat enable or not
     *
     * @param mbStatEnable -  whether enable mb stat publishing or not
     */
    public void setMbStatEnable(boolean mbStatEnable) {
        this.mbStatEnable = mbStatEnable;
    }

    /**
     * Get nodeURL
     *
     * @return nodeURL
     */
    public String getNodeURL() {
        return nodeURL;
    }

    /**
     * Set value of nodeURL
     *
     * @param nodeURL - Unique value of the node
     */
    public void setNodeURL(String nodeURL) {
        this.nodeURL = nodeURL;
    }

}
