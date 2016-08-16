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

package org.wso2.carbon.andes.core.task;

import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Holds a single {@link Task}. Has references to the next and previous {@link TaskHolder} of the {@link TaskHolder}
 * ring
 */
final class TaskHolder<T extends Task> implements Delayed {

    /**
     * {@link Task} implementation related to this {@link TaskHolder}
     */
    private final T task;

    /**
     * Whether the task execution is disabled or not. True if disabled
     */
    private final AtomicBoolean isDisabled;

    /**
     * Whether the task is being processed at a given point in time
     */
    private AtomicBoolean isProcessing;

    /**
     * Task expiry time in milliseconds. Expired messages will be picked from the
     * {@link java.util.concurrent.DelayQueue} by the {@link TaskProcessor}s
     */
    private long expiryTime;

    /**
     * Create a {@link TaskHolder} instance with a {@link Task} implementation
     * @param task {@link Task} implementation
     */
    TaskHolder(T task) {
        this.task = task;
        this.isDisabled = new AtomicBoolean(false);
        this.isProcessing = new AtomicBoolean(false);
        expiryTime = System.currentTimeMillis();
    }

    /**
     * Execute the underlying {@link Task} implementation. Only a single thread will at a time can process the
     * underlying {@link Task}
     *
     * @return Return the hint from the {@link Task} implementation.
     * @throws Exception
     */
    final T.TaskHint executeTask() throws Exception {
        T.TaskHint hint = Task.TaskHint.IDLE;
        if (isProcessing.compareAndSet(false, true) ) {
            try {
                if (!isDisabled.get()) {
                    hint = task.call();
                }
            } finally {
                isProcessing.set(false);
            }
        }

        return hint;
    }

    /**
     * Underlying {@link Task} implementation
     * @return {@link Task}
     */
    public T getTask() {
        return task;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "Task id " + task.getId();
    }

    /**
     * Check whether the task is disabled or not. This doesn't guarantee that the {@link Task} is not running at
     * the moment. It merely check whether a flag to disable the task is set. Future processing of the {@link Task}
     * will be stopped. If there is a {@link TaskProcessor} already processing the task it will continue.
     *
     * @return True if the future processing is disabled and false otherwise
     */
    boolean isDisabled() {
        return isDisabled.get();
    }

    /**
     * Disable processing {@link Task}
     */
    void disableProcessing() {
        isDisabled.set(true);
        while (isProcessing.get()) { // Wait till the task is processed
            Thread.yield(); // Hint to the scheduler that the thread is willing to yield. (minimise thread spin)
        }
    }

    /**
     * Unique id of the {@link TaskHolder}
     * @return unique string representing the {@link TaskHolder}
     */
    String getId() {
        return task.getId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getDelay(TimeUnit unit) {
        return unit.convert(expiryTime - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * Set the expiry time for the {@link Delayed}
     * @param delay delay
     * @param timeUnit {@link TimeUnit}
     */
    void setDelay(long delay, TimeUnit timeUnit) {
        this.expiryTime = System.currentTimeMillis() + TimeUnit.MILLISECONDS.convert(delay, timeUnit);
    }

    /**
     * <p>
     * Note: this class has a natural ordering that is inconsistent with equals.
     * </p>
     * <p>
     * For the Delayed queue we need the ordering according to the delay. But to remove {@link TaskHolder} from the
     * queue we need to have the equals method according to the id of the {@link Task}
     * </p>
     *
     * {@inheritDoc}
     */
    @Override
    public int compareTo(Delayed delayedObject) {

        int order = Long.compare(expiryTime, ((TaskHolder)delayedObject).expiryTime); // order by expiryTime
        if (order == 0 ) {
            order = getId().compareTo(((TaskHolder) delayedObject).getId()); // order by string id
        }
        return order;
    }

    /**
     * <p>
     * Note: this class has a natural ordering that is inconsistent with equals.
     * </p>
     * <p>
     * For the Delayed queue we need the ordering according to the delay. But to remove {@link TaskHolder} from the
     * queue we need to have the equals method according to the id of the {@link Task}
     * </p>
     *
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        boolean isEqual;
        if ( obj instanceof TaskHolder) {
            isEqual = ((TaskHolder) obj).getId().compareTo(getId()) == 0;
        } else {
            isEqual = this.equals(obj);
        }
        return isEqual;
    }

    /**
     * Ready to remove task. Invoke the {@link Task} callback #onRemove
     */
    void onRemoveTask() {
        task.onRemove();
    }
}
