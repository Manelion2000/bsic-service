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

@Entity @Getter @Setter @Table(name="ba_conge_mouvement")
public class BaCongeMouvement extends BaCongeEntity {
    @ManyToOne(fetch=FetchType.LAZY,optional=false) private BaCongeDroit droit;
    @ManyToOne(fetch=FetchType.LAZY) private BaCongeDemande demande;
    @Column(nullable=false,length=30) private String nature;
    @Column(nullable=false,precision=18,scale=4) private BigDecimal quantite;
    @Column(nullable=false,length=200,unique=true) private String cle;
    @Column(nullable=false,length=1200) private String source;
    @Column(nullable=false,length=2500) private String justification;
    @Column(nullable=false) private String auteurId;
    @Column(nullable=false) private Instant dateOperation = Instant.now();
}
