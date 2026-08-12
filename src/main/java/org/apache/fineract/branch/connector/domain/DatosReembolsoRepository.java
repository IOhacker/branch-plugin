/**
 * Copyright since 2026 Mifos Initiative
 *
 * <p>This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. If a copy
 * of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.apache.fineract.branch.connector.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DatosReembolsoRepository extends JpaRepository<DatosReembolsoEntity, Long> {

    Optional<DatosReembolsoEntity> findFirstByReferenciaRembolso(String referenciaRembolso);

    List<DatosReembolsoEntity> findByLoanId(Long loanId);

    @Query("SELECT d.loanId FROM DatosReembolsoEntity d WHERE d.referenciaRembolso = :ref")
    Optional<Long> findLoanIdByReferenciaRembolso(@Param("ref") String referenciaRembolso);

    boolean existsByReferenciaRembolso(String referenciaRembolso);
}
