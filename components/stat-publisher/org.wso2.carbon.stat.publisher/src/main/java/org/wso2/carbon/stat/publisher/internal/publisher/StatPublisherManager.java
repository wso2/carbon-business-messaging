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

import org.wso2.carbon.stat.publisher.conf.JMXConfiguration;
import org.wso2.carbon.stat.publisher.conf.StreamConfiguration;
import org.wso2.carbon.stat.publisher.exception.StatPublisherConfigurationException;
import org.wso2.carbon.stat.publisher.internal.util.XMLConfigurationReader;
import org.wso2.carbon.user.api.UserStoreException;

import java.util.HashMap;
import java.util.HashSet;

/**
 * StatPublisherManager manage all statPublisherObservers and store them in Hash map
 * if bundle activation , UI data storing events or specific tenant create it's carbon context occurs
 * StatPublisherObserver will change in Hash map
 */
public class StatPublisherManager {

    private JMXConfiguration jmxConfiguration;
    private StreamConfiguration streamConfiguration;
    private StatPublisherObserver statPublisherObserver;
    private HashMap<Integer, StatPublisherObserver> statPublisherObserverHashMap =
            new HashMap<Integer, StatPublisherObserver>();
    public static HashSet<String> messageStatEnableSet = new HashSet<String>();


    /**
     * public constructor of StatPublisherManager
     *
     * @throws StatPublisherConfigurationException
     */
    public StatPublisherManager() throws StatPublisherConfigurationException {
        jmxConfiguration = XMLConfigurationReader.readJMXConfiguration();
        streamConfiguration = XMLConfigurationReader.readStreamConfiguration();
    }

    /**
     * Create new StatPublisherObserver Instance and store it in Hash map by using tenant ID as key value
     *
     * @param tenantID tenantID of the specific tenant that need to create StatPublisherObserver instance
     * @throws StatPublisherConfigurationException
     */
    public void createStatPublisherObserver(int tenantID) throws StatPublisherConfigurationException {
        statPublisherObserver = new StatPublisherObserver(jmxConfiguration, streamConfiguration, tenantID);
        try {
            //start monitoring process
            statPublisherObserver.startObserver();
        } catch (UserStoreException e) {
            throw new StatPublisherConfigurationException("Could not start monitoring!", e);
        }
        //Add observer to statPublisherObserverHashMap Hash map
        statPublisherObserverHashMap.put(tenantID, statPublisherObserver);

        if (statPublisherObserver.getStatPublisherConfiguration().isMessageStatEnable()) {
            //if message statPublisher is enable it's relevant tenant domain add to messageStatEnableSet hash set
            messageStatEnableSet.add(statPublisherObserver.getTenantDomain());
        }
    }

    /**
     * This will use to update StatPublisherObserver Instance from Hash map
     *
     * @param tenantID tenantID of the specific tenant that need to update StatPublisherObserver instance
     * @throws StatPublisherConfigurationException
     */
    public void updateStatPublisherObserver(int tenantID) throws StatPublisherConfigurationException {
        removeStatPublisherObserver(tenantID);
        createStatPublisherObserver(tenantID);
    }

    /**
     * This will use to remove StatPublisherObserver Instance from Hash map
     *
     * @param tenantID tenantID of the specific tenant that need to remove StatPublisherObserver instance
     */
    public void removeStatPublisherObserver(int tenantID) {
        statPublisherObserver = statPublisherObserverHashMap.get(tenantID);
        if (statPublisherObserver != null) {
            //stop monitoring process
            statPublisherObserver.stopObserver();
            //remove tenant domain from hash map
            messageStatEnableSet.remove(statPublisherObserver.getTenantDomain());
            //remove publisher observer from hash map
            statPublisherObserverHashMap.remove(tenantID);
            if (statPublisherObserver.getStatPublisherDataAgent() != null)
                //stop load balancing data publisher when removing StatPublisherObserver from hash map
                statPublisherObserver.getStatPublisherDataAgent().getLoadBalancingDataPublisher().stop();
        }
    }

    /**
     * Get Message StatPublisherObserver for specific tenant from statPublisherObserverHashMap Hash map
     *
     * @param tenantID tenantID of the specific tenant that need to get StatPublisherObserver instance
     * @return StatPublisherObserver instance for requested tenant
     */
    public StatPublisherObserver getStatPublisherObserver(int tenantID) {
        statPublisherObserver = statPublisherObserverHashMap.get(tenantID);
        return statPublisherObserver;
    }

    /**
     * This will use to get public messageStatEnableSet
     *
     * @return messageStatEnableSet
     */
    public HashSet<String> getMessageStatEnableMap() {
        return messageStatEnableSet;
    }


}
