/*
*  Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
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

package org.wso2.carbon.stat.publisher.internal.util;

import org.wso2.carbon.databridge.commons.Attribute;
import org.wso2.carbon.databridge.commons.AttributeType;
import org.wso2.carbon.databridge.commons.StreamDefinition;
import org.wso2.carbon.databridge.commons.exception.MalformedStreamDefinitionException;
import org.wso2.carbon.stat.publisher.conf.StreamConfiguration;

import java.util.ArrayList;
import java.util.List;

public class StreamDefinitionCreator {
    public static String serverStatsStreamName = "SYSTEM_STATISTICS_MB";
    public static String serverStatsNickName = "system_statistics";
    public static String serverStatsDescription = "Publish Message broker server statistics";
    public static String mbStatsStreamName = "MB_STATISTICS_MB";
    public static String mbStatsNickName = "MB_statistics";
    public static String mbStatsDescription = "Publish Message broker topic/subscriber statistics";
    public static String messageStatsStreamName = "MESSAGE_STATISTICS_MB";
    public static String messageStatsNickName = "message statistics";
    public static String messageStatsDescription = "Publish Message broker message statistics";
    public static String ackStatsStreamName = "ACK_STATISTICS_MB";
    public static String ackStatsNickName = "ack statistics";
    public static String ackStatsDescription = "Publish Message broker acknowledgement message statistics";

    public static StreamDefinition getServerStatsStreamDef(StreamConfiguration streamConfiguration) throws
            MalformedStreamDefinitionException {
        StreamDefinition streamDefinition = new StreamDefinition(serverStatsStreamName,
                streamConfiguration.getSystemStatisticStreamVersion());
        streamDefinition.setDescription(serverStatsDescription);
        streamDefinition.setNickName(serverStatsNickName);
        streamDefinition.setMetaData(getMetaDefinitions());
        streamDefinition.setPayloadData(getServerStatsPayloadDefinition());
        streamDefinition.setCorrelationData(null);

        return streamDefinition;
    }

    public static StreamDefinition getMBStatsStreamDef(StreamConfiguration streamConfiguration) throws
            MalformedStreamDefinitionException {
        StreamDefinition streamDefinition = new StreamDefinition(mbStatsStreamName,
                streamConfiguration.getMbStatisticStreamVersion());
        streamDefinition.setDescription(mbStatsDescription);
        streamDefinition.setNickName(mbStatsNickName);
        streamDefinition.setMetaData(getMetaDefinitions());
        streamDefinition.setPayloadData(getMBStatsPayloadDefinition());
        streamDefinition.setCorrelationData(null);

        return streamDefinition;
    }

    public static StreamDefinition getMessageStatsStreamDef(StreamConfiguration streamConfiguration) throws
            MalformedStreamDefinitionException {
        StreamDefinition streamDefinition = new StreamDefinition(messageStatsStreamName,
                streamConfiguration.getMessageStreamVersion());
        streamDefinition.setDescription(messageStatsDescription);
        streamDefinition.setNickName(messageStatsNickName);
        streamDefinition.setMetaData(getMetaDefinitions());
        streamDefinition.setPayloadData(getMessageStatsPayloadDefinition());
        streamDefinition.setCorrelationData(null);

        return streamDefinition;
    }

    public static StreamDefinition getAckStatsStreamDef(StreamConfiguration streamConfiguration)
            throws MalformedStreamDefinitionException {
        StreamDefinition streamDefinition = new StreamDefinition(ackStatsStreamName,
                streamConfiguration.getAcknowledgeStreamVersion());
        streamDefinition.setDescription(ackStatsDescription);
        streamDefinition.setNickName(ackStatsNickName);
        streamDefinition.setMetaData(getMetaDefinitions());
        streamDefinition.setPayloadData(getAckStatsPayloadDefinition());
        streamDefinition.setCorrelationData(null);

        return streamDefinition;
    }

    private static ArrayList<Attribute> getMetaDefinitions() {
        ArrayList<Attribute> metaList = new ArrayList<Attribute>(1);
        metaList.add(new Attribute("publisherIP", AttributeType.STRING));
        return metaList;
    }


    private static List<Attribute> getServerStatsPayloadDefinition() {

        List<Attribute> payloadList = new ArrayList<Attribute>(4);
        payloadList.add(new Attribute("heapMemoryUsage", AttributeType.LONG));
        payloadList.add(new Attribute("nonHeapMemoryUsage", AttributeType.LONG));
        payloadList.add(new Attribute("cpuLoadAverage", AttributeType.DOUBLE));
        payloadList.add(new Attribute("timestamp", AttributeType.LONG));

        return payloadList;
    }

    private static List<Attribute> getMBStatsPayloadDefinition() {
        List<Attribute> payloadList = new ArrayList<Attribute>(3);
        payloadList.add(new Attribute("noOfSubscribers", AttributeType.INT));
        payloadList.add(new Attribute("noOfTopics", AttributeType.INT));
        payloadList.add(new Attribute("timestamp", AttributeType.LONG));

        return payloadList;
    }

    private static List<Attribute> getMessageStatsPayloadDefinition() {
        List<Attribute> payloadList = new ArrayList<Attribute>(6);
        payloadList.add(new Attribute("messageId", AttributeType.LONG));
        payloadList.add(new Attribute("destination", AttributeType.STRING));
        payloadList.add(new Attribute("messageContentLength", AttributeType.INT));
        payloadList.add(new Attribute("expirationTime", AttributeType.LONG));
        payloadList.add(new Attribute("noOfSubscriptions", AttributeType.INT));
        payloadList.add(new Attribute("timestamp", AttributeType.LONG));

        return payloadList;
    }

    private static List<Attribute> getAckStatsPayloadDefinition() {
        List<Attribute> payloadList = new ArrayList<Attribute>(3);
        payloadList.add(new Attribute("messageId", AttributeType.LONG));
        payloadList.add(new Attribute("queueName", AttributeType.STRING));
        payloadList.add(new Attribute("timestamp", AttributeType.LONG));

        return payloadList;
    }

}
