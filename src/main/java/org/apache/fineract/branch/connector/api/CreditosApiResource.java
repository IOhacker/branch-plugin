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
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.branch.connector.data.LoanBalanceData;
import org.apache.fineract.branch.connector.data.PaymentResultData;
import org.apache.fineract.branch.connector.data.RepaymentRequestData;
import org.apache.fineract.branch.connector.data.RepaymentScheduleData;
import org.apache.fineract.branch.connector.exception.BranchApiException;
import org.apache.fineract.branch.connector.service.BranchPaymentService;
import org.apache.fineract.branch.connector.service.ClientLoanQueryService;
import org.springframework.stereotype.Component;

@Path("/v1/creditos")
@Component
@Tag(name = "Créditos")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
public class CreditosApiResource {

    private final ClientLoanQueryService clientLoanQueryService;
    private final BranchPaymentService branchPaymentService;

    @GET
    @Path("/balance/{identificador}")
    @Operation(summary = "Consultar saldo de crédito")
    public LoanBalanceData getLoanBalance(@PathParam("identificador") String identificador,
            @QueryParam("associations") String associations, @QueryParam("exclude") String exclude) {
        return clientLoanQueryService.getLoanBalance(identificador);
    }

    @GET
    @Path("/calendario/{identificador}")
    @Operation(summary = "Consultar calendario de pagos")
    public RepaymentScheduleData getRepaymentSchedule(@PathParam("identificador") String identificador,
            @QueryParam("associations") String associations, @QueryParam("exclude") String exclude) {
        return clientLoanQueryService.getRepaymentSchedule(identificador);
    }
    
    @GET
    @Path("/calendario/cliente/{identificador}")
    @Operation(summary = "Consultar calendario de pagos")
    public List<RepaymentScheduleData> getRepaymentScheduleByClientId(@PathParam("identificador") String identificador,
            @QueryParam("associations") String associations, @QueryParam("exclude") String exclude) {
        return clientLoanQueryService.getRepaymentScheduleByClientId(identificador);
    }    

    @POST
    @Path("/pagos/{referencia}/transactions")
    @Operation(summary = "Registrar repayment mediante referencia de reembolso")
    public PaymentResultData createRepayment(@PathParam("referencia") String referencia,
            @QueryParam("command") String command, @HeaderParam("Idempotency-Key") String idempotencyKey,
            @Valid RepaymentRequestData body) {
        if (!"repayment".equalsIgnoreCase(command)) {
            throw BranchApiException.badRequest("INVALID_REQUEST", "command debe ser 'repayment'");
        }
        return branchPaymentService.createRepaymentByReference(referencia, body, idempotencyKey);
    }
}
