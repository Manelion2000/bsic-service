package com.bakouan.app.dto;

import com.bakouan.app.enums.EHabilitationStatut;
import com.bakouan.app.enums.EReinitialisationCompteStatut;
import com.bakouan.app.enums.EReinitialisationTraitementStatut;
import com.bakouan.app.enums.ETypePlateforme;
import lombok.Data;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;

@Data
public class BaReinitialisationCompteDto {
    private String id;
    private String idDemandeur;
    private String nomCompletDemandeur;
    private String matriculeDemandeur;
    private String idPlateforme;
    private String nomPlateforme;
    private ETypePlateforme typePlateforme;
    private String idCircuit;
    private String nomCircuit;
    private String motif;
    private EReinitialisationCompteStatut statutDemande;
    private ZonedDateTime dateValidation;
    private Instant createdDate;
    private List<BaReinitialisationCompteEtapeDto> etapes;
    private String idTraitement;
    private EReinitialisationTraitementStatut statutTraitement;
    private ZonedDateTime dateDebutTraitement;
    private ZonedDateTime dateTraitement;
    private String nomCompletTraitePar;
    private String commentaireTraitement;
}
