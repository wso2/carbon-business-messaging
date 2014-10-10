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

import org.wso2.carbon.andes.core.QueueManagerService;
import org.wso2.carbon.andes.core.SubscriptionManagerService;

public class AndesBrokerManagerAdminServiceDSHolder {
    private String accessKey;
    private QueueManagerService queueManagerService;
    private SubscriptionManagerService subscriptionManagerService;

    private static AndesBrokerManagerAdminServiceDSHolder instance = new AndesBrokerManagerAdminServiceDSHolder();


    public static AndesBrokerManagerAdminServiceDSHolder getInstance(){
        return instance;
    }

    public QueueManagerService getQueueManagerService(){
        return this.queueManagerService;
    }

    public SubscriptionManagerService getSubscriptionManagerService() {
        return this.subscriptionManagerService;
    }

    public void registerQueueManagerService(QueueManagerService cepService){
        this.queueManagerService = cepService;
    }

     public void unRegisterQueueManagerService(QueueManagerService cepService){
        this.queueManagerService = null;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey =  accessKey;
    }

    public String getAccessKey() {
        return this.accessKey;
    }

    public void registerSubscriptionManagerService(SubscriptionManagerService subscriptionManagerService) {
        this.subscriptionManagerService = subscriptionManagerService;
    }

    public void unRegisterSubscriptionManagerService(SubscriptionManagerService subscriptionManagerService) {
        this.subscriptionManagerService = null;
    }
}
