package com.bakouan.app.model;

import com.bakouan.app.enums.ECircuitStatut;
import com.bakouan.app.utils.BaUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BaCircuit extends BaAbstractAuditingEntity {

    @Id
    @Column(name = "id")
    private String id = BaUtils.randomUUID();

    @Column(name = "code", length = 50, unique = true)
    private String code;

    @Column(name = "libelle", length = 150)
    private String libelle;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "actif")
    private Boolean actif = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "statu")
    private ECircuitStatut statu = ECircuitStatut.EN_CONFIGURATION;

    @ManyToOne
    @JoinColumn(name = "plateforme_id")
    private BaPlateforme plateforme;

    @OneToMany(mappedBy = "circuit")
    private List<BaCircuitEtape> etapes = new ArrayList<>();
}
