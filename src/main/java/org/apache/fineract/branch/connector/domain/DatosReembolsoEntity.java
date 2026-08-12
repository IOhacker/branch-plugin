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
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "DATOS_REEMBOLSOS", indexes = {
        @Index(name = "idx_DATOS_REEMBOLSOS_loan_id", columnList = "loan_id"),
        @Index(name = "idx_DATOS_REEMBOLSOS_referencia_rembolso", columnList = "referenciaRembolso") })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DatosReembolsoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "loan_id", nullable = false)
    private Long loanId;

    @Column(name = "referenciaRembolso", columnDefinition = "text")
    private String referenciaRembolso;

    @Column(name = "referenciaDeposito", columnDefinition = "text")
    private String referenciaDeposito;

    @Column(name = "modalidadID", columnDefinition = "text")
    private String modalidadID;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
