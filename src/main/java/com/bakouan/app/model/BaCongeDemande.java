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

@Entity @Getter @Setter @Table(name="ba_conge_demande")
public class BaCongeDemande extends BaCongeEntity {
    @ManyToOne(fetch=FetchType.LAZY,optional=false) private BaUser agent;
    @Column(nullable=false,length=50) private String typeCode;
    @Column(nullable=false) private int revision = 1;
    @Column(nullable=false,length=20) private String etat = "BROUILLON";
    @Column(nullable=false) private LocalDate debut;
    @Column(nullable=false) private LocalDate fin;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private CongeDefinitions.Portion portionDebut;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private CongeDefinitions.Portion portionFin;
    private LocalTime heureDebut;
    private LocalTime heureFin;
    @Column(length=1000) private String motif;
    @ManyToOne(fetch=FetchType.LAZY) private BaCongeDemande remplaceDemande;
    private LocalDate repriseAnticipee;
    private LocalDate repriseConstatee;
    private LocalDate repriseDemandee;
    @Enumerated(EnumType.STRING) @Column(length=20) private CongeDefinitions.Portion portionRepriseDemandee;
    private LocalTime heureRepriseDemandee;
    @Column(length=20) private String retourStatut;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable=false,columnDefinition="jsonb") private List<CongeDefinitions.Imputation> restitutions = new ArrayList<>();
    @ManyToOne(fetch=FetchType.LAZY) private BaCongePolitique politique;
    @ManyToOne(fetch=FetchType.LAZY) private BaCongeCalendrier calendrier;
    @OneToOne(fetch=FetchType.LAZY) @JoinColumn(unique=true) private BaRhCircuit circuit;
    @JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition="jsonb") private CongeDefinitions.Calcul calcul;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable=false,columnDefinition="jsonb") private List<CongeDefinitions.Imputation> imputations = new ArrayList<>();
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable=false,columnDefinition="jsonb") private List<CongeDefinitions.Imputation> reservations = new ArrayList<>();
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable=false,columnDefinition="jsonb") private List<CongeDefinitions.ActionAudit> historique = new ArrayList<>();
    private Instant soumiseLe;
    private Instant approuveeLe;
    @Column(length=1500) private String derogationCouverture;
    private String derogationPar;
}
