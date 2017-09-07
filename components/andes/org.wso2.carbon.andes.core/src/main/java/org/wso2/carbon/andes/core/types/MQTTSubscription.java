/*
 * Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 */

package org.wso2.carbon.andes.core.types;

/**
 * This bean class represents the required attributes that are needed for the MQTT subscription
 */
public class MQTTSubscription {

    private boolean isDurable;
    private boolean isActive;
    private String protocolType;
    private String destinationType;
    private String filteredNamePattern;
    private boolean isFilteredNameByExactMatch;
    private String identifierPattern;
    private boolean isIdentifierPatternByExactMatch;
    private String ownNodeId;
    private int pageNumber;
    private int subscriptionCountPerPage;

    public boolean isDurable() {
        return isDurable;
    }

    public void setDurable(boolean durable) {
        isDurable = durable;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        this.isActive = active;
    }

    public String getProtocolType() {
        return protocolType;
    }

    public void setProtocolType(String protocolType) {
        this.protocolType = protocolType;
    }

    public String getDestinationType() {
        return destinationType;
    }

    public void setDestinationType(String destinationType) {
        this.destinationType = destinationType;
    }

    public String getFilteredNamePattern() {
        return filteredNamePattern;
    }

    public void setFilteredNamePattern(String filteredNamePattern) {
        this.filteredNamePattern = filteredNamePattern;
    }

    public boolean isFilteredNameByExactMatch() {
        return isFilteredNameByExactMatch;
    }

    public void setFilteredNameByExactMatch(boolean filteredNameByExactMatch) {
        isFilteredNameByExactMatch = filteredNameByExactMatch;
    }

    public String getIdentifierPattern() {
        return identifierPattern;
    }

    public void setIdentifierPattern(String identifierPattern) {
        this.identifierPattern = identifierPattern;
    }

    public boolean isIdentifierPatternByExactMatch() {
        return isIdentifierPatternByExactMatch;
    }

    public void setIdentifierPatternByExactMatch(boolean identifierPatternByExactMatch) {
        isIdentifierPatternByExactMatch = identifierPatternByExactMatch;
    }

    public String getOwnNodeId() {
        return ownNodeId;
    }

    public void setOwnNodeId(String ownNodeId) {
        this.ownNodeId = ownNodeId;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }

    public int getSubscriptionCountPerPage() {
        return subscriptionCountPerPage;
    }

    public void setSubscriptionCountPerPage(int subscriptionCountPerPage) {
        this.subscriptionCountPerPage = subscriptionCountPerPage;
    }

}
