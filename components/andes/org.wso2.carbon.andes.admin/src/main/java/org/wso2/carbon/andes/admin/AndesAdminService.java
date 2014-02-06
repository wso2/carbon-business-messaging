/*
*  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
package org.wso2.carbon.andes.admin;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.andes.admin.internal.Exception.QueueManagerAdminException;
import org.wso2.carbon.andes.admin.util.AndesQueueManagerAdminServiceDSHolder;
import org.wso2.carbon.andes.core.QueueManagerException;
import org.wso2.carbon.andes.core.QueueManagerService;
import org.wso2.carbon.core.AbstractAdmin;

import java.util.ArrayList;
import java.util.List;

public class AndesAdminService extends AbstractAdmin {
    private static Log log = LogFactory.getLog(AndesAdminService.class);


    public void createQueue(String queueName) throws QueueManagerAdminException {
        QueueManagerService queueManagerService = AndesQueueManagerAdminServiceDSHolder.getInstance().getQueueManagerService();
        try {
            queueManagerService.createQueue(queueName);
        } catch (QueueManagerException e) {
            log.error("Error in creating the queue", e);
            throw new QueueManagerAdminException("Error in creating the queue", e);
        }
    }

    public org.wso2.carbon.andes.admin.internal.Queue[] getAllQueues() throws QueueManagerAdminException {
        List<org.wso2.carbon.andes.admin.internal.Queue> allQueues
                = new ArrayList<org.wso2.carbon.andes.admin.internal.Queue>();
        org.wso2.carbon.andes.admin.internal.Queue[] queuesDTO = null;
        try {
            QueueManagerService queueManagerService =
                    AndesQueueManagerAdminServiceDSHolder.getInstance().getQueueManagerService();
            List<org.wso2.carbon.andes.core.types.Queue> queues = queueManagerService.getAllQueues();
            queuesDTO = new org.wso2.carbon.andes.admin.internal.Queue[queues.size()];
            for (org.wso2.carbon.andes.core.types.Queue queue : queues) {
                org.wso2.carbon.andes.admin.internal.Queue queueDTO = new org.wso2.carbon.andes.admin.internal.Queue();
                queueDTO.setQueueName(queue.getQueueName());
                queueDTO.setMessageCount(queue.getMessageCount());
                queueDTO.setCreatedTime(queue.getCreatedTime());
                queueDTO.setUpdatedTime(queue.getUpdatedTime());
                allQueues.add(queueDTO);
            }
            allQueues.toArray(queuesDTO);
        } catch (QueueManagerException e) {
            throw new QueueManagerAdminException("Problem in getting queues from back end", e);
        }
        return queuesDTO;
    }

    public void deleteQueue(String queueName) throws QueueManagerAdminException {
        try {
            QueueManagerService queueManagerService =
                    AndesQueueManagerAdminServiceDSHolder.getInstance().getQueueManagerService();
            queueManagerService.deleteQueue(queueName);
        } catch (QueueManagerException e) {
            throw new QueueManagerAdminException("Error in deleting queue",e);
        }

    }


}
