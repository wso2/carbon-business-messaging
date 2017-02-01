/*
*  Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.carbon.andes.core.internal.registry;

import org.wso2.carbon.andes.core.AndesException;
import org.wso2.carbon.andes.core.internal.util.AdministrativeManagementConstants;
import org.wso2.carbon.utils.CarbonUtils;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import java.io.File;
import java.lang.management.ManagementFactory;

/**
 * Class for managing message status via MBean operations
 */
public class MessageStatusInformationBeans {

    private static MessageStatusInformationBeans self = new MessageStatusInformationBeans();

    public static MessageStatusInformationBeans getInstance() {

        if (self == null) {
            self = new MessageStatusInformationBeans();
        }
        return self;
    }

    /**
     * Invoke MBean operations and dump status of all messages. The file will be created
     * inside [MB_HOME]/repository/logs
     *
     * @throws AndesException on MBean access issue
     */
    public void dumpMessageStatusInformation() throws AndesException {
        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        String filePath = CarbonUtils.getCarbonLogsPath() + File.separator
                + AdministrativeManagementConstants.MESSAGE_STATUS_DUMP_FILE_NAME;
        try {
            ObjectName objectName = new ObjectName("org.wso2.andes:type=MessageStatusInformation," +
                    "name=MessageStatusInformation");
            Object[] parameters = new Object[]{filePath};
            String[] signature = new String[]{String.class.getName()};

            mBeanServer.invoke(objectName,
                    AdministrativeManagementConstants.MESSAGE_STATUS_DUMP_MBEAN_OPERATION,
                    parameters, signature);

        } catch (MalformedObjectNameException | ReflectionException | MBeanException | InstanceNotFoundException e) {
            throw new AndesException("Cannot access mBean operations to " +
                    "dump message status", e);
        }
    }
}
