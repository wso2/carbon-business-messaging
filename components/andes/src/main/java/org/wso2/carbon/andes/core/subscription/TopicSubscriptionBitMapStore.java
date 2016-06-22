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

package org.wso2.carbon.andes.core.subscription;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.andes.core.AndesException;
import org.wso2.carbon.andes.core.AndesSubscription;
import org.wso2.carbon.andes.core.DestinationType;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Store subscriptions according to the respective protocol using bitmaps as the underlying data structure for
 * faster wildcard matching.
 */
public abstract class TopicSubscriptionBitMapStore implements AndesSubscriptionStore {

    private Log log = LogFactory.getLog(TopicSubscriptionBitMapStore.class);

    /**
     * The topic delimiter to differentiate each constituent according to the current subscription type.
     */
    private String constituentsDelimiter;

    /**
     * The multi level matching wildcard according to the current subscription type.
     */
    private String multiLevelWildCard;

    /**
     * The single level matching wildcard according to the current subscription type.
     */
    private String singleLevelWildCard;

    // 'Null' and 'Other' constituents are picked from restricted topic characters

    /**
     * Constituent name to represent that a constituent is not available at this location.
     */
    private static final String NULL_CONSTITUENT = "%null%";

    /**
     * Constituent name to represent any constituent except a wildcard.
     */
    private static final String OTHER_CONSTITUENT = "%other%";

    /**
     * Keeps all the subscriptions.
     */
    private List<AndesSubscription> subscriptionList = new ArrayList<AndesSubscription>();

    /**
     * Keeps all the subscription destinations broken into their constituents.
     */
    private Map<Integer, String[]> subscriptionConstituents = new HashMap<Integer, String[]>();

    /**
     * Keeps all the constituent tables as ListOfConstituentTables <ConstituentPart, BitSet>.
     */
    private List<Map<String, BitSet>> constituentTables = new ArrayList<Map<String, BitSet>>();

    /**
     * Initialize BitMapHandler with the protocol specific properties.
     *
     * @param multiLevelWildCard    The wildcard used to specify any child level of any constituent
     * @param singleLevelWildCard   The wildcard to specify one level of any constituent
     * @param constituentsDelimiter The delimiter to separate constituents
     */
    public TopicSubscriptionBitMapStore(String multiLevelWildCard, String singleLevelWildCard,
                                        String constituentsDelimiter) {
        this.multiLevelWildCard = multiLevelWildCard;
        this.singleLevelWildCard = singleLevelWildCard;
        this.constituentsDelimiter = constituentsDelimiter;
    }

