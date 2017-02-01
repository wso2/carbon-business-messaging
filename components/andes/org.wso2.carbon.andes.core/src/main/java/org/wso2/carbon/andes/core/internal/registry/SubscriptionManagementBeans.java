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

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;

public class SubscriptionManagementBeans {

    private static SubscriptionManagementBeans self = new SubscriptionManagementBeans();

    public static SubscriptionManagementBeans getInstance() {

        if (self == null) {
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
            throw new SubscriptionManagerException("Error while invoking mBean operations to get " +
                    "subscription list", e);
        }
	}

    /**
     * Get the pending message count for the specified subscription.
     *
     * @param queueName subscription identifier for which the pending message cound need to be calculated
     * @return pending message count for that subscription
     * @throws SubscriptionManagerException
     */
    public long getPendingMessageCount(String queueName) throws SubscriptionManagerException{

        long messageCount = 0;

        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            ObjectName objectName =
                    new ObjectName("org.wso2.andes:type=SubscriptionManagementInformation,"
                            + "name=SubscriptionManagementInformation");

            Object[] parameters = new Object[]{queueName};
            String[] signature = new String[]{String.class.getName()};

            Object result = mBeanServer.invoke(objectName,
                    SubscriptionManagementConstants.PENDING_MESSAGE_COUNT_MBEAN_ATTRIBUTE,
                    parameters, signature);

            if (null != result) {
                messageCount = (long) result;
            }
            return messageCount;

        } catch (MalformedObjectNameException | InstanceNotFoundException | MBeanException | ReflectionException e) {
            throw new SubscriptionManagerException("Error while invoking mBean operations to get "
                    + "message count for a subscription", e);
        }
    }

    /**
     * This method invokes the SubscriptionManagementInformationMBean initialized by andes component
     * to retrieve subscriptions matching to the given search criteria.
     *
     * @param isDurable  are the subscriptions to be retrieve durable (true/ false)
     * @param isActive   are the subscriptions to be retrieved active (true/false)
     * @param protocolType  the protocol type of the subscriptions to be retrieved
     * @param destinationType the destination type of the subscriptions to be retrieved
     * @param filteredNamePattern queue or topic name pattern to search the subscriptions ("" for all)
     * @param isFilteredNameByExactMatch exactly match the name or not
     * @param identifierPattern  identifier pattern to search the subscriptions ("" for all)
     * @param isIdentifierPatternByExactMatch  exactly match the identifier or not
     * @param ownNodeId node Id of the node which own the subscriptions
     * @param pageNumber  page number in the pagination table
     * @param subscriptionCountPerPage number of subscriptions to be shown in the UI per page
     * @return list of subscriptions matching to the search criteria
     * @throws SubscriptionManagerException throws when an error occurs
     */
    public ArrayList<Subscription> getFilteredSubscriptions(boolean isDurable, boolean isActive,
                                                            String protocolType, String destinationType,
                                                            String filteredNamePattern,
                                                            boolean isFilteredNameByExactMatch, String identifierPattern,
                                                            boolean isIdentifierPatternByExactMatch, String ownNodeId,
                                                            int pageNumber, int subscriptionCountPerPage)
                                                            throws SubscriptionManagerException {
        ArrayList<Subscription> subscriptionDetailsList = new ArrayList<>();
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            ObjectName objectName =
                    new ObjectName("org.wso2.andes:type=SubscriptionManagementInformation,"
                            + "name=SubscriptionManagementInformation");

            Object[] parameters = new Object[]{isDurable, isActive, protocolType, destinationType,
                    filteredNamePattern, isFilteredNameByExactMatch,identifierPattern,
                    isIdentifierPatternByExactMatch, ownNodeId, pageNumber,
                    subscriptionCountPerPage};
            String[] signature = new String[]{boolean.class.getName(), boolean.class.getName(),
                    String.class.getName(), String.class.getName(), String.class.getName(), boolean.class.getName(),
                    String.class.getName(), boolean.class.getName(), String.class.getName(),  int.class.getName(),
                    int.class.getName()};

            Object result = mBeanServer.invoke(objectName,
                    SubscriptionManagementConstants.FILTERED_SUBSCRIPTIONS_MBEAN_ATTRIBUTE, parameters, signature);

            if (null != result) {
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
     *
     * This method invokes the SubscriptionManagementInformationMBean initialized by andes component
     * to retrieve total subscription count matching to the given search criteria.
     *
     * @param isDurable are the subscriptions to be retrieve durable (true/ false)
     * @param isActive are the subscriptions to be retrieved active (true/false)
     * @param protocolType the protocol type of the subscriptions to be retrieved
     * @param destinationType the destination type of the subscriptions to be retrieved
     * @param filteredNamePattern queue or topic name pattern to search the subscriptions ("" for all)
     * @param isFilteredNameByExactMatch exactly match the name or not
     * @param identifierPattern identifier pattern to search the subscriptions ("" for all)
     * @param isIdentifierPatternByExactMatch exactly match the identifier or not
     * @param ownNodeId node Id of the node which own the subscriptions
     * @return total subscription count matching to the given criteria
     * @throws SubscriptionManagerException throws when an error occurs
     */
    public int getTotalSubscriptionCountForSearchResult(boolean isDurable, boolean isActive, String protocolType,
                                                        String destinationType, String filteredNamePattern,
                                                        boolean isFilteredNameByExactMatch, String identifierPattern,
                                                        boolean isIdentifierPatternByExactMatch, String ownNodeId)
                                                        throws SubscriptionManagerException {
        int totalSubscriptionCount = 0;
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        try {
            ObjectName objectName =
                    new ObjectName("org.wso2.andes:type=SubscriptionManagementInformation,"
                            + "name=SubscriptionManagementInformation");

            Object[] parameters = new Object[]{isDurable, isActive, protocolType, destinationType,
                    filteredNamePattern, isFilteredNameByExactMatch, identifierPattern,
                    isIdentifierPatternByExactMatch, ownNodeId};
            String[] signature = new String[]{boolean.class.getName(), boolean.class.getName(),
                    String.class.getName(), String.class.getName(), String.class.getName(), boolean.class.getName(),
                    String.class.getName(), boolean.class.getName(), String.class.getName()};

            Object result = mBeanServer.invoke(objectName,
                    SubscriptionManagementConstants.FILTERED_SUBSCRIPTION_COUNT_MBEAN_ATTRIBUTE, parameters, signature);

            if (null != result) {
                totalSubscriptionCount = (int) result;
            }

        } catch (MalformedObjectNameException | InstanceNotFoundException | MBeanException | ReflectionException e) {
            throw new SubscriptionManagerException("Error while invoking mBean operations to get "
                    + "subscription list", e);
        }
        return totalSubscriptionCount;

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
