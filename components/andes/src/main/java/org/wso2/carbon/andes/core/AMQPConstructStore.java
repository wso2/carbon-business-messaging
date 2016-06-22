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

import org.wso2.carbon.andes.core.store.AndesContextStore;
import org.wso2.carbon.andes.core.store.MessageStore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This class keep track of exchanges/queues/bindings
 * Also it updates the database as necessary when changes happen to
 * above constructs if change is local. If change is not local
 * only in-memory maps will be synced
 */
public class AMQPConstructStore {
    /**
     * Wildcard character to include all.
     */
    private static final String ALL_WILDCARD = "*";

    /**
     * Reference to AndesContextStore to manage exchanges/bindings and queues in persistence storage
     */
    private AndesContextStore andesContextStore;

    /**
     * Reference to message store to be used from message count related functionality
     */
    private MessageStore messageStore;

    private Map<String, AndesQueue> andesQueues = new HashMap<>();
    private Map<String, AndesExchange> andesExchanges = new HashMap<>();

    //keeps bindings <exchange>,<queue,binding>
    private Map<String, Map<String, AndesBinding>> andesBindings = new HashMap<>();


    public AMQPConstructStore(AndesContextStore contextStore, MessageStore messageStore) throws AndesException {
        this.andesContextStore = contextStore;
        this.messageStore = messageStore;
    }

    /**
     * Add an exchange
     *
     * @param exchange exchange to store
     * @param isLocal  is this a local change
     * @throws AndesException
     */
    public void addExchange(AndesExchange exchange, boolean isLocal) throws AndesException {
        if (isLocal) {
            andesContextStore.storeExchangeInformation(exchange.exchangeName, exchange.encodeAsString());
        }
        andesExchanges.put(exchange.exchangeName, exchange);
    }


    /**
     * remove an exchange
     *
     * @param exchangeName name of the exchange to remove
     * @param isLocal      is this a local change
     * @throws AndesException
     */
    public void removeExchange(String exchangeName, boolean isLocal) throws AndesException {
        if (isLocal) {
            andesContextStore.deleteExchangeInformation(exchangeName);
        }
        andesExchanges.remove(exchangeName);
    }

    /**
     * read all exchanges saved
     *
     * @return list of exchanges
     * @throws AndesException
     */
    public List<AndesExchange> getExchanges() throws AndesException {
        return new ArrayList<>(andesExchanges.values());
    }

    /**
     * read all exchange names
     *
     * @return list of exchange names
     * @throws AndesException
     */
    public List<String> getExchangeNames() throws AndesException {
        return new ArrayList<>(andesExchanges.keySet());
    }

    /**
     * store a queue
     *
     * @param queue   queue to be stored
     * @param isLocal is this a local change
     * @throws AndesException
     */
    public void addQueue(AndesQueue queue, boolean isLocal) throws AndesException {
        if (isLocal) {
            andesContextStore.storeQueueInformation(queue.queueName, queue.encodeAsString());
            //create a space to keep message counter on this queue
            messageStore.addQueue(queue.queueName);
        }
        andesQueues.put(queue.queueName, queue);
    }

    /**
     * remove a queue
     *
     * @param queueName name of the queue to be removed
     * @throws AndesException
     */
    public void removeQueue(String queueName) throws AndesException {

        // Remove the queue from internal maps
        removeLocalQueueData(queueName);

        // Remove queue information from database
        andesContextStore.deleteQueueInformation(queueName);
        messageStore.removeQueue(queueName);
    }

    /**
     * remove a queue from local maps
     *
     * @param queueName name of the queue to be removed
     * @throws AndesException
     */
    public void removeLocalQueueData(String queueName) throws AndesException {
        andesQueues.remove(queueName);
        messageStore.removeLocalQueueData(queueName);
    }

