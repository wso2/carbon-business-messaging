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

package org.wso2.carbon.andes.core.internal.cluster;

import java.util.concurrent.atomic.AtomicInteger;


/**
 * Generate Message ids based on the TimeStamp.
 * <p/>
 * Here to preserve the long range we use a time stamp that is created by getting difference between
 * System.currentTimeMillis() and a configured reference time. Reference time can be configured.
 * <p/>
 * Message Id will created by appending time stamp , selected unique id for the node and two digit sequence number
 * <p/>
 * <time stamp> + <selected unique id for the node> + <seq number>
 * <p/>
 * sequence number is used in a scenario when two or more messages comes with same timestamp
 * (within the same millisecond). So to allow message rages higher than 1000 msg/s we use this sequence number
 * where it will be incremented in case of message comes in same millisecond within the same node. With this approach
 * We can go up to 100,000 msg/s
 */
// TODO class name
public class TimeStampBasedMessageIdGenerator implements MessageIdGenerator {
    private int uniqueIdForNode = 0;
    private long lastTimestamp = 0;
    private long lastID = 0;

    /**
     * If two instances for the same time stamp comes add offset values to differentiate them
     */
    private AtomicInteger offset = new AtomicInteger();

    private static final long REFERENCE_START = 41L * 365L * 24L * 60L * 60L * 1000L; //this is 2011

    /**
     * Out of 64 bits for long, we will use the range as follows
     * [1 sign bit][45bits for time spent from reference time in milliseconds][8bit node id][10 bit offset for ID
     * falls within the same timestamp]
     * This assumes there will not be more than 1024 hits within a given millisecond. Range is sufficient for
     * 6029925857 years.
     *
     * @return Generated ID
     */
    public synchronized long getNextId() {
        //TODO review on how we could optimize this code
        uniqueIdForNode = ClusterResourceHolder.getInstance().getClusterManager().getUniqueIdForLocalNode();
        long ts = System.currentTimeMillis();
        int offset = 0;
        if (ts == lastTimestamp) {
            offset = this.offset.incrementAndGet();
        } else {
            this.offset.set(0);
        }
        lastTimestamp = ts;
        long id = (ts - REFERENCE_START) * 256 * 1024 + uniqueIdForNode * 1024 + offset;
        if (lastID == id) {
            throw new RuntimeException("duplicate ids detected. This should never happen");
        }
        lastID = id;
        return id;
    }
}
