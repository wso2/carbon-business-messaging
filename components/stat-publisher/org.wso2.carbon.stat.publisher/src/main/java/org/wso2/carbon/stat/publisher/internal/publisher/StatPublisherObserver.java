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

import org.apache.log4j.Logger;
import org.wso2.carbon.databridge.agent.thrift.exception.AgentException;
import org.wso2.carbon.stat.publisher.conf.JMXConfiguration;
import org.wso2.carbon.stat.publisher.conf.StatPublisherConfiguration;
import org.wso2.carbon.stat.publisher.conf.StreamConfiguration;
import org.wso2.carbon.stat.publisher.exception.StatPublisherConfigurationException;
import org.wso2.carbon.stat.publisher.exception.StatPublisherRuntimeException;
import org.wso2.carbon.stat.publisher.internal.ds.StatPublisherValueHolder;
import org.wso2.carbon.stat.publisher.internal.util.RegistryPersistenceManager;
import org.wso2.carbon.stat.publisher.internal.util.XMLConfigurationReader;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.tenant.TenantManager;

import javax.management.*;
import java.io.IOException;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * StatPublisherObserver create observer Instance for every tenant
 * System and MB Stat publishing activation and deactivation methods handle by this class
 * Message and ack stat publisher enable value also set in this class
 * Create a Data agent instance for tenant which enable stat publishing in any type of statistic
 */

public class StatPublisherObserver {

    private static final Logger logger = Logger.getLogger(StatPublisherObserver.class);

    private StatPublisherDataAgent getStatPublisherDataAgent;
    private StatPublisherConfiguration statPublisherConfiguration;
    private Timer statPublisherTimer;
    private TimerTask statPublisherTimerTask;
    private String tenantDomain = null;
    private int tenantID;

    private TenantManager tenantManager = StatPublisherValueHolder.getRealmService().getTenantManager();


    public StatPublisherObserver(JMXConfiguration jmxConfiguration, StreamConfiguration streamConfiguration,
                                 int tenantID) throws StatPublisherConfigurationException {
        this.tenantID = tenantID;
        this.statPublisherConfiguration = RegistryPersistenceManager.loadConfigurationData(tenantID);
        if (statPublisherConfiguration.isSystemStatEnable() || statPublisherConfiguration.isMbStatEnable()
                || statPublisherConfiguration.isMessageStatEnable()) {
            //create dataAgent for this observer
            getStatPublisherDataAgent =
                    new StatPublisherDataAgent(jmxConfiguration, streamConfiguration, statPublisherConfiguration);
        }
    }


    /**
     * Start periodical statistic Publishing using timer task
     * activate publishing after read registry configurations
     * set tenant domain value if MessageStat enabled in registry
     */
    public void startObserver() throws UserStoreException {

        //set tenant domain using tenantID
        if (statPublisherConfiguration.isMessageStatEnable()) {
            tenantDomain = tenantManager.getDomain(tenantID);
        }
        //Checking  System or MB stat enable or not
        if (statPublisherConfiguration.isSystemStatEnable() || statPublisherConfiguration.isMbStatEnable()) {


            statPublisherTimerTask = new TimerTask() {
                @Override
                public void run() {
                    //check system stat enable configuration
                    if (statPublisherConfiguration.isSystemStatEnable()) {
                        //System stat publishing activated
                        try {
                            getStatPublisherDataAgent.sendSystemStats();
                            logger.info("Sent system stats");

                        } catch (MalformedObjectNameException e) {
                            throw new StatPublisherRuntimeException("Fail to send system stats", e);
                        } catch (ReflectionException e) {
                            throw new StatPublisherRuntimeException("Fail to send system stats", e);
                        } catch (IOException e) {
                            throw new StatPublisherRuntimeException("Fail to send system stats", e);
                        } catch (InstanceNotFoundException e) {
                            throw new StatPublisherRuntimeException("Fail to send system stats", e);
                        } catch (AttributeNotFoundException e) {
                            throw new StatPublisherRuntimeException("Fail to send system stats", e);
                        } catch (MBeanException e) {
                            throw new StatPublisherRuntimeException("Fail to send system stats", e);
                        } catch (AgentException e) {
                            throw new StatPublisherRuntimeException("Fail to send system stats", e);
                        }
                    }
                    //check MB stat enable configuration
                    if (statPublisherConfiguration.isMbStatEnable()) {

                        try {

                            getStatPublisherDataAgent.sendMBStats();
                            logger.info("Sent MB stats");
                        } catch (MalformedObjectNameException e) {
                            throw new StatPublisherRuntimeException("Fail to send MB stats", e);
                        } catch (ReflectionException e) {
                            throw new StatPublisherRuntimeException("Fail to send MB stats", e);
                        } catch (IOException e) {
                            throw new StatPublisherRuntimeException("Fail to send MB stats", e);
                        } catch (InstanceNotFoundException e) {
                            throw new StatPublisherRuntimeException("Fail to send MB stats", e);
                        } catch (AttributeNotFoundException e) {
                            throw new StatPublisherRuntimeException("Fail to send MB stats", e);
                        } catch (MBeanException e) {
                            throw new StatPublisherRuntimeException("Fail to send MB stats", e);
                        }
                    }
                }


            };
            statPublisherTimer = new Timer();
            // scheduling the task at fixed rate
            Thread timerTaskThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        statPublisherTimer.scheduleAtFixedRate(statPublisherTimerTask, new Date(),
                                XMLConfigurationReader.readGeneralConfiguration().getTimeInterval());
                    } catch (StatPublisherConfigurationException e) {
                        logger.error("Exception in TimerTask initialization" + e);
                        throw new StatPublisherRuntimeException(e);
                    }
                }
            });
            timerTaskThread.start();
        }
    }

    /**
     * Stop periodical statistic Publishing by stopping statPublisherTimer task
     */
    public void stopObserver() {
        if (statPublisherTimer != null) {
            statPublisherTimer.cancel();
        }
    }

    /**
     * get statPublisher registry Configuration  of this Stat Publisher observer
     *
     * @return statPublisherConfiguration instance
     */
    public StatPublisherConfiguration getStatPublisherConfiguration() {
        return statPublisherConfiguration;
    }

    /**
     * get statPublisher DataAgent instance of this Stat Publisher observer
     *
     * @return getStatPublisherDataAgent
     */
    public StatPublisherDataAgent getStatPublisherDataAgent() {
        return getStatPublisherDataAgent;
    }

    /**
     * get tenant domain of this Stat Publisher observer if message stat enabled else it's return null
     *
     * @return tenantDomain
     */
    public String getTenantDomain() {
        return tenantDomain;
    }
}
