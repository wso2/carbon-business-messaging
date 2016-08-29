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

package org.wso2.carbon.andes.cluster.mgt.ui;

import org.apache.axis2.client.Options;
import org.apache.axis2.client.ServiceClient;
import org.apache.axis2.context.ConfigurationContext;
import org.wso2.carbon.andes.mgt.stub.AndesManagerServiceClusterMgtAdminExceptionException;
import org.wso2.carbon.andes.mgt.stub.AndesManagerServiceClusterMgtExceptionException;
import org.wso2.carbon.andes.mgt.stub.AndesManagerServiceStub;

import java.rmi.RemoteException;

/**
 * This class is used to call MB Cluster Manager service from client side
 */
@SuppressWarnings("UnusedDeclaration")
public class ClusterManagerClient {

    /**
     * AndesManagerService generated stubs
     */
    private AndesManagerServiceStub stub;

    /**
     * Constructor for ClusterManagerClient
     *
     * @param configCtx configuration context for server
     * @param backendServerURL server backend url
     * @param cookie session cookie
     * @throws Exception
     */
    @SuppressWarnings("UnusedDeclaration")
    public ClusterManagerClient(ConfigurationContext configCtx, String backendServerURL,
                                String cookie) throws Exception {
        String serviceURL = backendServerURL + "AndesManagerService";
        stub = new AndesManagerServiceStub(configCtx, serviceURL);
        ServiceClient client = stub._getServiceClient();
        Options options = client.getOptions();
        options.setManageSession(true);
        options.setProperty(org.apache.axis2.transport.http.HTTPConstants.COOKIE_STRING, cookie);
    }

    /**
     * Return addresses of the nodes in a cluster
     *
     * @return the addresses
     */
    public String[] getAllClusterNodeAddresses()
            throws AndesManagerServiceClusterMgtAdminExceptionException, RemoteException,
                   AndesManagerServiceClusterMgtExceptionException {

        return stub.getAllClusterNodeAddresses();
    }

    /**
     * check if broker is running in clustered mode
     *
     * @return whether clustering is enabled
     * @throws AndesManagerServiceClusterMgtExceptionException
     * @throws RemoteException
     */
    public boolean isClusteringEnabled()
            throws AndesManagerServiceClusterMgtExceptionException, RemoteException {
        return stub.isClusteringEnabled();
    }

    /**
     * get node ID assigned to this node in the cluster
     *
     * @return the current node's ID
     * @throws AndesManagerServiceClusterMgtExceptionException
     * @throws RemoteException
     */
    public String getMyNodeID()
            throws AndesManagerServiceClusterMgtExceptionException, RemoteException {
        return stub.getMyNodeID();
    }

    /**
     * Gets the message store's health status
     *
     * @return true if healthy, else false.
     */
    public boolean getStoreHealth() throws AndesManagerServiceClusterMgtExceptionException, RemoteException {
        return stub.getStoreHealth();
    }
}
