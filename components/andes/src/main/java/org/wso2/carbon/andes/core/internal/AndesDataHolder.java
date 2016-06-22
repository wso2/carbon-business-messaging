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

package org.wso2.carbon.andes.core.internal;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.osgi.HazelcastOSGiService;
import org.wso2.carbon.datasource.core.api.DataSourceService;
import org.wso2.carbon.kernel.CarbonRuntime;
import org.wso2.carbon.metrics.core.MetricService;

import java.util.logging.Logger;

/**
 * AndesDataHolder to hold {@link HazelcastInstance} instance referenced through {@link AndesServiceComponent}.
 */
public class AndesDataHolder {
    Logger logger = Logger.getLogger(AndesDataHolder.class.getName());

    private static AndesDataHolder instance = new AndesDataHolder();
    private HazelcastInstance carbonHazelcastAgent;
    private HazelcastOSGiService hazelcastOSGiService;
    private CarbonRuntime carbonRuntime;
    private MetricService metricService;

    /**
     * The datasource service instance provided by OSGI.
     */
    private DataSourceService dataSourceService;

    private AndesDataHolder() {

    }

    /**
     * This returns the AndesDataHolder instance.
     *
     * @return The AndesDataHolder instance of this singleton class
     */
    public static AndesDataHolder getInstance() {
        return instance;
    }

    /**
     * Get the data source service reference.
     *
     * @return The data source service instance
     */
    public DataSourceService getDataSourceService() {
        return dataSourceService;
    }

    /**
     * Initialize the data source service reference with a new reference.
     *
     * @param dataSourceManager The new data source service instance
     */
    public void setDataSourceService(DataSourceService dataSourceManager) {
        this.dataSourceService = dataSourceManager;
    }

    /**
     * Returns the CarbonRuntime service which gets set through a service component.
     *
     * @return CarbonRuntime Service
     */
    public CarbonRuntime getCarbonRuntime() {
        return carbonRuntime;
    }

    /**
     * This method is for setting the CarbonRuntime service. This method is used by
     * ServiceComponent.
     *
     * @param carbonRuntime The reference being passed through ServiceComponent
     */
    public void setCarbonRuntime(CarbonRuntime carbonRuntime) {
        this.carbonRuntime = carbonRuntime;
    }

    /**
     * Getter of {@link HazelcastOSGiService}
     *
     * @return {@link HazelcastOSGiService} object
     */
    public HazelcastOSGiService getHazelcastOSGiService() {
        return hazelcastOSGiService;
    }

    /**
     * Setter of {@link HazelcastOSGiService}
     *
     * @param hazelcastOSGiService {@link HazelcastOSGiService} object
     */
    public void setHazelcastOSGiService(HazelcastOSGiService hazelcastOSGiService) {
        this.hazelcastOSGiService = hazelcastOSGiService;
    }

    /**
     * Returns the {@link MetricService}, which is set by the service component
     *
     * @return The {@link MetricService} instance
     */
    public MetricService getMetricService() {
        return metricService;
    }

    /**
     * Set the {@link MetricService} when the service component gets a reference to the {@link MetricService} instance.
     *
     * @param metricService The {@link MetricService} reference being passed through the service component
     */
    public void setMetricService(MetricService metricService) {
        this.metricService = metricService;
    }

}
