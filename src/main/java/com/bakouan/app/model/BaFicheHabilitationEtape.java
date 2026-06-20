package com.bakouan.app.model;

import com.bakouan.app.enums.EHabilitationStatut;
import com.bakouan.app.utils.BaUtils;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "ba_fiche_habilitation_etape")
public class BaFicheHabilitationEtape extends BaAbstractAuditingEntity {

    @Id
    @Column(name = "id")
    private String id = BaUtils.randomUUID();

    @ManyToOne
    @JoinColumn(name = "fiche_id")
    private BaFicheHabilitation fiche;

    @ManyToOne
    @JoinColumn(name = "validateur_id")
    private BaUser validateur;

    @Column(name = "ordre")
    private Integer ordre;

    @Column(name = "statut_validation")
    @Enumerated(EnumType.STRING)
    private EHabilitationStatut statutValidation = EHabilitationStatut.EN_ATTENTE;

    @Column(name = "commentaire", length = 1500)
    private String commentaire;

    @Column(name = "date_validation")
    private java.time.ZonedDateTime dateValidation;
}
