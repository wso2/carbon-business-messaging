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
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.andes.core.security;

import org.wso2.carbon.andes.core.internal.AndesContext;
import org.wso2.carbon.security.caas.user.core.bean.Permission;
import org.wso2.carbon.security.caas.user.core.bean.User;
import org.wso2.carbon.security.caas.user.core.exception.AuthorizationStoreException;
import org.wso2.carbon.security.caas.user.core.exception.IdentityStoreException;
import org.wso2.carbon.security.caas.user.core.store.AuthorizationStore;

import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class evaluates the user permissions that are allowed for a user when doing an action for a
 * certain queue, topic or durable topic.
 */
public class AndesAuthorizationHandler {

    /**
     * Pre-declared name for amqp "Default Exchange".
     */
    private static final String DEFAULT_EXCHANGE = "default";

    /**
     * Pre-declared name for amqp "Direct Exchange".
     */
    private static final String DIRECT_EXCHANGE = "amq.direct";

    /**
     * Pre-declared name for amqp "Topic Exchange".
     */
    private static final String TOPIC_EXCHANGE = "amq.topic";

    /**
     * Underscore character for routing key change.
     */
    private static final String AT_REPLACE_CHAR = "_";

    /**
     * This map used to handle 'consume' authorization of non durable topic
     */
    private static ConcurrentHashMap<String, String> temporaryQueueToTopicMap = new ConcurrentHashMap<>();


    public static boolean handleCreateQueue(User user) throws IdentityStoreException, AuthorizationStoreException {
        AuthorizationStore authorizationStore = AndesContext.getInstance().getRealmService()
                .getAuthorizationStore();
        Permission queuePermission = new Permission("mb:queue", "mb:add");
        Permission topicPermission = new Permission("mb:topic", "mb:" + "mb:add");
        boolean authorized = false;
        if (authorizationStore.isUserAuthorized(user.getUserId(), queuePermission, user
                .getIdentityStoreId())) {
            authorized = true;
        } else if (authorizationStore.isUserAuthorized(user.getUserId(), topicPermission, user
                .getIdentityStoreId())) {
            authorized = true;
        }
        return authorized;
    }

    public static boolean handleBindQueue(User user, String resource, Properties properties) throws
            IdentityStoreException, AuthorizationStoreException {
        boolean authorized = false;
        AuthorizationStore authorizationStore = AndesContext.getInstance().getRealmService()
                .getAuthorizationStore();
        // Bind properties
        String exchangeName =
                AndesAuthorizationHandler.getRawExchangeName((String) properties.get("exch"));
        String queueName =
                AndesAuthorizationHandler.getRawQueueName((String) properties.get("queue"));
        String routingKey =
                AndesAuthorizationHandler.getRawRoutingKey((String) properties.get("routingKey"));


        if (exchangeName.equals(DEFAULT_EXCHANGE)) {
            authorized = true;
        } else if (exchangeName.equals(DIRECT_EXCHANGE)) {
            Permission queuePermission = new Permission("mb:queue", "mb:add");
            boolean isAuthorizedToAddQueue = authorizationStore.isUserAuthorized(user.getUserId(), queuePermission, user
                    .getIdentityStoreId());
            Permission queueResourcePermission = new Permission("mb:queue/" + resource, "mb:" + AuthorizeAction
                    .SUBSCRIBE
                    .name());
            boolean isAuthorizedToSubscribeQueue = authorizationStore.isUserAuthorized(user.getUserId(),
                    queueResourcePermission,
                    user.getIdentityStoreId());
            if (isAuthorizedToAddQueue && isAuthorizedToSubscribeQueue) {
                authorized = true;
            }
        } else if (exchangeName.equals(TOPIC_EXCHANGE)) {
            Permission topicPermission = new Permission("mb:topic", "mb:add");
            boolean isAuthorizedToAddTopic = authorizationStore.isUserAuthorized(user.getUserId(), topicPermission, user
                    .getIdentityStoreId());
            if (routingKey != null) {
                Permission topicResourcePermission = new Permission("mb:topic/" + routingKey, "mb:" + AuthorizeAction
                        .SUBSCRIBE
                        .name());
                boolean isAuthorizedToSubscribeTopic = authorizationStore.isUserAuthorized(user.getUserId(),
                        topicResourcePermission,
                        user.getIdentityStoreId());
                if (isAuthorizedToAddTopic && isAuthorizedToSubscribeTopic) {
                    authorized = true;
                }
                String newRoutingKey = routingKey.replace("@", AT_REPLACE_CHAR);
                temporaryQueueToTopicMap.put(resource, newRoutingKey);
            }
        }

        return authorized;
    }


