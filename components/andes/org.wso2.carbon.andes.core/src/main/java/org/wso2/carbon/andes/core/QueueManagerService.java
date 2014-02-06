package org.wso2.carbon.andes.core;


import java.util.List;

public interface QueueManagerService  {

    public void createQueue(String queueName) throws QueueManagerException;

    public List<org.wso2.carbon.andes.core.types.Queue> getAllQueues() throws QueueManagerException;

    public void deleteQueue(String queueName) throws QueueManagerException;
}
