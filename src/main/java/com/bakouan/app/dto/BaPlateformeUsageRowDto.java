package com.bakouan.app.dto;

import com.bakouan.app.enums.EHabilitationCompteStatut;
import com.bakouan.app.enums.EReinitialisationCompteStatut;
import com.bakouan.app.enums.EReinitialisationTraitementStatut;
import com.bakouan.app.enums.ETypePlateforme;
import lombok.Data;

import java.time.Instant;
import java.time.ZonedDateTime;

@Data
public class BaPlateformeUsageRowDto {
    private String idCompte;
    private String idDemandeur;
    private String nomDemandeur;
    private String matriculeDemandeur;
    private String idDepartement;
    private String departement;
    private String idService;
    private String service;
    private String idPlateforme;
    private String plateforme;
    private ETypePlateforme typePlateforme;
    private EHabilitationCompteStatut statutCreation;
    private Instant dateDemandeCompte;
    private ZonedDateTime dateCreationCompte;
    private String idCreePar;
    private String creePar;
    private long nombreReinitialisations;
    private Instant derniereReinitialisation;
    private EReinitialisationCompteStatut dernierStatutReinitialisation;
    private EReinitialisationTraitementStatut dernierStatutTraitement;
    private String idDernierTraitePar;
    private String dernierTraitePar;
}
