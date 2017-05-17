/*
 * Copyright (c) 2017, WSO2 Inc. (http://wso2.com) All Rights Reserved.
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

package org.wso2.carbon.business.messaging.admin.services.exception.mappers;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.wso2.carbon.business.messaging.admin.services.exception.InternalServerException;
import org.wso2.carbon.business.messaging.admin.services.types.ErrorResponse;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

/**
 * Mapper class for {@link InternalServerException}.
 */
public class InternalServerErrorMapper implements ExceptionMapper<InternalServerException> {
    /**
     * {@inheritDoc}
     */
    @Override
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response toResponse(InternalServerException e) {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setTitle("Error occurred in the server while performing an operation.");
        errorResponse.setCode(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        errorResponse.setMessage(e.getMessage());
        errorResponse.setDescription("Error occurred in the server.");
        errorResponse.setMoreInfo("Please contact administrator." + System.lineSeparator()
                                    + ExceptionUtils.getFullStackTrace(e));

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorResponse).build();
    }
}
