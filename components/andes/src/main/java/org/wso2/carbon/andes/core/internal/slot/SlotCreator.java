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

package org.wso2.carbon.andes.core.internal.slot;

import com.gs.collections.impl.list.mutable.primitive.LongArrayList;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.andes.core.AndesException;
import org.wso2.carbon.andes.core.internal.AndesContext;
import org.wso2.carbon.andes.core.internal.configuration.AndesConfigurationManager;
import org.wso2.carbon.andes.core.internal.configuration.enums.AndesConfiguration;
import org.wso2.carbon.andes.core.store.MessageStore;

/**
 * SlotCreator is used to recover slots belonging to a storage queue when the cluster is restarted.
 */
public class SlotCreator implements Runnable {

    /**
     * Interval between two consecutive stat logs in milliseconds
     */
    private static final int STAT_PUBLISHING_INTERVAL = 10 * 1000;

    /**
     * Class logger
     */
    private static Log log = LogFactory.getLog(SlotCreator.class);

    /**
     * Storage queue handled by current instance
     */
    private final String queueName;

    /**
     * Configured Size of a slot
     */
    private final int slotSize;

    /**
     * Message store instance used to read messages
     */
    private final MessageStore messageStore;

    public SlotCreator(MessageStore messageStore, String queueName) {
        this.messageStore = messageStore;
        this.queueName = queueName;
        this.slotSize = AndesConfigurationManager
                .readValue(AndesConfiguration.PERFORMANCE_TUNING_SLOTS_SLOT_WINDOW_SIZE);
    }

    @Override
    public void run() {
        try {
            log.info("Slot restoring started for " + queueName);
            initializeSlotMapForQueue();
            log.info("Slot restoring ended for " + queueName);
        } catch (Throwable e) {
            log.error("Error occurred in slot recovery", e);
        }
    }

    /**
     * Iteratively recover messages for the storage queue
     *
     * @throws AndesException
     * @throws ConnectionException
     */
    private void initializeSlotMapForQueue() throws AndesException, ConnectionException {
        int databaseReadsCounter = 0;
        int restoreMessagesCounter = 0;
        long messageCountOfQueue = messageStore.getMessageCountForQueue(queueName);

        LongArrayList messageIdList = messageStore.getNextNMessageIdsFromQueue(queueName, 0, slotSize);
        int numberOfMessages = messageIdList.size();

        databaseReadsCounter++;
        restoreMessagesCounter = restoreMessagesCounter + messageIdList.size();

        long lastMessageID;
        long firstMessageID;
        long lastStatPublishTime = System.currentTimeMillis();

        while (numberOfMessages > 0) {
            int lastMessageArrayIndex = numberOfMessages - 1;
            lastMessageID = messageIdList.get(lastMessageArrayIndex);
            firstMessageID = messageIdList.get(0);

            if (log.isDebugEnabled()) {
                log.debug("Created a slot with " + messageIdList.size() + " messages for queue (" + queueName + ")");
            }

            if (AndesContext.getInstance().isClusteringEnabled()) {
                SlotManagerClusterMode.getInstance().updateMessageID(queueName,
                                                                     AndesContext.getInstance().getClusterAgent()
                                                                             .getLocalNodeIdentifier(),
                                                                     firstMessageID,
                                                                     lastMessageID, lastMessageID);
            } else {
                SlotManagerStandalone.getInstance().updateMessageID(queueName, lastMessageID);
            }

            long currentTimeInMillis = System.currentTimeMillis();
            if (currentTimeInMillis - lastStatPublishTime > STAT_PUBLISHING_INTERVAL) {
                // messageCountOfQueue is multiplied by 1.0 to convert it to double
                double recoveredPercentage = (restoreMessagesCounter / (messageCountOfQueue * 1.0)) * 100.0;
                log.info(restoreMessagesCounter + "/" + messageCountOfQueue + " (" + Math.round(recoveredPercentage)
                                 + "%) messages recovered for queue \"" + queueName + "\"");
                lastStatPublishTime = currentTimeInMillis;
            }

            // We need to increment lastMessageID since the getNextNMessageMetadataFromQueue returns message list
            // including the given starting ID.
            messageIdList = messageStore.getNextNMessageIdsFromQueue(queueName, lastMessageID + 1, slotSize);
            numberOfMessages = messageIdList.size();
            //increase value of counters
            databaseReadsCounter++;
            restoreMessagesCounter = restoreMessagesCounter + messageIdList.size();
        }

        log.info("Recovered " + restoreMessagesCounter + " messages for queue \"" + queueName + "\" using "
                         + databaseReadsCounter + " database calls");
    }
}
