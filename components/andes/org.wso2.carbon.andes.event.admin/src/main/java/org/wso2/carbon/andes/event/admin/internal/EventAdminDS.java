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
package org.wso2.carbon.andes.event.admin.internal;

import org.osgi.service.component.ComponentContext;
import org.wso2.carbon.andes.event.core.EventBroker;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 * Service references initialize by this class. All necessary services references get initialize in bundle
 * activation.
 */
@Component(
        name = "AndesEventManagerAdmin.component",
        immediate = true)
public class EventAdminDS {

    @Activate
    protected void activate(ComponentContext context) {

    }

    @Reference(
            name = "eventbroker.service",
            service = org.wso2.carbon.andes.event.core.EventBroker.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unSetEventBroker")
    protected void setEventBroker(EventBroker eventBroker) {

        EventAdminHolder.getInstance().registerEventBroker(eventBroker);
    }

    protected void unSetEventBroker(EventBroker eventBroker) {

    }
}
