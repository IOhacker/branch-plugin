/**
 * Copyright 2026   Mifos Initiative
 */
package org.apache.fineract.branch.data;

import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundReferenceResolutionData {
    private String reference;
    /** ACTIVE | INACTIVE | EXPIRED | USED | BLOCKED */
    private String status;
    private ClientFileData client;
    private LoanBalanceData loan;
    private Boolean payable;
    private String blockingReason;
    private OffsetDateTime resolvedAt;
}
