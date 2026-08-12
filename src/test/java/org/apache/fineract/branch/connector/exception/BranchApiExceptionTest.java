/**
 * Copyright since 2026 Mifos Initiative
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.apache.fineract.branch.connector.exception;

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
