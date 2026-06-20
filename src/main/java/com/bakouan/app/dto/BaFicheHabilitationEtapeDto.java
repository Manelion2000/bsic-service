package com.bakouan.app.dto;

import com.bakouan.app.enums.EHabilitationStatut;
import com.bakouan.app.enums.EHabilitationEtapeType;
import com.bakouan.app.enums.EFonctionEmploye;
import lombok.Data;

@Data
public class BaFicheHabilitationEtapeDto {
    private String id;
    private String idFiche;
    private EHabilitationEtapeType typeEtape;
    private String idDepartement;
    private String nomDepartement;
    private String idService;
    private String nomService;
    private EFonctionEmploye fonctionRequise;
    private String idRole;
    private String roleLibelle;
    private String idValidateur;
    private String nomCompletValidateur;
    private String idDepartementValidateur;
    private String nomDepartementValidateur;
    private Integer ordre;
    private EHabilitationStatut statutValidation;
    private String commentaire;
    private java.time.ZonedDateTime dateValidation;
}
