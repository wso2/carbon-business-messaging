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


import org.apache.commons.lang.NotImplementedException;

import java.util.EnumSet;

/**
 * This enum defines the states of slots
 */
public enum SlotState {

    CREATED(1),
    ASSIGNED(2),
    OVERLAPPED(3),
    RETURNED(4),
    DELETED(5);

    private int code;

    //keep next possible states
    private EnumSet<SlotState> next;

    //keep previous possible states
    private EnumSet<SlotState> previous;

    /**
     * Define a slot state
     *
     * @param code integer representing state
     */
    SlotState(int code) {
        this.code = code;
    }

    /**
     * Get code of the state
     *
     * @return integer representing state
     */
    public int getCode() {
        return code;
    }

    /**
     * Get Slot state from id
     *
     * @param id of the Slot state
     * @return Slot state
     */
    public static SlotState getById(int id) {
        switch (id) {
            case 1:
                return CREATED;
            case 2:
                return ASSIGNED;
            case 3:
                return OVERLAPPED;
            case 4:
                return RETURNED;
            case 5:
                return DELETED;
            default:
                throw new NotImplementedException();
        }
    }

    /**
     * Check if submitted state is an allowed state as per state model
     *
     * @param nextState suggested next state to transit
     * @return if transition is valid
     */
    public boolean isValidNextTransition(SlotState nextState) {

        return next.contains(nextState);

    }

    /**
     * Check if submitted state is an allowed state as per state model
     *
     * @param previousState suggested next state to transit
     * @return if transition is valid
     */
    public boolean isValidPreviousState(SlotState previousState) {
        return previous.contains(previousState);
    }

    static SlotState parseSlotState(int state) {

        for (SlotState s : SlotState.values()) {
            if (s.code == state) {
                return s;
            }
        }

        throw new IllegalArgumentException("Invalid slot state agument speicified: " + state);
    }

    static {

        CREATED.next = EnumSet.of(ASSIGNED);
        CREATED.previous = EnumSet.complementOf(EnumSet.allOf(SlotState.class));

        ASSIGNED.next = EnumSet.of(OVERLAPPED, DELETED, RETURNED);
        ASSIGNED.previous = EnumSet.of(CREATED, RETURNED);

        OVERLAPPED.next = EnumSet.of(ASSIGNED, OVERLAPPED);
        OVERLAPPED.previous = EnumSet.of(ASSIGNED, OVERLAPPED);

        RETURNED.next = EnumSet.of(ASSIGNED, OVERLAPPED);
        RETURNED.previous = EnumSet.of(ASSIGNED, OVERLAPPED);

        DELETED.next = EnumSet.complementOf(EnumSet.allOf(SlotState.class));
        DELETED.previous = EnumSet.of(ASSIGNED);

    }

}
