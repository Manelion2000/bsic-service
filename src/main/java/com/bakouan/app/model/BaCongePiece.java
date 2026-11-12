package com.bakouan.app.model;

import com.bakouan.app.dto.CongeDefinitions;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity @Getter @Setter @Table(name="ba_conge_piece")
public class BaCongePiece extends BaCongeEntity {
    @ManyToOne(fetch=FetchType.LAZY,optional=false) private BaCongeDemande demande;
    @Column(nullable=false,length=150) private String nomFichier;
    @Column(nullable=false,length=100) private String contentType;
    @Column(nullable=false) private long taille;
    @JdbcTypeCode(SqlTypes.VARBINARY) @Column(nullable=false,columnDefinition="bytea") private byte[] contenu;
}

