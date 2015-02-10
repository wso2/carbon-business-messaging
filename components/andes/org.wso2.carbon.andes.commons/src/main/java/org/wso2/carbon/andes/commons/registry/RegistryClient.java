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

package org.wso2.carbon.andes.commons.registry;

import org.apache.axis2.databinding.utils.ConverterUtil;
import org.wso2.carbon.andes.commons.CommonsUtil;
import org.wso2.carbon.andes.commons.QueueDetails;
import org.wso2.carbon.andes.commons.SubscriptionDetails;
import org.wso2.carbon.andes.commons.internal.CommonsDataHolder;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.registry.core.Collection;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.util.Calendar;
import java.util.Date;

/**
 * This class wraps Registry API for the Andes component
 */
public class RegistryClient {

    private static final String OWNER = "Owner";
    private static final String NAME = "Name";
    private static final String CREATED_TIME = "createdTime";
    private static final String UPDATED_TIME = "updatedTime";
    private static final String CREATED_FROM = "createdFrom";
    private static final String CREATED_FROM_AMQP = "amqp";
    private static final String USER_COUNT = "userCount";

    /**
     * Create an entry for a queue in the Registry
     *
     * @param queueName Name of the queue
     * @param owner     Who creates the queue
     * @throws RegistryClientException
     */
    public static void createQueue(String queueName, String owner)
            throws RegistryClientException {
        try {

            RegistryService registryService = CommonsDataHolder.getInstance().getRegistryService();
            UserRegistry registry = registryService.getGovernanceSystemRegistry(
                    PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId() <= 0 ?
                    MultitenantConstants.SUPER_TENANT_ID :
                    PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId()
            );

            // Set queue properties
            Collection queue;
            String queueID = CommonsUtil.getQueueID(queueName);

            if (!registry.resourceExists(queueID)) { // Create queue
                queue = registry.newCollection();

                queue.setProperty(OWNER, owner);
                queue.setProperty(NAME, queueName);
                queue.setProperty(CREATED_TIME, ConverterUtil.convertToString(
                        Calendar.getInstance()));
                queue.setProperty(UPDATED_TIME, ConverterUtil.convertToString(
                        Calendar.getInstance()));
                queue.setProperty(CREATED_FROM, CREATED_FROM_AMQP);
                queue.setProperty(USER_COUNT, "1");
            } else { // Share queue
                queue = (Collection) registry.get(queueID);

                queue.setProperty(UPDATED_TIME, ConverterUtil.convertToString(
                        Calendar.getInstance()));

                String userCount = queue.getProperty(USER_COUNT);
                if (null != userCount) {
                    int count = Integer.parseInt(userCount);
                    queue.setProperty(USER_COUNT, Integer.toString(++count));
                }
            }

            registry.put(queueID, queue);
        } catch (RegistryException e) {
            throw new RegistryClientException(e);
        }
    }

    /**
     * Delete the entry for a queue from the Registry
     *
     * @param queueName Name of the queue to be deleted
     * @throws RegistryClientException
     */
    public static void deleteQueue(String queueName)
            throws RegistryClientException {
        try {
            RegistryService registryService = CommonsDataHolder.getInstance().getRegistryService();
            if (registryService != null) {
                UserRegistry registry = registryService.getGovernanceSystemRegistry();

                // Delete queue
                String queueID = CommonsUtil.getQueueID(queueName);

                if (registry.resourceExists(queueID)) {
                    String createdFrom = registry.get(queueID).getProperty(CREATED_FROM);

                    if ((null != createdFrom) && (CREATED_FROM_AMQP.equals(createdFrom))) {
                        Collection queue = (Collection) registry.get(queueID);

                        String userCount = queue.getProperty(USER_COUNT);
                        if (null != userCount) {
                            int count = Integer.parseInt(userCount);

                            if (count > 1) {
                                queue.setProperty(USER_COUNT, Integer.toString(--count));
                                registry.put(queueID, queue);
                            } else {
                                registry.delete(queueID);
                            }
                        }
                    }
                }
            }

        } catch (RegistryException e) {
            throw new RegistryClientException(e);
        }
    }

    /**
     * Get queues saved in the Registry
     *
     * @return Array of queues
     * @throws RegistryClientException
     */
    public static QueueDetails[] getQueues()
            throws RegistryClientException {
        try {
            QueueDetails[] queueDetailsArray = new QueueDetails[0];

            RegistryService registryService = CommonsDataHolder.getInstance().getRegistryService();
            UserRegistry registry = registryService.getGovernanceSystemRegistry(
                    CarbonContext.getThreadLocalCarbonContext().getTenantId() <= 0 ?
                    MultitenantConstants.SUPER_TENANT_ID :
                    CarbonContext.getThreadLocalCarbonContext().getTenantId()
            );

            // Get queues
            String queuesID = CommonsUtil.getQueuesID();
            if (registry.resourceExists(queuesID)) {
                Collection queueCollection = (Collection) registry.get(queuesID);
                queueDetailsArray = new QueueDetails[queueCollection.getChildCount()];

                int index = 0;
                for (String queueId : queueCollection.getChildren()) {
                    Collection queue = (Collection) registry.get(queueId);

                    QueueDetails queueDetails = new QueueDetails();
                    queueDetails.setName(queue.getProperty(NAME));
                    queueDetails.setOwner(queue.getProperty(OWNER));
                    queueDetails.setCreatedTime(queue.getProperty(CREATED_TIME));

                    queueDetailsArray[index++] = queueDetails;
                }
            }

            return queueDetailsArray;
        } catch (RegistryException e) {
            throw new RegistryClientException(e);
        }
    }

