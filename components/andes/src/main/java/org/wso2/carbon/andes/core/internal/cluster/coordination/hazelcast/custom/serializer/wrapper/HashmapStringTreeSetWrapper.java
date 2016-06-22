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
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

/**
 * This class is a wrapper class to a HashMap with String and Slot Tree Set. It encapsulates a
 * hashmap in order to
 * customize
 * the serialization of a HashMap<String,TreeSet<Slot>> in hazelcast. This class is used because
 * general HashMap<T,V>
 * class should not affect with the custom serialization.
 */
public class HashmapStringTreeSetWrapper implements Serializable {

    private HashMap<String, TreeSet<Slot>> stringListHashMap;


    public HashmapStringTreeSetWrapper() {
        stringListHashMap = new HashMap<String, TreeSet<Slot>>();
    }

    public HashMap<String, TreeSet<Slot>> getStringListHashMap() {
        return stringListHashMap;
    }

    public void setStringListHashMap(HashMap<String, TreeSet<Slot>> stringListHashMap) {
        this.stringListHashMap = stringListHashMap;
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();

        sb.append("MAPP");


        if (null != stringListHashMap) {
            for (Map.Entry<String, TreeSet<Slot>> map : stringListHashMap.entrySet()) {
                sb.append("queueName : " + map.getKey());

                for (Slot slot : map.getValue()) {
                    sb.append(slot);
                }
            }
        }

        return sb.toString();
    }

}
