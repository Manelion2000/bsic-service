package com.bakouan.app.model;

import com.bakouan.app.enums.EAbsenceStatut;
import com.bakouan.app.utils.BaUtils;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.ZonedDateTime;

@Getter
@Setter
@Entity
@Table(name = "ba_autorisation_absence")
public class BaAutorisationAbsence extends BaAbstractAuditingEntity {
    @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "circuit_id", unique = true) private BaRhCircuit circuit;
    @Id private String id = BaUtils.randomUUID();
    @Column(nullable = false, length = 1000) private String motif;
    @Column(name = "date_debut", nullable = false) private LocalDate dateDebut;
    @Column(name = "date_fin", nullable = false) private LocalDate dateFin;
    @Column(name = "nombre_jours", nullable = false) private int nombreJours;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "demandeur_id", nullable = false) private BaUser demandeur;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "validateur_id") private BaUser validateur;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private EAbsenceStatut statutValidation = EAbsenceStatut.EN_ATTENTE;
    @Column(name = "commentaire_validation", length = 1500) private String commentaireValidation;
    @Column(name = "date_validation") private ZonedDateTime dateValidation;
}
