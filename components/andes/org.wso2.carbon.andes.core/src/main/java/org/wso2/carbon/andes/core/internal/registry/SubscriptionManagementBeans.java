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
package org.wso2.carbon.andes.core.internal.registry;

import org.wso2.carbon.andes.core.SubscriptionManagerException;
import org.wso2.carbon.andes.core.internal.util.QueueManagementConstants;
import org.wso2.carbon.andes.core.internal.util.SubscriptionManagementConstants;
import org.wso2.carbon.andes.core.internal.util.Utils;
import org.wso2.carbon.andes.core.types.Subscription;

import javax.management.*;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;

public class SubscriptionManagementBeans {

    public static SubscriptionManagementBeans self = new SubscriptionManagementBeans();

    public static SubscriptionManagementBeans getInstance(){
        
        if(self == null){
            self = new SubscriptionManagementBeans();
        }
        return self;
    }

    /***
     * This method invokes the SubscriptionManagementInformationMBean initialized by andes component to gather information on topic subscriptions.
     * @param isDurable filter subscriptions for durable topics
     * @param isActive  filter only active subscriptions
     * @return ArrayList\<Subscription\>
     * @throws SubscriptionManagerException
     */
    public ArrayList<Subscription> getTopicSubscriptions(String isDurable,String isActive) throws SubscriptionManagerException {
        
        ArrayList<Subscription> subscriptionDetailsList = new ArrayList<>();
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            ObjectName objectName =
                    new ObjectName("org.wso2.andes:type=SubscriptionManagementInformation," +
                            "name=SubscriptionManagementInformation");

            Object[] parameters = new Object[]{isDurable, isActive};
            String[] signature = new String[]{String.class.getName(), String.class.getName()};

            Object result = mBeanServer.invoke(objectName,
                    SubscriptionManagementConstants.TOPIC_SUBSCRIPTIONS_MBEAN_ATTRIBUTE,
                    parameters, signature);

            if (result != null) {
                String[] subscriptionInformationList = (String[]) result;

                for (String subscriptionInfo : subscriptionInformationList) {
                    Subscription sub = Utils.parseStringToASubscription(subscriptionInfo);
                    subscriptionDetailsList.add(sub);
                }

            }
            return subscriptionDetailsList;

        //could catch all these exceptions in one block if we use Java 7
        } catch (MalformedObjectNameException | InstanceNotFoundException | MBeanException | ReflectionException e) {
            throw new SubscriptionManagerException("Error while invoking mBean operations to get " +
                    "subscription list", e);
        }
	}


    /***
     * This method invokes the SubscriptionManagementInformationMBean initialized by andes component to gather information on queue subscriptions.
     * @param isDurable filter subscriptions for durable queues
     * @param isActive filter only active subscriptions
     * @return ArrayList\<Subscription\>
     * @throws SubscriptionManagerException
     */
    public ArrayList<Subscription> getQueueSubscriptions(String isDurable,String isActive) throws SubscriptionManagerException {
        
        ArrayList<Subscription> subscriptionDetailsList = new ArrayList<>();
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            ObjectName objectName = new ObjectName("org.wso2.andes:type=SubscriptionManagementInformation," +
                    "name=SubscriptionManagementInformation");

            Object[] parameters = new Object[]{isDurable, isActive};
            String[] signature = new String[]{String.class.getName(), String.class.getName()};

            Object result = mBeanServer.invoke(objectName,
                    SubscriptionManagementConstants.QUEUE_SUBSCRIPTIONS_MBEAN_ATTRIBUTE,
                    parameters, signature);

            if (result != null) {
                String[] subscriptionInformationList = (String[]) result;

                for (String subscriptionInfo : subscriptionInformationList) {
                    Subscription sub = Utils.parseStringToASubscription(subscriptionInfo);
                    subscriptionDetailsList.add(sub);
                }

            }
            return subscriptionDetailsList;

        //could catch all these exceptions in one block if we use Java 7
        } catch (MalformedObjectNameException | ReflectionException | MBeanException | InstanceNotFoundException e) {
            throw new SubscriptionManagerException("Cannot access mBean operations to get " +
                    "subscription list", e);
        }
	}

    @Deprecated
    //Replaced by seperate mbean services for topics and queues
    public ArrayList<Subscription> getAllSubscriptions() throws SubscriptionManagerException {

        ArrayList<Subscription> subscriptionDetailsList = new ArrayList<>();
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            ObjectName objectName = new ObjectName("org.wso2.andes:type=QueueManagementInformation," +
                    "name=QueueManagementInformation");
            Object result = mBeanServer.getAttribute(objectName,
                    QueueManagementConstants.BROKER_SUBSCRIPTION_ATTRIBUTE);

            if (result != null) {
                String[] subscriptionInformationList = (String[]) result;

                for (String subscriptionInfo : subscriptionInformationList) {
                    Subscription sub = Utils.parseStringToASubscription(subscriptionInfo);
                    subscriptionDetailsList.add(sub);
                }

            }
            return subscriptionDetailsList;

        //could catch all these exceptions in one block if we use Java 7
        } catch (MalformedObjectNameException | ReflectionException | MBeanException | InstanceNotFoundException
				| AttributeNotFoundException e) {
            throw new SubscriptionManagerException("Cannot access mBean operations to get " +
                    "subscription list", e);
        }
	}

	/**
	 * This method invokes the SubscriptionManagementInformationMBean initialized by andes component to close the
	 * subscription forcibly.
	 *
	 * @param subscriptionID ID of the subscription to close
	 * @param destination queue/topic name of subscribed destination
	 */
	public void closeSubscription(String subscriptionID, String destination) throws SubscriptionManagerException {
		MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
		try {
			ObjectName objectName = new ObjectName("org.wso2.andes:type=SubscriptionManagementInformation," +
					"name=SubscriptionManagementInformation");

			Object[] parameters = new Object[]{subscriptionID, destination};
			String[] signature = new String[]{String.class.getName(), String.class.getName()};

			mBeanServer.invoke(objectName,
					SubscriptionManagementConstants.SUBSCRIPTION_CLOSE_MBEAN_ATTRIBUTE,
					parameters, signature);

			//could catch all these exceptions in one block if we use Java 7
		} catch (MalformedObjectNameException | ReflectionException | MBeanException | InstanceNotFoundException e) {
			throw new SubscriptionManagerException("Cannot access mBean operations to get " +
					"subscription list", e);
		}
	}
}
