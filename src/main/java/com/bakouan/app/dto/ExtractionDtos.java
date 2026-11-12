package com.bakouan.app.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.List;

public final class ExtractionDtos {
    private ExtractionDtos() {}
    public enum Etat { NOUVELLE, EN_COURS, TERMINEE, ANNULEE }
    public enum Resultat { SUCCES, ECHEC }
    public enum Environnement { RECETTE, PRODUCTION, SECOURS, AUTRE }
    public enum TypeParametre { TEXTE, DATE, NOMBRE }
    public record Reference(String id,String nom) {}
    public record ServiceReference(String id,String nom,String departementId) {}
    public record Demandeur(String id,String nom,Reference departement,Reference service) {}
    public record Configuration(boolean demander,boolean bibliotheque,boolean gerer,Reference moi,
        Reference departement,Reference service,List<Demandeur> demandeurs,List<Reference> departements,List<ServiceReference> services) {}
    public record Parametre(@NotBlank @Pattern(regexp="[A-Za-z][A-Za-z0-9_]{0,29}") String nom,
        @NotNull TypeParametre type,@NotBlank @Size(max=500) String description) {}
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown=true)
    public record RequeteEcriture(@NotBlank @Size(max=200) String titre,@NotBlank @Size(max=4000) String objectif,
        @NotNull @PastOrPresent LocalDate dateValidation,
        @NotBlank @Size(max=255) String departementId,@Size(max=255) String departementNom,
        @Size(max=255) String serviceId,@Size(max=255) String serviceNom,
        @Size(max=128) @Pattern(regexp="([A-Za-z][A-Za-z0-9_$#]{0,127})?") String schemaCible,
        @NotBlank @Size(max=50000) String sqlTexte,@Size(max=30) List<@NotNull @Valid Parametre> parametres,
        Long version,@Size(max=1000) String motif,@Size(max=200) String motCle) {
        public RequeteEcriture {parametres=parametres==null?List.of():List.copyOf(parametres);motCle=motCle==null?"":motCle.trim();}
    }
    public record RequeteLigne(String id,String titre,String objectif,LocalDate dateValidation,String auteur,Reference departementDemandeur,Reference serviceDemandeur,
        int edition,boolean verifiee,boolean archivee,long version,Instant modifieLe,String motCle) {}
    public record RequeteDetail(RequeteLigne fiche,String schemaCible,String sqlTexte,List<Parametre> parametres,
        List<Revision> revisions,List<Execution> executions) {}
    public record Revision(String id,int edition,Instant date,String auteur,String motif,RequeteEcriture contenu) {}
    public record DemandeEcriture(@NotBlank @Size(max=200) String titre,@NotBlank @Size(max=4000) String objectif,
        @NotNull @PastOrPresent LocalDate dateDemande,@Size(max=255) String demandeurId,@Size(max=255) String serviceId,
        LocalDate echeance,LocalDate periodeDebut,LocalDate periodeFin,@Size(max=4000) String criteres) {}
    public record DemandeLigne(String id,String reference,String titre,Etat etat,LocalDate dateDemande,
        LocalDate echeance,Reference demandeur,Reference departement,Reference service,String auteur,Reference chargeDe,
        String requeteTitre,Integer edition,long version,Instant creeLe) {}
    public record Evenement(Instant date,String auteur,String action,String motif) {}
    public record DemandeDetail(DemandeLigne fiche,String objectif,LocalDate periodeDebut,LocalDate periodeFin,
        String criteres,Revision requete,List<Execution> executions,List<Evenement> historique,
        boolean peutTraiter,boolean peutAnnuler) {}
    public record Operation(@NotNull Long version,@NotBlank @Size(max=1000) String motif) {}
    public record PriseEnCharge(@NotNull Long version,@NotBlank @Size(max=255) String requeteId,
        @NotBlank @Size(max=1000) String motif) {}
    public record Essai(@NotNull Long version,@NotNull @PastOrPresent Instant executeLe,
        @NotNull Environnement environnement,@NotNull Resultat resultat,
        @NotBlank @Size(max=500) String referenceVerification,@Min(0) Long lignes,
        @Size(max=1000) String commentaire) {}
    public record Execution(String id,int edition,Instant executeLe,Instant enregistreLe,String auteur,
        Environnement environnement,Resultat resultat,String referenceVerification,Long lignes,String commentaire,
        String demandeReference) {}
    public record Page<T>(List<T> content,long totalElements,int totalPages,int number,int size) {}
}
