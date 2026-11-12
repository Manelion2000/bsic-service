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

@Entity @Getter @Setter @Table(name="ba_conge_droit")
public class BaCongeDroit extends BaCongeEntity {
    @ManyToOne(fetch=FetchType.LAZY,optional=false) private BaUser agent;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) private BaCongePolitique politique;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) private BaCongeAffectation affectation;
    @Column(nullable=false,length=50) private String typeCode;
    @Column(nullable=false) private LocalDate origineDebut;
    @Column(nullable=false) private LocalDate origineFin;
    @Column(nullable=false) private LocalDate debutDisponibilite;
    @Column(nullable=false) private LocalDate finDisponibilite;
    private LocalDate expiration;
    private String origineDroitId;
    @Column(nullable=false) private boolean cloturee;
    @Column(nullable=false,length=180,unique=true) private String cleOrigine;
    @Column(nullable=false,length=1200) private String source;
    @Column(nullable=false,length=1000) private String justification;
}
