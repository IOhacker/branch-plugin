/**
 * Copyright since 2026 Mifos Initiative
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.apache.fineract.branch.connector.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.branch.connector.data.BranchPaymentRequestData;
import org.apache.fineract.branch.connector.data.BranchPaymentResponseData;
import org.apache.fineract.branch.connector.data.LoanBalanceData;
import org.apache.fineract.branch.connector.data.PaymentResultData;
import org.apache.fineract.branch.connector.data.ReconciliationItemData;
import org.apache.fineract.branch.connector.data.RefundReferenceResolutionData;
import org.apache.fineract.branch.connector.data.RepaymentRequestData;
import org.apache.fineract.branch.connector.data.ReversalRequestData;
import org.apache.fineract.branch.connector.domain.BranchPaymentEntity;
import org.apache.fineract.branch.connector.domain.BranchPaymentRepository;
import org.apache.fineract.branch.connector.exception.BranchApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class BranchPaymentService {

    private static final String CURRENCY_MXN = "MXN";
    private static final int MAX_SCALE = 2;

    private final BranchPaymentRepository paymentRepository;
    private final RefundReferenceService refundReferenceService;
    private final ClientLoanQueryService clientLoanQueryService;
    private final FineractRepaymentGateway repaymentGateway;

    /**
     * Current (legacy) endpoint: POST /creditos/pagos/{referencia}/transactions?command=repayment
     */    
    public PaymentResultData createRepaymentByReference(String reference, RepaymentRequestData request,
            String idempotencyKey) {
        String externalId = StringUtils.hasText(idempotencyKey) ? idempotencyKey : request.getExternalId();
        if (!StringUtils.hasText(externalId)) {
            throw BranchApiException.badRequest("INVALID_REQUEST", "externalId / Idempotency-Key es obligatorio");
        }

        // Idempotency short-circuit (read-only, safe)
        Optional<BranchPaymentEntity> existing = paymentRepository.findByExternalId(externalId);
        if (existing.isPresent()) {
            BranchPaymentEntity e = existing.get();
            if (e.getAmount().compareTo(request.getTransactionAmount()) != 0) {
                throw BranchApiException.conflict("PAYMENT_DUPLICATE_CONFLICT",
                        "externalId ya usado con monto diferente");
            }
            return PaymentResultData.builder()
                    .loanId(e.getLoanId())
                    .clientId(e.getClientId())
                    .resourceId(e.getResourceId())
                    .resourceExternalId(e.getExternalId())
                    .build();
        }

        validateAmount(request.getTransactionAmount());
        LocalDate txnDate = parseDate(request.getTransactionDate(), request.getDateFormat());

        RefundReferenceResolutionData resolution = refundReferenceService.resolve(reference);
        if (Boolean.FALSE.equals(resolution.getPayable())) {
            throw BranchApiException.unprocessable(
                    resolution.getBlockingReason() != null && resolution.getBlockingReason().contains("desembolso")
                            ? "LOAN_NOT_DISBURSED"
                            : "LOAN_NOT_ACTIVE",
                    resolution.getBlockingReason() != null ? resolution.getBlockingReason()
                            : "Crédito no elegible para cobro");
        }

        LoanBalanceData loan = resolution.getLoan();
        enforceNoOverpayment(loan, request.getTransactionAmount());

        String traceId = UUID.randomUUID().toString();
        BranchPaymentEntity pending = BranchPaymentEntity.builder()
                .externalId(externalId)
                .loanId(loan.getLoanId())
                .clientId(resolution.getClient().getClientId())
                .refundReference(reference)
                .depositReference(request.getDepositReference())
                .amount(request.getTransactionAmount())
                .currency(CURRENCY_MXN)
                .transactionDate(txnDate)
                .branchId("LEGACY")
                .operatorId("LEGACY")
                .terminalId("LEGACY")
                .status("PENDING")
                .note(request.getNote())
                .registeredAt(OffsetDateTime.now())
                .traceId(traceId)
                .build();
        // short TX #1 – commits immediately
        paymentRepository.save(pending);

        try {
            Long resourceId = repaymentGateway.executeRepayment(loan.getLoanId(), request, externalId);
            pending.setResourceId(resourceId);
            pending.setStatus("APPLIED");
            pending.setUpdatedAt(OffsetDateTime.now());
            // short TX #2
            paymentRepository.save(pending);

            return PaymentResultData.builder()
                    .officeId(resolution.getClient().getOfficeId())
                    .clientId(resolution.getClient().getClientId())
                    .loanId(loan.getLoanId())
                    .resourceId(resourceId)
                    .resourceExternalId(externalId)
                    .build();
        } catch (Exception ex) {
            log.error("Repayment failed for externalId={} traceId={}", externalId, traceId, ex);
            pending.setStatus("UNKNOWN");
            pending.setMessage(ex.getMessage() != null && ex.getMessage().length() > 500
                    ? ex.getMessage().substring(0, 500) : ex.getMessage());
            pending.setUpdatedAt(OffsetDateTime.now());
            try {
                paymentRepository.save(pending); // best-effort audit update
            } catch (Exception auditEx) {
                log.error("Failed to persist UNKNOWN status for externalId={}", externalId, auditEx);
            }
            throw BranchApiException.conflict("PAYMENT_STATUS_UNKNOWN",
                    "Resultado incierto; consulte por externalId antes de reintentar. traceId=" + traceId);
        }
    }

    /**
     * Proposed atomic branch payment: POST /sucursales/pagos
     */
    public BranchPaymentResponseData createBranchPayment(BranchPaymentRequestData request, String idempotencyKey) {
        String externalId = StringUtils.hasText(idempotencyKey) ? idempotencyKey : request.getExternalId();
        if (!StringUtils.hasText(externalId)) {
            throw BranchApiException.badRequest("INVALID_REQUEST", "externalId / Idempotency-Key es obligatorio");
        }
        if (!CURRENCY_MXN.equalsIgnoreCase(request.getCurrency())) {
            throw BranchApiException.badRequest("INVALID_REQUEST", "Solo se acepta moneda MXN");
        }

        Optional<BranchPaymentEntity> existing = paymentRepository.findByExternalId(externalId);
        if (existing.isPresent()) {
            return toResponse(existing.get());
        }

        validateAmount(request.getAmount());
        LocalDate txnDate = parseDate(request.getTransactionDate(), "yyyy-MM-dd");

        RefundReferenceResolutionData resolution = refundReferenceService.resolve(request.getRefundReference());
        if (Boolean.FALSE.equals(resolution.getPayable())) {
            throw BranchApiException.unprocessable("LOAN_NOT_ACTIVE",
                    resolution.getBlockingReason() != null ? resolution.getBlockingReason()
                            : "Crédito no elegible");
        }

        LoanBalanceData loan = resolution.getLoan();
        enforceNoOverpayment(loan, request.getAmount());

        String traceId = UUID.randomUUID().toString();
        String receiptNo = request.getBranch().getBranchId() + "-" + txnDate + "-"
                + externalId.substring(Math.max(0, externalId.length() - 6));

        BranchPaymentEntity entity = BranchPaymentEntity.builder()
                .externalId(externalId)
                .loanId(loan.getLoanId())
                .clientId(resolution.getClient().getClientId())
                .refundReference(request.getRefundReference())
                .depositReference(request.getDepositReference())
                .amount(request.getAmount())
                .currency(CURRENCY_MXN)
                .transactionDate(txnDate)
                .branchId(request.getBranch().getBranchId())
                .operatorId(request.getBranch().getOperatorId())
                .terminalId(request.getBranch().getTerminalId())
                .status("PENDING")
                .receiptNo(receiptNo)
                .note(request.getNote())
                .registeredAt(OffsetDateTime.now())
                .traceId(traceId)
                .build();
        paymentRepository.save(entity);

        RepaymentRequestData coreRequest = RepaymentRequestData.builder()
                .transactionDate(request.getTransactionDate())
                .dateFormat("yyyy-MM-dd")
                .locale("es")
                .paymentTypeId(request.getPaymentTypeId())
                .transactionAmount(request.getAmount())
                .externalId(externalId)
                .note(buildAuditNote(request))
                .depositReference(request.getDepositReference())
                .build();

        try {
            Long resourceId = repaymentGateway.executeRepayment(loan.getLoanId(), coreRequest, externalId);
            entity.setResourceId(resourceId);
            entity.setStatus("APPLIED");
            entity.setUpdatedAt(OffsetDateTime.now());

            // Refresh balance after payment using official read services
            LoanBalanceData after = clientLoanQueryService.getLoanBalanceById(loan.getLoanId());
            if (after.getTotalOutstanding() != null) {
                entity.setBalanceAfter(after.getTotalOutstanding().getAmount());
            }
            paymentRepository.save(entity);
            return toResponse(entity);
        } catch (Exception ex) {
            log.error("Branch payment UNKNOWN externalId={} traceId={}", externalId, traceId, ex);
            entity.setStatus("UNKNOWN");
            entity.setMessage(ex.getMessage());
            entity.setUpdatedAt(OffsetDateTime.now());
            paymentRepository.save(entity);
            throw BranchApiException.conflict("PAYMENT_STATUS_UNKNOWN",
                    "Resultado incierto. Consulte GET /sucursales/pagos/" + externalId + " antes de reintentar.");
        }
    }

    @Transactional(readOnly = true)
    public BranchPaymentResponseData getByExternalId(String externalId) {
        return paymentRepository.findByExternalId(externalId)
                .map(this::toResponse)
                .orElseThrow(() -> BranchApiException.notFound("PAYMENT_NOT_FOUND",
                        "Pago no encontrado: " + externalId));
    }

    @Transactional
    public BranchPaymentResponseData reverse(String externalId, ReversalRequestData request) {
        BranchPaymentEntity entity = paymentRepository.findByExternalId(externalId)
                .orElseThrow(() -> BranchApiException.notFound("PAYMENT_NOT_FOUND", "Pago no encontrado"));

        if ("REVERSED".equals(entity.getStatus())) {
            throw BranchApiException.conflict("PAYMENT_ALREADY_REVERSED", "El pago ya fue revertido");
        }
        if (!"APPLIED".equals(entity.getStatus()) || entity.getResourceId() == null) {
            throw BranchApiException.conflict("PAYMENT_NOT_REVERSIBLE",
                    "Solo se pueden revertir pagos en estado APPLIED");
        }

        repaymentGateway.reverseRepayment(entity.getLoanId(), entity.getResourceId(), request.getReason());
        entity.setStatus("REVERSED");
        entity.setMessage(request.getReason());
        entity.setUpdatedAt(OffsetDateTime.now());
        paymentRepository.save(entity);
        return toResponse(entity);
    }

    @Transactional(readOnly = true)
    public List<ReconciliationItemData> getBranchPayments(String branchId, LocalDate from, LocalDate to,
            String status) {
        List<BranchPaymentEntity> rows = paymentRepository.findForReconciliation(branchId, from, to, status);
        return rows.stream()
                .map(e -> ReconciliationItemData.builder()
                        .externalId(e.getExternalId())
                        .resourceId(e.getResourceId())
                        .branchId(e.getBranchId())
                        .operatorId(e.getOperatorId())
                        .terminalId(e.getTerminalId())
                        .refundReference(e.getRefundReference())
                        .depositReference(e.getDepositReference())
                        .amount(e.getAmount())
                        .transactionDate(e.getTransactionDate())
                        .registeredAt(e.getRegisteredAt())
                        .status(e.getStatus())
                        .reconciliationStatus(mapReconStatus(e))
                        .build())
                .toList();
    }

    // ---------- helpers ----------

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw BranchApiException.badRequest("PAYMENT_AMOUNT_INVALID", "El monto debe ser mayor a cero");
        }
        if (amount.scale() > MAX_SCALE) {
            throw BranchApiException.badRequest("PAYMENT_SCALE_INVALID",
                    "El monto no debe tener más de dos decimales");
        }
    }

    private void enforceNoOverpayment(LoanBalanceData loan, BigDecimal amount) {
        if (loan.getTotalOutstanding() != null
                && loan.getTotalOutstanding().getAmount() != null
                && amount.compareTo(loan.getTotalOutstanding().getAmount()) > 0) {
            // Default policy: reject. Change when product policy is confirmed.
            throw BranchApiException.unprocessable("OVERPAYMENT_NOT_ALLOWED",
                    "El monto excede el saldo pendiente del crédito");
        }
    }

    private LocalDate parseDate(String date, String format) {
        if (!StringUtils.hasText(date)) {
            throw BranchApiException.unprocessable("PAYMENT_DATE_INVALID", "Fecha de transacción inválida: " + date);
        }
        try {
            String fmt = StringUtils.hasText(format) ? format.trim() : "yyyy-MM-dd";
            // Fineract repayment stores LocalDate only. Accept pure dates and
            // datetime strings (e.g. "yyyy-MM-dd HH:mm:ss") by taking the date part.
            String value = date.trim();
            if (value.length() >= 10 && (fmt.contains("H") || fmt.contains("m") || fmt.contains("s")
                    || value.length() > 10 || value.contains("T") || value.contains(" "))) {
                // Keep only yyyy-MM-dd portion
                value = value.substring(0, 10);
                fmt = "yyyy-MM-dd";
            }
            return LocalDate.parse(value, DateTimeFormatter.ofPattern(fmt));
        } catch (DateTimeParseException | IllegalArgumentException ex) {
            throw BranchApiException.unprocessable("PAYMENT_DATE_INVALID", "Fecha de transacción inválida: " + date);
        }
    }

    private String buildAuditNote(BranchPaymentRequestData request) {
        return String.format("Cobro sucursal %s | operador %s | terminal %s | %s",
                request.getBranch().getBranchId(),
                request.getBranch().getOperatorId(),
                request.getBranch().getTerminalId(),
                request.getNote() != null ? request.getNote() : "");
    }

    private BranchPaymentResponseData toResponse(BranchPaymentEntity e) {
        return BranchPaymentResponseData.builder()
                .externalId(e.getExternalId())
                .status(e.getStatus())
                .resourceId(e.getResourceId())
                .loanId(e.getLoanId())
                .clientId(e.getClientId())
                .refundReference(e.getRefundReference())
                .amount(e.getAmount())
                .currency(e.getCurrency())
                .transactionDate(e.getTransactionDate())
                .registeredAt(e.getRegisteredAt())
                .receiptNo(e.getReceiptNo())
                .balanceAfter(e.getBalanceAfter())
                .message(e.getMessage())
                .build();
    }

    private String mapReconStatus(BranchPaymentEntity e) {
        return switch (e.getStatus()) {
            case "APPLIED" -> "MATCHED";
            case "UNKNOWN" -> "PENDING";
            case "REVERSED" -> "MATCHED";
            case "REJECTED" -> "MATCHED";
            default -> "PENDING";
        };
    }
}