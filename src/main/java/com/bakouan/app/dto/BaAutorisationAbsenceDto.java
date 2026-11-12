package com.bakouan.app.dto;

import com.bakouan.app.enums.EAbsenceStatut;
import lombok.Data;
import java.time.LocalDate;
import java.time.ZonedDateTime;

@Data
public class BaAutorisationAbsenceDto {
    private RhCircuitDto circuit;
    private String id;
    private String motif;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private int nombreJours;
    private EAbsenceStatut statutValidation;
    private String commentaireValidation;
    private ZonedDateTime dateValidation;
    private String idDemandeur;
    private String nomCompletDemandeur;
    private String matriculeDemandeur;
    private String departement;
    private String service;
    private String idValidateur;
    private String nomCompletValidateur;
}
