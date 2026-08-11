/**
 * Copyright 2026 Mifos Initiative
 *
 * Resolves a refund reference to a unique active loan.
 * Lookup order:
 *   1. Loan externalId (via LoanReadPlatformService)
 *   2. Loan account number (via ClientLoanQueryService)
 *   3. Numeric loan id
 *   4. referenciaRembolso from DATOS_REEMBOLSOS (via ClientLoanQueryService / repository)
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
import org.apache.fineract.infrastructure.core.domain.ExternalId;
import org.apache.fineract.infrastructure.core.service.ExternalIdFactory;
import org.apache.fineract.portfolio.loanaccount.service.LoanReadPlatformService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundReferenceService {

    private final LoanReadPlatformService loanReadPlatformService;
    private final ClientLoanQueryService clientLoanQueryService;

    public RefundReferenceResolutionData resolve(String reference) {
        validateReferenceFormat(reference);

        Long loanId = resolveLoanIdFromReference(reference);

        LoanBalanceData loan = clientLoanQueryService.getLoanBalanceById(loanId);
        ClientFileData client = clientLoanQueryService.getClientByLoanId(loanId);

        boolean payable = isPayable(loan);
        String blockingReason = payable ? null : buildBlockingReason(loan);

        return RefundReferenceResolutionData.builder()
                .reference(reference)
                .status(payable ? "ACTIVE" : mapLoanStatusToRefStatus(loan != null ? loan.getStatus() : null))
                .client(client)
                .loan(loan)
                .payable(payable)
                .blockingReason(blockingReason)
                .resolvedAt(OffsetDateTime.now())
                .build();
    }

    /**
     * Resolves reference using the same multi-strategy order as ClientLoanQueryService,
     * keeping LoanReadPlatformService first so existing unit tests that mock
     * getResolvedLoanId continue to work.
     */
    private Long resolveLoanIdFromReference(String reference) {
        // 1. externalId (primary strategy used by existing tests)
        try {
            ExternalId ext = ExternalIdFactory.produce(reference);
            Long id = loanReadPlatformService.getResolvedLoanId(ext);
            if (id != null) {
                return id;
            }
        } catch (Exception ignored) {
            // fall through to broader strategies
        }

        // 2-4. accountNo / numeric id / referenciaRembolso (via ClientLoanQueryService)
        try {
            LoanBalanceData balance = clientLoanQueryService.getLoanBalance(reference);
            if (balance != null && balance.getLoanId() != null) {
                return balance.getLoanId();
            }
        } catch (BranchApiException ex) {
            // already a domain exception – rethrow as REFERENCE_NOT_FOUND for API contract
            throw BranchApiException.notFound("REFERENCE_NOT_FOUND",
                    "Referencia de reembolso no encontrada: " + reference);
        } catch (Exception ignored) {
            // fall through
        }

        throw BranchApiException.notFound("REFERENCE_NOT_FOUND",
                "Referencia de reembolso no encontrada: " + reference);
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
        if (resolution.getLoan() != null
                && resolution.getLoan().getTotalOutstanding() != null
                && resolution.getLoan().getTotalOutstanding().getAmount() != null
                && request != null
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
        if (loan == null || loan.getStatus() == null) {
            return false;
        }
        String s = loan.getStatus().toUpperCase();
        return "ACTIVE".equals(s) || "300".equals(s);
    }

    private String buildBlockingReason(LoanBalanceData loan) {
        if (loan == null) {
            return "Crédito no encontrado";
        }
        if (Boolean.FALSE.equals(loan.getDisbursed())) {
            return "El crédito aún no ha sido desembolsado";
        }
        return "Crédito no elegible para cobro (estado: " + loan.getStatus() + ")";
    }

    private String mapLoanStatusToRefStatus(String loanStatus) {
        if (loanStatus == null) {
            return "INACTIVE";
        }
        return switch (loanStatus.toUpperCase()) {
            case "ACTIVE", "300" -> "ACTIVE";
            case "CLOSED", "600", "601", "CLOSED_OBLIGATIONS_MET", "CLOSED_WRITTEN_OFF" -> "USED";
            default -> "INACTIVE";
        };
    }
}