    /**
     * Create an entry for a topic subscription in the Registry
     *
     * @param topic            Topic name
     * @param subscriptionName Queue name used for the subscription
     * @param owner            Who creates the subscription
     * @throws RegistryClientException
     */
    public static void createSubscription(
            String topic, String subscriptionName, String owner)
            throws RegistryClientException {
        try {


            RegistryService registryService = CommonsDataHolder.getInstance().getRegistryService();
            UserRegistry registry = registryService.getGovernanceSystemRegistry(
                    CarbonContext.getThreadLocalCarbonContext().getTenantId() <= 0 ?
                    MultitenantConstants.SUPER_TENANT_ID :
                    CarbonContext.getThreadLocalCarbonContext().getTenantId()
            );

            // Add new subscription and set properties
            Collection subscription;
            String tenantBasedTopicName = getTenantBasedTopicName(topic);
            String subscriptionID = CommonsUtil.getSubscriptionID(tenantBasedTopicName, subscriptionName);

            if (!registry.resourceExists(subscriptionID)) {
                subscription = registry.newCollection();
            } else {
                subscription = (Collection) registry.get(subscriptionID);
            }

            subscription.setProperty(OWNER, owner);
            subscription.setProperty(NAME, subscriptionName);
            subscription.setProperty(CREATED_TIME, ConverterUtil.convertToString(new Date()));

            registry.put(subscriptionID, subscription);
        } catch (RegistryException e) {
            throw new RegistryClientException(e);
        }
    }

    /**
     * Gets tenant based topic name
     * @param topicName the topic name
     * @return tenant based topic name
     */
    public static String getTenantBasedTopicName(String topicName) {
        String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        String tenantBasedTopicName = topicName;
        if (tenantDomain != null && (!tenantDomain.equals(
                org.wso2.carbon.base.MultitenantConstants.SUPER_TENANT_DOMAIN_NAME))) {
            String formattedTenantDomain = tenantDomain + "/";
            if (topicName.contains(formattedTenantDomain)) {
                tenantBasedTopicName = topicName.substring(formattedTenantDomain.length());
            }
        }
        return tenantBasedTopicName;
    }


    /**
     * Delete the entry for a subscription from the Registry
     *
     * @param topic            Name of the topic that the subscription is made to
     * @param subscriptionName Name of the queue used for the subscription
     * @throws RegistryClientException
     */
    public static void deleteSubscription(String topic, String subscriptionName)
            throws RegistryClientException {
        try {
            RegistryService registryService = CommonsDataHolder.getInstance().getRegistryService();
            if (registryService != null) {
                UserRegistry registry = registryService.getGovernanceSystemRegistry(
                        CarbonContext.getThreadLocalCarbonContext().getTenantId() <= 0 ?
                        MultitenantConstants.SUPER_TENANT_ID :
                        CarbonContext.getThreadLocalCarbonContext().getTenantId()
                );

                // Delete subscription
                String subscriptionID = CommonsUtil.getSubscriptionID(topic, subscriptionName);
                if (registry.resourceExists(subscriptionID)) {
                    registry.delete(subscriptionID);
                }
            }
        } catch (RegistryException e) {
            throw new RegistryClientException(e);
        }
    }

    /**
     * Get subscriptions saved in the Registry
     *
     * @param topic Name of the topic
     * @return Array of subscriptions
     * @throws RegistryClientException
     */
    public static SubscriptionDetails[] getSubscriptions(String topic)
            throws RegistryClientException {
        try {
            SubscriptionDetails[] subscriptionDetailsArray = new SubscriptionDetails[0];

            RegistryService registryService = CommonsDataHolder.getInstance().getRegistryService();
            UserRegistry registry = registryService.getGovernanceSystemRegistry(
                    CarbonContext.getThreadLocalCarbonContext().getTenantId() <= 0 ?
                    MultitenantConstants.SUPER_TENANT_ID :
                    CarbonContext.getThreadLocalCarbonContext().getTenantId()
            );

            // Get subscriptions
            String subscriptionsID = CommonsUtil.getSubscriptionsID(topic);
            if (registry.resourceExists(subscriptionsID)) {
                Collection subscriptionCollection = (Collection) registry.get(subscriptionsID);
                subscriptionDetailsArray =
                        new SubscriptionDetails[subscriptionCollection.getChildCount()];

                int index = 0;
                for (String subs : subscriptionCollection.getChildren()) {
                    Collection subscription = (Collection) registry.get(subs);

                    SubscriptionDetails subscriptionDetails = new SubscriptionDetails();
                    subscriptionDetails.setName(subscription.getProperty(NAME));
                    subscriptionDetails.setOwner(subscription.getProperty(OWNER));
                    subscriptionDetails.setCreatedTime(subscription.getProperty(CREATED_TIME));

                    subscriptionDetailsArray[index++] = subscriptionDetails;
                }
            }

            return subscriptionDetailsArray;
        } catch (RegistryException e) {
            throw new RegistryClientException(e);
        }
    }
}
