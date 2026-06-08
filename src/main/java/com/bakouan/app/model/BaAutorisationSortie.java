package com.bakouan.app.model;

import com.bakouan.app.enums.EAutorisationSortieStatut;
import com.bakouan.app.utils.BaUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.ZonedDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "ba_autorisation_sortie")
public class BaAutorisationSortie extends BaAbstractAuditingEntity {

    @Id
    @Column(name = "id")
    private String id = BaUtils.randomUUID();

    @Column(name = "motif", nullable = false, length = 1000)
    private String motif;

    @Column(name = "date_sortie", nullable = false)
    private LocalDate dateSortie;

    @Column(name = "type_structure", length = 20)
    private String typeStructure;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "direction_id")
    private BaDepartement direction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id")
    private BaService service;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agence_id")
    private BaAgence agence;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "demandeur_id")
    private BaUser demandeur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validateur_id")
    private BaUser validateur;

    @Enumerated(EnumType.STRING)
    @Column(name = "statut_validation", length = 20, nullable = false)
    private EAutorisationSortieStatut statutValidation = EAutorisationSortieStatut.EN_ATTENTE;

    @Column(name = "commentaire_validation", length = 1500)
    private String commentaireValidation;

    @Column(name = "date_validation")
    private ZonedDateTime dateValidation;
}
