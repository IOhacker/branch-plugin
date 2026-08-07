/**
 * Copyright 2026   Mifos Initiative
 *
 * Read-side services that wrap Fineract loan/client queries and adapt them
 * to the   contract. Uses JDBC against the tenant schema so the plugin
 * remains independent of internal service refactorings.
 */
package org.apache.fineract.branch.service;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.fineract.branch.data.ClientFileData;
import org.apache.fineract.branch.data.LoanBalanceData;
import org.apache.fineract.branch.data.LoanSummaryData;
import org.apache.fineract.branch.data.MoneyData;
import org.apache.fineract.branch.data.RepaymentScheduleData;
import org.apache.fineract.branch.data.RepaymentSchedulePeriodData;
import org.apache.fineract.branch.exception.BranchApiException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientLoanQueryService {

    private final JdbcTemplate jdbcTemplate;

    public ClientFileData getClientFile(String identifier) {
        String sql = """
                SELECT c.id, c.account_no, c.external_id, c.display_name, c.status_enum,
                       c.office_id, o.name AS office_name, c.mobile_no, c.email_address
                FROM m_client c
                LEFT JOIN m_office o ON o.id = c.office_id
                WHERE c.external_id = ? OR c.account_no = ? OR CAST(c.id AS CHAR) = ?
                LIMIT 1
                """;
        try {
            return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> mapClient(rs), identifier, identifier, identifier);
        } catch (EmptyResultDataAccessException ex) {
            throw BranchApiException.notFound("CLIENT_NOT_FOUND", "Cliente no encontrado: " + identifier);
        }
    }

    public List<LoanSummaryData> getClientAccounts(String identifier) {
        ClientFileData client = getClientFile(identifier);
        String sql = """
                SELECT l.id, l.account_no, l.external_id, lp.name AS product_name, l.loan_status_id,
                       l.principal_amount, ls.total_outstanding_derived
                FROM m_loan l
                JOIN m_product_loan lp ON lp.id = l.product_id
                LEFT JOIN m_loan_summary ls ON ls.loan_id = l.id
                WHERE l.client_id = ?
                ORDER BY l.id
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> LoanSummaryData.builder().loanId(rs.getLong("id"))
                .accountNo(rs.getString("account_no")).externalId(rs.getString("external_id"))
                .productName(rs.getString("product_name")).status(mapLoanStatus(rs.getInt("loan_status_id")))
                .principal(MoneyData.mxn(rs.getBigDecimal("principal_amount")))
                .totalOutstanding(MoneyData.mxn(rs.getBigDecimal("total_outstanding_derived")))
                .refundReference(rs.getString("external_id")).build(), client.getClientId());
    }

    public LoanBalanceData getLoanBalance(String identifier) {
        Long loanId = resolveLoanId(identifier);
        return getLoanBalanceById(loanId);
    }

    public LoanBalanceData getLoanBalanceById(Long loanId) {
        String sql = """
                SELECT l.id, l.account_no, l.external_id, lp.name AS product_name, l.loan_status_id,
                       l.principal_amount, l.disbursedon_date,
                       ls.principal_outstanding_derived, ls.interest_outstanding_derived,
                       ls.fee_charges_outstanding_derived, ls.penalty_charges_outstanding_derived,
                       ls.total_outstanding_derived, ls.total_overdue_derived
                FROM m_loan l
                JOIN m_product_loan lp ON lp.id = l.product_id
                LEFT JOIN m_loan_summary ls ON ls.loan_id = l.id
                WHERE l.id = ?
                """;
        try {
            LoanBalanceData balance = jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                boolean disbursed = rs.getDate("disbursedon_date") != null;
                BigDecimal totalOutstanding = rs.getBigDecimal("total_outstanding_derived");
                BigDecimal totalOverdue = rs.getBigDecimal("total_overdue_derived");
                return LoanBalanceData.builder().loanId(rs.getLong("id")).accountNo(rs.getString("account_no"))
                        .externalId(rs.getString("external_id")).productName(rs.getString("product_name"))
                        .status(mapLoanStatus(rs.getInt("loan_status_id")))
                        .principal(MoneyData.mxn(rs.getBigDecimal("principal_amount")))
                        .totalOutstanding(MoneyData.mxn(totalOutstanding))
                        .principalOutstanding(MoneyData.mxn(rs.getBigDecimal("principal_outstanding_derived")))
                        .interestOutstanding(MoneyData.mxn(rs.getBigDecimal("interest_outstanding_derived")))
                        .feeOutstanding(MoneyData.mxn(rs.getBigDecimal("fee_charges_outstanding_derived")))
                        .penaltyOutstanding(MoneyData.mxn(rs.getBigDecimal("penalty_charges_outstanding_derived")))
                        .totalOverdue(MoneyData.mxn(totalOverdue)).disbursed(disbursed)
                        .inArrears(totalOverdue != null && totalOverdue.compareTo(BigDecimal.ZERO) > 0)
                        .refundReference(rs.getString("external_id")).build();
            }, loanId);

            // Next installment
            try {
                jdbcTemplate.query(
                        "SELECT duedate, principal_amount + interest_amount + fee_charges_amount + penalty_charges_amount AS total "
                                + "FROM m_loan_repayment_schedule WHERE loan_id = ? AND completed_derived = 0 "
                                + "ORDER BY installment ASC LIMIT 1",
                        rs -> {
                            if (rs.next()) {
                                balance.setNextDueDate(rs.getDate("duedate").toLocalDate());
                                balance.setNextInstallmentAmount(MoneyData.mxn(rs.getBigDecimal("total")));
                            }
                        }, loanId);
            } catch (Exception ignored) {
                // optional enrichment
            }
            return balance;
        } catch (EmptyResultDataAccessException ex) {
            throw BranchApiException.notFound("LOAN_NOT_FOUND", "Crédito no encontrado: " + loanId);
        }
    }

    public ClientFileData getClientByLoanId(Long loanId) {
        String sql = """
                SELECT c.id, c.account_no, c.external_id, c.display_name, c.status_enum,
                       c.office_id, o.name AS office_name, c.mobile_no, c.email_address
                FROM m_client c
                JOIN m_loan l ON l.client_id = c.id
                LEFT JOIN m_office o ON o.id = c.office_id
                WHERE l.id = ?
                """;
        try {
            return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> mapClient(rs), loanId);
        } catch (EmptyResultDataAccessException ex) {
            throw BranchApiException.notFound("CLIENT_NOT_FOUND", "Cliente del crédito no encontrado");
        }
    }

    public RepaymentScheduleData getRepaymentSchedule(String identifier) {
        Long loanId = resolveLoanId(identifier);
        LoanBalanceData balance = getLoanBalanceById(loanId);

        List<RepaymentSchedulePeriodData> periods = jdbcTemplate.query("""
                SELECT installment, duedate, principal_amount, interest_amount,
                       fee_charges_amount, penalty_charges_amount,
                       principal_completed_derived, interest_completed_derived,
                       fee_charges_completed_derived, penalty_charges_completed_derived,
                       completed_derived, obligations_met_on_date
                FROM m_loan_repayment_schedule
                WHERE loan_id = ?
                ORDER BY installment
                """, (rs, rowNum) -> {
            BigDecimal principalDue = nullToZero(rs.getBigDecimal("principal_amount"));
            BigDecimal interestDue = nullToZero(rs.getBigDecimal("interest_amount"));
            BigDecimal feeDue = nullToZero(rs.getBigDecimal("fee_charges_amount"));
            BigDecimal penaltyDue = nullToZero(rs.getBigDecimal("penalty_charges_amount"));
            BigDecimal totalDue = principalDue.add(interestDue).add(feeDue).add(penaltyDue);
            BigDecimal principalPaid = nullToZero(rs.getBigDecimal("principal_completed_derived"));
            BigDecimal interestPaid = nullToZero(rs.getBigDecimal("interest_completed_derived"));
            BigDecimal feePaid = nullToZero(rs.getBigDecimal("fee_charges_completed_derived"));
            BigDecimal penaltyPaid = nullToZero(rs.getBigDecimal("penalty_charges_completed_derived"));
            BigDecimal totalPaid = principalPaid.add(interestPaid).add(feePaid).add(penaltyPaid);
            boolean complete = rs.getBoolean("completed_derived");
            LocalDate dueDate = rs.getDate("duedate").toLocalDate();
            boolean overdue = !complete && dueDate.isBefore(LocalDate.now());
            return RepaymentSchedulePeriodData.builder().period(rs.getInt("installment")).dueDate(dueDate)
                    .principalDue(principalDue).interestDue(interestDue).feeChargesDue(feeDue)
                    .penaltyChargesDue(penaltyDue).totalDue(totalDue).totalPaid(totalPaid)
                    .totalOutstanding(totalDue.subtract(totalPaid)).complete(complete).overdue(overdue).build();
        }, loanId);

        return RepaymentScheduleData.builder().loanId(loanId).accountNo(balance.getAccountNo()).currency("MXN")
                .totalPrincipalDisbursed(
                        balance.getPrincipal() != null ? balance.getPrincipal().getAmount() : BigDecimal.ZERO)
                .totalOutstanding(balance.getTotalOutstanding() != null ? balance.getTotalOutstanding().getAmount()
                        : BigDecimal.ZERO)
                .periods(periods).build();
    }

    private Long resolveLoanId(String identifier) {
        if (!StringUtils.hasText(identifier)) {
            throw BranchApiException.badRequest("INVALID_IDENTIFIER", "Identificador de crédito vacío");
        }
        // Try numeric id, account_no, external_id
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT id FROM m_loan WHERE CAST(id AS CHAR) = ? OR account_no = ? OR external_id = ? LIMIT 1",
                    Long.class, identifier, identifier, identifier);
        } catch (EmptyResultDataAccessException ex) {
            throw BranchApiException.notFound("LOAN_NOT_FOUND", "Crédito no encontrado: " + identifier);
        }
    }

    private ClientFileData mapClient(ResultSet rs) throws SQLException {
        return ClientFileData.builder().clientId(rs.getLong("id")).accountNo(rs.getString("account_no"))
                .curp(rs.getString("external_id")).displayName(rs.getString("display_name"))
                .status(mapClientStatus(rs.getInt("status_enum"))).officeId(rs.getLong("office_id"))
                .officeName(rs.getString("office_name")).mobileNo(rs.getString("mobile_no"))
                .emailAddress(rs.getString("email_address")).build();
    }

    private String mapClientStatus(int statusEnum) {
        return switch (statusEnum) {
            case 100 -> "PENDING";
            case 300 -> "ACTIVE";
            case 600 -> "CLOSED";
            case 700 -> "REJECTED";
            case 800 -> "WITHDRAWN";
            default -> String.valueOf(statusEnum);
        };
    }

    private String mapLoanStatus(int statusId) {
        return switch (statusId) {
            case 100 -> "SUBMITTED";
            case 200 -> "APPROVED";
            case 300 -> "ACTIVE";
            case 303, 304 -> "TRANSFER";
            case 400 -> "WITHDRAWN";
            case 500 -> "REJECTED";
            case 600 -> "CLOSED_OBLIGATIONS_MET";
            case 601 -> "CLOSED_WRITTEN_OFF";
            case 602 -> "CLOSED_RESCHEDULED";
            case 700 -> "OVERPAID";
            default -> String.valueOf(statusId);
        };
    }

    private BigDecimal nullToZero(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
