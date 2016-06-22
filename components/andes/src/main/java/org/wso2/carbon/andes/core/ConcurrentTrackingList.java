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

import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Concurrent List like implementation.
 * This implementation will create thread safe LinkedList.
 * Two threads won't be able to execute same operation simultaneously
 * with this LinkedList.
 *
 * @param <T> Type of list items
 */
public class ConcurrentTrackingList<T> {
    private final LinkedList<T> dataList;
    private final ReentrantReadWriteLock lock;

    public ConcurrentTrackingList() {
        dataList = new LinkedList<T>();
        lock = new ReentrantReadWriteLock();
    }

    /**
     * Returns true if this list contains the specified element.
     *
     * @param item Ite, whose presence in this list is to be tested
     * @return true if this list contains the specified element
     */
    public boolean contains(T item) {
        try {
            lock.readLock().lock();
            return dataList.contains(item);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Appends the specified element to the end of this list.
     *
     * @param item Item to be appended to this list
     */
    public void add(T item) {
        try {
            lock.writeLock().lock();
            dataList.add(item);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Removes the first occurrence of the specified element from this list, if it is present.
     * If this list does not contain the element, it is unchanged.
     *
     * @param item Item to be removed from this list, if present
     */
    public void remove(T item) {
        try {
            lock.writeLock().lock();
            dataList.remove(item);
        } finally {
            lock.writeLock().unlock();
        }
    }
}
