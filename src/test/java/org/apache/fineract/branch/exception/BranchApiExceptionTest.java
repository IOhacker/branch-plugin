/**
 * Copyright 2026   Mifos Initiative
 */
package org.apache.fineract.branch.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class BranchApiExceptionTest {

    @Test
    void notFoundFactory() {
        BranchApiException ex = BranchApiException.notFound("CLIENT_NOT_FOUND", "missing");
        assertEquals(404, ex.getHttpStatus());
        assertEquals("CLIENT_NOT_FOUND", ex.getCode());
        assertEquals("missing", ex.getMessage());
        assertNull(ex.getDetails());
    }

    @Test
    void badRequestFactory() {
        BranchApiException ex = BranchApiException.badRequest("INVALID_REQUEST", "bad");
        assertEquals(400, ex.getHttpStatus());
        assertEquals("INVALID_REQUEST", ex.getCode());
    }

    @Test
    void conflictFactory() {
        BranchApiException ex = BranchApiException.conflict("PAYMENT_DUPLICATE", "dup");
        assertEquals(409, ex.getHttpStatus());
    }

    @Test
    void unprocessableFactory() {
        BranchApiException ex = BranchApiException.unprocessable("LOAN_NOT_DISBURSED", "nd");
        assertEquals(422, ex.getHttpStatus());
    }

    @Test
    void forbiddenFactory() {
        BranchApiException ex = BranchApiException.forbidden("FORBIDDEN", "no");
        assertEquals(403, ex.getHttpStatus());
    }
}
