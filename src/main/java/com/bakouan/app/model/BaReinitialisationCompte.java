package com.bakouan.app.model;

import com.bakouan.app.enums.EReinitialisationCompteStatut;
import com.bakouan.app.utils.BaUtils;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.ZonedDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "ba_reinitialisation_compte")
public class BaReinitialisationCompte extends BaAbstractAuditingEntity {
    @Id
    @Column(name = "id")
    private String id = BaUtils.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "demandeur_id", nullable = false)
    private BaUser demandeur;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plateforme_id", nullable = false)
    private BaPlateforme plateforme;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "circuit_id", nullable = false)
    private BaCircuit circuit;

    @Column(name = "motif", nullable = false, length = 1500)
    private String motif;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_demande", nullable = false, length = 20)
    private EReinitialisationCompteStatut statutDemande = EReinitialisationCompteStatut.EN_COURS;

    @Column(name = "date_validation")
    private ZonedDateTime dateValidation;
}
