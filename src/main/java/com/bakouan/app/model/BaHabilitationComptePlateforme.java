package com.bakouan.app.model;

import com.bakouan.app.enums.EHabilitationCompteStatut;
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
@Table(name = "ba_habilitation_compte_plateforme")
public class BaHabilitationComptePlateforme extends BaAbstractAuditingEntity {
    @Id
    @Column(name = "id")
    private String id = BaUtils.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fiche_id", nullable = false)
    private BaFicheHabilitation fiche;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plateforme_id", nullable = false)
    private BaPlateforme plateforme;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "demandeur_id", nullable = false)
    private BaUser demandeur;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_creation", nullable = false, length = 30)
    private EHabilitationCompteStatut statutCreation = EHabilitationCompteStatut.A_CREER;

    @Column(name = "date_debut_creation")
    private ZonedDateTime dateDebutCreation;

    @Column(name = "date_creation")
    private ZonedDateTime dateCreation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cree_par_id")
    private BaUser creePar;

    @Column(name = "commentaire_creation", length = 1500)
    private String commentaireCreation;
}
