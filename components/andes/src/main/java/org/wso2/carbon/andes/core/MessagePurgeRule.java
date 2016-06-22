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

/**
 * This class represents message purging Delivery Rule
 */
public class MessagePurgeRule implements CommonDeliveryRule {

    private static Log log = LogFactory.getLog(MessagePurgeRule.class);

    /**
     * Evaluating the message purge delivery rule
     *
     * @return isOKToDelivery
     * @throws AndesException
     */
    @Override
    public boolean evaluate(DeliverableAndesMetadata message, ProtocolType protocolType,
                            DestinationType destinationType) throws AndesException {
        long messageID = message.getMessageID();

        // Get last purged timestamp of the destination queue.
        long lastPurgedTimestampOfQueue =
                MessageFlusher.getInstance().getMessageDeliveryInfo(message.getDestination(), protocolType,
                                                                    destinationType)
                        .getLastPurgedTimestamp();

        if (message.getArrivalTime() <= lastPurgedTimestampOfQueue) {

            log.warn("Message was sent at " + message.getArrivalTime()
                             + " before last purge event at " + lastPurgedTimestampOfQueue
                             + ". Therefore, it will not be sent. id= "
                             + messageID);
            if (!message.isPurgedOrDeletedOrExpired()) {
                message.markAsPurgedMessage();
            }
            return false;
        } else {
            return true;
        }
    }
}
