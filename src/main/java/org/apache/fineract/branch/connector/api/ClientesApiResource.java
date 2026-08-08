/**
 * Copyright 2026   Mifos Initiative
 */
package org.apache.fineract.branch.connector.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.branch.connector.data.ClientFileData;
import org.apache.fineract.branch.connector.data.LoanSummaryData;
import org.apache.fineract.branch.connector.service.ClientLoanQueryService;
import org.springframework.stereotype.Component;

@Path("/v1/clientes")
@Component
@Tag(name = "Clientes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor
public class ClientesApiResource {

    private final ClientLoanQueryService clientLoanQueryService;

    @GET
    @Path("/expediente/{identificador}")
    @Operation(summary = "Consultar expediente del cliente")
    public ClientFileData getClientFile(@PathParam("identificador") String identificador) {
        return clientLoanQueryService.getClientFile(identificador);
    }

    @GET
    @Path("/cuentas/{identificador}/accounts")
    @Operation(summary = "Consultar cuentas y créditos del cliente")
    public List<LoanSummaryData> getClientAccounts(@PathParam("identificador") String identificador) {
        return clientLoanQueryService.getClientAccounts(identificador);
    }
}
