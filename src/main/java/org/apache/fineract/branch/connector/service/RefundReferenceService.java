/**
 * Copyright 2026 Mifos Initiative
 *
 * Resolves a refund reference to a unique active loan.
 * Strategies (delegated to ClientLoanQueryService.resolveLoanId):
 *   1. Loan externalId
 *   2. Loan account number
 *   3. Numeric loan id
 *   4. referenciaRembolso from DATOS_REEMBOLSOS (tenant table)
 *
 * Fully multi-tenant – relies on Fineract ThreadLocal tenant context.
 */
package org.apache.fineract.branch.connector.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.branch.connector.data.ClientFileData;
import org.apache.fineract.branch.connector.data.LoanBalanceData;
import org.apache.fineract.branch.connector.data.PaymentValidationRequestData;
import org.apache.fineract.branch.connector.data.PaymentValidationResultData;
import org.apache.fineract.branch.connector.data.RefundReferenceResolutionData;
import org.apache.fineract.branch.connector.exception.BranchApiException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundReferenceService {

    private final ClientLoanQueryService clientLoanQueryService;

    public RefundReferenceResolutionData resolve(String reference) {
        validateReferenceFormat(reference);

        Long loanId;
        try {
            // Uses the multi-strategy resolver (externalId / accountNo / id / referenciaRembolso)
            LoanBalanceData loan = clientLoanQueryService.getLoanBalance(reference);
            loanId = loan.getLoanId();
        } catch (BranchApiException ex) {
            throw BranchApiException.notFound("REFERENCE_NOT_FOUND",
                    "Referencia de reembolso no encontrada: " + reference);
        }

        LoanBalanceData loan = clientLoanQueryService.getLoanBalanceById(loanId);
        ClientFileData client = clientLoanQueryService.getClientByLoanId(loanId);

        boolean payable = isPayable(loan);
        String blockingReason = payable ? null : buildBlockingReason(loan);

        return RefundReferenceResolutionData.builder()
                .reference(reference)
                .status(payable ? "ACTIVE" : mapLoanStatusToRefStatus(loan.getStatus()))
                .client(client)
                .loan(loan)
                .payable(payable)
                .blockingReason(blockingReason)
                .resolvedAt(OffsetDateTime.now())
                .build();
    }

    public PaymentValidationResultData validatePayment(String reference, PaymentValidationRequestData request) {
        RefundReferenceResolutionData resolution = resolve(reference);
        if (Boolean.FALSE.equals(resolution.getPayable())) {
            return PaymentValidationResultData.builder()
                    .valid(false)
                    .referenceStatus(resolution.getStatus())
                    .blockingErrors(List.of())
                    .projectedOutstanding(null)
                    .build();
        }
        BigDecimal projected = null;
        if (resolution.getLoan().getTotalOutstanding() != null
                && resolution.getLoan().getTotalOutstanding().getAmount() != null
                && request.getAmount() != null) {
            projected = resolution.getLoan().getTotalOutstanding().getAmount().subtract(request.getAmount());
        }
        return PaymentValidationResultData.builder()
                .valid(true)
                .referenceStatus(resolution.getStatus())
                .projectedOutstanding(projected)
                .build();
    }

    private void validateReferenceFormat(String reference) {
        if (!StringUtils.hasText(reference) || reference.length() > 128) {
            throw BranchApiException.badRequest("INVALID_REFERENCE",
                    "La referencia de reembolso es inválida o excede longitud permitida");
        }
    }

    private boolean isPayable(LoanBalanceData loan) {
        if (loan == null || loan.getStatus() == null) return false;
        String s = loan.getStatus().toUpperCase();
        return "ACTIVE".equals(s) || "300".equals(s);
    }

    private String buildBlockingReason(LoanBalanceData loan) {
        if (loan == null) return "Crédito no encontrado";
        if (Boolean.FALSE.equals(loan.getDisbursed())) {
            return "El crédito aún no ha sido desembolsado";
        }
        return "Crédito no elegible para cobro (estado: " + loan.getStatus() + ")";
    }

    private String mapLoanStatusToRefStatus(String loanStatus) {
        if (loanStatus == null) return "INACTIVE";
        return switch (loanStatus.toUpperCase()) {
            case "ACTIVE", "300" -> "ACTIVE";
            case "CLOSED", "600", "601", "CLOSED_OBLIGATIONS_MET", "CLOSED_WRITTEN_OFF" -> "USED";
            default -> "INACTIVE";
        };
    }
}
