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
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

@Component(
        name = "org.wso2.carbon.andes.cluster.mgt.internal.ClusterManagerServiceComponent",
        immediate = true)
public class ClusterManagerServiceComponent {

    @Reference(
            name = "qpid.service",
            service = org.wso2.carbon.andes.service.QpidService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetQpidService")
    public void setQpidService(QpidService service) {

        if (ClusterManagementDataHolder.getClusterManagementDataHolder().getQpidService() == null) {
            ClusterManagementDataHolder.getClusterManagementDataHolder().setQpidService(service);
        }
    }

    public void unsetQpidService(QpidService service) {

    }
}
