/**
 * Copyright 2026   Mifos Initiative
 */
package org.apache.fineract.branch.data;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RepaymentScheduleData {
    private Long loanId;
    private String accountNo;
    private String currency;
    private BigDecimal totalPrincipalDisbursed;
    private BigDecimal totalRepaymentExpected;
    private BigDecimal totalOutstanding;
    private List<RepaymentSchedulePeriodData> periods;
}
