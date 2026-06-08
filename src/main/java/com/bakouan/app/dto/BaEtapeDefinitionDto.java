package com.bakouan.app.dto;

import com.bakouan.app.enums.EFonctionEmploye;
import com.bakouan.app.enums.EHabilitationEtapeType;
import lombok.Data;

@Data
public class BaEtapeDefinitionDto {
    private String id;
    private EHabilitationEtapeType type;
    private String idDepartement;
    private String nomDepartement;
    private String idService;
    private String nomService;
    private EFonctionEmploye fonctionRequise;
    private String roleCode;
    private String libelle;
}
