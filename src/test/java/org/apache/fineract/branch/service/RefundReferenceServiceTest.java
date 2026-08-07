/**
 * Copyright 2026   Mifos Initiative
 */
package org.apache.fineract.branch.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import org.apache.fineract.branch.data.ClientFileData;
import org.apache.fineract.branch.data.LoanBalanceData;
import org.apache.fineract.branch.data.MoneyData;
import org.apache.fineract.branch.data.PaymentValidationRequestData;
import org.apache.fineract.branch.data.PaymentValidationResultData;
import org.apache.fineract.branch.data.RefundReferenceResolutionData;
import org.apache.fineract.branch.exception.BranchApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

@ExtendWith(MockitoExtension.class)
class RefundReferenceServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private ClientLoanQueryService clientLoanQueryService;

    @InjectMocks
    private RefundReferenceService service;

    @Test
    void resolve_validReference() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq("7410912616407487")))
                .thenReturn(List.of(40901L));

        LoanBalanceData loan = LoanBalanceData.builder().loanId(40901L).status("ACTIVE").disbursed(true)
                .totalOutstanding(MoneyData.mxn(new BigDecimal("10000"))).build();
        ClientFileData client = ClientFileData.builder().clientId(48L).displayName("PERSONA").status("ACTIVE")
                .build();
        when(clientLoanQueryService.getLoanBalanceById(40901L)).thenReturn(loan);
        when(clientLoanQueryService.getClientByLoanId(40901L)).thenReturn(client);

        RefundReferenceResolutionData result = service.resolve("7410912616407487");

        assertEquals("ACTIVE", result.getStatus());
        assertTrue(result.getPayable());
        assertEquals(40901L, result.getLoan().getLoanId());
        assertEquals(48L, result.getClient().getClientId());
    }

    @Test
    void resolve_notFound() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyString()))
                .thenReturn(Collections.emptyList());

        BranchApiException ex = assertThrows(BranchApiException.class, () -> service.resolve("REF_INEXISTENTE"));
        assertEquals("REFERENCE_NOT_FOUND", ex.getCode());
        assertEquals(404, ex.getHttpStatus());
    }

    @Test
    void resolve_ambiguous() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq("DUP_REF"))).thenReturn(List.of(1L, 2L));

        BranchApiException ex = assertThrows(BranchApiException.class, () -> service.resolve("DUP_REF"));
        assertEquals("REFERENCE_AMBIGUOUS", ex.getCode());
        assertEquals(409, ex.getHttpStatus());
    }

    @Test
    void resolve_invalidEmpty() {
        BranchApiException ex = assertThrows(BranchApiException.class, () -> service.resolve(""));
        assertEquals("INVALID_REFERENCE", ex.getCode());
        assertEquals(400, ex.getHttpStatus());
    }

    @Test
    void resolve_invalidTooLong() {
        String longRef = "x".repeat(129);
        BranchApiException ex = assertThrows(BranchApiException.class, () -> service.resolve(longRef));
        assertEquals("INVALID_REFERENCE", ex.getCode());
    }

    @Test
    void resolve_notPayableWhenNotDisbursed() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyString())).thenReturn(List.of(100L));

        LoanBalanceData loan = LoanBalanceData.builder().loanId(100L).status("APPROVED").disbursed(false).build();
        ClientFileData client = ClientFileData.builder().clientId(1L).build();
        when(clientLoanQueryService.getLoanBalanceById(100L)).thenReturn(loan);
        when(clientLoanQueryService.getClientByLoanId(100L)).thenReturn(client);

        RefundReferenceResolutionData result = service.resolve("REF_APPROVED");
        assertFalse(result.getPayable());
        assertEquals("INACTIVE", result.getStatus());
        assertNotNullBlocking(result);
    }

    @Test
    void validatePayment_valid() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyString())).thenReturn(List.of(40901L));
        LoanBalanceData loan = LoanBalanceData.builder().loanId(40901L).status("ACTIVE").disbursed(true)
                .totalOutstanding(MoneyData.mxn(new BigDecimal("10000"))).build();
        when(clientLoanQueryService.getLoanBalanceById(40901L)).thenReturn(loan);
        when(clientLoanQueryService.getClientByLoanId(40901L))
                .thenReturn(ClientFileData.builder().clientId(1L).build());

        PaymentValidationRequestData req = PaymentValidationRequestData.builder().amount(new BigDecimal("910.00"))
                .transactionDate("2026-07-27").branchId("0042").build();

        PaymentValidationResultData result = service.validatePayment("7410912616407487", req);
        assertTrue(result.getValid());
        assertEquals(new BigDecimal("9090.00"), result.getProjectedOutstanding());
    }

    @Test
    void validatePayment_notPayable() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyString())).thenReturn(List.of(100L));
        LoanBalanceData loan = LoanBalanceData.builder().loanId(100L).status("CLOSED_OBLIGATIONS_MET").disbursed(true)
                .build();
        when(clientLoanQueryService.getLoanBalanceById(100L)).thenReturn(loan);
        when(clientLoanQueryService.getClientByLoanId(100L))
                .thenReturn(ClientFileData.builder().clientId(1L).build());

        PaymentValidationRequestData req = PaymentValidationRequestData.builder().amount(new BigDecimal("100.00"))
                .transactionDate("2026-07-27").branchId("0042").build();

        PaymentValidationResultData result = service.validatePayment("REF_CLOSED", req);
        assertFalse(result.getValid());
    }

    private void assertNotNullBlocking(RefundReferenceResolutionData result) {
        assertTrue(result.getBlockingReason() != null && !result.getBlockingReason().isBlank());
    }
}
