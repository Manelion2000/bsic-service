package com.bakouan.app.model;

import com.bakouan.app.dto.ExtractionDtos.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import java.util.UUID;

@Entity @Getter @Setter
public class BaExtractionExecution {
    @Id private String id=UUID.randomUUID().toString();
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="revision_id",nullable=false) private BaRequeteRevision revision;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="demande_id") private BaExtractionDemande demande;
    @Column(nullable=false) private Instant executeLe;
    @Column(nullable=false) private Instant enregistreLe;
    @Column(nullable=false,length=255) private String auteurId;
    @Column(nullable=false,length=255) private String auteur;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private Environnement environnement;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private Resultat resultat;
    @Column(nullable=false,length=500) private String referenceVerification;
    private Long lignes;
    @Column(length=1000) private String commentaire;
}
