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

import org.wso2.carbon.stat.publisher.conf.MessageStatistic;
import org.wso2.carbon.stat.publisher.exception.StatPublisherRuntimeException;
import org.wso2.carbon.stat.publisher.internal.ds.StatPublisherValueHolder;
import org.wso2.carbon.user.api.TenantManager;
import org.wso2.carbon.user.api.UserStoreException;

import javax.management.*;
import java.io.IOException;
import java.util.concurrent.BlockingQueue;

/**
 * AsyncMessageStatPublisher class use for publish message statistics asynchronously
 * this will get message stats from messageQueue Queue and publish to configured destination with out blocking
 * main message flow of message broker
 */
public class AsyncMessageStatPublisher implements Runnable {

    private BlockingQueue<MessageStatistic> messageQueue = StatPublisherMessageListenerImpl.getMessageQueue();

    @Override
    public void run() {
        int tenantID;
        //check message Queue has any object
        while (StatPublisherMessageListenerImpl.isMessageStatPublisherThreadContinue()) {
            MessageStatistic messageStatistic;
            try {
                //get message object from queue
                messageStatistic = messageQueue.take();
            } catch (InterruptedException e) {
                throw new StatPublisherRuntimeException(e);
            }
            TenantManager tenantManager = StatPublisherValueHolder.getRealmService().getTenantManager();
            try {
                //get tenant ID from tenant domain
                tenantID = tenantManager.getTenantId(messageStatistic.getDomain());
            } catch (UserStoreException e) {
                throw new StatPublisherRuntimeException(e);
            }
            //get statPublisher Observer objects from StatPublisherObserver list in statPublisher manager
            StatPublisherObserver statPublisherObserver = StatPublisherValueHolder.
                    getStatPublisherManager().getStatPublisherObserver(tenantID);

            //check is it a message or Ack message
            if (messageStatistic.isMessage()) {

                try {
                    //publish message stat
                    statPublisherObserver.getStatPublisherDataAgent().sendMessageStats(messageStatistic.
                            getAndesMessageMetadata(), messageStatistic.getNoOfSubscribers());
                } catch (MalformedObjectNameException e) {
                    throw new StatPublisherRuntimeException(e);
                } catch (ReflectionException e) {
                    throw new StatPublisherRuntimeException(e);
                } catch (IOException e) {
                    throw new StatPublisherRuntimeException(e);
                } catch (InstanceNotFoundException e) {
                    throw new StatPublisherRuntimeException(e);
                } catch (AttributeNotFoundException e) {
                    throw new StatPublisherRuntimeException(e);
                } catch (MBeanException e) {
                    throw new StatPublisherRuntimeException(e);
                }

            } else {
                try {
                    //publish ack message stat
                    statPublisherObserver.getStatPublisherDataAgent().sendAckStats(messageStatistic.getAndesAckData());

                } catch (MalformedObjectNameException e) {
                    throw new StatPublisherRuntimeException(e);
                } catch (ReflectionException e) {
                    throw new StatPublisherRuntimeException(e);
                } catch (IOException e) {
                    throw new StatPublisherRuntimeException(e);
                } catch (InstanceNotFoundException e) {
                    throw new StatPublisherRuntimeException(e);
                } catch (AttributeNotFoundException e) {
                    throw new StatPublisherRuntimeException(e);
                } catch (MBeanException e) {
                    throw new StatPublisherRuntimeException(e);
                }

            }

        }
    }

}
