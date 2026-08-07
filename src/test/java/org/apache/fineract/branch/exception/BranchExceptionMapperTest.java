/**
 * Copyright 2026   Mifos Initiative
 */
package org.apache.fineract.branch.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.ws.rs.core.Response;
import org.apache.fineract.branch.data.ApiErrorData;
import org.junit.jupiter.api.Test;

class BranchExceptionMapperTest {

    private final BranchExceptionMapper mapper = new BranchExceptionMapper();

    @Test
    void mapsToJsonResponse() {
        BranchApiException ex = BranchApiException.notFound("CLIENT_NOT_FOUND", "Cliente no encontrado");
        Response response = mapper.toResponse(ex);

        assertEquals(404, response.getStatus());
        ApiErrorData body = (ApiErrorData) response.getEntity();
        assertEquals("CLIENT_NOT_FOUND", body.getCode());
        assertEquals("Cliente no encontrado", body.getMessage());
        assertEquals(404, body.getStatus());
        assertNotNull(body.getTraceId());
        assertNotNull(body.getTimestamp());
    }

    @Test
    void mapsConflict() {
        BranchApiException ex = BranchApiException.conflict("PAYMENT_DUPLICATE", "dup");
        Response response = mapper.toResponse(ex);
        assertEquals(409, response.getStatus());
        ApiErrorData body = (ApiErrorData) response.getEntity();
        assertEquals("PAYMENT_DUPLICATE", body.getCode());
    }
}
