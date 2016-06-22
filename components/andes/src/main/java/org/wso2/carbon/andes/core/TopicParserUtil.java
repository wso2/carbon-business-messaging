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

import org.apache.log4j.Logger;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * This is a utility class for match topics filtered by topic separators and wildcard characters.
 */
public class TopicParserUtil {
    /**
     * Class Logger
     */
    private static final Logger log = Logger.getLogger(TopicParserUtil.class);

    /**
     * Character used to identify tokens in a topic name
     */
    public static final String TOPIC_TOKEN_SEPARATOR = "/";

    /**
     * Character used to identify multiple level topics
     */
    public static final String MULTIPLE_LEVEL_WILDCARD = "#";

    /**
     * Character used to identify single level topics
     */
    public static final String SINGLE_LEVEL_WILDCARD = "+";


    /**
     * Check whether subscribed topic name with wildcards matches the given subscription topic name.
     *
     * @param topicName             topic name
     * @param subscriptionTopicName subscribed topic name
     * @return true if matching
     * @throws AndesException
     * @see <a href="MQTT_V3.1_Protocol_Specific.pdf">
     * http://public.dhe.ibm.com/software/dw/webservices/ws-mqtt/MQTT_V3.1_Protocol_Specific.pdf</a>
     */
    public static boolean isMatching(String topicName, String subscriptionTopicName) throws AndesException {
        try {
            List<Token> msgTokens = splitTopic(topicName);
            List<Token> subscriptionTokens = splitTopic(subscriptionTopicName);
            int token = 0;
            Token subToken = null;
            for (; token < subscriptionTokens.size(); token++) {
                subToken = subscriptionTokens.get(token);
                if (subToken != Token.MULTI && subToken != Token.SINGLE) {
                    if (token >= msgTokens.size()) {
                        return false;
                    }
                    Token msgToken = msgTokens.get(token);
                    if (!msgToken.equals(subToken)) {
                        return false;
                    }
                } else {
                    if (subToken == Token.MULTI) {
                        return true;
                    }
                    // if execution reach this point Token is a "SINGLE".
                    // Therefore skip a step forward
                }
            }

            return token == msgTokens.size();
        } catch (ParseException ex) {
            log.error("Topic format is incorrect", ex);
            throw new AndesException(ex);
        }
    }

    /**
     * Split subscribed topic name based on empty level, multi level and single level tokens
     *
     * @param topic
     * @return
     * @throws ParseException
     */
    private static List<Token> splitTopic(String topic) throws ParseException {
        List<Token> resultArray = new ArrayList<Token>();
        String[] tokens = topic.split(TOPIC_TOKEN_SEPARATOR);

        if (tokens.length == 0) {
            resultArray.add(Token.EMPTY);
        }

        if (topic.endsWith("/")) {
            //Add a fictitious space
            String[] newSplit = new String[tokens.length + 1];
            System.arraycopy(tokens, 0, newSplit, 0, tokens.length);
            newSplit[tokens.length] = "";
            tokens = newSplit;
        }

        for (int i = 0; i < tokens.length; i++) {
            String token = tokens[i];
            if (token.isEmpty()) {
                resultArray.add(Token.EMPTY);
            } else if (token.equals(MULTIPLE_LEVEL_WILDCARD)) {
                //check that multi is the last symbol
                if (i != tokens.length - 1) {
                    throw new ParseException(
                            "Bad format of topic, the multi symbol (#) has to be" +
                                    " the last one after a separator", i);
                }
                resultArray.add(Token.MULTI);
            } else if (token.contains(MULTIPLE_LEVEL_WILDCARD)) {
                throw new ParseException("Bad format of topic, invalid subtopic name: " + token, i);
            } else if (token.equals(SINGLE_LEVEL_WILDCARD)) {
                resultArray.add(Token.SINGLE);
            } else if (token.contains(SINGLE_LEVEL_WILDCARD)) {
                throw new ParseException("Bad format of topic, invalid subtopic name: " + token, i);
            } else {
                resultArray.add(new Token(token));
            }
        }

        return resultArray;
    }

    /**
     * This class will create new token objects based on different wildcard topic
     * separators.
     */
    private static class Token {

        static final Token EMPTY = new Token("");
        static final Token MULTI = new Token(MULTIPLE_LEVEL_WILDCARD);
        static final Token SINGLE = new Token(SINGLE_LEVEL_WILDCARD);
        String name;

        protected Token(String tokenName) {
            name = tokenName;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            int hash = 7;
            hash = 29 * hash + (this.name != null ? this.name.hashCode() : 0);
            return hash;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals(Object obj) {
            if (null == obj) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Token other = (Token) obj;
            return !((null == this.name) ? (null != other.name) : !this.name.equals(other.name));
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return name;
        }

        /**
         * @return Token name
         */
        protected String name() {
            return name;
        }
    }
}
