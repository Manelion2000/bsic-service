package com.bakouan.app.model;

import com.bakouan.app.enums.EDgaPole;
import com.bakouan.app.utils.BaUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "ba_dga_pole_validateur")
public class BaDgaPoleValidateur extends BaAbstractAuditingEntity {
    @Id
    @Column(name = "id")
    private String id = BaUtils.randomUUID();

    @Enumerated(EnumType.STRING)
    @Column(name = "pole", nullable = false, length = 30)
    private EDgaPole pole;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dga_id", nullable = false)
    private BaUser dga;

    @Column(name = "date_debut", nullable = false)
    private LocalDate dateDebut;

    @Column(name = "date_fin")
    private LocalDate dateFin;

    @Column(name = "actif", nullable = false)
    private Boolean actif = Boolean.TRUE;
}
