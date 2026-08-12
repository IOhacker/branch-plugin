/**
 * Copyright since 2026 Mifos Initiative
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.apache.fineract.branch.connector.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.branch.connector.data.ClientFileData;
import org.apache.fineract.branch.connector.data.LoanBalanceData;
import org.apache.fineract.branch.connector.data.LoanSummaryData;
import org.apache.fineract.branch.connector.data.MoneyData;
import org.apache.fineract.branch.connector.data.RepaymentScheduleData;
import org.apache.fineract.branch.connector.data.RepaymentSchedulePeriodData;
import org.apache.fineract.branch.connector.domain.DatosReembolsoEntity;
import org.apache.fineract.branch.connector.domain.DatosReembolsoRepository;
import org.apache.fineract.branch.connector.exception.BranchApiException;
import org.apache.fineract.infrastructure.core.domain.ExternalId;
import org.apache.fineract.infrastructure.core.service.ExternalIdFactory;
import org.apache.fineract.infrastructure.core.service.Page;
import org.apache.fineract.infrastructure.core.service.SearchParameters;
import org.apache.fineract.portfolio.client.data.ClientData;
import org.apache.fineract.portfolio.client.service.ClientReadPlatformService;
import org.apache.fineract.portfolio.loanaccount.data.LoanAccountData;
import org.apache.fineract.portfolio.loanaccount.loanschedule.data.LoanScheduleData;
import org.apache.fineract.portfolio.loanaccount.loanschedule.data.LoanSchedulePeriodData;
import org.apache.fineract.portfolio.loanaccount.service.LoanReadPlatformService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientLoanQueryService {

    private final LoanReadPlatformService loanReadPlatformService;
    private final ClientReadPlatformService clientReadPlatformService;
    private final DatosReembolsoRepository datosReembolsoRepository;

    public ClientFileData getClientFile(String identifier) {
        ClientData client = resolveClient(identifier);
        return mapClient(client);
    }

    public List<LoanSummaryData> getClientAccounts(String identifier) {
        ClientData client = resolveClient(identifier);
        Page<LoanAccountData> page = loanReadPlatformService.retrieveAll(
                SearchParameters.builder()
                        .clientId(client.getId())
                        .limit(200)
                        .build());
        List<LoanAccountData> loans = page != null && page.getPageItems() != null
                ? page.getPageItems()
                : List.of();
        return loans.stream().map(this::mapLoanSummary).collect(Collectors.toList());
    }

    public LoanBalanceData getLoanBalance(String identifier) {
        Long loanId = resolveLoanId(identifier);
        return getLoanBalanceById(loanId);
    }

    public LoanBalanceData getLoanBalanceById(Long loanId) {
        LoanAccountData loan = loanReadPlatformService.retrieveOne(loanId);
        if (loan == null) {
            throw BranchApiException.notFound("LOAN_NOT_FOUND", "Crédito no encontrado: " + loanId);
        }
        return mapLoanBalance(loan);
    }

    public ClientFileData getClientByLoanId(Long loanId) {
        LoanAccountData loan = loanReadPlatformService.retrieveOne(loanId);
        if (loan == null || loan.getClientId() == null) {
            throw BranchApiException.notFound("CLIENT_NOT_FOUND", "Cliente del crédito no encontrado");
        }
        ClientData client = clientReadPlatformService.retrieveOne(loan.getClientId());
        return mapClient(client);
    }

    public RepaymentScheduleData getRepaymentSchedule(String identifier) {
        Long loanId = resolveLoanId(identifier);
        return getRepaymentScheduleByLoanId(loanId);
    }

    public RepaymentScheduleData getRepaymentScheduleByLoanId(Long loanId) {
        LoanAccountData loan = loanReadPlatformService.retrieveOne(loanId);
        if (loan == null) {
            throw BranchApiException.notFound("LOAN_NOT_FOUND", "Crédito no encontrado: " + loanId);
        }
        LoanAccountData withSchedule = loanReadPlatformService.fetchRepaymentScheduleData(loan);

        LoanScheduleData schedule = withSchedule != null ? withSchedule.getRepaymentSchedule() : null;
        List<RepaymentSchedulePeriodData> periods = new ArrayList<>();
        if (schedule != null && schedule.getPeriods() != null) {
            for (LoanSchedulePeriodData p : schedule.getPeriods()) {
                if (p.getPeriod() == null) {
                    continue; // skip disbursement / non-installment periods
                }
                BigDecimal totalDue = nullToZero(p.getPrincipalDue())
                        .add(nullToZero(p.getInterestDue()))
                        .add(nullToZero(p.getFeeChargesDue()))
                        .add(nullToZero(p.getPenaltyChargesDue()));
                BigDecimal totalPaid = nullToZero(p.getPrincipalPaid())
                        .add(nullToZero(p.getInterestPaid()))
                        .add(nullToZero(p.getFeeChargesPaid()))
                        .add(nullToZero(p.getPenaltyChargesPaid()));
                boolean complete = Boolean.TRUE.equals(p.getComplete());
                LocalDate dueDate = p.getDueDate();
                boolean overdue = !complete && dueDate != null && dueDate.isBefore(LocalDate.now());

                periods.add(RepaymentSchedulePeriodData.builder()
                        .period(p.getPeriod())
                        .dueDate(dueDate)
                        .principalDue(nullToZero(p.getPrincipalDue()))
                        .interestDue(nullToZero(p.getInterestDue()))
                        .feeChargesDue(nullToZero(p.getFeeChargesDue()))
                        .penaltyChargesDue(nullToZero(p.getPenaltyChargesDue()))
                        .totalDue(totalDue)
                        .totalPaid(totalPaid)
                        .totalOutstanding(totalDue.subtract(totalPaid))
                        .complete(complete)
                        .overdue(overdue)
                        .build());
            }
        }

        BigDecimal principal = loan.getPrincipal() != null ? loan.getPrincipal() : BigDecimal.ZERO;
        BigDecimal outstanding = loan.getSummary() != null && loan.getSummary().getTotalOutstanding() != null
                ? loan.getSummary().getTotalOutstanding()
                : BigDecimal.ZERO;

        return RepaymentScheduleData.builder()
                .loanId(loanId)
                .accountNo(loan.getAccountNo())
                .currency(loan.getCurrency() != null ? loan.getCurrency().getCode() : "MXN")
                .totalPrincipalDisbursed(principal)
                .totalOutstanding(outstanding)
                .periods(periods)
                .build();
    }

    public List<RepaymentScheduleData> getRepaymentScheduleByClientId(String clientId) {
        ClientData client = resolveClient(clientId);
        Page<LoanAccountData> page = loanReadPlatformService.retrieveAll(
                SearchParameters.builder()
                        .clientId(client.getId())
                        .limit(200)
                        .build());
        List<LoanAccountData> loans = page != null && page.getPageItems() != null
                ? page.getPageItems()
                : List.of();
        List<RepaymentScheduleData> result = new ArrayList<>(loans.size());
        for (LoanAccountData l : loans) {
            result.add(getRepaymentScheduleByLoanId(l.getId()));
        }
        return result;
    }

    // ---------- private helpers ----------

    private ClientData resolveClient(String identifier) {
        if (!StringUtils.hasText(identifier)) {
            throw BranchApiException.badRequest("INVALID_IDENTIFIER", "Identificador de cliente vacío");
        }

        // 1. Try externalId
        try {
            ExternalId ext = ExternalIdFactory.produce(identifier);
            Long id = clientReadPlatformService.retrieveClientIdByExternalId(ext);
            if (id != null) {
                return clientReadPlatformService.retrieveOne(id);
            }
        } catch (Exception ignored) {
            // fall through
        }

        // 2. Try numeric id
        try {
            Long numeric = Long.valueOf(identifier);
            return clientReadPlatformService.retrieveOne(numeric);
        } catch (NumberFormatException ignored) {
            // fall through
        } catch (Exception ex) {
            // not found or other platform exception
            throw BranchApiException.notFound("CLIENT_NOT_FOUND", "Cliente no encontrado: " + identifier);
        }

        throw BranchApiException.notFound("CLIENT_NOT_FOUND", "Cliente no encontrado: " + identifier);
    }

    /**
     * Resolves a loan identifier that can be:
     * <ul>
     *   <li>loan externalId</li>
     *   <li>loan account number (m_loan.account_no)</li>
     *   <li>numeric loan id</li>
     *   <li>referenciaRembolso stored in DATOS_REEMBOLSOS</li>
     * </ul>
     */
    private Long resolveLoanId(String identifier) {
        if (!StringUtils.hasText(identifier)) {
            throw BranchApiException.badRequest("INVALID_IDENTIFIER", "Identificador de crédito vacío");
        }

        // 1. Try loan externalId
        try {
            ExternalId ext = ExternalIdFactory.produce(identifier);
            Long id = loanReadPlatformService.getResolvedLoanId(ext);
            if (id != null) {
                log.debug("Resolved loan by externalId: {} -> {}", identifier, id);
                return id;
            }
        } catch (Exception ignored) {
            // fall through
        }

        // 2. Try account number (m_loan.account_no)
        Long byAccountNo = resolveLoanIdByAccountNo(identifier);
        if (byAccountNo != null) {
            log.debug("Resolved loan by accountNo: {} -> {}", identifier, byAccountNo);
            return byAccountNo;
        }

        // 3. Try numeric id
        try {
            Long numeric = Long.valueOf(identifier);
            // Verify it exists
            LoanAccountData loan = loanReadPlatformService.retrieveOne(numeric);
            if (loan != null) {
                log.debug("Resolved loan by numeric id: {}", numeric);
                return numeric;
            }
        } catch (NumberFormatException ignored) {
            // fall through
        } catch (Exception ex) {
            // not found – fall through to referenciaRembolso
        }

        // 4. Try referenciaRembolso from DATOS_REEMBOLSOS (tenant table)
        Optional<Long> byRef = datosReembolsoRepository.findLoanIdByReferenciaRembolso(identifier);
        if (byRef.isPresent()) {
            log.debug("Resolved loan by referenciaRembolso: {} -> {}", identifier, byRef.get());
            return byRef.get();
        }

        throw BranchApiException.notFound("LOAN_NOT_FOUND",
                "Crédito no encontrado por accountNo / externalId / id / referenciaRembolso: " + identifier);
    }

    /**
     * Looks up a loan by its account number using Fineract SearchParameters.
     * Returns null when no unique match is found.
     */
    private Long resolveLoanIdByAccountNo(String accountNo) {
        try {
            Page<LoanAccountData> page = loanReadPlatformService.retrieveAll(
                    SearchParameters.builder()
                            .accountNo(accountNo)
                            .limit(5)
                            .build());
            if (page == null || page.getPageItems() == null || page.getPageItems().isEmpty()) {
                return null;
            }
            List<LoanAccountData> matches = page.getPageItems().stream()
                    .filter(l -> accountNo.equals(l.getAccountNo()))
                    .toList();
            if (matches.size() == 1) {
                return matches.get(0).getId();
            }
            if (matches.size() > 1) {
                log.warn("Multiple loans found for accountNo={}, using first id={}", accountNo, matches.get(0).getId());
                return matches.get(0).getId();
            }
        } catch (Exception ex) {
            log.debug("AccountNo lookup failed for {}: {}", accountNo, ex.getMessage());
        }
        return null;
    }

    private ClientFileData mapClient(ClientData c) {
        String statusValue = null;
        if (c.getStatus() != null) {
            statusValue = c.getStatus().getValue();
        }
        String externalIdValue = c.getExternalId() != null ? c.getExternalId().getValue() : null;

        return ClientFileData.builder()
                .clientId(c.getId())
                .accountNo(c.getAccountNo())
                .curp(externalIdValue)
                .displayName(c.getDisplayName())
                .status(statusValue)
                .officeId(c.getOfficeId())
                .officeName(c.getOfficeName())
                .mobileNo(c.getMobileNo())
                .emailAddress(c.getEmailAddress())
                .build();
    }

    private LoanSummaryData mapLoanSummary(LoanAccountData l) {
        BigDecimal principal = l.getPrincipal() != null ? l.getPrincipal() : BigDecimal.ZERO;
        BigDecimal outstanding = l.getSummary() != null && l.getSummary().getTotalOutstanding() != null
                ? l.getSummary().getTotalOutstanding()
                : BigDecimal.ZERO;
        String statusValue = l.getStatus() != null ? l.getStatus().getValue() : null;
        String externalIdValue = l.getExternalId() != null ? l.getExternalId().getValue() : null;

        // Prefer referenciaRembolso from DATOS_REEMBOLSOS when available
        String refundRef = resolveRefundReference(l.getId(), externalIdValue);

        return LoanSummaryData.builder()
                .loanId(l.getId())
                .accountNo(l.getAccountNo())
                .externalId(externalIdValue)
                .productName(l.getLoanProductName())
                .status(statusValue)
                .principal(MoneyData.mxn(principal))
                .totalOutstanding(MoneyData.mxn(outstanding))
                .refundReference(refundRef)
                .build();
    }

    private LoanBalanceData mapLoanBalance(LoanAccountData l) {
        var summary = l.getSummary();
        boolean disbursed = l.getTimeline() != null && l.getTimeline().getActualDisbursementDate() != null;
        BigDecimal totalOutstanding = summary != null ? summary.getTotalOutstanding() : null;
        BigDecimal totalOverdue = summary != null ? summary.getTotalOverdue() : null;
        String statusValue = l.getStatus() != null ? l.getStatus().getValue() : null;
        String externalIdValue = l.getExternalId() != null ? l.getExternalId().getValue() : null;

        String refundRef = resolveRefundReference(l.getId(), externalIdValue);

        return LoanBalanceData.builder()
                .loanId(l.getId())
                .accountNo(l.getAccountNo())
                .externalId(externalIdValue)
                .productName(l.getLoanProductName())
                .status(statusValue)
                .principal(MoneyData.mxn(l.getPrincipal()))
                .totalOutstanding(MoneyData.mxn(totalOutstanding))
                .principalOutstanding(MoneyData.mxn(summary != null ? summary.getPrincipalOutstanding() : null))
                .interestOutstanding(MoneyData.mxn(summary != null ? summary.getInterestOutstanding() : null))
                .feeOutstanding(MoneyData.mxn(summary != null ? summary.getFeeChargesOutstanding() : null))
                .penaltyOutstanding(MoneyData.mxn(summary != null ? summary.getPenaltyChargesOutstanding() : null))
                .totalOverdue(MoneyData.mxn(totalOverdue))
                .disbursed(disbursed)
                .inArrears(totalOverdue != null && totalOverdue.compareTo(BigDecimal.ZERO) > 0)
                .refundReference(refundRef)
                .build();
    }

    /**
     * Returns the first non-blank referenciaRembolso for the loan, falling back to externalId.
     */
    private String resolveRefundReference(Long loanId, String externalIdFallback) {
        if (loanId != null) {
            List<DatosReembolsoEntity> refs = datosReembolsoRepository.findByLoanId(loanId);
            if (refs != null) {
                for (DatosReembolsoEntity r : refs) {
                    if (StringUtils.hasText(r.getReferenciaRembolso())) {
                        return r.getReferenciaRembolso();
                    }
                }
            }
        }
        return externalIdFallback;
    }

    private BigDecimal nullToZero(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
