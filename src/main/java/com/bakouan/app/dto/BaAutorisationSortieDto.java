package com.bakouan.app.dto;

import com.bakouan.app.enums.EAutorisationSortieStatut;
import lombok.Data;

import java.time.LocalDate;
import java.time.ZonedDateTime;

@Data
public class BaAutorisationSortieDto {
    private String id;
    private String motif;
    private LocalDate dateSortie;
    private EAutorisationSortieStatut statutValidation;
    private String commentaireValidation;
    private ZonedDateTime dateValidation;

    private String idDemandeur;
    private String nomCompletDemandeur;
    private String idValidateur;
    private String nomCompletValidateur;
    private String typeStructure;
    private String idDirection;
    private String nomDirection;
    private String idService;
    private String nomService;
    private String idAgence;
    private String nomAgence;
}
