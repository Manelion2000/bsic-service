package com.bakouan.app.dto;

import com.bakouan.app.enums.EDgaPole;
import lombok.Data;

import java.time.LocalDate;

@Data
public class BaDgaPoleValidateurDto {
    private String id;
    private EDgaPole pole;
    private String idDga;
    private String nomDga;
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private Boolean actif;
}
