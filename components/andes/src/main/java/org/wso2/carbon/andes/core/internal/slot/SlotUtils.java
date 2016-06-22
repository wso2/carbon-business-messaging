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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.andes.core.AndesException;
import org.wso2.carbon.andes.core.DeliverableAndesMetadata;
import org.wso2.carbon.andes.core.MessagingEngine;

import java.util.List;

/**
 * This class contains methods used by both coordinator and member nodes
 */
public class SlotUtils {

    private static Log log = LogFactory.getLog(SlotUtils.class);


    /**
     * TODO: We don't have to read whole metadata list for this
     * Check whether there  are any messages left in the slot after all the acks are received.
     * Returns false  if there are any.
     *
     * @param slot {@link Slot} to be checked
     * @return Whether the slot is empty or not
     */
    public static boolean checkSlotEmptyFromMessageStore(Slot slot) {
        try {
            List<DeliverableAndesMetadata> messagesReturnedFromCassandra =
                    MessagingEngine.getInstance().getMetaDataList(slot,
                                                                  slot.getStorageQueueName(), slot.getStartMessageId(),
                                                                  slot.getEndMessageId());
            return messagesReturnedFromCassandra == null || messagesReturnedFromCassandra.isEmpty();
        } catch (AndesException e) {
            log.error("Error occurred while querying metadata from message store", e);
            return false;
        }
    }
}
