/**
 * Copyright 2026   Mifos Initiative
 */
package org.apache.fineract.branch.data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReversalRequestData {

    @NotBlank
    @Size(min = 10, max = 500)
    private String reason;

    @NotBlank
    private String requestedBy;

    @NotBlank
    private String branchId;
}
