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
 * This class represents message expiration Delivery Rule
 */
public class MessageExpiredRule implements CommonDeliveryRule {

    private static Log log = LogFactory.getLog(MessageExpiredRule.class);

    /**
     * Evaluating the message expiration delivery rule
     *
     * @return isOKToDelivery
     */
    @Override
    public boolean evaluate(DeliverableAndesMetadata message, ProtocolType protocolType,
                            DestinationType destinationType) {
        long messageID = message.getMessageID();
        //Check if destination entry has expired. Any expired message will not be delivered
        if (message.isExpired()) {
            log.warn("Message is expired. Routing Message to DLC : id= " + messageID);
            return false;
        } else {
            return true;
        }
    }
}
