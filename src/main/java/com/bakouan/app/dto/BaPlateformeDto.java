package com.bakouan.app.dto;

import com.bakouan.app.enums.ETypePlateforme;
import lombok.Data;

@Data
public class BaPlateformeDto {
    private String id;
    private String code;
    private String nom;
    private String description;
    private ETypePlateforme typePlateforme;
    private String idCircuit;
    private String libelleCircuit;
}
