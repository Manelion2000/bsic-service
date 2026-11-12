package com.bakouan.app.model;

import com.bakouan.app.utils.BaUtils;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Même stockage bytea protégé que les pièces des démarches ; aucun lien public. */
@Entity @Getter @Setter
public class BaPlateformeManuel extends BaAbstractAuditingEntity {
    @Id private String id=BaUtils.randomUUID();
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="plateforme_id",nullable=false) private BaPlateforme plateforme;
    @Column(nullable=false,length=150) private String titre;
    @Column(length=2000) private String description;
    @Column(nullable=false,length=150) private String nomFichier;
    @Column(nullable=false,length=100) private String contentType;
    @Column(nullable=false) private long taille;
    @Column(nullable=false,length=64) private String sha256;
    @Column(nullable=false,length=255) private String auteur;
    @JdbcTypeCode(SqlTypes.VARBINARY) @Column(nullable=false,columnDefinition="bytea") private byte[] contenu;
}
