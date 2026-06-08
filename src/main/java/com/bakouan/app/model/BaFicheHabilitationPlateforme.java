package com.bakouan.app.model;

import com.bakouan.app.utils.BaUtils;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "ba_fiche_habilitation_plateforme")
public class BaFicheHabilitationPlateforme extends BaAbstractAuditingEntity {
    @Id
    @Column(name = "id")
    private String id = BaUtils.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fiche_id", nullable = false)
    private BaFicheHabilitation fiche;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plateforme_id", nullable = false)
    private BaPlateforme plateforme;
}
