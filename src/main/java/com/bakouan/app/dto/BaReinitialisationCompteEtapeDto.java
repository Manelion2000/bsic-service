package com.bakouan.app.dto;

import com.bakouan.app.enums.EHabilitationStatut;
import lombok.Data;

import java.time.ZonedDateTime;

@Data
public class BaReinitialisationCompteEtapeDto {
    private String id;
    private String idDemande;
    private String nomEtape;
    private String idValidateur;
    private String nomCompletValidateur;
    private Integer ordre;
    private EHabilitationStatut statutValidation;
    private String commentaire;
    private ZonedDateTime dateValidation;
}
