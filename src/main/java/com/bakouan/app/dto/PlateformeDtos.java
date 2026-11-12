package com.bakouan.app.dto;

import com.bakouan.app.enums.ETypePlateforme;
import com.bakouan.app.model.PlateformeFiche;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;

public final class PlateformeDtos {
    private PlateformeDtos() {}
    public record Reference(String id,String nom,boolean actif) {}
    public record Configuration(boolean consulter,boolean gerer,boolean habilitations,
                                List<Reference> departements,List<Reference> responsables,List<Reference> circuits) {}
    public record Ecriture(@NotBlank @Size(max=50) @Pattern(regexp="[A-Za-z0-9][A-Za-z0-9._-]*",message="Code : lettres, chiffres, points, tirets ou soulignés uniquement.") String code,
            @NotBlank @Size(max=200) String nom, @Size(max=2000) String description,
            ETypePlateforme typePlateforme, @Size(max=255) String idCircuit,
            @Size(max=255) String departementId, @Size(max=255) String responsableMetierId,
            @Size(max=255) String responsableTechniqueId, @NotNull @Valid PlateformeFiche fiche,
            @Size(max=100) List<@NotBlank @Size(max=255) String> dependanceIds,
            Long version, @NotBlank @Size(max=1000) String motif) {}
    public record Operation(@NotNull Long version,@NotBlank @Size(max=1000) String motif) {}
    public record NouveauManuel(@NotBlank @Size(max=150) String titre,@Size(max=2000) String description) {}
    public record ModificationsManuels(@Size(max=20) List<@NotNull @Valid NouveauManuel> ajouts,
            @Size(max=20) List<@NotBlank @Size(max=255) String> retraits) {
        public ModificationsManuels {ajouts=ajouts==null?List.of():List.copyOf(ajouts);retraits=retraits==null?List.of():List.copyOf(retraits);}
        public static ModificationsManuels vides(){return new ModificationsManuels(List.of(),List.of());}
    }
    public record Manuel(String id,String titre,String description,String nomFichier,String contentType,
            long taille,String sha256,Instant ajouteLe,String auteur) {}
    public record Ligne(String id,String code,String nom,String description,ETypePlateforme typePlateforme,
            PlateformeFiche.Categorie categorie,PlateformeFiche.CycleVie cycleVie,PlateformeFiche.Criticite criticite,
            Reference departement,Reference responsableTechnique,boolean archivee,long version,
            java.time.LocalDate dateProchaineRevue,java.time.LocalDate dateFinSupport,Instant modifieLe) {}
    public record Detail(String id,String code,String nom,String description,ETypePlateforme typePlateforme,
            Reference circuit,Reference departement,Reference responsableMetier,Reference responsableTechnique,
            PlateformeFiche fiche,List<Reference> dependances,List<Reference> utiliseePar,
            boolean archivee,long version,Instant modifieLe,List<Manuel> manuels) {
        public Detail {manuels=manuels==null?List.of():List.copyOf(manuels);}
    }
    public record Page(List<Ligne> content,long totalElements,int totalPages,int number,int size) {}
    public record Revision(long numero,Instant date,String auteur,String action,String motif,Detail snapshot) {}
}
