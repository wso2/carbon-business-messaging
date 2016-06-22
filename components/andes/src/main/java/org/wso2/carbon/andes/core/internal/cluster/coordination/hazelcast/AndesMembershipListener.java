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

package org.wso2.carbon.andes.core.internal.cluster.coordination.hazelcast;

import com.hazelcast.core.Member;
import com.hazelcast.core.MemberAttributeEvent;
import com.hazelcast.core.MembershipEvent;
import com.hazelcast.core.MembershipListener;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.andes.core.internal.cluster.HazelcastClusterAgent;

/**
 * This class act as the listener to handle cluster node added and removed actions.
 */
public class AndesMembershipListener implements MembershipListener {
    private static Log log = LogFactory.getLog(AndesMembershipListener.class);

    /**
     * This holds the Hazelcast agent instance for this broker. Used to check if the current node is the coordinator
     */
    private final HazelcastClusterAgent hazelcastAgent;

    /**
     * Default Constructor
     *
     * @param hazelcastAgent Hazelcast agent for current node
     */
    public AndesMembershipListener(HazelcastClusterAgent hazelcastAgent) {
        this.hazelcastAgent = hazelcastAgent;
    }

    /**
     * This is triggered when a new member joined to the cluster.
     *
     * @param membershipEvent contains the information about the added node.
     */
    @Override
    public void memberAdded(MembershipEvent membershipEvent) {
        Member member = membershipEvent.getMember();
        log.info("Handling cluster gossip: New member joined to the cluster. Member Socket Address:"
                         + member.getSocketAddress() + " UUID:" + member.getUuid());

        hazelcastAgent.memberAdded(membershipEvent.getMember());
    }

    /**
     * Invoked when an attribute of a member was changed.
     *
     * @param memberAttributeEvent information about the changed member attribute
     */
    @Override
    public void memberAttributeChanged(MemberAttributeEvent memberAttributeEvent) {
        // do nothing here, since member attributes are not used in the implementation
    }

    /**
     * This is triggered when a node left the cluster.
     *
     * @param membershipEvent contains the information about the removed node.
     */
    @Override
    public void memberRemoved(MembershipEvent membershipEvent) {
        Member member = membershipEvent.getMember();
        log.info("Handling cluster gossip: A member left the cluster. Member Socket Address:"
                         + member.getSocketAddress() + " UUID:" + member.getUuid());

        try {
            hazelcastAgent.memberRemoved(member);
        } catch (Exception e) {
            log.error("Error while handling node removal, NodeID:" + hazelcastAgent.getIdOfNode(member), e);
        }
    }
}
