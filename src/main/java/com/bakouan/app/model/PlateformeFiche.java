package com.bakouan.app.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.List;

/** Métadonnées du catalogue ; aucun identifiant de connexion ni secret. */
public record PlateformeFiche(
        Categorie categorie, CycleVie cycleVie, Criticite criticite,
        @Size(max=5000) String fonctionnalites, @Size(max=2000) String population,
        @Size(max=200) String editeur, @Size(max=100) String versionLogicielle,
        Hebergement hebergement, @Size(max=2000) String technologies,
        @Size(max=5000) String exploitation, @Size(max=3000) String sauvegarde,
        @Size(max=3000) String continuite,
        @Min(0) @Max(525600) @Digits(integer=6,fraction=0) java.math.BigDecimal rtoMinutes,
        @Min(0) @Max(525600) @Digits(integer=6,fraction=0) java.math.BigDecimal rpoMinutes,
        LocalDate dateMiseService, LocalDate dateFinSupport, LocalDate dateProchaineRevue,
        @Size(max=500) String contactSupport,
        @Size(max=12) List<@NotNull @Valid Environnement> environnements,
        @Size(max=20) List<@NotNull @Valid Lien> liens) {
    public enum Categorie { APPLICATION, INFRASTRUCTURE, SECURITE, COLLABORATION, DONNEES, AUTRE }
    public enum CycleVie { A_QUALIFIER, EN_PROJET, EN_SERVICE, EN_FIN_DE_VIE, RETIREE }
    public enum Criticite { A_QUALIFIER, FAIBLE, MODEREE, ELEVEE, CRITIQUE }
    public enum Hebergement { A_QUALIFIER, INTERNE, CLOUD_PRIVE, CLOUD_PUBLIC, SAAS, HYBRIDE }
    public enum TypeEnvironnement { PRODUCTION, PREPRODUCTION, RECETTE, DEVELOPPEMENT, SECOURS }
    public record Environnement(@NotNull TypeEnvironnement type, @NotBlank @Size(max=100) String nom,
                               @Size(max=1000) String url, @Size(max=2000) String description) {}
    public record Lien(@NotBlank @Size(max=150) String titre, @NotBlank @Size(max=1000) String url) {}
    public PlateformeFiche {
        categorie=categorie==null?Categorie.AUTRE:categorie;
        cycleVie=cycleVie==null?CycleVie.A_QUALIFIER:cycleVie;
        criticite=criticite==null?Criticite.A_QUALIFIER:criticite;
        hebergement=hebergement==null?Hebergement.A_QUALIFIER:hebergement;
        environnements=environnements==null?List.of():List.copyOf(environnements);
        liens=liens==null?List.of():List.copyOf(liens);
    }
    public static PlateformeFiche vide() { return new PlateformeFiche(null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,List.of(),List.of()); }
}
