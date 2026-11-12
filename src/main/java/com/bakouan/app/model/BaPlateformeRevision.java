package com.bakouan.app.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;
import com.bakouan.app.dto.PlateformeDtos.Detail;

/** Snapshot append-only : aucune API de modification ou suppression. */
@Entity @Getter @Setter
public class BaPlateformeRevision {
    @Id private String id;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="plateforme_id",nullable=false) private BaPlateforme plateforme;
    @Column(nullable=false) private long numero;
    @Column(nullable=false) private Instant date;
    @Column(nullable=false,length=255) private String auteurId;
    @Column(nullable=false,length=255) private String auteur;
    @Column(nullable=false,length=30) private String action;
    @Column(nullable=false,length=1000) private String motif;
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(nullable=false,columnDefinition="jsonb") private Detail snapshot;
}
