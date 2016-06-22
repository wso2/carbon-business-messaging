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

package org.wso2.carbon.andes.core.store.cache;

import com.gs.collections.impl.list.mutable.primitive.LongArrayList;
import com.gs.collections.impl.map.mutable.primitive.LongObjectHashMap;
import org.wso2.carbon.andes.core.AndesMessage;
import org.wso2.carbon.andes.core.AndesMessagePart;

import java.util.List;

/**
 * Keeps track of messages that were stored (from this message broker instance).
 * Intention is to eliminate the need to go to database to read
 * messages/metadata if they are inserted from
 * this node. (intension is to reduce the strain on database and improves
 * performance)
 */
public interface AndesMessageCache {

    /**
     * Add the given message to cache
     *
     * @param message the message
     */
    abstract void addToCache(AndesMessage message);

    /**
     * Removes given list of messages/ids from the cache
     *
     * @param messagesToRemove list of message Ids
     */
    abstract void removeFromCache(LongArrayList messagesToRemove);

    /**
     * Removes a message with a given id from the cache
     *
     * @param messageToRemove list of message Ids
     */
    abstract void removeFromCache(long messageToRemove);

    /**
     * Returns a message if found in cache
     *
     * @param messageId message id to look up
     * @return a message or null (if not found)
     */
    abstract AndesMessage getMessageFromCache(long messageId);

    /**
     * Get the list of messages found from the cache.
     * <b> This method modifies the provided messageIDList </b>
     *
     * @param messageIDList message id to be found in cache.
     * @param contentList   the list the fill
     */
    abstract void fillContentFromCache(LongArrayList messageIDList,
                                       LongObjectHashMap<List<AndesMessagePart>> contentList);

    /**
     * Return a {@link AndesMessagePart} from the cache.
     *
     * @param messageId   id of the massage
     * @param offsetValue the offset value
     * @return a {@link AndesMessagePart} if the message is found otherwise null
     */
    abstract AndesMessagePart getContentFromCache(long messageId, int offsetValue);

}
