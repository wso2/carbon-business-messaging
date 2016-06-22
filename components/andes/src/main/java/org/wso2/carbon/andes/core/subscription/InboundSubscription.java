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

package org.wso2.carbon.andes.core.subscription;

import org.wso2.carbon.andes.core.AndesException;
import org.wso2.carbon.andes.core.AndesSubscription;

/**
 * Contract defining subscription related to inbound subscription operations.
 */
public interface InboundSubscription extends AndesSubscription {

    /**
     * Acknowledge is received for message
     *
     * @param messageID id of the message
     * @throws AndesException in case of handling error
     */
    public void ackReceived(long messageID) throws AndesException;

    /**
     * Reject is received for message
     *
     * @param messageID id of the message
     * @throws AndesException in case of handling error
     */
    public void msgRejectReceived(long messageID) throws AndesException;

    /**
     * Close subscription is received
     *
     * @throws AndesException in case of handling error
     */
    public void close() throws AndesException;
}