    /**
     * Search for a specific queue.
     *
     * @param searchKeyword Search keyword for queue name. If "*", all queues are returned. Else, queues name that
     *                      <strong>contains</strong> the value are returned.
     * @return A list of queues
     * @throws AndesException
     */
    public List<AndesQueue> getQueues(String searchKeyword) throws AndesException {
        List<AndesQueue> searchedAndesQueues;
        if (null == searchKeyword) {
            searchedAndesQueues = new ArrayList<>();
        } else if (ALL_WILDCARD.equals(searchKeyword)) {
            searchedAndesQueues = new ArrayList<>(andesQueues.values());
        } else {
            searchedAndesQueues = andesQueues.values()
                    .stream()
                    .filter(andesQueue -> andesQueue.queueName.contains(searchKeyword))
                    .collect(Collectors.toList());
        }

        return searchedAndesQueues;
    }

    /**
     * Gets a specific queue as {@link AndesQueue}.
     *
     * @param queueName The name of the queue.
     * @return A queue if exists, else null is returned.
     * @throws AndesException
     */
    public AndesQueue getQueue(String queueName) throws AndesException {
        AndesQueue searchedAndesQueue = null;
        for (AndesQueue andesQueue : andesQueues.values()) {
            if (andesQueue.queueName.equals(queueName)) {
                searchedAndesQueue = andesQueue;
            }
        }
        return searchedAndesQueue;
    }

    /**
     * read all queue names
     *
     * @return a list of queue names
     * @throws AndesException
     */
    public List<String> getQueueNames() throws AndesException {
        return new ArrayList<>(andesQueues.keySet());
    }

    /**
     * store binding
     *
     * @param binding binding to be stored
     * @param isLocal is this a local change
     * @throws AndesException
     */
    public void addBinding(AndesBinding binding, boolean isLocal) throws AndesException {
        if (isLocal) {
            andesContextStore.storeBindingInformation(binding.boundExchangeName, binding.boundQueue.queueName,
                                                      binding.encodeAsString());
        }
        if (andesBindings.get(binding.boundExchangeName) != null) {
            (andesBindings.get(binding.boundExchangeName)).put(binding.boundQueue.queueName, binding);
        } else {
            Map<String, AndesBinding> tempBindingMap = new HashMap<>();
            tempBindingMap.put(binding.boundQueue.queueName, binding);
            andesBindings.put(binding.boundExchangeName, tempBindingMap);
        }
    }

    /**
     * remove binding
     *
     * @param exchangeName name of the exchange name of binding
     * @param queueName    name of the queue binding carries
     * @param isLocal      is this a local change
     * @throws AndesException
     */
    public void removeBinding(String exchangeName, String queueName, boolean isLocal) throws AndesException {
        if (isLocal) {
            andesContextStore.deleteBindingInformation(exchangeName, queueName);
        }
        if ((andesBindings.get(exchangeName)).get(queueName) != null) {
            (andesBindings.get(exchangeName)).remove(queueName);
        }
        if (andesBindings.get(exchangeName).isEmpty()) {
            andesBindings.remove(exchangeName);
        }
    }

    /**
     * get bindings belonging to an exchange
     *
     * @param exchange name of exchange
     * @return a list of bindings
     * @throws AndesException
     */
    public List<AndesBinding> getBindingsForExchange(String exchange) throws AndesException {
        List<AndesBinding> bindings = new ArrayList<>();
        if (andesBindings.get(exchange) != null) {
            bindings.addAll((andesBindings.get(exchange)).values());
        }
        return bindings;
    }

    /**
     * get all routing keys of bindings belonging to an exchange
     *
     * @param exchange name of the exchange
     * @return a list of routing keys
     * @throws AndesException
     */
    public List<String> getRoutingKeys(String exchange) throws AndesException {
        List<String> routingKeys = new ArrayList<>();
        List<AndesBinding> bindings = getBindingsForExchange(exchange);
        routingKeys.addAll(bindings
                                   .stream()
                                   .map(b -> b.routingKey)
                                   .collect(Collectors.toList()));
        return routingKeys;
    }

}
