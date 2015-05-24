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
package org.wso2.carbon.cassandra.datareader.cql;

import java.util.List;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlValue;

/**
 * This class represents the RDBMS configuration properties.
 */
@XmlRootElement(name = "configuration")
public class CassandraConfiguration {

    private String username;

    private Password passwordPersist;

    private String clusterName;

    private Integer port = 0;

    private List<String> hosts;

    private PoolingOptions poolOptions;

    private SocketOptions socketOptions;

    private ReconnectPolicyOptions reconnectPolicy;

    private LoadBalancingPolicyOptions loadBalancePolicy;

    private Integer maxConnections = 1;

    private Integer concurrency = 1;

    private Boolean async = false;

    private String compression;

    private String retryPolicy;

    private Boolean jmxDisabled = false;

    private String mode;

    private String keysapce;

    public PoolingOptions getPoolOptions() {
        return poolOptions;
    }

    public void setPoolOptions(PoolingOptions poolOptions) {
        this.poolOptions = poolOptions;
    }

    public SocketOptions getSocketOptions() {
        return socketOptions;
    }

    public void setSocketOptions(SocketOptions socketOptions) {
        this.socketOptions = socketOptions;
    }

    public ReconnectPolicyOptions getReconnectPolicy() {
        return reconnectPolicy;
    }

    public void setReconnectPolicy(ReconnectPolicyOptions reconnectPolicy) {
        this.reconnectPolicy = reconnectPolicy;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        if (this.getPasswordPersist() == null) {
            this.passwordPersist = new Password();
        }
        this.passwordPersist.setValue(password);
    }

    @XmlTransient
    public String getPassword() {
        if (this.getPasswordPersist() != null) {
            return this.getPasswordPersist().getValue();
        } else {
            return null;
        }
    }

    @XmlElement(name = "password")
    public Password getPasswordPersist() {
        return passwordPersist;
    }

    public void setPasswordPersist(Password passwordPersist) {
        this.passwordPersist = passwordPersist;
    }

    @XmlRootElement(name = "password")
    public static class Password {

        private boolean encrypted = true;

        private String value;

        @XmlAttribute(name = "encrypted")
        public boolean isEncrypted() {
            return encrypted;
        }

        public void setEncrypted(boolean encrypted) {
            this.encrypted = encrypted;
        }

        @XmlValue
        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

    }

    @XmlRootElement(name = "property")
    public static class DataSourceProperty {

        private boolean encrypted = true;

        private String name;

        private String value;

        @XmlAttribute(name = "encrypted")
        public boolean isEncrypted() {
            return encrypted;
        }

        public void setEncrypted(boolean encrypted) {
            this.encrypted = encrypted;
        }

        @XmlAttribute(name = "name")
        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @XmlValue
        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

    }

