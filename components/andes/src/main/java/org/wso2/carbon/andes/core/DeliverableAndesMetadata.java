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

package org.wso2.carbon.andes.core;

import com.gs.collections.impl.map.mutable.ConcurrentHashMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.andes.core.internal.slot.Slot;
import org.wso2.carbon.andes.core.subscription.LocalSubscription;
import org.wso2.carbon.andes.core.util.MessageTracer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * This class represents the message metadata and all the delivery aspects of it to the subscribers (outbound path).
 * The lifecycle of the message is maintained here itself.
 */
public class DeliverableAndesMetadata extends AndesMessageMetadata {

    private static Log log = LogFactory.getLog(DeliverableAndesMetadata.class);
    /**
     * Map to keep message status and delivery information of this message to vivid channels
     */
    private Map<UUID, ChannelInformation> channelDeliveryInfo;
    /**
     * State transition of the message
     */
    private List<MessageStatus> messageStatus;

    /**
     * Parent slot of message.
     */
    private Slot slot;

    /**
     * Time stamp message is read from the store
     */
    private long timeMessageIsRead;

    /**
     * In a session-transacted consumer scenario, a rollback will reject even the messages beyond the rollback point,
     * since they have been pre-fetched from the server to the client buffer. This property marks such messages.
     * Attempt to re-send a message beyond a rollback point should not be added to the deliveryAttempts. (Since such
     * messages have not yet been visible to the consumer through the client.)
     */
    private boolean isBeyondLastRollbackedMessage;

    /**
     * Indicate if the metadata should not be used.
     */
    private boolean stale;

    public DeliverableAndesMetadata(Slot slot, byte[] metadata) throws AndesException {
        super(metadata);
        this.slot = slot;
        this.timeMessageIsRead = System.currentTimeMillis();
        this.channelDeliveryInfo = new ConcurrentHashMap<>();
        this.messageStatus = Collections.synchronizedList(new ArrayList<MessageStatus>());
        this.messageStatus.add(MessageStatus.READ);
    }

    /**
     * Generate a new protocol deliverable message. This will include a reference of this message
     * plus snapshot of channel information message is delivered to
     *
     * @param channelID ID of the channel message is to be delivered
     * @return new ProtocolMessage object
     */
    public ProtocolMessage generateProtocolDeliverableMessage(UUID channelID) {
        return new ProtocolMessage(this, channelID);
    }

    /**
     * Change the belonging slot to a new one. Used when the current slot is overlapping with a slot tracked in the
     * {@link org.wso2.carbon.andes.core.internal.outbound.MessageDeliveryTask}.
     *
     * @param slot New Slot
     */
    public void changeSlot(Slot slot) {
        this.slot = slot;
    }

