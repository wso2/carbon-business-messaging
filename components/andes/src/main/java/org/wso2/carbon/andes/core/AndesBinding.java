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

import org.apache.commons.lang.builder.HashCodeBuilder;

import java.io.Serializable;

/**
 * Represents a queue binding in Andes
 */
public class AndesBinding implements Serializable {

    public String boundExchangeName;
    public String routingKey;
    public transient AndesQueue boundQueue;

    /**
     * create an instance of andes binding
     *
     * @param boundExchangeName name of exchange binding carries
     * @param boundQueue        name of the queue bound
     * @param routingKey        routing key of the binding
     */
    public AndesBinding(String boundExchangeName, AndesQueue boundQueue, String routingKey) {
        this.boundExchangeName = boundExchangeName;
        this.boundQueue = boundQueue;
        this.routingKey = routingKey;
    }

    /**
     * create an instance of andes binding
     *
     * @param bindingAsStr binding information as encoded string
     */
    public AndesBinding(String bindingAsStr) throws AndesException {
        String[] propertyToken = bindingAsStr.split("\\|");
        for (String pt : propertyToken) {
            String[] tokens = pt.split("&");
            if (tokens[0].equals("boundExchange")) {
                this.boundExchangeName = tokens[1];
            } else if (tokens[0].equals("boundQueue")) {
                this.boundQueue = new AndesQueue(tokens[1]);
            } else if (tokens[0].equals("routingKey")) {
                this.routingKey = tokens[1];
            }
        }
    }

    public String toString() {
        return "[Binding]" + "E=" + boundExchangeName +
                "/Q=" + boundQueue.queueName +
                "/RK=" + routingKey +
                "/D=" + boundQueue.isDurable +
                "/EX=" + boundQueue.isExclusive;
    }

    /**
     * Encode object to a string.
     */
    public String encodeAsString() {
        return "boundExchange&" + boundExchangeName +
                "|boundQueue&" + boundQueue.encodeAsString() +
                "|routingKey&" + routingKey;
    }

    public boolean equals(Object o) {
        if (o instanceof AndesBinding) {
            AndesBinding c = (AndesBinding) o;
            if (this.boundExchangeName.equals(c.boundExchangeName) &&
                    this.boundQueue.queueName.equals(c.boundQueue.queueName) &&
                    this.routingKey.equals(c.routingKey)) {
                return true;
            }
        }
        return false;
    }

    public int hashCode() {
        return new HashCodeBuilder(17, 31).
                append(boundExchangeName).
                append(boundQueue.queueName).
                append(routingKey).
                toHashCode();
    }
}
