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

package org.wso2.carbon.andes.core.internal.cluster.coordination.hazelcast.custom.serializer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import org.wso2.carbon.andes.core.internal.cluster.coordination.hazelcast.custom.serializer.wrapper
        .HashmapStringTreeSetWrapper;
import org.wso2.carbon.andes.core.internal.slot.Slot;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Class used to serialize/un-serialize a HashMap<String,TreeSet<Slot>> data structure.
 */
@SuppressWarnings("unused")
public class HashMapStringTreeSetWrapperSerializer implements
                                                   StreamSerializer<HashmapStringTreeSetWrapper> {


    @Override
    public void write(ObjectDataOutput objectDataOutput, HashmapStringTreeSetWrapper
            hashmapStringTreeSetWrapper) throws IOException {
        //Convert the hashmapStringListWrapper object to a json string and save it in hazelcast map
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{");
        HashMap<String, TreeSet<Slot>> hashmap = hashmapStringTreeSetWrapper.getStringListHashMap();
        stringBuilder.append("\"stringTreeSetHashMap\":{");
        if (hashmap != null) {
            Set<Map.Entry<String, TreeSet<Slot>>> entrySet = hashmap.entrySet();
            for (Map.Entry<String, TreeSet<Slot>> entry : entrySet) {
                stringBuilder.append("\"").append(entry.getKey()).append("\":[");
                TreeSet<Slot> slots = entry.getValue();
                if (slots != null) {
                    for (Slot slot : slots) {
                        String isActiveString;
                        if (slot.isSlotActive()) {
                            isActiveString = "true";
                        } else {
                            isActiveString = "false";
                        }
                        stringBuilder.append("{\"messageCount\":").append(slot.getMessageCount()).
                                append(",").append("\"startMessageId\":").append(slot.getStartMessageId()).
                                append(",").append("\"endMessageId\":").append(slot.getEndMessageId()).
                                append(",\"storageQueueName\":\"").append(slot.getStorageQueueName()).
                                append("\",\"destinationOfMessagesInSlot\":\"").append(
                                slot.getDestinationOfMessagesInSlot()).
                                append("\",\"states\":\"").append(slot.encodeSlotStates()).
                                append("\",\"isAnOverlappingSlot\":").append(
                                String.valueOf(slot.isAnOverlappingSlot())).
                                append(",\"isSlotActive\":").append(isActiveString).append("},");
                    }
                    if (slots.size() != 0) {
                        stringBuilder.deleteCharAt(stringBuilder.length() - 1);
                    }
                }
                stringBuilder.append("],");
            }
            if (hashmap.keySet().size() != 0) {
                stringBuilder.deleteCharAt(stringBuilder.length() - 1);
            }

        }
        stringBuilder.append("}");
        stringBuilder.append("}");
        objectDataOutput.writeUTF(stringBuilder.toString());
    }

    @Override
    public HashmapStringTreeSetWrapper read(ObjectDataInput objectDataInput) throws IOException {
        //Build HashmapStringListWrapper object using json string.
        String jsonString = objectDataInput.readUTF();
        HashmapStringTreeSetWrapper wrapper = new HashmapStringTreeSetWrapper();
        HashMap<String, TreeSet<Slot>> hashMap = new HashMap<>();
        JsonObject jsonObject = new JsonParser().parse(jsonString).getAsJsonObject()
                .getAsJsonObject("stringTreeSetHashMap");
        Set<Map.Entry<String, JsonElement>> set = jsonObject.entrySet();
        for (Map.Entry<String, JsonElement> entry : set) {
            String key = entry.getKey();
            JsonElement value = entry.getValue();
            TreeSet<Slot> arrayList = new TreeSet<>();
            JsonArray jsonArray = value.getAsJsonArray();
            for (JsonElement elem : jsonArray) {
                Slot slot = new Slot();
                JsonObject jsonObjectForSlot = (JsonObject) elem;
                slot.setMessageCount(jsonObjectForSlot.get("messageCount").getAsInt());
                slot.setStartMessageId(jsonObjectForSlot.get("startMessageId").getAsLong());
                slot.setEndMessageId(jsonObjectForSlot.get("endMessageId").getAsLong());
                slot.setStorageQueueName(jsonObjectForSlot.get("storageQueueName").getAsString());
                slot.decodeAndSetSlotStates(jsonObjectForSlot.get("states").getAsString());
                if (!jsonObjectForSlot.get("isSlotActive").getAsBoolean()) {
                    slot.setSlotInActive();
                }
                arrayList.add(slot);
            }
            hashMap.put(key, arrayList);
        }
        wrapper.setStringListHashMap(hashMap);
        return wrapper;
    }

    @Override
    public int getTypeId() {
        return 19900130;
    }

    @Override
    public void destroy() {
        //nothing to do here
    }
}
