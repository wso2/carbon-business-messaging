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

import java.util.concurrent.Callable;

/**
 *
 * This task will be processed by {@link TaskExecutorService} using {@link TaskProcessor} in a round robin manner
 *
 */
public abstract class Task implements Callable<Task.TaskHint> {

    /**
     * Hint for the {@link TaskProcessor} about the task execution
     */
    protected enum TaskHint {
        /**
         * Task ran as expected and can be re queued to be preprocessed immediately
         */
        ACTIVE,

        /**
         * Task didn't do any productive work, hence can be delayed for the next iteration
         */
        IDLE
    }

    /**
     * Callback invoked when the {@link Task} implementation is added to the internal task queue. If the task is
     * a duplicate entry this method won't get invoked.
     */
    public abstract void onAdd();

    /**
     * Callback invoked when the {@link Task} is removed by the {@link TaskExecutorService}
     */
    public abstract void onRemove();

    /**
     * Unique id for the Task. If the id is not unique and there is an existing {@link Task} in {@link TaskExecutorService}
     * this {@link Task} won't be processed
     *
     * @return unique id {@link String}
     */
    public abstract String getId();

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean equals(Object obj) {
        if (obj instanceof Task) {
            Task task = (Task) obj;
            return getId().equals(task.getId());
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final int hashCode() {
        return getId().hashCode();
    }
}
