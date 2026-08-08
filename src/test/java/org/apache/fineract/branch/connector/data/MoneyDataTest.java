/**
 * Copyright 2026   Mifos Initiative
 */
package org.apache.fineract.branch.connector.data;

import org.apache.fineract.branch.connector.data.MoneyData;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyDataTest {

    @Test
    void ofWithAmount() {
        MoneyData m = MoneyData.of("MXN", new BigDecimal("1250.50"));
        assertEquals("MXN", m.getCurrency());
        assertEquals(new BigDecimal("1250.50"), m.getAmount());
    }

    @Test
    void ofWithNullAmountDefaultsToZero() {
        MoneyData m = MoneyData.of("MXN", null);
        assertEquals(BigDecimal.ZERO, m.getAmount());
    }

    @Test
    void mxnHelper() {
        MoneyData m = MoneyData.mxn(new BigDecimal("100"));
        assertEquals("MXN", m.getCurrency());
        assertEquals(new BigDecimal("100"), m.getAmount());
    }
}
