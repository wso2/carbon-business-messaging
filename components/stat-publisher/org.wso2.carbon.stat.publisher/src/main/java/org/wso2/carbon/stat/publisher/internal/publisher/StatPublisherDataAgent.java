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

package org.wso2.carbon.stat.publisher.internal.publisher;

import org.wso2.andes.kernel.*;
import org.wso2.andes.subscription.SubscriptionStore;
import org.wso2.carbon.databridge.agent.thrift.exception.AgentException;
import org.wso2.carbon.databridge.agent.thrift.lb.DataPublisherHolder;
import org.wso2.carbon.databridge.agent.thrift.lb.LoadBalancingDataPublisher;
import org.wso2.carbon.databridge.agent.thrift.lb.ReceiverGroup;
import org.wso2.carbon.databridge.commons.StreamDefinition;
import org.wso2.carbon.databridge.commons.exception.MalformedStreamDefinitionException;
import org.wso2.carbon.stat.publisher.conf.JMXConfiguration;
import org.wso2.carbon.stat.publisher.conf.StatPublisherConfiguration;
import org.wso2.carbon.stat.publisher.conf.StreamConfiguration;
import org.wso2.carbon.stat.publisher.exception.StatPublisherRuntimeException;
import org.wso2.carbon.stat.publisher.internal.util.StreamDefinitionCreator;
import org.wso2.carbon.stat.publisher.internal.util.SystemStatsReader;

