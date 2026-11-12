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

@Entity @Getter @Setter @Table(name="ba_conge_affectation")
public class BaCongeAffectation extends BaCongeEntity {
    @ManyToOne(fetch=FetchType.LAZY,optional=false) private BaUser agent;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) private BaCongePolitique politique;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) private BaCongeCalendrier calendrier;
    @Column(nullable=false) private LocalDate debut;
    private LocalDate fin;
    private LocalDate dateEntree;
    private LocalDate dateSortie;
    @Column(nullable=false,length=1000) private String justification;
}

