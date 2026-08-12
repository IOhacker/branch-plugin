/**
 * Copyright since 2026 Mifos Initiative
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.apache.fineract.branch.connector.data;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MoneyData {
    private String currency;
    private BigDecimal amount;

    public static MoneyData of(String currency, BigDecimal amount) {
        return MoneyData.builder().currency(currency).amount(amount != null ? amount : BigDecimal.ZERO).build();
    }

    public static MoneyData mxn(BigDecimal amount) {
        return of("MXN", amount);
    }
}
