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

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * <p>
 * Utility class to encode/decode values to/from a {@link ByteBuffer}
 * </p>
 */
public class AndesEncodingUtil {

    /**
     * Encode a long variable
     *
     * @param buffer {@link ByteBuffer}
     * @param value  long value to be encoded
     */
    public static void putLong(ByteBuffer buffer, long value) {
        buffer.putLong(value);
    }

    /**
     * Encode an integer variable
     *
     * @param buffer {@link ByteBuffer}
     * @param value  integer value to be encoded
     */
    public static void putInt(ByteBuffer buffer, int value) {
        buffer.putInt(value);
    }

    /**
     * Encode a boolean value.
     *
     * @param buffer {@link ByteBuffer}
     * @param value  value to be encoded
     */
    public static void putBoolean(ByteBuffer buffer, boolean value) {
        buffer.put(value ? (byte) 1 : (byte) 0);
    }

    /**
     * Encode a string value
     *
     * @param buffer {@link ByteBuffer}
     * @param value  string value to be encoded
     */
    public static void putString(ByteBuffer buffer, String value) {
        byte[] byteArray = value.getBytes(StandardCharsets.UTF_8);
        buffer.putInt(byteArray.length);
        buffer.put(byteArray);
    }

    /**
     * Retrieve an encoded String
     *
     * @param buffer {@link ByteBuffer}
     * @return decoded String
     */
    public static String getString(ByteBuffer buffer) {
        int length = buffer.getInt();
        byte[] byteArray = new byte[length];
        buffer.get(byteArray);
        return new String(byteArray, StandardCharsets.UTF_8);
    }

    /**
     * Retrieve the value of an encoded boolean
     *
     * @param buffer {@link ByteBuffer}
     * @return decoded boolean value
     */
    public static boolean getBoolean(ByteBuffer buffer) {
        return buffer.get() == (byte) 1;
    }

    /**
     * Retrieve encoded integer
     *
     * @param buffer {@link ByteBuffer}
     * @return decoded integer value
     */
    public static int getEncodedInt(ByteBuffer buffer) {
        return buffer.getInt();
    }

    /**
     * Retrieve encoded long value
     *
     * @param buffer {@link ByteBuffer}
     * @return decoded long value
     */
    public static long getEncodedLong(ByteBuffer buffer) {
        return buffer.getLong();
    }

    /**
     * The number of bytes used to represent a {@code long} value in two's
     * complement binary form.
     *
     * @return encoded long in bytes
     */
    public static int getEncodedLongLength() {
        return Long.BYTES;
    }

    /**
     * The number of bytes used to represent a {@code int} value in two's
     * complement binary form.
     *
     * @return encoded integer in bytes
     */
    public static int getEncodedIntLength() {
        return Integer.BYTES;
    }

    /**
     * Length of encoded boolean
     *
     * @return encoded boolean in bytes
     */
    public static int getEncodedBooleanLength() {
        return Byte.BYTES;
    }

    /**
     * The number of bytes used to represent a {@code byte} value in two's
     * complement binary form.
     *
     * @return length in bytes
     */
    public static int getEncodedByteLength() {
        return Byte.BYTES;
    }

    /**
     * This method call is slow. This calls string#getBytes() which is slower.
     *
     * @param value String that we need to get the encoded length
     * @return length in bytes
     */
    public static int getEncodedStringLength(String value) {
        return Integer.BYTES + value.getBytes(StandardCharsets.UTF_8).length;
    }

}
