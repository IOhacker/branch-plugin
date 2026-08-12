/**
 * Copyright since 2026 Mifos Initiative
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.apache.fineract.branch.connector.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.branch.connector.data.PaymentValidationRequestData;
import org.apache.fineract.branch.connector.data.PaymentValidationResultData;
import org.apache.fineract.branch.connector.data.RefundReferenceResolutionData;
import org.apache.fineract.branch.connector.service.RefundReferenceService;
import org.springframework.stereotype.Component;

@Path("/v1/referencias")
@Component
@Tag(name = "Referencias propuestas")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
public class ReferenciasApiResource {

    private final RefundReferenceService refundReferenceService;

    @GET
    @Path("/reembolso/{referencia}")
    @Operation(summary = "[PROPUESTO] Resolver referencia de reembolso")
    public RefundReferenceResolutionData resolve(@PathParam("referencia") String referencia) {
        return refundReferenceService.resolve(referencia);
    }

    @POST
    @Path("/reembolso/{referencia}/validate")
    @Operation(summary = "[PROPUESTO] Validar cobro sin registrar movimiento")
    public PaymentValidationResultData validate(@PathParam("referencia") String referencia,
            @Valid PaymentValidationRequestData body) {
        return refundReferenceService.validatePayment(referencia, body);
    }
}