    /**
     * Add a new subscription to the structure.
     *
     * @param subscription The subscription to be added.
     * @throws AndesException
     */
    @Override
    public void addSubscription(AndesSubscription subscription) throws AndesException {
        String destination = subscription.getSubscribedDestination();

        if (StringUtils.isNotEmpty(destination)) {
            if (!isSubscriptionAvailable(subscription)) {
                int newSubscriptionIndex = subscriptionList.size();

                // The index is added to make it clear to which index this is being inserted
                subscriptionList.add(newSubscriptionIndex, subscription);

                String constituents[] = destination.split(Pattern.quote(constituentsDelimiter));

                subscriptionConstituents.put(newSubscriptionIndex, constituents);


                for (int constituentIndex = 0; constituentIndex < constituents.length; constituentIndex++) {
                    String constituent = constituents[constituentIndex];

                    Map<String, BitSet> constituentTable;

                    if ((constituentIndex + 1) > constituentTables.size()) {
                        // No tables exist for this constituent index need to create
                        constituentTable = addConstituentTable(constituentIndex);
                    } else {
                        constituentTable = constituentTables.get(constituentIndex);
                    }

                    if (!constituentTable.keySet().contains(constituent)) {
                        // This constituent is not available in this table. Need to add a new row
                        addConstituentRow(constituent, constituentIndex);
                    }
                }

                addSubscriptionColumn(destination, newSubscriptionIndex);
            } else {
                updateSubscription(subscription);
            }

        } else {
            throw new AndesException("Error adding a new subscription. Subscribed destination is empty.");
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void updateSubscription(AndesSubscription subscription) {
        if (isSubscriptionAvailable(subscription)) {
            // Need to add the new entry to the same index since bitmap logic is dependent on this index
            int index = subscriptionList.indexOf(subscription);

            // Should not allow to modify this list until the update is complete
            // Otherwise the subscription indexes will be invalid
            synchronized (subscriptionList) {
                subscriptionList.remove(index);
                subscriptionList.add(index, subscription);
            }
        }
    }

    /**
     * @param constituentIndex The index to create the constituent for
     * @return The created constituent table
     */
    private Map<String, BitSet> addConstituentTable(int constituentIndex) {
        Map<String, BitSet> constituentTable = new HashMap<String, BitSet>();

        BitSet nullBitSet = new BitSet(subscriptionList.size());
        BitSet otherBitSet = new BitSet(subscriptionList.size());

        // Fill null and other constituent values for all available subscriptions
        for (int subscriptionIndex = 0; subscriptionIndex < subscriptionList.size(); subscriptionIndex++) {
            String[] constituentsOfSubscription = subscriptionConstituents.get(subscriptionIndex);

            if (constituentsOfSubscription.length < constituentIndex + 1) {
                // There is no constituent in this subscription for this constituent index
                nullBitSet.set(subscriptionIndex);

                // If last constituent of the subscription is multiLevelWildCard, then any other is a match
                if (multiLevelWildCard.equals(constituentsOfSubscription[constituentsOfSubscription.length - 1])) {
                    otherBitSet.set(subscriptionIndex);
                }
            } else {
                String subscriptionConstituent = constituentsOfSubscription[constituentIndex];

                // Check if this is a wildcard
                if (multiLevelWildCard.equals(subscriptionConstituent) ||
                        singleLevelWildCard.equals(subscriptionConstituent)) {
                    otherBitSet.set(subscriptionIndex);
                }
            }
        }

        // Add 'null' and 'other' constituent
        constituentTable.put(NULL_CONSTITUENT, nullBitSet);
        constituentTable.put(OTHER_CONSTITUENT, otherBitSet);

        constituentTables.add(constituentIndex, constituentTable);

        return constituentTable;
    }


    /**
     * Run through each constituentTable and insert a new column for a new subscription filling it's values
     * by comparing constituents.
     * <p/>
     * This will only fill values for the already available constituents. Will not add new constituents.
     *
     * @param subscribedDestination The newly subscribed destination
     */
    private void addSubscriptionColumn(String subscribedDestination, int subscriptionIndex) throws AndesException {

        String[] subscribedDestinationConstituents = subscriptionConstituents.get(subscriptionIndex);

        // Create a mock destination with two constituents for 'other' wildcard matching
        String matchDestinationForOther = OTHER_CONSTITUENT + constituentsDelimiter + OTHER_CONSTITUENT;

        // Create a mock destination with three constituents for 'null' wildcard matching
        String matchDestinationForNull = NULL_CONSTITUENT + constituentsDelimiter + NULL_CONSTITUENT +
                constituentsDelimiter + NULL_CONSTITUENT;

        // Loop through each constituent table for the new constituents
        for (int constituentIndex = 0; constituentIndex < subscribedDestinationConstituents.length;
             constituentIndex++) {
            String currentConstituent = subscribedDestinationConstituents[constituentIndex];
            Map<String, BitSet> constituentTable = constituentTables.get(constituentIndex);

            // Loop through each constituent row in the table and fill values
            for (Map.Entry<String, BitSet> constituentRow : constituentTable.entrySet()) {
                String constituentOfCurrentRow = constituentRow.getKey();
                BitSet bitSet = constituentRow.getValue();

                if (constituentOfCurrentRow.equals(currentConstituent)) {
                    bitSet.set(subscriptionIndex);
                } else if (NULL_CONSTITUENT.equals(constituentOfCurrentRow)) {
                    // Check if this constituent being null matches the destination if we match it with
                    // a null constituent
                    String wildcardDestination = NULL_CONSTITUENT + constituentsDelimiter +
                            currentConstituent;
                    bitSet.set(subscriptionIndex, isMatchForProtocolType(wildcardDestination,
                                                                         matchDestinationForNull));
//                    }
                } else if (OTHER_CONSTITUENT.equals(constituentOfCurrentRow)) {
                    // Check if other is matched by comparing wildcard through specific wildcard matching
                    // Create a mock destinations with current constituent added last and check if it match with a
                    // non-wildcard destination match with the corresponding matching method
                    String wildCardDestination = OTHER_CONSTITUENT + constituentsDelimiter + currentConstituent;

                    bitSet.set(subscriptionIndex, isMatchForProtocolType(wildCardDestination,
                                                                         matchDestinationForOther));
                } else if (singleLevelWildCard.equals(currentConstituent) ||
                        multiLevelWildCard.equals(currentConstituent)) {
                    // If there is any wildcard at this position, then this should match.
                    bitSet.set(subscriptionIndex);
                } else {
                    bitSet.set(subscriptionIndex, false);
                }

            }

        }

        int noOfMaxConstituents = constituentTables.size();

        if (noOfMaxConstituents > subscribedDestinationConstituents.length) {
            // There are more constituent tables to be filled. Wildcard matching is essential here.

            boolean matchingOthers = true;
            // The OTHER_CONSTITUENT is added here to represent any constituent
            if (!multiLevelWildCard.equals(subscribedDestinationConstituents[subscribedDestinationConstituents.length
                    - 1])) {
                String otherConstituentComparer = subscribedDestination + constituentsDelimiter + OTHER_CONSTITUENT;
                matchingOthers = isMatchForProtocolType(subscribedDestination, otherConstituentComparer);
            } // Else matchingOthers will be true

            for (int constituentIndex = subscribedDestinationConstituents.length; constituentIndex <
                    noOfMaxConstituents; constituentIndex++) {
                Map<String, BitSet> constituentTable = constituentTables.get(constituentIndex);

                // Loop through each constituent row in the table and fill values
                for (Map.Entry<String, BitSet> constituentRow : constituentTable.entrySet()) {
                    String constituentOfCurrentRow = constituentRow.getKey();
                    BitSet bitSet = constituentRow.getValue();

                    if (NULL_CONSTITUENT.equals(constituentOfCurrentRow)) {
                        // Null constituent is always true here
                        bitSet.set(subscriptionIndex);
                    } else {
                        bitSet.set(subscriptionIndex, matchingOthers);
                    }
                }
            }
        }

    }

    /**
     * Add a new constituent row for the given constituent index table and fill values for already available
     * subscriptions.
     *
     * @param constituent      The constituent to add
     * @param constituentIndex The index of the constituent
     */
    private void addConstituentRow(String constituent, int constituentIndex) {
        Map<String, BitSet> constituentTable = constituentTables.get(constituentIndex);
        BitSet bitSet = new BitSet();

        for (int i = 0; i < subscriptionConstituents.size(); i++) {
            String[] constituentsOfSubscription = subscriptionConstituents.get(i);

            if (constituentIndex < constituentsOfSubscription.length) {
                // Get the i'th subscription's [constituentIndex]'th constituent
                String subscriptionConstituent = constituentsOfSubscription[constituentIndex];
                if (subscriptionConstituent.equals(constituent) || multiLevelWildCard.equals(subscriptionConstituent)
                        || singleLevelWildCard.equals(subscriptionConstituent)) {
                    // The new constituent matches the subscriptions i'th constituent
                    bitSet.set(i);
                } else {
                    // The new constituent does not match the i'th subscriptions [constituentIndex] constituent
                    bitSet.set(i, false);
                }
            } else {
                // The subscription does not have a constituent for this index
                // If the last constituent of the subscription is multiLevelWildCard we match else false
                if (multiLevelWildCard.equals(constituentsOfSubscription[constituentsOfSubscription.length - 1])) {
                    bitSet.set(i);
                } else {
                    bitSet.set(i, false);
                }
            }
        }

        constituentTable.put(constituent, bitSet);
    }

    /**
     * This methods adds a constituent table with only null and other constituents.
     * This is required when a message comes with more than the available number of constituents. If wildcard
     * subscriptions are available for those, they should match. Hence need to create these empty constituent tables.
     */
    private void addEmptyConstituentTable() {
        int noOfSubscriptions = subscriptionList.size();
        Map<String, BitSet> constituentTable = new HashMap<String, BitSet>();

        BitSet nullBitSet = new BitSet(noOfSubscriptions);
        BitSet otherBitSet = new BitSet(noOfSubscriptions);

        if (noOfSubscriptions > 0) {

            // Null constituent will always be true for empty constituents, hence need to flip
            nullBitSet.flip(0, noOfSubscriptions - 1);

            for (int subscriptionIndex = 0; subscriptionIndex < noOfSubscriptions; subscriptionIndex++) {
                // For 'other', if subscribers last constituent is multi level wild card then matching
                String[] allConstituent = subscriptionConstituents.get(subscriptionIndex);
                String lastConstituent = allConstituent[allConstituent.length - 1];

                if (multiLevelWildCard.equals(lastConstituent)) {
                    otherBitSet.set(subscriptionIndex);
                } else {
                    otherBitSet.set(subscriptionIndex, false);
                }
            }
        }

        constituentTable.put(NULL_CONSTITUENT, nullBitSet);
        constituentTable.put(OTHER_CONSTITUENT, otherBitSet);

        constituentTables.add(constituentTable);
    }

    /**
     * Removing a subscription from the structure.
     *
     * @param subscription The subscription to remove
     */
    @Override
    public void removeSubscription(AndesSubscription subscription) {
        int subscriptionIndex = subscriptionList.indexOf(subscription);

        if (subscriptionIndex > -1) {
            for (Map<String, BitSet> constituentTable : constituentTables) {
                for (Map.Entry<String, BitSet> constituentRow : constituentTable.entrySet()) {
                    // For every row create a new BitSet with the values for the removed subscription removed
                    String constituent = constituentRow.getKey();
                    BitSet bitSet = constituentRow.getValue();
                    BitSet newBitSet = new BitSet();

                    int bitIndex = 0;

                    for (int i = 0; i < bitSet.size(); i++) {
                        if (bitIndex == subscriptionIndex) {
                            // If the this is the index to remove then skip this round
                            bitIndex++;
                        }
                        newBitSet.set(i, bitSet.get(bitIndex));
                        bitIndex++;
                    }

                    constituentTable.put(constituent, newBitSet);

                }
            }

            // Remove the subscription from subscription list
            subscriptionList.remove(subscriptionIndex);
        } else {
            log.warn("Subscription for destination : " + subscription.getSubscribedDestination() + " is not found to " +
                             "remove");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSubscriptionAvailable(AndesSubscription subscription) {
        return subscriptionList.contains(subscription);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<AndesSubscription> getMatchingSubscriptions(String destination, DestinationType destinationType) {
        Set<AndesSubscription> subscriptions = new HashSet<>();

        if (StringUtils.isNotEmpty(destination)) {

            // constituentDelimiter is quoted to avoid making the delimiter a regex symbol
            String[] constituents = destination.split(Pattern.quote(constituentsDelimiter), -1);

            int noOfCurrentMaxConstituents = constituentTables.size();

            // If given destination has more constituents than any subscriber has, then create constituent tables
            // for those before collecting matching subscribers
            if (constituents.length > noOfCurrentMaxConstituents) {
                for (int i = noOfCurrentMaxConstituents; i < constituents.length; i++) {
                    addEmptyConstituentTable();
                }
            }

            // Keeps the results of 'AND' operations between each bit sets
            BitSet andBitSet = new BitSet(subscriptionList.size());

            // Since BitSet is initialized with false for each element we need to flip
            andBitSet.flip(0, subscriptionList.size());

            // Get corresponding bit set for each constituent in the destination and operate bitwise AND operation
            for (int constituentIndex = 0; constituentIndex < constituents.length; constituentIndex++) {
                String constituent = constituents[constituentIndex];
                Map<String, BitSet> constituentTable = constituentTables.get(constituentIndex);

                BitSet bitSetForAnd = constituentTable.get(constituent);

                if (null == bitSetForAnd) {
                    // The constituent is not found in the table, hence matching with 'other' constituent
                    bitSetForAnd = constituentTable.get(OTHER_CONSTITUENT);
                }

                andBitSet.and(bitSetForAnd);
            }

            // If there are more constituent tables, get the null constituent in each of them and operate bitwise AND
            for (int constituentIndex = constituents.length; constituentIndex < constituentTables.size();
                 constituentIndex++) {
                Map<String, BitSet> constituentTable = constituentTables.get(constituentIndex);
                andBitSet.and(constituentTable.get(NULL_CONSTITUENT));
            }


            // Valid subscriptions are filtered, need to pick from subscription pool
            int nextSetBitIndex = andBitSet.nextSetBit(0);
            while (nextSetBitIndex > -1) {
                subscriptions.add(subscriptionList.get(nextSetBitIndex));
                nextSetBitIndex = andBitSet.nextSetBit(nextSetBitIndex + 1);
            }

        } else {
            log.warn("Cannot retrieve subscriptions via bitmap handler since destination to match is empty");
        }

        return subscriptions;
    }

    /**
     * Get all the subscriptions currently saved.
     *
     * @return List of all subscriptions
     */
    @Override
    public List<AndesSubscription> getAllSubscriptions() {
        return subscriptionList;
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> getAllDestinations(DestinationType destinationType) {
        Set<String> topics = new HashSet<>();


        for (Map.Entry<Integer, String[]> subcriberConstituent : subscriptionConstituents.entrySet()) {

            StringBuilder topic = new StringBuilder();
            String[] constituents = subcriberConstituent.getValue();

            for (int i = 0; i < constituents.length; i++) {
                String constituent = constituents[i];
                // if this is a wildcard constituent, we provide it as 'ANY' in it's place for readability
                if (multiLevelWildCard.equals(constituent) || singleLevelWildCard.equals(constituent)) {
                    topic.append("ANY");
                } else {
                    topic.append(constituent);
                }

                // append the delimiter if there are more constituents to come
                if ((constituents.length - 1) > i) {
                    topic.append(constituentsDelimiter);
                }

            }

            topics.add(topic.toString());
        }

        return topics;
    }

    /**
     * Returns whether a wildcard matches to a given destination according to the implemented protocol.
     * Protocol specific implementation should be here.
     *
     * @param wildcardDestination    The wildcard to match
     * @param nonWildcardDestination The non-wildcard destination to match against
     * @return True if non wildcard destination is implied by the wildcard
     */
    protected abstract boolean isMatchForProtocolType(String wildcardDestination, String nonWildcardDestination);


}
