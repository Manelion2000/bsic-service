package com.bakouan.app.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class BaRepriseServiceRequestDto {
    private String motifAbsence;
    private LocalDate dateDebutConge;
    private LocalDate dateFinConge;
    private LocalDate dateReprise;
}