    public static boolean handleConsumeQueue(User user, String resource) throws
            IdentityStoreException, AuthorizationStoreException {
        boolean authorized;
        AuthorizationStore authorizationStore = AndesContext.getInstance().getRealmService()
                .getAuthorizationStore();
        if (temporaryQueueToTopicMap.get(resource) != null) {

            Permission topicPermission = new Permission("mb:topic/" + temporaryQueueToTopicMap.get(resource), "mb:"
                                                                                  + AuthorizeAction.SUBSCRIBE.name());
            authorized = authorizationStore.isUserAuthorized(user.getUserId(), topicPermission,
                    user.getIdentityStoreId());
        } else {
            Permission queuePermission = new Permission("mb:queue/" + resource, "mb:" +
                                                                                AuthorizeAction.SUBSCRIBE.name());
            authorized = authorizationStore.isUserAuthorized(user.getUserId(), queuePermission,
                    user.getIdentityStoreId());
        }
        return authorized;
    }

    public static boolean handleUnbindQueue(User user, String resource, Properties properties) throws
            IdentityStoreException, AuthorizationStoreException {

        boolean authorized = false;
        AuthorizationStore authorizationStore = AndesContext.getInstance().getRealmService()
                .getAuthorizationStore();
        String exchangeName =
                AndesAuthorizationHandler.getRawExchangeName((String) properties.get("exch"));
        String queueName = (String) properties.get("queue");
        if (exchangeName.equals(DEFAULT_EXCHANGE)) {
            authorized = true;
        } else if (exchangeName.equals(DIRECT_EXCHANGE)) {
            Permission queuePermission = new Permission("mb:queue", "mb:delete");
            authorized = authorizationStore.isUserAuthorized(user.getUserId(), queuePermission, user
                    .getIdentityStoreId());
        } else if (exchangeName.equals(TOPIC_EXCHANGE)) {
            Permission topicPermission = new Permission("mb:topic", "mb:delete");
            authorized = authorizationStore.isUserAuthorized(user.getUserId(), topicPermission, user
                    .getIdentityStoreId());
            if (authorized) {
                temporaryQueueToTopicMap.remove(queueName);
            }
        }
        return authorized;
    }

    public static boolean handleDeleteQueue(User user, String resource, Properties properties) throws
            IdentityStoreException, AuthorizationStoreException {

        boolean authorized = false;
        AuthorizationStore authorizationStore = AndesContext.getInstance().getRealmService()
                .getAuthorizationStore();
        return  true;

    }


    /**
     * Internally default exchange has the name <<default>> that can not be used as Registry node.
     * This method trims off leading and trailing > and < characters and returns "default"
     *
     * @param exchangeName <<default>> for the default exchange
     * @return default for <<default>>
     */
    public static String getRawExchangeName(String exchangeName) {
        return exchangeName.equals("<<default>>") ? DEFAULT_EXCHANGE : exchangeName;
    }

    /**
     * Internally durable queue names have the format [client id]:[raw queue name]. This method
     * extracts raw name from it's internal name..
     *
     * @param queueName Internal queue name
     * @return Raw queue name
     */
    public static String getRawQueueName(String queueName) {
        if (queueName.contains(";")) {
            queueName = queueName.substring(0, queueName.indexOf(";"));
        }
        return queueName.substring(queueName.indexOf(":") + 1, queueName.length());
    }

    /**
     * Internally durable queue routing keys have the format [client id]:[raw routing key]. This
     * method extracts raw name from it's internal name..
     *
     * @param routingKey Internal routing key
     * @return Raw routing key
     */
    public static String getRawRoutingKey(String routingKey) {
        String virtualHostFormatted = "carbon" + ":";
        int startIndex = !routingKey.contains(virtualHostFormatted) ? 0 : routingKey.indexOf(virtualHostFormatted);
        return routingKey.substring(startIndex, routingKey.length());
    }
}
