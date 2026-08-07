/**
 * Copyright 2026   Mifos Initiative
 */
package org.apache.fineract.branch.data;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResultData {
    private Long officeId;
    private Long clientId;
    private Long loanId;
    private Long resourceId;
    private String resourceExternalId;
}
