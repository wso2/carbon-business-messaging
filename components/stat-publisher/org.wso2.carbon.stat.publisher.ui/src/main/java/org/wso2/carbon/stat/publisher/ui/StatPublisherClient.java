/*
*  Copyright (c) 2005-2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*  WSO2 Inc. licenses this file to you under the Apache License,
*  Version 2.0 (the "License"); you may not use this file except
*  in compliance with the License.
*  You may obtain a copy of the License at
*
*    http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied.  See the License for the
* specific language governing permissions and limitations
* under the License.
*/

package org.wso2.carbon.stat.publisher.ui;

import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.wso2.carbon.stat.publisher.conf.xsd.StatPublisherConfiguration;

import java.rmi.RemoteException;

/**
 * This class will handle client side of UI request and connect that request to server side
 */
public class StatPublisherClient {

    private StatPublisherServiceStub stub;

    public StatPublisherClient(ConfigurationContext configCtx, String backendServerURL, String cookie) throws Exception {
        String serviceURL = backendServerURL + "StatPublisherService";
        stub = new StatPublisherServiceStub(configCtx, serviceURL);
        ServiceClient client = stub._getServiceClient();
        Options options = client.getOptions();
        options.setManageSession(true);
        options.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, cookie);

    }

    /**
     * get statistic configuration from StatPublisher server component
     *
     * @return stat publisher configurations store in registry
     * @throws StatPublisherServiceStatPublisherConfigurationExceptionException
     */
    public StatPublisherConfiguration getStatConfiguration() throws StatPublisherServiceStatPublisherConfigurationExceptionException {

        StatPublisherConfiguration statConfigurationInstance;
        try {
            statConfigurationInstance = stub.getStatConfiguration();
        } catch (RemoteException e) {
            statConfigurationInstance = new StatPublisherConfiguration();
        }

        return statConfigurationInstance;
    }

    /**
     * send statistic configuration to StatPublisher server component
     *
     * @param statPublisherConfiguration stat publisher configurations that set through UI
     * @return Configuration Saved Successfully if process is success else return exception message
     */
    public String setStatConfiguration(StatPublisherConfiguration statPublisherConfiguration) {
        String response;

        try {
            stub.setStatConfiguration(statPublisherConfiguration);
            response = "Configuration Saved Successfully";
        } catch (RemoteException e) {
            return e + "";
        } catch (StatPublisherServiceStatPublisherConfigurationExceptionException e) {
            return e + "";
        }

        return response;
    }


}
