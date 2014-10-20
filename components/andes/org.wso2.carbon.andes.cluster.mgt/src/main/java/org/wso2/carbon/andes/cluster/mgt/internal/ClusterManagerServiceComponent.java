/*
 * Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 */
package org.wso2.carbon.andes.cluster.mgt.internal;


import org.wso2.carbon.andes.service.QpidService;

/**
 * @scr.component  name="org.wso2.carbon.andes.cluster.mgt.internal.ClusterManagerServiceComponent"
 *                              immediate="true"
 * @scr.reference    name="qpid.service"
 *                              interface="org.wso2.carbon.andes.service.QpidService"
 *                              cardinality="1..1"
 *                              policy="dynamic"
 *                              bind="setQpidService"
 *                              unbind="unsetQpidService"
 */
public class ClusterManagerServiceComponent {

    public void   setQpidService(QpidService service) {
        if(ClusterManagementDataHolder.getClusterManagementDataHolder().getQpidService() == null) {
            ClusterManagementDataHolder.getClusterManagementDataHolder().setQpidService(service);
        }
    }


    public void unsetQpidService(QpidService service) {

    }
}
