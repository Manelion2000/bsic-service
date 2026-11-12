package com.bakouan.app.model;

import com.bakouan.app.dto.ExtractionDtos.RequeteEcriture;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.UUID;

@Entity @Getter @Setter
public class BaRequeteRevision {
    @Id private String id=UUID.randomUUID().toString();
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="requete_id",nullable=false) private BaRequeteOracle requete;
    @Column(nullable=false) private int edition;
    @Column(nullable=false) private Instant date;
    @Column(nullable=false,length=255) private String auteurId;
    @Column(nullable=false,length=255) private String auteur;
    @Column(nullable=false,length=1000) private String motif;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable=false,columnDefinition="jsonb") private RequeteEcriture contenu;
}
