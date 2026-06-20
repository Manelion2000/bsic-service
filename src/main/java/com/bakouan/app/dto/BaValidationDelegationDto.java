package com.bakouan.app.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class BaValidationDelegationDto {
    private String id;
    private String idDelegant;
    private String nomDelegant;
    private String idDelegue;
    private String nomDelegue;
    private String roleCode;
    private String idDepartement;
    private String nomDepartement;
    private String idService;
    private String nomService;
    private String idAgence;
    private String nomAgence;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private Boolean actif;
    private String motif;
}
