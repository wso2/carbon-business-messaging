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
 * Implementation which doesn't cache any message. used when user disabled the
 * message cache via configuration.
 */
public class DisabledMessageCacheImpl implements AndesMessageCache {

    /**
     * {@inheritDoc}
     */
    @Override
    public void addToCache(AndesMessage message) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeFromCache(LongArrayList messagesToRemove) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeFromCache(long messageToRemove) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AndesMessage getMessageFromCache(long messageId) {

        return null;

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void fillContentFromCache(LongArrayList messageIDList,
                                     LongObjectHashMap<List<AndesMessagePart>> contentList) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AndesMessagePart getContentFromCache(long messageId, int offsetValue) {
        return null;

    }

}
