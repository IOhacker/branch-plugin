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
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.branch.connector.data.BranchPaymentRequestData;
import org.apache.fineract.branch.connector.data.BranchPaymentResponseData;
import org.apache.fineract.branch.connector.data.ReconciliationItemData;
import org.apache.fineract.branch.connector.data.ReversalRequestData;
import org.apache.fineract.branch.connector.service.BranchPaymentService;
import org.springframework.stereotype.Component;

@Path("/v1/sucursales")
@Component
@Tag(name = "Pagos de sucursal propuestos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
public class SucursalesApiResource {

    private final BranchPaymentService branchPaymentService;

    @POST
    @Path("/pagos")
    @Operation(summary = "[PROPUESTO] Registrar pago atómico de sucursal")
    public Response createBranchPayment(@HeaderParam("Idempotency-Key") String idempotencyKey,
            @Valid BranchPaymentRequestData body) {
        BranchPaymentResponseData result = branchPaymentService.createBranchPayment(body, idempotencyKey);
        return Response.status(Response.Status.CREATED).entity(result).build();
    }

    @GET
    @Path("/pagos/{externalId}")
    @Operation(summary = "[PROPUESTO] Consultar pago por llave idempotente")
    public BranchPaymentResponseData getByExternalId(@PathParam("externalId") String externalId) {
        return branchPaymentService.getByExternalId(externalId);
    }

    @POST
    @Path("/pagos/{externalId}/reversal")
    @Operation(summary = "[PROPUESTO] Reversar pago autorizado")
    public BranchPaymentResponseData reverse(@PathParam("externalId") String externalId,
            @Valid ReversalRequestData body) {
        return branchPaymentService.reverse(externalId, body);
    }

    @GET
    @Path("/{branchId}/pagos")
    @Operation(summary = "[PROPUESTO] Consultar pagos para conciliación")
    public List<ReconciliationItemData> getBranchPayments(@PathParam("branchId") String branchId,
            @QueryParam("fromDate") String fromDate, @QueryParam("toDate") String toDate,
            @QueryParam("status") String status) {
        LocalDate from = LocalDate.parse(fromDate);
        LocalDate to = LocalDate.parse(toDate);
        return branchPaymentService.getBranchPayments(branchId, from, to, status);
    }
}
