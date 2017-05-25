/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.wso2.carbon.business.messaging.identity;

import org.wso2.carbon.business.messaging.identity.exception.StoreException;

/**
 * Action bean
 */
public class Action {

    public static final String DELIMITER = ":";

    private String actionNamespace;
    private String action;
    private String authorizationStore;

    public Action(String actionString) throws StoreException {

        if (!actionString.contains(DELIMITER)) {
            throw new StoreException("Invalid or cannot find the delimiter.");
        }

        actionNamespace = actionString.substring(0, actionString.indexOf(DELIMITER));
        action = actionString.substring(actionString.indexOf(DELIMITER) + 1, actionString.length());
    }

    public Action(String actionNamespace, String action) {
        this.actionNamespace = actionNamespace;
        this.action = action;
    }

    private Action(String actionNamespace, String action, String authorizationStore) {
        this.actionNamespace = actionNamespace;
        this.action = action;
        this.authorizationStore = authorizationStore;
    }

    public String getActionNamespace() {
        return actionNamespace;
    }

    public String getAction() {
        return action;
    }

    public String getActionString() {
        return actionNamespace + DELIMITER + action;
    }

    public String getAuthorizationStore() {
        return authorizationStore;
    }

    /**
     * Builder for the action.
     */
    public static class ActionBuilder {

        private String actionNamespace;
        private String action;
        private String authorizationStore;

        public ActionBuilder setActionNamespace(String actionNamespace) {
            this.actionNamespace = actionNamespace;
            return this;
        }

        public ActionBuilder setAction(String action) {
            this.action = action;
            return this;
        }

        public ActionBuilder setAuthorizationStore(String authorizationStore) {
            this.authorizationStore = authorizationStore;
            return this;
        }

        public Action build() throws StoreException {

            if (actionNamespace == null || action == null || authorizationStore == null) {
                throw new StoreException("Required data is missing to build the action.");
            }

            return new Action(actionNamespace, action, authorizationStore);
        }
    }


}
