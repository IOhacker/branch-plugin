/**
 * Copyright 2026   Mifos Initiative
 */
package org.apache.fineract.branch.connector.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class LoanSummaryData {
    private Long loanId;
    private String accountNo;
    private String externalId;
    private String productName;
    private String status;
    private MoneyData principal;
    private MoneyData totalOutstanding;
    /** Refund reference used by branch tellers to locate the loan. Stored as text. */
    private String refundReference;
}
