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

package org.wso2.carbon.andes.core.internal.compression;

import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;
import org.wso2.carbon.andes.core.AndesMessagePart;
import org.wso2.carbon.andes.core.internal.configuration.AndesConfigurationManager;
import org.wso2.carbon.andes.core.internal.configuration.enums.AndesConfiguration;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Handles compression and decompression of message contents, using LZ4 library
 */
public class LZ4CompressionHelper {

    /**
     * Whether message content compression from the server side is enabled
     */
    private final boolean isCompressionEnabled = (boolean) AndesConfigurationManager.readValue
            (AndesConfiguration.PERFORMANCE_TUNING_ALLOW_COMPRESSION);

    /**
     * Maximum content size to check before compress message content, in bytes. Messages with
     * content less than this size will not compress.
     */
    private final int contentCompressionThreshold = (int) AndesConfigurationManager.readValue
            (AndesConfiguration.PERFORMANCE_TUNING_CONTENT_COMPRESSION_THRESHOLD);

    /**
     * Maximum allowed chunk size to be stored in DB, in bytes.
     */
    private final int maxChunkSize = (int) AndesConfigurationManager.readValue(
            AndesConfiguration.PERFORMANCE_TUNING_MAX_CONTENT_CHUNK_SIZE);

    /**
     * Keep a reference to lz4 instance
     */
    private static final LZ4Factory factory = LZ4Factory.fastestInstance();

    /**
     * Keep a reference to lz4 fast compressor to compress data
     */
    private static final LZ4Compressor compressor = factory.fastCompressor();

    /**
     * Keep a reference to lz4 fast decompressor to decompress data
     */
    private static final LZ4FastDecompressor decompressor = factory.fastDecompressor();

    /**
     * Compress the content of the message, using andes message part list
     *
     * @param partList              Original content chunk list
     * @param originalContentLength Total content length of the original message content
     * @return Compressed message content as an AndesMessagePart
     */
    public AndesMessagePart getCompressedMessage(List<AndesMessagePart> partList, int originalContentLength) {

        byte[] messageData = getByteArrayFromPartListForCompression(partList, originalContentLength);

        // Compress message content
        int maxCompressedLength = compressor.maxCompressedLength(originalContentLength);
        byte[] compressed = new byte[maxCompressedLength];
        int compressedLength =
                compressor.compress(messageData, 0, originalContentLength, compressed, 0, maxCompressedLength);

        // Final compressed array without extra indexes, to reduce the array size after compression
        byte[] compressedMessage = Arrays.copyOf(compressed, compressedLength);

        // Getting andes message part of the compressed message content and returning
        return getAndesMessagePart(compressedMessage, partList.get(0).getMessageID());
    }

    /**
     * Decompress the compressed message content. Usage: before deliver to subscriber
     *
     * @param messagePartList       Compressed message content as a collection of andes message parts
     * @param originalContentLength Total content length of the original message content
     * @param messageID             Message ID of the message
     * @return Decompressed message content as a map of andes message parts and offsets
     */
    public Map<Integer, AndesMessagePart> getDecompressedMessage(Collection<AndesMessagePart> messagePartList, int
            originalContentLength, long messageID) {
        byte[] compressedMessageContent = getByteArrayFromPartListForDecompression(messagePartList);

        // Decompress message content
        byte[] decompressedMessage = new byte[originalContentLength];
        decompressor.decompress(compressedMessageContent, 0, decompressedMessage, 0, originalContentLength);

        // Creating message part map using the decompressed message, and returning
        return getHashMapFromByteArray(decompressedMessage, messageID);
    }

    /**
     * Decompress the compressed message content. Usage: for the user interface
     *
     * @param partList              Compressed message content, as a message part list
     * @param originalContentLength Total content length of the original message content
     * @return Decompressed message content as an AndesMessagePart
     */
    public AndesMessagePart getDecompressedMessage(List<AndesMessagePart> partList, int originalContentLength) {
        byte[] compressedMessageContent = getByteArrayFromPartListForDecompression(partList);

        // Decompress message content
        byte[] decompressedMessage = new byte[originalContentLength];
        decompressor.decompress(compressedMessageContent, 0, decompressedMessage, 0, originalContentLength);

        // Creating the AndesMessagePart from decompressed message content
        AndesMessagePart andesMessagePart = new AndesMessagePart();
        andesMessagePart.setData(decompressedMessage);
        andesMessagePart.setOffSet(decompressedMessage.length);

        return andesMessagePart;
    }

