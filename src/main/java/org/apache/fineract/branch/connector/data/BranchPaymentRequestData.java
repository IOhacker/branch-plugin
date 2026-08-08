/**
 * Copyright 2026   Mifos Initiative
 */
package org.apache.fineract.branch.connector.data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchPaymentRequestData {

    @NotBlank
    @Size(max = 128)
    private String refundReference;

    @NotBlank
    private String transactionDate;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal amount;

    @NotBlank
    private String currency;

    @NotNull
    private Long paymentTypeId;

    @NotBlank
    @Size(min = 8, max = 100)
    private String externalId;

    @Size(max = 128)
    private String depositReference;

    @NotNull
    @Valid
    private BranchContextData branch;

    @Size(max = 500)
    private String note;
}
