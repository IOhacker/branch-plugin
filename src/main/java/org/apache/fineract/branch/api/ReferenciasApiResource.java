/**
 * Copyright 2026   Mifos Initiative
 */
package org.apache.fineract.branch.api;

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
import org.apache.fineract.branch.data.PaymentValidationRequestData;
import org.apache.fineract.branch.data.PaymentValidationResultData;
import org.apache.fineract.branch.data.RefundReferenceResolutionData;
import org.apache.fineract.branch.service.RefundReferenceService;
import org.springframework.stereotype.Component;

@Path("/v1.0/referencias")
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
