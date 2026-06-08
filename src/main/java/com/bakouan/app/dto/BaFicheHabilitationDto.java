package com.bakouan.app.dto;

import lombok.Data;

import java.util.List;
import java.time.ZonedDateTime;
import com.bakouan.app.enums.EHabilitationFicheStatut;
import com.bakouan.app.enums.EFonctionEmploye;
import com.bakouan.app.enums.EPriorite;

@Data
public class BaFicheHabilitationDto {
    private String id;
    private String idEmploye;
    private String matriculeEmploye;
    private String nomCompletEmploye;
    private String idServiceEmploye;
    private String nomServiceEmploye;
    private String idDepartementEmploye;
    private String nomDepartementEmploye;
    private String idDirectionEmploye;
    private String nomDirectionEmploye;
    private String idAgenceEmploye;
    private String nomAgenceEmploye;
    private String idCircuit;
    private String nomCircuit;
    private String nomPlateforme;
    private List<String> idsPlateformes;
    private List<String> nomsPlateformes;
    private String motif;
    private EHabilitationFicheStatut statutHabilitation;
    private EPriorite priorite;
    private ZonedDateTime createdDate;
    private String justificatifId;
    private EFonctionEmploye fonctionEmploye;
    private String telephoneMobileEmploye;
    private List<BaFicheHabilitationEtapeDto> etapes;
}
