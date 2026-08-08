/**
 * Copyright 2026   Mifos Initiative
 */
package org.apache.fineract.branch.connector.data;

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
public class ReconciliationItemData {
    private String externalId;
    private Long resourceId;
    private String branchId;
    private String operatorId;
    private String terminalId;
    private String refundReference;
    private String depositReference;
    private BigDecimal amount;
    private LocalDate transactionDate;
    private OffsetDateTime registeredAt;
    private String status;
    /** MATCHED | AMOUNT_MISMATCH | MISSING_IN_MIFOS | MISSING_IN_BRANCH | PENDING */
    private String reconciliationStatus;
}
