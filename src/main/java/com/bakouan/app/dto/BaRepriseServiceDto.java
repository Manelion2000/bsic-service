package com.bakouan.app.dto;

import com.bakouan.app.enums.ERepriseServiceStatut;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;

@Data
public class BaRepriseServiceDto {
    private String id;
    private String motifAbsence;
    private LocalDate dateDebutConge;
    private LocalDate dateFinConge;
    private LocalDate dateReprise;
    private ERepriseServiceStatut statutValidation;
    private String commentaireValidation;
    private ZonedDateTime dateValidation;
    private Instant createdDate;

    private String idDemandeur;
    private String matriculeDemandeur;
    private String nomCompletDemandeur;
    private String nomServiceDemandeur;
    private String nomDepartementDemandeur;

    private String idSignataire;
    private String nomCompletSignataire;
    private String nomDepartementSignataire;
    private String fonctionSignataire;
}
