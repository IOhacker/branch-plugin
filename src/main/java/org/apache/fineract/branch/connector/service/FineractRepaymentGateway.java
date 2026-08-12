/**
 * Copyright 2026 Mifos Initiative
 *
 * Thin adapter that posts a repayment (or reversal) into Fineract core
 * via the official command bus. All business rules, accounting and
 * schedule updates remain inside Fineract.
 */
package org.apache.fineract.branch.connector.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.branch.connector.data.RepaymentRequestData;
import org.apache.fineract.branch.connector.exception.BranchApiException;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FineractRepaymentGateway {

    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
    private final FromJsonHelper fromApiJsonHelper;
    private final Gson gson = new Gson();

    /**
     * Execute repayment against the given loan using the official command bus.
     *
     * @return resourceId (m_loan_transaction.id) of the created transaction
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long executeRepayment(Long loanId, RepaymentRequestData request, String externalId) {
        Map<String, Object> body = new HashMap<>();
        body.put("transactionDate", request.getTransactionDate());
        body.put("dateFormat", request.getDateFormat() != null ? request.getDateFormat() : "yyyy-MM-dd");
        body.put("locale", request.getLocale() != null ? request.getLocale() : "en");
        body.put("paymentTypeId", request.getPaymentTypeId());
        body.put("transactionAmount", request.getTransactionAmount());
        body.put("externalId", externalId);
        if (request.getNote() != null) {
            body.put("note", request.getNote());
        }

        String json = gson.toJson(body);
        log.info("Submitting repayment via command bus loanId={} externalId={} amount={}",
                loanId, externalId, request.getTransactionAmount());

        try {
            CommandWrapper commandRequest = new CommandWrapperBuilder()
                    .withJson(json)
                    .loanRepaymentTransaction(loanId)
                    .build();

            CommandProcessingResult result = commandsSourceWritePlatformService.logCommandSource(commandRequest);
            if (result == null || result.getResourceId() == null) {
                throw BranchApiException.conflict("PAYMENT_STATUS_UNKNOWN",
                        "Command bus returned empty result for externalId=" + externalId);
            }
            return result.getResourceId();
        } catch (BranchApiException ex) {
            log.error(">>> FINERACT RAW ERROR <<<", ex);
            throw ex;
        } catch (Exception ex) {
            log.error("Repayment command failed loanId={} externalId={}", loanId, externalId, ex);
            throw BranchApiException.conflict("PAYMENT_STATUS_UNKNOWN",
                    "Resultado incierto; consulte por externalId antes de reintentar. " + ex.getMessage());
        }
    }

    /**
     * Reverse / adjust an existing loan transaction via the command bus.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reverseRepayment(Long loanId, Long transactionId, String reason) {
        log.info("Reversing repayment loanId={} transactionId={} reason={}", loanId, transactionId, reason);

        JsonObject body = new JsonObject();
        body.addProperty("transactionDate", java.time.LocalDate.now().toString());
        body.addProperty("dateFormat", "yyyy-MM-dd");
        body.addProperty("locale", "en");
        body.addProperty("note", reason != null ? reason : "Branch connector reversal");

        String json = gson.toJson(body);

        try {
            CommandWrapper commandRequest = new CommandWrapperBuilder()
                    .withJson(json)
                    .adjustTransaction(loanId, transactionId)
                    .build();

            commandsSourceWritePlatformService.logCommandSource(commandRequest);
        } catch (Exception ex) {
            log.error("Reversal command failed loanId={} transactionId={}", loanId, transactionId, ex);
            throw BranchApiException.conflict("REVERSAL_FAILED",
                    "No se pudo revertir la transacción: " + ex.getMessage());
        }
    }
}