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
 * Represents an Exchange within MB core
 */
public class AndesExchange implements Serializable {
    public String exchangeName;
    public String type;
    public boolean autoDelete;

    /**
     * Default Serialization UID
     */
    private static final long serialVersionUID = 1L;

    /**
     * create an instance of Andes Exchange
     *
     * @param exchangeName name of exchange
     * @param type         type of the exchange
     * @param autoDelete   is exchange auto deletable
     */
    public AndesExchange(String exchangeName, String type, boolean autoDelete) {
        this.exchangeName = exchangeName;
        this.autoDelete = autoDelete;
        this.type = type;
    }

    /**
     * create an instance of Andes Exchange
     *
     * @param exchangeAsStr exchange as encoded string
     */
    public AndesExchange(String exchangeAsStr) {
        String[] propertyToken = exchangeAsStr.split(",");
        for (String pt : propertyToken) {
            String[] tokens = pt.split("=");
            if (tokens[0].equals("exchangeName")) {
                this.exchangeName = tokens[1];
            } else if (tokens[0].equals("type")) {
                this.type = tokens[1];
            } else if (tokens[0].equals("autoDelete")) {
                this.autoDelete = Boolean.parseBoolean(tokens[1]);
            }
        }
    }

    public String toString() {
        return "[" + exchangeName + "] " +
                "T=" + type +
                "/AD=" + autoDelete;
    }

    public String encodeAsString() {
        return "exchangeName=" + exchangeName +
                ",type=" + type +
                ",autoDelete" + autoDelete;
    }

    public boolean equals(Object o) {
        if (o instanceof AndesExchange) {
            AndesExchange c = (AndesExchange) o;
            if (this.exchangeName.equals(c.exchangeName) &&
                    this.type.equals(c.type) &&
                    this.autoDelete == c.autoDelete) {
                return true;
            }
        }
        return false;
    }

    public int hashCode() {
        return new HashCodeBuilder(17, 31).
                append(exchangeName).
                append(type).
                append(autoDelete).
                toHashCode();
    }
}
