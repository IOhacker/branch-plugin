/**
 * Copyright 2026   Mifos Initiative
 */
package org.apache.fineract.branch.exception;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.apache.fineract.branch.data.ApiErrorData;
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
