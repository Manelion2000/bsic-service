package com.bakouan.app.dto;
import com.bakouan.app.enums.EFonctionEmploye;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
public final class ProcessusDtos {
 private ProcessusDtos(){}
 public record Etape(@NotBlank @Size(max=150) String libelle,@NotBlank String selection,
  EFonctionEmploye fonction,String departementId,String serviceId,String agenceId,
  boolean departementDemandeur,List<@NotBlank String> utilisateurs,
  List<@NotNull EFonctionEmploye> fonctionsDemandeur,@Min(1) @Max(365) Integer delaiJours){}
 public record Definition(@NotBlank String mode,@NotBlank String politiqueRefus,Boolean dgObligatoire,
  @Size(max=30) List<@NotNull @Valid Etape> etapes){}
 public record Modele(@NotBlank @Size(max=150) String libelle,@Size(max=2000) String description,
  @NotNull @Valid Definition definition,Long version){}
 public record Publication(@NotNull Long version,@NotBlank @Size(max=1500) String justification){}
 public record CibleSuppression(@NotBlank @Pattern(regexp="CIRCUIT|PUBLICATION") String type,
  @NotBlank String id,@NotNull Long version){}
 public record Suppression(@NotEmpty @Size(max=100) List<@NotNull @Valid CibleSuppression> cibles){}
 public record Champ(@NotBlank @Pattern(regexp="[a-z][a-zA-Z0-9_]{0,49}") String cle,
  @NotBlank @Size(max=150) String libelle,@NotBlank String type,boolean obligatoire,
  @Size(max=30) List<@NotBlank @Size(max=150) String> choix){}
 public record Demarche(@NotBlank @Pattern(regexp="[A-Z][A-Z0-9_]{1,49}") String code,
  @NotBlank @Size(max=150) String libelle,@Size(max=2000) String description,
  @NotBlank String domaine,@NotBlank String publicationId,boolean actif,
  @Size(max=30) List<@NotNull @Valid Champ> champs,Long version){}
 public record Dossier(@NotBlank String demarcheId,@NotBlank @Size(max=200) String titre,
  @Size(max=100) Map<String,String> valeurs,Long version){}
 public record Action(@NotNull Long version,@NotBlank @Size(max=100) String cle,Integer etape,
  @Size(max=1500) String commentaire){}
}
