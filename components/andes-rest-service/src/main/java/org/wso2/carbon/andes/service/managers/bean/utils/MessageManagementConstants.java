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

package org.wso2.carbon.andes.service.managers.bean.utils;

/**
 * Stores message management constants which are used to contact through MBeans.
 */
public class MessageManagementConstants {
    public static final String MESSAGE_OBJECT_NAME
                                = "org.wso2.andes:type=MessageManagementInformation,name=MessageManagementInformation";
    public static final String BROWSE_DESTINATIONS_WITH_MESSAGE_ID_MBEAN_ATTRIBUTE = "browseDestinationWithMessageID";
    public static final String BROWSE_DESTINATIONS_WITH_OFFSET_MBEAN_ATTRIBUTE = "browseDestinationWithOffset";
    public static final String GET_MESSAGE_MBEAN_ATTRIBUTE = "getMessage";
    public static final String DELETE_MESSAGE_MBEAN_OPERATION = "deleteMessages";
}
