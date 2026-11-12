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

@Entity @Getter @Setter @Table(name="ba_conge_operation")
public class BaCongeOperation extends BaCongeEntity {
    @Column(nullable=false,length=200,unique=true) private String cle;
    @Column(nullable=false) private String auteurId;
    @Column(nullable=false,length=50) private String action;
    private String demandeId;
    @Column(nullable=false,length=64) private String empreinte;
}

