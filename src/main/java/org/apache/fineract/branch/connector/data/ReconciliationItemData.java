/**
 * Copyright since 2026 Mifos Initiative
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.apache.fineract.branch.connector.data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReconciliationItemData {
    private String externalId;
    private Long resourceId;
    private String branchId;
    private String operatorId;
    private String terminalId;
    private String refundReference;
    private String depositReference;
    private BigDecimal amount;
    private LocalDate transactionDate;
    private OffsetDateTime registeredAt;
    private String status;
    /** MATCHED | AMOUNT_MISMATCH | MISSING_IN_MIFOS | MISSING_IN_BRANCH | PENDING */
    private String reconciliationStatus;
}
