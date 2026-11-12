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

@Entity @Getter @Setter @Table(name="ba_conge_calendrier")
public class BaCongeCalendrier extends BaCongeEntity {
    @Column(nullable=false,length=150) private String nom;
    @Column(nullable=false) private LocalDate debutEffet;
    private LocalDate finEffet;
    @Column(nullable=false,length=1000) private String sourceRh;
    @Column(nullable=false) private boolean confirme;
    private String confirmePar;
    private Instant confirmeLe;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable=false,columnDefinition="jsonb") private CongeDefinitions.Calendrier regles;
}

