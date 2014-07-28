package org.wso2.carbon.andes.core;


import javax.jms.JMSException;
import javax.xml.stream.XMLStreamException;
import java.io.FileNotFoundException;
import java.util.Enumeration;
import java.util.List;

public interface QueueManagerService  {

    public void createQueue(String queueName) throws QueueManagerException;

    public List<org.wso2.carbon.andes.core.types.Queue> getAllQueues() throws QueueManagerException;

    public void deleteQueue(String queueName) throws QueueManagerException;

    public void restoreMessagesFromDeadLetterQueue(String[] messageIDs) throws Exception;

    public void restoreMessagesFromDeadLetterQueueWithDifferentDestination(String[] messageIDs, String destination) throws Exception;

    public void deleteMessagesFromDeadLetterQueue(String[] messageIDs) throws Exception;

    public void purgeMessagesOfQueue(String queueName) throws Exception;

    public long getMessageCountForQueue(String queueName,String msgPattern) throws Exception;

    public void updatePermission(String queueName, org.wso2.carbon.andes.core.types.QueueRolePermission[] queueRolePermissions)
            throws QueueManagerException;

    public String[] getBackendRoles() throws QueueManagerException;

    public org.wso2.carbon.andes.core.types.QueueRolePermission[] getQueueRolePermission(String queueName) throws QueueManagerException;

    public org.wso2.carbon.andes.core.types.Message[] browseQueue(String nameOfQueue, String userName, String accessKey, int startingIndex, int maxMsgCount)
            throws QueueManagerException;

    public long getTotalMessagesInQueue(String queueName) throws QueueManagerException;

    public boolean sendMessage(String nameOfQueue, String userName, String accessKey, String jmsType, String jmsCorrelationID, int numberOfMessages,
                               String message, int deliveryMode, int priority, long expireTime) throws QueueManagerException;

}
