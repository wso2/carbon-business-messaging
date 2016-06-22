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
import org.wso2.carbon.andes.core.internal.cluster.coordination.hazelcast.custom.serializer.wrapper.TreeSetSlotWrapper;
import org.wso2.carbon.andes.core.internal.slot.Slot;

import java.io.IOException;
import java.util.TreeSet;

/**
 * This class implements the custom serialization methods for TreeSetSlotWrapper objects.
 */
@SuppressWarnings("unused")
public class TreeSetSlotWrapperSerializer implements StreamSerializer<TreeSetSlotWrapper> {

    @Override
    public void write(ObjectDataOutput objectDataOutput, TreeSetSlotWrapper treeSetStringWrapper) throws IOException {
        //Convert the treeSetStringWrapper object to a json string and save it in hazelcast map
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{");
        TreeSet<Slot> treeSet = treeSetStringWrapper.getSlotTreeSet();
        if (treeSet != null) {
            stringBuilder.append("\"slotTreeSet\":[");
            for (Slot slot : treeSet) {
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
                        append("\",\"destinationOfMessagesInSlot\":\"").append(slot.getDestinationOfMessagesInSlot()).
                        append("\",\"states\":\"").append(slot.encodeSlotStates()).
                        append("\",\"isAnOverlappingSlot\":").append(String.valueOf(slot.isAnOverlappingSlot())).
                        append(",\"isSlotActive\":").append(isActiveString).append("},");
            }
            if (treeSet.size() != 0) {
                stringBuilder.deleteCharAt(stringBuilder.length() - 1);
            }
            stringBuilder.append("]");
        }
        stringBuilder.append("}");

        objectDataOutput.writeUTF(stringBuilder.toString());
    }

    @Override
    public TreeSetSlotWrapper read(ObjectDataInput objectDataInput) throws IOException {
        //Build treeSetStringWrapper object using json string.
        String jsonString = objectDataInput.readUTF();
        JsonArray jsonArray = new JsonParser().parse(jsonString).getAsJsonObject().getAsJsonArray
                ("slotTreeSet");
        TreeSet<Slot> treeSet = new TreeSet<>();
        for (JsonElement jsonElement : jsonArray) {
            JsonObject jsonObject = (JsonObject) jsonElement;
            Slot slot = new Slot();
            slot.setMessageCount(jsonObject.get("messageCount").getAsInt());
            slot.setStartMessageId(jsonObject.get("startMessageId").getAsLong());
            slot.setEndMessageId(jsonObject.get("endMessageId").getAsLong());
            slot.setStorageQueueName(jsonObject.get("storageQueueName").getAsString());
            slot.decodeAndSetSlotStates(jsonObject.get("states").getAsString());
            if (!jsonObject.get("isSlotActive").getAsBoolean()) {
                slot.setSlotInActive();
            }
            treeSet.add(slot);
        }
        TreeSetSlotWrapper wrapper = new TreeSetSlotWrapper();
        wrapper.setSlotTreeSet(treeSet);
        return wrapper;

    }

    @Override
    public int getTypeId() {
        return 19900129;
    }

    @Override
    public void destroy() {
        //nothing to do here
    }
}
