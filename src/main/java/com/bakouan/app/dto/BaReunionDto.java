package com.bakouan.app.dto;

import lombok.Data;

import java.time.Instant;

@Data
public class BaReunionDto {
    private String id;
    private String titre;
    private String objet;
    private Instant dateHeure;
    private String procesVerbalId;
    private Boolean valideParDirecteur;
    private String idDepartement;
    private String nomDepartement;
    private String idAgence;
    private String nomAgence;
    private String idService;
    private String nomService;
}
