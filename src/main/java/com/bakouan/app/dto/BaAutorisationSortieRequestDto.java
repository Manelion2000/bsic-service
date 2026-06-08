package com.bakouan.app.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class BaAutorisationSortieRequestDto {
    private String motif;
    private LocalDate dateSortie;
    private String typeStructure;
    private String idDirection;
    private String idService;
    private String idAgence;
}
