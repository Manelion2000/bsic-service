package com.bakouan.app.model;

import com.bakouan.app.enums.ERepriseServiceStatut;
import com.bakouan.app.utils.BaUtils;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.ZonedDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "ba_reprise_service")
public class BaRepriseService extends BaAbstractAuditingEntity{
    @Id
    @Column(name = "id")
    private String id= BaUtils.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "demandeur_id", nullable = false)
    private BaUser demandeur;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "signataire_id", nullable = false)
    private BaUser signataire;

    @Column(name = "motif_absence", nullable = false, length = 1000)
    private String motifAbsence;

    @Column(name = "date_debut_conge", nullable = false)
    private LocalDate dateDebutConge;

    @Column(name = "date_fin_conge", nullable = false)
    private LocalDate dateFinConge;

    @Column(name="date_reprise", nullable = false)
    private LocalDate dateReprise;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_validation", nullable = false, length = 20)
    private ERepriseServiceStatut statutValidation = ERepriseServiceStatut.EN_ATTENTE;

    @Column(name = "commentaire_validation", length = 1500)
    private String commentaireValidation;

    @Column(name = "date_validation")
    private ZonedDateTime dateValidation;
}
