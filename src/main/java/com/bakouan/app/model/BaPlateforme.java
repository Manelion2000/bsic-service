package com.bakouan.app.model;

import com.bakouan.app.enums.ETypePlateforme;
import com.bakouan.app.utils.BaUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BaPlateforme extends BaAbstractAuditingEntity {

    @Id
    @Column(name = "id")
    private String id = BaUtils.randomUUID();

    @Column(name = "code", unique = true)
    private String code;

    @Column(name = "nom")
    private String nom;

    @Column(name = "description", length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_plateforme", length = 30)
    private ETypePlateforme typePlateforme = ETypePlateforme.RESEAUX;

    @ManyToOne
    @JoinColumn(name = "circuit_id")
    private BaCircuit circuit;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name="fiche", columnDefinition="jsonb", nullable=false)
    private PlateformeFiche fiche = PlateformeFiche.vide();

    @ManyToOne(fetch=jakarta.persistence.FetchType.LAZY)
    @JoinColumn(name="departement_proprietaire_id")
    private BaDepartement departementProprietaire;

    @ManyToOne(fetch=jakarta.persistence.FetchType.LAZY)
    @JoinColumn(name="responsable_metier_id")
    private BaUser responsableMetier;

    @ManyToOne(fetch=jakarta.persistence.FetchType.LAZY)
    @JoinColumn(name="responsable_technique_id")
    private BaUser responsableTechnique;

    @jakarta.persistence.ManyToMany
    @jakarta.persistence.JoinTable(name="ba_plateforme_dependance",
        joinColumns=@JoinColumn(name="plateforme_id"), inverseJoinColumns=@JoinColumn(name="dependance_id"))
    private java.util.Set<BaPlateforme> dependances = new java.util.LinkedHashSet<>();
}
