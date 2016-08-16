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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Manage processing of {@link Task}. Holds the {@link TaskHolder} ring and the {@link TaskProcessor} list that
 * process the {@link Task}
 */
public final class TaskExecutorService<T extends Task> {

    /**
     * Logger
     */
    private static Log log = LogFactory.getLog(TaskExecutorService.class);

    /**
     * {@link DelayQueue} used by processors to schedule tasks. Idle task will be processed after a delay
     */
    private final DelayQueue<TaskHolder> taskHolderDelayQueue;

    /**
     * Mapping of registered tasks with its task id
     */
    private final Map<String, TaskHolder<T>> taskHolderRegistry;

    /**
     * Maximum number of {@link TaskProcessor} instances processing the {@link Task}s
     */
    private final int workerCount;

    /**
     * Thread executor service for {@link TaskProcessor}s
     */
    private final ExecutorService taskExecutorPool;

    /**
     * Queue containing the current running {@link TaskProcessor}s
     */
    private final Queue<TaskProcessor> taskProcessorQueue;

    /**
     * Executor service to process add remove requests from the editRequestQueue
     */
    private final ExecutorService taskUpdateExecutorService;

    /**
     * Exception handler implementation defining how to handle the exceptions
     */
    private TaskExceptionHandler taskExceptionHandler;

    /**
     * Delay for processing IDLE tasks in the next iteration
     */
    private long idleTaskDelayMillis;

    /**
     * Create a Task manager with a given number of threads to process the tasks
     *
     * @param workerCount maximum number of threads spawned to process the tasks
     * @param idleTaskDelayMillis delay set for processing a task with IDLE {@link org.wso2.andes.task.Task.TaskHint}
     * @param threadFactory  thread factory to be used for processing the tasks
     */
    public TaskExecutorService(int workerCount, long idleTaskDelayMillis, ThreadFactory threadFactory) {

        taskExecutorPool = Executors.newFixedThreadPool(workerCount, threadFactory);
        this.workerCount = workerCount;
        taskProcessorQueue = new ArrayDeque<>(workerCount);
        taskUpdateExecutorService = Executors.newSingleThreadExecutor(threadFactory);
        taskExceptionHandler = new DefaultExceptionHandler();
        taskHolderDelayQueue = new DelayQueue<>();
        taskHolderRegistry = new ConcurrentHashMap<>();
        this.idleTaskDelayMillis = idleTaskDelayMillis;
    }

    /**
     * Add a new task. If the task is already added (same id) task add request will be ignored.
     * <p>
     * This add {@link Task} request is processed asynchronously
     *
     * @param task {@link Task}
     */
    public void add(T task) {
        if (log.isDebugEnabled()) {
            log.debug("Task add request " + task.getId());
        }
        taskUpdateExecutorService.submit(new AddRequest(task));
    }

    /**
     * Removes the {@link Task} with the given task id from the {@link Task} ring
     *
     * @param id  ID of the{@link Task} to be removed
     */
    public void remove(String id) {
        if (log.isDebugEnabled()) {
            log.debug("Task remove request. Task id" + id);
        }
        taskUpdateExecutorService.submit(new RemoveRequest(id));
    }

    /**
     * Returns the {@link Task} implementation relevant to the task id
     *
     * @param taskId id of the {@link Task} implementation
     * @return Task implementation
     */
    public T getTask(String taskId) {
        TaskHolder<T> taskHolder = taskHolderRegistry.get(taskId);
        if (taskHolder != null ) {
            return taskHolder.getTask();
        }
        return null;
    }

    /**
     * Stop processing the tasks
     */
    public synchronized void stop() {
        log.info("Stopping task manager. Task count " + taskHolderDelayQueue.size());
        for (TaskProcessor taskProcessor : taskProcessorQueue) {
            taskProcessor.deactivate();
        }
        taskProcessorQueue.clear();
    }

    /**
     * Start processing the tasks
     */
    public synchronized void start() {
        log.info("Starting task manager. Task count " + taskHolderDelayQueue.size());

        for (int i = 0; i < workerCount; i++) {
            TaskProcessor taskProcessor =
                    new TaskProcessor(taskHolderDelayQueue, taskExceptionHandler, idleTaskDelayMillis);
            taskProcessorQueue.add(taskProcessor);
            taskExecutorPool.submit(taskProcessor);
        }
    }

    /**
     * Set the exception handler for the task processors
     *
     * @param exceptionHandler {@link TaskExceptionHandler}
     */
    public void setExceptionHandler(TaskExceptionHandler exceptionHandler) {
        this.taskExceptionHandler = exceptionHandler;
    }

    /**
     * Task add request
     */
    private class AddRequest implements Runnable {

        private T task;

        AddRequest(T task) {
            this.task = task;
        }

        @Override
        public void run() {
            try {
                if (taskHolderRegistry.containsKey(task.getId())) {
                    return;
                }
                TaskHolder<T> taskHolder = new TaskHolder<>(task);
                task.onAdd(); // Invoke task callback before adding the task to the taskHolderDelayQueue
                              // to be processed
                taskHolderRegistry.put(task.getId(), taskHolder);
                taskHolderDelayQueue.add(taskHolder);
                if (log.isDebugEnabled()) {
                    log.debug("Task added. ID " + task.getId() + " Total Tasks " + taskHolderDelayQueue.size());
                }
            } catch (Throwable e) {
                log.error("Error occurred while adding Task " + task, e);
            }
        }
    }

    /**
     * Task remove request
     */
    private class RemoveRequest implements Runnable {

        private String id;

        RemoveRequest(String id) {
            this.id = id;
        }

        @Override
        public void run() {
            try {
                TaskHolder taskHolder = taskHolderRegistry.remove(id);
                taskHolder.disableProcessing(); // disable processors from processing the task
                if (log.isDebugEnabled()) {
                    log.debug("Task removed. ID " + taskHolder.getId() + " Total tasks " + taskHolderDelayQueue.size());
                }
            } catch (Throwable e) {
                log.error("Error occurred while removing task. Task id " + id, e);
            }
        }
    }

    /**
     * Default exception handler that throws a runtime exception and logs the event
     */
    private static class DefaultExceptionHandler implements TaskExceptionHandler {

        private static Log log = LogFactory.getLog(DefaultExceptionHandler.class);

        @Override
        public void handleException(Throwable throwable, String id) {
            log.error("Error occurred while processing task. Task id " + id, throwable);
            throw new RuntimeException(throwable);
        }
    }
}
