/**
 * Copyright 2026   Mifos Initiative
 *
 * Thin adapter that posts a repayment command into Fineract core.
 * Uses the portfolio command source so all business rules, accounting
 * and schedule updates remain inside Fineract.
 *
 * NOTE: At runtime this class should inject the real PortfolioCommandSourceWritePlatformService
 * / LoanWritePlatformService. The implementation below uses a JDBC fallback for environments
 * where the full Fineract command bus is not yet wired, so the plugin compiles and can be
 * completed once the exact Fineract version/service signatures are confirmed.
 */
package org.apache.fineract.branch.connector.service;

import com.google.gson.Gson;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.branch.connector.data.RepaymentRequestData;
import org.apache.fineract.branch.connector.exception.BranchApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FineractRepaymentGateway {

    private final JdbcTemplate jdbcTemplate;
    private final Gson gson = new Gson();

    /**
     * Execute repayment against the given loan.
     * Preferred path: call Fineract LoanWritePlatformService.makeLoanRepayment(...)
     * via the command bus. Fallback documented below for scaffolding.
     *
     * @return resourceId (m_loan_transaction.id) of the created transaction
     */
    public Long executeRepayment(Long loanId, RepaymentRequestData request, String externalId) {
        // Build the same JSON body Fineract expects for command=repayment
        Map<String, Object> body = new HashMap<>();
        body.put("transactionDate", request.getTransactionDate());
        body.put("dateFormat", request.getDateFormat());
        body.put("locale", request.getLocale());
        body.put("paymentTypeId", request.getPaymentTypeId());
        body.put("transactionAmount", request.getTransactionAmount());
        body.put("externalId", externalId);
        if (request.getNote() != null) {
            body.put("note", request.getNote());
        }

        log.info("Submitting repayment loanId={} externalId={} amount={}", loanId, externalId,
                request.getTransactionAmount());

        /*
         * PRODUCTION wiring (uncomment when Fineract services are on the classpath):
         *
         * JsonCommand command = JsonCommand.from(gson.toJson(body), ...);
         * CommandProcessingResult result = loanWritePlatformService.makeLoanRepayment(loanId, command, false);
         * return result.getResourceId();
         *
         * Until then we record a placeholder transaction id so the rest of the
         * orchestration and idempotency layer can be tested end-to-end.
         */
        try {
            // Detect if a real transaction already exists for this externalId (idempotency at core level)
            Long existing = jdbcTemplate.query(
                    "SELECT id FROM m_loan_transaction WHERE loan_id = ? AND external_id = ? AND is_reversed = 0 LIMIT 1",
                    rs -> rs.next() ? rs.getLong("id") : null, loanId, externalId);
            if (existing != null) {
                return existing;
            }
        } catch (Exception ignored) {
            // table may not have external_id in older schemas
        }

        // Scaffold: return a synthetic id. Replace with real command-bus call.
        log.warn("FineractRepaymentGateway running in scaffold mode – replace with LoanWritePlatformService call");
        return System.currentTimeMillis() % 1_000_000L;
    }

    public void reverseRepayment(Long loanId, Long transactionId, String reason) {
        log.info("Reversing repayment loanId={} transactionId={} reason={}", loanId, transactionId, reason);
        /*
         * PRODUCTION:
         * loanWritePlatformService.adjustLoanTransaction(loanId, transactionId, command);
         * or undoWriteOff / reverse depending on Fineract version.
         */
        log.warn("FineractRepaymentGateway.reverseRepayment scaffold – wire to LoanWritePlatformService");
    }
}
