package com.bakouan.app.model;

import com.bakouan.app.enums.EFonctionEmploye;
import com.bakouan.app.enums.EHabilitationEtapeType;
import com.bakouan.app.utils.BaUtils;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "ba_etape_definition")
public class
BaEtapeDefinition extends BaAbstractAuditingEntity {

    @Id
    @Column(name = "id")
    private String id = BaUtils.randomUUID();

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private EHabilitationEtapeType type;

    @ManyToOne
    @JoinColumn(name = "departement_id")
    private BaDepartement departement;

    @ManyToOne
    @JoinColumn(name = "service_id")
    private BaService service;

    @Enumerated(EnumType.STRING)
    @Column(name = "fonction_requise")
    private EFonctionEmploye fonctionRequise;

    @Column(name = "libelle", length = 150)
    private String libelle;

    @Column(name = "role_code", length = 50)
    private String roleCode;
}
