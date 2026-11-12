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

@Entity @Getter @Setter @Table(name="ba_conge_echeance")
public class BaCongeEcheance extends BaCongeEntity {
    @ManyToOne(fetch=FetchType.LAZY,optional=false) private BaCongeDroit droit;
    @Column(nullable=false) private LocalDate dateAcquisition;
    @Column(nullable=false,precision=18,scale=4) private BigDecimal quantite;
    @Column(nullable=false,length=200,unique=true) private String cle;
    @Column(nullable=false) private boolean executee;
}

