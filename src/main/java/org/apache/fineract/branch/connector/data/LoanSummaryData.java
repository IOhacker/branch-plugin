/**
 * Copyright since 2026 Mifos Initiative
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.apache.fineract.branch.connector.data;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class LoanSummaryData {
    private Long loanId;
    private String accountNo;
    private String externalId;
    private String productName;
    private String status;
    private MoneyData principal;
    private MoneyData totalOutstanding;
    /** Refund reference used by branch tellers to locate the loan. Stored as text. */
    private String refundReference;
}
