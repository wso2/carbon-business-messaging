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

import org.wso2.carbon.andes.core.internal.inbound.PubAckHandler;

/**
 * This Implementation of ack handler drops any ack or nack received
 * from Andes.
 * <p>
 * For instance AMQP 0.91 doesn't support publisher acknowledgements. For that use
 * case publisher acknowledgements can be dropped using this handler
 */
public class DisablePubAckImpl implements PubAckHandler {

    /**
     * Drops the publisher acknowledgment.
     *
     * @param metadata AndesMessageMetadata
     */
    @Override
    public void ack(AndesMessageMetadata metadata) {
        // Do nothing
    }

    /**
     * Drop publisher negative acknowledgment
     *
     * @param metadata AndesMessageMetadata
     */
    @Override
    public void nack(AndesMessageMetadata metadata) {
        // Do nothing
    }
}