    /**
     * Check if message is expired
     *
     * @return check expire result
     */
    public boolean isExpired() {
        if (getExpirationTime() != 0L) {
            long now = System.currentTimeMillis();
            if (now > getExpirationTime()) {
                addMessageStatus(MessageStatus.EXPIRED);
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public Slot getSlot() {
        return slot;
    }

    /**
     * Get Message Status this message passed as a string
     *
     * @return encoded status history
     */
    public String getStatusHistoryAsString() {
        String history = "";
        for (MessageStatus status : messageStatus) {
            history = history + status + ">>";
        }
        return history;
    }

    /**
     * Get complete message status history together with all channel status
     *
     * @return above information as a string
     */
    public String getMessageStatusWithAllChannelStatus() {
        String messageStatusHistory = getStatusHistoryAsString();
        String deliveries = "";
        for (UUID channelID : getAllDeliveredChannels()) {
            deliveries = deliveries + channelID + " : " + channelDeliveryInfo.get(channelID)
                    .getMessageStatusHistoryForChannelAsString() + " | ";
        }

        String completeInfo = "[" + messageStatusHistory + "]" + deliveries;
        return completeInfo;

    }

    /**
     * Get message status this message went through as a list
     *
     * @return list of MessageStatus
     */
    public List<MessageStatus> getStatusHistory() {
        return messageStatus;
    }

    /**
     * Get current status of the message
     *
     * @return message status
     */
    public MessageStatus getLatestState() {
        MessageStatus latest = null;
        if (messageStatus.size() > 0) {
            latest = messageStatus.get(messageStatus.size() - 1);
        }
        return latest;
    }

    /**
     * Check if this message is to be redelivered. This method should be evaluated before calling
     * markAsDeliveredToChannel method.
     *
     * @param channelID ID of the channel to deliver
     * @return if message is a redelivery
     */
    public boolean isRedelivered(UUID channelID) {
        Integer numOfDeliveries = channelDeliveryInfo.get(channelID).getDeliveryCount();
        return numOfDeliveries > 0;
    }

    /**
     * Mark the message as buffered. Buffered messages will be scheduled to the subscribers.
     */
    public void markAsBuffered() {
        addMessageStatus(MessageStatus.BUFFERED);
    }

    /**
     * Mark message as scheduled to deliver to given subscribers
     *
     * @param localSubscriptions local subscriptions to deliver. AMQP/MQTT subscribers have individual
     *                           delivery channels
     */
    public void markAsScheduledToDeliver(Collection<LocalSubscription> localSubscriptions) {
        for (LocalSubscription subscription : localSubscriptions) {
            ChannelInformation channelInformation = channelDeliveryInfo.get(subscription.getChannelID());
            if (null == channelInformation) {
                channelInformation = new ChannelInformation();
                channelDeliveryInfo.put(subscription.getChannelID(), channelInformation);
            }
        }
        addMessageStatus(MessageStatus.SCHEDULED_TO_SEND);
    }

    /**
     * Mark message as scheduled to deliver to given subscriber
     *
     * @param subscription subscription to deliver message
     */
    public void markAsScheduledToDeliver(LocalSubscription subscription) {
        ChannelInformation channelInformation = channelDeliveryInfo.get(subscription.getChannelID());
        if (null == channelInformation) {
            channelInformation = new ChannelInformation();
            channelDeliveryInfo.put(subscription.getChannelID(), channelInformation);
        }
        addMessageStatus(MessageStatus.SCHEDULED_TO_SEND);
    }

    /**
     * Mark the message as dispatched to given channel (subscriber). This is the
     * First status of a message recorded channel-wise
     *
     * @param channelID ID of the channel
     */
    public void markAsDispatchedToDeliver(UUID channelID) {
        ChannelInformation channelInformation = channelDeliveryInfo.get(channelID);
        channelInformation.addChannelStatus(ChannelMessageStatus.DISPATCHED);

        if (!this.isBeyondLastRollbackedMessage) {
            channelInformation.incrementDeliveryCount();
        } else {
            // No need to increase deliveryCount if this message is beyond the last rollback.
            MessageTracer.trace(getMessageID(), getDestination(), MessageTracer.MESSAGE_BEYOND_LAST_ROLLBACK);
        }
    }

    /**
     * Record acknowledge by channel
     *
     * @param channelID Id of the channel
     * @return if acknowledges by all the channels are received
     */
    public boolean markAsAcknowledgedByChannel(UUID channelID) {
        boolean isAcknowledgedByAll = false;
        ChannelInformation channelInformation = channelDeliveryInfo.get(channelID);
        channelInformation.addChannelStatus(ChannelMessageStatus.ACKED);
        channelDeliveryInfo.put(channelID, channelInformation);

        if (isMarkAsAcked()) {
            addMessageStatus(MessageStatus.ACKED_BY_ALL);
            isAcknowledgedByAll = true;
        }
        return isAcknowledgedByAll;
    }

    /**
     * Record the NAK/REJECT by a channel
     *
     * @param channelID ID of the channel
     */
    public void markAsNackedByClient(UUID channelID) {
        ChannelInformation channelInformation = channelDeliveryInfo.get(channelID);
        channelInformation.addChannelStatus(ChannelMessageStatus.NACKED);
    }

    /**
     * NAK has received repeatedly by channel. Mark message as permanently rejected
     * by subscriber associated with channel.
     *
     * @param channelID ID of the channel
     */
    public void markAsRejectedByClient(UUID channelID) {
        ChannelInformation channelInformation = channelDeliveryInfo.get(channelID);
        channelInformation.addChannelStatus(ChannelMessageStatus.CLIENT_REJECTED);
    }

    /**
     * Mark as a Dead Letter Channel message
     */
    public void markAsDLCMessage() {
        addMessageStatus(MessageStatus.DLC_MESSAGE);
    }

    /**
     * Check if message is sent to DLC
     *
     * @return true if conditions met
     */
    public boolean isDLCMessage() {
        return getLatestState().equals(MessageStatus.DLC_MESSAGE);
    }

    /**
     * Check if message is acknowledged by all channels message is scheduled
     *
     * @return true if message is acknowledged
     */
    public boolean isAknowledgedByAll() {
        return getLatestState().equals(MessageStatus.ACKED_BY_ALL);
    }

    /**
     * Check if message is deleted or purged
     *
     * @return true if conditions met
     */
    public boolean isPurgedOrDeletedOrExpired() {
        MessageStatus currentStatus = getLatestState();
        return currentStatus.equals(MessageStatus.PURGED) || currentStatus.equals(MessageStatus.DELETED)
                || currentStatus.equals(MessageStatus.EXPIRED);
    }

    /**
     * Check if the message is OK to clear from memory
     *
     * @return true if conditions are met
     */
    public boolean isOKToDispose() {
        return MessageStatus.isOKToRemove(messageStatus);
    }

    /**
     * Mark ad purged message
     */
    public void markAsPurgedMessage() {
        addMessageStatus(MessageStatus.PURGED);
    }

    /**
     * Mark as deleted message
     */
    public void markAsDeletedMessage() {
        addMessageStatus(MessageStatus.DELETED);
    }

    /**
     * Check if metadata is stale
     *
     * @return true if message is stale
     */
    public boolean isStale() {
        return stale;
    }

    public void markAsStale() {
        stale = true;
    }

    /**
     * Mark as slot removed message
     */
    public void markAsSlotRemoved() {
        addMessageStatus(MessageStatus.SLOT_REMOVED);
    }

    /**
     * Due to last subscription close of local node
     * slots can get returned to slot coordinator. There we mark
     * the messages in that slot as slot returned
     */
    public void markAsSlotReturned() {
        addMessageStatus(MessageStatus.SLOT_RETURNED);
    }

    /**
     * Cancel message delivery for channel. This is called when a
     * message delivery is failed from broker side. By the time message MUST be maked
     * as SENT (we assume and mark SENT before actual send) to this channel
     *
     * @param channelID id of the channel
     * @return current number of times this message is delivered to the given channel
     */
    public int markDeliveryFailureOfASentMessage(UUID channelID) {
        channelDeliveryInfo.get(channelID).
                addChannelStatus(ChannelMessageStatus.SEND_FAILED);
        return channelDeliveryInfo.get(channelID).decrementDeliveryCount();
    }

    /**
     * Cancel message delivery for channel. This is called when a
     * message delivery is failed from broker side. By the time message MUST be maked
     * as DISPATCHED to this channel. Here we do not decrement delivery count so that
     * if delivery failure is consistent it is checked by max delivery count rule and
     * ultimately sent to DLC.
     *
     * @param channelID id of the channel message is sent
     */
    public void markDeliveryFailureByProtocol(UUID channelID) {
        channelDeliveryInfo.get(channelID).
                addChannelStatus(ChannelMessageStatus.SEND_FAILED);
    }

    /**
     * Evaluate message acknowledgement. Whenever relevant message status are updated
     * this evaluation should be performed and subsequently try to delete the message
     * if ACKED_BY_ALL evaluation returned success
     */
    public void evaluateMessageAcknowledgement() {
        if (isMarkAsAcked()) {
            addMessageStatus(MessageStatus.ACKED_BY_ALL);
        }
    }

    /**
     * Mark the scheduled channel as closed. When the subscriber closes, if the message
     * is already scheduled mark it as closed.
     *
     * @param channelID ID of the channel
     */
    public void markDeliveredChannelAsClosed(UUID channelID) {
        channelDeliveryInfo.get(channelID).
                addChannelStatus(ChannelMessageStatus.CLOSED);
    }

    /**
     * Get the channels this message is delivered to
     *
     * @return Set of channel IDs
     */
    public Set<UUID> getAllDeliveredChannels() {
        return channelDeliveryInfo.keySet();
    }

    /**
     * Check if this message is acknowledged by all the channels it is delivered to
     *
     * @return true if message is acknowledged by all the channels
     */
    private boolean isMarkAsAcked() {
        boolean isAcked = true;
        for (Map.Entry<UUID, ChannelInformation> channelInfoEntry : channelDeliveryInfo.entrySet()) {
            ChannelMessageStatus messageStatus = channelInfoEntry.getValue().getLatestMessageStatus();

            //if channel is closed ignore it from considering
            if (null != messageStatus && messageStatus.equals(ChannelMessageStatus.CLOSED)) {
                continue;
            }
            //if message is rejected by client repeatedly ignore it from considering
            if (null != messageStatus && messageStatus.equals(ChannelMessageStatus.CLIENT_REJECTED)) {
                continue;
            }
            if (null == messageStatus || !messageStatus.equals(ChannelMessageStatus.ACKED)) {
                isAcked = false;
                break;
            }
        }
        if (channelDeliveryInfo.isEmpty()) {
            isAcked = false;
        }
        return isAcked;
    }

    /**
     * Get the number of times this message is delivered to the given channel
     *
     * @param channelID Id of the channel
     * @return number of deliveries
     */
    public int getNumOfDeliveries4Channel(UUID channelID) {
         /* Since sometimes Broker tries to send stored messages when it initialised a subscription
            so then it returns null value for that subscription's channel's amount of deliveries,
            Since we need to the evaluate the rules before we send message, therefore we have to ignore the null value,
            then we have to check the number of deliveries for the particular channel */
        if (null != channelDeliveryInfo.get(channelID)) {
            return channelDeliveryInfo.get(channelID).getDeliveryCount();
        } else {
            return 0;
        }
    }

    /**
     * Check if state going to be added is valid considering it as the next
     * transition compared to current latest state.
     *
     * @param state state to be transferred
     */
    public boolean addMessageStatus(MessageStatus state) {

        boolean isValidTransition = false;

        if (messageStatus.isEmpty()) {
            if (MessageStatus.READ.equals(state)) {
                isValidTransition = true;
                messageStatus.add(state);
            } else {
                log.warn(
                        "Invalid message state transition suggested: " + state + " Message ID: " + getMessageID() +
                                "slot = "
                                + slot.getId());
            }
        } else {
            isValidTransition = messageStatus.get(messageStatus.size() - 1).isValidNextTransition(state);
            if (isValidTransition) {
                messageStatus.add(state);
            } else {
                log.warn("Invalid message state transition from " + messageStatus.get(messageStatus.size() - 1)
                                 + " suggested: " + state + " Message ID: " + getMessageID() + " slot = " + slot.getId()
                                 + " Message Status History >> " + messageStatus);
            }
        }

        return isValidTransition;
    }

    /**
     * Check if state going to be added is valid considering it as the next transition compared
     * to current latest state. This status is for individual delivery channels
     * @param channelID ID of the channel to record status
     * @param status state to be transferred
     */
/*    public boolean addMessageStatusForChannel(UUID channelID, MessageStatus status) {
         return channelDeliveryInfo.get(channelID).addChannelStatus(status);
    }*/

    /**
     * Get message status history as a string.
     *
     * @throws AndesException
     */
    public String dumpMessageStatus() throws AndesException {

        StringBuilder information = new StringBuilder();

        information.append("Message ID ");
        information.append(Long.toString(getMessageID()));
        information.append(',');
        information.append("Message Header ");
        information.append("null");
        information.append(',');
        information.append("Destination ");
        information.append(getDestination());
        information.append(',');
        information.append("Message status ");
        information.append(getStatusHistoryAsString());
        information.append(',');
        information.append("Slot Info {");
        information.append(slot.toString());
        information.append("},");
        information.append("Timestamp ");
        information.append(Long.toString(timeMessageIsRead));
        information.append(',');
        information.append("Expiration time ");
        information.append(Long.toString(getExpirationTime()));
        information.append(',');
        information.append("Channels sent ");
        String deliveries = "";
        for (UUID channelID : getAllDeliveredChannels()) {
            deliveries = deliveries + channelID + " : " + channelDeliveryInfo.get(channelID)
                    .getMessageStatusHistoryForChannelAsString() + " | ";
        }
        information.append(deliveries);
        information.append('\n');

        return information.toString();
    }

    /**
     * Inner class to hold Message status channel-wise
     */
    private class ChannelInformation {

        private Integer channelToNumOfDeliveries = 0;
        private List<ChannelMessageStatus> messageStatusesForChannel = new ArrayList<>(5);

        private int incrementDeliveryCount() {
            channelToNumOfDeliveries = channelToNumOfDeliveries + 1;
            return channelToNumOfDeliveries;
        }

        private int decrementDeliveryCount() {
            channelToNumOfDeliveries = channelToNumOfDeliveries - 1;
            return channelToNumOfDeliveries;
        }

        private int getDeliveryCount() {
            return channelToNumOfDeliveries;
        }

        /**
         * Check if state going to be added is valid considering it as the next transition compared
         * to current latest state. This status is for individual delivery channels
         *
         * @param state state to be transferred
         */
        private boolean addChannelStatus(ChannelMessageStatus state) {

            boolean isValidTransition = false;

            if (messageStatusesForChannel.isEmpty()) {
                if (ChannelMessageStatus.DISPATCHED.equals(state)) {
                    isValidTransition = true;
                    messageStatusesForChannel.add(state);
                } else {
                    log.warn(
                            "Invalid channel message state transition suggested: " + state + " Message ID: " +
                                    getMessageID() + " Slot = " + slot.getId() + " Message Status History >> " +
                                    messageStatus);
                }
            } else {
                isValidTransition = messageStatusesForChannel.
                        get(messageStatusesForChannel.size() - 1).isValidNextTransition(state);

                if (isValidTransition) {
                    messageStatusesForChannel.add(state);
                } else {
                    log.warn("Invalid channel message state transition from " + messageStatusesForChannel
                            .get(messageStatusesForChannel.size() - 1) + " suggested: " + state + " Message ID: "
                                     + getMessageID() + " Slot = " + slot.getId() + " Channel Status History >> "
                                     + messageStatusesForChannel);
                }
            }

            return isValidTransition;
        }

        private ChannelMessageStatus getLatestMessageStatus() {
            if (!messageStatusesForChannel.isEmpty()) {
                return messageStatusesForChannel.get(messageStatusesForChannel.size() - 1);
            } else {
                return null;
            }
        }

        private String getMessageStatusHistoryForChannelAsString() {
            StringBuilder channelInfo = new StringBuilder();
            for (ChannelMessageStatus channelMessageStatus : messageStatusesForChannel) {
                channelInfo.append(channelMessageStatus).append(">>");
            }
            return channelInfo.toString();
        }

    }

    /**
     * Set beyondLastRollbackedMessage
     *
     * @param beyondLastRollbackedMessage true if this message is beyond the last rollbacked message.
     */
    public void setIsBeyondLastRollbackedMessage(boolean beyondLastRollbackedMessage) {
        if (log.isDebugEnabled()) {
            log.debug("setIsBeyondLastRollbackedMessage : " + beyondLastRollbackedMessage);
        }
        isBeyondLastRollbackedMessage = beyondLastRollbackedMessage;
    }

}
