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

import com.lmax.disruptor.EventFactory;
import org.apache.log4j.Logger;
import org.wso2.carbon.andes.core.AndesContent;
import org.wso2.carbon.andes.core.ProtocolMessage;
import org.wso2.carbon.andes.core.subscription.LocalSubscription;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Delivery event data holder. This is used to store and retrieve data between different handlers
 */
public class DeliveryEventData {
    /**
     * Recipient of the message
     */
    private LocalSubscription localSubscription;

    /**
     * Metadata of the message
     */
    private ProtocolMessage metadata;

    /**
     * Indicate if any error occurred during processing handlers
     */
    private boolean errorOccurred;

    /**
     * Provide access to message content
     */
    private AndesContent andesContent;

    /**
     * When content is decompressed this boolean is set to false
     * {@link ContentDecompressionHandler} will check this boolean and
     * if not decompressed will decompress the content.
     */
    private final AtomicBoolean freshContent;

    private static final Logger log = Logger.getLogger(DeliveryEventData.class);

    public DeliveryEventData() {
        this.errorOccurred = false;
        freshContent = new AtomicBoolean(true);
    }

    /**
     * Factory used by the Disruptor to create delivery event data
     *
     * @return Delivery event data holder factory
     */
    public static EventFactory<DeliveryEventData> getFactory() {
        return new DeliveryEventDataFactory();
    }

    /**
     * Clear state data for current instance. This should be called by the last event handler for the ring-buffer
     */
    public void clearData() {
        errorOccurred = false;
        andesContent = null;
        freshContent.set(true);
    }

    /**
     * Get the content message object
     *
     * @return content object
     */
    public AndesContent getAndesContent() {
        return andesContent;
    }

    /**
     * Set message content object
     *
     * @param andesContent content object
     */
    public void setAndesContent(AndesContent andesContent) {
        this.andesContent = andesContent;
    }

    /**
     * Get local subscription for current event
     *
     * @return Local subscription
     */
    public LocalSubscription getLocalSubscription() {
        return localSubscription;
    }

    /**
     * Set AMQP local subscription for current event
     *
     * @param localSubscription Local subscription
     */
    public void setLocalSubscription(LocalSubscription localSubscription) {
        this.localSubscription = localSubscription;
    }

    /**
     * Get metadata for current event
     *
     * @return Metadata
     */
    public ProtocolMessage getMetadata() {
        return metadata;
    }

    /**
     * Set metadata for current event
     *
     * @param metadata Metadata
     */
    public void setMetadata(ProtocolMessage metadata) {
        this.metadata = metadata;
    }

    /**
     * Used to indicate errors by handlers
     */
    public void reportExceptionOccurred() {
        errorOccurred = true;
    }

    @Override
    public String toString() {
        return "Message ID: " + metadata.getMessage().getMessageId() + ", Error occurred : " + isErrorOccurred();
    }

    /**
     * Check if any errors reported by previous handlers
     *
     * @return true if error occurred
     */
    public boolean isErrorOccurred() {
        return errorOccurred;
    }

    /**
     * If content is available to decompress this will return true and atomically updated the state
     * of the event to denote content is taken for decompression
     * <p/>
     * This method is used by {@link ContentDecompressionHandler} to confirm that the message isn't
     * decompressed yet
     *
     * @return true if content isn't decompressed yet and false otherwise
     */
    public boolean availableForDecompression() {
        return freshContent.compareAndSet(true, false);
    }

    /**
     * Factory class for delivery event data
     */
    public static class DeliveryEventDataFactory implements EventFactory<DeliveryEventData> {
        @Override
        public DeliveryEventData newInstance() {
            return new DeliveryEventData();
        }
    }
}
