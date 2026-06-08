package com.bakouan.app.model;

import com.bakouan.app.enums.EHabilitationEtapeType;
import com.bakouan.app.utils.BaUtils;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "circuit_etape")
public class BaCircuitEtape extends BaAbstractAuditingEntity {
    @Id
    private String id = BaUtils.randomUUID();

    @Column(name = "ordre", nullable = false)
    private Integer ordre;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private EHabilitationEtapeType type = EHabilitationEtapeType.DEPARTEMENT;

    @ManyToOne(optional = false)
    @JoinColumn(name = "circuit_id", nullable = false)
    private BaCircuit circuit;

    @ManyToOne
    @JoinColumn(name = "departement_id")
    private BaDepartement departement;

    @ManyToOne
    @JoinColumn(name = "service_id")
    private BaService service;

    @ManyToOne
    @JoinColumn(name = "role_id")
    private BaRole role;

    @Column(name = "fonction_requise", length = 100)
    private String fonctionRequise;

    @Column(name = "obligatoire")
    private Boolean obligatoire = true;

    @Column(name = "actif")
    private Boolean actif = true;

    @Column(name = "delai_validation_jours")
    private Integer delaiValidationJours;

    @Column(name = "conditions", length = 1500)
    private String conditions;
}
