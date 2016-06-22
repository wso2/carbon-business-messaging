/*
 * Copyright (c) 2016, WSO2 Inc. (http://wso2.com) All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.andes.tests.unit;

import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import org.wso2.carbon.andes.core.AndesException;
import org.wso2.carbon.andes.core.DestinationType;
import org.wso2.carbon.andes.core.ProtocolType;
import org.wso2.carbon.andes.service.exceptions.DestinationManagerException;
import org.wso2.carbon.andes.service.managers.DestinationManagerService;
import org.wso2.carbon.andes.service.managers.bean.impl.DestinationManagerServiceBeanImpl;
import org.wso2.carbon.andes.service.types.Destination;
import org.wso2.carbon.andes.tests.core.UnitBaseTest;

/**
 * Tests the implementation for destination manager services.
 */
public class DestinationManagerServiceBeanImplTestCase extends UnitBaseTest {

    DestinationManagerService destinationManagerService;

    @BeforeTest
    public void init() {
        destinationManagerService = new DestinationManagerServiceBeanImpl();
    }

    /**
     * Creates a new queue destination.
     *
     * @throws DestinationManagerException
     */
    @Test(expectedExceptions = {org.wso2.carbon.andes.service.exceptions.DestinationManagerException.class,
                                NullPointerException.class})
    public void createQueue() throws DestinationManagerException, AndesException {
        // QueueManagementBeans beans = new QueueManagementBeans();
        // beans.createQueue("queue1", "admin");
        Destination destination = destinationManagerService.createDestination("amqp", "queue", "testQueue");
        Assert.assertEquals(destination.getProtocol(), new ProtocolType("AMQP", "0-91"),
                "Invalid protocol type assigned");
        Assert.assertEquals(destination.getDestinationType(), DestinationType.QUEUE, "Invalid destination type " +
                                                                                     "assigned");
        Assert.assertEquals(destination.getDestinationName(), "testQueue", "Invalid destination name assigned");
    }
}
