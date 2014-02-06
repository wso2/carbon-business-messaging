/*
 * Copyright 2004,2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wso2.carbon.andes.ui;

public interface Constants {

    public static final String SQS_OPERATION_SEND_MESSAGE = "SendMessage";
    public static final String SQS_OPERATION_RECEIVE_MESSAGE = "ReceiveMessage";
    public static final String SQS_OPERATION_DELETE_MESSAGE = "DeleteMessage";
    public static final String SQS_OPERATION_CHANGE_MESSAGE_VISIBILITY = "ChangeMessageVisibility";
    public static final String SQS_OPERATION_GET_QUEUE_ATTRIBUTES = "GetQueueAttributes";

    public static final String MB_QUEUE_CREATED_FROM_SQS_CLIENT = "sqsClient";
    public static final String MB_QUEUE_CREATED_FROM_AMQP = "amqp";

    public static final String SUCCESS = "success";
    public static final String FAILURE = "failure";

}
