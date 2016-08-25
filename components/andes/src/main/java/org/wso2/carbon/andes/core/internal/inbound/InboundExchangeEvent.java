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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.andes.core.AndesContextInformationManager;
import org.wso2.carbon.andes.core.AndesException;
import org.wso2.carbon.andes.core.AndesExchange;

/**
 * Exchange related inbound event
 */
public class InboundExchangeEvent extends AndesExchange implements AndesInboundStateEvent {

    private static Log log = LogFactory.getLog(InboundExchangeEvent.class);

    /**
     * Default Serialization UID
     */
    private static final long serialVersionUID = 1L;

    private enum EventType {

        CREATE_EXCHANGE_EVENT,

        DELETE_EXCHANGE_EVENT
    }

    /**
     * Event type this event
     */
    private EventType eventType;

    /**
     * Reference to AndesContextInformationManager to update create/ remove queue state
     */
    private transient AndesContextInformationManager contextInformationManager;

    /**
     * create an instance of Andes Exchange
     *
     * @param exchangeName name of exchange
     * @param type         type of the exchange
     * @param autoDelete   is exchange auto deletable
     */
    public InboundExchangeEvent(String exchangeName, String type, boolean autoDelete) {
        super(exchangeName, type, autoDelete);
    }

    /**
     * create an instance of Andes Exchange
     *
     * @param exchangeAsStr exchange as encoded string
     */
    public InboundExchangeEvent(String exchangeAsStr) {
        super(exchangeAsStr);
    }

    @Override
    public void updateState() throws AndesException {
        switch (eventType) {
            case CREATE_EXCHANGE_EVENT:
                contextInformationManager.createExchange(this);
                break;
            case DELETE_EXCHANGE_EVENT:
                contextInformationManager.deleteExchange(this);
                break;
            default:
                log.error("Event type not set properly " + eventType);
                break;
        }
    }

    @Override
    public String eventInfo() {
        return eventType.toString();
    }

    /**
     * Update the event to a create exchange event
     *
     * @param contextInformationManager AndesContextInformationManager
     */
    public void prepareForCreateExchange(AndesContextInformationManager contextInformationManager) {
        eventType = EventType.CREATE_EXCHANGE_EVENT;
        this.contextInformationManager = contextInformationManager;
    }

    /**
     * Update event to delete exchange event
     *
     * @param contextInformationManager AndesContextInformationManager
     */
    public void prepareForDeleteExchange(AndesContextInformationManager contextInformationManager) {
        eventType = EventType.DELETE_EXCHANGE_EVENT;
        this.contextInformationManager = contextInformationManager;
    }
}
