/*
 * Copyright (c) 2016, WSO2 Inc. (http://wso2.com) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.andes.service.types;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.List;

/**
 * This class represent a cluster information object.
 */
@ApiModel(value = "Cluster Information", description = "Contains clustering related information and it's member " +
                                                       "details.")
public class ClusterInformation {
    @ApiModelProperty(value = "Whether clustering is enabled for the current node.")
    private boolean isClusteringEnabled;
    @ApiModelProperty(value = "The Node ID for the current node. Modifiable via conf/broker.xml")
    private String nodeID;
    @ApiModelProperty(value = "The coordinator node's hostname and port used for clustering. " +
                              "Example : 123.234.345.456:888")
    private String coordinatorAddress;
    @ApiModelProperty(value = "Hostname and port of all the member's in the cluster. The port refers to the " +
                              "clustering port.")
    private List<NodeInformation> nodeAddresses;

    public boolean isClusteringEnabled() {
        return isClusteringEnabled;
    }

    public void setClusteringEnabled(boolean clusteringEnabled) {
        isClusteringEnabled = clusteringEnabled;
    }

    public String getNodeID() {
        return nodeID;
    }

    public void setNodeID(String nodeID) {
        this.nodeID = nodeID;
    }

    public String getCoordinatorAddress() {
        return coordinatorAddress;
    }

    public void setCoordinatorAddress(String coordinatorAddress) {
        this.coordinatorAddress = coordinatorAddress;
    }

    public List<NodeInformation> getNodeAddresses() {
        return nodeAddresses;
    }

    public void setNodeAddresses(List<NodeInformation> nodeAddresses) {
        this.nodeAddresses = nodeAddresses;
    }
}
