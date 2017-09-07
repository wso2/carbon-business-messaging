/*
*  Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.andes.core.internal.util;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.wso2.andes.configuration.AndesConfigurationManager;
import org.wso2.andes.configuration.enums.AndesConfiguration;
import org.wso2.andes.configuration.modules.JKSStore;
import org.wso2.andes.kernel.AndesConstants;
import org.wso2.carbon.andes.core.QueueManagerException;
import org.wso2.carbon.andes.core.internal.ds.QueueManagerServiceValueHolder;
import org.wso2.carbon.andes.core.types.Queue;
import org.wso2.carbon.andes.core.types.Subscription;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.registry.core.exceptions.RegistryException;
import org.wso2.carbon.registry.core.service.RegistryService;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.user.api.UserRealm;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageEOFException;
import javax.jms.ObjectMessage;
import javax.jms.StreamMessage;
import javax.jms.TextMessage;
import javax.xml.stream.XMLStreamException;
import java.io.FileNotFoundException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Provides common utilities for UI related functions and services.
 */
public class MQTTUtils {

    private static final String DIRECT_EXCHANGE = "amq.direct";
    private static final String TOPIC_EXCHANGE = "amq.topic";
    private static final String ANDES_CONF_DIR = "/repository/conf/advanced/";
    private static final String ANDES_CONF_FILE = "qpid-config.xml";
    private static final String ANDES_CONF_CONNECTOR_NODE = "connector";
    private static final String ANDES_CONF_SSL_NODE = "ssl";
    private static final String CARBON_CLIENT_ID = "carbon";
    private static final String CARBON_VIRTUAL_HOST_NAME = "carbon";
    private static final int CHARACTERS_TO_SHOW = 15;

    /**
     * Maximum size a message will be displayed on UI
     */
    public static final Integer MESSAGE_DISPLAY_LENGTH_MAX =
            AndesConfigurationManager.readValue(AndesConfiguration.MANAGEMENT_CONSOLE_MAX_DISPLAY_LENGTH_FOR_MESSAGE_CONTENT);

    /**
     * Shown to user has a indication that the particular message has more content than shown in UI
     */
    public static final String DISPLAY_CONTINUATION = "...";

    /**
     * Message shown in UI if message content exceed the limit - Further enhancement,
     * these needs to read from a resource bundle
     */
    public static final String DISPLAY_LENGTH_EXCEEDED = "Message Content is too large to display.";


    /**
     * Filters the domain specific subscriptions from a list of subscriptions
     *
     * @param allSubscriptions input subscription list
     * @return filtered list of {@link org.wso2.carbon.andes.core.types.Subscription}
     */
    public static List<Subscription> filterDomainSpecificSubscribers(
            List<Subscription> allSubscriptions, String tenantDomain) {
        ArrayList<Subscription> tenantFilteredSubscriptions = new ArrayList<>();

        //filter subscriptions belonging to the tenant domain
        if (tenantDomain != null && !CarbonContext.getThreadLocalCarbonContext().getTenantDomain().
                equals(org.wso2.carbon.base.MultitenantConstants.SUPER_TENANT_DOMAIN_NAME)) {
            for (Subscription subscription : allSubscriptions) {

                //for queues filter by queue name queueName=<tenantDomain>/queueName
                //for temp topics filter by topic name topicName=<tenantDomain>/topicName
                //for durable topic subs filter by topic name topicName=<tenantDomain>/topicName
                if (subscription.getSubscribedQueueOrTopicName().startsWith(tenantDomain + AndesConstants.TENANT_SEPARATOR)) {
                    tenantFilteredSubscriptions.add(subscription);
                }
            }
            //super tenant domain queue should have '/'
        } else if (tenantDomain != null && CarbonContext.getThreadLocalCarbonContext().getTenantDomain().
                equals(org.wso2.carbon.base.MultitenantConstants.SUPER_TENANT_DOMAIN_NAME)) {
            for (Subscription subscription : allSubscriptions) {
                if (subscription.getSubscribedQueueOrTopicName().contains(AndesConstants.TENANT_SEPARATOR)) {
                    tenantFilteredSubscriptions.add(subscription);
                }
            }
        }
        return tenantFilteredSubscriptions;
    }

}
