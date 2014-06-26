/*
*  Copyright (c) 2005-2011, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.andes.cluster.mgt;

public class NodeDetail {

    private String hostName;
    private String ipAddress;
    private String zookeeperID;

    private long numOfTopics;
    private long numOfGlobalQueues;

    private long throughput;
    private long memoryUsage;

    private int messagesReceivedLastHalfMin;
    private  int messagesReceivedLastFiveMin;
    private  int messagesReceivedLastHour;

    public long getNumOfTopics() {
        return numOfTopics;
    }

    public void setNumOfTopics(long numOfTopics) {
        this.numOfTopics = numOfTopics;
    }

    public long getNumOfGlobalQueues() {
        return numOfGlobalQueues;
    }

    public void setNumOfGlobalQueues(long numOfGlobalQueues) {
        this.numOfGlobalQueues = numOfGlobalQueues;
    }

    public long getThroughput() {
        return throughput;
    }

    public void setThroughput(long throughput) {
        this.throughput = throughput;
    }

    public String getZookeeperID() {
        return zookeeperID;
    }

    public void setZookeeperID(String zookeeperID) {
        this.zookeeperID = zookeeperID;
    }

    public int getMessagesReceivedLastHalfMin() {
        return messagesReceivedLastHalfMin;
    }

    public void setMessagesReceivedLastHalfMin(int messagesReceivedLastHalfMin) {
        this.messagesReceivedLastHalfMin = messagesReceivedLastHalfMin;
    }

    public int getMessagesReceivedLastFiveMin() {
        return messagesReceivedLastFiveMin;
    }

    public void setMessagesReceivedLastFiveMin(int messagesReceivedLastFiveMin) {
        this.messagesReceivedLastFiveMin = messagesReceivedLastFiveMin;
    }

    public int getMessagesReceivedLastHour() {
        return messagesReceivedLastHour;
    }

    public void setMessagesReceivedLastHour(int messagesReceivedLastHour) {
        this.messagesReceivedLastHour = messagesReceivedLastHour;
    }

    public long getMemoryUsage() {
        return memoryUsage;
    }

    public void setMemoryUsage(long memoryUsage) {
        this.memoryUsage = memoryUsage;
    }

    public String getNodeId() {
        return NodeId;
    }

    public void setNodeId(String nodeId) {
        NodeId = nodeId;
    }

    private String NodeId;

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getHostName() {
        return hostName;
    }

}
