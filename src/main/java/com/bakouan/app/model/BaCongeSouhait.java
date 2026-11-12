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

@Entity @Getter @Setter @Table(name="ba_conge_souhait")
public class BaCongeSouhait extends BaCongeEntity {
    @ManyToOne(fetch=FetchType.LAZY,optional=false) private BaUser agent;
    @Column(nullable=false) private LocalDate debut;
    @Column(nullable=false) private LocalDate fin;
}

