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

import java.net.Socket;


public class TestServerAjaxProcessorHelper {


    public boolean isNotNullOrEmpty(String string){
        return string != null && !string.equals("");
    }

    public String backendServerExists(String URL) {
       String response = "true";
        Socket serverSocket;
        String[] URLArray = URL.split(",");


        for (int count = 0; count < URLArray.length; count++) {

            try {
                String serverIp = URLArray[count].split("://")[1].split(":")[0];
                String authPort = URLArray[count].split("://")[1].split(":")[1];


                serverSocket = new Socket(serverIp, Integer.parseInt(authPort));


            } catch (Exception e) {
                response = "false";

            }


        }


        return response;
    }
}
