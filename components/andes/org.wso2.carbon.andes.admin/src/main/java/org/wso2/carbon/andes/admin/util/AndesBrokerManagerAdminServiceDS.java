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
package org.wso2.carbon.andes.admin.util;

import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.andes.authentication.service.AuthenticationService;
import org.wso2.carbon.andes.core.QueueManagerService;
import org.wso2.carbon.andes.core.SubscriptionManagerService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * this class is used to get the QueueMangerInterface service. it is used to send the
 * requests received from the Admin service to real cep engine
 */
@Component(
        name = "AndesQueueManagerAdmin.component",
        immediate = true)
public class AndesBrokerManagerAdminServiceDS {

    @Activate
    protected void activate(ComponentContext context) {

    }

    @Reference(
            name = "QueueManagerService.component",
            service = org.wso2.carbon.andes.core.QueueManagerService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unSetQueueManagerService")
    protected void setQueueManagerService(QueueManagerService cepService) {

        AndesBrokerManagerAdminServiceDSHolder.getInstance().registerQueueManagerService(cepService);
    }

    protected void unSetQueueManagerService(QueueManagerService cepService) {

        AndesBrokerManagerAdminServiceDSHolder.getInstance().unRegisterQueueManagerService(cepService);
    }

    @Reference(
            name = "SubscriptionManagerService.component",
            service = org.wso2.carbon.andes.core.SubscriptionManagerService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unSetSubscriptionManagerService")
    protected void setSubscriptionManagerService(SubscriptionManagerService subscriptionManagerService) {

        AndesBrokerManagerAdminServiceDSHolder.getInstance().registerSubscriptionManagerService
                (subscriptionManagerService);
    }

    protected void unSetSubscriptionManagerService(SubscriptionManagerService subscriptionManagerService) {

        AndesBrokerManagerAdminServiceDSHolder.getInstance().unRegisterSubscriptionManagerService
                (subscriptionManagerService);
    }

    @Reference(
            name = "org.wso2.carbon.andes.authentication.service.AuthenticationService",
            service = org.wso2.carbon.andes.authentication.service.AuthenticationService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetAccessKey")
    protected void setAccessKey(AuthenticationService authenticationService) {

        AndesBrokerManagerAdminServiceDSHolder.getInstance().setAccessKey(authenticationService.getAccessKey());
    }

    protected void unsetAccessKey(AuthenticationService authenticationService) {

        AndesBrokerManagerAdminServiceDSHolder.getInstance().setAccessKey(null);
    }
}
