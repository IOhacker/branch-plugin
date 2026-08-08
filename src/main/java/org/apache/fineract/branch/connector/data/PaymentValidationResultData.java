/**
 * Copyright 2026   Mifos Initiative
 */
package org.apache.fineract.branch.connector.data;

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
public class PaymentValidationResultData {
    private Boolean valid;
    private String referenceStatus;
    private List<String> warnings;
    private List<ApiErrorDetailData> blockingErrors;
    private BigDecimal projectedOutstanding;
}