    /**
     * Make one byte array from data of andes message parts, when compress messages
     *
     * @param partList              Message content as an AndesMessagePart list
     * @param originalContentLength Original message content length
     * @return Combined message content as a byte array
     */
    private byte[] getByteArrayFromPartListForCompression(Collection<AndesMessagePart> partList,
                                                          int originalContentLength) {

        byte[] messageData = new byte[originalContentLength];

        for (AndesMessagePart messagePart : partList) {
            byte[] messagePartData = messagePart.getData();
            System.arraycopy(messagePartData, 0, messageData, messagePart.getOffset(), messagePartData.length);
        }

        return messageData;
    }

    /**
     * Make one byte array from data of andes message parts, when decompress messages
     *
     * @param partList Message content as an AndesMessagePart list
     * @return Combined message content as a byte array
     */
    private byte[] getByteArrayFromPartListForDecompression(Collection<AndesMessagePart> partList) {

        //Maximum data length can be greater than original content length after compression
        int maximumCompressedDataLength = partList.size() * maxChunkSize;
        byte[] messageData = new byte[maximumCompressedDataLength];
        int exactCompressedDataLength = 0;
        int messagePartLength;

        for (AndesMessagePart messagePart : partList) {
            byte[] messagePartData = messagePart.getData();
            messagePartLength = messagePartData.length;
            System.arraycopy(messagePartData, 0, messageData, messagePart.getOffset(), messagePartLength);
            exactCompressedDataLength = exactCompressedDataLength + messagePartLength;
        }

        System.arraycopy(messageData, 0, messageData, 0, exactCompressedDataLength);

        return messageData;
    }

    /**
     * Make one AndesMessagePart from message content
     *
     * @param data      Message content as a byte array
     * @param messageID message ID
     * @return Message content as an AndesMessagePart
     */
    AndesMessagePart getAndesMessagePart(byte[] data, long messageID) {
        AndesMessagePart messagePart = new AndesMessagePart();
        messagePart.setMessageID(messageID);
        // Ultimately we write the full content as a chunk here
        messagePart.setDataLength(data.length);
        messagePart.setOffSet(0);
        messagePart.setData(data);

        return messagePart;
    }

    /**
     * Creating message part map using the decompressed message
     *
     * @param decompressedMessage Decompressed message content as a byte array
     * @param messageID           Message ID of the message
     * @return Decompressed message content as a map of andes message parts and offsets
     */
    public Map<Integer, AndesMessagePart> getHashMapFromByteArray(byte[] decompressedMessage, long messageID) {

        // Here, decompressedMessageLength = original message size
        int decompressedMessageLength = decompressedMessage.length;

        // Size of this map is equals to the minimum number of andes message parts that are needed
        Map<Integer, AndesMessagePart> messagePartMapToDeliver =
                new HashMap<>((int) (Math.ceil(decompressedMessageLength / ((float) maxChunkSize))));

        int srcOffset = 0;
        int noOfElementsToCopy;
        int remainingElements = decompressedMessageLength - srcOffset;

        while (remainingElements > 0) {

            noOfElementsToCopy = Math.min((decompressedMessageLength - srcOffset), maxChunkSize);
            byte[] copy = new byte[maxChunkSize];

            System.arraycopy(decompressedMessage, srcOffset, copy, 0, noOfElementsToCopy);

            AndesMessagePart messagePart = new AndesMessagePart();
            messagePart.setMessageID(messageID);
            messagePart.setDataLength(copy.length);
            messagePart.setOffSet(srcOffset);
            messagePart.setData(copy);

            messagePartMapToDeliver.put(messagePart.getOffset(), messagePart);

            srcOffset = srcOffset + maxChunkSize;
            remainingElements = decompressedMessageLength - srcOffset;
        }

        return messagePartMapToDeliver;
    }

    /**
     * Check if compression is enabled or not
     *
     * @return Value of the allow compression from configuration
     */
    public boolean isCompressionEnabled() {
        return isCompressionEnabled;
    }

    /**
     * Get the content compression threshold
     *
     * @return Value of the threshold from configuration
     */
    public int getContentCompressionThreshold() {
        return contentCompressionThreshold;
    }

}
