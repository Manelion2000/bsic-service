package com.bakouan.app.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.time.LocalDate;

@Data
public class BaAutorisationAbsenceRequestDto {
    @NotBlank @Size(max = 1000) private String motif;
    @NotNull private LocalDate dateDebut;
    @NotNull private LocalDate dateFin;
}
