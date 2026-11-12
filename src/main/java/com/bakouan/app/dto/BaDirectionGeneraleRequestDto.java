package com.bakouan.app.dto;

import com.bakouan.app.enums.EDgaPole;
import com.bakouan.app.enums.ESexe;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDate;

/** Identite uniquement : la fonction et les droits sont fixes par le serveur. */
@Data
public class BaDirectionGeneraleRequestDto {
    @NotBlank @Size(max = 50) private String nom;
    @NotBlank @Size(max = 50) private String prenom;
    @NotBlank @Email @Size(max = 50) private String email;
    @NotBlank @Size(max = 50) private String telephone;
    @NotNull private ESexe sexe;
    @Size(max = 50) private String matricule;
    private EDgaPole pole;
    private LocalDate dateDebut;
    private LocalDate dateFin;
}
