package com.bakouan.app.model;

import com.bakouan.app.enums.EReinitialisationTraitementStatut;
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
@Table(name = "ba_reinitialisation_compte_traitement")
public class BaReinitialisationCompteTraitement extends BaAbstractAuditingEntity {
    @Id
    @Column(name = "id")
    private String id = BaUtils.randomUUID();

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "demande_id", nullable = false, unique = true)
    private BaReinitialisationCompte demande;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_traitement", nullable = false, length = 30)
    private EReinitialisationTraitementStatut statutTraitement = EReinitialisationTraitementStatut.A_TRAITER;

    @Column(name = "date_debut_traitement")
    private ZonedDateTime dateDebutTraitement;

    @Column(name = "date_traitement")
    private ZonedDateTime dateTraitement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "traite_par_id")
    private BaUser traitePar;

    @Column(name = "commentaire_traitement", length = 1500)
    private String commentaireTraitement;
}
