/**
 *  Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.wso2.carbon.cassandra.datareader.hector;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * This class mapping xml data source configuration to jaxb object
 */
@XmlRootElement(name = "configuration")
public class HectorConfiguration {

    private String hosts = "localhost:9160";
    private String username;
    private String password;
    private String clusterName;
    private int maxActive = -1;
    private int maxWaitTimeWhenExhausted = -1;
    private boolean useThriftFramedTransport = false;
    private boolean useSocketKeepalive = false;
    private int cassandraThriftSocketTimeout = -1;
    private boolean retryDownedHosts = false;
    private int retryDownedHostsDelayInSeconds = -1;
    private boolean autoDiscoverHosts = false;
    private int autoDiscoverDelayInSeconds = -1;
    private boolean runAutoDiscoveryAtStartup = false;
    private boolean useHostTimeoutTracker = false;
    private int hostTimeoutCounter = -1;
    private int hostTimeoutWindow = -1;
    private int hostTimeoutSuspensionDurationInSeconds = -1;
    private int hostTimeoutUnsuspendCheckDelay = -1;
    private int maxConnectTimeMillis = -1;
    private LoadBalancingPolicy loadBalancingPolicy;
    private boolean enableSecurity = false;
    private boolean lifo = false;

    public static enum LoadBalancingPolicy {
        DYNAMIC, ROUND_ROBIN, LEAST_ACTIVE
    }

    @XmlElement(name = "hosts")
    public String getHosts() {
        return hosts;
    }

    public void setHosts(String hosts) {
        this.hosts = hosts;
    }

    @XmlElement(name = "username")
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @XmlElement(name = "password")
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @XmlElement(name = "clusterName")
    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    @XmlElement(name = "maxActive")
    public int getMaxActive() {
        return maxActive;
    }

    public void setMaxActive(int maxActive) {
        this.maxActive = maxActive;
    }

    @XmlElement(name = "getMaxWaitTimeWhenExhausted")
    public int getMaxWaitTimeWhenExhausted() {
        return maxWaitTimeWhenExhausted;
    }

    public void setMaxWaitTimeWhenExhausted(int maxWaitTimeWhenExhausted) {
        this.maxWaitTimeWhenExhausted = maxWaitTimeWhenExhausted;
    }

    @XmlElement(name = "useThriftFramedTransport")
    public boolean getUseThriftFramedTransport() {
        return useThriftFramedTransport;
    }

    public void setUseThriftFramedTransport(boolean useThriftFramedTransport) {
        this.useThriftFramedTransport = useThriftFramedTransport;
    }

    @XmlElement(name = "useSocketKeepalive")
    public boolean getUseSocketKeepalive() {
        return useSocketKeepalive;
    }

    public void setUseSocketKeepalive(boolean useSocketKeepalive) {
        this.useSocketKeepalive = useSocketKeepalive;
    }

    @XmlElement(name = "cassandraThriftSocketTimeout")
    public int getCassandraThriftSocketTimeout() {
        return cassandraThriftSocketTimeout;
    }

    public void setCassandraThriftSocketTimeout(int cassandraThriftSocketTimeout) {
        this.cassandraThriftSocketTimeout = cassandraThriftSocketTimeout;
    }

    @XmlElement(name = "retryDownedHosts")
    public boolean getRetryDownedHosts() {
        return retryDownedHosts;
    }

    public void setRetryDownedHosts(boolean retryDownedHosts) {
        this.retryDownedHosts = retryDownedHosts;
    }

    @XmlElement(name = "retryDownedHostsDelayInSeconds")
    public int getRetryDownedHostsDelayInSeconds() {
        return retryDownedHostsDelayInSeconds;
    }

    public void setRetryDownedHostsDelayInSeconds(int retryDownedHostsDelayInSeconds) {
        this.retryDownedHostsDelayInSeconds = retryDownedHostsDelayInSeconds;
    }

    @XmlElement(name = "autoDiscoverHosts")
    public boolean getAutoDiscoverHosts() {
        return autoDiscoverHosts;
    }

    public void setAutoDiscoverHosts(boolean autoDiscoverHosts) {
        this.autoDiscoverHosts = autoDiscoverHosts;
    }

    @XmlElement(name = "autoDiscoverDelayInSeconds")
    public int getAutoDiscoverDelayInSeconds() {
        return autoDiscoverDelayInSeconds;
    }

    public void setAutoDiscoverDelayInSeconds(int autoDiscoverDelayInSeconds) {
        this.autoDiscoverDelayInSeconds = autoDiscoverDelayInSeconds;
    }

    @XmlElement(name = "runAutoDiscoveryAtStartup")
    public boolean getRunAutoDiscoveryAtStartup() {
        return runAutoDiscoveryAtStartup;
    }

    public void setRunAutoDiscoveryAtStartup(boolean runAutoDiscoveryAtStartup) {
        this.runAutoDiscoveryAtStartup = runAutoDiscoveryAtStartup;
    }

    @XmlElement(name = "useHostTimeoutTracker")
    public boolean getUseHostTimeoutTracker() {
        return useHostTimeoutTracker;
    }

    public void setUseHostTimeoutTracker(boolean useHostTimeoutTracker) {
        this.useHostTimeoutTracker = useHostTimeoutTracker;
    }

    @XmlElement(name = "hostTimeoutCounter")
    public int getHostTimeoutCounter() {
        return hostTimeoutCounter;
    }

    public void setHostTimeoutCounter(int hostTimeoutCounter) {
        this.hostTimeoutCounter = hostTimeoutCounter;
    }

    @XmlElement(name = "hostTimeoutWindow")
    public int getHostTimeoutWindow() {
        return hostTimeoutWindow;
    }

    public void setHostTimeoutWindow(int hostTimeoutWindow) {
        this.hostTimeoutWindow = hostTimeoutWindow;
    }

    @XmlElement(name = "hostTimeoutSuspensionDurationInSeconds")
    public int getHostTimeoutSuspensionDurationInSeconds() {
        return hostTimeoutSuspensionDurationInSeconds;
    }

    public void setHostTimeoutSuspensionDurationInSeconds(int hostTimeoutSuspensionDurationInSeconds) {
        this.hostTimeoutSuspensionDurationInSeconds = hostTimeoutSuspensionDurationInSeconds;
    }

    @XmlElement(name = "hostTimeoutUnsuspendCheckDelay")
    public int getHostTimeoutUnsuspendCheckDelay() {
        return hostTimeoutUnsuspendCheckDelay;
    }

    public void setHostTimeoutUnsuspendCheckDelay(int hostTimeoutUnsuspendCheckDelay) {
        this.hostTimeoutUnsuspendCheckDelay = hostTimeoutUnsuspendCheckDelay;
    }

    public int getMaxConnectTimeMillis() {
        return maxConnectTimeMillis;
    }

    public void setMaxConnectTimeMillis(int maxConnectTimeMillis) {
        this.maxConnectTimeMillis = maxConnectTimeMillis;
    }

    public boolean getLifo() {
        return lifo;
    }

    public boolean isLifo() {
        return lifo;
    }

    public void setLifo(boolean lifo) {
        this.lifo = lifo;
    }

    public boolean isEnableSecurity() {
        return enableSecurity;
    }

    public void setEnableSecurity(boolean enableSecurity) {
        this.enableSecurity = enableSecurity;
    }

    public LoadBalancingPolicy getLoadBalancingPolicy() {
        return loadBalancingPolicy;
    }

    public void setLoadBalancingPolicy(LoadBalancingPolicy loadBalancingPolicy) {
        this.loadBalancingPolicy = loadBalancingPolicy;
    }
}
