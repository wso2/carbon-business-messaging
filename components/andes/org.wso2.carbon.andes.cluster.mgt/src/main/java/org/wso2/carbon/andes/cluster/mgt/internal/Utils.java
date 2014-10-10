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

import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.andes.cluster.mgt.Queue;
import java.util.ArrayList;
import java.util.List;

public class Utils {

    public static String getTenantBasedQueueName(String queueName) {
        String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        if (tenantDomain != null &&
                (!tenantDomain.equals(org.wso2.carbon.base.MultitenantConstants.SUPER_TENANT_DOMAIN_NAME))) {
            queueName = tenantDomain + "/" + queueName;
        }
        return queueName;
    }

    public static List<Queue> filterDomainSpecificQueues(List<Queue> fullList) {
        String domainName = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        ArrayList<Queue> tenantFilteredQueues = new ArrayList<Queue>();
        if(domainName != null && !CarbonContext.getThreadLocalCarbonContext().getTenantDomain().
                equals(org.wso2.carbon.base.MultitenantConstants.SUPER_TENANT_DOMAIN_NAME)) {
            for (Queue aQueue : fullList) {
                if(aQueue.getQueueName().startsWith(domainName)) {
                    tenantFilteredQueues.add(aQueue);
                }
            }
        }
        //for super tenant load all queues not specific to a domain. That means queues created by external
        //JMS clients are visible, and those names should not have "/" in their queue names
        else if(domainName != null && CarbonContext.getThreadLocalCarbonContext().getTenantDomain().
                equals(org.wso2.carbon.base.MultitenantConstants.SUPER_TENANT_DOMAIN_NAME)){
            for (Queue aQueue : fullList) {
                if(!aQueue.getQueueName().contains("/")) {
                    tenantFilteredQueues.add(aQueue);
                }
            }
        }

        return tenantFilteredQueues;
    }
}
