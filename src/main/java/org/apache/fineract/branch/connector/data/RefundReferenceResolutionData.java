/**
 * Copyright since 2026 Mifos Initiative
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.apache.fineract.branch.connector.data;

import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundReferenceResolutionData {
    private String reference;
    /** ACTIVE | INACTIVE | EXPIRED | USED | BLOCKED */
    private String status;
    private ClientFileData client;
    private LoanBalanceData loan;
    private Boolean payable;
    private String blockingReason;
    private OffsetDateTime resolvedAt;
}
