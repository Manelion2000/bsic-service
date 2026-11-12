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

@Entity @Getter @Setter @Table(name="ba_conge_passation")
public class BaCongePassation extends BaCongeEntity {
    @OneToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(unique=true) private BaCongeDemande demande;
    @ManyToOne(fetch=FetchType.LAZY) private BaUser remplacant;
    @Column(nullable=false,length=5000) private String tachesEtEcheances;
    @Column(length=1000) private String contactsUtiles;
    private Instant priseEnChargeLe;
}

