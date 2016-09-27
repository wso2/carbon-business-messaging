/*
* Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
* WSO2 Inc. licenses this file to you under the Apache License,
* Version 2.0 (the "License"); you may not use this file except
* in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied. See the License for the
* specific language governing permissions and limitations
* under the License.
*/
package org.wso2.carbon.andes.discovery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.andes.server.cluster.IpAddressRetriever;
import org.wso2.andes.server.cluster.MultipleAddressDetails;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class LocalCluster implements IpAddressRetriever {

        /**
         * IP address of all the interfaces in node.
         */

        private List<MultipleAddressDetails> multipleAddressDetailsList = new ArrayList<>();
        /**
         * IP address of one of interface.
         */
        private String localIpAddress = null;

        /**
         * Class logger.
         */
        protected final Logger logger = LoggerFactory.getLogger(LocalCluster.class);

        /**
         * Get the interface bind ip address.
         */
        private void lookupLocalAddress() {
            try {
            /* This class represents a Network Interface made up of a name,
             and a list of IP addresses assigned to this interface.
             */
                Enumeration<NetworkInterface> networkInterfaces = NetworkInterface
                        .getNetworkInterfaces();

                while (networkInterfaces.hasMoreElements()) {
                    NetworkInterface networkInterface = networkInterfaces
                            .nextElement();
                /* Check for mac address */
                    byte[] hardwareAddress = networkInterface.getHardwareAddress();
                    if ((hardwareAddress == null) || (hardwareAddress.length == 0)) {
                        continue;
                    }
                    Set<InetAddress> subAddresses = new HashSet<InetAddress>();
                    Enumeration<NetworkInterface> subInterfaces = networkInterface.getSubInterfaces();
                    while (subInterfaces.hasMoreElements()) {
                        NetworkInterface subInterface = subInterfaces.nextElement();
                        for (InterfaceAddress interfaceAddress : subInterface
                                .getInterfaceAddresses()) {
                            subAddresses.add(interfaceAddress.getAddress());
                        }
                    }
                    List<InterfaceAddress> interfaceAddresses = networkInterface
                            .getInterfaceAddresses();
                    for (InterfaceAddress interfaceAddress : interfaceAddresses) {
                        InetAddress address = interfaceAddress.getAddress();
                        if (address.isLoopbackAddress()) {
                            continue;
                        }
                   /* if (subAddresses.contains(address)) {
                        continue;
                    }*/

                        if (address instanceof Inet4Address) {

                            MultipleAddressDetails multipleAddressDetails = new MultipleAddressDetails
                                    (networkInterface.getDisplayName(),address.getHostAddress());
                            multipleAddressDetailsList.add(multipleAddressDetails);

                        }
                    }
                }
            } catch (SocketException e) {
                logger.error("Error occurred while retrieving IP address",e);
            }
            if (localIpAddress == null) {
                try {
                    localIpAddress = InetAddress.getLocalHost().getHostAddress();
                } catch (UnknownHostException e) {
                    localIpAddress = "127.0.0.1";
                }
            }
        }

        /**
         * {@inheritDoc}
         */
        public List<MultipleAddressDetails> getLocalAddress() {

                 lookupLocalAddress();
                //return localIpAddress;
                return multipleAddressDetailsList;

        }


}
