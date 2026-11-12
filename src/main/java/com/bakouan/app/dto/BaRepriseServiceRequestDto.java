package com.bakouan.app.dto;

import lombok.Data;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

@Data
public class BaRepriseServiceRequestDto {
    @NotBlank(message = "Le motif d'absence est obligatoire.")
    private String motifAbsence;
    @NotNull(message = "La date de debut de conge est obligatoire.")
    private LocalDate dateDebutConge;
    @NotNull(message = "La date de fin de conge est obligatoire.")
    private LocalDate dateFinConge;
    @NotNull(message = "La date de reprise est obligatoire.")
    private LocalDate dateReprise;

    @AssertTrue(message = "Les dates de conge et de reprise sont incoherentes.")
    public boolean isChronologieValide() {
        return dateDebutConge == null || dateFinConge == null || dateReprise == null
                || (!dateFinConge.isBefore(dateDebutConge) && dateReprise.isAfter(dateFinConge));
    }
}
