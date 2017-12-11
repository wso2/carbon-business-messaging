/*
 * Copyright (c) 2017, WSO2 Inc. (http://wso2.com) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.business.messaging.admin.services.managers.impl;

import org.wso2.andes.kernel.Andes;
import org.wso2.andes.kernel.AndesException;
import org.wso2.andes.kernel.disruptor.inbound.InboundQueueEvent;
import org.wso2.carbon.business.messaging.admin.services.exceptions.InternalServerException;
import org.wso2.carbon.business.messaging.admin.services.internal.MbRestServiceDataHolder;
import org.wso2.carbon.business.messaging.admin.services.managers.MessageManagerService;

/**
 * Implementation for handling messages related queries.
 */
public class MessageManagerServiceImpl implements MessageManagerService {
    /**
     * Registered andes core instance through OSGi.
     */
    private Andes andesCore;

    public MessageManagerServiceImpl() {
        andesCore = MbRestServiceDataHolder.getInstance().getAndesCore();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteMessages(String protocol, String destinationType, String destinationName)
            throws InternalServerException {
        try {
            andesCore.purgeQueue(
                    new InboundQueueEvent(destinationName, Boolean.TRUE, Boolean.FALSE, "admin", Boolean.FALSE));
        } catch (AndesException e) {
            throw new InternalServerException("Error while purging the destination.", e);
        }
    }
}
