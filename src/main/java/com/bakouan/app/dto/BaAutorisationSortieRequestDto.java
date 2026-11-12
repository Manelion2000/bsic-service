package com.bakouan.app.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

@Data
public class BaAutorisationSortieRequestDto {
    @NotBlank(message = "Le motif est obligatoire.")
    private String motif;
    @NotNull(message = "La date de sortie est obligatoire.")
    private LocalDate dateSortie;
    @NotBlank(message = "Le type de structure est obligatoire.")
    private String typeStructure;
    private String idDirection;
    private String idService;
    private String idAgence;
}
