package com.bakouan.app.dto;

import com.bakouan.app.enums.EDgaPole;
import lombok.Data;

@Data
public class BaDepartementDto {
    private String id;
    private String code;
    private String nom;
    private EDgaPole dgaPole;
    private String idParentDepartement;
    private String nomParentDepartement;
    private String idDgaValidateur;
    private String nomDgaValidateur;
    private String idDgaEffectif;
    private String nomDgaEffectif;
}
