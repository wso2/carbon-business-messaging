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

package org.wso2.carbon.andes.core.internal.cluster.coordination.hazelcast;

import com.hazelcast.core.Message;
import com.hazelcast.core.MessageListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.andes.core.AndesBinding;
import org.wso2.carbon.andes.core.AndesException;
import org.wso2.carbon.andes.core.BindingListener;
import org.wso2.carbon.andes.core.internal.cluster.coordination.ClusterNotification;

import java.util.ArrayList;
import java.util.List;

/**
 * This listener class is triggered when binding change happened in cluster via HazelCast.
 */
public class ClusterBindingChangedListener implements MessageListener<ClusterNotification> {

    private static Log log = LogFactory.getLog(ClusterBindingChangedListener.class);
    private List<BindingListener> bindingListeners = new ArrayList<>();

    /**
     * Register a listener interested on binding changes in cluster
     *
     * @param listener listener to be registered
     */
    public void addBindingListener(BindingListener listener) {
        bindingListeners.add(listener);
    }

    @Override
    public void onMessage(Message<ClusterNotification> message) {
        ClusterNotification clusterNotification = message.getMessageObject();
        log.debug(
                "Handling cluster gossip: received a binding change notification " + clusterNotification
                        .getDescription());
        try {
            AndesBinding andesBinding = new AndesBinding(clusterNotification.getEncodedObjectAsString());
            BindingListener.BindingEvent change = BindingListener.BindingEvent
                    .valueOf(clusterNotification.getChangeType());
            for (BindingListener bindingListener : bindingListeners) {
                bindingListener.handleClusterBindingsChanged(andesBinding, change);
            }
        } catch (AndesException e) {
            log.error("error while handling cluster binding change notification", e);
        }
    }
}
