/**
 * Copyright 2026   Mifos Initiative
 */
package org.apache.fineract.branch.connector.data;

import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class LoanBalanceData extends LoanSummaryData {
    private MoneyData principalOutstanding;
    private MoneyData interestOutstanding;
    private MoneyData feeOutstanding;
    private MoneyData penaltyOutstanding;
    private MoneyData totalOverdue;
    private LocalDate nextDueDate;
    private MoneyData nextInstallmentAmount;
    private Boolean inArrears;
    private Boolean disbursed;
}
