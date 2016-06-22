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

package org.wso2.carbon.andes.core.internal.disruptor;

import com.lmax.disruptor.AlertException;
import com.lmax.disruptor.Sequence;
import com.lmax.disruptor.SequenceBarrier;
import com.lmax.disruptor.WaitStrategy;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Blocking wait strategy for Disruptor ring buffer handlers. The wait strategy reduces cpu usage
 */
public class SleepingBlockingWaitStrategy implements WaitStrategy {
    private final Lock lock = new ReentrantLock();
    private final Condition processorNotifyCondition;

    public SleepingBlockingWaitStrategy() {
        this.processorNotifyCondition = this.lock.newCondition();
    }

    public long waitFor(long sequence, Sequence cursorSequence, Sequence dependentSequence, SequenceBarrier barrier)
            throws AlertException, InterruptedException {
        if (cursorSequence.get() < sequence) {
            this.lock.lock();

            try {
                while (cursorSequence.get() < sequence) {
                    barrier.checkAlert();
                    this.processorNotifyCondition.await();
                    LockSupport.parkNanos(1L);
                }
            } finally {
                this.lock.unlock();
            }
        }

        long availableSequence;
        while ((availableSequence = dependentSequence.get()) < sequence) {
            barrier.checkAlert();
            LockSupport.parkNanos(1L);
        }

        return availableSequence;
    }

    public void signalAllWhenBlocking() {
        this.lock.lock();

        try {
            this.processorNotifyCondition.signalAll();
        } finally {
            this.lock.unlock();
        }

    }
}
