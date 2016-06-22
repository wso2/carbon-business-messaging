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

import com.lmax.disruptor.EventHandler;
import org.apache.log4j.Logger;
import org.wso2.carbon.andes.core.AndesException;
import org.wso2.carbon.andes.core.AndesMessagePart;
import org.wso2.carbon.andes.core.DisruptorCachedContent;
import org.wso2.carbon.andes.core.ProtocolMessage;
import org.wso2.carbon.andes.core.internal.compression.LZ4CompressionHelper;
import org.wso2.carbon.andes.core.util.MessageTracer;

import java.util.Collection;
import java.util.Map;


/**
 * Disruptor handler used to decompress message contents, before sending messages to subscribers.
 */
public class ContentDecompressionHandler implements EventHandler<DeliveryEventData> {

    /**
     * Class Logger for logging information, error and warning.
     */
    private static final Logger log = Logger.getLogger(ContentDecompressionHandler.class);

    /**
     * Maximum content chunk size stored in DB
     */
    private final int maxChunkSize;

    private LZ4CompressionHelper lz4CompressionHelper;

    /**
     * Creates a {@link ContentDecompressionHandler} object
     *
     * @param maxContentChunkSize maximum content chunk size stored in DB
     */
    public ContentDecompressionHandler(int maxContentChunkSize) {
        this.maxChunkSize = maxContentChunkSize;
        this.lz4CompressionHelper = new LZ4CompressionHelper();
    }

    /**
     * {@inheritDoc}
     */
    /**
     * Handle content decompression
     *
     * @param deliveryEventData Delivery event published to the ring buffer of the DisruptorBasedFlusher
     * @param sequence          Sequence of the event being processed
     * @param endOfBatch        Flag to indicate if this is the last event or not
     */
    @Override
    public void onEvent(DeliveryEventData deliveryEventData, long sequence, boolean endOfBatch) throws Exception {

        // If the content is already decompressed, there is no point in further decompressing
        // Therefore ignore
        if (!(deliveryEventData.availableForDecompression())) {
            return;
        }

        DisruptorCachedContent content = (DisruptorCachedContent) deliveryEventData.getAndesContent();

        ProtocolMessage metadata = deliveryEventData.getMetadata();
        int originalMessageSize = metadata.getMessage().getMessageContentLength();
        long messageID = metadata.getMessageID();

        boolean isCompressed = metadata.getMessage().isCompressed();

        if (log.isDebugEnabled()) {
            log.debug("Message " + messageID + " is compressed = " + isCompressed);
        }

        if (null != content) {
            /* If the current message was compressed by the server, decompress the message content and, update
             * AndesContent of the delivery event data
             */
            if (isCompressed) {

                Map<Integer, AndesMessagePart> messagePartMapFromContentReader = content.getContentList();
                Collection<AndesMessagePart> contentList = messagePartMapFromContentReader.values();

                // Get the decompressed message, as a message part map
                Map<Integer, AndesMessagePart> messagePartMapToDeliver = lz4CompressionHelper.getDecompressedMessage
                        (contentList, originalMessageSize, messageID);

                // Creating the DisruptorCachedContent  to deliver
                content = new DisruptorCachedContent(messagePartMapToDeliver, originalMessageSize, maxChunkSize);

                //Tracing Message
                MessageTracer.trace(messageID, metadata.getMessage().getDestination(),
                                    MessageTracer.CONTENT_DECOMPRESSION_HANDLER_MESSAGE);

            }
            deliveryEventData.setAndesContent(content);

        } else if (0 == originalMessageSize) {
            deliveryEventData.setAndesContent(content);
        } else {
            throw new AndesException(
                    "Empty message received while retrieving message content for message id " + messageID);
        }
    }
}