    public String getClusterName() {
        return clusterName;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public Integer getMaxConnections() {
        return maxConnections;
    }

    public void setMaxConnections(Integer maxConnections) {
        this.maxConnections = maxConnections;
    }

    public Integer getConcurrency() {
        return concurrency;
    }

    public void setConcurrency(Integer concurrency) {
        this.concurrency = concurrency;
    }

    public Boolean getAsync() {
        return async;
    }

    public void setAsync(Boolean async) {
        this.async = async;
    }

    public String getCompression() {
        return compression;
    }

    public void setCompression(String compression) {
        this.compression = compression;
    }

    public List<String> getHosts() {
        return hosts;
    }

    @XmlElementWrapper(name = "hosts")
    @XmlElement(name = "host")
    public void setHosts(List<String> hosts) {
        this.hosts = hosts;
    }

    public String getRetryPolicy() {
        return retryPolicy;
    }

    public void setRetryPolicy(String retryPolicy) {
        this.retryPolicy = retryPolicy;
    }

    public LoadBalancingPolicyOptions getLoadBalancePolicy() {
        return loadBalancePolicy;
    }

    public void setLoadBalancePolicy(LoadBalancingPolicyOptions loadBalancePolicy) {
        this.loadBalancePolicy = loadBalancePolicy;
    }


    public Boolean getJmxDisabled() {
        return jmxDisabled;
    }

    public void setJmxDisabled(Boolean jmxDisabled) {
        this.jmxDisabled = jmxDisabled;
    }


    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getKeysapce() {
        return keysapce;
    }

    public void setKeysapce(String keysapce) {
        this.keysapce = keysapce;
    }

    // ------------------Pooling Options------------------------
    public static class PoolingOptions {
        private Integer minSimultaneousRequestsForLocal;
        private Integer minSimultaneousRequestsForRemote;

        private Integer maxSimultaneousRequestsForLocal;
        private Integer maxSimultaneousRequestsForRemote;

        private Integer coreConnectionsForLocal;
        private Integer coreConnectionsForRemote;

        private Integer maxConnectionsForLocal;
        private Integer maxConnectionsForRemote;

        public Integer getMinSimultaneousRequestsForLocal() {
            return minSimultaneousRequestsForLocal;
        }

        public void setMinSimultaneousRequestsForLocal(Integer minSimultaneousRequestsForLocal) {
            this.minSimultaneousRequestsForLocal = minSimultaneousRequestsForLocal;
        }

        public Integer getMinSimultaneousRequestsForRemote() {
            return minSimultaneousRequestsForRemote;
        }

        public void setMinSimultaneousRequestsForRemote(Integer minSimultaneousRequestsForRemote) {
            this.minSimultaneousRequestsForRemote = minSimultaneousRequestsForRemote;
        }

        public Integer getMaxSimultaneousRequestsForLocal() {
            return maxSimultaneousRequestsForLocal;
        }

        public void setMaxSimultaneousRequestsForLocal(Integer maxSimultaneousRequestsForLocal) {
            this.maxSimultaneousRequestsForLocal = maxSimultaneousRequestsForLocal;
        }

        public Integer getMaxSimultaneousRequestsForRemote() {
            return maxSimultaneousRequestsForRemote;
        }

        public void setMaxSimultaneousRequestsForRemote(Integer maxSimultaneousRequestsForRemote) {
            this.maxSimultaneousRequestsForRemote = maxSimultaneousRequestsForRemote;
        }

        public Integer getCoreConnectionsForLocal() {
            return coreConnectionsForLocal;
        }

        public void setCoreConnectionsForLocal(Integer coreConnectionsForLocal) {
            this.coreConnectionsForLocal = coreConnectionsForLocal;
        }

        public Integer getCoreConnectionsForRemote() {
            return coreConnectionsForRemote;
        }

        public void setCoreConnectionsForRemote(Integer coreConnectionsForRemote) {
            this.coreConnectionsForRemote = coreConnectionsForRemote;
        }

        public Integer getMaxConnectionsForLocal() {
            return maxConnectionsForLocal;
        }

        public void setMaxConnectionsForLocal(Integer maxConnectionsForLocal) {
            this.maxConnectionsForLocal = maxConnectionsForLocal;
        }

        public Integer getMaxConnectionsForRemote() {
            return maxConnectionsForRemote;
        }

        public void setMaxConnectionsForRemote(Integer maxConnectionsForRemote) {
            this.maxConnectionsForRemote = maxConnectionsForRemote;
        }

    }

    // -------------------Socket Options-----------------------------
    public static class SocketOptions {

        private Boolean tcpNoDelay;
        private Integer connectTimeoutMillis;
        private Integer readTimeoutMillis;
        private Boolean keepAlive;
        private Boolean reuseAddress;
        private Integer soLinger;
        private Integer receiveBufferSize;
        private Integer sendBufferSize;

        public Boolean getTcpNoDelay() {
            return tcpNoDelay;
        }

        public void setTcpNoDelay(Boolean tcpNoDelay) {
            this.tcpNoDelay = tcpNoDelay;
        }

        public Integer getConnectTimeoutMillis() {
            return connectTimeoutMillis;
        }

        public void setConnectTimeoutMillis(Integer connectTimeoutMillis) {
            this.connectTimeoutMillis = connectTimeoutMillis;
        }

        public Integer getReadTimeoutMillis() {
            return readTimeoutMillis;
        }

        public void setReadTimeoutMillis(Integer readTimeoutMillis) {
            this.readTimeoutMillis = readTimeoutMillis;
        }

        public Boolean getKeepAlive() {
            return keepAlive;
        }

        public void setKeepAlive(Boolean keepAlive) {
            this.keepAlive = keepAlive;
        }

        public Boolean getReuseAddress() {
            return reuseAddress;
        }

        public void setReuseAddress(Boolean reuseAddress) {
            this.reuseAddress = reuseAddress;
        }

        public Integer getSoLinger() {
            return soLinger;
        }

        public void setSoLinger(Integer soLinger) {
            this.soLinger = soLinger;
        }

        public Integer getReceiveBufferSize() {
            return receiveBufferSize;
        }

        public void setReceiveBufferSize(Integer receiveBufferSize) {
            this.receiveBufferSize = receiveBufferSize;
        }

        public Integer getSendBufferSize() {
            return sendBufferSize;
        }

        public void setSendBufferSize(Integer sendBufferSize) {
            this.sendBufferSize = sendBufferSize;
        }

    }

    // ----------------Reconnect Policy options ----------------------
    public static class ReconnectPolicyOptions {

        private String policyName;
        private Long delayMs;
        private Long baseDelayMs;
        private Long maxDelayMs;

        public Long getDelayMs() {
            return delayMs;
        }

        public void setDelayMs(Long delayMs) {
            this.delayMs = delayMs;
        }

        public Long getBaseDelayMs() {
            return baseDelayMs;
        }

        public void setBaseDelayMs(Long baseDelayMs) {
            this.baseDelayMs = baseDelayMs;
        }

        public Long getMaxDelayMs() {
            return maxDelayMs;
        }

        public void setMaxDelayMs(Long maxDelayMs) {
            this.maxDelayMs = maxDelayMs;
        }

        public String getPolicyName() {
            return policyName;
        }

        public void setPolicyName(String policyName) {
            this.policyName = policyName;
        }

    }

    // -----------------------Load balancing policy-------------------------
    public static class LoadBalancingPolicyOptions {
        private String policyName;

        private String localDc;
        private Integer usedHostsPerRemoteDc;
        private Double exclusionThreshold;

        private Long scale;
        private Long retryPeriod;
        private Long minMeasure;

        private Boolean latencyAware;

        public String getPolicyName() {
            return policyName;
        }

        public String getLocalDc() {
            return localDc;
        }

        public void setLocalDc(String localDc) {
            this.localDc = localDc;
        }

        public Integer getUsedHostsPerRemoteDc() {
            return usedHostsPerRemoteDc;
        }

        public void setUsedHostsPerRemoteDc(Integer usedHostsPerRemoteDc) {
            this.usedHostsPerRemoteDc = usedHostsPerRemoteDc;
        }

        public Double getExclusionThreshold() {
            return exclusionThreshold;
        }

        public void setExclusionThreshold(Double exclusionThreshold) {
            this.exclusionThreshold = exclusionThreshold;
        }

        public Long getScale() {
            return scale;
        }

        public void setScale(Long scale) {
            this.scale = scale;
        }

        public Long getRetryPeriod() {
            return retryPeriod;
        }

        public void setRetryPeriod(Long retryPeriod) {
            this.retryPeriod = retryPeriod;
        }

        public Long getMinMeasure() {
            return minMeasure;
        }

        public void setMinMeasure(Long minMeasure) {
            this.minMeasure = minMeasure;
        }

        public void setPolicyName(String policyName) {
            this.policyName = policyName;
        }

        public Boolean getLatencyAware() {
            return latencyAware;
        }

        public void setLatencyAware(Boolean latencyAware) {
            this.latencyAware = latencyAware;
        }


    }

}
