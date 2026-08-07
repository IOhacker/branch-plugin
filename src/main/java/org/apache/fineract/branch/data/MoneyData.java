/**
 * Copyright 2026   Mifos Initiative
 */
package org.apache.fineract.branch.data;

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
