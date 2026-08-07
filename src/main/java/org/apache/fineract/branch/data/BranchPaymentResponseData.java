/**
 * Copyright 2026   Mifos Initiative
 */
package org.apache.fineract.branch.data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchPaymentResponseData {
    private String externalId;
    /** RECEIVED | VALIDATING | APPLIED | REJECTED | UNKNOWN | REVERSED */
    private String status;
    private Long resourceId;
    private Long loanId;
    private Long clientId;
    private String refundReference;
    private BigDecimal amount;
    private String currency;
    private LocalDate transactionDate;
    private OffsetDateTime registeredAt;
    private String receiptNo;
    private BigDecimal balanceAfter;
    private String message;
}
