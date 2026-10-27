/**
 * Copyright since 2026 Mifos Initiative
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.apache.fineract.branch.connector.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.loanaccount.data.LoanAccountData;
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
    private final PlatformSecurityContext platformSecurityContext;
    /**
     * All registered {@link DefaultToApiJsonSerializer} beans; functionally equivalent
     * for {@link DefaultToApiJsonSerializer#serialize(Object)} (generic type is erased).
     * Used to serialize {@link LoanAccountData} exactly like the standard loan endpoint.
     */
    private final List<DefaultToApiJsonSerializer<LoanAccountData>> loanSerializers;

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

    @GET
    @Path("/por-referencia-reembolso/{referenciaRembolso}")
    @Operation(summary = "Consultar crédito completo por referencia de reembolso",
            description = "Resuelve el loanId desde DATOS_REEMBOLSOS.referenciaRembolso y devuelve exactamente el mismo payload que GET /v1/loans/{loanId} del core de Fineract.")
    @ApiResponse(responseCode = "200", description = "Datos del crédito, idénticos al endpoint estándar de préstamos")
    @ApiResponse(responseCode = "400", description = "referenciaRembolso vacía o inválida")
    @ApiResponse(responseCode = "404", description = "Referencia de reembolso no encontrada o préstamo vinculado inexistente")
    @ApiResponse(responseCode = "409", description = "La referencia está vinculada a varios préstamos (ambigua)")
    @ApiResponse(responseCode = "403", description = "Sin permiso de lectura sobre préstamos")
    public String getLoanByReferenciaRembolso(
            @Parameter(description = "Valor de referenciaRembolso almacenado en DATOS_REEMBOLSOS", required = true)
            @PathParam("referenciaRembolso") String referenciaRembolso) {

        platformSecurityContext.authenticatedUser().validateHasReadPermission("LOAN");

        LoanAccountData loan = clientLoanQueryService.retrieveLoanByReferenciaRembolso(referenciaRembolso);

        DefaultToApiJsonSerializer<LoanAccountData> serializer = loanSerializers.stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No hay un DefaultToApiJsonSerializer de LoanAccountData registrado en el contexto"));

        return serializer.serialize(loan);
    }
}
