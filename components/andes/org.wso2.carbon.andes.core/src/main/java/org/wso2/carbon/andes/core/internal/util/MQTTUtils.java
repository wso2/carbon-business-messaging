/*
*  Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.andes.core.internal.util;

import org.wso2.andes.kernel.AndesConstants;
import org.wso2.carbon.andes.core.types.Subscription;
import org.wso2.carbon.context.CarbonContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides common utilities for UI related functions and services.
 */
public class MQTTUtils {
    /**
     * Filters the domain specific subscriptions from a list of subscriptions
     *
     * @param allSubscriptions input subscription list
     * @return filtered list of {@link org.wso2.carbon.andes.core.types.Subscription}
     */
    public static List<Subscription> filterDomainSpecificSubscribers(
            List<Subscription> allSubscriptions, String tenantDomain) {
        ArrayList<Subscription> tenantFilteredSubscriptions = new ArrayList<>();

        //filter subscriptions belonging to the tenant domain
        if (tenantDomain != null && !CarbonContext.getThreadLocalCarbonContext().getTenantDomain().
                equals(org.wso2.carbon.base.MultitenantConstants.SUPER_TENANT_DOMAIN_NAME)) {
            for (Subscription subscription : allSubscriptions) {

                //for queues filter by queue name queueName=<tenantDomain>/queueName
                //for temp topics filter by topic name topicName=<tenantDomain>/topicName
                //for durable topic subs filter by topic name topicName=<tenantDomain>/topicName
                if (subscription.getSubscribedQueueOrTopicName().startsWith(tenantDomain + AndesConstants.TENANT_SEPARATOR)) {
                    tenantFilteredSubscriptions.add(subscription);
                }
            }
            //super tenant domain queue should have '/'
        } else if (tenantDomain != null && CarbonContext.getThreadLocalCarbonContext().getTenantDomain().
                equals(org.wso2.carbon.base.MultitenantConstants.SUPER_TENANT_DOMAIN_NAME)) {
            for (Subscription subscription : allSubscriptions) {
                if (subscription.getSubscribedQueueOrTopicName().contains(AndesConstants.TENANT_SEPARATOR)) {
                    tenantFilteredSubscriptions.add(subscription);
                }
            }
        }
        return tenantFilteredSubscriptions;
    }
}
