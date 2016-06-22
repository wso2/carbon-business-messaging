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

package org.wso2.carbon.andes.core.internal.slot;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.andes.core.AndesException;

import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * This Runnable will calculate safe zone for the cluster time to time.
 * In normal cases, this should run as long as the Slot manager node is alive
 */
public class SlotDeleteSafeZoneCalc implements Runnable {

    private static Log log = LogFactory.getLog(SlotDeleteSafeZoneCalc.class);

    private AtomicLong slotDeleteSafeZone;

    private boolean running;

    private boolean isLive;

    private int seekInterval;

    private static final int TIME_TO_SLEEP_ON_ERROR = 15 * 1000;


    /**
     * Define a safe zone calculator. This will run once seekInterval
     * When created by default it is marked as live
     *
     * @param seekInterval interval in milliseconds calculation should run
     */
    public SlotDeleteSafeZoneCalc(int seekInterval) {
        this.seekInterval = seekInterval;
        this.running = true;
        this.isLive = true;
        this.slotDeleteSafeZone = new AtomicLong(Long.MAX_VALUE);

    }

    @Override
    public void run() {
        if (log.isDebugEnabled()) {
            log.debug("Slot deletion safe zone calculation started.");
        }
        while (running) {
            if (isLive()) {
                try {
                    Set<String> nodesWithPublishedMessages;
                    try {
                        nodesWithPublishedMessages = SlotManagerClusterMode.getInstance().getMessagePublishedNodes();

                    } catch (AndesException e) {
                        log.error("SlotDeleteSafeZoneCalc stopped due to failing to get message published nodes. "
                                          + "Retrying after 15 seconds", e);
                        try {
                            Thread.sleep(TIME_TO_SLEEP_ON_ERROR);
                        } catch (InterruptedException ignore) {
                        }
                        continue;
                    }

                    /** calculate safe zone (minimum value of messageIDs published so far to the
                     * cluster by each node)
                     */
                    long globalSafeZoneVal = Long.MAX_VALUE;
                    for (String nodeID : nodesWithPublishedMessages) {

                        long safeZoneValue = Long.MAX_VALUE;

                        //get the maximum message id published by node so far
                        Long safeZoneByPublishedMessages;
                        try {
                            safeZoneByPublishedMessages = SlotManagerClusterMode.getInstance()
                                    .getLocalSafeZone(nodeID);

                        } catch (AndesException e) {
                            log.error(
                                    "SlotDeleteSafeZoneCalc stopped due to failing to get last published id for node:" +
                                            nodeID + ". Retrying after 15 seconds", e);
                            try {
                                Thread.sleep(TIME_TO_SLEEP_ON_ERROR);
                            } catch (InterruptedException ignore) {
                            }
                            continue;
                        }

                        if (null != safeZoneByPublishedMessages) {
                            safeZoneValue = safeZoneByPublishedMessages;
                        }

                        globalSafeZoneVal = Math.min(globalSafeZoneVal, safeZoneValue);
                    }

                    slotDeleteSafeZone.set(globalSafeZoneVal);

                    if (log.isDebugEnabled()) {
                        log.debug("Safe Zone Calculated : " + slotDeleteSafeZone);
                    }

                    try {
                        Thread.sleep(seekInterval);
                    } catch (InterruptedException ignore) {
                    }

                } catch (Throwable e) {
                    log.error("Error occurred while calculating safe zone at coordinator", e);
                }
            } else {
                try {
                    Thread.sleep(TIME_TO_SLEEP_ON_ERROR);
                } catch (InterruptedException ignore) {
                }
            }

        }
        log.info("Slot delete safe zone calculator stopped. Global safe zone value " + slotDeleteSafeZone.get());
    }

    /**
     * Get slot deletion safe zone calculated in last iteration
     *
     * @return current clot deletion safe zone
     */
    public long getSlotDeleteSafeZone() {
        return slotDeleteSafeZone.get();
    }

    /**
     * Specifically set slot deletion safe zone
     *
     * @param slotDeleteSafeZone safe zone value to be set
     */
    public void setSlotDeleteSafeZone(long slotDeleteSafeZone) {
        this.slotDeleteSafeZone.set(slotDeleteSafeZone);
    }

    /**
     * Check if safe zone calculator is running
     *
     * @return true if calc is running
     */
    public boolean isRunning() {
        return running;
    }

    /**
     * Define if the calc thread should run. When staring the thread
     * this should be set to true. Setting false will destroy the calc
     * thread.
     *
     * @param running if the calc thread should run
     */
    public void setRunning(boolean running) {
        this.running = running;
    }

    /**
     * Check if calc thread is doing calculations actively.
     *
     * @return if calc is doing calculations.
     */
    public boolean isLive() {
        return isLive;
    }

    /**
     * Define if calc thread should do calculations. Setting to false
     * will not destroy thread but will stop calculations.
     *
     * @param isLive set if calc should do calculations
     */
    public void setLive(boolean isLive) {
        this.isLive = isLive;
    }

    /**
     * Set the interval calc thread is running
     *
     * @return seek interval
     */
    public int getSeekInterval() {
        return seekInterval;
    }

    /**
     * Set interval calc thread should run. Calculating safe zone will be done
     * once this interval. Set in milliseconds.
     *
     * @param seekInterval interval in milliseconds.
     */
    public void setSeekInterval(int seekInterval) {
        this.seekInterval = seekInterval;
    }
}
