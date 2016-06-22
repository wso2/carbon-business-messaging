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

package org.wso2.carbon.andes.core.internal.inbound;

import org.wso2.carbon.andes.core.AndesException;

/**
 * Andes Inbound event types that only update state implements this interface
 */
public interface AndesInboundStateEvent {

    /**
     * Updates Andes internal state according to the inbound event.
     * Method visibility is package specific. Only StateEventHandler should call this
     *
     * @throws AndesException
     */
    void updateState() throws AndesException;

    /**
     * Returns a human readable information about the event
     *
     * @return String
     */
    String eventInfo();

}
