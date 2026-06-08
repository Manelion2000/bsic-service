package com.bakouan.app.dto;

import com.bakouan.app.enums.EHabilitationCompteStatut;
import com.bakouan.app.enums.EReinitialisationCompteStatut;
import com.bakouan.app.enums.EReinitialisationTraitementStatut;
import com.bakouan.app.enums.ETypePlateforme;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class BaPlateformeStatFilterDto {
    private String idPlateforme;
    private ETypePlateforme typePlateforme;
    private String idDepartement;
    private String idService;
    private String idDemandeur;
    private String idCreateurCompte;
    private String idTraitePar;
    private EHabilitationCompteStatut statutCreation;
    private EReinitialisationCompteStatut statutReinitialisation;
    private EReinitialisationTraitementStatut statutTraitement;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateDebut;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateFin;
}
