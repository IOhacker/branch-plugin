/**
 * Copyright since 2026 Mifos Initiative
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.apache.fineract.branch.connector.data;

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
