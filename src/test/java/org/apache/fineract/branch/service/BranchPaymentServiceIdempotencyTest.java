/**
 * Copyright 2026   Mifos Initiative
 *
 * Unit-level verification of the critical idempotency rule:
 * same externalId must never create a second payment.
 */
package org.apache.fineract.branch.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.apache.fineract.branch.data.BranchContextData;
import org.apache.fineract.branch.data.BranchPaymentRequestData;
import org.apache.fineract.branch.data.BranchPaymentResponseData;
import org.apache.fineract.branch.data.ClientFileData;
import org.apache.fineract.branch.data.LoanBalanceData;
import org.apache.fineract.branch.data.MoneyData;
import org.apache.fineract.branch.data.RefundReferenceResolutionData;
import org.apache.fineract.branch.domain.BranchPaymentEntity;
import org.apache.fineract.branch.domain.BranchPaymentRepository;
import org.apache.fineract.branch.exception.BranchApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class BranchPaymentServiceIdempotencyTest {

    @Mock
    private BranchPaymentRepository paymentRepository;
    @Mock
    private RefundReferenceService refundReferenceService;
    @Mock
    private ClientLoanQueryService clientLoanQueryService;
    @Mock
    private FineractRepaymentGateway repaymentGateway;
    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private BranchPaymentService service;

    private BranchPaymentRequestData request;

    @BeforeEach
    void setUp() {
        request = BranchPaymentRequestData.builder().refundReference("7410912616407487")
                .transactionDate("2026-07-27").amount(new BigDecimal("910.00")).currency("MXN").paymentTypeId(1L)
                .externalId("FIN-SUC-0042-20260727-000001")
                .branch(BranchContextData.builder().branchId("0042").operatorId("operador01").terminalId("CAJA-02")
                        .build())
                .build();
    }

    @Test
    void sameExternalIdReturnsOriginalWithoutCallingCore() {
        BranchPaymentEntity existing = BranchPaymentEntity.builder().externalId(request.getExternalId())
                .status("APPLIED").resourceId(1544L).loanId(40901L).clientId(44333L).amount(request.getAmount())
                .currency("MXN").transactionDate(LocalDate.parse("2026-07-27")).branchId("0042")
                .operatorId("operador01").terminalId("CAJA-02").refundReference(request.getRefundReference())
                .registeredAt(OffsetDateTime.now()).build();

        when(paymentRepository.findByExternalId(request.getExternalId())).thenReturn(Optional.of(existing));

        BranchPaymentResponseData response = service.createBranchPayment(request, request.getExternalId());

        assertEquals("APPLIED", response.getStatus());
        assertEquals(1544L, response.getResourceId());
        verify(repaymentGateway, never()).executeRepayment(any(), any(), anyString());
    }

    @Test
    void differentAmountOnSameExternalIdRaisesConflict() {
        BranchPaymentEntity existing = BranchPaymentEntity.builder().externalId(request.getExternalId())
                .status("APPLIED").amount(new BigDecimal("500.00")).currency("MXN")
                .transactionDate(LocalDate.parse("2026-07-27")).branchId("0042").operatorId("operador01")
                .terminalId("CAJA-02").refundReference(request.getRefundReference())
                .registeredAt(OffsetDateTime.now()).build();

        when(paymentRepository.findByExternalId(anyString())).thenReturn(Optional.of(existing));

        // createRepaymentByReference path checks amount conflict
        var repaymentReq = org.apache.fineract.branch.data.RepaymentRequestData.builder()
                .transactionDate("2026-07-27").dateFormat("yyyy-MM-dd").locale("es").paymentTypeId(1L)
                .transactionAmount(new BigDecimal("910.00")).externalId(request.getExternalId()).build();

        BranchApiException ex = assertThrows(BranchApiException.class,
                () -> service.createRepaymentByReference(request.getRefundReference(), repaymentReq,
                        request.getExternalId()));
        assertEquals("PAYMENT_DUPLICATE_CONFLICT", ex.getCode());
    }
}
