package com.bakouan.app.dto;

import com.bakouan.app.enums.EHabilitationCompteStatut;
import com.bakouan.app.enums.ETypePlateforme;
import lombok.Data;

import java.time.ZonedDateTime;
import java.time.Instant;

@Data
public class BaHabilitationComptePlateformeDto {
    private String id;
    private String idFiche;
    private String idPlateforme;
    private String nomPlateforme;
    private ETypePlateforme typePlateforme;
    private String idDemandeur;
    private String matriculeDemandeur;
    private String nomCompletDemandeur;
    private String nomServiceDemandeur;
    private String nomDepartementDemandeur;
    private EHabilitationCompteStatut statutCreation;
    private ZonedDateTime dateDebutCreation;
    private ZonedDateTime dateCreation;
    private String idCreePar;
    private String nomCompletCreePar;
    private String commentaireCreation;
    private Instant createdDate;
}
