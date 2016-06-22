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
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.StreamSerializer;
import org.wso2.carbon.andes.core.internal.cluster.coordination.hazelcast.custom.serializer.wrapper.TreeSetLongWrapper;

import java.io.IOException;
import java.util.TreeSet;

/**
 * This class implements the custom serialization methods for TreeSetLongWrapper objects.
 */
@SuppressWarnings("unused")
public class TreeSetLongWrapperSerializer implements StreamSerializer<TreeSetLongWrapper> {


    @Override
    public void write(ObjectDataOutput objectDataOutput, TreeSetLongWrapper treeSetLongWrapper)
            throws IOException {
        //Convert the TreeSetLongWrapper object to a json string and save it in hazelcast map
        JsonArray jsonArray = new JsonArray();
        for (long elem : treeSetLongWrapper.getLongTreeSet()) {
            JsonPrimitive jsonPrimitive = new JsonPrimitive(elem);
            jsonArray.add(jsonPrimitive);
        }
        objectDataOutput.writeUTF(jsonArray.toString());
    }

    @Override
    public TreeSetLongWrapper read(ObjectDataInput objectDataInput) throws IOException {
        //Build TreeSetLongWrapper object using json string.
        String jsonString = objectDataInput.readUTF();
        JsonArray jsonArray = new JsonParser().parse(jsonString).getAsJsonArray();
        TreeSet<Long> treeSet = new TreeSet<Long>();
        for (JsonElement jsonElement : jsonArray) {
            treeSet.add(Long.valueOf(jsonElement.toString()));
        }
        TreeSetLongWrapper wrapper = new TreeSetLongWrapper();
        wrapper.setLongTreeSet(treeSet);
        return wrapper;
    }

    @Override
    public int getTypeId() {
        return 19900128;
    }

    @Override
    public void destroy() {
        //nothing to do here
    }
}
