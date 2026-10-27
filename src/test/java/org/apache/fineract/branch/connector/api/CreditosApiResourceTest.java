/**
 * Copyright since 2026 Mifos Initiative
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0.
 */
package org.apache.fineract.branch.connector.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.apache.fineract.branch.connector.exception.BranchApiException;
import org.apache.fineract.branch.connector.service.BranchPaymentService;
import org.apache.fineract.branch.connector.service.ClientLoanQueryService;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.loanaccount.data.LoanAccountData;
import org.apache.fineract.useradministration.domain.AppUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CreditosApiResourceTest {

    private static final String REFERENCIA = "REF-12345";
    private static final String LOAN_JSON = "{\"id\":41246,\"accountNo\":\"000041246\"}";

    @Test
    void getLoanByReferenciaRembolso_returnsSamePayloadAsStandardEndpoint() {
        ClientLoanQueryService loanService = mock(ClientLoanQueryService.class);
        BranchPaymentService paymentService = mock(BranchPaymentService.class);
        PlatformSecurityContext security = mock(PlatformSecurityContext.class);
        AppUser user = mock(AppUser.class);
        DefaultToApiJsonSerializer<LoanAccountData> serializer = mock(DefaultToApiJsonSerializer.class);
        LoanAccountData loan = mock(LoanAccountData.class);

        when(security.authenticatedUser()).thenReturn(user);
        when(loanService.retrieveLoanByReferenciaRembolso(REFERENCIA)).thenReturn(loan);
        when(serializer.serialize(loan)).thenReturn(LOAN_JSON);

        CreditosApiResource resource = new CreditosApiResource(loanService, paymentService, security,
                List.of(serializer));

        String result = resource.getLoanByReferenciaRembolso(REFERENCIA);

        assertEquals(LOAN_JSON, result);
        verify(loanService).retrieveLoanByReferenciaRembolso(REFERENCIA);
        verify(serializer).serialize(loan);
        verify(user).validateHasReadPermission("LOAN");
    }

    @Test
    void getLoanByReferenciaRembolso_propagatesNotFound() {
        ClientLoanQueryService loanService = mock(ClientLoanQueryService.class);
        PlatformSecurityContext security = mock(PlatformSecurityContext.class);
        AppUser user = mock(AppUser.class);
        when(security.authenticatedUser()).thenReturn(user);
        BranchApiException notFound = BranchApiException.notFound("LOAN_NOT_FOUND", "no existe");
        when(loanService.retrieveLoanByReferenciaRembolso(REFERENCIA)).thenThrow(notFound);

        CreditosApiResource resource = new CreditosApiResource(loanService, mock(BranchPaymentService.class),
                security, List.of());

        BranchApiException ex = assertThrows(BranchApiException.class,
                () -> resource.getLoanByReferenciaRembolso(REFERENCIA));
        assertSame(notFound, ex);
        assertEquals(404, ex.getHttpStatus());
    }
}
