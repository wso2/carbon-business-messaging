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

package org.wso2.carbon.andes.service.exceptions.mappers;

import org.wso2.carbon.andes.service.exceptions.DLCQueueNotFoundException;
import org.wso2.carbon.andes.service.types.ErrorResponse;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

/**
 * Mapper class for {@link DLCQueueNotFoundException}.
 */
public class DLCQueueNotFoundMapper implements ExceptionMapper<DLCQueueNotFoundException> {
    /**
     * {@inheritDoc}
     */
    @Override
    public Response toResponse(DLCQueueNotFoundException e) {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setTitle("Error occurred while performing an operation as requested DLC does not exist.");
        errorResponse.setCode(Response.Status.NOT_FOUND.getStatusCode());
        errorResponse.setMessage(e.getMessage());
        errorResponse.setDescription("Requested DLC was not found.");
        errorResponse.setMoreInfo("Requested DLC is not available.");

        return Response.status(Response.Status.NOT_FOUND)
                .entity(errorResponse)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .build();
    }
}
