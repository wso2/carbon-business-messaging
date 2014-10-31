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

import org.apache.log4j.Logger;
import org.wso2.andes.management.common.JMXConnnectionFactory;
import org.wso2.carbon.stat.publisher.conf.JMXConfiguration;
import org.wso2.carbon.stat.publisher.exception.StatPublisherRuntimeException;
import org.wso2.carbon.stat.publisher.internal.ds.StatPublisherValueHolder;
import org.wso2.carbon.user.core.UserRealm;
import org.wso2.carbon.user.core.UserStoreException;

import javax.management.*;
import javax.management.openmbean.CompositeData;
import javax.management.remote.JMXConnector;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.Set;

public class SystemStatsReader {

    public MBeanServerConnection connection = null;
    private final Logger logger = Logger.getLogger(SystemStatsReader.class);

    public SystemStatsReader(final JMXConfiguration jmxConfiguration) {

        //get MB username and password
        UserRealm realm;
        final String userName;
        final String password;
        try {
            realm = StatPublisherValueHolder.getRealmService().getBootstrapRealm();
            userName = realm.getRealmConfiguration().getAdminUserName();
            password = realm.getRealmConfiguration().getAdminPassword();
        } catch (UserStoreException e) {
            throw new StatPublisherRuntimeException("Fail to get admin username or password of MB", e);
        }
        //create jmxConnection

        final RetryOnExceptionStrategy retry = new RetryOnExceptionStrategy();

        Thread createJMXConnectionThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (retry.shouldRetry()) {
                    try {

                        createJMXConnection(jmxConfiguration, userName, password);
                        logger.info("JMX connection created");
                        break;

                    } catch (Exception e) {
                        try {
                            logger.error("Retrying to get JMX connection",e);
                            retry.errorOccured();
                        } catch (RuntimeException e1) {
                            throw new RuntimeException("Exception while creating jmx connection"
                                    , e);
                        } catch (Exception e1) {
                            throw new RuntimeException(e1);
                        }
                    }
                }
            }
        });
        createJMXConnectionThread.start();


    }

    private void createJMXConnection(JMXConfiguration jmxConfiguration, String userName, String password)
            throws Exception {
        //get JMX port if() {
        final int jmxPort = Integer.parseInt(jmxConfiguration.getRmiRegistryPort()) +
                Integer.parseInt(jmxConfiguration.getOffSet());
        long timeout = 100000;
        JMXConnector jmxConnector = JMXConnnectionFactory.getJMXConnection(timeout, jmxConfiguration.getHostName(),
                jmxPort, userName, password);
        connection = jmxConnector.getMBeanServerConnection();

    }

    public String HeapMemoryUsage() throws MalformedObjectNameException, IOException, AttributeNotFoundException,
        MBeanException, ReflectionException, InstanceNotFoundException {
        Set<ObjectInstance> set = connection.queryMBeans(new ObjectName("java.lang:type=Memory"), null);
        ObjectInstance oi = set.iterator().next();
        Object attrValue = connection.getAttribute(oi.getObjectName(), "HeapMemoryUsage");
        return ((CompositeData) attrValue).get("used").toString();
    }

    public String NonHeapMemoryUsage() throws MalformedObjectNameException, IOException, AttributeNotFoundException,
            MBeanException, ReflectionException, InstanceNotFoundException {
        Set<ObjectInstance> set = connection.queryMBeans(new ObjectName("java.lang:type=Memory"), null);
        ObjectInstance oi = set.iterator().next();
        Object attrValue_nonHeapMem = connection.getAttribute(oi.getObjectName(), "NonHeapMemoryUsage");

        return ((CompositeData) attrValue_nonHeapMem).get("used").toString();
    }

    public String CPUUsage() {
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();

        return Double.toString(osBean.getSystemLoadAverage());
    }


    public SystemStatsData getMbeansStatsData() throws MalformedObjectNameException, InstanceNotFoundException,
        IOException, ReflectionException, AttributeNotFoundException, MBeanException {
        SystemStatsData systemStatsData = new SystemStatsData();
        systemStatsData.setHeapMemoryUsage(HeapMemoryUsage());
        systemStatsData.setNonHeapMemoryUsage(NonHeapMemoryUsage());
        systemStatsData.setCPULoadAverage(CPUUsage());

        return systemStatsData;
    }

    public static class SystemStatsData {
        private String heapMemoryUsage;
        private String nonHeapMemoryUsage;
        private String CPULoadAverage;

        public String getHeapMemoryUsage() {
            return heapMemoryUsage;
        }

        public void setHeapMemoryUsage(String heapMemoryUsage) {
            this.heapMemoryUsage = heapMemoryUsage;
        }

        public String getNonHeapMemoryUsage() {
            return nonHeapMemoryUsage;
        }

        public void setNonHeapMemoryUsage(String nonHeapMemoryUsage) {
            this.nonHeapMemoryUsage = nonHeapMemoryUsage;
        }

        public String getCPULoadAverage() {
            return CPULoadAverage;
        }

        public void setCPULoadAverage(String CPULoadAverage) {
            this.CPULoadAverage = CPULoadAverage;
        }
    }
}
