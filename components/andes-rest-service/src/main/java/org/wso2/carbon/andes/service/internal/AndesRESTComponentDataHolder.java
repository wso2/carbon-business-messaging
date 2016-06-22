/*
 * Copyright (c) 2016, WSO2 Inc. (http://wso2.com) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.andes.service.internal;

import org.wso2.carbon.andes.core.Andes;

/**
 * Data Holder for Andes rest service
 */
public class AndesRESTComponentDataHolder {
    private static AndesRESTComponentDataHolder instance = new AndesRESTComponentDataHolder();
    private Andes andesInstance;

    private AndesRESTComponentDataHolder() {}

    /**
     * This returns the DataHolder instance.
     *
     * @return The DataHolder instance of this singleton class
     */
    public static AndesRESTComponentDataHolder getInstance() {
        return instance;
    }

    public Andes getAndesInstance() {
        return andesInstance;
    }

    public void setAndesInstance(Andes andesInstance) {
        this.andesInstance = andesInstance;
    }
}
