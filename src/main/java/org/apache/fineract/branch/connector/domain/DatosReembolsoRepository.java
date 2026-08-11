/**
 * Copyright 2026 Mifos Initiative
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
