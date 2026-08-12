/**
 * Copyright since 2026 Mifos Initiative
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.apache.fineract.branch.connector.data;

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
public class RepaymentRequestData {

    @NotBlank
    private String transactionDate;

    @NotBlank
    private String dateFormat;

    @NotBlank
    private String locale;

    @NotNull
    private Long paymentTypeId;

    @NotNull
    @DecimalMin(value = "0.01", inclusive = true)
    private BigDecimal transactionAmount;

    @NotBlank
    @Size(min = 8, max = 100)
    private String externalId;

    @Size(max = 500)
    private String note;

    @Size(max = 128)
    private String depositReference;
}
