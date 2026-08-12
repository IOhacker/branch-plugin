/**
 * Copyright since 2026 Mifos Initiative
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
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

@Slf4j
@Service
@RequiredArgsConstructor
public class FineractRepaymentGateway {

    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
    private final FromJsonHelper fromApiJsonHelper;
    private final Gson gson = new Gson();

    /**
     * Execute repayment against the given loan using the official command bus.
     * <p>
     * Note: No {@code @Transactional} annotation is used here because the command bus
     * already manages its own transactions. Wrapping it with an additional transaction
     * can cause {@link org.springframework.transaction.TransactionSystemException} when
     * the inner command succeeds but the outer transaction is marked rollback‑only.
     *
     * @return resourceId (m_loan_transaction.id) of the created transaction
     */
    public Long executeRepayment(Long loanId, RepaymentRequestData request, String externalId) {
        log.debug("[MONITOR] START executeRepayment | loanId={} externalId={} amount={}",
                loanId, externalId, request.getTransactionAmount());
        long startTime = System.currentTimeMillis();

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
        log.debug("[MONITOR] Repayment payload prepared | loanId={} externalId={} payload={}",
                loanId, externalId, json);
        log.info("Submitting repayment via command bus loanId={} externalId={} amount={}",
                loanId, externalId, request.getTransactionAmount());

        try {
            CommandWrapper commandRequest = new CommandWrapperBuilder()
                    .withJson(json)
                    .loanRepaymentTransaction(loanId)
                    .build();

            log.debug("[MONITOR] CommandWrapper built | loanId={} externalId={} entityName={} actionName={}",
                    loanId, externalId, commandRequest.getEntityName(), commandRequest.getActionName());

            CommandProcessingResult result = commandsSourceWritePlatformService.logCommandSource(commandRequest);

            long duration = System.currentTimeMillis() - startTime;
            log.debug("[MONITOR] Command bus responded | loanId={} externalId={} durationMs={}",
                    loanId, externalId, duration);

            if (result == null || result.getResourceId() == null) {
                log.warn("[MONITOR] Empty result from command bus | loanId={} externalId={} result={}",
                        loanId, externalId, result);
                throw BranchApiException.conflict("PAYMENT_STATUS_UNKNOWN",
                        "Command bus returned empty result for externalId=" + externalId);
            }

            log.debug("[MONITOR] Repayment successful | loanId={} externalId={} resourceId={} officeId={} clientId={} durationMs={}",
                    loanId, externalId, result.getResourceId(), result.getOfficeId(), result.getClientId(), duration);
            log.info("[MONITOR] END executeRepayment | loanId={} externalId={} resourceId={} durationMs={}",
                    loanId, externalId, result.getResourceId(), duration);

            return result.getResourceId();

        } catch (BranchApiException ex) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[MONITOR] BranchApiException during repayment | loanId={} externalId={} durationMs={} errorCode={}",
                    loanId, externalId, duration, ex.getCode(), ex);
            log.error(">>> FINERACT RAW ERROR <<<", ex);
            throw ex;
        } catch (Exception ex) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[MONITOR] Unexpected exception during repayment | loanId={} externalId={} durationMs={} exceptionType={}",
                    loanId, externalId, duration, ex.getClass().getName(), ex);
            log.error("Repayment command failed loanId={} externalId={}", loanId, externalId, ex);
            throw BranchApiException.conflict("PAYMENT_STATUS_UNKNOWN",
                    "Resultado incierto; consulte por externalId antes de reintentar. " + ex.getMessage());
        }
    }

    /**
     * Reverse / adjust an existing loan transaction via the command bus.
     * <p>
     * Also no {@code @Transactional} – the command bus handles its own transaction.
     */
    public void reverseRepayment(Long loanId, Long transactionId, String reason) {
        log.debug("[MONITOR] START reverseRepayment | loanId={} transactionId={} reason={}",
                loanId, transactionId, reason);
        long startTime = System.currentTimeMillis();

        JsonObject body = new JsonObject();
        body.addProperty("transactionDate", java.time.LocalDate.now().toString());
        body.addProperty("dateFormat", "yyyy-MM-dd");
        body.addProperty("locale", "en");
        body.addProperty("note", reason != null ? reason : "Branch connector reversal");

        String json = gson.toJson(body);
        log.debug("[MONITOR] Reversal payload prepared | loanId={} transactionId={} payload={}",
                loanId, transactionId, json);
        log.info("Reversing repayment loanId={} transactionId={} reason={}", loanId, transactionId, reason);

        try {
            CommandWrapper commandRequest = new CommandWrapperBuilder()
                    .withJson(json)
                    .adjustTransaction(loanId, transactionId)
                    .build();

            log.debug("[MONITOR] CommandWrapper built for reversal | loanId={} transactionId={} entityName={} actionName={}",
                    loanId, transactionId, commandRequest.getEntityName(), commandRequest.getActionName());

            commandsSourceWritePlatformService.logCommandSource(commandRequest);

            long duration = System.currentTimeMillis() - startTime;
            log.debug("[MONITOR] Reversal successful | loanId={} transactionId={} durationMs={}",
                    loanId, transactionId, duration);
            log.info("[MONITOR] END reverseRepayment | loanId={} transactionId={} durationMs={}",
                    loanId, transactionId, duration);

        } catch (BranchApiException ex) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[MONITOR] BranchApiException during reversal | loanId={} transactionId={} durationMs={} errorCode={}",
                    loanId, transactionId, duration, ex.getCode(), ex);
            throw ex;
        } catch (Exception ex) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[MONITOR] Unexpected exception during reversal | loanId={} transactionId={} durationMs={} exceptionType={}",
                    loanId, transactionId, duration, ex.getClass().getName(), ex);
            log.error("Reversal command failed loanId={} transactionId={}", loanId, transactionId, ex);
            throw BranchApiException.conflict("REVERSAL_FAILED",
                    "No se pudo revertir la transacción: " + ex.getMessage());
        }
    }
}