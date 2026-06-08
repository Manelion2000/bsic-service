package com.bakouan.app.dto;

import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;

@Data
public class BaRepriseServiceSuiviDto {
    private String id;
    private String motifAbsence;
    private LocalDate dateDebutConge;
    private LocalDate dateFinConge;
    private LocalDate dateReprise;
    private long nombreJoursAbsence;
    private Instant createdDate;
    private String statutValidation;
    private String commentaireValidation;
    private ZonedDateTime dateValidation;
    private String idDemandeur;
    private String nomDemandeur;
    private String matriculeDemandeur;
    private String idDepartement;
    private String departement;
    private String idService;
    private String service;
    private String idSignataire;
    private String signataire;
}
