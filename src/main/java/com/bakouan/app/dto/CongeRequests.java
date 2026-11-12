package com.bakouan.app.dto;

import com.bakouan.app.dto.CongeDefinitions.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public final class CongeRequests {
    private CongeRequests() {}
    @Data public static class PolitiqueRequest {
        @NotBlank @Pattern(regexp="[A-Z][A-Z0-9_]{1,49}") private String typeCode;
        @NotBlank @Size(max=150) private String libelle;
        @NotBlank @Pattern(regexp="CONGE_ANNUEL|AUTRE_CONGE") private String categorie;
        @NotNull private LocalDate debutEffet;
        private LocalDate finEffet;
        @NotBlank @Size(max=1000) private String sourceRh;
        @NotNull @Valid private Politique regles;
    }
    @Data public static class CalendrierRequest {
        @NotBlank @Size(max=150) private String nom;
        @NotNull private LocalDate debutEffet;
        private LocalDate finEffet;
        @NotBlank @Size(max=1000) private String sourceRh;
        @NotNull @Valid private Calendrier regles;
    }
    @Data public static class Confirmation {
        @AssertTrue private boolean confirme;
        @NotBlank @Size(max=1000) private String justification;
    }
    @Data public static class Affectation {
        @NotBlank private String agentId;
        @NotBlank private String politiqueId;
        @NotBlank private String calendrierId;
        @NotNull private LocalDate debut;
        private LocalDate fin;
        private LocalDate dateEntree;
        private LocalDate dateSortie;
        @NotBlank @Size(max=1000) private String justification;
    }
    @Data public static class Demande {
        private String idBrouillon;
        @NotBlank private String typeCode;
        @NotNull private LocalDate debut;
        @NotNull private LocalDate fin;
        @NotNull private Portion portionDebut;
        @NotNull private Portion portionFin;
        private LocalTime heureDebut;
        private LocalTime heureFin;
        @Size(max=1000) private String motif;
        private String remplaceDemandeId;
        private LocalDate repriseAnticipee;
    }
    @Data public static class ModificationBrouillon {
        @NotNull @Valid private Demande demande;
        @NotNull @Valid private Operation operation;
    }
    @Data public static class Operation {
        @NotNull @Min(0) private Long version;
        @NotBlank @Size(max=150) private String cle;
        @Size(max=1500) private String justification;
        private Integer etape;
        private LocalDate dateReprise;
        private Portion portionReprise;
        private LocalTime heureReprise;
        private Boolean derogationCouverture;
    }
    @Data public static class PeriodeDroits {
        @NotBlank private String affectationId;
        @NotNull private LocalDate debut;
        @NotNull private LocalDate fin;
        private LocalDate expiration;
        @NotBlank @Size(max=150) private String cle;
        @NotBlank @Size(max=1000) private String source;
        @NotBlank @Size(max=1000) private String justification;
    }
    @Data public static class Ajustement {
        @NotBlank private String droitId;
        @NotNull @Digits(integer=14,fraction=4) private BigDecimal quantite;
        @NotBlank @Size(max=150) private String cle;
        @NotBlank @Size(max=1000) private String source;
        @NotBlank @Size(max=1000) private String justification;
    }
    @Data public static class Report {
        @NotBlank private String sourceDroitId;
        @NotBlank private String cibleDroitId;
        @NotBlank @Size(max=150) private String cle;
        @NotBlank @Size(max=1000) private String justification;
    }
    @Data public static class Passation {
        private String remplacantId;
        @NotBlank @Size(max=5000) private String tachesEtEcheances;
        @Size(max=1000) private String contactsUtiles;
    }
    @Data public static class CouvertureRequest {
        @NotBlank @Size(max=150) private String nom;
        @NotNull private LocalDate debut;
        private LocalDate fin;
        @NotBlank @Size(max=1000) private String sourceRh;
        @NotNull @Valid private Couverture regles;
    }
    @Data public static class Souhait {
        @NotNull private LocalDate debut;
        @NotNull private LocalDate fin;
    }
    @Data public static class ImportSolde {
        @NotNull private LocalDate arrete;
        @NotBlank @Size(max=1000) private String source;
        @Valid @NotEmpty @Size(max=500) private List<LigneSolde> lignes = new ArrayList<>();
    }
    @Data public static class LigneSolde {
        @NotBlank private String cle;
        @NotBlank private String affectationId;
        @NotNull private LocalDate origineDebut;
        @NotNull private LocalDate origineFin;
        private LocalDate expiration;
        @NotNull @DecimalMin("0") @Digits(integer=14,fraction=4) private BigDecimal solde;
        @NotBlank @Size(max=1000) private String justification;
    }
    @Data public static class RepriseSoldes {
        @NotNull @Valid private ImportSolde importation;
        @NotBlank private String empreinteSimulation;
        @NotNull @Valid private Confirmation confirmation;
    }
}
