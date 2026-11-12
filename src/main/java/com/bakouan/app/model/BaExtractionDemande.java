package com.bakouan.app.model;

import com.bakouan.app.dto.ExtractionDtos.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.*;
import java.util.*;

@Entity @Getter @Setter
public class BaExtractionDemande extends BaAbstractAuditingEntity {
    @Id private String id=UUID.randomUUID().toString();
    @Column(nullable=false,length=30,unique=true) private String reference;
    @Column(nullable=false,length=200) private String titre;
    @Column(nullable=false,length=4000) private String objectif;
    @Column(nullable=false) private LocalDate dateDemande;
    private LocalDate echeance;
    private LocalDate periodeDebut;
    private LocalDate periodeFin;
    @Column(length=4000) private String criteres;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="demandeur_id",nullable=false) private BaUser demandeur;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="departement_id",nullable=false) private BaDepartement departement;
    @Column(nullable=false,length=255) private String departementNom;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="service_id") private BaService service;
    @Column(length=255) private String serviceNom;
    @Column(nullable=false,length=255) private String demandeurNom;
    @Column(nullable=false,length=255) private String auteurId;
    @Column(nullable=false,length=255) private String auteur;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private Etat etat=Etat.NOUVELLE;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="charge_de_id") private BaUser chargeDe;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="revision_id") private BaRequeteRevision revision;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable=false,columnDefinition="jsonb") private List<Evenement> historique=new ArrayList<>();
}
