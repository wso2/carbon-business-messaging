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

package org.wso2.carbon.andes.core;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.andes.core.internal.configuration.AndesConfigurationManager;
import org.wso2.carbon.andes.core.internal.configuration.enums.AndesConfiguration;

import java.util.List;

/**
 * This thread will keep looking for expired messages within the broker and remove them.
 */
public class MessageExpirationWorker extends Thread {

    private static Log log = LogFactory.getLog(MessageExpirationWorker.class);
    private volatile boolean working = false;

    //configurations
    private final Integer workerWaitInterval;
    private final Integer messageBatchSize;
    private final Boolean saveExpiredToDLC;

    public MessageExpirationWorker() {

        workerWaitInterval = AndesConfigurationManager.readValue
                (AndesConfiguration.PERFORMANCE_TUNING_MESSAGE_EXPIRATION_CHECK_INTERVAL);
        messageBatchSize = AndesConfigurationManager.readValue
                (AndesConfiguration.PERFORMANCE_TUNING_MESSAGE_EXPIRATION_BATCH_SIZE);
        saveExpiredToDLC = AndesConfigurationManager.readValue
                (AndesConfiguration.TRANSPORTS_AMQP_SEND_EXPIRED_MESSAGES_TO_DLC);

        this.start();
        this.startWorking();
    }

    @Override
    public void run() {

        int failureCount = 0;

        // The purpose of the "while true" loop here is to ensure that once the worker is started, it will verify the
        // "working" volatile variable by itself
        // and be able to wake up if the working state is changed to "false" and then "true".
        // If we remove the "while (true)" part, we will need to re-initialize the MessageExpirationChecker from
        // various methods outside
        // once we set the "working" variable to false (since the thread will close).
        while (true) {
            if (working) {
                try {
                    //Get Expired message IDs from the database with the massageBatchSize as the limit
                    // we cannot delegate a cascaded delete to cassandra since it doesn't maintain associations
                    // between columnfamilies.
                    List<AndesMessageMetadata> expiredMessages = MessagingEngine.getInstance().getExpiredMessages(
                            messageBatchSize);

                    if (expiredMessages == null || expiredMessages.size() == 0) {
                        sleepForWaitInterval(workerWaitInterval);
                    } else {

                        if (log.isDebugEnabled()) {
                            log.debug("Expired message count : " + expiredMessages.size());
                        }

                        if (log.isTraceEnabled()) {

                            String messagesQueuedForExpiry = "";

                            for (AndesMessageMetadata arm : expiredMessages) {
                                messagesQueuedForExpiry += arm.getMessageId() + ",";
                            }
                            log.trace("Expired messages queued for deletion : " + messagesQueuedForExpiry);
                        }

                        Andes.getInstance().deleteMessages(expiredMessages, saveExpiredToDLC);
                        sleepForWaitInterval(workerWaitInterval);

                        // Note : We had a different alternative to employ cassandra column level TTLs to
                        // automatically handle
                        // deletion of expired message references. But since we need to abstract database specific
                        // logic to
                        // support different data models (like RDBMC) in future, the above approach is followed.
                    }

                } catch (Throwable e) {
                    log.error("Error running Message Expiration Checker " + e.getMessage(), e);
                    // The wait time here is designed to increase per failure to avoid unnecessary attempts to wake
                    // up the thread.
                    // However, given that the most probable error here could be a timeout during the database call,
                    // it could recover in the next few attempts.
                    // Therefore, no need to keep on delaying the worker.
                    // So the maximum interval between the startup attempt will be 5 * regular wait time.
                    long waitTime = workerWaitInterval;
                    failureCount++;
                    long faultWaitTime = Math.max(waitTime * 5, failureCount * waitTime);
                    try {
                        Thread.sleep(faultWaitTime);
                    } catch (InterruptedException ignore) {
                        //silently ignore
                    }

                }
            } else {
                sleepForWaitInterval(workerWaitInterval);
            }
        }
    }

    /**
     * get if Message Expiration Worker is active
     *
     * @return isWorking
     */
    public boolean isWorking() {
        return working;
    }

    /**
     * set Message Expiration Worker active
     */
    public void startWorking() {
        if (log.isDebugEnabled()) {
            log.debug("Starting message expiration checker.");
        }
        working = true;
    }

    /**
     * Stop Message expiration Worker
     */
    public void stopWorking() {
        if (log.isDebugEnabled()) {
            log.debug("Shutting down message expiration checker.");
        }
        working = false;
    }

    private void sleepForWaitInterval(int sleepInterval) {
        try {
            Thread.sleep(sleepInterval);
        } catch (InterruptedException ignore) {
            //ignored
        }
    }

    public static boolean isExpired(Long msgExpiration) {
        if (msgExpiration > 0) {
            return (System.currentTimeMillis() > msgExpiration);
        } else {
            return false;
        }
    }
}
