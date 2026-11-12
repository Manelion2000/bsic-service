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

@Entity @Getter @Setter @Table(name="ba_conge_anomalie")
public class BaCongeAnomalie extends BaCongeEntity {
    @ManyToOne(fetch=FetchType.LAZY) private BaCongeDroit droit;
    @ManyToOne(fetch=FetchType.LAZY) private BaCongeDemande demande;
    @Column(nullable=false,length=200,unique=true) private String cle;
    @Column(nullable=false,length=1000) private String description;
    @Column(nullable=false) private boolean resolue;
    private String resoluePar;
}

