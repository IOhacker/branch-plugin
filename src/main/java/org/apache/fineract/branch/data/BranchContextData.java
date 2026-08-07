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
public class BranchContextData {

    @NotBlank
    @Size(max = 30)
    private String branchId;

    @NotBlank
    @Size(max = 50)
    private String operatorId;

    @NotBlank
    @Size(max = 30)
    private String terminalId;
}
