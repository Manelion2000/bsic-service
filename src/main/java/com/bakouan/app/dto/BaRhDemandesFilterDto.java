package com.bakouan.app.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Data
public class BaRhDemandesFilterDto {
    private String idDemandeur;
    private String idDepartement;
    private String idService;
    private String idValidateur;
    private String statutValidation;
    private String typeStructure;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateDebut;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateFin;
}
