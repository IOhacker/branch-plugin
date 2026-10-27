/**
 * Copyright since 2026 Mifos Initiative
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0.
 */
package org.apache.fineract.branch.connector.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.apache.fineract.branch.connector.domain.DatosReembolsoEntity;
import org.apache.fineract.branch.connector.domain.DatosReembolsoRepository;
import org.apache.fineract.branch.connector.exception.BranchApiException;
import org.apache.fineract.portfolio.loanaccount.data.LoanAccountData;
import org.apache.fineract.portfolio.loanaccount.service.LoanReadPlatformService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClientLoanQueryServiceTest {

    private static final String REFERENCIA = "REF-12345";
    private static final Long LOAN_ID = 41246L;

    @Mock
    private LoanReadPlatformService loanReadPlatformService;

    @Mock
    private DatosReembolsoRepository datosReembolsoRepository;

    @InjectMocks
    private ClientLoanQueryService service;

    @Test
    void retrieveLoanByReferenciaRembolso_returnsLoanFromCore() {
        DatosReembolsoEntity datos = DatosReembolsoEntity.builder().id(1L).loanId(LOAN_ID)
                .referenciaRembolso(REFERENCIA).build();
        LoanAccountData loan = mock(LoanAccountData.class);

        when(datosReembolsoRepository.countByReferenciaRembolso(REFERENCIA)).thenReturn(1L);
        when(datosReembolsoRepository.findFirstByReferenciaRembolsoOrderByIdDesc(REFERENCIA))
                .thenReturn(java.util.Optional.of(datos));
        when(loanReadPlatformService.retrieveOne(LOAN_ID)).thenReturn(loan);
        when(loanReadPlatformService.countInstallmentsByLoanIdWhereIsAdditionalFalseAndIsDownPaymentFalse(LOAN_ID))
                .thenReturn(10);

        LoanAccountData result = service.retrieveLoanByReferenciaRembolso(REFERENCIA);

        assertNotNull(result);
        assertSame(loan, result);
        verify(loan).setActualNoTerm(10);
    }

    @Test
    void retrieveLoanByReferenciaRembolso_blankReference() {
        BranchApiException ex = assertThrows(BranchApiException.class,
                () -> service.retrieveLoanByReferenciaRembolso(""));
        assertEquals("INVALID_REFERENCIA_REMBOLSO", ex.getCode());
        assertEquals(400, ex.getHttpStatus());
    }

    @Test
    void retrieveLoanByReferenciaRembolso_notFoundInDataTable() {
        when(datosReembolsoRepository.countByReferenciaRembolso(REFERENCIA)).thenReturn(0L);

        BranchApiException ex = assertThrows(BranchApiException.class,
                () -> service.retrieveLoanByReferenciaRembolso(REFERENCIA));
        assertEquals("LOAN_NOT_FOUND_BY_REFERENCIA_REMBOLSO", ex.getCode());
        assertEquals(404, ex.getHttpStatus());
    }

    @Test
    void retrieveLoanByReferenciaRembolso_ambiguousReference() {
        when(datosReembolsoRepository.countByReferenciaRembolso(REFERENCIA)).thenReturn(2L);

        BranchApiException ex = assertThrows(BranchApiException.class,
                () -> service.retrieveLoanByReferenciaRembolso(REFERENCIA));
        assertEquals("REFERENCE_AMBIGUOUS", ex.getCode());
        assertEquals(409, ex.getHttpStatus());
    }

    @Test
    void retrieveLoanByReferenciaRembolso_loanNotFoundInCore() {
        DatosReembolsoEntity datos = DatosReembolsoEntity.builder().id(1L).loanId(LOAN_ID)
                .referenciaRembolso(REFERENCIA).build();

        when(datosReembolsoRepository.countByReferenciaRembolso(REFERENCIA)).thenReturn(1L);
        when(datosReembolsoRepository.findFirstByReferenciaRembolsoOrderByIdDesc(REFERENCIA))
                .thenReturn(java.util.Optional.of(datos));
        when(loanReadPlatformService.retrieveOne(LOAN_ID)).thenReturn(null);

        BranchApiException ex = assertThrows(BranchApiException.class,
                () -> service.retrieveLoanByReferenciaRembolso(REFERENCIA));
        assertEquals("LOAN_NOT_FOUND", ex.getCode());
        assertEquals(404, ex.getHttpStatus());
    }
}
