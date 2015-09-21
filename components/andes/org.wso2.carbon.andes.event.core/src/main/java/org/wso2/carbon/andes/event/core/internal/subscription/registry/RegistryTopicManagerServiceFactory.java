/*
 * Copyright 2004,2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.andes.event.core.internal.subscription.registry;

import org.apache.axiom.om.OMElement;
import org.wso2.carbon.andes.event.core.TopicManagerService;
import org.wso2.carbon.andes.event.core.TopicManagerServiceFactory;
import org.wso2.carbon.andes.event.core.exception.EventBrokerConfigurationException;
import org.wso2.carbon.andes.event.core.internal.util.JavaUtil;

/**
 * This class used to create registry based topic manager
 */
public class RegistryTopicManagerServiceFactory implements TopicManagerServiceFactory {

    public static final String EB_ELE_TOPIC_STORAGE_PATH = "topicStoragePath";

    /**
     * {@inheritDoc}
     */
    public TopicManagerService getTopicManagerService(OMElement config) throws EventBrokerConfigurationException {
        String topicStoragePath = JavaUtil.getValue(config, EB_ELE_TOPIC_STORAGE_PATH);
        return new TopicManagerServiceImpl(topicStoragePath);
    }
}