import javax.management.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class StatPublisherDataAgent {

    private StatPublisherConfiguration statPublisherConfiguration;
    private SystemStatsReader systemStatReader = null;
    private StreamDefinition serverStatsStreamDef;
    private StreamDefinition mbStatsStreamDef;
    private StreamDefinition messageStatsStreamDef;
    private StreamDefinition ackStatsStreamDef;

    public LoadBalancingDataPublisher getLoadBalancingDataPublisher() {
        return loadBalancingDataPublisher;
    }

    private LoadBalancingDataPublisher loadBalancingDataPublisher;
    private List<Object> metaData;
    private List<Object> payLoadData;
    private SubscriptionStore subscriptionStore;


    public StatPublisherDataAgent(JMXConfiguration jmxConfiguration,
                                  StreamConfiguration streamConfiguration,
                                  StatPublisherConfiguration statPublisherConfiguration) {

        //set configurations
        this.statPublisherConfiguration = statPublisherConfiguration;

        try {
            //creating stream definitions
            serverStatsStreamDef = StreamDefinitionCreator.getServerStatsStreamDef(streamConfiguration);
            mbStatsStreamDef = StreamDefinitionCreator.getMBStatsStreamDef(streamConfiguration);
            messageStatsStreamDef = StreamDefinitionCreator.getMessageStatsStreamDef(streamConfiguration);
            ackStatsStreamDef = StreamDefinitionCreator.getAckStatsStreamDef(streamConfiguration);
        } catch (MalformedStreamDefinitionException e) {
            throw new StatPublisherRuntimeException(e);
        }


        ArrayList<ReceiverGroup> allReceiverGroups = new ArrayList<ReceiverGroup>();
        ArrayList<DataPublisherHolder> dataPublisherHolders = new ArrayList<DataPublisherHolder>();
        String[] urls = statPublisherConfiguration.getURL().split(",");

        for (String aUrl : urls) {
            System.out.println(aUrl);
            DataPublisherHolder aNode = new DataPublisherHolder(null, aUrl.trim(),
                    statPublisherConfiguration.getUsername(), statPublisherConfiguration.getPassword());
            dataPublisherHolders.add(aNode);
        }

        ReceiverGroup group = new ReceiverGroup(dataPublisherHolders);
        allReceiverGroups.add(group);

        loadBalancingDataPublisher = new LoadBalancingDataPublisher(allReceiverGroups);

        //adding Stream definitions to publisher
        loadBalancingDataPublisher.addStreamDefinition(serverStatsStreamDef);
        loadBalancingDataPublisher.addStreamDefinition(mbStatsStreamDef);
        loadBalancingDataPublisher.addStreamDefinition(messageStatsStreamDef);
        loadBalancingDataPublisher.addStreamDefinition(ackStatsStreamDef);

        metaData = constructMetaData();

        //get server statistics
        systemStatReader = new SystemStatsReader(jmxConfiguration);
    }

    public void sendSystemStats() throws MalformedObjectNameException, ReflectionException, IOException,
            InstanceNotFoundException, AttributeNotFoundException, MBeanException, AgentException {
        if (systemStatReader.connection != null) {
            payLoadData = getServerStatsPayLoadData(systemStatReader.getMbeansStatsData());

            loadBalancingDataPublisher.publish(serverStatsStreamDef.getName(), serverStatsStreamDef.getVersion(),
                    getObjectArray(metaData), null,
                    getObjectArray(payLoadData));
        }

    }

    public void sendMBStats() throws MalformedObjectNameException, ReflectionException, IOException,
            InstanceNotFoundException, AttributeNotFoundException, MBeanException {

        try {
            payLoadData = getMBStatsPayLoadData();
        } catch (Exception e) {
            throw new StatPublisherRuntimeException(e);
        }

        try {
            loadBalancingDataPublisher.publish(mbStatsStreamDef.getName(), mbStatsStreamDef.getVersion(),
                    getObjectArray(metaData), null,
                    getObjectArray(payLoadData));
        } catch (AgentException e) {
            throw new StatPublisherRuntimeException(e);
        }


    }

    public void sendMessageStats(AndesMessageMetadata message, int subscribers)
            throws MalformedObjectNameException, ReflectionException, IOException,
            InstanceNotFoundException, AttributeNotFoundException, MBeanException {

        try {
            payLoadData = getMessageStatsPayLoadData(message, subscribers);
        } catch (Exception e) {
            throw new StatPublisherRuntimeException(e);
        }
        try {
            loadBalancingDataPublisher.publish(messageStatsStreamDef.getName(), messageStatsStreamDef.getVersion(),
                    getObjectArray(metaData), null,
                    getObjectArray(payLoadData));
        } catch (AgentException e) {
            throw new StatPublisherRuntimeException(e);
        }
    }

    public void sendAckStats(AndesAckData message)
            throws MalformedObjectNameException, ReflectionException, IOException,
            InstanceNotFoundException, AttributeNotFoundException, MBeanException {

        try {
            payLoadData = getAckStatsPayLoadData(message);
        } catch (Exception e) {
            throw new StatPublisherRuntimeException(e);
        }

        try {
            loadBalancingDataPublisher.publish(ackStatsStreamDef.getName(), ackStatsStreamDef.getVersion(),
                    getObjectArray(metaData), null,
                    getObjectArray(payLoadData));
        } catch (AgentException e) {
            throw new StatPublisherRuntimeException(e);
        }


    }

    private Object[] getObjectArray(List<Object> list) {
        if (list.size() > 0) {
            return list.toArray();
        }
        return null;
    }

    public List<Object> constructMetaData() {
        ArrayList<Object> metaData = new ArrayList<Object>(1);
        metaData.add(statPublisherConfiguration.getNodeURL());
        return metaData;
    }

    public List<Object> getServerStatsPayLoadData(SystemStatsReader.SystemStatsData systemStatsData) {
        ArrayList<Object> payloadData = new ArrayList<Object>(4);
        payloadData.add(Long.parseLong(systemStatsData.getHeapMemoryUsage()));
        payloadData.add(Long.parseLong(systemStatsData.getNonHeapMemoryUsage()));
        payloadData.add(Double.parseDouble(systemStatsData.getCPULoadAverage()));
        payloadData.add(getCurrentTimeStamp());

        return payloadData;
    }

    public List<Object> getMBStatsPayLoadData() throws Exception {
        ArrayList<Object> payloadData = new ArrayList<Object>(3);
        payloadData.add(getTotalSubscriptions());
        payloadData.add(getTopicList().size());
        payloadData.add(getCurrentTimeStamp());

        return payloadData;
    }

    public List<Object> getMessageStatsPayLoadData(AndesMessageMetadata message, int subscribers) throws Exception {
        ArrayList<Object> payloadData = new ArrayList<Object>(6);
        payloadData.add(message.getMessageID());
        payloadData.add(message.getDestination());
        payloadData.add(message.getMessageContentLength());
        payloadData.add(message.getExpirationTime());

        payloadData.add(subscribers);
        payloadData.add(getCurrentTimeStamp());

        return payloadData;
    }

    public List<Object> getAckStatsPayLoadData(AndesAckData message) throws Exception {
        ArrayList<Object> payloadData = new ArrayList<Object>(3);
        payloadData.add(message.messageID);
        payloadData.add(message.qName);
        payloadData.add(getCurrentTimeStamp());

        return payloadData;
    }

    private long getCurrentTimeStamp() {

        return System.currentTimeMillis();
    }

    private int getTotalSubscriptions() throws Exception {
        int totalSubscribers = 0;
        List<String> topics = getTopicList();

        for (String topic : topics) {
            List<AndesSubscription> subscriptionsList = subscriptionStore.getActiveClusterSubscribersForDestination(topic, true);
            totalSubscribers += subscriptionsList.size();
        }

        return totalSubscribers;
    }

    private List<String> getTopicList() throws Exception {
        MessagingEngine messagingEngine = MessagingEngine.getInstance();
        subscriptionStore = AndesContext.getInstance().getSubscriptionStore();
        return subscriptionStore.getTopics();
    }
}
