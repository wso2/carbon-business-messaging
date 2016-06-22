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

/**
 * This interface have methods to implement to evaluate common
 * Deliver Rule
 */
public interface CommonDeliveryRule {

    /**
     * Evaluate delivery rule. This returns true if the rule is passed. Should be called in kernel
     * before message is dispatched to the protocol. If failed message should not be sent and
     * necessary actions should be taken.
     *
     * @param message         Message to evaluate rules
     * @param protocolType    The protocol type of the message
     * @param destinationType The destination type of the message
     * @return True if rule is passed
     * @throws AndesException
     */
    boolean evaluate(DeliverableAndesMetadata message, ProtocolType protocolType, DestinationType destinationType)
            throws AndesException;
}
