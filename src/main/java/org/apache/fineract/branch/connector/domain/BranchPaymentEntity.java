/**
 * Copyright since 2026 Mifos Initiative
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.apache.fineract.branch.connector.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "m_branch_connector_payment", uniqueConstraints = {
        @UniqueConstraint(name = "uk_branch_external_id", columnNames = "external_id") }, indexes = {
                @Index(name = "idx_branch_connector_date", columnList = "branch_id,transaction_date"),
                @Index(name = "idx_branch_refund_ref", columnList = "refund_reference"),
                @Index(name = "idx_branch_status", columnList = "status") })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchPaymentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", nullable = false, length = 100)
    private String externalId;

    @Column(name = "resource_id")
    private Long resourceId;

    @Column(name = "loan_id")
    private Long loanId;

    @Column(name = "client_id")
    private Long clientId;

    @Column(name = "refund_reference", nullable = false, length = 128)
    private String refundReference;

    @Column(name = "deposit_reference", length = 128)
    private String depositReference;

    @Column(name = "amount", nullable = false, precision = 19, scale = 6)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "transaction_date", nullable = false)
    private LocalDate transactionDate;

    @Column(name = "branch_id", nullable = false, length = 30)
    private String branchId;

    @Column(name = "operator_id", nullable = false, length = 50)
    private String operatorId;

    @Column(name = "terminal_id", nullable = false, length = 30)
    private String terminalId;

    /** RECEIVED | VALIDATING | APPLIED | REJECTED | UNKNOWN | REVERSED */
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "receipt_no", length = 64)
    private String receiptNo;

    @Column(name = "balance_after", precision = 19, scale = 6)
    private BigDecimal balanceAfter;

    @Column(name = "message", length = 500)
    private String message;

    @Column(name = "note", length = 500)
    private String note;

    @Column(name = "registered_at", nullable = false)
    private OffsetDateTime registeredAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "trace_id", length = 64)
    private String traceId;
}
