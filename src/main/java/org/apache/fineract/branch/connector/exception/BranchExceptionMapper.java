/**
 * Copyright since 2026 Mifos Initiative
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.apache.fineract.branch.connector.exception;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.apache.fineract.branch.connector.data.ApiErrorData;
import org.springframework.stereotype.Component;

@Provider
@Component
public class BranchExceptionMapper implements ExceptionMapper<BranchApiException> {

    @Override
    public Response toResponse(BranchApiException exception) {
        String traceId = UUID.randomUUID().toString();
        ApiErrorData body = ApiErrorData.builder().timestamp(OffsetDateTime.now()).status(exception.getHttpStatus())
                .code(exception.getCode()).message(exception.getMessage()).traceId(traceId)
                .details(exception.getDetails()).build();
        return Response.status(exception.getHttpStatus()).entity(body).type(MediaType.APPLICATION_JSON).build();
    }
}
