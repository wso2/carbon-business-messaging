/*
 * Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing,
 *   software distributed under the License is distributed on an
 *   "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *   KIND, either express or implied.  See the License for the
 *   specific language governing permissions and limitations
 *   under the License.
 */

package org.wso2.carbon.andes.service;

import org.wso2.carbon.andes.commons.QueueDetails;
import org.wso2.carbon.andes.commons.SubscriptionDetails;
import org.wso2.carbon.andes.listeners.BrokerLifecycleListener;

import javax.xml.stream.XMLStreamException;
import java.io.FileNotFoundException;

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

    public Integer getAMQPPort();

    public void setAMQPPort(Integer amqpPort);

    public Integer getAMQPSSLPort();

    public void setAMQPSSLPort(Integer amqpSSLPort);

    public Integer getMqttPort();

    public void setMqttPort(Integer mqttPort);

    public Integer getMqttSSLPort();

    public void setMqttSSLPort(Integer mqttSSLPort);

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
        * @return An array of SubscriptionDetails objects.
        */
    public SubscriptionDetails[] getSubscriptions(String topic, boolean isDurable);

    /**
     * Return if {@code<sslOnly>} option is enabled for embedded-qpid
     *
     * @return boolean representing whether sslOnly option is enabled.
     * @throws Exception Thrown when an error occurs while figuring out whether {@code<sslOnly>} option is enabled
     */
    public boolean getIfSSLOnly() throws Exception;

    /**
     * Register broker lifecycle listener.
     *
     * @param brokerLifecycleListener The brokerLifecycleListener to be registered.
     */
    public void registerBrokerLifecycleListener(BrokerLifecycleListener brokerLifecycleListener);


}
