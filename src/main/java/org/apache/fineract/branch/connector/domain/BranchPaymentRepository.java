/**
 * Copyright since 2026 Mifos Initiative
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.apache.fineract.branch.connector.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BranchPaymentRepository extends JpaRepository<BranchPaymentEntity, Long> {

    Optional<BranchPaymentEntity> findByExternalId(String externalId);

    boolean existsByExternalId(String externalId);

    @Query("SELECT p FROM BranchPaymentEntity p WHERE p.branchId = :branchId "
            + "AND p.transactionDate >= :fromDate AND p.transactionDate <= :toDate "
            + "AND (:status IS NULL OR :status = 'ALL' OR p.status = :status) "
            + "ORDER BY p.transactionDate, p.registeredAt")
    List<BranchPaymentEntity> findForReconciliation(@Param("branchId") String branchId,
            @Param("fromDate") LocalDate fromDate, @Param("toDate") LocalDate toDate,
            @Param("status") String status);
}
