package org.wso2.carbon.business.messaging.admin.services.managers.impl;

import org.wso2.andes.kernel.Andes;
import org.wso2.andes.kernel.AndesException;
import org.wso2.andes.kernel.AndesMessage;
import org.wso2.andes.kernel.AndesMessageMetadata;
import org.wso2.carbon.business.messaging.admin.services.exceptions.InternalServerException;
import org.wso2.carbon.business.messaging.admin.services.internal.MbRestServiceDataHolder;
import org.wso2.carbon.business.messaging.admin.services.managers.DlcManagerService;

import java.util.List;


/**
 * Implementation for handling dlc queue related resource through OSGi.
 */
public class DlcManagerServiceImpl implements DlcManagerService {
    /**
     * Registered andes core instance through OSGi.
     */
    private Andes andesCore;

    public DlcManagerServiceImpl() {
        andesCore = MbRestServiceDataHolder.getInstance().getAndesCore();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int rerouteAllMessagesInDeadLetterChannelForQueue(String dlcQueueName, String sourceQueue, String
            targetQueue, int internalBatchSize, boolean restoreToOriginalQueue) throws InternalServerException {
        try {
            return andesCore.rerouteAllMessagesInDeadLetterChannelForQueue(dlcQueueName, sourceQueue, targetQueue,
                    internalBatchSize, restoreToOriginalQueue);
        } catch (AndesException e) {
            throw new InternalServerException("Error while moving messages from dlc to queue", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int moveMessagesFromDLCToNewDestination(List<Long> messageIds, String sourceQueue, String targetQueue,
                                                   boolean restoreToOriginalQueue) throws InternalServerException {
        try {
            return andesCore.moveMessagesFromDLCToNewDestination(messageIds, sourceQueue, targetQueue,
                    restoreToOriginalQueue);
        } catch (AndesException e) {
            throw new InternalServerException("Error while moving messages from dlc to queue", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<AndesMessageMetadata> getMessageMetadataInDLCForQueue(String queueName, String dlcQueueName, long
            firstMsgId, int count) throws InternalServerException {
        try {
            return andesCore.getNextNMessageMetadataInDLCForQueue(queueName, dlcQueueName, firstMsgId, count);
        } catch (AndesException e) {
            throw new InternalServerException("Error while getting message metadata from dlc", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<AndesMessage> getMessageContentInDLCForQueue(String queueName, String dlcQueueName, long
            firstMsgId, int count) throws InternalServerException {
        try {
            return andesCore.getNextNMessageContentInDLCForQueue(queueName, dlcQueueName, firstMsgId, count);
        } catch (AndesException e) {
            throw new InternalServerException("Error while getting message content from dlc", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteMessagesFromDeadLetterQueue(long[] andesMetadataIDs, String dlcQueueName) {
        andesCore.deleteMessagesFromDeadLetterQueue(andesMetadataIDs, dlcQueueName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getMessageCountInDLCForQueue(String queueName, String dlcQueueName) throws InternalServerException {
        try {
            return andesCore.getMessageCountInDLCForQueue(queueName, dlcQueueName);
        } catch (AndesException e) {
            throw new InternalServerException("Error while getting message count in dlc for queue", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getMessagCountInDLC(String dlcQueueName) throws InternalServerException {
        try {
            return andesCore.getMessageCountInDLC(dlcQueueName);
        } catch (AndesException e) {
            throw new InternalServerException("Error while getting message count in dlc", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isQueueExists(String queueName) {
        return andesCore.isQueueExists(queueName);
    }
}
