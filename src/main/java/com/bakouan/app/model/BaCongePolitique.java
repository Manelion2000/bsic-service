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

@Entity @Getter @Setter @Table(name="ba_conge_politique")
public class BaCongePolitique extends BaCongeEntity {
    @Column(nullable=false,length=50) private String typeCode;
    @Column(nullable=false,length=150) private String libelle;
    @Column(nullable=false,length=30) private String categorie;
    @Column(nullable=false) private int versionPolitique;
    @Column(nullable=false,length=20) private String etat = "BROUILLON";
    @Column(nullable=false) private LocalDate debutEffet;
    private LocalDate finEffet;
    @Column(nullable=false,length=1000) private String sourceRh;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable=false,columnDefinition="jsonb") private CongeDefinitions.Politique regles;
    private String confirmePar;
    private Instant confirmeLe;
}

