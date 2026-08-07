/**
 * Copyright 2026   Mifos Initiative
 */
package org.apache.fineract.branch.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import java.util.List;
import java.util.Optional;
import org.apache.fineract.branch.data.BranchContextData;
import org.apache.fineract.branch.data.BranchPaymentRequestData;
import org.apache.fineract.branch.data.BranchPaymentResponseData;
import org.apache.fineract.branch.data.ClientFileData;
import org.apache.fineract.branch.data.LoanBalanceData;
import org.apache.fineract.branch.data.MoneyData;
import org.apache.fineract.branch.data.PaymentResultData;
import org.apache.fineract.branch.data.ReconciliationItemData;
import org.apache.fineract.branch.data.RefundReferenceResolutionData;
import org.apache.fineract.branch.data.RepaymentRequestData;
import org.apache.fineract.branch.data.ReversalRequestData;
import org.apache.fineract.branch.domain.BranchPaymentEntity;
import org.apache.fineract.branch.domain.BranchPaymentRepository;
import org.apache.fineract.branch.exception.BranchApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class BranchPaymentServiceTest {

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

    private BranchPaymentRequestData branchRequest;
    private RefundReferenceResolutionData resolution;
    private LoanBalanceData loan;

    @BeforeEach
    void setUp() {
        loan = LoanBalanceData.builder().loanId(40901L).accountNo("000040901").status("ACTIVE").disbursed(true)
                .totalOutstanding(MoneyData.mxn(new BigDecimal("10000.00")))
                .principal(MoneyData.mxn(new BigDecimal("10000.00"))).build();

        ClientFileData client = ClientFileData.builder().clientId(44333L).accountNo("0000123456")
                .displayName("PERSONA DE PRUEBA").status("ACTIVE").officeId(1L).build();

        resolution = RefundReferenceResolutionData.builder().reference("7410912616407487").status("ACTIVE")
                .client(client).loan(loan).payable(true).build();

        branchRequest = BranchPaymentRequestData.builder().refundReference("7410912616407487")
                .transactionDate("2026-07-27").amount(new BigDecimal("910.00")).currency("MXN").paymentTypeId(1L)
                .externalId("FIN-SUC-0042-20260727-000001")
                .branch(BranchContextData.builder().branchId("0042").operatorId("operador01").terminalId("CAJA-02")
                        .build())
                .note("Cobro en sucursal").build();
    }

    @Test
    void createBranchPayment_success() {
        when(paymentRepository.findByExternalId(anyString())).thenReturn(Optional.empty());
        when(refundReferenceService.resolve(anyString())).thenReturn(resolution);
        when(paymentRepository.save(any(BranchPaymentEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(repaymentGateway.executeRepayment(eq(40901L), any(), anyString())).thenReturn(1544L);
        when(clientLoanQueryService.getLoanBalanceById(40901L)).thenReturn(loan);

        BranchPaymentResponseData response = service.createBranchPayment(branchRequest, null);

        assertEquals("APPLIED", response.getStatus());
        assertEquals(1544L, response.getResourceId());
        assertEquals("FIN-SUC-0042-20260727-000001", response.getExternalId());
        assertEquals(new BigDecimal("910.00"), response.getAmount());
        verify(repaymentGateway).executeRepayment(eq(40901L), any(), eq("FIN-SUC-0042-20260727-000001"));
    }

    @Test
    void createBranchPayment_idempotentReturn() {
        BranchPaymentEntity existing = baseEntity("APPLIED", 1544L);
        when(paymentRepository.findByExternalId(anyString())).thenReturn(Optional.of(existing));

        BranchPaymentResponseData response = service.createBranchPayment(branchRequest, branchRequest.getExternalId());

        assertEquals("APPLIED", response.getStatus());
        assertEquals(1544L, response.getResourceId());
        verify(repaymentGateway, never()).executeRepayment(any(), any(), anyString());
    }

    @Test
    void createBranchPayment_rejectsZeroAmount() {
        branchRequest.setAmount(BigDecimal.ZERO);
        when(paymentRepository.findByExternalId(anyString())).thenReturn(Optional.empty());

        BranchApiException ex = assertThrows(BranchApiException.class,
                () -> service.createBranchPayment(branchRequest, null));
        assertEquals("PAYMENT_AMOUNT_INVALID", ex.getCode());
        assertEquals(400, ex.getHttpStatus());
    }

    @Test
    void createBranchPayment_rejectsNegativeAmount() {
        branchRequest.setAmount(new BigDecimal("-10.00"));
        when(paymentRepository.findByExternalId(anyString())).thenReturn(Optional.empty());

        BranchApiException ex = assertThrows(BranchApiException.class,
                () -> service.createBranchPayment(branchRequest, null));
        assertEquals("PAYMENT_AMOUNT_INVALID", ex.getCode());
    }

    @Test
    void createBranchPayment_rejectsTooManyDecimals() {
        branchRequest.setAmount(new BigDecimal("910.123"));
        when(paymentRepository.findByExternalId(anyString())).thenReturn(Optional.empty());

        BranchApiException ex = assertThrows(BranchApiException.class,
                () -> service.createBranchPayment(branchRequest, null));
        assertEquals("PAYMENT_SCALE_INVALID", ex.getCode());
    }

    @Test
    void createBranchPayment_rejectsNonMxn() {
        branchRequest.setCurrency("USD");
        when(paymentRepository.findByExternalId(anyString())).thenReturn(Optional.empty());

        BranchApiException ex = assertThrows(BranchApiException.class,
                () -> service.createBranchPayment(branchRequest, null));
        assertEquals("INVALID_REQUEST", ex.getCode());
    }

    @Test
    void createBranchPayment_rejectsMissingExternalId() {
        branchRequest.setExternalId(null);
        BranchApiException ex = assertThrows(BranchApiException.class,
                () -> service.createBranchPayment(branchRequest, null));
        assertEquals("INVALID_REQUEST", ex.getCode());
    }

    @Test
    void createBranchPayment_rejectsOverpayment() {
        when(paymentRepository.findByExternalId(anyString())).thenReturn(Optional.empty());
        when(refundReferenceService.resolve(anyString())).thenReturn(resolution);
        branchRequest.setAmount(new BigDecimal("20000.00"));

        BranchApiException ex = assertThrows(BranchApiException.class,
                () -> service.createBranchPayment(branchRequest, null));
        assertEquals("OVERPAYMENT_NOT_ALLOWED", ex.getCode());
        assertEquals(422, ex.getHttpStatus());
    }

    @Test
    void createBranchPayment_rejectsNonPayableLoan() {
        resolution.setPayable(false);
        resolution.setBlockingReason("El crédito aún no ha sido desembolsado");
        when(paymentRepository.findByExternalId(anyString())).thenReturn(Optional.empty());
        when(refundReferenceService.resolve(anyString())).thenReturn(resolution);

        BranchApiException ex = assertThrows(BranchApiException.class,
                () -> service.createBranchPayment(branchRequest, null));
        assertEquals("LOAN_NOT_ACTIVE", ex.getCode());
    }

    @Test
    void createBranchPayment_marksUnknownOnGatewayFailure() {
        when(paymentRepository.findByExternalId(anyString())).thenReturn(Optional.empty());
        when(refundReferenceService.resolve(anyString())).thenReturn(resolution);
        when(paymentRepository.save(any(BranchPaymentEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(repaymentGateway.executeRepayment(any(), any(), anyString()))
                .thenThrow(new RuntimeException("timeout"));

        BranchApiException ex = assertThrows(BranchApiException.class,
                () -> service.createBranchPayment(branchRequest, null));
        assertEquals("PAYMENT_STATUS_UNKNOWN", ex.getCode());

        ArgumentCaptor<BranchPaymentEntity> captor = ArgumentCaptor.forClass(BranchPaymentEntity.class);
        verify(paymentRepository, org.mockito.Mockito.atLeast(2)).save(captor.capture());
        assertEquals("UNKNOWN", captor.getValue().getStatus());
    }

    @Test
    void createRepaymentByReference_success() {
        when(paymentRepository.findByExternalId(anyString())).thenReturn(Optional.empty());
        when(refundReferenceService.resolve(anyString())).thenReturn(resolution);
        when(paymentRepository.save(any(BranchPaymentEntity.class))).thenAnswer(inv -> inv.getArgument(0));
        when(repaymentGateway.executeRepayment(eq(40901L), any(), anyString())).thenReturn(1544L);

        RepaymentRequestData req = RepaymentRequestData.builder().transactionDate("2026-07-27")
                .dateFormat("yyyy-MM-dd").locale("es").paymentTypeId(1L).transactionAmount(new BigDecimal("910.00"))
                .externalId("FIN-SUC-0042-20260727-000001").build();

        PaymentResultData result = service.createRepaymentByReference("7410912616407487", req, null);

        assertEquals(1544L, result.getResourceId());
        assertEquals(40901L, result.getLoanId());
        assertEquals(44333L, result.getClientId());
    }

    @Test
    void createRepaymentByReference_duplicateSameAmount() {
        BranchPaymentEntity existing = baseEntity("APPLIED", 1544L);
        when(paymentRepository.findByExternalId(anyString())).thenReturn(Optional.of(existing));

        RepaymentRequestData req = RepaymentRequestData.builder().transactionDate("2026-07-27")
                .dateFormat("yyyy-MM-dd").locale("es").paymentTypeId(1L).transactionAmount(new BigDecimal("910.00"))
                .externalId("FIN-SUC-0042-20260727-000001").build();

        PaymentResultData result = service.createRepaymentByReference("7410912616407487", req,
                "FIN-SUC-0042-20260727-000001");
        assertEquals(1544L, result.getResourceId());
        verify(repaymentGateway, never()).executeRepayment(any(), any(), anyString());
    }

    @Test
    void createRepaymentByReference_duplicateDifferentAmount() {
        BranchPaymentEntity existing = baseEntity("APPLIED", 1544L);
        existing.setAmount(new BigDecimal("500.00"));
        when(paymentRepository.findByExternalId(anyString())).thenReturn(Optional.of(existing));

        RepaymentRequestData req = RepaymentRequestData.builder().transactionDate("2026-07-27")
                .dateFormat("yyyy-MM-dd").locale("es").paymentTypeId(1L).transactionAmount(new BigDecimal("910.00"))
                .externalId("FIN-SUC-0042-20260727-000001").build();

        BranchApiException ex = assertThrows(BranchApiException.class,
                () -> service.createRepaymentByReference("7410912616407487", req, "FIN-SUC-0042-20260727-000001"));
        assertEquals("PAYMENT_DUPLICATE_CONFLICT", ex.getCode());
        assertEquals(409, ex.getHttpStatus());
    }

    @Test
    void getByExternalId_found() {
        when(paymentRepository.findByExternalId("FIN-SUC-0042-20260727-000001"))
                .thenReturn(Optional.of(baseEntity("APPLIED", 1544L)));

        BranchPaymentResponseData response = service.getByExternalId("FIN-SUC-0042-20260727-000001");
        assertEquals("APPLIED", response.getStatus());
        assertNotNull(response.getExternalId());
    }

    @Test
    void getByExternalId_notFound() {
        when(paymentRepository.findByExternalId(anyString())).thenReturn(Optional.empty());
        BranchApiException ex = assertThrows(BranchApiException.class,
                () -> service.getByExternalId("MISSING"));
        assertEquals("PAYMENT_NOT_FOUND", ex.getCode());
        assertEquals(404, ex.getHttpStatus());
    }

    @Test
    void reverse_success() {
        BranchPaymentEntity existing = baseEntity("APPLIED", 1544L);
        when(paymentRepository.findByExternalId(anyString())).thenReturn(Optional.of(existing));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ReversalRequestData req = ReversalRequestData.builder().reason("Pago registrado incorrectamente")
                .requestedBy("supervisor01").branchId("0042").build();

        BranchPaymentResponseData response = service.reverse("FIN-SUC-0042-20260727-000001", req);
        assertEquals("REVERSED", response.getStatus());
        verify(repaymentGateway).reverseRepayment(eq(40901L), eq(1544L), anyString());
    }

    @Test
    void reverse_alreadyReversed() {
        BranchPaymentEntity existing = baseEntity("REVERSED", 1544L);
        when(paymentRepository.findByExternalId(anyString())).thenReturn(Optional.of(existing));

        ReversalRequestData req = ReversalRequestData.builder().reason("Pago registrado incorrectamente")
                .requestedBy("supervisor01").branchId("0042").build();

        BranchApiException ex = assertThrows(BranchApiException.class,
                () -> service.reverse("FIN-SUC-0042-20260727-000001", req));
        assertEquals("PAYMENT_ALREADY_REVERSED", ex.getCode());
    }

    @Test
    void reverse_notApplied() {
        BranchPaymentEntity existing = baseEntity("UNKNOWN", null);
        when(paymentRepository.findByExternalId(anyString())).thenReturn(Optional.of(existing));

        ReversalRequestData req = ReversalRequestData.builder().reason("Pago registrado incorrectamente")
                .requestedBy("supervisor01").branchId("0042").build();

        BranchApiException ex = assertThrows(BranchApiException.class,
                () -> service.reverse("FIN-SUC-0042-20260727-000001", req));
        assertEquals("PAYMENT_NOT_REVERSIBLE", ex.getCode());
    }

    @Test
    void getBranchPayments_mapsReconciliation() {
        BranchPaymentEntity e = baseEntity("APPLIED", 1544L);
        when(paymentRepository.findForReconciliation(eq("0042"), any(), any(), anyString())).thenReturn(List.of(e));

        List<ReconciliationItemData> items = service.getBranchPayments("0042", LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31), "ALL");

        assertEquals(1, items.size());
        assertEquals("MATCHED", items.get(0).getReconciliationStatus());
        assertEquals("APPLIED", items.get(0).getStatus());
    }

    @Test
    void getBranchPayments_unknownMapsPending() {
        BranchPaymentEntity e = baseEntity("UNKNOWN", null);
        when(paymentRepository.findForReconciliation(anyString(), any(), any(), any())).thenReturn(List.of(e));

        List<ReconciliationItemData> items = service.getBranchPayments("0042", LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 7, 31), "UNKNOWN");
        assertEquals("PENDING", items.get(0).getReconciliationStatus());
    }

    private BranchPaymentEntity baseEntity(String status, Long resourceId) {
        return BranchPaymentEntity.builder().externalId("FIN-SUC-0042-20260727-000001").status(status)
                .resourceId(resourceId).loanId(40901L).clientId(44333L).amount(new BigDecimal("910.00"))
                .currency("MXN").transactionDate(LocalDate.parse("2026-07-27")).branchId("0042")
                .operatorId("operador01").terminalId("CAJA-02").refundReference("7410912616407487")
                .registeredAt(OffsetDateTime.now()).build();
    }
}
