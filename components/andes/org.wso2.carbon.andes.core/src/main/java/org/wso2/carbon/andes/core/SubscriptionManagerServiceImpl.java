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

    public List<Subscription> getAllSubscriptions() throws SubscriptionManagerException {
        List<Subscription> allSubscriptions = SubscriptionManagementBeans.getInstance().getAllSubscriptions();
        //show queues belonging to current domain of user
        //also set queue name used by user
        return Utils.filterDomainSpecificSubscribers(allSubscriptions);
    }

    public List<Subscription> getAllDurableQueueSubscriptions() throws SubscriptionManagerException {
        List<Subscription> allSubscriptions = getAllSubscriptions();
        List<Subscription> durableQueueSubscriptions = new ArrayList<Subscription>();
        for(Subscription sub : allSubscriptions) {
            if(sub.getSubscriberQueueBoundExchange().equals(Utils.DIRECT_EXCHANGE) && sub.isDurable()) {
                durableQueueSubscriptions.add(sub);
            }
        }
        return durableQueueSubscriptions;
    }

    public List<Subscription> getAllLocalTempQueueSubscriptions() throws SubscriptionManagerException {
        List<Subscription> allSubscriptions = getAllSubscriptions();
        List<Subscription> tempLocalQueueSubscriptions = new ArrayList<Subscription>();
        for(Subscription sub : allSubscriptions) {
            if(sub.getSubscriberQueueBoundExchange().equals(Utils.DIRECT_EXCHANGE) && !sub.isDurable()) {
                tempLocalQueueSubscriptions.add(sub);
            }
        }

        return tempLocalQueueSubscriptions;
    }

    public List<Subscription> getAllDurableTopicSubscriptions() throws SubscriptionManagerException {
        List<Subscription> allSubscriptions = getAllSubscriptions();
        List<Subscription> durableTopicSubscriptions = new ArrayList<Subscription>();
        for(Subscription sub : allSubscriptions) {
            if(sub.getSubscriberQueueBoundExchange().equals(Utils.TOPIC_EXCHANGE) && sub.isDurable()) {
                durableTopicSubscriptions.add(sub);
            }
        }
        return durableTopicSubscriptions;
    }

    public List<Subscription> getAllLocalTempTopicSubscriptions() throws SubscriptionManagerException {
        List<Subscription> allSubscriptions = getAllSubscriptions();
        List<Subscription> tempLocalTopicSubscriptions = new ArrayList<Subscription>();
        for(Subscription sub : allSubscriptions) {
            if(sub.getSubscriberQueueBoundExchange().equals(Utils.TOPIC_EXCHANGE) && !sub.isDurable()) {
                tempLocalTopicSubscriptions.add(sub);
            }
        }

        return tempLocalTopicSubscriptions;
    }
}
