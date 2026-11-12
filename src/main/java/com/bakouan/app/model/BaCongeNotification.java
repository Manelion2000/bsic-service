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

@Entity @Getter @Setter @Table(name="ba_conge_notification")
public class BaCongeNotification extends BaCongeEntity {
    @ManyToOne(fetch=FetchType.LAZY,optional=false) private BaCongeDemande demande;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) private BaUser destinataire;
    @Column(nullable=false,length=200,unique=true) private String cle;
    @Column(nullable=false,length=50) private String evenement;
    @Column(nullable=false) private boolean envoyee;
    @Column(nullable=false) private int essais;
    private Instant prochainEssai;
    private Instant lueLe;
}

