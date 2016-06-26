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

import org.osgi.service.component.annotations.Component;
import org.wso2.carbon.andes.service.exceptions.InvalidLimitValueException;
import org.wso2.carbon.andes.service.types.ErrorResponse;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

/**
 * Mapper class for {@link InvalidLimitValueException}.
 */
@Component(
        name = "org.wso2.carbon.andes.exception.InvalidLimitValueMapper",
        service = ExceptionMapper.class,
        immediate = true
)
public class InvalidLimitValueMapper implements ExceptionMapper<InvalidLimitValueException> {
    /**
     * {@inheritDoc}
     */
    @Override
    public Response toResponse(InvalidLimitValueException e) {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setTitle("The entered value for limit is invalid.");
        errorResponse.setCode(Response.Status.BAD_REQUEST.getStatusCode());
        errorResponse.setMessage("The entered value for limit is invalid. The value should be greater than 0.");
        errorResponse.setDescription("Invalid value for limit query param.");
        errorResponse.setMoreInfo("Limit value should be greater than 0. See swagger documentation for more " +
                                  "information.");

        return Response.status(Response.Status.BAD_REQUEST)
                .entity(errorResponse)
                .type(MediaType.APPLICATION_JSON_TYPE)
                .build();
    }
}
