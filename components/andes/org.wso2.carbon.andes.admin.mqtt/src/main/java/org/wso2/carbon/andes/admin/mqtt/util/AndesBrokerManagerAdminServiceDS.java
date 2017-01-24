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
package org.wso2.carbon.andes.admin.mqtt.util;

import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.andes.authentication.service.AuthenticationService;
import org.wso2.carbon.andes.core.QueueManagerService;
import org.wso2.carbon.andes.core.SubscriptionManagerService;

/**
 * this class is used to get the QueueMangerInterface service. it is used to send the
 * requests received from the Admin service to real cep engine
 *
 * @scr.component name="AndesQueueManagerAdminMQTT.component" immediate="true"
 * @scr.reference name="org.wso2.carbon.andes.authentication.service.AuthenticationService"
 * interface="org.wso2.carbon.andes.authentication.service.AuthenticationService"
 * cardinality="1..1"
 * policy="dynamic"
 * bind="setAccessKey"
 * unbind="unsetAccessKey"
 */

public class AndesBrokerManagerAdminServiceDS {

    protected void activate(ComponentContext context) {

    }

    protected void setTopicManagerService(QueueManagerService cepService) {
        AndesBrokerManagerAdminServiceDSHolder.getInstance().registerQueueManagerService(cepService);
    }

    protected void unSetTopoicManagerService(QueueManagerService cepService) {
        AndesBrokerManagerAdminServiceDSHolder.getInstance().unRegisterQueueManagerService(cepService);
    }

    protected void setSubscriptionManagerService(SubscriptionManagerService subscriptionManagerService) {
        AndesBrokerManagerAdminServiceDSHolder.getInstance().registerSubscriptionManagerService
                (subscriptionManagerService);
    }

    protected void unSetSubscriptionManagerService(SubscriptionManagerService subscriptionManagerService) {
        AndesBrokerManagerAdminServiceDSHolder.getInstance().unRegisterSubscriptionManagerService
                (subscriptionManagerService);
    }


    protected void setAccessKey(AuthenticationService authenticationService) {
        AndesBrokerManagerAdminServiceDSHolder.getInstance().setAccessKey(authenticationService.getAccessKey());
    }

    protected void unsetAccessKey(AuthenticationService authenticationService) {
        AndesBrokerManagerAdminServiceDSHolder.getInstance().setAccessKey(null);
    }
}
