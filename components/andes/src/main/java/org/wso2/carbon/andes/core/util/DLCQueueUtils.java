/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.andes.core.util;

import org.apache.log4j.Logger;
import org.wso2.carbon.andes.core.AndesConstants;
import org.wso2.carbon.andes.core.AndesException;

/**
 * This class centralises the management of Dead Letter Queues by creating Dead Letter Queues when
 * requested and deciding on whether a queue is a Dead Letter Queue or not and generating the 'Dead
 * Letter Queue' queue name for the tenant.
 */
public class DLCQueueUtils {

    private static final Logger log = Logger.getLogger(DLCQueueUtils.class);

    /**
     * Derive the Dead Letter Queue name of the tenant with respect to a given queue of the same
     * tenant.
     *
     * @param queueName A queue name in the same tenant.
     * @return The Dead Letter Queue name for the tenant.
     */
    public static String identifyTenantInformationAndGenerateDLCString(String queueName) {
        String destinationString;

        if (queueName.contains(AndesConstants.TENANT_SEPARATOR)) {
            //The Queue is in the tenant realm
            destinationString = queueName.split(AndesConstants.TENANT_SEPARATOR,
                                                2)[0] + AndesConstants.TENANT_SEPARATOR + AndesConstants
                    .DEAD_LETTER_QUEUE_SUFFIX;
        } else {
            destinationString = AndesConstants.DEAD_LETTER_QUEUE_SUFFIX;
        }

        return destinationString;
    }

    /**
     * Derive the Dead Letter Queue name of the tenant with respect to a given queue of the same
     * tenant.
     * <p/>
     * The dead letter of the super domain is named as 'DeadLetterChannel' and the DLC of a tenant takes the
     * format 'tenantDomain/DeadLetterChannel'
     * Therefore, The DLC queue name return by this method takes either of the above formats.
     *
     * @param tenantDomain the domain name of the tenant
     * @return The Dead Letter Queue name for the tenant.
     */
    public static String generateDLCQueueNameFromTenant(String tenantDomain) {
        //TODO: C5-migration
//        //If the tenantDomain is either null of if it is "carbon.super", the DLC of the super domain will be returned
//        //Else, the dlc for the respective tenant will be returned
//        if (null != tenantDomain && !(org.wso2.carbon.base.MultitenantConstants.SUPER_TENANT_DOMAIN_NAME
//                .equals(tenantDomain))) {
//            return tenantDomain + AndesConstants.TENANT_SEPARATOR + AndesConstants.DEAD_LETTER_QUEUE_SUFFIX;
//        } else {
        return AndesConstants.DEAD_LETTER_QUEUE_SUFFIX;
//        }
    }

    /**
     * Decides on whether a given queue name is a Dead Letter Queue or not.
     *
     * @param queueName The Queue name to test.
     * @return True if a Dead Letter Queue, False if not a Dead Letter Queue.
     */
    public static boolean isDeadLetterQueue(String queueName) {
        boolean isDeadLetterQueue = false;
        if (queueName.contains(AndesConstants.TENANT_SEPARATOR)) {
            //The Queue is in the tenant realm
            if (AndesConstants.DEAD_LETTER_QUEUE_SUFFIX.equals(
                    queueName.split(AndesConstants.TENANT_SEPARATOR, 2)[1])) {
                isDeadLetterQueue = true;
            }
        } else {
            if (AndesConstants.DEAD_LETTER_QUEUE_SUFFIX.equals(queueName)) {
                isDeadLetterQueue = true;
            }
        }

        return isDeadLetterQueue;
    }

    /**
     * Creates a Dead Letter Queue for the tenant.
     * Only one DLC queue is valid for a tenant.
     *
     * @param tenantName  The tenant name for which the DLC should be created.
     * @param tenantOwner The admin of the tenant
     * @throws AndesException
     */
    public static synchronized void createDLCQueue(String tenantName, String tenantOwner) throws AndesException {
//        IApplicationRegistry applicationRegistry = ApplicationRegistry.getInstance();
//        VirtualHost virtualHost = applicationRegistry.getVirtualHostRegistry().getDefaultVirtualHost();
//        QueueRegistry queueRegistry = virtualHost.getQueueRegistry();

        String dlcQueueName;

        //TODO: C5-migration
//        if (org.wso2.carbon.base.MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(tenantName)) {
        dlcQueueName = AndesConstants.DEAD_LETTER_QUEUE_SUFFIX;
//        } else {
//            dlcQueueName = tenantName + AndesConstants.TENANT_SEPARATOR + AndesConstants.DEAD_LETTER_QUEUE_SUFFIX;
//        }

        // Try to retrieve queue to check if it is already available
//        AMQQueue queue = queueRegistry.getQueue(new AMQShortString(dlcQueueName));

        // Skip creating if already available
//        if (null == queue) {
//            ProtocolType dlcProtocolType =
//                                new ProtocolType(AndesConstants.DLC_PROTOCOL_NAME, AndesConstants
// .DLC_PROTOCOL_VERSION);
//
//            //store a queue for DLC and notify the Hazelcast instance about the change
//            Andes.getInstance().createQueue(new InboundQueueEvent(
//                    dlcQueueName, tenantOwner, false, true, dlcProtocolType, AndesConstants.DLC_DESTINATION_TYPE));
//
//            //add the queue into internal qpid
//            ClusterResourceHolder.getInstance().getConfigSynchronizer().queue(dlcQueueName, tenantOwner,
//                    false, null, dlcProtocolType);
//            log.info(dlcQueueName + " Queue Created as Dead Letter Channel");
//        }
    }

    /**
     * Add message to DLC
     *
     * @param message Message to be moved to DLC
     * @param protocolType {@link ProtocolType}
     */
//    public static void addToDeadLetterChannel(QueueEntry message, ProtocolType protocolType) {
//        try {

//        AMQMessage amqMessage = (AMQMessage)message.getMessage();
//        Long messageID = amqMessage.getMessageNumber();
//        String storageQueue = amqMessage.getRoutingKey();
//        AndesMessageMetadata messageToMove = AMQPUtils.convertAMQMessageToAndesMetadata(amqMessage, protocolType);
//        messageToMove.setStorageDestination(storageQueue);
//
//        List<AndesMessageMetadata> messageToMoveToDLC = new ArrayList<>();
//        messageToMoveToDLC.add(messageToMove);
//
//        if (log.isDebugEnabled()) {
//            log.debug("Moving message to Dead Letter Channel. Message ID " + messageID);
//        }
//
//        Andes.getInstance().deleteMessages(messageToMoveToDLC, true);
//
//        } catch (AndesException dlcException) {
//            // If an exception occur in this level, it means that there is a message store level error.
//            // There's a possibility that we might lose this message
//            // If the message is not removed the slot will not get removed which will lead to an
//            // inconsistency
//            log.error("Error moving message " + message.getMessage().getMessageNumber() + " to dead letter channel.",
//                      dlcException);
//        }
//    }
}
