/**
 * Copyright 2026   Mifos Initiative
 *
 * Resolves a refund reference (texto) to a unique active loan.
 * The exact storage of the reference in Mifos (Data Table / custom field /
 * externalId) must be confirmed; this implementation uses a configurable
 * strategy that can be adapted once the field location is known.
 */
package org.apache.fineract.branch.connector.service;

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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefundReferenceService {

    private final JdbcTemplate jdbcTemplate;
    private final ClientLoanQueryService clientLoanQueryService;

    /**
     * Resolve refund reference → client + loan.
     * Strategy: look for the reference in m_loan.external_id first, then in a
     * configurable data-table column (placeholder). Adjust SQL once the exact
     * field is confirmed by the Mifos team.
     */
    public RefundReferenceResolutionData resolve(String reference) {
        validateReferenceFormat(reference);

        // Strategy 1: loan.external_id
        List<Long> loanIds = jdbcTemplate.query(
                "SELECT id FROM m_loan WHERE external_id = ? AND loan_status_id IN (300, 303, 304)", // Active / Transfer
                (rs, rowNum) -> rs.getLong("id"), reference);

        if (loanIds.isEmpty()) {
            // Strategy 2: custom attribute / data table – adjust table/column names
            // when confirmed. Example for a data-table "loan_extra":
            // SELECT loan_id FROM extra_loan WHERE refund_reference = ?
            loanIds = jdbcTemplate.query(
                    "SELECT l.id FROM m_loan l "
                            + "JOIN m_loan_transaction_processing_strategy s ON s.id = l.loan_transaction_strategy_id "
                            + "WHERE CAST(l.id AS CHAR) = ? LIMIT 0", // placeholder – never matches until configured
                    (rs, rowNum) -> rs.getLong("id"), reference);
        }

        if (loanIds.isEmpty()) {
            throw BranchApiException.notFound("REFERENCE_NOT_FOUND",
                    "Referencia de reembolso no encontrada: " + reference);
        }
        if (loanIds.size() > 1) {
            throw BranchApiException.conflict("REFERENCE_AMBIGUOUS",
                    "La referencia resuelve a más de un crédito activo");
        }

        Long loanId = loanIds.get(0);
        LoanBalanceData loan = clientLoanQueryService.getLoanBalanceById(loanId);
        ClientFileData client = clientLoanQueryService.getClientByLoanId(loanId);

        boolean payable = isPayable(loan);
        String blockingReason = payable ? null : buildBlockingReason(loan);

        return RefundReferenceResolutionData.builder().reference(reference)
                .status(payable ? "ACTIVE" : mapLoanStatusToRefStatus(loan.getStatus())).client(client).loan(loan)
                .payable(payable).blockingReason(blockingReason).resolvedAt(OffsetDateTime.now()).build();
    }

    public PaymentValidationResultData validatePayment(String reference, PaymentValidationRequestData request) {
        RefundReferenceResolutionData resolution = resolve(reference);
        if (Boolean.FALSE.equals(resolution.getPayable())) {
            return PaymentValidationResultData.builder().valid(false).referenceStatus(resolution.getStatus())
                    .blockingErrors(List.of()).projectedOutstanding(null).build();
        }
        // Additional amount / date checks can be added here
        return PaymentValidationResultData.builder().valid(true).referenceStatus(resolution.getStatus())
                .projectedOutstanding(resolution.getLoan().getTotalOutstanding() != null
                        ? resolution.getLoan().getTotalOutstanding().getAmount().subtract(request.getAmount())
                        : null)
                .build();
    }

    private void validateReferenceFormat(String reference) {
        if (!StringUtils.hasText(reference) || reference.length() > 128) {
            throw BranchApiException.badRequest("INVALID_REFERENCE",
                    "La referencia de reembolso es inválida o excede longitud permitida");
        }
        // Keep as pure text – never parse as number
    }

    private boolean isPayable(LoanBalanceData loan) {
        if (loan == null) {
            return false;
        }
        String status = loan.getStatus();
        if (status == null) {
            return false;
        }
        return "ACTIVE".equalsIgnoreCase(status) || "300".equals(status);
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
            case "CLOSED", "600", "601" -> "USED";
            default -> "INACTIVE";
        };
    }
}
