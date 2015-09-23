/*
 * Copyright 2004,2015 The Apache Software Foundation.
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

package org.wso2.carbon.andes.event.admin;

import org.wso2.carbon.andes.event.core.subscription.EventDispatcher;
import org.wso2.carbon.andes.event.core.subscription.EventFilter;

import java.util.Calendar;

/**
 * Keep subscription related information to handle necessary operation in AndesEventAdminService.
 */
public class Subscription {

    // these properties are come from the eventing specification
    private String eventSinkURL;
    private Calendar expires;
    private EventFilter eventFilter;
    // Delivary mode
    // these properites are used in wso2 carbon implementation.
    private String id;
    private String topicName;
    private EventDispatcher eventDispatcher;
    private String eventDispatcherName;
    private Calendar createdTime;
    private String owner;
    private String mode;


    public String getEventSinkURL() {
        return eventSinkURL;
    }

    public void setEventSinkURL(String eventSinkURL) {
        this.eventSinkURL = eventSinkURL;
    }

    public Calendar getExpires() {
        return expires;
    }

    public void setExpires(Calendar expires) {
        this.expires = expires;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public EventDispatcher getEventDispatcher() {
        return eventDispatcher;
    }

    public void setEventDispatcher(EventDispatcher eventDispatcher) {
        this.eventDispatcher = eventDispatcher;
    }

    public String getEventDispatcherName() {
        return eventDispatcherName;
    }

    public void setEventDispatcherName(String eventDispatcherName) {
        this.eventDispatcherName = eventDispatcherName;
    }

    public Calendar getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(Calendar createdTime) {
        this.createdTime = createdTime;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public EventFilter getEventFilter() {
        return eventFilter;
    }

    public void setEventFilter(EventFilter eventFilter) {
        this.eventFilter = eventFilter;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }
}
