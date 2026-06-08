package com.bakouan.app.dto;

import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;

@Data
public class BaAutorisationSortieSuiviDto {
    private String id;
    private String motif;
    private LocalDate dateSortie;
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
    private String typeStructure;
    private String structure;
    private String idValidateur;
    private String validateur;
}
