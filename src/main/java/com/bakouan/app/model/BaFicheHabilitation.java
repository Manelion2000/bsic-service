package com.bakouan.app.model;

import com.bakouan.app.utils.BaUtils;
import com.bakouan.app.enums.EHabilitationFicheStatut;
import com.bakouan.app.enums.EFonctionEmploye;
import com.bakouan.app.enums.EPriorite;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BaFicheHabilitation extends BaAbstractAuditingEntity {

    @Id
    @Column(name = "id")
    private String id = BaUtils.randomUUID();

    @ManyToOne
    @JoinColumn(name = "employe_id")
    private BaUser employe;

    @ManyToOne
    @JoinColumn(name = "circuit_id")
    private BaCircuit circuit;

    @Column(name = "motif", length = 1500)
    private String motif;

    @Column(name = "statut_habilitation")
    @Enumerated(EnumType.STRING)
    private EHabilitationFicheStatut statutHabilitation = EHabilitationFicheStatut.EN_COURS;

    @Column(name = "priorite")
    @Enumerated(EnumType.STRING)
    private EPriorite priorite = EPriorite.NORMALE;

    @Column(name = "justificatif_id")
    private String justificatifId;

    @Enumerated(EnumType.STRING)
    @Column(name = "fonction_employe", length = 100)
    private EFonctionEmploye fonctionEmploye;

    @Column(name = "telephone_mobile_employe", length = 30)
    private String telephoneMobileEmploye;
}
