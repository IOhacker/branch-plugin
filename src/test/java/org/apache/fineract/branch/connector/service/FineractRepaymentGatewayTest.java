/**
 * Copyright since 2026 Mifos Initiative
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.apache.fineract.branch.connector.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import org.apache.fineract.branch.connector.data.RepaymentRequestData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

@ExtendWith(MockitoExtension.class)
class FineractRepaymentGatewayTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private FineractRepaymentGateway gateway;

    @Test
    void executeRepayment_returnsExistingWhenPresent() {
        when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class), eq(40901L), eq("EXT-1")))
                .thenReturn(999L);

        RepaymentRequestData req = RepaymentRequestData.builder().transactionDate("2026-07-27")
                .dateFormat("yyyy-MM-dd").locale("es").paymentTypeId(1L).transactionAmount(new BigDecimal("100.00"))
                .externalId("EXT-1").build();

        Long id = gateway.executeRepayment(40901L, req, "EXT-1");
        assertEquals(999L, id);
    }

    @Test
    void executeRepayment_scaffoldWhenNoExisting() {
        when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class), any(), anyString()))
                .thenReturn(null);

        RepaymentRequestData req = RepaymentRequestData.builder().transactionDate("2026-07-27")
                .dateFormat("yyyy-MM-dd").locale("es").paymentTypeId(1L).transactionAmount(new BigDecimal("100.00"))
                .externalId("EXT-NEW").note("test").build();

        Long id = gateway.executeRepayment(40901L, req, "EXT-NEW");
        assertNotNull(id);
    }

    @Test
    void executeRepayment_scaffoldWhenQueryThrows() {
        when(jdbcTemplate.query(anyString(), any(ResultSetExtractor.class), any(), anyString()))
                .thenThrow(new RuntimeException("no column"));

        RepaymentRequestData req = RepaymentRequestData.builder().transactionDate("2026-07-27")
                .dateFormat("yyyy-MM-dd").locale("es").paymentTypeId(1L).transactionAmount(new BigDecimal("50.00"))
                .externalId("EXT-2").build();

        Long id = gateway.executeRepayment(1L, req, "EXT-2");
        assertNotNull(id);
    }

    @Test
    void reverseRepayment_doesNotThrow() {
        gateway.reverseRepayment(1L, 2L, "reason");
    }

    private static void assertEquals(long expected, Long actual) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
    }
}
