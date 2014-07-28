/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.andes.core;

import org.wso2.carbon.andes.core.internal.registry.SubscriptionManagementBeans;
import org.wso2.carbon.andes.core.internal.util.Utils;
import org.wso2.carbon.andes.core.types.Subscription;

import java.util.ArrayList;
import java.util.List;

public class SubscriptionManagerServiceImpl implements SubscriptionManagerService {

    @Deprecated
    //kept temporarily for back tracking purposes TODO hasithad remove after verifying
    public List<Subscription> getAllSubscriptions() throws SubscriptionManagerException {
        List<Subscription> allSubscriptions = SubscriptionManagementBeans.getInstance().getAllSubscriptions();
        //show queues belonging to current domain of user
        //also set queue name used by user
        return Utils.filterDomainSpecificSubscribers(allSubscriptions);
    }

    /**
     * show queues belonging to current domain of user
     * also set queue name used by user
    */
    public List<Subscription> getAllDurableQueueSubscriptions() throws SubscriptionManagerException {

        return Utils.filterDomainSpecificSubscribers(SubscriptionManagementBeans.getInstance().getQueueSubscriptions("true","*"));
    }

    /**
     * show non-durable queues belonging to current domain of user
     * also set queue name used by user
     */
    public List<Subscription> getAllLocalTempQueueSubscriptions() throws SubscriptionManagerException {

        return Utils.filterDomainSpecificSubscribers(SubscriptionManagementBeans.getInstance().getQueueSubscriptions("false","*"));
    }

    /**
     * show durable topics belonging to current domain of user
     * also set topic name used by user
     */
    public List<Subscription> getAllDurableTopicSubscriptions() throws SubscriptionManagerException {

        return Utils.filterDomainSpecificSubscribers(SubscriptionManagementBeans.getInstance().getTopicSubscriptions("true","*"));
    }

    /**
     * show durable topics belonging to current domain of user
     * also set topic name used by user
     * @return
     * @throws SubscriptionManagerException
     */
    public List<Subscription> getAllLocalTempTopicSubscriptions() throws SubscriptionManagerException {

        return Utils.filterDomainSpecificSubscribers(SubscriptionManagementBeans.getInstance().getTopicSubscriptions("false","*"));
    }
}
