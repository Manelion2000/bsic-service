package com.bakouan.app.model;

import com.bakouan.app.enums.EHabilitationStatut;
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
@Table(name = "ba_reinitialisation_compte_etape")
public class BaReinitialisationCompteEtape extends BaAbstractAuditingEntity {
    @Id
    @Column(name = "id")
    private String id = BaUtils.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "demande_id", nullable = false)
    private BaReinitialisationCompte demande;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validateur_id")
    private BaUser validateur;

    @Column(name = "ordre")
    private Integer ordre;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_validation", nullable = false, length = 20)
    private EHabilitationStatut statutValidation = EHabilitationStatut.EN_ATTENTE;

    @Column(name = "commentaire", length = 1500)
    private String commentaire;

    @Column(name = "date_validation")
    private ZonedDateTime dateValidation;
}
