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

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

public class SubscriptionManagementBeans {

    public static SubscriptionManagementBeans self = new SubscriptionManagementBeans();

    public static SubscriptionManagementBeans getInstance(){
        
        if(self == null){
            self = new SubscriptionManagementBeans();
        }
        return self;
    }

    /***
     * This method invokes the SubscriptionManagementInformationMBean initialized by andes component
     * to gather information on subscriptions.
     *
     * @param isDurable filter subscriptions for durable topics
     * @param isActive  filter only active subscriptions
     * @param protocolType The protocol type of the subscriptions
     * @param destinationType The destination type of the subscriptions
     * @return ArrayList\<Subscription\>
     * @throws SubscriptionManagerException
     */
    public ArrayList<Subscription> getSubscriptions(String isDurable, String isActive,
                                                    String protocolType, String destinationType)
            throws SubscriptionManagerException {
        
        ArrayList<Subscription> subscriptionDetailsList = new ArrayList<>();
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            ObjectName objectName =
                    new ObjectName("org.wso2.andes:type=SubscriptionManagementInformation," +
                            "name=SubscriptionManagementInformation");

            Object[] parameters = new Object[]{isDurable, isActive, protocolType, destinationType};
            String[] signature = new String[]{String.class.getName(), String.class.getName(),
                    String.class.getName(), String.class.getName()};

            Object result = mBeanServer.invoke(objectName,
                    SubscriptionManagementConstants.SUBSCRIPTIONS_MBEAN_ATTRIBUTE,
                    parameters, signature);

            if (result != null) {
                String[] subscriptionInformationList = (String[]) result;

                for (String subscriptionInfo : subscriptionInformationList) {
                    Subscription sub = Utils.parseStringToASubscription(subscriptionInfo);
                    subscriptionDetailsList.add(sub);
                }

            }
            return subscriptionDetailsList;

        } catch (MalformedObjectNameException | InstanceNotFoundException | MBeanException | ReflectionException e) {
            throw new SubscriptionManagerException("Error while invoking mBean operations to get "
                                                   + "subscription list", e);
        }
    }

    /**
     * This method invokes the SubscriptionManagementInformationMBean initialized by andes component
     * to get pending message ccount of subscriber
     *
     * @param subscriptionId  Id of the subscriber
     * @param isDurable       filter subscriptions for durable topics
     * @param isActive        filter only active subscriptions
     * @param protocolType    The protocol type of the subscriptions
     * @param destinationType The destination type of the subscriptions
     * @return ArrayList\<Subscription\>
     * @throws SubscriptionManagerException
     */
    public long getPendingMessageCount(String subscriptionId, String isDurable, String isActive,
                                      String protocolType, String destinationType)
            throws SubscriptionManagerException {

        long pendingMessageCount = 0;
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            ObjectName objectName =
                    new ObjectName("org.wso2.andes:type=SubscriptionManagementInformation,"
                                   + "name=SubscriptionManagementInformation");

            Object[] parameters = new Object[]{subscriptionId, isDurable, isActive, protocolType, destinationType};
            String[] signature = new String[]{String.class.getName(), String.class.getName(), String.class.getName(),
                    String.class.getName(), String.class.getName()};

            Object result = mBeanServer.invoke(objectName,
                    SubscriptionManagementConstants.PENDING_MESSAGE_COUNT_MBEAN_ATTRIBUTE,
                    parameters, signature);

            if (result != null) {
                pendingMessageCount = (long) result;

            }
            return pendingMessageCount;

        } catch (MalformedObjectNameException | InstanceNotFoundException | MBeanException | ReflectionException e) {
            throw new SubscriptionManagerException("Error while invoking mBean operations to get "
                                                   + "pending message count", e);
        }
    }

    /**
     * This method invokes the SubscriptionManagementInformationMBean initialized by andes component
     * to retrieve subscriptions matching to the given search criteria
     *
     * @param isDurable                         retrieve durable subscriptions (can be true/false)
     * @param isActive                          retrieve active subscriptions (can be true/false/*, * meaning any)
     * @param protocolType                      the protocol type of the subscriptions to retrieve
     * @param destinationType                   the destination type of the subscriptions to retrieve
     * @param filteredNamePattern               queue or topic name pattern to search the subscriptions (* for all)
     * @param identifierPattern                 identifier pattern to search the subscriptions (* for all)
     * @param ownNodeId                         node Id of the node which own the subscriptions
     * @param pageNumber                        page number in the pagination table
     * @param subscriptionCountPerPage          number of subscriptions to be shown in the UI per page
     * @param isFilteredNameByExactMatch        exactly match the name or not
     * @param isIdentifierPatternByExactMatch   exactly match the identifier or not
     * @return list of subscriptions matching to the search criteria
     * @throws SubscriptionManagerException throws when an error occurs
     */
    public ArrayList<Subscription> getFilteredSubscriptions(String isDurable, String isActive,
            String protocolType, String destinationType, String filteredNamePattern, String identifierPattern,
            String ownNodeId, int pageNumber, int subscriptionCountPerPage, boolean isFilteredNameByExactMatch,
            boolean isIdentifierPatternByExactMatch) throws SubscriptionManagerException {

        ArrayList<Subscription> subscriptionDetailsList = new ArrayList<>();
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            ObjectName objectName =
                    new ObjectName("org.wso2.andes:type=SubscriptionManagementInformation,"
                                      + "name=SubscriptionManagementInformation");

            Object[] parameters = new Object[]{isDurable, isActive, protocolType, destinationType,
                    filteredNamePattern, identifierPattern, ownNodeId, pageNumber, subscriptionCountPerPage,
                                               isFilteredNameByExactMatch, isIdentifierPatternByExactMatch};
            String[] signature = new String[]{String.class.getName(), String.class.getName(),
                    String.class.getName(), String.class.getName(), String.class.getName(), String.class.getName(),
                    String.class.getName(),  int.class.getName(), int.class.getName(), boolean.class.getName(),
                                              boolean.class.getName()};

            Object result = mBeanServer.invoke(objectName,
                    SubscriptionManagementConstants.FILTERED_SUBSCRIPTIONS_MBEAN_ATTRIBUTE,
                    parameters, signature);

            if (result != null) {
                String[] subscriptionInformationList = (String[]) result;

                for (String subscriptionInfo : subscriptionInformationList) {
                    Subscription sub = Utils.parseStringToASubscription(subscriptionInfo);
                    subscriptionDetailsList.add(sub);
                }

            }
            return subscriptionDetailsList;

        } catch (MalformedObjectNameException | InstanceNotFoundException | MBeanException | ReflectionException e) {
            throw new SubscriptionManagerException("Error while invoking mBean operations to get subscription list", e);
        }

    }

    /**
     * This method invokes the SubscriptionManagementInformationMBean initialized by andes component
     * to retrieve total subscription count matching to the given search criteria
     *
     * @param isDurable                         retrieve durable subscriptions (can be true/false)
     * @param isActive                          retrieve active subscriptions (can be true/false/*, * meaning any)
     * @param protocolType                      the protocol type of the subscriptions to retrieve
     * @param destinationType                   the destination type of the subscriptions to retrieve
     * @param filteredNamePattern               queue or topic name pattern to search the subscriptions (* for all)
     * @param identifierPattern                 identifier pattern to search the subscriptions (* for all)
     * @param ownNodeId                         node Id of the node which own the subscriptions
     * @param isFilteredNameByExactMatch        exactly match the name or not
     * @param isIdentifierPatternByExactMatch   exactly match the identifier or not
     * @return total subscription count matching to the given criteria
     * @throws SubscriptionManagerException throws when an error occurs
     */
    public int getTotalSubscriptionCountForSearchResult(String isDurable, String isActive, String
            protocolType, String destinationType, String filteredNamePattern, String identifierPattern,
            String ownNodeId, boolean isFilteredNameByExactMatch,
            boolean isIdentifierPatternByExactMatch) throws SubscriptionManagerException {

        int totalSubscriptionCount = 0;
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            ObjectName objectName =
                    new ObjectName("org.wso2.andes:type=SubscriptionManagementInformation,"
                                   + "name=SubscriptionManagementInformation");

            Object[] parameters = new Object[]{isDurable, isActive, protocolType, destinationType,
                    filteredNamePattern, identifierPattern, ownNodeId, isFilteredNameByExactMatch, isIdentifierPatternByExactMatch};
            String[] signature = new String[]{String.class.getName(), String.class.getName(),
                    String.class.getName(), String.class.getName(), String.class.getName(), String.class.getName(),
                    String.class.getName(), boolean.class.getName(), boolean.class.getName()};

            Object result = mBeanServer.invoke(objectName,
                    SubscriptionManagementConstants.FILTERED_SUBSCRIPTION_COUNT_MBEAN_ATTRIBUTE,
                    parameters, signature);

            if (result != null) {
                totalSubscriptionCount = (int) result;
            }

        } catch (MalformedObjectNameException | InstanceNotFoundException | MBeanException | ReflectionException e) {
            throw new SubscriptionManagerException("Error while invoking mBean operations to get subscription list", e);
        }
        return totalSubscriptionCount;

    }


    @Deprecated
    //Replaced by separate mbean services for topics and queues
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
	public void closeSubscription(String subscriptionID, String destination, String protocolType,
                                  String destinationType) throws SubscriptionManagerException {
		MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
		try {
			ObjectName objectName = new ObjectName("org.wso2.andes:type=SubscriptionManagementInformation," +
					"name=SubscriptionManagementInformation");

			Object[] parameters = new Object[]{subscriptionID, destination, protocolType, destinationType};
			String[] signature = new String[]{String.class.getName(), String.class.getName(), String.class.getName(),
                    String.class.getName()};

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
