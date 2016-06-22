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

package org.wso2.carbon.andes.core.internal.cluster.coordination.hazelcast.custom.serializer.wrapper;

import org.wso2.carbon.andes.core.internal.slot.Slot;

import java.io.Serializable;
import java.util.TreeSet;

/**
 * This class is a wrapper class to a TreeSet<Slot>.It encapsulates a Treeset in order to
 * customize the serialization of a Slot treeset in hazelcast. This class is used because general
 * TreeSet class should not affect with the custom serialization.
 */
public class TreeSetSlotWrapper implements Serializable {

    private TreeSet<Slot> slotTreeSet = new TreeSet<Slot>();

    public TreeSet<Slot> getSlotTreeSet() {
        return slotTreeSet;
    }

    public void setSlotTreeSet(TreeSet<Slot> stringTreeSet) {
        this.slotTreeSet = stringTreeSet;
    }
}
