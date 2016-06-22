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

package org.wso2.carbon.andes.core.internal.outbound;

import com.lmax.disruptor.ExceptionHandler;
import com.lmax.disruptor.IgnoreExceptionHandler;
import org.apache.log4j.Logger;

/**
 * Disruptor Exception handler for delivery disruptor ring buffer
 */
public class DeliveryExceptionHandler implements ExceptionHandler {
    /**
     * Class logger
     */
    private static final Logger log = Logger.getLogger(IgnoreExceptionHandler.class);

    /**
     * This method is called if an uncaught exception occurred in a handler
     *
     * @param exception Exception
     * @param sequence  Sequence Id of the event data
     * @param event     Event data holder
     */
    @Override
    public void handleEventException(final Throwable exception, final long sequence, final Object event) {
        log.error("Exception processing: " + sequence + " " + event, exception);

        // Mark event data for error
        DeliveryEventData eventData = (DeliveryEventData) event;
        eventData.reportExceptionOccurred();
    }

    /**
     * Handle startup errors of disruptor
     *
     * @param ex Exception
     */
    @Override
    public void handleOnStartException(final Throwable ex) {
        log.error("Exception during onStart()", ex);
    }

    /**
     * Handle shutdown errors of disruptor
     *
     * @param ex Exception
     */
    @Override
    public void handleOnShutdownException(final Throwable ex) {
        log.error("Exception during onShutdown()", ex);
    }
}
