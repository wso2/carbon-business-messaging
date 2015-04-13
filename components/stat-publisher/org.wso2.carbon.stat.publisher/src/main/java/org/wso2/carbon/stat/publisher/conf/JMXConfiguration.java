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
 * Configuration values from jmx and carbon xml files.
 */
public class JMXConfiguration {

    private String hostName;
    private String rmiRegistryPort;
    private String offSet;

    /**
     * Set value of hostName node
     * @param hostName - connect to MB server
     */
    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    /**
     * Set value of rmiRegistryPort node
     * @param rmiRegistryPort - Registry port used by RMI
     */
    public void setRmiRegistryPort(String rmiRegistryPort) {
        this.rmiRegistryPort = rmiRegistryPort;
    }

    /**
     * Set value of offSet node
     * @param offSet - which used to change port
     */
    public void setOffSet(String offSet) {
        this.offSet = offSet;
    }

    /**
     * Get value of hostName
     * @return hostName
     */
    public String getHostName() {
        return hostName;
    }

    /**
     * Get value of rmiRegistryPort
     * @return rmiRegistryPort
     */
    public String getRmiRegistryPort() {
        return rmiRegistryPort;
    }

    /**
     * Get value of offSet
     * @return offSet
     */
    public String getOffSet() {
        return offSet;
    }

}
