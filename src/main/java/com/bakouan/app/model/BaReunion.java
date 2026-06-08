package com.bakouan.app.model;

import com.bakouan.app.utils.BaUtils;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BaReunion extends BaAbstractAuditingEntity {
    @Id
    @Column(name = "id")
    private String id = BaUtils.randomUUID();

    @Column(name = "titre")
    private String titre;

    @Column(name = "objet", length = 1500)
    private String objet;

    @Column(name = "date_heure")
    private Instant dateHeure;

    @Column(name = "proces_verbal_id")
    private String procesVerbalId;

    @Column(name = "valide_par_directeur")
    private Boolean valideParDirecteur = Boolean.FALSE;

    @OneToOne(mappedBy = "reunion")
    private BaDocumentReunion document;

    @ManyToOne
    @JoinColumn(name = "departement_id")
    private BaDepartement departement;

    @ManyToOne
    @JoinColumn(name = "agence_id")
    private BaAgence agence;

    @ManyToOne
    @JoinColumn(name = "service_id")
    private BaService service;






}
