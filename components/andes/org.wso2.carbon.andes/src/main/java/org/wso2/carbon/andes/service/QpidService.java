/*
 *  Copyright (c) 2008, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.wso2.carbon.andes.service;

import org.wso2.carbon.andes.commons.QueueDetails;
import org.wso2.carbon.andes.commons.SubscriptionDetails;

/**
 * This is an interface that declares access methods for properties
 * exposed from the qpid component to other Carbon components.
 */
public interface QpidService {
    /**
        * Get the access key that should be used for internal authentication.
        *
        * The access key is generated when the authentication bundle is activated and
        * published with the AuthenticationService
        *
        * @return
        *           Access key
        */
    public String getAccessKey();

    /**
        * Get client id (machine name) that is used to connect to the broker
        *
        * @return
        *           Client ID
        */
    public String getClientID();

    /**
        * Get default virtual host name of the broker
        *
        * @return
        *           Default virtual host name
        */
    public String getVirtualHostName();

    /**
        * Get hostname of the machine that broker (i.e.Carbon) runs on
        *
        * @return
        *           Carbon hostname
        */
    public String getHostname();

    /**
        * Get TCP port of the broker
        *
        * @return
        *           Broker TCP port
        */
    public String getPort();

    /**
        * Get In-VM AMQP connection URL for internal components
        *
        * @param username
        *           Authentication user
        * @return
        *           Internal AMQP connection URL
        */
    public String getInVMConnectionURL(String username);

    /**
        * Get AMQP connection URL for external applications
        *
        * @param username
        *           Authentication username
        * @param password
        *           Authentication password
        * @return
        *           External AMQP connection URL 
        */
    public String getTCPConnectionURL(String username, String password);

    /**
        * Get AMQP connection URL for external applications
        *
        * @param username
        *           Authentication username
        * @param password
        *           Authentication password
        * @param clientID
        *           Owner name  
        * @return
        *           External AMQP connection URL
        */
    public String getTCPConnectionURL(String username, String password, String clientID);


    /**
        * Get AMQP connection URL for internal applications
        *
        * @param username
        *           Authentication username
        * @param password
        *           Authentication password
        * @return
        *           External AMQP connection URL
        */
    public String getInternalTCPConnectionURL(String username, String password);

    /**
        * Get AMQP connection URL for internal applications
        *
        * @param username
        *           Authentication username
        * @param password
        *           Authentication password
        * @param clientID
        *           Owner name
        * @return
        *           External AMQP connection URL
        */
    public String getInternalTCPConnectionURL(String username, String password, String clientID);

    /**
        * Get Qpid home directory
        *
        * @return
        *           Absolute path to the location where Qpid configuration files reside 
        */
    public String getQpidHome();

    /**
        * Get details about all queues (durable/non-durable) created in the broker
        *
        * @param isDurable
        *               Durable queues? 
        * @return
        *           An array of QueueDetails objects 
        */
    public QueueDetails[] getQueues(boolean isDurable);

    /**
        * Get details about all subscriptions (durable/non-durable) created in the broker
        *
        * @param topic
        *                Name of the topic that subscriptions bound to
        * @param isDurable
        *               Durable subscriptions?  
        * @return
        */
    public SubscriptionDetails[] getSubscriptions(String topic, boolean isDurable);

    /**
     * Returns the SSL port that a client can use to comminicate with the broker over SSL
     * @return the SSL port 
     */
    public String getSSLPort();

    /**
     * Return if <sslOnly> option is enabled for embedded-qpid
     * @return
     */
    public boolean getIfSSLOnly();

    /**
     * Returns whether external cassandra server required
     * @return required status of external cassandra server*/
    public boolean isExternalCassandraServerRequired();


    /**
     * Returns weather external zookeeper server required
     * @return required status of external
     */
    public boolean isExternalZookeeperServerRequired();

    /**
     * Returns Cassandra Connection String used in the internal broker
     * @return cassandra connection string
     */
    public String getCassandraConnectionString();


    /**
     * Returns Zookeeper Connection String used in the internal broker
     * @return zookeeper connection string
     */
    public String getZookeeperConnectionString();


    public int getCassandraConnectionPort();

}
