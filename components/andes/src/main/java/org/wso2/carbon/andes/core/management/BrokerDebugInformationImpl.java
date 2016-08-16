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

package org.wso2.carbon.andes.core.management;

import org.wso2.carbon.andes.core.AndesException;
import org.wso2.carbon.andes.core.internal.outbound.SlotDeliveryWorkerManager;
import org.wso2.carbon.andes.core.management.mbeans.BrokerDebugInformationMXBean;

import java.io.File;
import java.io.IOException;

/**
 * MBeans for debugging the broker. Exposes Andes internal data/information.
 */
public class BrokerDebugInformationImpl implements BrokerDebugInformationMXBean {

    /**
     * {@inheritDoc}
     */
    @Override
    public void dumpMessageStatusInfo(String filePath) {
        try {
            File fileToWriteMessageStatus = new File(filePath);
            if (fileToWriteMessageStatus.exists()) {
                fileToWriteMessageStatus.delete();
            }
            fileToWriteMessageStatus.getParentFile().mkdirs();
            fileToWriteMessageStatus.createNewFile();

            //Get slot delivery workers
            SlotDeliveryWorkerManager.getInstance().dumpAllSlotInformationToFile(fileToWriteMessageStatus);

        } catch (AndesException e) {
            throw new RuntimeException("Internal error while dumping message status", e);
        } catch (IOException e) {
            throw new RuntimeException("Cannot create a file to dump message status", e);
        }

    }
}
